package com.evora.technologies.saola.domain.model

import kotlin.math.log10

/**
 * A real place near the traveller, assembled from OpenStreetMap and Wikipedia.
 *
 * Deliberately *not* a model's opinion about where someone should go next. The app used
 * to carry one of those, and everything on it — the address, the price, the walking time
 * — was written by the model and could be wrong in ways nobody could check. Everything
 * here was mapped by a person or written in an encyclopaedia, so a marker is where the
 * place actually is and the words under it are somebody's account of it rather than a
 * plausible-sounding invention.
 *
 * **There is no star rating, and there is not meant to be.** No open data source carries
 * Google's ratings, and rather than invent a number the screen ranks by two things that
 * can be checked: how many people read about the place ([monthlyReaders]) and how
 * carefully somebody mapped it ([mappingDetail]). A number nobody can verify would be
 * exactly the fault this feature exists to avoid.
 *
 * @param id stable across refreshes: the OSM element it came from, e.g. `node/445345255`.
 * @param monthlyReaders Wikipedia readers over the last 60 days, or null when the place
 *   has no article. Null and zero are different: one means "not written about", the
 *   other "written about and unread".
 * @param mappingDetail how many optional facts OSM holds about this place — opening
 *   hours, a website, a phone number, a heritage listing. A weak proxy for "somebody
 *   cared enough to fill this in", which turns out to track real prominence closely.
 * @param distanceMeters straight-line from where the search was centred.
 */
data class NearbyPlace(
    val id: String,
    val name: String,
    val category: DiscoveryCategory,
    val typeLabel: String?,
    val location: GeoPoint,
    val summary: String?,
    val photoUrl: String?,
    val distanceMeters: Int,
    val monthlyReaders: Int?,
    val mappingDetail: Int,
    val openingHours: String?,
    val website: String?,
    val phone: String?,
    val cuisine: String?,
    val wikipediaTitle: String?,
) {
    /** Somewhere to eat rather than somewhere to look at, which the map colours apart. */
    val isFood: Boolean get() = category == DiscoveryCategory.FOOD

    /**
     * How strongly this place should be pushed up the list.
     *
     * Four signals, weighted by how much each can be trusted:
     *
     * - **Readers**, logarithmically. The step from 100 readers to 1,000 says far more
     *   than the step from 10,000 to 10,900, and Lăng Chủ tịch Hồ Chí Minh outreading
     *   a neighbourhood park by two orders of magnitude is exactly the signal wanted.
     * - **Mapping detail**, which is the only signal a restaurant has at all.
     * - **Having an article and a photograph**, because a place the screen can actually
     *   *show* and *describe* is worth more to a traveller than a bare name on a pin.
     * - **Distance**, which only breaks ties. Inside a 5 km circle everything is
     *   reachable, so it must never bury a better place a street further on.
     */
    val prominence: Double
        get() {
            val readerScore = log10((monthlyReaders ?: 0).toDouble() + 1.0) * READER_WEIGHT
            val detailScore = mappingDetail * DETAIL_WEIGHT
            val describedBonus = if (summary != null) SUMMARY_BONUS else 0.0
            val picturedBonus = if (photoUrl != null) PHOTO_BONUS else 0.0
            // Somewhere to *see* outranks somewhere to eat at equal evidence. Without
            // this the screen tips over completely in a residential district, where
            // takeaway grills carry a house number and a cuisine tag — two points of
            // mapping detail each — while the local temple carries nothing at all. The
            // tab answers "where should I go", and forty restaurants is not an answer.
            val kindBonus = if (isFood) 0.0 else SIGHT_BONUS
            val distancePenalty = distanceMeters / METERS_PER_KM * DISTANCE_WEIGHT
            return readerScore + detailScore + describedBonus + picturedBonus +
                kindBonus - distancePenalty
        }

    private companion object {
        const val READER_WEIGHT = 3.0
        const val DETAIL_WEIGHT = 0.9
        const val SUMMARY_BONUS = 1.5
        const val PHOTO_BONUS = 0.8
        const val SIGHT_BONUS = 1.5
        const val DISTANCE_WEIGHT = 0.35
        const val METERS_PER_KM = 1000.0
    }
}

/**
 * The rest of what is known about a place, fetched only once its sheet is open.
 *
 * Thinner than it was when Google supplied it — there are no reviews to show, because
 * no open source has any. What is here instead is the encyclopaedia article, the
 * photographs other people have taken of the place, and the contact details whoever
 * mapped it wrote down.
 *
 * @param articleUrl the Wikipedia page, so a traveller who wants the whole story can
 *   go and read it rather than being given two sentences and no way onward.
 */
data class PlaceDetails(
    val id: String,
    val fullSummary: String?,
    val photoUrls: List<String>,
    val articleUrl: String?,
) {
    companion object {
        fun empty(id: String) = PlaceDetails(
            id = id,
            fullSummary = null,
            photoUrls = emptyList(),
            articleUrl = null,
        )
    }
}
