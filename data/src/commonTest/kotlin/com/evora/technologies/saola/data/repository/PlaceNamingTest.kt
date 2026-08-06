package com.evora.technologies.saola.data.repository

import com.evora.technologies.saola.data.remote.openmap.OverpassClient
import com.evora.technologies.saola.data.remote.openmap.WikipediaClient
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.NearbyPlace
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Which name a place is shown under, driven through the whole search rather than the mapper.
 *
 * OpenStreetMap holds a translated name for only about half of what is around Hoàn Kiếm —
 * 47% of the attractions carry `name:en` and effectively none carries `name:ja`, `name:ko`
 * or `name:th` — so within a single list some places are named in the traveller's language
 * and the rest are not. That mixture is the whole difficulty, and none of it is visible from
 * `toDomain` alone: the case that bites is **deduplication**, which runs after the mapper and
 * compares places against each other.
 *
 * The repository is exercised rather than faked because that comparison is the subject. The
 * two clients are real, over a `MockEngine` that answers Overpass from a fixture and hands
 * Wikipedia and Commons an empty page list — enrichment cannot fail a search and has nothing
 * to say about names.
 */
class PlaceNamingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        isLenient = true
        coerceInputValues = true
    }

    /** Where every fixture below is placed, and where the search is centred. */
    private val origin = GeoPoint(21.0287, 105.8524)

    /**
     * The places [elements] describes, as the screen would receive them in [language].
     *
     * Only the sights half of the split search is answered; the food half is given an empty
     * document so a fixture is not counted twice. A fixture of restaurants therefore has to
     * be declared as the sights half's answer, which is what [foodElements] is for.
     */
    private suspend fun search(
        language: AppLanguage,
        elements: String,
        foodElements: String = EMPTY_ELEMENTS,
    ): List<NearbyPlace> {
        val engine = MockEngine { request ->
            val host = request.url.host
            val body = when {
                // Enrichment. Neither can fail a search, and an empty answer is the ordinary
                // case anyway: most places have no article and no photograph.
                host.contains("wikipedia") || host.contains("wikimedia") -> EMPTY_PAGES
                isFoodQuery(request.bodyText()) -> foodElements
                else -> elements
            }
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json"),
            )
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
        }
        val repository = PlaceRepositoryImpl(
            overpass = OverpassClient(http),
            wikipedia = WikipediaClient(http),
            // Unconfined rather than a test dispatcher: the repository's own `withContext`
            // is the only thing this hands work to, and there is no timing to drive.
            ioDispatcher = Dispatchers.Unconfined,
        )
        return assertNotNull(
            repository.nearbyPlaces(origin, radiusMeters = 5_000, language = language).getOrNull(),
            "the search must succeed for the naming to be the thing under test",
        )
    }

    @Test
    fun `an English traveller reads the English name and keeps the local one underneath`() =
        runTest {
            val places = search(AppLanguage.ENGLISH, TRANSLATED_PRISON)

            val prison = places.single()
            assertEquals("Hoa Lo Prison", prison.name)
            // The name on the sign. Without it the sheet gives a traveller no way to match
            // what they are reading against the building in front of them.
            assertEquals("Nhà tù Hỏa Lò", prison.localName)
            assertEquals("Nhà tù Hỏa Lò", prison.mappedName)
        }

    @Test
    fun `a place nobody translated keeps its local name and offers no second one`() = runTest {
        val places = search(AppLanguage.ENGLISH, UNTRANSLATED_TEMPLE)

        val temple = places.single()
        assertEquals("Đền Ngọc Sơn", temple.name)
        // Null rather than the same string twice, so the sheet has nothing to draw and the
        // screen does not print one name above an identical one.
        assertNull(temple.localName)
        assertEquals("Đền Ngọc Sơn", temple.mappedName)
    }

    @Test
    fun `a Vietnamese traveller is never handed the English name`() = runTest {
        // The `name` tag in Vietnam is already the Vietnamese name, so the English fallback
        // every other language depends on is a downgrade here and is switched off.
        val places = search(AppLanguage.VIETNAMESE, TRANSLATED_PRISON)

        val prison = places.single()
        assertEquals("Nhà tù Hỏa Lò", prison.name)
        assertNull(prison.localName)
    }

    @Test
    fun `a language OSM does not carry falls back to English rather than to Vietnamese`() =
        runTest {
            // Effectively nothing in Vietnam is tagged `name:ja`. English is not one option
            // among several for a Japanese traveller — it is the only translation there is.
            val places = search(AppLanguage.JAPANESE, TRANSLATED_PRISON)

            val prison = places.single()
            assertEquals("Hoa Lo Prison", prison.name)
            assertEquals("Nhà tù Hỏa Lò", prison.localName)
        }

    @Test
    fun `a name in the traveller's own language wins over the English one`() = runTest {
        val places = search(AppLanguage.JAPANESE, JAPANESE_PRISON)

        assertEquals("ホアロー収容所", places.single().name)
    }

    @Test
    fun `two branches of one chain still collapse when only one has been translated`() = runTest {
        // The regression this whole change risks. Deduplication is by name, and the *shown*
        // name now moves with the language — so keyed on that, these two stop being the same
        // café the moment a mapper translates one of them, and the list grows a duplicate row.
        // Measured within 5 km of Hoàn Kiếm there are 16 branches of this one chain.
        val places = search(AppLanguage.ENGLISH, EMPTY_ELEMENTS, foodElements = TWO_CAFE_BRANCHES)

        assertEquals(1, places.size, "two branches of one chain must still be one row")
        // The survivor is the better-mapped copy, which here is the translated one — so the
        // traveller keeps both the English name and the deduplication.
        assertEquals("Cong Caphe", places.single().name)
    }

    /** The half of a split search that asks for restaurants. */
    private fun isFoodQuery(query: String) = query.contains("restaurant")

    private fun io.ktor.client.request.HttpRequestData.bodyText(): String = when (val c = body) {
        is TextContent -> c.text
        is OutgoingContent.ByteArrayContent -> c.bytes().decodeToString()
        else -> ""
    }

    private companion object {
        val EMPTY_ELEMENTS = """{"version":0.6,"elements":[]}"""
        val EMPTY_PAGES = """{"query":{"pages":[]}}"""

        /** Hỏa Lò really is tagged this way: a Vietnamese `name` and an English `name:en`. */
        val TRANSLATED_PRISON = """
        {
          "version": 0.6,
          "elements": [
            {
              "type": "node", "id": 445345255, "lat": 21.0257, "lon": 105.8465,
              "tags": {
                "name": "Nhà tù Hỏa Lò", "name:en": "Hoa Lo Prison", "tourism": "museum"
              }
            }
          ]
        }
        """.trimIndent()

        /** The other 53%: no translation of any kind. */
        val UNTRANSLATED_TEMPLE = """
        {
          "version": 0.6,
          "elements": [
            {
              "type": "way", "id": 178995262, "center": { "lat": 21.0307, "lon": 105.8523 },
              "tags": { "name": "Đền Ngọc Sơn", "amenity": "place_of_worship" }
            }
          ]
        }
        """.trimIndent()

        val JAPANESE_PRISON = """
        {
          "version": 0.6,
          "elements": [
            {
              "type": "node", "id": 445345255, "lat": 21.0257, "lon": 105.8465,
              "tags": {
                "name": "Nhà tù Hỏa Lò", "name:en": "Hoa Lo Prison",
                "name:ja": "ホアロー収容所", "tourism": "museum"
              }
            }
          ]
        }
        """.trimIndent()

        /** One café chain, mapped twice, translated once. The better-mapped copy is first. */
        val TWO_CAFE_BRANCHES = """
        {
          "version": 0.6,
          "elements": [
            {
              "type": "node", "id": 111, "lat": 21.0290, "lon": 105.8530,
              "tags": {
                "name": "Cộng Cà Phê", "name:en": "Cong Caphe", "amenity": "cafe",
                "opening_hours": "07:00-23:00", "website": "https://congcaphe.com"
              }
            },
            {
              "type": "node", "id": 222, "lat": 21.0300, "lon": 105.8540,
              "tags": { "name": "Cộng Cà Phê", "amenity": "cafe" }
            }
          ]
        }
        """.trimIndent()
    }
}
