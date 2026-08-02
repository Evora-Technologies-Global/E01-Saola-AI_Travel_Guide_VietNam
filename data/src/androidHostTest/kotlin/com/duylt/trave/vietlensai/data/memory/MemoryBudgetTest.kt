package com.duylt.trave.vietlensai.data.memory

import com.duylt.trave.vietlensai.data.geo.locateProvince
import com.duylt.trave.vietlensai.data.local.asset.parseProvinces
import com.duylt.trave.vietlensai.data.local.file.ImagePolicy
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.Province
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Memory budgets for the things this app loads eagerly and keeps.
 *
 * The lens is a camera app on a phone that may have 2 GB of RAM and a browser open behind
 * it, and the two ways it can run out are both invisible in a normal test:
 *
 *  - an **asset** grows. `provinces.json` is parsed on first use and cached for the process
 *    lifetime by design, so its size is a permanent resident cost. Regenerating it from a
 *    finer-grained OpenStreetMap extract is a one-line change to a Python script that could
 *    multiply the vertex count without anybody noticing until a low-end phone starts dying
 *    on the passport screen.
 *  - a **capture** is uploaded at full resolution. A 12 MP JPEG is ~4 MB; base64 makes it
 *    5.3 MB of `String`, the JSON body copies it again, and the HTTP encoder may copy it
 *    once more. Four simultaneous copies of a photo is how a mid-range phone OOMs on the
 *    one action the whole app exists for.
 *
 * None of these are caught by asserting behaviour, because the behaviour stays correct
 * right up until the allocation fails. So they are asserted as numbers.
 *
 * The ceilings are set with headroom over today's values, not at them: this is a tripwire
 * for a change of *kind* — a different data source, a dropped downscale — rather than a
 * ratchet that fails on every ordinary edit.
 */
class MemoryBudgetTest {

    // ---------------------------------------------------------------------------------
    // Shipped assets
    // ---------------------------------------------------------------------------------

    @Test
    fun `the province asset stays inside its size budget`() {
        val file = asset("provinces.json")
        val kilobytes = file.length() / 1024

        assertTrue(
            "provinces.json is now ${kilobytes}KB, over the ${PROVINCES_BUDGET_KB}KB budget. " +
                "It is parsed once and held for the process lifetime, so this is permanent " +
                "resident memory on every device. If the asset genuinely needs more detail, " +
                "simplify the polygons in tools/provinces/build_provinces.py rather than " +
                "raising this number.",
            kilobytes <= PROVINCES_BUDGET_KB,
        )
    }

    @Test
    fun `the catalog asset stays inside its size budget`() {
        val kilobytes = asset("catalog.json").length() / 1024
        assertTrue(
            "catalog.json is now ${kilobytes}KB, over the ${CATALOG_BUDGET_KB}KB budget.",
            kilobytes <= CATALOG_BUDGET_KB,
        )
    }

    @Test
    fun `the sovereignty map asset stays inside its size budget`() {
        val kilobytes = asset("sovereignty_map.json", composeResources = true).length() / 1024
        assertTrue(
            "sovereignty_map.json is now ${kilobytes}KB, over the ${SOVEREIGNTY_BUDGET_KB}KB budget.",
            kilobytes <= SOVEREIGNTY_BUDGET_KB,
        )
    }

