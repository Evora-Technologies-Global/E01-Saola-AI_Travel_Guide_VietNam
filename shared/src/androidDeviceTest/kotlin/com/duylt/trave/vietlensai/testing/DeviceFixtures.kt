package com.duylt.trave.vietlensai.testing

import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.DiscoveryCategory
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import kotlin.time.Instant

/**
 * Domain objects for the instrumented tests, in one place.
 *
 * `commonTest/testing/Fakes.kt` already has builders of this shape and this file is not it:
 * source sets do not see each other, so `androidDeviceTest` cannot call them. Rather than a
 * third copy appearing every time a device test needs a `Discovery` — there were two before
 * the two-pane tests were written — the builders live here and every instrumented test uses
 * them.
 *
 * Deliberately *not* a fake repository or a Koin module. What a device test drives is a
 * composable holding `state`, so all it needs is a value; anything more would be the same
 * duplication one layer up.
 */

/**
 * A landmark with nothing optional filled in.
 *
 * Every field a caller might want to vary is a parameter with a default, and every field that
 * is only there because the constructor demands it is fixed here — which is what keeps a test
 * that cares about *twelve rows existing* from spelling out eighteen fields to say so.
 */
internal fun discovery(
    id: String,
    title: String = "Temple $id",
    category: DiscoveryCategory = DiscoveryCategory.LANDMARK,
    isFavorite: Boolean = false,
) = Discovery(
    id = id,
    title = title,
    localName = null,
    category = category,
    imagePath = null,
    summary = "",
    sections = emptyList(),
    funFacts = emptyList(),
    tags = emptyList(),
    nearbySuggestions = emptyList(),
    suggestedQuestions = emptyList(),
    confidence = 0.9f,
    location = GeoPoint(HANOI_LAT, HANOI_LON),
    placeHint = null,
    isFavorite = isFavorite,
    modelUsed = null,
    createdAt = Instant.fromEpochSeconds(0),
)

private const val HANOI_LAT = 21.0
private const val HANOI_LON = 105.0
