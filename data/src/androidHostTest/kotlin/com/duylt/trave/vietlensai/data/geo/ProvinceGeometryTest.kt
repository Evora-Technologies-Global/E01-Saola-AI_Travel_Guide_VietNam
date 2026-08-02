package com.duylt.trave.vietlensai.data.geo

import com.duylt.trave.vietlensai.data.local.asset.parseProvinces
import com.duylt.trave.vietlensai.domain.model.Archipelago
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.Province
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Reads the asset straight off disk rather than from the test classpath.
 *
 * Android library assets are not on the unit-test classpath, and the AGP source-set
 * DSL that used to put them there no longer accepts the cast. Both candidate paths
 * are tried because the working directory is the module when Gradle runs the test
 * and the repository root when an IDE does.
 *
 * This is why the suite lives in `androidHostTest` rather than `commonTest`: reading a
 * file relative to the project directory is a JVM-only trick, and the 34 outlines it
 * checks are the same bytes on both platforms anyway.
 */
private fun readShippedAsset(): String {
    val candidates = listOf(
        File("src/androidMain/assets/provinces.json"),
        File("data/src/androidMain/assets/provinces.json"),
    )
    val file = candidates.firstOrNull { it.isFile }
        ?: error("provinces.json not found; looked in ${candidates.map { it.absolutePath }}")
    return file.readText()
}

/**
 * Runs against the real `provinces.json` that ships in the APK.
 *
 * The cases below are the coordinates of actual cities, and they are here because
 * this is the one part of the passport that can be silently, invisibly wrong: a map
 * that renders beautifully while stamping Huế for a photo taken in Đà Nẵng looks
 * perfectly fine and is useless. They also pin the 2025 reorganisation — if the
 * asset were ever regenerated from a pre-merger source, the "old capital now belongs
 * to" cases would fail immediately instead of shipping 63 provinces' worth of wrong.
 */
class ProvinceGeometryTest {

    private val provinces: List<Province> = parseProvinces(readShippedAsset())

    private fun locate(latitude: Double, longitude: Double): Province? =
        locateProvince(provinces, GeoPoint(latitude = latitude, longitude = longitude))

    @Test
    fun `asset carries all 34 post-merger units`() {
        assertEquals(34, provinces.size)
        assertEquals(34, provinces.map { it.id }.distinct().size)
        assertTrue(provinces.all { it.rings.isNotEmpty() })
        assertTrue(provinces.all { ring -> ring.rings.all { it.size >= 6 && it.size % 2 == 0 } })
    }

    @Test
    fun `provincial capitals resolve to their own province`() {
        val cases = listOf(
            Triple("Hà Nội", 21.0287 to 105.8524, "Hà Nội"),
            Triple("Hải Phòng", 20.8449 to 106.6881, "Hải Phòng"),
            Triple("Thanh Hóa", 19.8067 to 105.7852, "Thanh Hóa"),
            Triple("Vinh", 18.6796 to 105.6813, "Nghệ An"),
            Triple("Hà Tĩnh", 18.3559 to 105.8877, "Hà Tĩnh"),
            Triple("Đông Hà", 16.8163 to 107.1003, "Quảng Trị"),
            Triple("Huế", 16.4637 to 107.5909, "Huế"),
            Triple("Đà Nẵng", 16.0544 to 108.2022, "Đà Nẵng"),
            Triple("Quảng Ngãi", 15.1214 to 108.8044, "Quảng Ngãi"),
            Triple("Pleiku", 13.9833 to 108.0000, "Gia Lai"),
            Triple("Buôn Ma Thuột", 12.6667 to 108.0500, "Đắk Lắk"),
            Triple("Nha Trang", 12.2388 to 109.1967, "Khánh Hòa"),
            Triple("Đà Lạt", 11.9404 to 108.4583, "Lâm Đồng"),
            Triple("Biên Hòa", 10.9574 to 106.8426, "Đồng Nai"),
            Triple("TP.HCM", 10.7769 to 106.7009, "Hồ Chí Minh"),
            Triple("Tây Ninh", 11.3100 to 106.0983, "Tây Ninh"),
            Triple("Cao Lãnh", 10.4600 to 105.6300, "Đồng Tháp"),
            Triple("Vĩnh Long", 10.2500 to 105.9700, "Vĩnh Long"),
            Triple("Cần Thơ", 10.0452 to 105.7469, "Cần Thơ"),
            Triple("Long Xuyên", 10.3860 to 105.4350, "An Giang"),
            Triple("Cà Mau", 9.1769 to 105.1524, "Cà Mau"),
            Triple("Lào Cai", 22.4856 to 103.9707, "Lào Cai"),
            Triple("Điện Biên Phủ", 21.3833 to 103.0167, "Điện Biên"),
            Triple("Sơn La", 21.3270 to 103.9141, "Sơn La"),
            Triple("Lai Châu", 22.3960 to 103.4580, "Lai Châu"),
            Triple("Tuyên Quang", 21.8230 to 105.2140, "Tuyên Quang"),
            Triple("Cao Bằng", 22.6660 to 106.2570, "Cao Bằng"),
            Triple("Thái Nguyên", 21.5942 to 105.8482, "Thái Nguyên"),
            Triple("Lạng Sơn", 21.8530 to 106.7610, "Lạng Sơn"),
            Triple("Việt Trì", 21.3227 to 105.4024, "Phú Thọ"),
            Triple("Bắc Ninh", 21.1861 to 106.0763, "Bắc Ninh"),
            Triple("Hưng Yên", 20.6464 to 106.0511, "Hưng Yên"),
            Triple("Ninh Bình", 20.2506 to 105.9744, "Ninh Bình"),
            Triple("Hạ Long", 20.9517 to 107.0784, "Quảng Ninh"),
        )
        cases.forEach { (label, coordinate, expected) ->
            val (lat, lon) = coordinate
            assertEquals("$label should resolve to $expected", expected, locate(lat, lon)?.name)
        }
    }

