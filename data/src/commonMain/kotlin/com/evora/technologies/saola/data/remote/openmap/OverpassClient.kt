package com.evora.technologies.saola.data.remote.openmap

import com.evora.technologies.saola.data.remote.openmap.dto.OverpassElement
import com.evora.technologies.saola.data.remote.openmap.dto.OverpassResponse
import com.evora.technologies.saola.data.util.log
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import com.evora.technologies.saola.domain.util.SAOLA_USER_AGENT
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.io.IOException

/**
 * OpenStreetMap, queried through Overpass — the source of *what is a place*.
 *
 * OSM decides the place list on its own, and that is a deliberate division of labour.
 * An earlier design let Wikipedia contribute places too, and it put "Hà Nội" and
 * "Liên bang Đông Dương" at the top of a list of things to go and see: Wikipedia
 * geotags articles, including articles about cities, hospitals, embassies and defunct
 * political entities, and none of those is a destination. OSM has actual POI
 * classification — `tourism=museum`, `historic=monument`, `amenity=restaurant` — so it
 * can answer "is this somewhere you can go" in a way Wikipedia cannot.
 *
 * Wikipedia is then used only where OSM explicitly points at it through a `wikipedia`
 * tag. That link is asserted by whoever mapped the place, so it cannot produce the
 * false matches that looking articles up by name does — a zoo enclosure named
 * "Báo lửa" matches the encyclopaedia article about the *leopard species*, and a bell
 * named "Chuông" matches the article about bells.
 *
 * **Overpass is a donated public service.** It throttles aggressively and answers a
 * refused request with an HTML page rather than JSON, so [search] retries with a delay
 * and reports a refusal as a domain error instead of a parse failure.
 */
