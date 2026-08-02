package com.duylt.trave.vietlensai.data.mapper

import com.duylt.trave.vietlensai.data.remote.gemini.dto.DiscoveryPayload
import com.duylt.trave.vietlensai.data.remote.gemini.dto.NearbySuggestionPayload
import com.duylt.trave.vietlensai.data.remote.gemini.dto.SectionPayload
import com.duylt.trave.vietlensai.domain.model.DiscoveryCategory
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.time.Instant

/**
 * The mapper is the app's sanitising layer: the model is asked to behave, but the
 * UI is only safe because whatever comes back is cleaned exactly once, here.
 */
class DiscoveryMapperTest {

    private val now = Instant.fromEpochMilliseconds(1_700_000_000_000)

    @Test
    fun `round-trips a full payload through storage back to the domain`() {
        val payload = DiscoveryPayload(
            recognized = true,
            title = "  Temple of Literature  ",
            localName = " Văn Miếu - Quốc Tử Giám ",
            category = "landmark",
            confidence = 0.93f,
            summary = " Vietnam's first university. ",
            sections = listOf(SectionPayload("History", "Founded in 1070.")),
            funFacts = listOf(" 82 stone stelae ", ""),
            tags = listOf("confucian", " hanoi ", "  "),
            nearbySuggestions = listOf(
                NearbySuggestionPayload("Hoàn Kiếm Lake", "A ten minute stroll", "NATURE", 10),
                NearbySuggestionPayload("", "should be dropped", "FOOD", 2),
            ),
            suggestedQuestions = listOf("Who founded it?"),
            placeHint = "Đống Đa, Hà Nội",
        )

        val domain = payload
            .toEntity(
                id = "id-1",
                imagePath = "/data/img.jpg",
                location = GeoPoint(21.0287, 105.8524),
                provinceId = "VN-HN",
                modelUsed = "gemini-3.5-flash",
                createdAt = now,
            )
            .toDomain()

        assertEquals("Temple of Literature", domain.title)
        assertEquals("Văn Miếu - Quốc Tử Giám", domain.localName)
        assertEquals(DiscoveryCategory.LANDMARK, domain.category)
        assertEquals("Vietnam's first university.", domain.summary)
        assertEquals(listOf("82 stone stelae"), domain.funFacts)
        assertEquals(listOf("confucian", "hanoi"), domain.tags)
        // A suggestion with no name is unusable in the UI, so it never reaches it.
        assertEquals(1, domain.nearbySuggestions.size)
        assertEquals(DiscoveryCategory.NATURE, domain.nearbySuggestions.first().category)
        assertEquals(GeoPoint(21.0287, 105.8524), domain.location)
        assertEquals("gemini-3.5-flash", domain.modelUsed)
        assertEquals(now.toEpochMilliseconds(), domain.createdAt.toEpochMilliseconds())
        assertTrue(domain.isConfident)
    }

    @Test
    fun `clamps a confidence score outside the legal range`() {
        val overconfident = DiscoveryPayload(recognized = true, title = "X", confidence = 4.2f)
            .toEntity("id", null, null, null, null, now)
            .toDomain()
        assertEquals(1f, overconfident.confidence, 0.001f)

        val negative = DiscoveryPayload(recognized = true, title = "X", confidence = -0.5f)
            .toEntity("id", null, null, null, null, now)
            .toDomain()
        assertEquals(0f, negative.confidence, 0.001f)
    }

    @Test
    fun `maps an unknown category to OTHER rather than failing`() {
        val domain = DiscoveryPayload(recognized = true, title = "X", category = "SPACESHIP")
            .toEntity("id", null, null, null, null, now)
            .toDomain()
        assertEquals(DiscoveryCategory.OTHER, domain.category)
    }

    @Test
    fun `drops a location when only one coordinate survived`() {
        val entity = DiscoveryPayload(recognized = true, title = "X")
            .toEntity("id", null, null, null, null, now)
            .copy(latitude = 21.0)
        assertNull(entity.toDomain().location)
    }

    @Test
    fun `treats a blank local name as absent`() {
        val domain = DiscoveryPayload(recognized = true, title = "X", localName = "   ")
            .toEntity("id", null, null, null, null, now)
            .toDomain()
        assertNull(domain.localName)
    }

    @Test
    fun `flags a low-confidence match so the UI can warn`() {
        val domain = DiscoveryPayload(recognized = true, title = "X", confidence = 0.3f)
            .toEntity("id", null, null, null, null, now)
            .toDomain()
        assertTrue(!domain.isConfident)
    }
}
