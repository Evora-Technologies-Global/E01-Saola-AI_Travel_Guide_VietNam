package com.evora.technologies.saola.data.mapper

import com.evora.technologies.saola.data.local.db.Converters
import com.evora.technologies.saola.data.local.db.entity.ChatMessageEntity
import com.evora.technologies.saola.data.local.db.entity.DiscoveryNoteEntity
import com.evora.technologies.saola.data.local.db.entity.DiscoveryReportEntity
import com.evora.technologies.saola.data.local.db.entity.TextBoxJson
import com.evora.technologies.saola.data.local.db.entity.TranslationBlockJson
import com.evora.technologies.saola.data.local.db.entity.TranslationEntity
import com.evora.technologies.saola.data.local.db.entity.TripSummaryEntity
import com.evora.technologies.saola.data.remote.gemini.dto.LineTranslationPayload
import com.evora.technologies.saola.data.remote.gemini.dto.TripSummaryPayload
import com.evora.technologies.saola.domain.model.ChatMessage
import com.evora.technologies.saola.domain.model.ChatRole
import com.evora.technologies.saola.domain.model.DiscoveryNote
import com.evora.technologies.saola.domain.model.DiscoveryReport
import com.evora.technologies.saola.domain.model.RecognizedLine
import com.evora.technologies.saola.domain.model.ReportReason
import com.evora.technologies.saola.domain.model.TextBox
import com.evora.technologies.saola.domain.model.TranslateLanguage
import com.evora.technologies.saola.domain.model.TranslationBlock
import com.evora.technologies.saola.domain.model.TranslationResult
import com.evora.technologies.saola.domain.model.TripSummary
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

// --- Chat ---

internal fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    discoveryId = discoveryId,
    role = ChatRole.fromWire(role),
    content = content,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
)

internal fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    discoveryId = discoveryId,
    role = role.wireName,
    content = content,
    createdAt = createdAt.toEpochMilliseconds(),
)

// --- Notes ---

internal fun DiscoveryNoteEntity.toDomain(resolveCapture: (String) -> String): DiscoveryNote = DiscoveryNote(
    discoveryId = discoveryId,
    body = body,
    // A note whose photo files were lost still reads back as a note: decoding is lenient,
    // and the traveller's words are the part that cannot be replaced.
    photoPaths = Converters.decodeStrings(photoNamesJson).map(resolveCapture),
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

// --- Reports ---

/**
 * Decodes leniently, exactly as the note above it does, and for a sharper reason.
 *
 * The stored [DiscoveryReportEntity.reason] is a `ReportReason` by `name`, so a constant
 * renamed in `:domain` leaves rows on disk naming a case that no longer exists. Throwing there
 * would take down the result page of every discovery the traveller has ever objected to —
 * on a `Flow`, which means permanently, since a collector that throws is not retried.
 * [ReportReason.OTHER] is the honest answer instead: the note beside it still says what was
 * wrong, which is the part that could not be reconstructed.
 */
internal fun DiscoveryReportEntity.toDomain(): DiscoveryReport = DiscoveryReport(
    discoveryId = discoveryId,
    reason = ReportReason.entries.firstOrNull { it.name == reason } ?: ReportReason.OTHER,
    note = note,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
)

// --- Translation ---

/**
 * Pins each translated line back onto the line the recogniser found.
 *
 * Matched by the index the model was given rather than by position in its reply:
 * a model that drops or reorders one entry would otherwise shift every following
 * translation onto someone else's box, and the overlay has no way to notice. A
 * line the model never answered for keeps its original text, which reads as "not
 * translated" instead of as a confident mistranslation.
 */
internal fun LineTranslationPayload.toEntity(
    id: String,
    /** A capture **name**, never a path — see `CaptureStore.nameOf`. */
    imageName: String?,
    recognized: List<RecognizedLine>,
    target: TranslateLanguage,
    createdAt: Instant,
): TranslationEntity {
    val byIndex = lines.associateBy { it.index }
    return TranslationEntity(
        id = id,
        imageName = imageName,
        detectedLanguage = detectedLanguage.trim(),
        targetLanguage = target.code,
        blocksJson = Converters.encodeBlocks(
            recognized.mapIndexed { index, line ->
                val translated = byIndex[index]
                TranslationBlockJson(
                    original = translated?.original?.trim()?.takeIf(String::isNotEmpty) ?: line.text,
                    translated = translated?.translated?.trim().orEmpty().ifEmpty { line.text },
                    note = translated?.note?.trim()?.takeIf(String::isNotEmpty),
                    price = translated?.price?.trim()?.takeIf(String::isNotEmpty),
                    box = TextBoxJson(
                        left = line.box.left,
                        top = line.box.top,
                        right = line.box.right,
                        bottom = line.box.bottom,
                    ),
                )
            },
        ),
        contextNote = contextNote?.trim()?.takeIf { it.isNotEmpty() },
        createdAt = createdAt.toEpochMilliseconds(),
    )
}

internal fun TranslationEntity.toDomain(resolveCapture: (String) -> String): TranslationResult = TranslationResult(
    id = id,
    imagePath = imageName?.let(resolveCapture),
    detectedLanguage = detectedLanguage,
    targetLanguage = targetLanguage,
    blocks = Converters.decodeBlocks(blocksJson).map {
        TranslationBlock(
            original = it.original,
            translated = it.translated,
            note = it.note,
            price = it.price,
            box = it.box?.let { box ->
                TextBox(left = box.left, top = box.top, right = box.right, bottom = box.bottom)
            },
        )
    },
    contextNote = contextNote,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
)

// --- Trip summary ---

internal fun TripSummaryPayload.toEntity(date: LocalDate, generatedAt: Instant): TripSummaryEntity =
    TripSummaryEntity(
        date = date.toString(),
        headline = headline.trim(),
        narrative = narrative.trim(),
        highlightsJson = Converters.encodeStrings(
            highlights.mapNotNull { it.trim().takeIf(String::isNotEmpty) },
        ),
        tomorrowIdeasJson = Converters.encodeStrings(
            tomorrowIdeas.mapNotNull { it.trim().takeIf(String::isNotEmpty) },
        ),
        generatedAt = generatedAt.toEpochMilliseconds(),
    )

internal fun TripSummaryEntity.toDomain(): TripSummary = TripSummary(
    date = LocalDate.parse(date),
    headline = headline,
    narrative = narrative,
    highlights = Converters.decodeStrings(highlightsJson),
    tomorrowIdeas = Converters.decodeStrings(tomorrowIdeasJson),
    generatedAt = Instant.fromEpochMilliseconds(generatedAt),
)
