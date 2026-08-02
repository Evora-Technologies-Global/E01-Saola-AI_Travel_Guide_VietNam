package com.duylt.trave.vietlensai.data.mapper

import com.duylt.trave.vietlensai.data.remote.openmap.dto.OverpassElement
import com.duylt.trave.vietlensai.domain.model.DiscoveryCategory
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.NearbyPlace
import kotlin.math.roundToInt

/**
 * OpenStreetMap tags → the app's own place type.
 *
 * @param origin where the search was centred, for the distance.
 * @return null when the element has no name or no position. Both are optional on the
 *   wire and neither is optional to a map: a marker with no coordinates cannot be
 *   drawn, and OSM is full of unnamed benches and plaques that have no business on a
 *   list of places to go.
 */
internal fun OverpassElement.toDomain(origin: GeoPoint): NearbyPlace? {
    val name = tags["name"]?.takeIf { it.isNotBlank() } ?: return null
    if (isNotADestination(name, tags)) return null
    // Nodes carry their own position; ways and relations carry a centroid, which is why
    // the query asks for `out center`. Larger sites — Hoàng thành Thăng Long, most
    // museums, every park — are mapped as areas, so ignoring `center` would drop them.
    val latitude = lat ?: center?.lat ?: return null
    val longitude = lon ?: center?.lon ?: return null
    val location = GeoPoint(latitude, longitude)

    return NearbyPlace(
        id = "$type/$id",
        name = name,
        category = categoryOf(tags),
        typeLabel = null, // filled in by the presentation layer, which knows the language
        location = location,
        summary = tags["description"]?.takeIf { it.isNotBlank() },
        photoUrl = null, // Wikipedia and Commons supply this later
        distanceMeters = origin.distanceMetersTo(location).roundToInt(),
        monthlyReaders = null,
        mappingDetail = MAPPING_DETAIL_TAGS.count { tags.containsKey(it) },
        openingHours = tags["opening_hours"]?.takeIf { it.isNotBlank() },
        website = (tags["website"] ?: tags["contact:website"])?.takeIf { it.isNotBlank() },
        phone = (tags["phone"] ?: tags["contact:phone"])?.takeIf { it.isNotBlank() },
        cuisine = tags["cuisine"]?.takeIf { it.isNotBlank() },
        wikipediaTitle = tags["wikipedia"]?.let(::wikipediaTitleOf),
    )
}

/**
 * Things OpenStreetMap files under "attraction" that nobody would travel to see.
 *
 * Measured against 249 named attractions within 5 km of Hoàn Kiếm, roughly half were
 * not destinations at all. This removes the three largest and most embarrassing groups
 * rather than attempting a general theory of what is worth visiting — a curated
 * blocklist is honest about being a blocklist.
 *
 * - **Zoo enclosures.** Twenty-five individual pens inside Thủ Lệ zoo are mapped as
 *   `tourism=attraction`, so a traveller four kilometres away was being recommended
 *   "Khỉ vàng" — a golden monkey — as somewhere to go. They carry `attraction=animal`
 *   or sit under a `zoo` key, which is what makes them separable from the zoo itself.
 * - **Traffic-island flower beds.** Forty-eight entries named "Vườn hoa …" are
 *   three-hundred-square-metre planted roundabouts tagged `leisure=park`, identically
 *   to Công viên Thống Nhất. Only the ones somebody thought worth describing survive.
 * - **Shops filed as artwork.** Tour agencies, framing shops and showrooms tagged
 *   `tourism=artwork`, which is how "Ha Giang Majestic Tours" ends up on a list of
 *   things to see.
 *
 * Plus outright noise: entries genuinely present in the data include `0 km`,
 * `Abandoned Van`, `Chuông` (a bell) and `Lư` (an incense burner).
 */
private fun isNotADestination(name: String, tags: Map<String, String>): Boolean {
    if (tags.containsKey("attraction") || tags.containsKey("zoo")) return true
    if (tags["tourism"] == "artwork" && SHOPFRONT_KEYS.any(tags::containsKey)) return true
    // A named park with nothing else recorded about it is, in this data, almost always
    // a planted roundabout. One with a Wikipedia article or opening hours is a park.
    if (tags["leisure"] in NATURE_LEISURE &&
        name.startsWith(FLOWER_BED_PREFIX, ignoreCase = true) &&
        SUBSTANCE_KEYS.none(tags::containsKey)
    ) {
        return true
    }
    val trimmed = name.trim()
    // "Lư" (an incense burner), "Nai" (a deer pen), "SAM-2" (a missile on a plinth).
    if (trimmed.length < MIN_NAME_LENGTH || trimmed.none { it.isLetter() }) return true
    // "0 km" — a distance marker mapped as an attraction. Caught by the *shape* of the
    // name rather than by its length, because a plain length rule takes "Phở 10" with
    // it, and a phở shop numbered like that is exactly what the screen is for.
    return trimmed.first().isDigit() && trimmed.count { it.isLetter() } < MIN_LETTERS
}

