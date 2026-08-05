package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.saola.core.designsystem.component.OverlayHeader
import com.evora.technologies.saola.core.designsystem.component.OverlayHeaderStyle
import com.evora.technologies.saola.core.designsystem.component.OverlayIconButton
import com.evora.technologies.saola.core.designsystem.theme.Motion
import com.evora.technologies.saola.domain.repository.CaptureStore
import com.evora.technologies.saola.feature.camera.CameraPreview
import com.evora.technologies.saola.feature.camera.LensFacing
import com.evora.technologies.saola.feature.camera.rememberCameraController
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.action_close
import com.evora.technologies.saola.resources.discovery_note_camera_flip
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The viewfinder, for a photo that belongs to a note rather than to recognition.
 *
 * Deliberately not the lens screen: that one is an instrument for pointing at a thing and
 * asking what it is, with zoom, flash, timers and modes to match. This is a shutter and a way
 * to turn the camera around, because the traveller already knows what they are looking at.
 *
 * [CaptureStore] is taken from Koin here rather than reached through the ViewModel. Unlike the
 * lens, this shutter is pressed inside the overlay and the ViewModel never learns of the press,
 * so there is no effect for a path to travel on — and the alternative was a `newCapturePath`
 * lambda threaded down five levels from the route, plus a second public method on a ViewModel
 * whose only entry point is meant to be `onIntent`. It is the store, not the ViewModel: nothing
 * below the route sees the ViewModel, and where captures are written stays the store's business.
 */
@Composable
internal fun NoteCameraOverlay(
    onCaptured: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val captureStore: CaptureStore = koinInject()
    val controller = rememberCameraController()
    val capabilities by controller.capabilities.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var lensFacing by remember { mutableStateOf(LensFacing.BACK) }
    var isCapturing by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(
            controller = controller,
            lensFacing = lensFacing,
            modifier = Modifier.fillMaxSize(),
        )

        OverlayHeader(
            style = OverlayHeaderStyle.Plain,
            modifier = Modifier.align(Alignment.TopCenter),
            leading = {
                OverlayIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.action_close),
                    onClick = onClose,
                )
            },
            // Only where there is a second camera to turn to: a flip button on a phone
            // with one lens is a control that answers a tap by doing nothing. Which of the
            // two it is arrives a moment after the preview does, so it fades in rather
            // than appearing fully formed over an image that is already live.
            trailing = {
                AnimatedVisibility(
                    visible = capabilities.hasFrontCamera,
                    enter = fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = POP_SCALE),
                    exit = fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = POP_SCALE),
                ) {
                    OverlayIconButton(
                        icon = Icons.Filled.Cameraswitch,
                        contentDescription = stringResource(Res.string.discovery_note_camera_flip),
                        onClick = { lensFacing = lensFacing.flipped() },
                    )
                }
            },
        )

        // The one control on this screen with no state to show except that it was pressed.
        // The white disc dips and the ring behind it brightens for as long as the capture
        // takes, which is the only acknowledgement there is — the photo does not appear
        // here, it appears in the strip on a page that is not on screen yet.
        val shutterSize by animateDpAsState(
            targetValue = if (isCapturing) SHUTTER_DISC_PRESSED else SHUTTER_DISC,
            animationSpec = Motion.morph(Motion.QUICK_MILLIS),
            label = "shutterDisc",
        )
        val shutterRing by animateFloatAsState(
            targetValue = if (isCapturing) RING_PRESSED_ALPHA else RING_ALPHA,
            animationSpec = Motion.morph(Motion.QUICK_MILLIS),
            label = "shutterRing",
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = SHUTTER_INSET)
                .size(SHUTTER_TARGET)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = shutterRing))
                .border(SHUTTER_BORDER, Color.White, CircleShape)
                // Latched for the length of the capture: the shutter is one tap, and a
                // second one mid-write would leave a JPEG nothing goes on to claim.
                .clickable(enabled = !isCapturing) {
                    isCapturing = true
                    scope.launch {
                        val path = controller.capture(captureStore.newCapturePath())
                        isCapturing = false
                        if (path != null) onCaptured(path) else onClose()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(shutterSize)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

/** Where the shutter sits above the navigation bar. A hit target, so it is not snapped. */
private val SHUTTER_INSET = 40.dp

private val SHUTTER_TARGET = 76.dp
private val SHUTTER_DISC = 58.dp
private val SHUTTER_DISC_PRESSED = 44.dp
private val SHUTTER_BORDER = 3.dp

private const val RING_ALPHA = 0.25f
private const val RING_PRESSED_ALPHA = 0.35f

/** How small the flip button starts and ends — a pop, not an arrival from elsewhere. */
private const val POP_SCALE = 0.8f