    @Test
    fun `capitals of merged provinces resolve to the province that absorbed them`() {
        val cases = listOf(
            Triple("Hải Dương", 20.9373 to 106.3145, "Hải Phòng"),
            Triple("Đồng Hới (Quảng Bình)", 17.4689 to 106.6223, "Quảng Trị"),
            Triple("Tam Kỳ (Quảng Nam)", 15.5736 to 108.4740, "Đà Nẵng"),
            Triple("Kon Tum", 14.3500 to 108.0000, "Quảng Ngãi"),
            Triple("Quy Nhơn (Bình Định)", 13.7830 to 109.2196, "Gia Lai"),
            Triple("Tuy Hòa (Phú Yên)", 13.0882 to 109.0929, "Đắk Lắk"),
            Triple("Phan Rang (Ninh Thuận)", 11.5645 to 108.9887, "Khánh Hòa"),
            Triple("Gia Nghĩa (Đắk Nông)", 11.9990 to 107.6910, "Lâm Đồng"),
            Triple("Phan Thiết (Bình Thuận)", 10.9280 to 108.1020, "Lâm Đồng"),
            Triple("Vũng Tàu (Bà Rịa – Vũng Tàu)", 10.3460 to 107.0843, "Hồ Chí Minh"),
            Triple("Thủ Dầu Một (Bình Dương)", 10.9804 to 106.6519, "Hồ Chí Minh"),
            Triple("Đồng Xoài (Bình Phước)", 11.5340 to 106.8830, "Đồng Nai"),
            Triple("Tân An (Long An)", 10.5350 to 106.4130, "Tây Ninh"),
            Triple("Mỹ Tho (Tiền Giang)", 10.3600 to 106.3600, "Đồng Tháp"),
            Triple("Bến Tre", 10.2415 to 106.3759, "Vĩnh Long"),
            Triple("Trà Vinh", 9.9347 to 106.3453, "Vĩnh Long"),
            Triple("Sóc Trăng", 9.6025 to 105.9739, "Cần Thơ"),
            Triple("Vị Thanh (Hậu Giang)", 9.7840 to 105.4700, "Cần Thơ"),
            Triple("Rạch Giá (Kiên Giang)", 10.0125 to 105.0808, "An Giang"),
            Triple("Bạc Liêu", 9.2940 to 105.7215, "Cà Mau"),
            Triple("Yên Bái", 21.7050 to 104.8700, "Lào Cai"),
            Triple("Hà Giang", 22.8233 to 104.9836, "Tuyên Quang"),
            Triple("Bắc Kạn", 22.1470 to 105.8340, "Thái Nguyên"),
            Triple("Hòa Bình", 20.8133 to 105.3383, "Phú Thọ"),
            Triple("Vĩnh Yên (Vĩnh Phúc)", 21.3089 to 105.6047, "Phú Thọ"),
            Triple("Bắc Giang", 21.2730 to 106.1946, "Bắc Ninh"),
            Triple("Thái Bình", 20.4463 to 106.3366, "Hưng Yên"),
            Triple("Nam Định", 20.4388 to 106.1621, "Ninh Bình"),
            Triple("Phủ Lý (Hà Nam)", 20.5410 to 105.9130, "Ninh Bình"),
        )
        cases.forEach { (label, coordinate, expected) ->
            val (lat, lon) = coordinate
            assertEquals("$label is part of $expected since 2025", expected, locate(lat, lon)?.name)
        }
    }