/** Below this many letters, a name starting with a digit is a measurement, not a place. */
private const val MIN_LETTERS = 4

/** Tags that betray a business rather than a work of art. */
private val SHOPFRONT_KEYS = listOf("shop", "office", "opening_hours", "brand")

/** Any one of these means somebody thought the place worth recording properly. */
private val SUBSTANCE_KEYS = listOf("wikipedia", "wikidata", "opening_hours", "description", "heritage")

/** "Vườn hoa" — literally "flower garden", and in Hanoi's data usually a roundabout. */
private const val FLOWER_BED_PREFIX = "Vườn hoa"

/** Shorter than this and it is a label, not a name: "0 km", "Lư", "Nai". */
private const val MIN_NAME_LENGTH = 4

/**
 * `vi:Đền Ngọc Sơn` → `Đền Ngọc Sơn`.
 *
 * OSM prefixes the tag with the language code of the wiki the article lives on. The
 * prefix is dropped rather than honoured: the app asks the wiki for the traveller's own
 * language, and a Vietnamese reader is better served by the Vietnamese article than by
 * whichever one the mapper happened to link. When that article does not exist the
 * lookup simply comes back empty, which is the same outcome as an unmapped place.
 */
private fun wikipediaTitleOf(tag: String): String? =
    tag.substringAfter(':', missingDelimiterValue = tag).takeIf { it.isNotBlank() }

/**
 * OSM tags → the app's own seven categories.
 *
 * Reusing [DiscoveryCategory] rather than inventing a parallel enum is what lets a
 * marker on the Explore map carry the same colour a temple already has in the journal
 * and on the culture board. The traveller learns one colour scheme, not two.
 *
 * **Precedence is this list's order, not the tag map's.** An earlier version iterated
 * whatever order the keys arrived in, which meant a museum inside a park could come out
 * as either depending on how the JSON happened to be ordered — the same place changing
 * colour between refreshes. Food is tested first because a restaurant inside a museum
 * is somewhere to eat; `historic` beats `tourism` because `historic=monument` with
 * `tourism=attraction` is a monument that happens to be worth visiting.
 */
private fun categoryOf(tags: Map<String, String>): DiscoveryCategory {
    tags["amenity"]?.let { amenity ->
        if (amenity in FOOD_AMENITIES) return DiscoveryCategory.FOOD
    }
    tags["historic"]?.let { historic ->
        return when (historic) {
            in WORSHIP_HISTORIC -> DiscoveryCategory.ARCHITECTURE
            in ARTEFACT_HISTORIC -> DiscoveryCategory.ARTIFACT
            else -> DiscoveryCategory.LANDMARK
        }
    }
    tags["tourism"]?.let { tourism ->
        return when (tourism) {
            "museum", "gallery" -> DiscoveryCategory.CULTURE
            "artwork" -> DiscoveryCategory.ARTIFACT
            "zoo", "aquarium", "viewpoint" -> DiscoveryCategory.NATURE
            else -> DiscoveryCategory.LANDMARK
        }
    }
    tags["leisure"]?.let { leisure ->
        if (leisure in NATURE_LEISURE) return DiscoveryCategory.NATURE
    }
    // Last, but the clause that matches most of Vietnam's landmarks. Pagodas, đình,
    // temples and cathedrals here are overwhelmingly tagged only as places of worship,
    // with no `tourism` or `historic` key to be caught by the branches above.
    if (tags["amenity"] == "place_of_worship" ||
        tags["landuse"] == "religious" ||
        tags.containsKey("religion")
    ) {
        return DiscoveryCategory.ARCHITECTURE
    }
    return DiscoveryCategory.OTHER
}

/**
 * The optional tags counted as evidence that somebody cared about this entry.
 *
 * A weak signal taken seriously, because for a restaurant it is the *only* signal
 * available: no open source carries ratings, and a place whose mapper bothered to add
 * opening hours, a website and a phone number is far more likely to be a real
 * established business than a bare node somebody dropped on a street corner.
 */
private val MAPPING_DETAIL_TAGS = listOf(
    "wikipedia",
    "wikidata",
    "opening_hours",
    "website",
    "contact:website",
    "phone",
    "contact:phone",
    "description",
    "heritage",
    "image",
    "cuisine",
    "addr:housenumber",
)

private val FOOD_AMENITIES = setOf("restaurant", "cafe", "bakery", "fast_food", "bar", "pub")

private val WORSHIP_HISTORIC = setOf("church", "temple", "monastery", "shrine", "mosque")

private val ARTEFACT_HISTORIC = setOf("memorial", "stone", "wayside_shrine", "boundary_stone")

private val NATURE_LEISURE = setOf("park", "garden", "nature_reserve")
