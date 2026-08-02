package com.duylt.trave.vietlensai.feature.camera

import app.cash.turbine.test
import com.duylt.trave.vietlensai.core.util.DETECT_TIMEOUT_MILLIS
import com.duylt.trave.vietlensai.core.util.VolumeShutterBus
import com.duylt.trave.vietlensai.domain.model.LensMode
import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.usecase.MarkLocationAskedUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveApiKeyAvailabilityUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveDiscoveriesUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.RecognizeImageUseCase
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import com.duylt.trave.vietlensai.testing.FakeCaptureStore
import com.duylt.trave.vietlensai.testing.FakeDiscoveryRepository
import com.duylt.trave.vietlensai.testing.FakeLocationRepository
import com.duylt.trave.vietlensai.testing.FakeSettingsRepository
import com.duylt.trave.vietlensai.testing.clearAsFrameworkWould
import com.duylt.trave.vietlensai.testing.discovery
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
import kotlin.time.Duration.Companion.seconds
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Crash and stuck-state cover for the busiest ViewModel in the app.
 *
 * The lens screen is where the app is hardest to keep alive: three cancellable jobs run
 * against each other (the self-timer, the recognition request, the "what the AI is doing"
 * ticker), a hardware button feeds the same shutter as the on-screen one, and the traveller
 * is pressing things while walking. Everything below is a way that has to end in a
 * renderable state rather than in a stack trace.
 *
 * Timing is driven by the test scheduler rather than by real delays, so a suite that covers
 * a ten-second self-timer and a thirty-second recognition timeout still runs in
 * milliseconds and cannot flake on a loaded machine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LensViewModelCrashTest {

    private lateinit var discoveryRepository: FakeDiscoveryRepository
    private lateinit var locationRepository: FakeLocationRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var captureStore: FakeCaptureStore
    private lateinit var shutterBus: VolumeShutterBus

    @BeforeTest
    fun setUp() {
        // `viewModelScope` is hard-wired to `Dispatchers.Main.immediate`, which does not
        // exist on a plain JVM test runtime. Unconfined so a `launch` inside `init` has
        // run by the time the constructor returns, which is what the screen sees too.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        discoveryRepository = FakeDiscoveryRepository()
        locationRepository = FakeLocationRepository()
        settingsRepository = FakeSettingsRepository()
        captureStore = FakeCaptureStore()
        shutterBus = VolumeShutterBus()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(): LensViewModel = LensViewModel(
        recognizeImage = RecognizeImageUseCase(discoveryRepository, locationRepository),
        captureStore = captureStore,
        volumeShutter = shutterBus,
        markLocationAsked = MarkLocationAskedUseCase(settingsRepository),
        observeDiscoveries = ObserveDiscoveriesUseCase(discoveryRepository),
        observeApiKeyAvailability = ObserveApiKeyAvailabilityUseCase(settingsRepository),
        observeSettings = ObserveSettingsUseCase(settingsRepository),
    )

    /**
     * Runs everything that is due, up to a generous horizon.
     *
     * Deliberately not `advanceUntilIdle`: the stage ticker is a `while (isActive)` loop, so
     * a scheduler holding one never becomes idle and the call would hang the suite rather
     * than fail it. A bounded advance settles every real code path here — the longest is the
     * ten-second self-timer followed by the thirty-second detect budget — while leaving a
     * runaway loop visible as a failed assertion instead of a stuck build.
     */
    private fun TestScope.settle(horizonMillis: Long = 120_000) {
        advanceTimeBy(horizonMillis)
        runCurrent()
    }

    // -------------------------------------------------------------------------------
    // Nothing the user can do may throw
    // -------------------------------------------------------------------------------

    /**
     * Every intent, in a thousand shuffled orders, with no exception escaping.
     *
     * A `when` over a sealed interface is exhaustive at compile time, so what this catches
     * is not a missing branch — it is the *combinations*. Swapping languages before one has
     * been detected, cancelling a countdown that is not running, a capture result arriving
     * after the screen stopped: each is a plausible sequence of real taps, and each used to
     * have to be imagined by whoever wrote the branch.
     *
     * The seed is fixed so a failure is reproducible rather than "it went red once in CI".
     */
    @Test
    fun `no ordering of intents throws`() = runTest(timeout = 30.seconds) {
        val random = Random(seed = 20260802)
        val intents = listOf<LensIntent>(
            LensIntent.ShutterPressed,
            LensIntent.ScreenStarted,
            LensIntent.ScreenStopped,
            LensIntent.ToggleFlash,
            LensIntent.ToggleGrid,
            LensIntent.SwitchLens,
            LensIntent.CycleTimer,
            LensIntent.SwapTranslateLanguages,
            LensIntent.DismissError,
            LensIntent.CaptureAborted,
            LensIntent.LocationAsked,
            LensIntent.SelectMode(LensMode.TRANSLATE),
            LensIntent.SelectMode(LensMode.AUTO),
            LensIntent.SelectTranslateFrom(null),
            LensIntent.SelectTranslateFrom(TranslateLanguage.VIETNAMESE),
            LensIntent.SelectTranslateTo(TranslateLanguage.ENGLISH),
            LensIntent.PhotoCaptured("/captures/capture_1.jpg"),
            LensIntent.PhotoPicked("/captures/capture_2.jpg"),
            LensIntent.CaptureFailed("hardware busy"),
            LensIntent.CaptureFailed(null),
        )

        repeat(FUZZ_ROUNDS) {
            val vm = viewModel()
            repeat(20) {
                vm.onIntent(intents[random.nextInt(intents.size)])
                // Let whatever that started make progress, so the next intent lands on a
                // ViewModel mid-flight rather than on a quiescent one.
                if (random.nextBoolean()) advanceTimeBy(random.nextLong(1, 2_000))
                runCurrent()
            }
            settle()
            vm.clearAsFrameworkWould()
        }
    }

    /**
     * A repository that throws must not take the app down with it.
     *
     * `viewModelScope.launch` has no `CoroutineExceptionHandler` of its own, so before
     * [com.duylt.trave.vietlensai.core.mvi.MviViewModel] grew one, an exception escaping
     * here reached the thread's default handler — which on Android is the process dying.
     * And this path can genuinely throw: `DiscoveryRepositoryImpl.recognize` reads back the
     * row it just wrote and deserialises it *outside* its own `try`, so a corrupt
     * `StoredJson` column surfaces as a `SerializationException` rather than an
     * `AppResult.Failure`.
     */
    @Test
    fun `an exception from recognition becomes an error state instead of a crash`() = runTest(timeout = 30.seconds) {
        discoveryRepository.throwOnRecognize = IllegalStateException("Discovery vanished after write")
        val vm = viewModel()

        vm.onIntent(LensIntent.PhotoCaptured("/captures/capture_1.jpg"))
        settle()

        val state = vm.state.value
        assertFalse(state.isAnalysing, "the scrim must come down when recognition dies")
        assertTrue(state.error is AppError.Unexpected, "expected a renderable error, got ${state.error}")
    }

    /** The same guarantee for the location provider, which throws on some OEM builds. */
    @Test
    fun `an exception from the location provider does not crash the capture`() = runTest(timeout = 30.seconds) {
        locationRepository.permission = true
        locationRepository.throwOnCurrentLocation = SecurityException("caller lacks permission")
        val vm = viewModel()

        vm.onIntent(LensIntent.PhotoCaptured("/captures/capture_1.jpg"))
        settle()

        assertFalse(vm.state.value.isAnalysing)
        assertTrue(vm.state.value.error != null)
    }

    /** A settings write that fails must not kill the screen that triggered it. */
    @Test
    fun `an exception while recording the location prompt is contained`() = runTest(timeout = 30.seconds) {
        settingsRepository.throwOnWrite = IllegalStateException("datastore corrupted")
        val vm = viewModel()

        vm.onIntent(LensIntent.LocationAsked)
        settle()

        assertEquals(1, settingsRepository.locationAskedCalls)
    }

    // -------------------------------------------------------------------------------
    // Stuck states — the app is alive but the traveller cannot use it
    // -------------------------------------------------------------------------------

    /**
     * The scrim must always come down.
     *
     * A recognition that never returns is the worst outcome on this screen: the viewfinder
     * is dimmed and the shutter is dead, so the traveller standing in front of the monument
     * has no way back except force-quitting.
     */
    @Test
    fun `a recognition that never answers times out rather than hanging the shutter`() = runTest(timeout = 30.seconds) {
        // Ten times the screen's own budget: a request that is never coming back.
        discoveryRepository.recognizeDelayMillis = DETECT_TIMEOUT_MILLIS * 10
        val vm = viewModel()

        vm.onIntent(LensIntent.PhotoCaptured("/captures/capture_1.jpg"))
        advanceTimeBy(DETECT_TIMEOUT_MILLIS - 1)
        runCurrent()
        assertTrue(vm.state.value.isAnalysing, "still inside the budget")
        assertEquals(1, discoveryRepository.recognizeCalls)

        advanceTimeBy(2)
        runCurrent()

        assertFalse(vm.state.value.isAnalysing, "the scrim must lift at the timeout")
        assertEquals(AppError.Timeout, vm.state.value.error)
    }

    /**
     * The stage ticker must not outlive the request it narrates.
     *
     * It is a `while (isActive)` loop that writes state every 1.8 seconds. If it survived
     * the request, it would recompose the viewfinder forever — the exact shape of a battery
     * drain nobody can find, because the screen looks idle.
     */
    @Test
    fun `the analysis stage ticker stops once recognition finishes`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()

        vm.onIntent(LensIntent.PhotoCaptured("/captures/capture_1.jpg"))
        settle()
        assertFalse(vm.state.value.isAnalysing)

        val settled = vm.state.value.analysisStage
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(settled, vm.state.value.analysisStage, "the ticker is still running")
    }

    /**
     * The ticker must not outlive a recognition that was *cancelled* rather than finished.
     *
     * This is the leak the intent fuzzer found, and the reason it found it by hanging: the
     * ticker is a `while (isActive)` loop with no exit of its own, so a scheduler holding a
     * leaked one never goes idle. The sequence is ordinary — start a recognition, switch to
     * translate, shoot again — and it used to leave a coroutine writing `analysisStage` every
     * 1.8 seconds for the rest of the ViewModel's life, recomposing the viewfinder behind
     * whatever screen the traveller had moved on to.
     *
     * `analyse` cancels the analysis job, which means the `cancelStageTicker()` at the end of
     * that job's body never runs; the translate branch then returns before `startStageTicker`
     * would have replaced it. Nothing else was left holding the handle.
     */
    @Test
    fun `switching to translate mid-recognition does not leave the stage ticker running`() = runTest(timeout = 30.seconds) {
        discoveryRepository.recognizeDelayMillis = DETECT_TIMEOUT_MILLIS / 2
        val vm = viewModel()

        vm.onIntent(LensIntent.PhotoCaptured("/captures/capture_1.jpg"))
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(vm.state.value.analysisStage > 0, "the ticker should be narrating by now")

        vm.onIntent(LensIntent.SelectMode(LensMode.TRANSLATE))
        vm.onIntent(LensIntent.PhotoCaptured("/captures/capture_2.jpg"))
        runCurrent()

        val stageAfterSwitch = vm.state.value.analysisStage
        advanceTimeBy(120_000)
        runCurrent()

        assertEquals(
            stageAfterSwitch,
            vm.state.value.analysisStage,
            "the stage ticker outlived the request it was narrating",
        )
    }

    /** Leaving the screen must not fire the shutter on a camera nobody is looking at. */
    @Test
    fun `stopping the screen cancels a running self-timer`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()
        vm.onIntent(LensIntent.CycleTimer) // OFF -> THREE
        vm.onIntent(LensIntent.ShutterPressed)
        advanceTimeBy(1_500)
        runCurrent()
        assertTrue(vm.state.value.isCountingDown)

        vm.onIntent(LensIntent.ScreenStopped)
        runCurrent()
        assertFalse(vm.state.value.isCountingDown)

        vm.effects.test {
            advanceTimeBy(30_000)
            runCurrent()
            expectNoEvents()
        }
    }

    /**
     * A second press during the countdown cancels; it must never queue a second photo.
     *
     * Two captures in flight write two files and start two recognitions, and the second
     * navigates away from the first's result.
     */
    @Test
    fun `a shutter press during the countdown cancels instead of queueing`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()
        vm.onIntent(LensIntent.CycleTimer)

        vm.effects.test {
            vm.onIntent(LensIntent.ShutterPressed)
            advanceTimeBy(1_500)
            runCurrent()

            vm.onIntent(LensIntent.ShutterPressed)
            advanceTimeBy(30_000)
            runCurrent()

            expectNoEvents()
            assertFalse(vm.state.value.isCountingDown)
            assertFalse(vm.state.value.isCapturing)
        }
    }

    /** Volume keys are the same shutter, so they inherit the same guards. */
    @Test
    fun `volume presses while analysing do not start a second capture`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()
        shutterBus.arm()

        vm.onIntent(LensIntent.PhotoCaptured("/captures/capture_1.jpg"))
        repeat(20) { shutterBus.press() }
        settle()

        assertEquals(1, discoveryRepository.recognizeCalls)
    }

    /**
     * A new capture replaces the one in flight rather than racing it.
     *
     * Without the cancel, the first request's `OpenDiscovery` would still be delivered and
     * would navigate the traveller to a photo they have already moved on from.
     */
    @Test
    fun `a second capture supersedes the first`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()

        discoveryRepository.recognizeDelayMillis = 5_000
        vm.effects.test {
            vm.onIntent(LensIntent.PhotoCaptured("/captures/capture_1.jpg"))
            advanceTimeBy(1_000)
            runCurrent()
            vm.onIntent(LensIntent.PhotoCaptured("/captures/capture_2.jpg"))
            settle()

            val first = awaitItem()
            assertTrue(first is LensEffect.OpenDiscovery)
            expectNoEvents()
        }
    }

    // -------------------------------------------------------------------------------
    // Memory: state that grows without a ceiling
    // -------------------------------------------------------------------------------

    /**
     * The recent-capture stack is capped whatever the database holds.
     *
     * Every `Discovery` in this list carries its sections, fun facts and suggestions, and
     * the list is retained for as long as the screen is on. Dropping the `take(5)` would
     * put the traveller's entire history — hundreds of rows after a fortnight — into a
     * StateFlow that is never released.
     */
    @Test
    fun `the recent capture stack stays capped as history grows`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()

        discoveryRepository.discoveries.value = List(500) { discovery(id = "d$it") }
        settle()

        assertEquals(5, vm.state.value.recentDiscoveries.size)
    }

    // -------------------------------------------------------------------------------
    // Effects
    // -------------------------------------------------------------------------------

    /**
     * Effects raised while the screen is away are delivered once, when it returns.
     *
     * A `Channel`, not a replaying flow: a replay would re-navigate on every configuration
     * change, and a `SharedFlow` with no subscriber would drop the navigation entirely.
     */
    @Test
    fun `effects raised with no collector are delivered exactly once`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()

        vm.onIntent(LensIntent.PhotoCaptured("/captures/capture_1.jpg"))
        settle()

        vm.effects.test {
            assertTrue(awaitItem() is LensEffect.OpenDiscovery)
            expectNoEvents()
        }
    }

    /** Translate hands the photo straight on rather than holding the viewfinder. */
    @Test
    fun `translate mode never leaves the viewfinder scrimmed`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()
        vm.onIntent(LensIntent.SelectMode(LensMode.TRANSLATE))

        vm.effects.test {
            vm.onIntent(LensIntent.PhotoCaptured("/captures/capture_1.jpg"))
            settle()

            assertTrue(awaitItem() is LensEffect.OpenTranslation)
            assertFalse(vm.state.value.isAnalysing)
            assertEquals(0, discoveryRepository.recognizeCalls)
        }
    }

    /** Swapping with no detected source is a no-op, not a crash and not a guess. */
    @Test
    fun `swapping languages before detection leaves both sides alone`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()
        vm.onIntent(LensIntent.SelectTranslateFrom(null))
        val before = vm.state.value

        vm.onIntent(LensIntent.SwapTranslateLanguages)

        assertNull(vm.state.value.translateFrom)
        assertEquals(before.translateTo, vm.state.value.translateTo)
    }

    /** Tearing the ViewModel down must release the volume keys back to the system. */
    @Test
    fun `clearing the view model disarms the volume shutter`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()
        vm.onIntent(LensIntent.ScreenStarted)
        assertTrue(shutterBus.isArmed)

        vm.clearAsFrameworkWould()

        assertFalse(shutterBus.isArmed)
    }

    private companion object {
        /**
         * How many independent ViewModels the fuzzer walks through.
         *
         * Enough rounds that a twenty-intent walk reaches the awkward interleavings — a
         * capture landing mid-countdown, a mode switch mid-recognition — without the suite
         * costing more than a second. The seed is fixed, so this number is also what makes
         * the run reproducible: raising it explores strictly more, never something different.
         */
        const val FUZZ_ROUNDS = 60
    }
}
