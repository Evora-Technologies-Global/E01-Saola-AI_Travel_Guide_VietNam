package com.evora.technologies.saola.feature.explore

import app.cash.turbine.test
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.usecase.GetCurrentLocationUseCase
import com.evora.technologies.saola.domain.usecase.LoadNearbyPlacesUseCase
import com.evora.technologies.saola.domain.usecase.LoadPlaceDetailsUseCase
import com.evora.technologies.saola.domain.usecase.ObserveSettingsUseCase
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import com.evora.technologies.saola.testing.FakeLocationRepository
import com.evora.technologies.saola.testing.FakePlaceRepository
import com.evora.technologies.saola.testing.FakeSettingsRepository
import com.evora.technologies.saola.testing.nearbyPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Whether the Explore tab can ever be stranded.
 *
 * This screen is the one with the most ways to be waiting at once — a location fix, a place
 * search and a details fetch, two of which raise a flag that also guards re-entry. That
 * combination is what makes it worth its own suite: a flag raised before a call and lowered
 * only on the paths somebody thought of is not a cosmetic spinner, it is a screen that has
 * stopped accepting the one action that would fix it. The MVI doc's §7 "stuck states" row is
 * this exact shape, and `LLM.md` §11 row #24 is the same defect already found in the chat.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {

    private val location = FakeLocationRepository()
    private val places = FakePlaceRepository()
    private val settings = FakeSettingsRepository()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        location.permission = true
        location.fix = AppResult.Success(GeoPoint(21.0287, 105.8524))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `granting the permission starts the first search`() = runTest {
        places.places = AppResult.Success(listOf(nearbyPlace()))
        val vm = viewModel()

        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()

        assertEquals(1, places.nearbyCalls)
        assertEquals(1, vm.state.value.places.size)
        assertFalse(vm.state.value.isLoading)
        assertNotNull(vm.state.value.center, "the map is drawn from the fix alone")
    }

    @Test
    fun `losing the permission takes the open sheet with it`() = runTest {
        places.places = AppResult.Success(listOf(nearbyPlace()))
        val vm = viewModel()
        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()
        vm.onIntent(ExploreIntent.SelectPlace("p1"))
        runCurrent()
        assertEquals("p1", vm.state.value.selectedPlaceId)

        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = false))
        runCurrent()

        assertNull(vm.state.value.selectedPlaceId, "a sheet must not outlive the map behind it")
        assertNull(vm.state.value.details)
    }

    @Test
    fun `a fix that fails is reported and both spinners come down`() = runTest {
        location.fix = AppResult.Failure(AppError.LocationUnavailable)
        val vm = viewModel()

        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()

        assertEquals(AppError.LocationUnavailable, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
        assertFalse(vm.state.value.isRefreshing)
        assertEquals(0, places.nearbyCalls, "no search without a centre to search around")
    }

    @Test
    fun `a search that throws leaves the screen able to try again`() = runTest {
        // The handled failure is covered above. This is the unwrapped one — a mapper meeting a
        // payload it did not expect — which reaches `launchSafely`'s onError rather than either
        // AppResult arm. The flag raised before the call is not on that path, and because
        // `search` refuses to start while it is up, a screen that never lowers it has retired
        // its own retry button for the life of the process.
        places.throwOnNearby = IllegalStateException("Overpass returned something else")
        val vm = viewModel()

        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()

        assertNotNull(vm.state.value.error, "an unwrapped throw must be reported")
        assertFalse(vm.state.value.isLoading, "the whole-screen spinner must come down")
        assertFalse(vm.state.value.isRefreshing)

        // The proof that it matters: the retry has to actually reach the repository.
        places.throwOnNearby = null
        places.places = AppResult.Success(listOf(nearbyPlace()))
        vm.onIntent(ExploreIntent.Refresh(force = true))
        runCurrent()

        assertEquals(2, places.nearbyCalls, "a retry after a throw must reach the repository")
        assertEquals(1, vm.state.value.places.size)
    }

    @Test
    fun `a refresh that fails keeps the markers already on the map`() = runTest {
        places.places = AppResult.Success(listOf(nearbyPlace()))
        val vm = viewModel()
        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()

        places.places = AppResult.Failure(AppError.NoConnection)
        vm.onIntent(ExploreIntent.Refresh(force = true))
        runCurrent()

        assertEquals(1, vm.state.value.places.size, "good markers are not thrown away")
        assertEquals(AppError.NoConnection, vm.state.value.error)
    }

    @Test
    fun `a failed refresh over a populated map is spoken rather than drawn`() = runTest {
        places.places = AppResult.Success(listOf(nearbyPlace()))
        val vm = viewModel()
        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()

        places.places = AppResult.Failure(AppError.NoConnection)
        vm.effects.test {
            vm.onIntent(ExploreIntent.Refresh(force = true))
            runCurrent()
            assertTrue(awaitItem() is ExploreEffect.ShowError)
        }
    }

    @Test
    fun `re-selecting the place already open does not restart its request`() = runTest {
        places.places = AppResult.Success(listOf(nearbyPlace()))
        val vm = viewModel()
        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()

        vm.onIntent(ExploreIntent.SelectPlace("p1"))
        runCurrent()
        vm.onIntent(ExploreIntent.SelectPlace("p1"))
        runCurrent()

        assertEquals(listOf("p1"), places.detailsAskedFor)
    }

    @Test
    fun `a refresh that drops the open place clears the selection`() = runTest {
        places.places = AppResult.Success(listOf(nearbyPlace(id = "p1")))
        val vm = viewModel()
        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()
        vm.onIntent(ExploreIntent.SelectPlace("p1"))
        runCurrent()

        places.places = AppResult.Success(listOf(nearbyPlace(id = "p2")))
        vm.onIntent(ExploreIntent.Refresh(force = true))
        runCurrent()

        assertNull(vm.state.value.selectedPlaceId, "a sheet with no marker under it must close")
        assertNull(vm.state.value.details)
        assertFalse(vm.state.value.isLoadingDetails)
    }

    @Test
    fun `a slow answer for a place since closed is discarded`() = runTest {
        places.places = AppResult.Success(listOf(nearbyPlace(id = "p1"), nearbyPlace(id = "p2")))
        places.detailsDelayMillis = 500
        val vm = viewModel()
        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()

        vm.onIntent(ExploreIntent.SelectPlace("p1"))
        runCurrent()
        vm.onIntent(ExploreIntent.DismissSelection)
        advanceTimeBy(1_000)
        runCurrent()

        assertNull(vm.state.value.selectedPlaceId, "a closed sheet must not reopen itself")
        assertNull(vm.state.value.details)
    }

    @Test
    fun `a details request that throws leaves the sheet readable`() = runTest {
        // Same shape as the search above and the same cost in miniature — the sheet keeps its
        // own spinner, and nothing else on this screen will ever lower it.
        places.places = AppResult.Success(listOf(nearbyPlace()))
        val vm = viewModel()
        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()

        places.throwOnDetails = IllegalStateException("Wikipedia returned something else")
        vm.onIntent(ExploreIntent.SelectPlace("p1"))
        runCurrent()

        assertFalse(vm.state.value.isLoadingDetails, "the sheet's spinner must come down")
    }

    @Test
    fun `recentring asks for a fresh fix rather than reusing the search centre`() = runTest {
        places.places = AppResult.Success(listOf(nearbyPlace()))
        val vm = viewModel()
        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()

        val moved = GeoPoint(21.05, 105.90)
        location.fix = AppResult.Success(moved)
        vm.onIntent(ExploreIntent.RecenterOnUser)
        runCurrent()

        assertEquals(moved, vm.state.value.center)
        assertEquals(moved, vm.state.value.camera?.target)
    }

    @Test
    fun `a camera move is retired once the map has carried it out`() = runTest {
        places.places = AppResult.Success(listOf(nearbyPlace()))
        val vm = viewModel()
        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()
        assertNotNull(vm.state.value.camera)

        vm.onIntent(ExploreIntent.CameraApplied)
        runCurrent()

        assertNull(vm.state.value.camera, "a move left in state is re-applied on every return")
    }

    @Test
    fun `navigation is an effect and never a flag in state`() = runTest {
        places.places = AppResult.Success(listOf(nearbyPlace()))
        val vm = viewModel()
        vm.onIntent(ExploreIntent.PermissionResolved(isGranted = true))
        runCurrent()

        vm.effects.test {
            vm.onIntent(ExploreIntent.StartNavigation("p1"))
            runCurrent()
            assertTrue(awaitItem() is ExploreEffect.OpenUrl)
        }
    }

    @Test
    fun `every intent in any order is survivable`() = runTest {
        // The standard for a view model with more than two concurrent jobs, per §9. Seeded so a
        // failure is reproducible; the assertion is only that nothing escapes to kill the app.
        val everyIntent = listOf(
            ExploreIntent.PermissionResolved(isGranted = true),
            ExploreIntent.PermissionResolved(isGranted = false),
            ExploreIntent.Refresh(force = true),
            ExploreIntent.Refresh(force = false),
            ExploreIntent.SelectPlace("p1"),
            ExploreIntent.SelectPlace("p2"),
            ExploreIntent.SelectPlace("missing"),
            ExploreIntent.DismissSelection,
            ExploreIntent.RecenterOnUser,
            ExploreIntent.CameraApplied,
            ExploreIntent.StartNavigation("p1"),
            ExploreIntent.StartNavigation("missing"),
        )
        places.places = AppResult.Success(listOf(nearbyPlace(id = "p1"), nearbyPlace(id = "p2")))

        val random = Random(seed = 20260804)
        repeat(times = 25) {
            val vm = viewModel()
            everyIntent.shuffled(random).forEach { intent ->
                vm.onIntent(intent)
                runCurrent()
            }
            advanceTimeBy(2_000)
            runCurrent()
        }
    }

    private fun TestScope.viewModel() = ExploreViewModel(
        getCurrentLocation = GetCurrentLocationUseCase(location),
        loadNearbyPlaces = LoadNearbyPlacesUseCase(places),
        loadPlaceDetails = LoadPlaceDetailsUseCase(places),
        observeSettings = ObserveSettingsUseCase(settings),
    )
}
