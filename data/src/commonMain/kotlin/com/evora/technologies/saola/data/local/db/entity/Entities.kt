package com.evora.technologies.saola.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room is the single source of truth for everything the traveller has seen.
 *
 * List-shaped fields are stored as JSON through
 * [com.evora.technologies.saola.data.local.db.Converters]. Normalising sections and
 * fun facts into their own tables would buy nothing here — they are only ever read
 * and written together with their parent discovery, and never queried across rows.
 */
@Entity(
    tableName = "discoveries",
    indices = [Index("createdAt"), Index("isFavorite"), Index("provinceId")],
)
data class DiscoveryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val localName: String?,
    val category: String,
    /** The capture's file **name**, never a path — see `CaptureStore.nameOf`. */
    val imageName: String?,
    val summary: String,
    val sectionsJson: String,
    val funFactsJson: String,
    val tagsJson: String,
    val nearbyJson: String,
    val suggestedQuestionsJson: String,
    val confidence: Float,
    val latitude: Double?,
    val longitude: Double?,
    /**
     * Which of the 34 provinces this was captured in, resolved once at write time.
     *
     * Denormalised on purpose. The passport map needs a per-province roll-up on every
     * emission, and running point-in-polygon over 9,228 vertices per row on each read
     * — for a value that can never change once the photo is taken — would be work
     * repeated forever to save one nullable column. Null when location was
     * unavailable or the fix was outside Vietnam.
     */
    val provinceId: String?,
    val placeHint: String?,
    val isFavorite: Boolean,
    val modelUsed: String?,
    val createdAt: Long,
)

/**
 * A single chat turn.
 *
 * Cascade-deletes with its discovery: a conversation about a deleted temple has
 * nothing left to be grounded on.
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = DiscoveryEntity::class,
            parentColumns = ["id"],
            childColumns = ["discoveryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("discoveryId"), Index("createdAt")],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val discoveryId: String,
    val role: String,
    val content: String,
    val createdAt: Long,
)

/**
 * The traveller's own note about a discovery, and the photos they kept with it.
 *
 * Keyed by `discoveryId` rather than an id of its own: there is at most one note per
 * discovery, so the discovery's id already identifies it and an upsert edits in place
 * without the caller having to look up a row id first.
 *
 * Cascade-deletes with its discovery. The photo *files* are not cascaded — SQLite cannot
 * delete a JPEG — so [com.evora.technologies.saola.data.repository.DiscoveryRepositoryImpl]
 * reads these paths before dropping the row.
 */
@Entity(
    tableName = "discovery_notes",
    foreignKeys = [
        ForeignKey(
            entity = DiscoveryEntity::class,
            parentColumns = ["id"],
            childColumns = ["discoveryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("createdAt")],
)
data class DiscoveryNoteEntity(
    @PrimaryKey val discoveryId: String,
    val body: String,
    /** JSON array of capture file **names**, never paths — see `CaptureStore.nameOf`. */
    val photoNamesJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * The traveller's objection to what a discovery says, kept until they withdraw it by
 * deleting the discovery itself.
 *
 * Keyed by `discoveryId` for the same reason [DiscoveryNoteEntity] is — at most one per
 * discovery — but the two differ in what a second write means: an upsert here *replaces*
 * the objection and moves `createdAt` with it, because a report is a claim about a result
 * rather than a memory of a place. See `DiscoveryReport`'s KDoc for that argument in full.
 *
 * [reason] holds a `ReportReason` by `name`, so the enum constants are a stored format:
 * renaming one reclassifies every row already written as `OTHER`, silently, because
 * `ReportRepositoryImpl` decodes an unknown name to that rather than throwing at a screen.
 */
@Entity(
    tableName = "discovery_reports",
    foreignKeys = [
        ForeignKey(
            entity = DiscoveryEntity::class,
            parentColumns = ["id"],
            childColumns = ["discoveryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("createdAt")],
)
data class DiscoveryReportEntity(
    @PrimaryKey val discoveryId: String,
    /** A `ReportReason` by `name`. Unknown values decode to `OTHER`. */
    val reason: String,
    val note: String,
    /** When the *latest* objection was filed, not the first — see the class KDoc. */
    val createdAt: Long,
)

@Entity(
    tableName = "translations",
    indices = [Index("createdAt")],
)
data class TranslationEntity(
    @PrimaryKey val id: String,
    /** The capture's file **name**, never a path — see `CaptureStore.nameOf`. */
    val imageName: String?,
    val detectedLanguage: String,
    val targetLanguage: String,
    val blocksJson: String,
    val contextNote: String?,
    val createdAt: Long,
)

/** One AI-written diary entry per day, keyed by ISO date so regeneration overwrites. */
@Entity(tableName = "trip_summaries")
data class TripSummaryEntity(
    @PrimaryKey val date: String,
    val headline: String,
    val narrative: String,
    val highlightsJson: String,
    val tomorrowIdeasJson: String,
    val generatedAt: Long,
)
