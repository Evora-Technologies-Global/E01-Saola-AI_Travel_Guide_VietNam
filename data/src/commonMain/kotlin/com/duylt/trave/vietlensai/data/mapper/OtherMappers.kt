package com.duylt.trave.vietlensai.data.mapper

import com.duylt.trave.vietlensai.data.local.db.Converters
import com.duylt.trave.vietlensai.data.local.db.entity.ChatMessageEntity
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryNoteEntity
import com.duylt.trave.vietlensai.data.local.db.entity.TextBoxJson
import com.duylt.trave.vietlensai.data.local.db.entity.TranslationBlockJson
import com.duylt.trave.vietlensai.data.local.db.entity.TranslationEntity
import com.duylt.trave.vietlensai.data.local.db.entity.TripSummaryEntity
import com.duylt.trave.vietlensai.data.remote.gemini.dto.LineTranslationPayload
import com.duylt.trave.vietlensai.data.remote.gemini.dto.TripSummaryPayload
import com.duylt.trave.vietlensai.domain.model.ChatMessage
import com.duylt.trave.vietlensai.domain.model.ChatRole
import com.duylt.trave.vietlensai.domain.model.DiscoveryNote
import com.duylt.trave.vietlensai.domain.model.RecognizedLine
import com.duylt.trave.vietlensai.domain.model.TextBox
import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.model.TranslationBlock
import com.duylt.trave.vietlensai.domain.model.TranslationResult
import com.duylt.trave.vietlensai.domain.model.TripSummary
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
