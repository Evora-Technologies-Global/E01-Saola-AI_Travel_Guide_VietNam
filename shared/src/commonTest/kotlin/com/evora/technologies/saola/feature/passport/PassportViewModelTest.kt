package com.evora.technologies.saola.feature.passport

import com.evora.technologies.saola.domain.model.GeoBounds
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.JournalStats
import com.evora.technologies.saola.domain.model.PassportStamp
import com.evora.technologies.saola.domain.model.Province
import com.evora.technologies.saola.domain.model.ProvinceType
import com.evora.technologies.saola.domain.model.TravelPassport
import com.evora.technologies.saola.domain.usecase.BackfillProvincesUseCase
import com.evora.technologies.saola.domain.usecase.ObserveJournalStatsUseCase
import com.evora.technologies.saola.domain.usecase.ObserveProvinceDiscoveriesUseCase
import com.evora.technologies.saola.domain.usecase.ObserveSettingsUseCase
import com.evora.technologies.saola.domain.usecase.ObserveTravelPassportUseCase
import com.evora.technologies.saola.testing.FakeCaptureStore
import com.evora.technologies.saola.testing.FakeDiscoveryRepository
import com.evora.technologies.saola.testing.FakeJournalRepository
import com.evora.technologies.saola.testing.FakeProvinceRepository
import com.evora.technologies.saola.testing.FakeSettingsRepository
import com.evora.technologies.saola.testing.discovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The map of where the traveller has been, and the two ways it can lie.
 *
 * The first is the province sheet showing the previous province's photographs under the new
 * province's name, which is why the list is dropped with the selection rather than left to be
 * overwritten. The second is quieter: this screen starts a province backfill in `init` that
 * nothing is waiting on and no button can retry, so a throw from it has to be contained
 * without taking the map down with it.
 *
 * Covers are deliberately not asserted as images. A capture that will not decode is the
 * ordinary case on a test runtime with no image pipeline, and the property that matters is
 * exactly the one that survives it — a cover that cannot be read costs a photograph inside an
 * outline, never the screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PassportViewModelTest {

    private val provinces = FakeProvinceRepository()
    private val journal = FakeJournalRepository()
    private val settings = FakeSettingsRepository()
    private val discoveries = FakeDiscoveryRepository()
    private val captures = FakeCaptureStore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the passport is published and the spinner comes down`() = runTest {
        provinces.passport.value = TravelPassport(listOf(stamp("hanoi", count = 2)))
        val vm = viewModel()
        runCurrent()

        assertFalse(vm.state.value.isLoading)
        assertEquals(1, vm.state.value.passport.unlockedCount)
    }

    @Test
    fun `a backfill that throws leaves the map standing`() = runTest {
        // Nothing awaits this and no control retries it, so an unwrapped throw here has no
        // user action to be reported against — it must simply not reach the process's handler.
        provinces.throwOnBackfill = IllegalStateException("Room is corrupt")
        provinces.passport.value = TravelPassport(listOf(stamp("hanoi", count = 1)))

        val vm = viewModel()
        runCurrent()

        assertEquals(1, provinces.backfillCalls)
        assertFalse(vm.state.value.isLoading, "the map still loads")
        assertEquals(1, vm.state.value.passport.stamps.size)
    }

    @Test
    fun `selecting a province drops the previous one's photographs at once`() = runTest {
        provinces.passport.value =
            TravelPassport(listOf(stamp("hanoi", count = 1), stamp("hue", count = 1)))
        // Only Hanoi has answered so far. Hue's query is still outstanding, which is the state
        // the sheet is actually in for a frame after every tap.
        discoveries.provinceDiscoveries.value = mapOf("hanoi" to listOf(discovery(id = "d1")))
        val vm = viewModel()
        runCurrent()

        vm.onIntent(PassportIntent.SelectProvince("hanoi"))
        runCurrent()
        assertEquals(1, vm.state.value.selectedDiscoveries.size)

        vm.onIntent(PassportIntent.SelectProvince("hue"))
        runCurrent()

        assertTrue(
            vm.state.value.selectedDiscoveries.isEmpty(),
            "the sheet must never show one province's photos under another's name",
        )
        assertEquals("hue", vm.state.value.selectedProvinceId)

        // And when Hue's query does land, it is Hue's photographs that appear.
        discoveries.provinceDiscoveries.value =
            discoveries.provinceDiscoveries.value + ("hue" to listOf(discovery(id = "d2")))
        runCurrent()

        assertEquals(listOf("d2"), vm.state.value.selectedDiscoveries.map { it.id })
    }

    @Test
    fun `re-selecting the province already open changes nothing`() = runTest {
        provinces.passport.value = TravelPassport(listOf(stamp("hanoi", count = 1)))
        discoveries.provinceDiscoveries.value = mapOf("hanoi" to listOf(discovery(id = "d1")))
        val vm = viewModel()
        runCurrent()
        vm.onIntent(PassportIntent.SelectProvince("hanoi"))
        runCurrent()

        vm.onIntent(PassportIntent.SelectProvince("hanoi"))

        assertEquals(1, vm.state.value.selectedDiscoveries.size, "the list is not blinked away")
    }

    @Test
    fun `dismissing clears both the selection and its list`() = runTest {
        provinces.passport.value = TravelPassport(listOf(stamp("hanoi", count = 1)))
        discoveries.provinceDiscoveries.value = mapOf("hanoi" to listOf(discovery(id = "d1")))
        val vm = viewModel()
        runCurrent()
        vm.onIntent(PassportIntent.SelectProvince("hanoi"))
        runCurrent()

        vm.onIntent(PassportIntent.DismissSelection)
        runCurrent()

        assertNull(vm.state.value.selectedProvinceId)
        assertTrue(vm.state.value.selectedDiscoveries.isEmpty())
    }

    @Test
    fun `captures with no location are told apart from no captures at all`() = runTest {
        journal.stats.value = JournalStats(
            totalDiscoveries = 20,
            totalDays = 3,
            favoriteCount = 1,
            categoryBreakdown = emptyMap(),
        )
        val vm = viewModel()
        runCurrent()

        assertTrue(
            vm.state.value.hasDiscoveriesButNoLocation,
            "twenty discoveries and no stamps is a permission problem",
        )
    }

    @Test
    fun `a cover that cannot be read costs the photograph and not the screen`() = runTest {
        captures.throwOnRead = IllegalStateException("The file is gone")
        provinces.passport.value = TravelPassport(
            listOf(stamp("hanoi", count = 1, cover = "/captures/gone.jpg")),
        )

        val vm = viewModel()
        runCurrent()

        assertTrue(vm.state.value.covers.isEmpty())
        assertFalse(vm.state.value.isLoading, "the map is still drawn")
    }

    private fun stamp(id: String, count: Int, cover: String? = null) = PassportStamp(
        province = province(id),
        discoveryCount = count,
        coverImagePath = cover,
        firstVisitAt = null,
        lastVisitAt = null,
    )

    private fun province(id: String) = Province(
        id = id,
        name = id,
        nameEn = id,
        type = ProvinceType.CITY,
        mergedFrom = emptyList(),
        center = GeoPoint(21.0, 105.8),
        bounds = GeoBounds(105.0, 20.5, 106.0, 21.5),
        mainlandBounds = GeoBounds(105.0, 20.5, 106.0, 21.5),
        mainlandRings = emptyList(),
        offshoreRings = emptyList(),
    )

    private fun TestScope.viewModel() = PassportViewModel(
        observePassport = ObserveTravelPassportUseCase(provinces),
        observeJournalStats = ObserveJournalStatsUseCase(journal),
        observeSettings = ObserveSettingsUseCase(settings),
        observeProvinceDiscoveries = ObserveProvinceDiscoveriesUseCase(discoveries),
        backfillProvinces = BackfillProvincesUseCase(provinces),
        captureStore = captures,
    )
}
