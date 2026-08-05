package com.evora.technologies.saola.feature.camera

import androidx.lifecycle.viewModelScope
import com.evora.technologies.saola.core.mvi.MviViewModel
import com.evora.technologies.saola.core.util.DETECT_TIMEOUT_MILLIS
import com.evora.technologies.saola.core.util.VolumeShutterBus
import com.evora.technologies.saola.domain.model.LensMode
import com.evora.technologies.saola.domain.repository.CaptureStore
import com.evora.technologies.saola.domain.usecase.MarkLocationAskedUseCase
import com.evora.technologies.saola.domain.usecase.ObserveApiKeyAvailabilityUseCase
import com.evora.technologies.saola.domain.usecase.ObserveDiscoveriesUseCase
import com.evora.technologies.saola.domain.usecase.ObserveSettingsUseCase
import com.evora.technologies.saola.domain.usecase.RecognizeImageUseCase
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Drives the camera screen and the recognition round-trip.
 *
 * Translate leaves by a different door from the same capture button: recognition
 * is worth waiting for on the viewfinder, because the traveller is still looking
 * at the thing they photographed, while a sign they have already read is not —
 * so that mode hands the photo on and lets the next screen do the waiting.
 */
class LensViewModel(
    private val recognizeImage: RecognizeImageUseCase,
    private val captureStore: CaptureStore,
    private val volumeShutter: VolumeShutterBus,
    private val markLocationAsked: MarkLocationAskedUseCase,
    observeDiscoveries: ObserveDiscoveriesUseCase,
    observeApiKeyAvailability: ObserveApiKeyAvailabilityUseCase,
    observeSettings: ObserveSettingsUseCase,
) : MviViewModel<LensState, LensIntent, LensEffect>(LensState()) {

    private var analysisJob: Job? = null

    private var countdownJob: Job? = null

    init {
        observeDiscoveries()
            .map { it.take(RECENT_COUNT) }
            .onEach { recent -> setState { copy(recentDiscoveries = recent) } }
            .launchIn(viewModelScope)

        observeApiKeyAvailability()
            .onEach { available -> setState { copy(hasApiKey = available) } }
            .launchIn(viewModelScope)

        observeSettings()
            .onEach { settings -> setState { copy(hasAskedLocation = settings.hasAskedLocation) } }
            .launchIn(viewModelScope)

        // A volume key is the same decision as the on-screen shutter, so it runs the
        // same path — self-timer, busy guards and all.
        volumeShutter.presses
            .onEach { onShutterPressed() }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: LensIntent) {
        when (intent) {
            is LensIntent.SelectMode -> setState { copy(mode = intent.mode, error = null) }
            is LensIntent.SelectTranslateFrom -> setState { copy(translateFrom = intent.language) }
            is LensIntent.SelectTranslateTo -> setState { copy(translateTo = intent.language) }
            is LensIntent.SwapTranslateLanguages -> setState {
                // Nothing to exchange while the source is still being detected; the
                // button is dimmed for that case, and this is the same rule in code.
                val from = translateFrom ?: return@setState this
                copy(translateFrom = translateTo, translateTo = from)
            }
            is LensIntent.ToggleFlash -> setState { copy(flashEnabled = !flashEnabled) }
            is LensIntent.ToggleGrid -> setState { copy(gridEnabled = !gridEnabled) }
            is LensIntent.CycleTimer -> setState { copy(timer = timer.next()) }
            is LensIntent.SwitchLens -> {
                // The framing is gone the moment the lens flips, so a timer that
                // was counting down on the old view is cancelled rather than fired.
                cancelCountdown()
                setState { copy(lensFacing = lensFacing.flipped()) }
            }
            is LensIntent.ShutterPressed -> onShutterPressed()
            is LensIntent.ScreenStarted -> volumeShutter.arm()
            is LensIntent.ScreenStopped -> {
                volumeShutter.disarm()
                cancelCountdown()
            }
            is LensIntent.DismissError -> setState { copy(error = null) }
            is LensIntent.LocationAsked -> launchSafely { markLocationAsked() }
            is LensIntent.PhotoCaptured -> {
                setState { copy(isCapturing = false) }
                analyse(intent.path)
            }
            is LensIntent.PhotoPicked -> analyse(intent.path)
            is LensIntent.CaptureAborted -> setState { copy(isCapturing = false) }
            is LensIntent.CaptureFailed -> {
                cancelCountdown()
                setState {
                    copy(
                        isCapturing = false,
                        isAnalysing = false,
                        error = AppError.ImageUnavailable(intent.message),
                    )
                }
                sendEffect(LensEffect.ShowMessage(AppError.ImageUnavailable(intent.message)))
            }
        }
    }

    /**
     * One button, three meanings: cancel a running countdown, start one, or shoot.
     *
     * Cancelling comes first. Once the number is on screen the shutter has become
     * the "stop" button — a second press that queued another photo instead would
     * be unrecoverable, since the first is already on its way.
     */
    private fun onShutterPressed() {
        if (currentState.isAnalysing || currentState.isCapturing) return
        if (currentState.isCountingDown) {
            cancelCountdown()
            return
        }

        val seconds = currentState.timer.seconds
        if (seconds == 0) {
            setState { copy(isCapturing = true) }
            sendEffect(LensEffect.TakePhoto(captureStore.newCapturePath()))
            return
        }

        countdownJob = launchSafely {
            for (remaining in seconds downTo 1) {
                setState { copy(countdown = remaining) }
                delay(COUNTDOWN_TICK_MILLIS)
            }
            setState { copy(countdown = 0, isCapturing = true) }
            // Named at the moment the shutter actually fires rather than when the timer
            // started: a countdown that is cancelled and restarted must not leave a
            // reserved path behind that no JPEG is ever written to.
            sendEffect(LensEffect.TakePhoto(captureStore.newCapturePath()))
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        if (currentState.countdown != 0) setState { copy(countdown = 0) }
    }

    private fun analyse(imagePath: String) {
        // A second capture while one is in flight replaces it: the traveller has
        // moved on to a different subject, and finishing the old request would
        // navigate them somewhere they no longer care about.
        analysisJob?.cancel()
        cancelCountdown()

        if (currentState.mode == LensMode.TRANSLATE) {
            // Nothing to wait for here: the photo is the whole payload, and the
            // translation screen shows it while it asks. Leaving the viewfinder
            // under an "analysing" scrim would hold the traveller on a screen
            // that has already finished its job.
            setState { copy(isAnalysing = false, error = null) }
            sendEffect(
                LensEffect.OpenTranslation(
                    imagePath = imagePath,
                    from = currentState.translateFrom,
                    to = currentState.translateTo,
                ),
            )
            return
        }

        // `isAnalysing = false` as well as the error: an unexpected throw skips every
        // `when` branch below, so this is the only thing that lifts the scrim. Leaving it
        // up would trade a crash for a dead screen, which is not obviously the better one.
        analysisJob = launchSafely(
            onError = { setState { copy(isAnalysing = false, error = it) } },
        ) {
            setState { copy(isAnalysing = true, error = null, analysisStage = 0) }

            // The stage ticker is a *child* of this job rather than a field of its own.
            //
            // It used to be `stageJob`, cancelled by hand at each of the four places a
            // request can end — and it only takes one of those to be missed for a
            // `while (isActive)` loop to be left writing state every 1.8 seconds for the
            // rest of the ViewModel's life, recomposing the viewfinder behind whatever
            // screen the traveller moved on to. Two of them were missed: the translate
            // branch above, which returns early, and any exception, which jumps straight
            // past the cancel at the end of this block.
            //
            // Owning it structurally removes the whole class of mistake: the ticker cannot
            // outlive the request it narrates, however that request ends — answered, failed,
            // timed out, or superseded by the next capture.
            val ticker = launch {
                var stage = 0
                while (isActive) {
                    delay(STAGE_INTERVAL_MILLIS)
                    stage += 1
                    setState { copy(analysisStage = stage) }
                }
            }

            try {
                // Called off rather than waited out: past [DETECT_TIMEOUT_MILLIS] the
                // scrim has stopped reading as "working on it". `OrNull` rather than a
                // thrown TimeoutCancellationException, so the expiry lands in the same
                // `when` as every other outcome instead of in a catch that would have to
                // be careful not to swallow the screen's own cancellation.
                val result = withTimeoutOrNull(DETECT_TIMEOUT_MILLIS) {
                    recognizeImage(imagePath, currentState.mode)
                }

                when (result) {
                    null -> emitError(AppError.Timeout)
                    is AppResult.Failure -> emitError(result.error)
                    is AppResult.Success -> {
                        setState { copy(isAnalysing = false) }
                        sendEffect(LensEffect.OpenDiscovery(result.data.id))
                    }
                }
            } finally {
                // An infinite child would otherwise keep this job — and `launchSafely`'s
                // own `finally`-free path — from ever completing.
                ticker.cancel()
            }
        }
    }

    private fun emitError(error: AppError) {
        setState { copy(isAnalysing = false, error = error) }
        sendEffect(LensEffect.ShowMessage(error))
    }

    override fun onCleared() {
        super.onCleared()
        // Belt and braces: the screen disarms on stop, but a ViewModel torn down
        // without one must not leave the volume keys captured.
        volumeShutter.disarm()
        analysisJob?.cancel()
        countdownJob?.cancel()
    }

    private companion object {
        /** The lens shows recent captures as a small stack; more than five stops reading as one. */
        const val RECENT_COUNT = 5
        const val STAGE_INTERVAL_MILLIS = 1_800L
        const val COUNTDOWN_TICK_MILLIS = 1_000L
    }
}
