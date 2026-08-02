package com.duylt.trave.vietlensai.data.remote.openmap

import com.duylt.trave.vietlensai.data.mapper.toDomain
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.DiscoveryCategory
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The open-data wire formats, pinned against responses captured from the live services.
 *
 * This is the part of the feature that cannot be checked by compiling it. Overpass
 * answers a refused request with **HTTP 200 carrying an HTML page**, and MediaWiki
 * answers under a title it resolved rather than the one asked for — both look like
 * ordinary success to a naive parser and both produce an empty map with no error.
 *
 * The fixtures are trimmed from real `overpass-api.de` and `vi.wikipedia.org` responses,
 * field names untouched.
 */
class OpenMapClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        isLenient = true
        coerceInputValues = true
    }

    /**
     * @param body answered to every request, including both halves of a place search.
     *   Overpass searches are split into a sights query and a food query, so a test that
     *   cares which is which should use [clientForSplit] instead.
     */
    private fun clientFor(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): Pair<HttpClient, MutableList<HttpRequestData>> = clientForSplit(status) { body }

    /** Answers each request from its own query text, so the two halves can differ. */
    private fun clientForSplit(
        status: HttpStatusCode = HttpStatusCode.OK,
        bodyFor: (query: String) -> String,
    ): Pair<HttpClient, MutableList<HttpRequestData>> {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            val query = when (val content = request.body) {
                is TextContent -> content.text
                is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
                else -> ""
            }
            respond(
                content = bodyFor(query),
                status = status,
                headers = headersOf("Content-Type", "application/json"),
            )
        }
        val client = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
        }
        return client to requests
    }

    /** The half of a split search that asks for restaurants. */
    private fun isFoodQuery(query: String) = query.contains("restaurant")

    // ------------------------------------------------------------------ Overpass

    @Test
    fun `a search is split into a sights query and a food query on different hosts`() = runTest {
        val (http, requests) = clientFor(OVERPASS_RESPONSE)

        OverpassClient(http).search(GeoPoint(21.0287, 105.8524), radiusMeters = 5_000)

        // One combined query does not finish: inside 5 km of central Hanoi the union
        // spans ~250 sights and ~1,700 places to eat, and the public instance gives up.
        assertEquals(2, requests.size, "the search must be split in two")
        val sights = requests.map { it.bodyText() }.first { !isFoodQuery(it) }
        val food = requests.map { it.bodyText() }.first { isFoodQuery(it) }

        // Ways matter — Hoàng thành Thăng Long and every park are areas — but relations
        // are the priciest thing Overpass can be asked for and there are ~5 of them here.
        assertTrue(sights.contains("nw%5B") || sights.contains("nw["), sights)
        assertTrue(!sights.contains("nwr"), "relations cost more than they return: $sights")
        assertTrue(sights.contains("out+center") || sights.contains("out center"), sights)
        assertTrue(sights.contains("around%3A5000") || sights.contains("around:5000"), sights)
        assertTrue(sights.contains("tourism") && sights.contains("historic"), sights)
        assertTrue(!sights.contains("restaurant"), "the sights half must not carry food")
        assertTrue(food.contains("restaurant"), "somewhere to eat is part of the screen")

        // The two halves must land on different hosts, or one instance is asked for both.
        assertEquals(
            2,
            requests.map { it.url.host }.toSet().size,
            "the halves must be put to different mirrors",
        )
        // Overpass and Wikimedia both throttle generic agents first.
        requests.forEach { request ->
            assertTrue(
                request.headers[HttpHeaders.UserAgent]?.contains("VietLensAI") == true,
                "a descriptive User-Agent is required by both services' usage policies",
            )
        }
    }

    @Test
    fun `a failed food half still yields a map of things to see`() = runTest {
        // A map of sights with no restaurants is a worse screen than a complete one and
        // a far better screen than an error.
        val (http, _) = clientForSplit { query ->
            if (isFoodQuery(query)) "<html>rate limited</html>" else OVERPASS_RESPONSE
        }

        val elements = assertNotNull(
            OverpassClient(http).search(GeoPoint(21.0287, 105.8524), radiusMeters = 5_000).getOrNull(),
        )
        assertEquals(4, elements.size)
    }

    @Test
    fun `an HTML refusal served as 200 is reported as throttling rather than as corruption`() =
        runTest {
            // Overpass really does answer a refused request this way. Parsed strictly it
            // is a deserialisation failure, which would reach the traveller as "the app
            // is broken" when the truth is "try again in a moment".
            val (http, _) = clientFor("<?xml version=\"1.0\"?><html><body>rate limited</body></html>")

            val result = OverpassClient(http).search(GeoPoint(21.0, 105.0), radiusMeters = 5_000)

            assertTrue(
                (result as AppResult.Failure).error is AppError.RateLimited,
                "expected RateLimited, was ${result.error}",
            )
        }

    @Test
    fun `a server-side timeout is a failure rather than an empty neighbourhood`() = runTest {
        // Overpass answers an over-budget query with 200 OK, an empty element list and a
        // remark. Read naively this is indistinguishable from open countryside, and the
        // screen told a traveller standing in central Hanoi there was nothing around them.
        val (http, _) = clientFor(
            """{"version":0.6,"elements":[],"remark":"runtime error: Query timed out in \"query\" at line 1"}""",
        )

        val result = OverpassClient(http).search(GeoPoint(21.0287, 105.8524), radiusMeters = 5_000)

        assertTrue(
            (result as AppResult.Failure).error is AppError.Timeout,
            "expected Timeout, was ${result.error}",
        )
    }

    @Test
    fun `the sights query filters by tag before position and asks for worship sites first`() =
        runTest {
            val (http, requests) = clientFor(OVERPASS_RESPONSE)

            OverpassClient(http).search(GeoPoint(21.0287, 105.8524), radiusMeters = 5_000)
            val sent = requests.map { it.bodyText() }.first { !isFoodQuery(it) }

            // Written the other way round — spatial filter first — this exact query times
            // out on the public instance, and a timed-out query answers 200 OK with an
            // empty list. The tag filter has to narrow the set before `around`.
            val worship = sent.indexOf("place_of_worship")
            val firstAround = sent.indexOf("around")
            assertTrue(worship in 0..<firstAround, "tag filters must precede (around:...): $sent")
            // Pagodas, temples and cathedrals in Vietnam carry no tourism or historic tag,
            // so without these two clauses the map of Hanoi has no Đền Ngọc Sơn on it.
            assertTrue(sent.contains("place_of_worship"), sent)
            assertTrue(sent.contains("landuse") && sent.contains("religious"), sent)
            // The union is truncated in clause order, so the important ones come first.
            assertTrue(
                sent.indexOf("place_of_worship") < sent.indexOf("leisure"),
                "worship sites must be requested before parks: $sent",
            )
        }

    @Test
    fun `elements map onto the domain with areas keeping their centre`() = runTest {
        // Only the sights half answers, so the fixture is not counted twice.
        val (http, _) = clientForSplit { query ->
            if (isFoodQuery(query)) EMPTY_RESPONSE else OVERPASS_RESPONSE
        }
        val origin = GeoPoint(21.0287, 105.8524)

        val elements = assertNotNull(
            OverpassClient(http).search(origin, radiusMeters = 5_000).getOrNull(),
        )
        assertEquals(4, elements.size)

        val places = elements.mapNotNull { it.toDomain(origin) }
        // The unnamed node is dropped: a place with no name cannot go on a list.
        assertEquals(3, places.size)

        val prison = assertNotNull(places.firstOrNull { it.name == "Nhà tù Hỏa Lò" })
        assertEquals(DiscoveryCategory.CULTURE, prison.category)
        assertEquals("Nhà tù Hỏa Lò", prison.wikipediaTitle) // `vi:` prefix stripped
        assertEquals("08:00-17:00", prison.openingHours)
        assertTrue(prison.mappingDetail >= 3, "was ${prison.mappingDetail}")

        // A way carries its position in `center`, not in lat/lon.
        val citadel = assertNotNull(places.firstOrNull { it.name == "Hoàng thành Thăng Long" })
        assertEquals(21.0384, citadel.location.latitude)
        assertEquals(DiscoveryCategory.LANDMARK, citadel.category)

        val pho = assertNotNull(places.firstOrNull { it.name == "Phở 10" })
        assertEquals(DiscoveryCategory.FOOD, pho.category)
        assertEquals("vietnamese", pho.cuisine)
    }

    @Test
    fun `zoo enclosures and flower beds are kept off a list of places to go`() {
        fun element(json: String) = OpenMapClientTest@this.json.decodeFromString(
            com.duylt.trave.vietlensai.data.remote.openmap.dto.OverpassElement.serializer(),
            json,
        )
        val origin = GeoPoint(21.0, 105.0)

        // Twenty-five animal pens inside Thủ Lệ zoo are tagged `tourism=attraction`,
        // so the screen was recommending a golden monkey as somewhere to visit.
        assertNull(
            element("""{"type":"node","id":1,"lat":21.0,"lon":105.0,
                "tags":{"name":"Khỉ vàng","tourism":"attraction","attraction":"animal"}}""")
                .toDomain(origin),
        )
        // Forty-eight planted roundabouts share `leisure=park` with real parks; only
        // the ones somebody described survive.
        assertNull(
            element("""{"type":"node","id":2,"lat":21.0,"lon":105.0,
                "tags":{"name":"Vườn hoa Hàng Trống","leisure":"park"}}""").toDomain(origin),
        )
        assertNotNull(
            element("""{"type":"node","id":3,"lat":21.0,"lon":105.0,
                "tags":{"name":"Vườn hoa Diên Hồng","leisure":"park","wikipedia":"vi:X"}}""")
                .toDomain(origin),
            "a park with an article is a park",
        )
        // Genuine entries in the data: a bell, and a distance marker.
        assertNull(
            element("""{"type":"node","id":4,"lat":21.0,"lon":105.0,
                "tags":{"name":"Lư","historic":"memorial"}}""").toDomain(origin),
        )
        assertNull(
            element("""{"type":"node","id":5,"lat":21.0,"lon":105.0,
                "tags":{"name":"0 km","tourism":"attraction"}}""").toDomain(origin),
        )
        // ...but the rule must not take a numbered phở shop with it.
        assertNotNull(
            element("""{"type":"node","id":6,"lat":21.0,"lon":105.0,
                "tags":{"name":"Phở 10","amenity":"restaurant"}}""").toDomain(origin),
            "a real restaurant whose name contains a number must survive",
        )
    }

    @Test
    fun `a pagoda tagged only as a place of worship still becomes a place`() {
        // Đền Ngọc Sơn carries no tourism or historic key at all.
        val temple = json.decodeFromString(
            com.duylt.trave.vietlensai.data.remote.openmap.dto.OverpassElement.serializer(),
            """{"type":"way","id":178995262,"center":{"lat":21.0307,"lon":105.8523},
                "tags":{"name":"Đền Ngọc Sơn","amenity":"place_of_worship","building":"temple"}}""",
        )
        val place = assertNotNull(temple.toDomain(GeoPoint(21.0287, 105.8524)))
        assertEquals(DiscoveryCategory.ARCHITECTURE, place.category)
    }

    @Test
    fun `a restaurant inside a museum reads as somewhere to eat`() {
        // Precedence is the mapper's declared order, not the tag map's iteration order.
        // Left to the map's order the same place changed category between refreshes.
        val element = json.decodeFromString(
            com.duylt.trave.vietlensai.data.remote.openmap.dto.OverpassElement.serializer(),
            """{"type":"node","id":1,"lat":21.0,"lon":105.0,
                "tags":{"name":"Café","amenity":"cafe","tourism":"museum","historic":"building"}}""",
        )
        assertEquals(DiscoveryCategory.FOOD, assertNotNull(element.toDomain(GeoPoint(21.0, 105.0))).category)
    }

    // ------------------------------------------------------------------ Wikipedia

    @Test
    fun `articles are re-keyed onto the titles asked for through redirects`() = runTest {
        val (http, requests) = clientFor(WIKI_RESPONSE)

        val result = WikipediaClient(http).articles(
            // The second is a redirect; MediaWiki answers under the target title.
            titles = listOf("Nhà tù Hỏa Lò", "Hoa Lo"),
            language = AppLanguage.VIETNAMESE,
        )

        assertEquals("https://vi.wikipedia.org/w/api.php", requests.single().url.toString().substringBefore('?'))

        val direct = assertNotNull(result["Nhà tù Hỏa Lò"], "direct title should resolve")
        assertEquals(2820, direct.readersLast60Days)
        assertTrue(direct.extract!!.startsWith("Nhà tù Hỏa Lò"))
        assertNotNull(direct.thumbnailUrl)

        // Without following the redirect chain this comes back null, and the place
        // silently loses its description and its ranking signal.
        val redirected = assertNotNull(result["Hoa Lo"], "redirected title should resolve too")
        assertEquals(direct.readersLast60Days, redirected.readersLast60Days)
    }

    @Test
    fun `titles are requested in batches of ten because pageviews degrade above that`() = runTest {
        val (http, requests) = clientFor(WIKI_RESPONSE)

        WikipediaClient(http).articles(
            titles = (1..25).map { "Place $it" },
            language = AppLanguage.VIETNAMESE,
        )

        // 25 titles must not become one request: `prop=pageviews` silently returns nulls
        // for everything past roughly the sixth page of a large batch, and a null there
        // is indistinguishable from an article nobody reads.
        assertEquals(3, requests.size, "expected 25 titles to be split into batches of ten")
        requests.forEach { request ->
            val titles = request.url.parameters["titles"].orEmpty().split("|")
            assertTrue(titles.size <= 10, "batch of ${titles.size} exceeds the safe size")
        }
    }

    @Test
    fun `a failed enrichment is empty rather than fatal`() = runTest {
        val (http, _) = clientFor("nonsense", status = HttpStatusCode.ServiceUnavailable)

        // The places are already on the map; losing their descriptions is a poorer
        // screen, not a broken one, so nothing here may throw.
        assertTrue(
            WikipediaClient(http).articles(listOf("Anything"), AppLanguage.ENGLISH).isEmpty(),
        )
        assertTrue(WikipediaClient(http).photosNear(GeoPoint(21.0, 105.0), 5_000).isEmpty())
    }

    @Test
    fun `commons photographs come back with the coordinates they were taken at`() = runTest {
        val (http, _) = clientFor(COMMONS_RESPONSE)

        val photos = WikipediaClient(http).photosNear(GeoPoint(21.0287, 105.8524), 10_000)

        assertEquals(2, photos.size)
        assertEquals(21.0308, photos.first().location.latitude)
        assertTrue(photos.first().url.contains("thumb"), "the scaled URL, not the original")
        // The entry with no imageinfo is dropped rather than yielding a blank tile.
        assertNull(photos.firstOrNull { it.url.isBlank() })
    }

    @Test
    fun `the article link points at the wiki the traveller is reading`() {
        val (http, _) = clientFor(WIKI_RESPONSE)
        assertEquals(
            "https://vi.wikipedia.org/wiki/Nhà_tù_Hỏa_Lò",
            WikipediaClient(http).articleUrl("Nhà tù Hỏa Lò", AppLanguage.VIETNAMESE),
        )
        assertEquals(
            "https://en.wikipedia.org/wiki/Hoa_Lo_Prison",
            WikipediaClient(http).articleUrl("Hoa Lo Prison", AppLanguage.ENGLISH),
        )
    }

    private fun HttpRequestData.bodyText(): String = when (val content = body) {
        is TextContent -> content.text
        is OutgoingContent.ByteArrayContent -> content.bytes().decodeToString()
        else -> ""
    }

    private companion object {
        /** A well-formed but empty answer, for muting one half of a split search. */
        val EMPTY_RESPONSE = """{"version":0.6,"elements":[]}"""

        /** Trimmed from a real Overpass response; note the way with only a `center`. */
        val OVERPASS_RESPONSE = """
        {
          "version": 0.6,
          "generator": "Overpass API 0.7.62.11",
          "elements": [
            {
              "type": "node", "id": 445345255, "lat": 21.0257, "lon": 105.8465,
              "tags": {
                "name": "Nhà tù Hỏa Lò", "tourism": "museum",
                "wikipedia": "vi:Nhà tù Hỏa Lò", "wikidata": "Q1188382",
                "opening_hours": "08:00-17:00", "website": "https://hoalo.vn"
              }
            },
            {
              "type": "way", "id": 26567890,
              "center": { "lat": 21.0384, "lon": 105.8375 },
              "tags": {
                "name": "Hoàng thành Thăng Long", "historic": "citadel",
                "wikipedia": "vi:Hoàng thành Thăng Long", "wikidata": "Q1132443"
              }
            },
            {
              "type": "node", "id": 991122, "lat": 21.0180, "lon": 105.8510,
              "tags": { "name": "Phở 10", "amenity": "restaurant", "cuisine": "vietnamese" }
            },
            {
              "type": "node", "id": 5, "lat": 21.03, "lon": 105.85,
              "tags": { "historic": "memorial" }
            }
          ]
        }
        """.trimIndent()

        /** Trimmed from vi.wikipedia.org, including a redirect entry. */
        val WIKI_RESPONSE = """
        {
          "query": {
            "redirects": [ { "from": "Hoa Lo", "to": "Nhà tù Hỏa Lò" } ],
            "pages": [
              {
                "pageid": 123,
                "title": "Nhà tù Hỏa Lò",
                "extract": "Nhà tù Hỏa Lò là một nhà tù do thực dân Pháp xây dựng ở Hà Nội.",
                "thumbnail": { "source": "https://upload.wikimedia.org/thumb/hoalo.jpg", "width": 800 },
                "pageviews": { "2026-06-01": 1200, "2026-06-02": 1620, "2026-06-03": null }
              }
            ]
          }
        }
        """.trimIndent()

        val COMMONS_RESPONSE = """
        {
          "query": {
            "pages": [
              {
                "title": "File:Cau The Huc.jpg",
                "coordinates": [ { "lat": 21.0308, "lon": 105.8527 } ],
                "imageinfo": [ { "thumburl": "https://upload.wikimedia.org/thumb/thehuc.jpg" } ]
              },
              {
                "title": "File:Turtle Tower.jpg",
                "coordinates": [ { "lat": 21.0279, "lon": 105.8535 } ],
                "imageinfo": [ { "thumburl": "https://upload.wikimedia.org/thumb/turtle.jpg" } ]
              },
              {
                "title": "File:No info.jpg",
                "coordinates": [ { "lat": 21.0, "lon": 105.0 } ]
              }
            ]
          }
        }
        """.trimIndent()
    }
}
