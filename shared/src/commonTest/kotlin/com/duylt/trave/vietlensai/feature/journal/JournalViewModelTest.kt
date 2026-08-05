package com.duylt.trave.vietlensai.feature.journal

import app.cash.turbine.test
import com.duylt.trave.vietlensai.domain.usecase.GenerateDaySummaryUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveCollectionUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveJournalStatsUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveJournalUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveTravelPassportUseCase
import com.duylt.trave.vietlensai.domain.usecase.ToggleFavoriteUseCase
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.testing.FakeCatalogRepository
import com.duylt.trave.vietlensai.testing.FakeDiscoveryRepository
import com.duylt.trave.vietlensai.testing.FakeJournalRepository
import com.duylt.trave.vietlensai.testing.FakeProvinceRepository
import com.duylt.trave.vietlensai.testing.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * A day summary that fails must say so.
 *
 * This screen has no inline error anywhere: while a story is being written the only thing
 * that moves is the spinner on the day header, and it stops the same way whether the story
 * arrived or not. So the effect *is* the report — if it is not raised, or is raised carrying
 * nothing the route can turn into words, the traveller waits, sees the spinner stop, and is
 * told nothing at all. That shipped: the failure went into a `JournalState.error` field the
 * screen resolved during composition and the collector read a frame too early, so the message
 * was always the stale null. See `LLM.md` §11 row #15.
 *
 * These tests pin the two halves that fix keeps: the error travels *on* the effect, and every
 * way the generation can fail raises one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModelTest {

    private lateinit var journalRepository: FakeJournalRepository
    private lateinit var discoveryRepository: FakeDiscoveryRepository
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var provinceRepository: FakeProvinceRepository
    private lateinit var catalogRepository: FakeCatalogRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        journalRepository = FakeJournalRepository()
        discoveryRepository = FakeDiscoveryRepository()
        settingsRepository = FakeSettingsRepository()
        provinceRepository = FakeProvinceRepository()
        catalogRepository = FakeCatalogRepository()
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = JournalViewModel(
        observeJournal = ObserveJournalUseCase(journalRepository),
        observeStats = ObserveJournalStatsUseCase(journalRepository),
        observeSettings = ObserveSettingsUseCase(settingsRepository),
        observePassport = ObserveTravelPassportUseCase(provinceRepository),
        observeCollection = ObserveCollectionUseCase(catalogRepository),
        toggleFavorite = ToggleFavoriteUseCase(discoveryRepository),
        generateDaySummary = GenerateDaySummaryUseCase(journalRepository),
    )

    /**
     * The ordinary failure: the repository answers with an [AppError] rather than throwing.
     *
     * The assertion that matters is the payload, not just the type. A `ShowMessage` with no
     * error on it would leave the route back where it started — reading state that has not
     * been recomposed yet.
     */
    @Test
    fun `a failed day summary raises the failure on the effect`() = runTest(timeout = 30.seconds) {
        journalRepository.failOnGenerate = AppError.NoConnection
        val vm = viewModel()

        vm.effects.test {
            vm.onIntent(JournalIntent.GenerateSummary(DAY))
            runCurrent()

            val effect = awaitItem()
            assertTrue(effect is JournalEffect.ShowMessage, "a failure has to be reported — got $effect")
            assertEquals(
                AppError.NoConnection,
                effect.error,
                "the route turns this into words itself, so the error has to ride on the effect",
            )
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(
            vm.state.value.generatingDate,
            "the day header must stop spinning even though the story never arrived",
        )
    }

    /**
     * The crash floor reports too.
     *
     * An unwrapped throw skips every `when` branch below it, so before this it only lowered
     * the spinner — indistinguishable from success. `launchSafely`'s `onError` now raises the
     * same effect an ordinary failure does.
     *
     * The name has no comma in it, and that is not a style choice: Kotlin/Native rejects `,`
     * `.` `;` `:` and brackets inside a backticked identifier outright — *"Name contains
     * illegal characters"* — while the JVM accepts all of them. This test read
     * "…is reported, not swallowed" and was the second thing keeping `commonTest` off iOS,
     * hidden behind the first until it was removed. `LLM.md` §11 row #14.
     */
    @Test
    fun `an exception while writing the story is reported rather than swallowed`() =
        runTest(timeout = 30.seconds) {
            journalRepository.throwOnGenerate = IllegalStateException("model exploded")
            val vm = viewModel()

            vm.effects.test {
                vm.onIntent(JournalIntent.GenerateSummary(DAY))
                runCurrent()

                val effect = awaitItem()
                assertTrue(
                    effect is JournalEffect.ShowMessage && effect.error is AppError.Unexpected,
                    "an unwrapped throw must reach the traveller as a message — got $effect",
                )
                cancelAndIgnoreRemainingEvents()
            }

            assertNull(vm.state.value.generatingDate, "the spinner must come down after a throw")
        }

    /** A favourite that could not be written is the screen's other failure path. */
    @Test
    fun `a failed favourite toggle is reported`() = runTest(timeout = 30.seconds) {
        discoveryRepository.throwOnToggleFavorite = IllegalStateException("database gone")
        val vm = viewModel()

        vm.effects.test {
            vm.onIntent(JournalIntent.ToggleFavorite("discovery-1"))
            runCurrent()

            val effect = awaitItem()
            assertTrue(
                effect is JournalEffect.ShowMessage,
                "a favourite that did not stick must not look like one that did — got $effect",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * One narrative at a time.
     *
     * Two in flight would let a slow one overwrite a fresh one when it lands, and would raise
     * two messages for what the traveller did once.
     */
    @Test
    fun `a second request while one is in flight is ignored`() = runTest(timeout = 30.seconds) {
        journalRepository.generateDelayMillis = 5_000
        journalRepository.failOnGenerate = AppError.Timeout
        val vm = viewModel()

        vm.onIntent(JournalIntent.GenerateSummary(DAY))
        runCurrent()
        vm.onIntent(JournalIntent.GenerateSummary(DAY))
        runCurrent()

        assertEquals(1, journalRepository.generateCalls, "the second press must not start a second call")

        advanceTimeBy(10_000)
        runCurrent()
        assertNull(vm.state.value.generatingDate)
    }

    /** A success leaves no message behind: the story itself arrives through `observeJournal()`. */
    @Test
    fun `a successful summary raises nothing`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()

        vm.effects.test {
            vm.onIntent(JournalIntent.GenerateSummary(DAY))
            advanceTimeBy(1_000)
            runCurrent()

            expectNoEvents()
        }
        assertNull(vm.state.value.generatingDate)
    }

    private companion object {
        val DAY = LocalDate(2026, 8, 3)
    }
}
