package com.evora.technologies.saola.feature.settings

import app.cash.turbine.test
import com.evora.technologies.saola.domain.usecase.ClearHistoryUseCase
import com.evora.technologies.saola.domain.usecase.ObserveSettingsUseCase
import com.evora.technologies.saola.domain.usecase.UpdateThemeUseCase
import com.evora.technologies.saola.domain.model.ThemePreference
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.testing.FakeDiscoveryRepository
import com.evora.technologies.saola.testing.FakeSettingsRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * The settings screen must not confirm a write that did not happen.
 *
 * The write that carries this rule is clearing the history, and it is the only one left on the
 * page: it is irreversible, it is announced in words, and nothing on screen would contradict a
 * confirmation that was wrong — the traveller is looking at Settings, not at their journal. So
 * a delete that failed used to say "all discoveries cleared" over a journal that was still
 * there, and they would only find out by walking back into it.
 *
 * It inherited the rule from the API-key card, which is where the defect was originally found
 * and which was removed on 06.08.2026 with the rest of the Intelligence section. That is why
 * `SettingsRepository`'s writes return
 * [com.evora.technologies.saola.domain.util.AppResult] at all, and why the two toggles are
 * allowed to ignore what the delete has to check: a switch redraws from the settings flow and
 * puts itself back, a deleted journal has nothing to redraw from.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var discoveryRepository: FakeDiscoveryRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        settingsRepository = FakeSettingsRepository()
        discoveryRepository = FakeDiscoveryRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(
        observeSettings = ObserveSettingsUseCase(settingsRepository),
        settingsRepository = settingsRepository,
        updateTheme = UpdateThemeUseCase(settingsRepository),
        clearHistory = ClearHistoryUseCase(discoveryRepository),
    )

    @Test
    fun `a history that cleared is confirmed and closes the dialog`() =
        runTest(timeout = 30.seconds) {
            val vm = viewModel()

            vm.effects.test {
                vm.onIntent(SettingsIntent.RequestClearHistory)
                assertTrue(vm.state.value.showClearConfirm, "guard: the dialog is up")

                vm.onIntent(SettingsIntent.ConfirmClearHistory)
                runCurrent()

                assertEquals(SettingsEffect.HistoryCleared, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertFalse(vm.state.value.showClearConfirm)
            assertTrue(discoveryRepository.deleteAllCalls > 0, "the delete has to have run")
        }

    /**
     * The defect this suite exists for, one screen element on from where it was found.
     *
     * Announced unconditionally — which is how `ConfirmClearHistory` was written until
     * 06.08.2026 — every assertion below fails: the effect is `HistoryCleared` and the
     * traveller is told their whole journal is gone while every photograph is still on the
     * device.
     */
    @Test
    fun `a history that failed to clear is not confirmed and reports the failure`() =
        runTest(timeout = 30.seconds) {
            discoveryRepository.failOnDeleteAll = AppError.Storage("database is locked")
            val vm = viewModel()

            vm.effects.test {
                vm.onIntent(SettingsIntent.ConfirmClearHistory)
                runCurrent()

                val effect = awaitItem()
                assertTrue(
                    effect is SettingsEffect.ShowMessage,
                    "a failed delete must report the failure, not confirm it — got $effect",
                )
                // The error rides on the effect rather than being read back out of state.
                // This assertion used to be `vm.state.value.error`, on the reasoning that
                // "the screen resolves its message from state" — which was the bug: the
                // route resolves during composition, and the effect is handled a
                // main-queue turn before the next frame, so the message was always the
                // stale null. See `AppError.userMessage` and `LLM.md` §11 row #15.
                assertEquals(
                    AppError.Storage("database is locked"),
                    effect.error,
                    "the message the screen shows comes from the effect, so the error has to be on it",
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    /**
     * A throw is the other half of the contract, and it must not reach the platform handler.
     *
     * `launchSafely` is the floor: `viewModelScope` carries no `CoroutineExceptionHandler`,
     * so an escaping exception here would take the process down.
     */
    @Test
    fun `an exception while clearing the history does not escape the ViewModel`() =
        runTest(timeout = 30.seconds) {
            discoveryRepository.throwOnDeleteAll = IllegalStateException("database file locked")
            val vm = viewModel()

            vm.onIntent(SettingsIntent.RequestClearHistory)
            vm.onIntent(SettingsIntent.ConfirmClearHistory)
            runCurrent()

            // Reaching this line at all is the assertion: the test worker is still alive. The
            // dialog is down either way — it is lowered before the delete is attempted, so a
            // throw cannot strand a modal over the page.
            assertFalse(
                vm.state.value.showClearConfirm,
                "a throw must not leave the confirmation dialog up",
            )
        }

    @Test
    fun `the toggles ignore a failed write because their own state comes back from settings`() =
        runTest(timeout = 30.seconds) {
            settingsRepository.failOnWrite = AppError.Storage("read-only volume")
            val vm = viewModel()
            // Speaking answers is on by default, so turning it *off* is the move that has
            // something to prove — flipping it on would assert the starting value.
            assertTrue(vm.state.value.settings.speakAnswers, "guard: the default is on")

            vm.onIntent(SettingsIntent.SetSpeakAnswers(enabled = false))
            vm.onIntent(SettingsIntent.SelectTheme(ThemePreference.DARK))
            runCurrent()

            // No write landed, so the settings flow never emitted, so both controls are still
            // where they were. That is the self-correcting behaviour the delete does not have,
            // and why these toggles are allowed to discard the result the delete must check.
            assertTrue(
                vm.state.value.settings.speakAnswers,
                "a toggle whose write failed must snap back rather than show the new position",
            )
            assertEquals(
                ThemePreference.SYSTEM,
                vm.state.value.settings.darkTheme,
                "the theme row is the same case as the switch",
            )
        }
}
