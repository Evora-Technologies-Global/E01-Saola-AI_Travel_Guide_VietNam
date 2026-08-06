package com.evora.technologies.saola.data.seed

import com.evora.technologies.saola.data.local.db.Converters
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The half of the demo seeder that can be wrong on its own.
 *
 * Writing the rows needs Room, a file system and an asset packager; deciding *what* the rows
 * say needs none of those, which is why the arithmetic and the wire names live in
 * [toTrip] and are pinned here. The two properties worth holding are that nothing lands in
 * the future — a journal showing a find "at 21:42" while the clock says noon is the first
 * thing anyone notices — and that a photograph's name is the one the rest of the app already
 * knows how to resolve.
 */
class DemoContentMapperTest {

    private val zone = TimeZone.UTC

    /** 2026-08-05T12:00:00Z, so "today" has hours on both sides of the current one. */
    private val noon = 1_785_931_200_000L

    private fun content(vararg discoveries: DemoDiscovery, chat: DemoChat? = null) =
        DemoContent(discoveries = discoveries.toList(), chat = chat)

    private fun demo(id: String, day: Int, hour: Int) =
        DemoDiscovery(id = id, day = day, hour = hour, title = id, category = "FOOD")

    @Test
    fun `an entry earlier in today keeps the hour it was written with`() {
        val trip = content(demo("pho", day = 0, hour = 7)).toTrip(noon, zone)

        val expected = noon - 12 * 3_600_000L + 7 * 3_600_000L
        assertEquals(expected, trip.discoveries.single().entity.createdAt)
    }

    @Test
    fun `an entry later than the current hour is pulled back to now`() {
        // 21:00 today has not happened yet when the app is opened at noon.
        val trip = content(demo("ca-phe", day = 0, hour = 21)).toTrip(noon, zone)

        assertTrue(
            trip.discoveries.single().entity.createdAt <= noon,
            "no find may be stamped in the future",
        )
    }

    @Test
    fun `two clamped entries keep a stable order`() {
        val trip = content(
            demo("first", day = 0, hour = 21),
            demo("second", day = 0, hour = 22),
        ).toTrip(noon, zone)

        val stamps = trip.discoveries.map { it.entity.createdAt }
        assertEquals(stamps.distinct().size, stamps.size, "a clamp may not collapse two finds")
    }

    @Test
    fun `an earlier day keeps its whole day of offset`() {
        val trip = content(demo("ha-long", day = 3, hour = 15)).toTrip(noon, zone)

        val expected = noon - 12 * 3_600_000L - 3 * 86_400_000L + 15 * 3_600_000L
        assertEquals(expected, trip.discoveries.single().entity.createdAt)
    }

    @Test
    fun `the photograph is named the way a real capture is`() {
        val trip = content(demo("pho", day = 1, hour = 8)).toTrip(noon, zone)
        val seeded = trip.discoveries.single()

        // `CaptureStore.resolve` and the orphan sweep both parse this shape; a name they
        // cannot date is a file the sweep refuses to touch and a photo nothing can find.
        val name = assertNotNull(seeded.entity.imageName)
        assertTrue(name.startsWith("capture_") && name.endsWith(".jpg"), name)
        assertEquals(seeded.entity.createdAt.toString(), name.removePrefix("capture_").removeSuffix(".jpg"))
        assertEquals("seed/pho.jpg", seeded.photoAsset)
    }

    @Test
    fun `province is left for the app to resolve`() {
        val trip = content(demo("sa-pa", day = 2, hour = 8)).toTrip(noon, zone)

        assertEquals(null, trip.discoveries.single().entity.provinceId)
    }

    @Test
    fun `chat turns hang off the discovery they are about`() {
        val trip = content(
            demo("van-mieu", day = 1, hour = 9),
            chat = DemoChat(
                discoveryId = "van-mieu",
                messages = listOf(
                    DemoMessage(role = "user", content = "Vì sao?"),
                    DemoMessage(role = "model", content = "Bởi vì…"),
                ),
            ),
        ).toTrip(noon, zone)

        val parent = trip.discoveries.single().entity.id
        assertEquals(2, trip.messages.size)
        assertTrue(trip.messages.all { it.discoveryId == parent })
        assertTrue(
            trip.messages[0].createdAt < trip.messages[1].createdAt,
            "a reply may not predate its question",
        )
    }

    @Test
    fun `a conversation about a discovery that is not in the file is dropped`() {
        val trip = content(
            demo("pho", day = 0, hour = 7),
            chat = DemoChat(discoveryId = "nowhere", messages = listOf(DemoMessage())),
        ).toTrip(noon, zone)

        // Rather than written with a dangling foreign key, which Room rejects at insert time
        // and would take the rest of the seed down with it.
        assertTrue(trip.messages.isEmpty())
    }

    @Test
    fun `list columns are stored in the format the app reads back`() {
        val trip = content(
            DemoDiscovery(
                id = "pho",
                title = "Phở",
                category = "FOOD",
                tags = listOf("phở", "phở bò"),
                sections = listOf(DemoSection(title = "Nước dùng", body = "Ninh xương")),
                nearby = listOf(DemoNearby(name = "Chợ", reason = "Gần", walkingMinutes = 6)),
            ),
        ).toTrip(noon, zone)

        val entity = trip.discoveries.single().entity
        assertEquals(listOf("phở", "phở bò"), Converters.decodeStrings(entity.tagsJson))
        assertEquals("Nước dùng", Converters.decodeSections(entity.sectionsJson).single().title)
        assertEquals(6, Converters.decodeNearby(entity.nearbyJson).single().walkingMinutes)
    }

    @Test
    fun `the shipped demo file parses into a full trip`() {
        // The file the debug build actually packages. Parsing it here is what stops a hand
        // edit to `tools/seed/demo-content.json` from being discovered on a device.
        val parsed = Json { ignoreUnknownKeys = true }
            .decodeFromString<DemoContent>(DEMO_CONTENT_SAMPLE)

        val trip = parsed.toTrip(noon, zone)
        assertEquals(1, trip.discoveries.size)
        assertEquals("demo-pho", trip.discoveries.single().entity.id)
    }
}

/**
 * A cut-down copy of `tools/seed/demo-content.json`'s shape.
 *
 * Inline rather than read from disk because `commonTest` compiles for Kotlin/Native, where
 * `java.io.File` does not exist and the working directory is not the repository — see
 * `LLM.md` §9. The real file is validated against this shape by the debug build itself.
 */
private const val DEMO_CONTENT_SAMPLE = """
{
  "note": "ignored",
  "discoveries": [
    {
      "id": "pho", "day": 6, "hour": 7, "title": "Phở bò", "localName": "Phở",
      "category": "FOOD", "commons": "x.jpg", "lat": 21.03, "lon": 105.79,
      "province": "Hà Nội", "placeHint": "Cầu Giấy", "confidence": 0.96, "favorite": true,
      "summary": "Bánh phở dẹt.",
      "sections": [{ "title": "Nước dùng", "body": "Xương ống bò." }],
      "funFacts": ["pot-au-feu"], "tags": ["phở"],
      "nearby": [{ "name": "Chợ", "reason": "Gần", "category": "CULTURE", "walkingMinutes": 6 }],
      "suggestedQuestions": ["Khác nhau ra sao?"]
    }
  ],
  "chat": { "discoveryId": "pho", "messages": [{ "role": "user", "text": "Sao?" }] },
  "tripSummaries": [
    { "day": 6, "headline": "Một ngày", "narrative": "…", "highlights": ["a"], "tomorrowIdeas": ["b"] }
  ]
}
"""
