package com.duylt.trave.vietlensai.data.repository

import com.duylt.trave.vietlensai.data.local.db.dao.DiscoveryDao
import com.duylt.trave.vietlensai.data.local.db.dao.NoteDao
import com.duylt.trave.vietlensai.data.local.db.dao.TripSummaryDao
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryNoteEntity
import com.duylt.trave.vietlensai.data.mapper.toDomain
import com.duylt.trave.vietlensai.data.mapper.toEntity
import com.duylt.trave.vietlensai.data.remote.gemini.GeminiRemoteDataSource
import com.duylt.trave.vietlensai.domain.model.longLabel
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.JournalDay
import com.duylt.trave.vietlensai.domain.model.JournalStats
import com.duylt.trave.vietlensai.domain.model.TripSummary
import com.duylt.trave.vietlensai.domain.repository.JournalRepository
import com.duylt.trave.vietlensai.domain.repository.SettingsRepository
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The travel diary.
 *
 * Days are cut using the device's own time zone rather than UTC — a bowl of phở at
 * 11pm in Hanoi belongs to that evening, not to the next morning, and the whole
 * point of the journal is that it matches the traveller's memory of the day.
 */
internal class JournalRepositoryImpl(
    private val discoveryDao: DiscoveryDao,
    private val summaryDao: TripSummaryDao,
    private val noteDao: NoteDao,
    private val remote: GeminiRemoteDataSource,
    private val settingsRepository: SettingsRepository,
    private val timeZone: TimeZone,
    private val ioDispatcher: CoroutineDispatcher,
) : JournalRepository {

    override fun observeJournal(): Flow<List<JournalDay>> = combine(
        discoveryDao.observeAll(),
        summaryDao.observeAll(),
    ) { discoveries, summaries ->
        val summariesByDate = summaries.associate { it.date to it.toDomain() }
        discoveries
            .map { it.toDomain() }
            .groupBy { it.createdAt.toLocalDate() }
            // `toSortedMap` is java.util-only. Sorting the entries and keeping the
            // result a List is what the caller wanted anyway — the map was never read
            // by key, only iterated newest-day-first.
            .entries
            .sortedByDescending { it.key }
            .map { (date, items) ->
                JournalDay(
                    date = date,
                    discoveries = items.sortedByDescending { it.createdAt },
                    summary = summariesByDate[date.toString()],
                )
            }
    }
        // Off the collector's thread: the `combine` above decodes every discovery, parses a
        // `LocalDate` per summary row and then groups and sorts the lot, and the collector is
        // a ViewModel on `Dispatchers.Main.immediate`. One `flowOn` here covers the whole
        // transform because it applies upstream of this point.
        .flowOn(ioDispatcher)
        // The diary is the app's home screen, and this is the read that runs before the
        // traveller has done anything. A summary row whose `date` column no longer parses
        // — `TripSummaryEntity.toDomain` calls `LocalDate.parse` on it — would otherwise
        // kill the collector and leave the journal blank for good, with nothing on screen
        // to retry from.
        .fallbackOnFailure(emptyList(), what = "read the journal")

    override fun observeStats(): Flow<JournalStats> =
        discoveryDao.observeAll()
            .map { entities ->
                val discoveries = entities.map { it.toDomain() }
                JournalStats(
                    totalDiscoveries = discoveries.size,
                    totalDays = discoveries.mapTo(mutableSetOf()) { it.createdAt.toLocalDate() }.size,
                    favoriteCount = discoveries.count { it.isFavorite },
                    categoryBreakdown = discoveries
                        .groupingBy { it.category }
                        .eachCount(),
                )
            }
            .flowOn(ioDispatcher)
            .fallbackOnFailure(
                JournalStats(
                    totalDiscoveries = 0,
                    totalDays = 0,
                    favoriteCount = 0,
                    categoryBreakdown = emptyMap(),
                ),
                what = "read the journal statistics",
            )

    override suspend fun generateSummary(date: LocalDate): AppResult<TripSummary> =
        withContext(ioDispatcher) {
            val dayStart = date.atStartOfDayIn(timeZone).toEpochMilliseconds()
            val dayEnd = date.plus(1, DateTimeUnit.DAY)
                .atStartOfDayIn(timeZone)
                .toEpochMilliseconds() - 1

            val discoveries = when (
                val read = runCatchingStorage(what = "read the discoveries for $date") {
                    discoveryDao.getBetween(dayStart, dayEnd).map { it.toDomain() }
                }
            ) {
                is AppResult.Failure -> return@withContext read
                is AppResult.Success -> read.data
            }
            if (discoveries.isEmpty()) {
                return@withContext AppResult.Failure(
                    AppError.Unexpected("Nothing was discovered on $date"),
                )
            }

            val settings = settingsRepository.current()
            val dateLabel = date.longLabel(settings.language)

            // Read by discovery id rather than by `createdAt` between the same bounds: a
            // note written on the bus the next morning belongs to the day it is about,
            // and the discoveries above have already had that day cut for them.
            //
            // The notes are the only optional half of the prompt, so a failure to read
            // them costs the diary its quotations and nothing else — refusing to write the
            // day at all because one table would not open serves nobody.
            val notesByDiscovery = runCatchingStorageOr(
                what = "read the notes for $date",
                fallback = emptyMap(),
            ) {
                noteDao.getForDiscoveries(discoveries.map { it.id }).associateBy { it.discoveryId }
            }

            val response = when (
                val result = remote.daySummary(
                    dateLabel = dateLabel,
                    entries = discoveries.map { it.toPromptLine() },
                    notes = discoveries.mapNotNull { it.toNotePromptLine(notesByDiscovery) },
                    language = settings.language,
                    model = settings.preferredModel,
                )
            ) {
                is AppResult.Failure -> return@withContext result
                is AppResult.Success -> result.data
            }

            val entity = response.value.toEntity(date = date, generatedAt = Clock.System.now())
            val stored = runCatchingStorage(what = "persist the summary for $date") {
                summaryDao.upsert(entity)
            }
            if (stored is AppResult.Failure) return@withContext stored
            AppResult.Success(entity.toDomain())
        }

    private fun Discovery.toPromptLine(): String = buildString {
        append(title)
        localName?.takeIf { it.isNotBlank() && it != title }?.let { append(" (").append(it).append(')') }
        append(" — ").append(category.wireName.lowercase())
        placeHint?.takeIf { it.isNotBlank() }?.let { append(", ").append(it) }
        if (summary.isNotBlank()) append(": ").append(summary.take(MAX_SUMMARY_CHARS))
    }

    /**
     * The traveller's own note about this discovery, tied back to the place it is about.
     *
     * Quoted rather than paraphrased into the prompt so the model can hear the register
     * they wrote in — "chen chúc nhưng đáng" reads nothing like a caption, and a diary
     * that sounds like its author is the whole point of feeding these in.
     */
    private fun Discovery.toNotePromptLine(
        notesByDiscovery: Map<String, DiscoveryNoteEntity>,
    ): String? {
        val note = notesByDiscovery[id] ?: return null
        val body = note.body.trim().takeIf { it.isNotEmpty() } ?: return null
        return buildString {
            append(title).append(" — they wrote: \"")
            append(body.take(MAX_NOTE_CHARS))
            append('"')
            // How many photos they kept is a measure of how much the moment mattered,
            // which the note text alone does not always carry.
            val photoCount = note.photoPaths().size
            if (photoCount > 0) {
                append(", and kept ").append(photoCount).append(" photo")
                if (photoCount > 1) append('s')
                append(" of their own")
            }
        }
    }

    private fun Instant.toLocalDate(): LocalDate = toLocalDateTime(timeZone).date

    private companion object {
        const val MAX_SUMMARY_CHARS = 200

        /** Long enough for a real diary entry, short enough that ten of them still fit. */
        const val MAX_NOTE_CHARS = 400
    }
}
