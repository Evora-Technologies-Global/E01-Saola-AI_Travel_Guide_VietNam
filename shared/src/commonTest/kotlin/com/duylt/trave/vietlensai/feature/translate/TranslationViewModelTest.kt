package com.duylt.trave.vietlensai.feature.translate

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.duylt.trave.vietlensai.core.util.DETECT_TIMEOUT_MILLIS
import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.TranslateImageUseCase
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import com.duylt.trave.vietlensai.navigation.Routes
import com.duylt.trave.vietlensai.testing.FakeSettingsRepository
import com.duylt.trave.vietlensai.testing.FakeTextToSpeech
import com.duylt.trave.vietlensai.testing.FakeTranslationRepository
import com.duylt.trave.vietlensai.testing.clearAsFrameworkWould
import com.duylt.trave.vietlensai.testing.translationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A photograph with the words on it replaced, and the wait in front of it.
 *
 * The request starts in `init` rather than from an intent, so the failure paths here are the
 * ones the traveller reaches without pressing anything. All three matter: the request can
 * fail, it can outrun the screen's own ceiling, and it can throw — and the screen keeps a
 * spinner over the photo across every one of them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TranslationViewModelTest {

    private val translations = FakeTranslationRepository()
    private val settings = FakeSettingsRepository()
    private val speech = FakeTextToSpeech()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the arguments are the first state rather than a frame later`() = runTest {
        val vm = viewModel()

        assertEquals("/captures/capture_1.jpg", vm.state.value.imagePath)
        assertEquals(TranslateLanguage.ENGLISH, vm.state.value.targetLanguage)
    }

    @Test
    fun `a translation lands and the spinner comes down`() = runTest {
        val vm = viewModel()
        runCurrent()

        assertNotNull(vm.state.value.translation)
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `a photo that was never passed is reported without a request`() = runTest {
        val vm = viewModel(imagePath = "")
        runCurrent()

        assertTrue(vm.state.value.error is AppError.ImageUnavailable)
        assertFalse(vm.state.value.isLoading)
        assertEquals(0, translations.translateCalls)
    }

    @Test
    fun `a request that outruns the ceiling is reported as a timeout`() = runTest {
        translations.translateDelayMillis = DETECT_TIMEOUT_MILLIS * 2
        val vm = viewModel()
        advanceTimeBy(DETECT_TIMEOUT_MILLIS + 1_000)
        runCurrent()

        assertEquals(AppError.Timeout, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `a failure is reported and the photo stays readable`() = runTest {
        translations.result = AppResult.Failure(AppError.NoConnection)
        val vm = viewModel()
        runCurrent()

        assertEquals(AppError.NoConnection, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
        assertEquals("/captures/capture_1.jpg", vm.state.value.imagePath)
    }

    @Test
    fun `a request that throws still lets the retry button work`() = runTest {
        // The unwrapped path. `onError` writes the error and nothing lowers `isLoading`, so the
        // screen reports a failure with a spinner still turning over the top of it — the two
        // states that cannot both be true.
        translations.throwOnTranslate = IllegalStateException("The recogniser returned nothing")
        val vm = viewModel()
        runCurrent()

        assertNotNull(vm.state.value.error, "an unwrapped throw must be reported")
        assertFalse(vm.state.value.isLoading, "and the spinner must not outlive it")

        translations.throwOnTranslate = null
        vm.onIntent(TranslationIntent.Retry)
        runCurrent()

        assertNotNull(vm.state.value.translation)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `a retry replaces the request in flight rather than queueing it`() = runTest {
        translations.translateDelayMillis = 1_000
        val vm = viewModel()
        runCurrent()

        vm.onIntent(TranslationIntent.Retry)
        vm.onIntent(TranslationIntent.Retry)
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(3, translations.translateCalls, "each press starts one")
        assertNotNull(vm.state.value.translation, "and the last one wins")
    }

    @Test
    fun `backing out mid-request asks first`() = runTest {
        translations.translateDelayMillis = 10_000
        val vm = viewModel()
        runCurrent()
        assertTrue(vm.state.value.isLoading)

        vm.onIntent(TranslationIntent.BackPressed)

        assertTrue(vm.state.value.confirmingExit, "there is still something to lose")
    }

    @Test
    fun `backing out once the words are on screen just closes`() = runTest {
        val vm = viewModel()
        runCurrent()

        vm.effects.test {
            vm.onIntent(TranslationIntent.BackPressed)
            runCurrent()
            assertTrue(awaitItem() is TranslationEffect.Close)
        }
        assertFalse(vm.state.value.confirmingExit, "a dialog between the traveller and the way out")
    }

    @Test
    fun `confirming the exit cancels the request rather than letting it land`() = runTest {
        translations.translateDelayMillis = 10_000
        val vm = viewModel()
        runCurrent()

        vm.onIntent(TranslationIntent.BackPressed)
        vm.effects.test {
            vm.onIntent(TranslationIntent.ConfirmExit)
            runCurrent()
            assertTrue(awaitItem() is TranslationEffect.Close)
        }

        advanceTimeBy(20_000)
        runCurrent()
        assertNull(vm.state.value.translation, "a screen nobody is looking at is not written to")
    }

    @Test
    fun `the words spoken are the translation and not the original`() = runTest {
        translations.result = AppResult.Success(translationResult())
        val vm = viewModel()
        runCurrent()

        vm.onIntent(TranslationIntent.ToggleSpeech)

        assertEquals(listOf("Beef noodle soup"), speech.spoken)
    }

    @Test
    fun `leaving silences only the narration this screen started`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(TranslationIntent.ToggleSpeech)

        vm.clearAsFrameworkWould()

        assertEquals(1, speech.stopCalls)
    }

    private fun TestScope.viewModel(
        imagePath: String = "/captures/capture_1.jpg",
    ) = TranslationViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                Routes.ARG_IMAGE_PATH to imagePath,
                Routes.ARG_TARGET_LANGUAGE to TranslateLanguage.ENGLISH.code,
            ),
        ),
        translateImage = TranslateImageUseCase(translations),
        observeSettings = ObserveSettingsUseCase(settings),
        textToSpeech = speech,
    )
}
