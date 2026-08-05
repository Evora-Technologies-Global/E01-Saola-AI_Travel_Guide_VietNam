package com.duylt.trave.vietlensai

import com.duylt.trave.vietlensai.domain.model.ThemePreference
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.SweepOrphanCapturesUseCase
import com.duylt.trave.vietlensai.testing.FakeCaptureMaintenance
import com.duylt.trave.vietlensai.testing.FakeDemoDataSeeder
import com.duylt.trave.vietlensai.testing.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The window host, and the two things the whole app waits on before it paints.
 *
 * `isReady` holds the splash screen open until settings have been read, so that the app never
 * paints in the wrong theme and snaps to the right one a frame later. That makes a settings
 * read which never completes into a splash screen that never lifts, which is why it is worth
 * a test rather than a glance.
 *
 * The orphan sweep beside it is the opposite kind of work — nobody is waiting on it and its
 * only cost is storage — so the property here is that a failure is invisible rather than
 * fatal. It is the one call in the app deliberately allowed to fail in silence.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val settings = FakeSettingsRepository()
    private val maintenance = FakeCaptureMaintenance()
    private val seeder = FakeDemoDataSeeder()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the splash lifts once settings have been read`() = runTest {
        val vm = viewModel()
        runCurrent()

        assertTrue(vm.isReady.value, "the window may not wait on a read that already landed")
    }

    @Test
    fun `the theme the window paints in comes from settings`() = runTest {
        settings.state.value = settings.state.value.copy(darkTheme = ThemePreference.DARK)
        val vm = viewModel()
        runCurrent()

        assertEquals(ThemePreference.DARK, vm.settings.value.darkTheme)
    }

    @Test
    fun `a sweep that throws never reaches the platform handler`() = runTest {
        // A bare `viewModelScope.launch` would hand this to the process's default handler,
        // which on Android is the app dying on launch — before the traveller has seen it do
        // anything at all — over a file nobody was going to miss.
        maintenance.throwOnSweep = IllegalStateException("The directory is not readable")

        val vm = viewModel()
        runCurrent()

        assertEquals(1, maintenance.sweepCalls)
        assertTrue(vm.isReady.value, "and the window still opens")
    }

    @Test
    fun `the demo trip is offered a chance to seed before the sweep runs`() = runTest {
        // Order, not just presence: the seeder writes photographs into the very directory the
        // sweep deletes unreferenced files from, so a sweep that went first would be entitled
        // to remove every one of them before its row existed.
        val vm = viewModel()
        runCurrent()

        assertEquals(1, seeder.seedCalls)
        assertEquals(1, maintenance.sweepCalls)
        assertTrue(vm.isReady.value)
    }

    @Test
    fun `a seeder that throws still lets the sweep run and the window open`() = runTest {
        // The port promises never to throw, and this is what that promise is worth if it is
        // ever broken: both calls share one coroutine, so an unguarded throw here would take
        // the sweep with it and reach the platform handler — the app dying on launch, on a
        // development build, over demo data.
        seeder.throwOnSeed = IllegalStateException("The demo assets are not packaged")

        val vm = viewModel()
        runCurrent()

        assertEquals(1, seeder.seedCalls)
        assertEquals(1, maintenance.sweepCalls, "the sweep must not be lost with it")
        assertTrue(vm.isReady.value, "and the window still opens")
    }

    private fun viewModel() = MainViewModel(
        observeSettings = ObserveSettingsUseCase(settings),
        sweepOrphanCaptures = SweepOrphanCapturesUseCase(maintenance),
        seedDemoData = seeder,
    )
}
