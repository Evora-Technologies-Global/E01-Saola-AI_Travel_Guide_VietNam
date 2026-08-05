package com.duylt.trave.vietlensai.data.seed

import com.duylt.trave.vietlensai.data.local.db.Converters
import com.duylt.trave.vietlensai.data.local.db.entity.ChatMessageEntity
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryEntity
import com.duylt.trave.vietlensai.data.local.db.entity.NearbyJson
import com.duylt.trave.vietlensai.data.local.db.entity.SectionJson
import com.duylt.trave.vietlensai.data.local.db.entity.TripSummaryEntity
import com.duylt.trave.vietlensai.data.local.file.ImagePolicy
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** One discovery ready to write, and the asset holding the photograph that belongs to it. */
internal data class SeededDiscovery(
    val entity: DiscoveryEntity,
    val photoAsset: String,
)

internal data class SeededTrip(
    val discoveries: List<SeededDiscovery>,
    val messages: List<ChatMessageEntity>,
    val summaries: List<TripSummaryEntity>,
)

/**
 * Turns the hand-written demo file into rows, anchored to the day the app is opened.
 *
 * Pure, and separated from the seeder for exactly that reason: the writing half needs Room, a
 * file system and an asset packager, while everything that could actually be *wrong* — the
 * arithmetic below, the category and role wire names, the JSON columns — is here and testable
 * on the JVM in milliseconds.
 *
 * @param nowMillis the moment the app opened.
 * @param timeZone the device's zone, so "yesterday" means the traveller's yesterday.
 */
internal fun DemoContent.toTrip(nowMillis: Long, timeZone: TimeZone): SeededTrip {
    val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
    val startOfToday = today.atStartOfDayIn(timeZone).toEpochMilliseconds()

    // Today's entries are written with an hour-of-day, and the app may well be opened before
    // that hour has arrived. Clamping keeps the journal from showing a find "at 21:42" while
    // the clock says noon; the minute-per-item step preserves their order after the clamp.
    var clamped = 0
    val ids = mutableMapOf<String, String>()

    val discoveries = discoveries.map { demo ->
        val stamp = startOfToday - demo.day * DAY_MILLIS + demo.hour * HOUR_MILLIS
        val createdAt = if (stamp <= nowMillis) stamp else nowMillis - (clamped++) * MINUTE_MILLIS
        val rowId = "$ID_PREFIX${demo.id}"
        ids[demo.id] = rowId

        SeededDiscovery(
            photoAsset = "$ASSET_DIRECTORY/${demo.id}.jpg",
            entity = DiscoveryEntity(
                id = rowId,
                title = demo.title,
                localName = demo.localName,
                category = demo.category,
                // Named the way a real capture is, so the orphan sweep dates it and the
                // reader resolves it through exactly the same path as a photograph taken here.
                imageName = ImagePolicy.captureFileName(createdAt),
                summary = demo.summary,
                sectionsJson = Converters.encodeSections(
                    demo.sections.map { SectionJson(title = it.title, body = it.body) },
                ),
                funFactsJson = Converters.encodeStrings(demo.funFacts),
                tagsJson = Converters.encodeStrings(demo.tags),
                nearbyJson = Converters.encodeNearby(
                    demo.nearby.map {
                        NearbyJson(
                            name = it.name,
                            reason = it.reason,
                            category = it.category,
                            walkingMinutes = it.walkingMinutes,
                        )
                    },
                ),
                suggestedQuestionsJson = Converters.encodeStrings(demo.suggestedQuestions),
                confidence = demo.confidence,
                latitude = demo.lat,
                longitude = demo.lon,
                // Left null on purpose: the app's own backfill resolves every row against the
                // shipped province outlines on first open, which is the same geometry a real
                // capture goes through. Writing the answer here would test nothing.
                provinceId = null,
                placeHint = demo.placeHint,
                isFavorite = demo.favorite,
                modelUsed = MODEL_LABEL,
                createdAt = createdAt,
            ),
        )
    }

    val parentId = chat?.discoveryId?.let(ids::get)
    val messages = if (chat == null || parentId == null) {
        emptyList()
    } else {
        val base = discoveries.first { it.entity.id == parentId }.entity.createdAt + MINUTE_MILLIS
        chat.messages.mapIndexed { index, message ->
            ChatMessageEntity(
                id = "$ID_PREFIX$CHAT_PREFIX$index",
                discoveryId = parentId,
                role = message.role,
                content = message.content,
                createdAt = base + index * CHAT_STEP_MILLIS,
            )
        }
    }

    val summaries = tripSummaries.map { summary ->
        val date = today.minus(summary.day, DateTimeUnit.DAY)
        TripSummaryEntity(
            date = date.toString(),
            headline = summary.headline,
            narrative = summary.narrative,
            highlightsJson = Converters.encodeStrings(summary.highlights),
            tomorrowIdeasJson = Converters.encodeStrings(summary.tomorrowIdeas),
            generatedAt = date.atStartOfDayIn(timeZone).toEpochMilliseconds() + EVENING_MILLIS,
        )
    }

    return SeededTrip(discoveries, messages, summaries)
}

/**
 * Where the seed assets sit inside the packaged app.
 *
 * `:app`'s debug variant is the only thing that puts them there — see its `seedAssets` task.
 */
internal const val ASSET_DIRECTORY = "seed"
internal const val DEMO_CONTENT_ASSET = "$ASSET_DIRECTORY/demo-content.json"

/**
 * Row ids are derived from the demo file rather than random.
 *
 * Two things fall out of that and both are wanted: re-seeding cannot double up, and a row that
 * turns up in a bug report is identifiable as demo data at a glance.
 */
private const val ID_PREFIX = "demo-"
private const val CHAT_PREFIX = "chat-"
private const val MODEL_LABEL = "seed"

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 3_600_000L
private const val DAY_MILLIS = 86_400_000L
private const val CHAT_STEP_MILLIS = 90_000L

/** Where a day's summary is stamped — late enough to read as written at the end of it. */
private const val EVENING_MILLIS = 20 * HOUR_MILLIS