internal class OverpassClient(
    private val httpClient: HttpClient,
) {

    /**
     * Every named POI worth considering inside [radiusMeters] of [center].
     *
     * **Two queries, run concurrently against different mirrors.** One combined query
     * was the first design and it does not finish: inside 5 km of central Hanoi the
     * union spans some 250 sights and 1,700 places to eat, and asking a donated public
     * instance for all of it in one statement times out server-side. Split, each half
     * is cheap, the two run in parallel so the wall-clock is the slower of them rather
     * than the sum, and they are put to *different* hosts so one instance is never
     * asked for both.
     *
     * Either half failing is survivable. A map of sights with no restaurants on it is a
     * worse screen than a complete one and a far better screen than an error.
     */
    suspend fun search(
        center: GeoPoint,
        radiusMeters: Int,
    ): AppResult<List<OverpassElement>> = coroutineScope {
        val sights = async {
            runQuery(
                query = buildQuery(center, radiusMeters, SIGHT_CLAUSES, MAX_SIGHT_ELEMENTS),
                startAt = 0,
            )
        }
        // Offset by one so the two halves start on different hosts.
        val food = async {
            runQuery(
                query = buildQuery(center, radiusMeters, FOOD_CLAUSES, MAX_FOOD_ELEMENTS),
                startAt = 1,
            )
        }

        val sightResult = sights.await()
        val foodResult = food.await()
        val found = listOfNotNull(sightResult.getOrNull(), foodResult.getOrNull()).flatten()

        when {
            // Sights are the point of the screen, so their failure is reported even when
            // the restaurants arrived; the reverse is merely logged.
            sightResult is AppResult.Failure && found.isEmpty() -> sightResult
            sightResult is AppResult.Failure -> {
                log.w { "Explore has food but no sights: ${sightResult.error}" }
                AppResult.Success(found)
            }
            else -> {
                (foodResult as? AppResult.Failure)?.let { log.w { "No food places: ${it.error}" } }
                AppResult.Success(found)
            }
        }
    }

    /**
     * Puts one query to each mirror in turn until one answers.
     *
     * Round-robin rather than retrying a single host. Overpass throttles by IP *per
     * instance*, so a second attempt against the instance that just refused is the one
     * request guaranteed not to work — while the same query put to a different mirror
     * usually answers immediately.
     *
     * @param startAt which mirror to begin from, so concurrent callers spread their load
     *   instead of queueing behind each other on the same host.
     */
    private suspend fun runQuery(query: String, startAt: Int): AppResult<List<OverpassElement>> {
        var lastFailure: AppResult.Failure? = null
        ENDPOINTS.indices.forEach { offset ->
            val endpoint = ENDPOINTS[(startAt + offset) % ENDPOINTS.size]
            when (val result = attempt(endpoint, query)) {
                is AppResult.Success -> return result
                is AppResult.Failure -> {
                    val worthMoving = result.error is AppError.RateLimited ||
                        result.error is AppError.Timeout ||
                        result.error is AppError.Api
                    if (!worthMoving) return result
                    log.w { "$endpoint refused (${result.error}); trying the next mirror" }
                    lastFailure = result
                    delay(RETRY_DELAY_MILLIS)
                }
            }
        }
        return lastFailure ?: AppResult.Failure(AppError.Timeout)
    }

    private suspend fun attempt(
        endpoint: String,
        query: String,
    ): AppResult<List<OverpassElement>> = try {
        // Form-encoded rather than a raw body: Overpass expects `data=<query>`, and the
        // query is full of characters that must not travel unescaped.
        val response: HttpResponse = httpClient.submitForm(
            url = endpoint,
            formParameters = parameters { append("data", query) },
        ) {
            // Wikimedia and Overpass both ask for a descriptive agent identifying the
            // application. Ktor's default says only "Ktor client", which is exactly the
            // sort of caller these services throttle first.
            header(HttpHeaders.UserAgent, SAOLA_USER_AGENT)
            // Far shorter than the client-wide 90 s, which is sized for a Gemini vision
            // call. Overpass either answers in a few seconds or is refusing us, and a
            // traveller watching a spinner does not care which — three attempts at the
            // shared timeout is four and a half minutes of nothing happening.
            timeout { requestTimeoutMillis = ATTEMPT_TIMEOUT_MILLIS }
        }

        when {
            response.status.value in SUCCESS_RANGE -> decode(response)
            // 429 is the documented throttle; 504 is what a slot-starved instance
            // returns once it gives up waiting for one.
            response.status.value == HttpStatusCode.TooManyRequests.value ->
                AppResult.Failure(AppError.RateLimited(null))
            response.status.value == HttpStatusCode.GatewayTimeout.value ->
                AppResult.Failure(AppError.Timeout)
            else -> {
                log.w { "Overpass answered ${response.status.value}" }
                AppResult.Failure(AppError.Api(response.status.value, null))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: UnresolvedAddressException) {
        AppResult.Failure(AppError.NoConnection)
    } catch (e: HttpRequestTimeoutException) {
        AppResult.Failure(AppError.Timeout)
    } catch (e: IOException) {
        log.w(e) { "Network failure calling Overpass" }
        AppResult.Failure(AppError.NoConnection)
    } catch (e: Exception) {
        log.e(e) { "Unexpected failure calling Overpass" }
        AppResult.Failure(AppError.Unexpected(e.message))
    }

    /**
     * Reads the body, treating "HTML instead of JSON" as a throttle rather than a bug.
     *
     * A refused Overpass request comes back as **200 OK** carrying an HTML error page.
     * Parsed strictly that is a deserialisation failure, which would surface to the
     * traveller as "the app is broken" when the truth is "try again in a moment".
     */
    private suspend fun decode(response: HttpResponse): AppResult<List<OverpassElement>> = try {
        val body = response.body<OverpassResponse>()
        val remark = body.remark
        when {
            // "runtime error: Query timed out" / "Query ran out of memory". Retryable,
            // and emphatically not the same thing as an empty neighbourhood.
            remark != null && body.elements.isEmpty() -> {
                log.w { "Overpass gave up: $remark" }
                AppResult.Failure(AppError.Timeout)
            }
            // A remark alongside results usually means the answer was truncated, which
            // is survivable — the screen only ever shows a few dozen of them anyway.
            remark != null -> {
                log.w { "Overpass returned ${body.elements.size} elements with: $remark" }
                AppResult.Success(body.elements)
            }
            else -> AppResult.Success(body.elements)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val body = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
        if (body.trimStart().startsWith("<")) {
            log.w { "Overpass returned an HTML refusal rather than JSON" }
            AppResult.Failure(AppError.RateLimited(null))
        } else {
            log.e(e) { "Overpass returned unparseable JSON" }
            AppResult.Failure(AppError.Malformed(e.message))
        }
    }

    /**
     * The Overpass QL for one family of places.
     *
     * `nw` rather than `nwr`: ways matter — Hoàng thành Thăng Long, most museums and
     * every park are mapped as areas, so a node-only query loses them — but relations
     * are the most expensive thing Overpass can be asked for, and there are almost none
     * of them here. Measured within 5 km of Hoàn Kiếm: one tourism relation and four
     * historic ones, against roughly 250 nodes and ways.
     *
     * `["name"]` is required rather than preferred — an unnamed POI cannot be put on a
     * list, and OSM is full of unnamed benches, plaques and drinking fountains.
     *
     * Tag filters come BEFORE the spatial one, which is not a style choice. Written the
     * other way round — `nw(around:…)["name"]["amenity"="place_of_worship"]` — this
     * query times out on the public instance, and a timed-out Overpass query answers
     * 200 OK with an empty element list. The screen read that as "there is nothing
     * around you" and said so, confidently, in the middle of Hanoi.
     */
    private fun buildQuery(
        center: GeoPoint,
        radiusMeters: Int,
        clauses: List<String>,
        limit: Int,
    ): String {
        val around = "(around:$radiusMeters,${center.latitude},${center.longitude})"
        val body = clauses.joinToString("") { clause -> """nw$clause["name"]$around;""" }
        return "[out:json][timeout:$SERVER_TIMEOUT_SECONDS];($body);out center tags $limit;"
    }

    private companion object {
        /**
         * Public Overpass instances with **worldwide** coverage, tried in order.
         *
         * All donated infrastructure with independent per-IP allowances, which is the
         * point: one refusing says nothing about the next. A burst of development
         * traffic exhausted the main instance's allowance and the screen sat on a
         * spinner while a mirror answered the same query in a second.
         *
         * The word "worldwide" is doing real work here. Several widely-listed mirrors
         * are **regional extracts**, and they do not announce it — `overpass.osm.ch`
         * answers a Hanoi query with a cheerful `200 OK` and zero elements, because it
         * holds Switzerland and nothing else. That is indistinguishable from an empty
         * neighbourhood, so it briefly shipped here as a fallback that silently emptied
         * the map. Any mirror added to this list must be checked against a query for
         * somewhere in Vietnam, not merely for being reachable.
         *
         * `overpass.private.coffee` and `overpass.osm.jp` did not resolve at all.
         */
        val ENDPOINTS = listOf(
            // Fastest of the three when measured from Vietnam: it answered the food half
            // in 2 s while the main instance was still queueing. `overpass-api.de` is the
            // best-known host and therefore the most heavily loaded one, which is a
            // reason to try it second rather than first.
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
        )

        /**
         * Things to see, in the order they are asked for.
         *
         * Places of worship come first because without them the query misses most of
         * what a traveller in Vietnam came for. Measured inside 5 km of Hoàn Kiếm:
         * Đền Ngọc Sơn is tagged `place_of_worship` + `building=temple` and nothing
         * else, Nhà thờ Lớn is `place_of_worship` alone, Văn Miếu is `landuse=religious`
         * with `religion=confucian` — and 222 named places of worship are invisible to a
         * tourism/historic filter. A "where to go" screen in Hanoi that lists
         * traffic-island flower beds and omits every pagoda is indefensible.
         *
         * The union is truncated in clause order, so the most important come first.
         */
        val SIGHT_CLAUSES = listOf(
            """["amenity"="place_of_worship"]""",
            """["landuse"="religious"]""",
            """["tourism"~"attraction|museum|artwork|viewpoint|gallery|theme_park|zoo|aquarium"]""",
            """["historic"]""",
            """["leisure"~"park|garden"]""",
        )

        val FOOD_CLAUSES = listOf("""["amenity"~"restaurant|cafe|bakery"]""")

        /**
         * Overpass's own budget for the query, and deliberately **below**
         * [ATTEMPT_TIMEOUT_MILLIS].
         *
         * Whichever clock runs out first decides what the app is told, so the server's
         * has to be the shorter one. It gives up politely — 200 OK with a `remark` this
         * client reads and reports as a retryable timeout — whereas the client giving up
         * first drops the connection, which is indistinguishable from being offline and
         * sends the traveller to check a network that was never the problem.
         *
         * Forty seconds rather than the ten a fast answer needs. A public instance under
         * load spends most of that queueing rather than working — the same query that
         * returns in two seconds on an idle mirror was measured at 25 s on a busy one —
         * and the screen can afford the wait because the map is already drawn from the
         * location fix by the time the search starts. What it cannot afford is giving up
         * on an answer that was thirty seconds away.
         */
        const val SERVER_TIMEOUT_SECONDS = 40

        /**
         * Enough to rank well from without asking a donated service for a whole city.
         *
         * The screen shows at most a couple of dozen; the rest is the pool that ranking
         * chooses from. The food budget is the smaller of the two because the screen
         * deliberately shows far fewer restaurants than sights — and because in a
         * Vietnamese city centre the food half of the map is by far the denser one.
         */
        const val MAX_SIGHT_ELEMENTS = 300
        const val MAX_FOOD_ELEMENTS = 150

        const val RETRY_DELAY_MILLIS = 1_500L

        /**
         * Per attempt, and deliberately a little longer than [SERVER_TIMEOUT_SECONDS].
         *
         * The server has to be the one that gives up first — it does so politely, with a
         * `remark` this client reads as a retryable timeout, whereas the client giving up
         * drops the connection and that is indistinguishable from being offline.
         *
         * Worth knowing when developing against Overpass: it rate-limits by IP address,
         * so a burst of testing from one machine exhausts the allowance and every
         * request until it recovers comes back refused. That is a development hazard
         * rather than a production one — the app makes two requests per search.
         */
        const val ATTEMPT_TIMEOUT_MILLIS = 45_000L
        val SUCCESS_RANGE = 200..299
    }
}
