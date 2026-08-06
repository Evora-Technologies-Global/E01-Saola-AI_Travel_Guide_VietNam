package com.evora.technologies.saola.domain.model

/** Tỉnh, or a centrally-governed thành phố. Only affects how the name is written. */
enum class ProvinceType { PROVINCE, CITY }

/** A WGS84 rectangle. Used to fit a photo into a province and to cheaply reject hit tests. */
data class GeoBounds(
    val minLongitude: Double,
    val minLatitude: Double,
    val maxLongitude: Double,
    val maxLatitude: Double,
) {
    val longitudeSpan: Double get() = maxLongitude - minLongitude
    val latitudeSpan: Double get() = maxLatitude - minLatitude

    operator fun contains(point: GeoPoint): Boolean =
        point.longitude in minLongitude..maxLongitude &&
            point.latitude in minLatitude..maxLatitude
}

/**
 * Vietnam's two offshore archipelagos.
 *
 * They sit 400–1,000 km out to sea, so drawing them in place would push the map's
 * extent 3.4° further east and squeeze the mainland — the part a traveller actually
 * collects — into little more than half the screen. Every Vietnamese map solves this
 * the same way, with a small labelled inset box, and so does this one.
 */
enum class Archipelago(
    val vietnameseName: String,
    val bounds: GeoBounds,
) {
    // Both boxes are drawn a little wider than the islands they hold. An island
    // sitting exactly on the edge would be projected onto the frame and half of it
    // clipped away, and the southernmost reef in Trường Sa reaches 7.75°N.

    /** Administered as huyện đảo Hoàng Sa, a district of Đà Nẵng. */
    HOANG_SA(
        vietnameseName = "Hoàng Sa",
        bounds = GeoBounds(111.2, 15.5, 112.9, 17.3),
    ),

    /** Administered as huyện Trường Sa, part of Khánh Hòa. */
    TRUONG_SA(
        vietnameseName = "Trường Sa",
        bounds = GeoBounds(111.2, 7.5, 115.4, 11.8),
    ),
    ;

    companion object {
        /** Which archipelago a ring belongs to, by where its first vertex falls. */
        fun of(ring: DoubleArray): Archipelago? {
            if (ring.size < 2) return null
            val point = GeoPoint(latitude = ring[1], longitude = ring[0])
            return entries.firstOrNull { point in it.bounds }
        }
    }
}

/**
 * One of the 34 administrative units Vietnam has had since the 2025 reorganisation.
 *
 * Rings are flat `[lon, lat, lon, lat, …]` arrays rather than `List<GeoPoint>`. The
 * country is ~9,200 vertices; boxing each one would mean 9,200 allocations at load
 * and a pointer chase per vertex in both the renderer and the hit test, which run on
 * every frame and every capture respectively. The last point of a ring does not
 * repeat the first — consumers close the path themselves.
 *
 * Equality is by [id] alone. The geometry is immutable and loaded once, so two
 * instances with the same id are the same province; comparing megabytes of
 * coordinates to discover that would be pure waste, and `DoubleArray` equality is by
 * reference anyway, which would make a generated `equals` quietly wrong.
 */
class Province(
    val id: String,
    val name: String,
    val nameEn: String,
    val type: ProvinceType,
    /** Pre-2025 provinces folded into this one. Empty when the territory is unchanged. */
    val mergedFrom: List<String>,
    val center: GeoPoint,
    /** Everything, archipelagos included. This is what containment tests against. */
    val bounds: GeoBounds,
    /** The mainland outline alone, which is what the map is fitted to. */
    val mainlandBounds: GeoBounds,
    /** Closed rings of the mainland and its coastal islands, largest first. */
    val mainlandRings: List<DoubleArray>,
    /** Hoàng Sa and Trường Sa, drawn in the map's inset boxes instead of in place. */
    val offshoreRings: List<DoubleArray>,
) {
    /**
     * Every ring, wherever it is drawn.
     *
     * Containment has to consider these together: a photo taken on Trường Sa belongs
     * to Khánh Hòa no matter which box the map happens to draw it in.
     */
    val rings: List<DoubleArray> get() = mainlandRings + offshoreRings

    /** How the traveller should see it written: "Tỉnh Bắc Ninh", "Thành phố Đà Nẵng". */
    val displayName: String
        get() = when (type) {
            ProvinceType.PROVINCE -> "Tỉnh $name"
            ProvinceType.CITY -> "Thành phố $name"
        }

    /** The archipelagos this province administers, if any. */
    val archipelagos: Set<Archipelago>
        get() = offshoreRings.mapNotNullTo(mutableSetOf()) { Archipelago.of(it) }

    /**
     * Whether a coordinate falls inside the province, by even-odd ray casting.
     *
     * Lives in the model because two callers need exactly this and nothing more: the
     * data layer resolving a GPS fix into a stamp, and the map resolving a finger tap
     * into a province. Anything policy-shaped — how far offshore a fix may drift
     * before it still counts — stays in the data layer on top of this.
     *
     * Rings are separate islands rather than holes, so being inside any one of them
     * means being inside the province.
     */
    fun contains(longitude: Double, latitude: Double): Boolean {
        if (longitude < bounds.minLongitude || longitude > bounds.maxLongitude ||
            latitude < bounds.minLatitude || latitude > bounds.maxLatitude
        ) {
            return false
        }
        if (mainlandRings.any { ringContains(it, longitude, latitude) }) return true
        return offshoreRings.any { ringContains(it, longitude, latitude) }
    }

    fun contains(point: GeoPoint): Boolean = contains(point.longitude, point.latitude)

    override fun equals(other: Any?): Boolean = this === other || (other is Province && other.id == id)

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Province($id, $name)"
}

/** Even-odd ray casting against one flat `[lon, lat, …]` ring. */
internal fun ringContains(ring: DoubleArray, longitude: Double, latitude: Double): Boolean {
    val n = ring.size / 2
    if (n < 3) return false
    var inside = false
    var j = n - 1
    for (i in 0 until n) {
        val xi = ring[i * 2]
        val yi = ring[i * 2 + 1]
        val xj = ring[j * 2]
        val yj = ring[j * 2 + 1]
        if ((yi > latitude) != (yj > latitude) &&
            longitude < (xj - xi) * (latitude - yi) / (yj - yi) + xi
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}
