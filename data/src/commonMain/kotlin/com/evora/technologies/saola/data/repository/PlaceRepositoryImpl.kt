package com.evora.technologies.saola.data.repository

import com.evora.technologies.saola.data.mapper.toDomain
import com.evora.technologies.saola.data.remote.openmap.GeoPhoto
import com.evora.technologies.saola.data.remote.openmap.OverpassClient
import com.evora.technologies.saola.data.remote.openmap.WikipediaClient
import com.evora.technologies.saola.data.util.log
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.NearbyPlace
import com.evora.technologies.saola.domain.model.PlaceDetails
import com.evora.technologies.saola.domain.repository.PlaceRepository
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The Explore map's data, assembled from three open sources and ranked here.
 *
 * Each source answers exactly one question, and none of them is asked anything else:
 *
 * | Source | Question |
 * |---|---|
 * | OpenStreetMap (Overpass) | what *is* a place, and where |
 * | Wikipedia | what is this place, and how many people read about it |
 * | Wikimedia Commons | what does it look like |
 *
 * The division matters. An earlier design let Wikipedia contribute places as well as
 * describe them and the top of the list became "Hà Nội" and "Liên bang Đông Dương" —
 * geotagged articles that are not destinations. OSM classifies POIs properly, so it
 * alone decides what appears; Wikipedia is consulted only where OSM's own `wikipedia`
 * tag points at an article, which makes false matches impossible.
 *
 * **Ranking is this class's job**, because none of the sources ranks anything: OSM has
 * no notion of importance and Wikipedia's geosearch is ordered by distance alone. See
 * [NearbyPlace.prominence].
 *
 * **The cache is in memory and short.** Every other repository here is Room-backed
 * because the traveller's own history has to survive a cold launch; this one holds
 * public data about wherever the phone happened to be, and restoring it from disk would
 * open the screen on last week's city. It would also have meant a schema migration, and
 * this project's `fallbackToDestructiveMigration` would drop the whole journal to cache
 * a list of cafés.
 */
