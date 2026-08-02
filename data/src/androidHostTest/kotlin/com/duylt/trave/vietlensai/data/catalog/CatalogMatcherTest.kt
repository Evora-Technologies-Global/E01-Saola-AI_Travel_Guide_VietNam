package com.duylt.trave.vietlensai.data.catalog

import com.duylt.trave.vietlensai.data.local.asset.parseCatalog
import com.duylt.trave.vietlensai.domain.model.CatalogItem
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.DiscoveryCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.time.Instant

/**
 * Reads the asset straight off disk, for the same reasons [ProvinceGeometryTest] does:
 * Android library assets are not on the unit-test classpath, and the working directory
 * is the module under Gradle but the repository root under an IDE.
 */
private fun readShippedCatalog(): String {
    val candidates = listOf(
        File("src/androidMain/assets/catalog.json"),
        File("data/src/androidMain/assets/catalog.json"),
    )
    val file = candidates.firstOrNull { it.isFile }
        ?: error("catalog.json not found; looked in ${candidates.map { it.absolutePath }}")
    return file.readText()
}

/**
 * Runs against the real `catalog.json` that ships in the app.
 *
 * The matching rule is the whole feature and it fails silently in both directions: a
 * missed match leaves a tile grey while the traveller is looking straight at their own
 * photograph of the thing, and a false one collects Phở from a picture of a street.
 * Neither shows up as a crash, so they are pinned here instead.
 */
class CatalogMatcherTest {

    private val items: List<CatalogItem> = parseCatalog(readShippedCatalog())

    private fun discovery(
        title: String,
        localName: String? = null,
        tags: List<String> = emptyList(),
        id: String = title,
    ) = Discovery(
        id = id,
        title = title,
        localName = localName,
        category = DiscoveryCategory.OTHER,
        imagePath = "/tmp/$id.jpg",
        summary = "",
        sections = emptyList(),
        funFacts = emptyList(),
        tags = tags,
        nearbySuggestions = emptyList(),
        suggestedQuestions = emptyList(),
        confidence = 0.9f,
        location = null,
        placeHint = null,
        isFavorite = false,
        modelUsed = null,
        createdAt = Instant.fromEpochSeconds(0),
    )

    /** Which catalogue ids the given discoveries collect. */
    private fun collectedIds(vararg discoveries: Discovery): Set<String> =
        buildCollection(items, discoveries.toList())
            .sections
            .flatMap { it.entries }
            .filter { it.isCollected }
            .map { it.item.id }
            .toSet()

    // --- the asset itself -------------------------------------------------------

    @Test
    fun `catalogue parses and every entry is complete`() {
        assertTrue("catalogue is empty", items.isNotEmpty())
        assertEquals("duplicate ids", items.size, items.map { it.id }.distinct().size)
        assertTrue(items.all { it.name.isNotBlank() && it.nameEn.isNotBlank() })
        assertTrue(items.all { it.hint.isNotBlank() && it.hintEn.isNotBlank() })
        assertTrue(items.all { it.aliases.isNotEmpty() })
    }

    /**
     * `DiscoveryCategory.fromWire` falls back to `OTHER` for anything it does not
     * recognise, so a mistyped category in the asset would not fail to parse — it
     * would quietly create a seventh section called "Khác". No entry uses `OTHER`
     * deliberately, so finding one means a typo.
     */
    @Test
    fun `no entry fell back to the OTHER category`() {
        val stray = items.filter { it.category == DiscoveryCategory.OTHER }
        assertTrue("mistyped category on ${stray.map { it.id }}", stray.isEmpty())
    }

    @Test
    fun `no alias is shared by two entries`() {
        val owners = mutableMapOf<String, String>()
        val clashes = mutableListOf<String>()
        items.forEach { item ->
            item.aliases.forEach { alias ->
                val previous = owners.put(alias.lowercase(), item.id)
                if (previous != null) clashes += "'$alias' on $previous and ${item.id}"
            }
        }
        assertTrue("aliases claimed twice: $clashes", clashes.isEmpty())
    }