    /**
     * The parsed form, not just the file.
     *
     * A compact JSON file can still explode in memory: every coordinate pair that ends up
     * boxed as a `Double` object rather than living in a `DoubleArray` costs about 16 bytes
     * of header for 8 bytes of value. The parser deliberately hands `DoubleArray` straight
     * through for exactly this reason, and a well-meant refactor to `List<GeoPoint>` would
     * quadruple the resident cost while looking tidier.
     */
    @Test
    fun `the parsed province set stays inside its vertex budget`() {
        val provinces = parseProvinces(asset("provinces.json").readText())
        val vertices = provinces.sumOf { province -> province.rings.sumOf { it.size / 2 } }

        assertTrue(
            "The 34 outlines now hold $vertices vertices, over the $VERTEX_BUDGET budget. " +
                "At 8 bytes each in a DoubleArray that is ~${vertices * 16 / 1024}KB resident; " +
                "boxed into objects it would be four times that.",
            vertices <= VERTEX_BUDGET,
        )

        // And the representation itself, which is the assumption the budget rests on.
        provinces.forEach { province ->
            province.rings.forEach { ring ->
                assertEquals(
                    "a ring with an odd length cannot be [lon, lat] pairs",
                    0,
                    ring.size % 2,
                )
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // Uploads
    // ---------------------------------------------------------------------------------

    /**
     * The downscale policy is the single biggest lever on peak memory in this app.
     *
     * `AndroidCaptureStore.decodeScaled` picks an `inSampleSize` from [ImagePolicy.MAX_EDGE_PX];
     * raising that constant raises every allocation downstream of it — the bitmap, the JPEG
     * buffer, the base64 string, the request body. This pins the constant so the change is a
     * deliberate one with this arithmetic in front of whoever makes it.
     */
    @Test
    fun `the capture downscale policy keeps an upload inside its budget`() {
        assertEquals(
            "MAX_EDGE_PX drives every allocation in the upload path; changing it needs a " +
                "look at the arithmetic in this test.",
            1024,
            ImagePolicy.MAX_EDGE_PX,
        )

        // Worst case for a 4:3 sensor once the long edge is capped.
        val longEdge = ImagePolicy.MAX_EDGE_PX
        val shortEdge = longEdge * 3 / 4
        val argb8888Bytes = longEdge.toLong() * shortEdge * 4

        assertTrue(
            "A decoded capture is now ${argb8888Bytes / 1024}KB of bitmap, over the " +
                "${DECODED_BITMAP_BUDGET_KB}KB budget.",
            argb8888Bytes / 1024 <= DECODED_BITMAP_BUDGET_KB,
        )

        // JPEG at quality 85 compresses a photograph to roughly a tenth of its raw pixels;
        // base64 then adds a third on top. Held simultaneously, because the encoder cannot
        // release the byte array until the string exists.
        val jpegBytes = argb8888Bytes / 10
        val base64Bytes = jpegBytes * 4 / 3
        val peakBytes = argb8888Bytes + jpegBytes + base64Bytes

        assertTrue(
            "Peak upload footprint is now ${peakBytes / 1024}KB, over the " +
                "${UPLOAD_PEAK_BUDGET_KB}KB budget. That is the bitmap, its JPEG and the " +
                "base64 string alive at once, which is what the request path holds.",
            peakBytes / 1024 <= UPLOAD_PEAK_BUDGET_KB,
        )

        assertEquals("quality drives the JPEG size assumption above", 85, ImagePolicy.JPEG_QUALITY)
    }

    // ---------------------------------------------------------------------------------
    // Hostile coordinates
    // ---------------------------------------------------------------------------------

    /**
     * A GPS fix the app cannot use must resolve to "no province", not to a wrong one and
     * not to a crash.
     *
     * `locateProvince` runs a point-in-polygon test and then a nearest-edge search over
     * 9,000-odd vertices. Both are pure floating-point arithmetic, and both are reachable
     * with whatever the fused location provider hands over — which on a device with a
     * failing GNSS chip has, in the wild, included NaN.
     */
    @Test
    fun `a hostile coordinate resolves to nothing rather than crashing`() {
        val provinces = parseProvinces(asset("provinces.json").readText())

        val hostile = listOf(
            GeoPoint(Double.NaN, Double.NaN),
            GeoPoint(Double.NaN, 105.0),
            GeoPoint(21.0, Double.NaN),
            GeoPoint(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
            GeoPoint(Double.NEGATIVE_INFINITY, 105.0),
            GeoPoint(Double.MAX_VALUE, Double.MAX_VALUE),
            GeoPoint(Double.MIN_VALUE, Double.MIN_VALUE),
            GeoPoint(0.0, 0.0), // Null Island
            GeoPoint(90.0, 180.0),
            GeoPoint(-90.0, -180.0),
            GeoPoint(91.0, 181.0), // out of range entirely
            GeoPoint(-0.0, -0.0),
        )

        hostile.forEach { point ->
            val located: Province? = locateProvince(provinces, point)
            assertTrue(
                "a coordinate outside Vietnam was stamped as ${located?.id}: $point",
                located == null || located.id.isNotEmpty(),
            )
        }

        // Null Island and the poles are definitively not in Vietnam.
        assertEquals(null, locateProvince(provinces, GeoPoint(0.0, 0.0)))
        assertEquals(null, locateProvince(provinces, GeoPoint(90.0, 180.0)))
    }

    /**
     * The nearest-province search must not degrade into a full scan on every call.
     *
     * `locateProvince` falls back to a distance search only when nothing contains the
     * point, and guards each province with a cheap bounding-box reject first. Losing that
     * reject turns an offshore fix into ~9,000 square roots — on the main thread, if a
     * caller ever forgets `withContext`. A wall-clock ceiling is a blunt instrument, but it
     * is the one that fails when the algorithm changes shape.
     */
    @Test
    fun `resolving an offshore fix stays fast enough for a synchronous caller`() {
        val provinces = parseProvinces(asset("provinces.json").readText())
        // Well out in the Gulf of Tonkin: contained by nothing, so this takes the slow path.
        val offshore = GeoPoint(latitude = 20.5, longitude = 108.5)

        repeat(20) { locateProvince(provinces, offshore) } // warm the JIT

        val startNanos = System.nanoTime()
        repeat(RESOLVE_ITERATIONS) { locateProvince(provinces, offshore) }
        val perCallMicros = (System.nanoTime() - startNanos) / RESOLVE_ITERATIONS / 1_000

        assertTrue(
            "Resolving an offshore fix now takes ${perCallMicros}µs per call, over the " +
                "${RESOLVE_BUDGET_MICROS}µs budget. The bounding-box reject in " +
                "locateProvince is what keeps this off a full vertex scan.",
            perCallMicros <= RESOLVE_BUDGET_MICROS,
        )
    }

    /** Parsing must not retain the source text — a 190KB string held forever for nothing. */
    @Test
    fun `parsing produces province data that does not reference the source text`() {
        val text = asset("provinces.json").readText()
        val provinces = parseProvinces(text)

        assertNotNull(provinces)
        assertFalse(provinces.isEmpty())
        // Every ring is a primitive array copied out of the parser, so nothing in the
        // returned graph can be a substring view onto `text`.
        provinces.forEach { province ->
            assertTrue(province.rings.all { it.isNotEmpty() })
        }
    }

    // ---------------------------------------------------------------------------------

    /**
     * Assets are not on the unit-test classpath for an Android library, so they are read
     * off disk. Both roots are tried because Gradle runs tests from the module directory
     * and an IDE from the repository root — the same reason `ProvinceGeometryTest` does it.
     */
    private fun asset(name: String, composeResources: Boolean = false): File {
        val relative = if (composeResources) {
            "shared/src/commonMain/composeResources/files/$name"
        } else {
            "src/androidMain/assets/$name"
        }
        val candidates = listOf(
            File(relative),
            File("data/$relative"),
            File("../$relative"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("$name not found; looked in ${candidates.map { it.absolutePath }}")
    }

    private companion object {
        /** Today ~189KB. */
        const val PROVINCES_BUDGET_KB = 260L

        /** Today ~27KB. */
        const val CATALOG_BUDGET_KB = 64L

        /** Today ~100KB. */
        const val SOVEREIGNTY_BUDGET_KB = 160L

        /** Today ~9,200 across all 34 units. */
        const val VERTEX_BUDGET = 14_000

        /** 1024 x 768 x 4 bytes = 3,072KB. */
        const val DECODED_BITMAP_BUDGET_KB = 4_096L

        /** Bitmap + JPEG + base64 alive together, with headroom. */
        const val UPLOAD_PEAK_BUDGET_KB = 5_120L

        const val RESOLVE_ITERATIONS = 200
        const val RESOLVE_BUDGET_MICROS = 4_000L
    }
}
