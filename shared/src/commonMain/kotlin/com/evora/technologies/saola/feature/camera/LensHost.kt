package com.evora.technologies.saola.feature.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.saola.core.mvi.CollectEffects
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Everything the lens does that is not layout, in one place both arrangements call.
 *
 * The phone and the tablet draw the viewfinder differently — a column with the shutter under
 * the frame, or a panel of controls beside it — but they hold the camera identically: the
 * same controller, the same effect handling, the same lifecycle contract for the volume keys.
 * That half is behaviour, and `LLM.md` §3 is explicit that a presentation branch owns no
 * behaviour. Two copies of the capture coroutine below would mean the next fix to its
 * `finally` block lands on one form factor and not the other, and the symptom — a shutter
 * stuck disabled after the traveller left mid-capture — is one nobody reproduces on purpose.
 *
 * So this is the Route, shared, and each branch supplies only [content]: given the state, the
 * camera and one way to write back, place the composables. It is the one place in the app
 * where the Route/Screen split of §5 straddles the branch line, and it does so because
 * everything above the split turned out to be the same on both sides.
 *
 * @param content the arrangement. Receives the state to read, the [CameraController] to draw a
 *   preview from, and `onIntent` — the only way to write.
 */
@Composable
internal fun LensHost(
    viewModel: LensViewModel,
    onDiscoveryCaptured: (String) -> Unit,
    /** Photo path, source language code (blank for detect), target language code. */
    onTranslationCaptured: (String, String, String) -> Unit,
    content: @Composable (
        state: LensState,
        controller: CameraController,
        onIntent: (LensIntent) -> Unit,
    ) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The controller is owned above the screen because the shutter is no longer a
    // click: with a self-timer running, the ViewModel decides when the photo is
    // taken and says so with an effect, which is collected here.
    val controller = rememberCameraController()
    val scope = rememberCoroutineScope()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is LensEffect.OpenDiscovery -> onDiscoveryCaptured(effect.id)
            is LensEffect.OpenTranslation -> onTranslationCaptured(
                effect.imagePath,
                effect.from?.code.orEmpty(),
                effect.to.code,
            )
            is LensEffect.ShowMessage -> Unit // rendered inline by the error snackbar
            is LensEffect.TakePhoto -> scope.launch {
                // Launched outside the collector, which is now lifecycle-scoped and is
                // cancelled at ON_STOP. A capture already in flight has to reach its
                // `finally` whatever the screen does — that is what raises PhotoCaptured
                // / CaptureFailed / CaptureAborted and releases the shutter, and a
                // capture killed silently mid-write would leave `isCapturing` true with
                // nothing left to clear it. `rememberCoroutineScope()` lives until the
                // composable leaves composition, which is the lifetime this needs.
                var path: String? = null
                try {
                    // Wrapped because takePicture throws outright if the use case
                    // was unbound between the timer firing and this frame.
                    path = runCatching { controller.capture(effect.outputPath) }
                        .getOrNull()
                } finally {
                    // Reported from a finally because this scope dies with the
                    // composition: a capture the screen outlived must still
                    // release the shutter, without looking like a failure.
                    val captured = path
                    viewModel.onIntent(
                        when {
                            captured != null -> LensIntent.PhotoCaptured(captured)
                            isActive -> LensIntent.CaptureFailed(null)
                            else -> LensIntent.CaptureAborted
                        },
                    )
                }
            }
        }
    }

    // Inside a NavHost the lifecycle owner is the back-stack entry, so this catches
    // both a tab switch and Home — the two ways a countdown outlives its frame, and
    // the two ways the volume keys must go back to meaning volume.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onIntent(LensIntent.ScreenStarted)
                Lifecycle.Event.ON_STOP -> viewModel.onIntent(LensIntent.ScreenStopped)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onIntent(LensIntent.ScreenStopped)
        }
    }

    content(state, controller, viewModel::onIntent)
}
