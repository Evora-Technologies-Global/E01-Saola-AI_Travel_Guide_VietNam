package com.evora.technologies.saola.data.mapper

import com.evora.technologies.saola.data.local.db.Converters
import com.evora.technologies.saola.data.local.db.entity.DiscoveryEntity
import com.evora.technologies.saola.data.local.db.entity.NearbyJson
import com.evora.technologies.saola.data.local.db.entity.SectionJson
import com.evora.technologies.saola.data.remote.gemini.dto.DiscoveryPayload
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.DiscoveryCategory
import com.evora.technologies.saola.domain.model.DiscoverySection
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.NearbySuggestion
import kotlin.time.Instant

/**
 * Wire → storage → domain conversions for discoveries.
 *
 * Three representations exist on purpose: the DTO can change with the prompt, the
 * entity has to stay stable for old rows, and the domain model is what the UI is
 * written against. Mapping between them is where model output gets sanitised —
 * confidence is clamped and blank strings are dropped here, once, instead of being
 * defended against in every composable.
 */
internal fun DiscoveryPayload.toEntity(
    id: String,
    /** A capture **name**, never a path — see `CaptureStore.nameOf`. */
    imageName: String?,
    location: GeoPoint?,
    provinceId: String?,
    modelUsed: String?,
    createdAt: Instant,
): DiscoveryEntity = DiscoveryEntity(
    id = id,
    title = title.trim(),
    localName = localName?.trim()?.takeIf { it.isNotEmpty() },
    category = DiscoveryCategory.fromWire(category).wireName,
    imageName = imageName,
    summary = summary.trim(),
    sectionsJson = Converters.encodeSections(
        sections
            .filter { it.title.isNotBlank() && it.body.isNotBlank() }
            .map { SectionJson(title = it.title.trim(), body = it.body.trim()) },
    ),
    funFactsJson = Converters.encodeStrings(funFacts.mapNotNull { it.trim().takeIf(String::isNotEmpty) }),
    tagsJson = Converters.encodeStrings(tags.mapNotNull { it.trim().takeIf(String::isNotEmpty) }),
    nearbyJson = Converters.encodeNearby(
        nearbySuggestions
            .filter { it.name.isNotBlank() }
            .map {
                NearbyJson(
                    name = it.name.trim(),
                    reason = it.reason.trim(),
                    category = DiscoveryCategory.fromWire(it.category).wireName,
                    walkingMinutes = it.walkingMinutes,
                )
            },
    ),
    suggestedQuestionsJson = Converters.encodeStrings(
        suggestedQuestions.mapNotNull { it.trim().takeIf(String::isNotEmpty) },
    ),
    confidence = confidence.coerceIn(0f, 1f),
    latitude = location?.latitude,
    longitude = location?.longitude,
    provinceId = provinceId,
    placeHint = placeHint?.trim()?.takeIf { it.isNotEmpty() },
    isFavorite = false,
    modelUsed = modelUsed,
    createdAt = createdAt.toEpochMilliseconds(),
)

/**
 * @param resolveCapture turns the stored file name into a path that can be opened on this
 *   launch. Passed in rather than looked up because the answer belongs to the platform's
 *   `CaptureStore`, and it must be asked again every time — `imageName` on the entity is
 *   only a name, and the directory it sits in moves. See `CaptureStore.nameOf`.
 */
internal fun DiscoveryEntity.toDomain(resolveCapture: (String) -> String): Discovery = Discovery(
    id = id,
    title = title,
    localName = localName,
    category = DiscoveryCategory.fromWire(category),
    imagePath = imageName?.let(resolveCapture),
    summary = summary,
    sections = Converters.decodeSections(sectionsJson)
        .map { DiscoverySection(title = it.title, body = it.body) },
    funFacts = Converters.decodeStrings(funFactsJson),
    tags = Converters.decodeStrings(tagsJson),
    nearbySuggestions = Converters.decodeNearby(nearbyJson).map {
        NearbySuggestion(
            name = it.name,
            reason = it.reason,
            category = DiscoveryCategory.fromWire(it.category),
            walkingMinutes = it.walkingMinutes,
        )
    },
    suggestedQuestions = Converters.decodeStrings(suggestedQuestionsJson),
    confidence = confidence,
    location = if (latitude != null && longitude != null) GeoPoint(latitude, longitude) else null,
    placeHint = placeHint,
    isFavorite = isFavorite,
    modelUsed = modelUsed,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
)
