package com.duylt.trave.vietlensai.data.remote.openmap

import com.duylt.trave.vietlensai.data.remote.openmap.dto.WikiQueryResponse
import com.duylt.trave.vietlensai.data.util.log
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException

/**
 * Wikipedia and Wikimedia Commons — the source of *what a place is and looks like*.
 *
 * Enrichment only. It is never asked "what is near me", because the answer includes
 * cities, hospitals and embassies; it is asked "tell me about this exact article",
 * where the article title came from the `wikipedia` tag on an OSM element and is
 * therefore an assertion by whoever mapped the place.
 *
 * Nothing here throws. Enrichment is by definition optional — a place with no article
 * still belongs on the map with its name, its category and its distance — so every
 * method degrades to an empty result and logs, rather than failing a search that has
 * already succeeded.
 */
internal class WikipediaClient(
    private val httpClient: HttpClient,
) {

    /**
     * Article summary, lead image and readership for a batch of titles.
     *
     * @param titles as OSM spelled them. The response is re-keyed back onto these,
     *   following MediaWiki's normalisation and redirect chains, so the caller can look
     *   results up by the title it asked with.
     */
    suspend fun articles(
        titles: List<String>,
        language: AppLanguage,
    ): Map<String, WikiArticle> {
        if (titles.isEmpty()) return emptyMap()

        val collected = mutableMapOf<String, WikiArticle>()
        // Ten at a time. `prop=pageviews` quietly returns nulls for everything past
        // roughly the sixth page of a larger batch — measured, not documented — and a
        // null there is indistinguishable from an article nobody reads. Fifty titles
        // per request would have silently flattened the ranking signal to zero for
        // every place after the first handful.
        titles.distinct().chunked(PAGEVIEW_SAFE_BATCH).forEach { batch ->
            collected += fetchBatch(batch, language)
        }
        return collected
    }

    private suspend fun fetchBatch(
        titles: List<String>,
        language: AppLanguage,
    ): Map<String, WikiArticle> = try {
        val response: WikiQueryResponse = httpClient.get(apiUrl(language)) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            parameter("action", "query")
            parameter("format", "json")
            parameter("formatversion", "2")
            parameter("redirects", "1")
            parameter("titles", titles.joinToString("|"))
            parameter("prop", "extracts|pageimages|pageviews")
            parameter("exintro", "1")
            parameter("explaintext", "1")
            parameter("exsentences", SUMMARY_SENTENCES.toString())
            parameter("piprop", "thumbnail")
            parameter("pithumbsize", THUMBNAIL_PIXELS.toString())
            parameter("pvipdays", READERSHIP_DAYS.toString())
        }.body()

        val query = response.query ?: return emptyMap()

        // MediaWiki answers under the title it resolved to, not the one asked for, so
        // both chains have to be walked to get back to the caller's key. Normalisation
        // runs first (underscores, leading case), then redirects.
        val resolved = buildMap {
            query.normalized.forEach { put(it.from, it.to) }
            query.redirects.forEach { put(it.from, it.to) }
        }
        val pages = query.pages.filter { !it.missing }.associateBy { it.title }

        buildMap {
            titles.forEach { asked ->
                // Two hops at most: `A -> B` by normalisation, then `B -> C` by redirect.
                val once = resolved[asked] ?: asked
                val page = pages[resolved[once] ?: once] ?: pages[once] ?: return@forEach
                put(
                    asked,
                    WikiArticle(
                        title = page.title,
                        extract = page.extract?.takeIf { it.isNotBlank() },
                        thumbnailUrl = page.thumbnail?.source?.takeIf { it.isNotBlank() },
                        readersLast60Days = page.pageviews.values.filterNotNull().sum(),
                    ),
                )
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Deliberately swallowed. The places are already on the map; losing their
        // descriptions is a poorer screen, not a broken one.
        log.w(e) { "Could not enrich ${titles.size} places from Wikipedia" }
        emptyMap()
    }

    /**
     * Photographs taken near a coordinate, from Wikimedia Commons.
     *
     * One request covering the whole search area rather than one per place: Commons
     * geosearch returns everything within a radius, and matching by proximity
     * afterwards costs nothing. Called with a generous radius and a large limit for
     * that reason.
     *
     * Best-effort in the strongest sense — Commons has been observed returning 500
     * results to one call and a single result to the next identical one, so nothing on
     * the screen may depend on this having worked.
     */
    suspend fun photosNear(
        center: GeoPoint,
        radiusMeters: Int,
        limit: Int = MAX_PHOTOS,
    ): List<GeoPhoto> = try {
        val response: WikiQueryResponse = httpClient.get(COMMONS_API) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            parameter("action", "query")
            parameter("format", "json")
            parameter("formatversion", "2")
            parameter("generator", "geosearch")
            parameter("ggscoord", "${center.latitude}|${center.longitude}")
            parameter("ggsradius", radiusMeters.coerceAtMost(MAX_GEOSEARCH_RADIUS).toString())
            parameter("ggslimit", limit.toString())
            // Namespace 6 is File:. Without it the generator also returns category and
            // gallery pages, which have coordinates but no image behind them.
            parameter("ggsnamespace", "6")
            parameter("prop", "imageinfo|coordinates")
            parameter("iiprop", "url")
            parameter("iiurlwidth", THUMBNAIL_PIXELS.toString())
        }.body()

        response.query?.pages.orEmpty().mapNotNull { page ->
            val coordinate = page.coordinates.firstOrNull() ?: return@mapNotNull null
            val url = page.imageinfo.firstOrNull()?.thumbUrl ?: return@mapNotNull null
            GeoPhoto(
                location = GeoPoint(coordinate.lat, coordinate.lon),
                url = url,
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.w(e) { "Could not fetch Commons photographs" }
        emptyList()
    }

    /** The public page for an article, for the "read the whole thing" link. */
    fun articleUrl(title: String, language: AppLanguage): String =
        "https://${language.code}.wikipedia.org/wiki/${title.replace(' ', '_')}"

    private fun apiUrl(language: AppLanguage) =
        "https://${language.code}.wikipedia.org/w/api.php"

    private companion object {
        const val COMMONS_API = "https://commons.wikimedia.org/w/api.php"

        /**
         * Required by Wikimedia's user-agent policy, which asks for an identifiable
         * application rather than a generic library string. Requests without one are
         * liable to be refused outright.
         */
        const val USER_AGENT = "VietLensAI/1.0 (https://github.com/duylt/vietlens; hackathon build)"

        /** See [articles] — this is a correctness limit, not a performance one. */
        const val PAGEVIEW_SAFE_BATCH = 10

        const val SUMMARY_SENTENCES = 3
        const val THUMBNAIL_PIXELS = 800
        const val READERSHIP_DAYS = 60
        const val MAX_PHOTOS = 500
        const val MAX_GEOSEARCH_RADIUS = 10_000
    }
}

/** What Wikipedia knows about one place. */
internal data class WikiArticle(
    val title: String,
    val extract: String?,
    val thumbnailUrl: String?,
    val readersLast60Days: Int,
)

/** A Commons photograph and where it was taken. */
internal data class GeoPhoto(
    val location: GeoPoint,
    val url: String,
)