    @Test
    fun `every alias survives tokenisation`() {
        val empty = items.flatMap { item -> item.aliases.map { item.id to it } }
            .filter { (_, alias) -> tokenize(alias).isEmpty() }
        assertTrue("aliases that tokenise to nothing: $empty", empty.isEmpty())
    }

    // --- matching ---------------------------------------------------------------

    @Test
    fun `a recognised dish collects its entry`() {
        assertTrue("pho" in collectedIds(discovery("Phở bò tái")))
        assertTrue("banh-mi" in collectedIds(discovery("Bánh mì thịt nướng")))
        assertTrue("bun-bo-hue" in collectedIds(discovery("Bún bò Huế")))
    }

    @Test
    fun `the local name and the tags are searched too`() {
        assertTrue(
            "cao-lau" in collectedIds(
                discovery(title = "Hoi An noodle dish", localName = "Cao lầu"),
            ),
        )
        assertTrue(
            "non-la" in collectedIds(
                discovery(title = "Conical hat", tags = listOf("nón lá", "thủ công")),
            ),
        )
    }

    /**
     * The reason diacritics are kept rather than folded.
     *
     * Folded, `phố` and `phở` are the same six bytes, and a photograph of the Hội An
     * old quarter would collect a bowl of noodles. This is the case that would be
     * reintroduced by anyone "fixing" the matcher to be accent-insensitive.
     */
    @Test
    fun `phố does not collect phở`() {
        assertFalse("pho" in collectedIds(discovery("Phố cổ Hội An")))
    }

    @Test
    fun `a longer word does not collect a shorter alias`() {
        // `chèo` contains `chè`; whole-word matching is what keeps them apart.
        assertFalse("che" in collectedIds(discovery("Hát chèo")))
        assertFalse("thac-nuoc" in collectedIds(discovery("Thách thức")))
    }

    @Test
    fun `a multi-word alias needs its words adjacent`() {
        assertFalse(
            "bun-bo-hue" in collectedIds(discovery("Bún riêu cua và bò kho")),
        )
    }

    @Test
    fun `an unphotographed catalogue collects nothing`() {
        val collection = buildCollection(items, emptyList())
        assertEquals(items.size, collection.total)
        assertEquals(0, collection.collectedCount)
        assertEquals(0f, collection.progress, 0f)
    }

    @Test
    fun `progress counts every section`() {
        val collection = buildCollection(
            items,
            listOf(discovery("Phở gà"), discovery("Áo dài truyền thống")),
        )
        assertEquals(2, collection.collectedCount)
        assertEquals(collection.sections.sumOf { it.total }, collection.total)
        assertEquals(
            collection.collectedCount,
            collection.sections.sumOf { it.collectedCount },
        )
    }

    /**
     * The tile shows the most recent photograph, because that is the order the journal
     * hands discoveries over in.
     */
    @Test
    fun `the newest matching photograph wins`() {
        val collection = buildCollection(
            items,
            listOf(
                discovery(title = "Phở bò", id = "newest"),
                discovery(title = "Phở gà", id = "older"),
            ),
        )
        val pho = collection.sections
            .flatMap { it.entries }
            .first { it.item.id == "pho" }
        assertNotNull(pho.discovery)
        assertEquals("newest", pho.discovery?.id)
    }

    @Test
    fun `an unrelated photograph collects nothing`() {
        val collected = collectedIds(discovery("Bình hoa nhựa trên bàn làm việc"))
        assertTrue("unexpectedly collected $collected", collected.isEmpty())
    }

    @Test
    fun `entries with no photograph carry no discovery`() {
        val collection = buildCollection(items, listOf(discovery("Phở bò")))
        val uncollected = collection.sections.flatMap { it.entries }.filter { !it.isCollected }
        assertTrue(uncollected.isNotEmpty())
        uncollected.forEach { assertNull(it.discovery) }
    }
}