internal class PlaceRepositoryImpl(
    private val overpass: OverpassClient,
    private val wikipedia: WikipediaClient,
    private val ioDispatcher: CoroutineDispatcher,
) : PlaceRepository {

    /** Guards [cached] so two screens resuming at once cannot fire two searches. */
    private val searchLock = Mutex()
    private var cached: CachedSearch? = null

    private val detailsLock = Mutex()
    private val detailsCache = mutableMapOf<DetailsKey, PlaceDetails>()

    override suspend fun nearbyPlaces(
        center: GeoPoint,
        radiusMeters: Int,
        language: AppLanguage,
        forceRefresh: Boolean,
    ): AppResult<List<NearbyPlace>> = withContext(ioDispatcher) {
        runCatchingUnexpected(what = "search for places around $center") {
            searchLock.withLock {
                if (!forceRefresh) {
                    cached?.takeIf { it.covers(center, radiusMeters, language) }
                        ?.let { return@withLock AppResult.Success(it.places) }
                }

                // OSM first and alone: without places there is nothing to enrich, and a
                // failure here is the only one that can fail the whole search.
                val elements = when (val result = overpass.search(center, radiusMeters)) {
                    is AppResult.Failure -> return@withLock result
                    is AppResult.Success -> result.data
                }

                val places = elements
                    .mapNotNull { it.toDomain(origin = center, language = language) }
                    .let(::deduplicate)

                // Both enrichments run concurrently and neither can fail the search: a
                // place with no article and no photograph is still a real place with a
                // real position, and the traveller is better served by a plainer map than
                // by an error where a map should be.
                val enriched = coroutineScope {
                    val articles = async {
                        wikipedia.articles(
                            titles = places.mapNotNull { it.wikipediaTitle },
                            language = language,
                        )
                    }
                    val photographs = async {
                        wikipedia.photosNear(center = center, radiusMeters = radiusMeters)
                    }
                    val byTitle = articles.await()
                    val nearbyPhotos = photographs.await()

                    places.map { place ->
                        val article = place.wikipediaTitle?.let(byTitle::get)
                        place.copy(
                            summary = article?.extract ?: place.summary,
                            monthlyReaders = article?.readersLast60Days,
                            photoUrl = article?.thumbnailUrl ?: nearbyPhotos.nearestTo(place),
                        )
                    }
                }

                val ranked = rankWithBalance(enriched)
                log.d {
                    "Explore: ${elements.size} OSM elements -> ${ranked.size} places, " +
                        "${ranked.count { it.summary != null }} described, " +
                        "${ranked.count { it.photoUrl != null }} pictured"
                }

                cached = CachedSearch(center, radiusMeters, language, ranked, Clock.System.now())
                AppResult.Success(ranked)
            }
        }
    }

    override suspend fun placeDetails(
        place: NearbyPlace,
        language: AppLanguage,
    ): AppResult<PlaceDetails> = withContext(ioDispatcher) {
        runCatchingUnexpected(what = "open the sheet for ${place.name}") {
            // Keyed on the language as well as the place. Keyed on the id alone, switching
            // the app to English and reopening a sheet served the Vietnamese article back
            // out of the cache — the one thing the language switch was supposed to change.
            val key = DetailsKey(place.id, language)
            detailsLock.withLock { detailsCache[key] }
                ?.let { return@runCatchingUnexpected AppResult.Success(it) }

            val title = place.wikipediaTitle
            val article = title?.let { wikipedia.articles(listOf(it), language)[it] }

            // Photographs of *this* place: a tight radius, unlike the search-wide sweep,
            // because a 150 m circle around a temple is that temple and a 5 km one is the
            // whole district.
            val photos = wikipedia
                .photosNear(center = place.location, radiusMeters = DETAIL_PHOTO_RADIUS_METERS)
                .filter {
                    it.location.distanceMetersTo(place.location) <= DETAIL_PHOTO_RADIUS_METERS
                }
                .sortedBy { it.location.distanceMetersTo(place.location) }
                .map { it.url }

            val details = PlaceDetails(
                id = place.id,
                fullSummary = article?.extract ?: place.summary,
                // The lead image is already the sheet's header, so repeating it as the
                // first tile of the strip would look like the same photograph twice.
                photoUrls = (listOfNotNull(article?.thumbnailUrl) + photos)
                    .distinct()
                    .filterNot { it == place.photoUrl }
                    .take(MAX_DETAIL_PHOTOS),
                articleUrl = article?.let { wikipedia.articleUrl(it.title, language) },
            )

            detailsLock.withLock {
                // Bounded rather than left to grow for the life of the process. A traveller
                // walking a city can open a great many sheets, and each one holds a handful
                // of image URLs plus an article extract.
                if (detailsCache.size >= MAX_CACHED_DETAILS) detailsCache.clear()
                detailsCache[key] = details
            }
            AppResult.Success(details)
        }
    }

    /**
     * Turns an unexpected throw into the failure this interface promises to return.
     *
     * Both clients this class holds already fold their own errors into [AppResult], so what
     * is left to catch is the mapping and ranking between them — and there is a real case
     * there, not a hypothetical one. `OverpassElement.toDomain` ends with
     * `origin.distanceMetersTo(location).roundToInt()`, and `roundToInt` throws on NaN. OSM
     * coordinates arrive as plain JSON doubles out of a public, crowd-edited database, so a
     * single element with a nonsense position is enough to take the whole Explore screen
     * down with an exception where several hundred good places should have been.
     *
     * [AppError.Unexpected] rather than [AppError.Storage]: nothing here touches the
     * database, and telling the traveller their storage failed would send them to clear an
     * app cache that was never involved.
     */
    private inline fun <T> runCatchingUnexpected(
        what: String,
        block: () -> AppResult<T>,
    ): AppResult<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.e(e) { "Could not $what" }
        AppResult.Failure(AppError.Unexpected(e.message))
    }

    /**
     * Ranks by prominence, but reserves most of the map for things to see.
     *
     * A single sorted list is the obvious implementation and it produces a bad screen.
     * Measured in a residential district of Hanoi, an unfiltered ranking came back as
     * forty restaurants and not one landmark: OSM's food coverage is dense and
     * consistently tagged everywhere, while its attraction coverage is thin outside
     * historic centres. The weighting in [NearbyPlace.prominence] tilts the ordering,
     * and this caps the outcome — together they mean the tab always answers "where
     * should I go" before it answers "where could I eat".
     *
     * Food is not dropped when there are few sights; the cap is a ceiling on the food
     * share, not a floor under the sights. Standing somewhere with nothing to visit,
     * the traveller still gets somewhere to eat rather than an empty map.
     */
    private fun rankWithBalance(places: List<NearbyPlace>): List<NearbyPlace> {
        val (food, sights) = places.sortedByDescending { it.prominence }.partition { it.isFood }
        val keptSights = sights.take(MAX_PLACES)
        val foodBudget = (MAX_PLACES - keptSights.size).coerceAtMost(MAX_FOOD_PLACES)
        return (keptSights + food.take(foodBudget)).sortedByDescending { it.prominence }
    }

    /**
     * Drops the second copy of a place mapped more than once.
     *
     * By name alone, not by name *and* proximity. Proximity handles the obvious case —
     * a museum mapped both as a building and as a point inside it — and misses the one
     * that actually spoils the screen: measured within 5 km of Hoàn Kiếm, OSM holds 35
     * branches of Highlands Coffee, 16 of Cộng Cà Phê and 13 of Pizza Hut. A list of
     * six identical "Highlands Coffee" rows is worse than useless, and the traveller
     * only ever wants the nearest one anyway.
     *
     * The cost is that two genuinely different places sharing a name collapse into one.
     * In this data that means the fifteen ward war memorials all called
     * "Đài tưởng niệm liệt sĩ" — which is a saving, not a loss.
     *
     * **Keyed on `mappedName`, never on `name`.** The displayed name follows the traveller's
     * language and only about half of these places have been translated, so within one search
     * some names are English and the rest Vietnamese — two branches of one chain would stop
     * collapsing as soon as a mapper gave one of them a `name:en`. What is being deduplicated
     * is the OSM data, not the screen.
     */
    private fun deduplicate(places: List<NearbyPlace>): List<NearbyPlace> = places
        // Best-mapped first, nearest to break the tie, so the copy that survives is the
        // one carrying the Wikipedia link and the opening hours.
        .sortedWith(compareByDescending<NearbyPlace> { it.mappingDetail }.thenBy { it.distanceMeters })
        .distinctBy { it.mappedName.lowercase().trim() }

    /** The closest Commons photograph, if one was taken near enough to be of this place. */
    private fun List<GeoPhoto>.nearestTo(place: NearbyPlace): String? =
        minByOrNull { it.location.distanceMetersTo(place.location) }
            ?.takeIf { it.location.distanceMetersTo(place.location) <= PHOTO_MATCH_RADIUS_METERS }
            ?.url

    private data class DetailsKey(val placeId: String, val language: AppLanguage)

    /**
     * One search's worth of results, and the circumstances that make it still true.
     *
     * @param fetchedAt compared against the clock rather than counted down, so time
     *   passing while the app is backgrounded expires the entry the same as time
     *   passing in front of the traveller.
     */
    private data class CachedSearch(
        val center: GeoPoint,
        val radiusMeters: Int,
        val language: AppLanguage,
        val places: List<NearbyPlace>,
        val fetchedAt: Instant,
    ) {
        fun covers(other: GeoPoint, radius: Int, otherLanguage: AppLanguage): Boolean =
            radius == radiusMeters &&
                otherLanguage == language &&
                Clock.System.now() - fetchedAt < CACHE_TTL &&
                center.distanceMetersTo(other) < CACHE_RADIUS_METERS
    }

    private companion object {
        /** More than this and the markers merge into a single blot at city zoom. */
        const val MAX_PLACES = 40

        /** The most of [MAX_PLACES] that may be somewhere to eat. See [rankWithBalance]. */
        const val MAX_FOOD_PLACES = 12

        /**
         * How close a Commons photograph has to be to be treated as *of* a place.
         *
         * Generous on purpose. Photographs are geotagged where the camera stood, not
         * where the subject is, so a picture of a temple gate is routinely a hundred
         * metres from the temple's own node.
         */
        const val PHOTO_MATCH_RADIUS_METERS = 150.0

        const val DETAIL_PHOTO_RADIUS_METERS = 150
        const val MAX_DETAIL_PHOTOS = 6
        const val MAX_CACHED_DETAILS = 60

        val CACHE_TTL = 15.minutes

        /** Roughly a long block: far enough to be worth reusing, near enough to be true. */
        const val CACHE_RADIUS_METERS = 300.0
    }
}
