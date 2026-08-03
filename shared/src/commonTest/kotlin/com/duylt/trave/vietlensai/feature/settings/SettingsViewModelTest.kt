package com.duylt.trave.vietlensai.feature.settings

import app.cash.turbine.test
import com.duylt.trave.vietlensai.domain.usecase.ClearHistoryUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveApiKeyAvailabilityUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.SaveApiKeyUseCase
import com.duylt.trave.vietlensai.domain.usecase.UpdateModelUseCase
import com.duylt.trave.vietlensai.domain.usecase.UpdateThemeUseCase
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.testing.FakeDiscoveryRepository
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
import kotlin.time.Duration.Companion.seconds

/**
 * The settings screen must not confirm a save that did not happen.
 *
 * This is the one screen where a swallowed write is worse than a crash. Saving a key does
 * three things at once — it clears the field, it shows a green confirmation, and it is the
 * only way to make recognition work — so a write that failed silently left the traveller
 * with their key discarded, a message saying it was stored, and a camera that went on
 * refusing to run, with nothing on screen connecting the three.
 *
 * `SettingsRepository`'s writes return [com.duylt.trave.vietlensai.domain.util.AppResult]
 * for exactly this reason. These tests pin the behaviour that depends on it, because the
 * failure it prevents is invisible: nothing crashes, nothing logs at ERROR, and the screen
 * looks like it worked.
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
        observeApiKeyAvailability = ObserveApiKeyAvailabilityUseCase(settingsRepository),
        settingsRepository = settingsRepository,
        saveApiKey = SaveApiKeyUseCase(settingsRepository),
        updateModel = UpdateModelUseCase(settingsRepository),
        updateTheme = UpdateThemeUseCase(settingsRepository),
        clearHistory = ClearHistoryUseCase(discoveryRepository),
    )

    @Test
    fun `a key that stored is confirmed and leaves the field empty`() = runTest(timeout = 30.seconds) {
        val vm = viewModel()

        vm.effects.test {
            vm.onIntent(SettingsIntent.ApiKeyDraftChanged("AIza-good-key"))
            vm.onIntent(SettingsIntent.SaveApiKey)
            runCurrent()

            assertEquals(SettingsEffect.ApiKeySaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("", vm.state.value.apiKeyDraft, "the field must not keep a secret on screen")
        assertEquals("AIza-good-key", settingsRepository.state.value.apiKey)
    }

    /**
     * The defect this whole change exists for.
     *
     * Before the writes returned a result, this test's assertions all failed: the effect was
     * `ApiKeySaved`, the draft was cleared, and the key was gone.
     */
    @Test
    fun `a key that failed to store is not confirmed and is left in the field`() =
        runTest(timeout = 30.seconds) {
            settingsRepository.failOnWrite = AppError.Storage("disk full")
            val vm = viewModel()

            vm.effects.test {
                vm.onIntent(SettingsIntent.ApiKeyDraftChanged("AIza-unlucky"))
                vm.onIntent(SettingsIntent.SaveApiKey)
                runCurrent()

                val effect = awaitItem()
                assertTrue(
                    effect is SettingsEffect.ShowMessage,
                    "a failed write must report the failure, not confirm a save — got $effect",
                )
                // The error rides on the effect rather than being read back out of state.
                // This assertion used to be `vm.state.value.error`, on the reasoning that
                // "the screen resolves its message from state" — which was the bug: the
                // route resolves during composition, and the effect is handled a
                // main-queue turn before the next frame, so the message was always the
                // stale null. See `AppError.userMessage` and `LLM.md` §11 row #15.
                assertEquals(
                    AppError.Storage("disk full"),
                    (effect as SettingsEffect.ShowMessage).error,
                    "the message the screen shows comes from the effect, so the error has to be on it",
                )
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(
                "AIza-unlucky",
                vm.state.value.apiKeyDraft,
                "the key must survive in the field so the traveller can retry without retyping",
            )
        }

    /**
     * A throw is the other half of the contract, and it must not reach the platform handler.
     *
     * `launchSafely` is the floor: `viewModelScope` carries no `CoroutineExceptionHandler`,
     * so an escaping exception here would take the process down with the traveller's
     * unsaved key in it.
     */
    @Test
    fun `an exception while storing the key does not escape the ViewModel`() =
        runTest(timeout = 30.seconds) {
            settingsRepository.throwOnWrite = IllegalStateException("preferences file locked")
            val vm = viewModel()

            vm.onIntent(SettingsIntent.ApiKeyDraftChanged("AIza-doomed"))
            vm.onIntent(SettingsIntent.SaveApiKey)
            runCurrent()

            // Reaching this line at all is the assertion: the test worker is still alive.
            assertEquals(
                "AIza-doomed",
                vm.state.value.apiKeyDraft,
                "a throw must not clear the field either",
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
            runCurrent()

            // No write landed, so the settings flow never emitted, so the switch is still on.
            // That is the self-correcting behaviour the API-key path does not have, and why
            // these toggles are allowed to discard the result the API-key path must check.
            assertTrue(
                vm.state.value.settings.speakAnswers,
                "a toggle whose write failed must snap back rather than show the new position",
            )
        }
}