    @Test
    fun `islands belong to the province that administers them`() {
        assertEquals("An Giang", locate(10.2270, 103.9640)?.name) // Phú Quốc
        assertEquals("Hồ Chí Minh", locate(8.6900, 106.6000)?.name) // Côn Đảo
        assertEquals("Quảng Ngãi", locate(15.3830, 109.1170)?.name) // Lý Sơn
        assertEquals("Hải Phòng", locate(20.7280, 107.0480)?.name) // Cát Bà
    }

    @Test
    fun `both archipelagos are present and belong to their province`() {
        val daNang = provinces.first { it.name == "Đà Nẵng" }
        val khanhHoa = provinces.first { it.name == "Khánh Hòa" }

        assertTrue("Đà Nẵng must carry Hoàng Sa", Archipelago.HOANG_SA in daNang.archipelagos)
        assertTrue("Khánh Hòa must carry Trường Sa", Archipelago.TRUONG_SA in khanhHoa.archipelagos)

        // The asset is built from two different sources — Trường Sa comes inside
        // OSM's Khánh Hòa relation, Hoàng Sa has to be attached separately — so it is
        // entirely possible to regenerate it and silently lose one of them.
        assertEquals("Đà Nẵng", locate(16.5330, 111.6060)?.name) // Đảo Hoàng Sa
        assertEquals("Khánh Hòa", locate(8.6440, 111.9200)?.name) // Trường Sa Lớn
    }

    @Test
    fun `offshore rings are excluded from the bounds the map is fitted to`() {
        val khanhHoa = provinces.first { it.name == "Khánh Hòa" }

        // Fitting the map to the full bounds is what flattened the country: the
        // mainland ends at 109.5°E and Trường Sa reaches past 114°E.
        assertTrue(
            "mainland bounds must stop at the coast",
            khanhHoa.mainlandBounds.maxLongitude < 110.0,
        )
        assertTrue(
            "full bounds must still reach Trường Sa",
            khanhHoa.bounds.maxLongitude > 114.0,
        )
        assertTrue(
            "no mainland ring may sit offshore",
            provinces.all { province ->
                province.mainlandRings.all { ring ->
                    (0 until ring.size / 2).all { ring[it * 2] < 110.5 }
                }
            },
        )
    }

    @Test
    fun `coastal spots just offshore still stamp their province`() {
        // Boundaries are clipped to the coastline, so these fall in open water and
        // only resolve through the snap-to-nearest pass. They are also precisely
        // where a traveller takes photos, which is why the fallback exists at all.
        assertEquals("Quảng Ninh", locate(20.90, 107.15)?.name) // Hạ Long Bay cruise
        assertEquals("Lâm Đồng", locate(10.9330, 108.2870)?.name) // Mũi Né
        assertEquals("Đà Nẵng", locate(16.0600, 108.2470)?.name) // Mỹ Khê beach
        assertEquals("An Giang", locate(10.2150, 103.9600)?.name) // Dương Đông, Phú Quốc
    }

    @Test
    fun `points well outside vietnam resolve to nothing`() {
        assertNull("Bangkok", locate(13.7563, 100.5018))
        assertNull("Phnom Penh", locate(11.5564, 104.9282))
        assertNull("Hong Kong", locate(22.3193, 114.1694))
        assertNull("middle of the South China Sea", locate(14.0, 113.0))
        assertNull("Gulf of Thailand, far offshore", locate(9.0, 101.5))
    }

    @Test
    fun `snap radius does not reach across open water`() {
        // ~90 km east of Nha Trang: too far to be a beach photo, so it must not
        // silently claim Khánh Hòa.
        assertNull(locate(12.2400, 110.0500))
    }

    @Test
    fun `containment agrees with the province bounding box`() {
        provinces.forEach { province ->
            val c = province.center
            assertTrue(
                "${province.name} centre must sit inside its own bounds",
                c.longitude >= province.bounds.minLongitude &&
                    c.longitude <= province.bounds.maxLongitude &&
                    c.latitude >= province.bounds.minLatitude &&
                    c.latitude <= province.bounds.maxLatitude,
            )
            assertNotNull("${province.name} centre must resolve", locate(c.latitude, c.longitude))
        }
    }

    @Test
    fun `no two provinces claim the same point`() {
        provinces.forEach { province ->
            val c = province.center
            val claimants = provinces.filter { it.contains(c.longitude, c.latitude) }
            assertEquals(
                "${province.name} centre claimed by ${claimants.map { it.name }}",
                1,
                claimants.size,
            )
        }
    }
}
