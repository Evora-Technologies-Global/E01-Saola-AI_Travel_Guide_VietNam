package com.duylt.trave.vietlensai.data.repository

import com.duylt.trave.vietlensai.data.local.db.Converters
import com.duylt.trave.vietlensai.data.local.db.dao.NoteDao
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryNoteEntity
import com.duylt.trave.vietlensai.data.mapper.toDomain
import com.duylt.trave.vietlensai.domain.model.DiscoveryNote
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.repository.NoteRepository
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * The traveller's notes, and the photo files that belong to them.
 *
 * This repository owns the lifetime of every JPEG a note points at. Nothing else can:
 * the note row is the only record that a picked photo is still wanted, so dropping a
 * photo from an edit and deleting the row are both moments where a file stops being
 * reachable and has to go with it.
 */
internal class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val captureStore: CaptureStore,
    private val ioDispatcher: CoroutineDispatcher,
) : NoteRepository {

    override fun observeNote(discoveryId: String): Flow<DiscoveryNote?> =
        noteDao.observeByDiscovery(discoveryId)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
            .fallbackOnFailure(null, what = "observe the note for $discoveryId")

    override suspend fun save(
        discoveryId: String,
        body: String,
        photoPaths: List<String>,
    ): AppResult<Unit> = withContext(ioDispatcher) {
        val trimmed = body.trim()
        val photos = photoPaths.distinct().take(DiscoveryNote.MAX_PHOTOS)

        // Clearing the text and removing every photo is how a traveller deletes a note;
        // storing the blank row instead would leave the screen showing an empty card
        // they have no obvious way to get rid of.
        if (trimmed.isEmpty() && photos.isEmpty()) {
            return@withContext deleteInternal(discoveryId)
        }

        // Read and write under one guard. The read used to sit outside the try below, which
        // meant a note whose row could not be fetched threw out of a method that promises
        // AppResult — and this is the read the composer makes every time it opens.
        val written = runCatchingStorage(what = "save the note for $discoveryId") {
            val existing = noteDao.getByDiscovery(discoveryId)
            val now = Clock.System.now().toEpochMilliseconds()
            noteDao.upsert(
                DiscoveryNoteEntity(
                    discoveryId = discoveryId,
                    body = trimmed,
                    photoPathsJson = Converters.encodeStrings(photos),
                    // Rewriting a note does not change when the memory was written down.
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            existing
        }
        val existing = when (written) {
            is AppResult.Failure -> return@withContext written
            is AppResult.Success -> written.data
        }

        // Only after the write lands: a photo dropped from an edit that failed to save is
        // still referenced by the row on disk, and deleting it first would leave the note
        // pointing at a file that no longer exists.
        existing?.photoPaths()
            ?.filterNot { it in photos }
            ?.forEach { captureStore.delete(it) }

        AppResult.Success(Unit)
    }

    override suspend fun delete(discoveryId: String): AppResult<Unit> = withContext(ioDispatcher) {
        deleteInternal(discoveryId)
    }

    private suspend fun deleteInternal(discoveryId: String): AppResult<Unit> =
        runCatchingStorage(what = "delete the note for $discoveryId") {
            val existing = noteDao.getByDiscovery(discoveryId)
            noteDao.deleteByDiscovery(discoveryId)
            existing?.photoPaths()?.forEach { captureStore.delete(it) }
        }
}

internal fun DiscoveryNoteEntity.photoPaths(): List<String> =
    Converters.decodeStrings(photoPathsJson)
