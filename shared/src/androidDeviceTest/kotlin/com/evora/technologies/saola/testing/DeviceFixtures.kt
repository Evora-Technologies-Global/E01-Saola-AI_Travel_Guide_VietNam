package com.evora.technologies.saola.testing

import com.evora.technologies.saola.domain.model.CatalogItem
import com.evora.technologies.saola.domain.model.CollectionEntry
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.DiscoveryCategory
import com.evora.technologies.saola.domain.model.GeoBounds
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.PassportStamp
import com.evora.technologies.saola.domain.model.Province
import com.evora.technologies.saola.domain.model.ProvinceType
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

/**
 * A rectangular province, which is all a map test needs one to be.
 *
 * Real outlines are thousands of vertices of coastline; a square is enough to exercise every
 * question this suite asks — where the projection puts a province, which one a tap resolves
 * to, and what a screen reader is told about it. Using the shipped asset instead would make
 * the test depend on 34 real polygons and on whichever of them happen to be adjacent, which is
 * data the assertions would then have to be re-derived from every time it is corrected.
 *
 * The ring is not closed: `Province` documents that consumers close it themselves.
 */
internal fun province(
    id: String,
    name: String = id,
    minLongitude: Double,
    minLatitude: Double,
    maxLongitude: Double,
    maxLatitude: Double,
): Province {
    val bounds = GeoBounds(minLongitude, minLatitude, maxLongitude, maxLatitude)
    return Province(
        id = id,
        name = name,
        nameEn = name,
        type = ProvinceType.PROVINCE,
        mergedFrom = emptyList(),
        center = GeoPoint(
            latitude = (minLatitude + maxLatitude) / 2,
            longitude = (minLongitude + maxLongitude) / 2,
        ),
        bounds = bounds,
        mainlandBounds = bounds,
        mainlandRings = listOf(
            doubleArrayOf(
                minLongitude, minLatitude,
                maxLongitude, minLatitude,
                maxLongitude, maxLatitude,
                minLongitude, maxLatitude,
            ),
        ),
        offshoreRings = emptyList(),
    )
}

/** A stamp on [province]; [discoveryCount] above zero is what "visited" means. */
internal fun stamp(province: Province, discoveryCount: Int = 0) = PassportStamp(
    province = province,
    discoveryCount = discoveryCount,
    coverImagePath = null,
    firstVisitAt = null,
    lastVisitAt = null,
)

/** One catalogue entry, collected when [discovery] is non-null. */
internal fun collectionEntry(
    id: String,
    name: String = id,
    hint: String = "how to spot a $id",
    discovery: Discovery? = null,
) = CollectionEntry(
    item = CatalogItem(
        id = id,
        category = DiscoveryCategory.FOOD,
        name = name,
        hint = hint,
        aliases = listOf(id),
    ),
    discovery = discovery,
)

private const val HANOI_LAT = 21.0
private const val HANOI_LON = 105.0
