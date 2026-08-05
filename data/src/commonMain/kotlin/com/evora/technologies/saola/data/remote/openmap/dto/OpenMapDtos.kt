package com.evora.technologies.saola.data.remote.openmap.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * The wire formats of the three open sources behind the Explore map.
 *
 * All three are public APIs that need no key and no billing account, which is the whole
 * reason they are here: the Places API they replaced could be switched off by a Google
 * Cloud setting, and this cannot.
 *
 * Every field is nullable or defaulted. None of these APIs promises a field will be
 * present — an OSM node carries whatever tags somebody happened to type, and a
 * Wikipedia page may have no thumbnail, no extract and no view data at all.
 */

// ---------------------------------------------------------------- Overpass (OSM)

@Serializable
internal data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList(),
    /**
     * How Overpass reports that it gave up.
     *
     * A query that exceeds its server-side budget answers **200 OK** with an empty
     * element list and a remark reading "runtime error: Query timed out". Without
     * reading this field the response is indistinguishable from a genuinely empty
     * area, and the screen tells a traveller standing in central Hanoi that there is
     * nothing around them.
     */
    val remark: String? = null,
)

/**
 * One OSM element.
 *
 * [lat]/[lon] are set for nodes; ways and relations carry their centroid in [center]
 * instead, which is why the query asks for `out center`. Many Vietnamese temples,
 * museums and parks are mapped as areas rather than points, so dropping everything
 * without a top-level `lat` would lose exactly the places most worth showing.
 */
@Serializable
internal data class OverpassElement(
    val type: String = "",
    val id: Long = 0,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String> = emptyMap(),
)

@Serializable
internal data class OverpassCenter(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)

// ---------------------------------------------------------------- MediaWiki

/**
 * The shared envelope of every MediaWiki `action=query` response.
 *
 * [normalized] and [redirects] matter more than they look. A title asked for is not
 * always the title answered with: MediaWiki silently normalises underscores and case,
 * and follows redirects. Without following both chains, a lookup for a title OSM
 * supplied comes back keyed under a name the caller never asked about, and the answer
 * is quietly dropped as missing.
 */
@Serializable
internal data class WikiQueryResponse(
    val query: WikiQuery? = null,
)

@Serializable
internal data class WikiQuery(
    val pages: List<WikiPage> = emptyList(),
    val normalized: List<WikiTitleMapping> = emptyList(),
    val redirects: List<WikiTitleMapping> = emptyList(),
)

@Serializable
internal data class WikiTitleMapping(
    val from: String = "",
    val to: String = "",
)

@Serializable
internal data class WikiPage(
    val pageid: Long? = null,
    val title: String = "",
    /** Present, and true, only when the page does not exist. */
    val missing: Boolean = false,
    val extract: String? = null,
    val thumbnail: WikiThumbnail? = null,
    val coordinates: List<WikiCoordinate> = emptyList(),
    val imageinfo: List<WikiImageInfo> = emptyList(),
    /**
     * Daily view counts keyed by date, with nulls for days with no data.
     *
     * Requested in batches of ten titles. The API silently returns nulls for every page
     * past roughly the sixth in a larger batch, which looks exactly like a genuinely
     * unread article — so the batch size is a correctness constraint, not a tuning knob.
     */
    val pageviews: Map<String, Int?> = emptyMap(),
)

@Serializable
internal data class WikiThumbnail(
    val source: String = "",
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
internal data class WikiCoordinate(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)

@Serializable
internal data class WikiImageInfo(
    /** A pre-scaled URL, from `iiurlwidth`. The full-size `url` is often many megabytes. */
    @SerialName("thumburl") val thumbUrl: String? = null,
    val url: String? = null,
    @SerialName("descriptionurl") val descriptionUrl: String? = null,
)
