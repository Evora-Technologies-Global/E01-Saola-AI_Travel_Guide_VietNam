package com.evora.technologies.saola.feature.camera.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.InkBrown
import com.evora.technologies.saola.core.designsystem.theme.Marigold
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.StampType
import com.evora.technologies.saola.feature.camera.CameraCapabilities
import com.evora.technologies.saola.feature.camera.CameraController
import com.evora.technologies.saola.feature.camera.CameraPreview
import com.evora.technologies.saola.feature.camera.LensState
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.camera_ae_af_locked
import org.jetbrains.compose.resources.stringResource

/**
 * The viewfinder and everything drawn *inside* the frame: grid, pinch, focus ring.
 *
 * Binding lives here rather than in the parent so a lens switch is expressed the
 * declarative way — the facing is a key, and changing it rebinds.
 */
@Composable
internal fun Viewfinder(
    controller: CameraController,
    state: LensState,
    capabilities: CameraCapabilities,
    onPinch: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusLocked by remember { mutableStateOf(false) }
    var frameSize by remember { mutableStateOf(IntSize.Zero) }

    // A lens switch drops the reticle: the point the traveller metered belongs to the frame
    // the other camera was showing. Binding itself is the platform device's business now —
    // it owns the preview surface, so it is the only thing that knows when there is one to
    // bind to.
    LaunchedEffect(state.lensFacing) {
        focusPoint = null
        focusLocked = false
    }
    LaunchedEffect(state.flashEnabled) { controller.setFlash(state.flashEnabled) }

    Box(
        // Clipped because FILL_CENTER scales the preview surface until it covers the
        // view, and an Android view inside Compose is not clipped by default: at 1:1
        // the frame would spill a fifth of the picture over the black band it is
        // supposed to end at.
        modifier = modifier.clipToBounds().onSizeChanged { frameSize = it },
    ) {
        CameraPreview(
            controller = controller,
            lensFacing = state.lensFacing,
            modifier = Modifier.fillMaxSize(),
        )

        // A transparent layer the same size as the preview, so a tap offset divided by the
        // frame is exactly the fraction `focusOn` wants — and each platform converts that
        // fraction into its own metering coordinates.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(controller, capabilities.canZoom) {
                    if (!capabilities.canZoom) return@pointerInput
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) onPinch(zoom)
                    }
                }
                .pointerInput(controller) {
                    detectTapGestures(
                        onTap = { offset ->
                            // A tap while locked means "not there, here": release
                            // first, then meter the new point.
                            if (focusLocked) {
                                controller.clearFocus()
                                focusLocked = false
                            }
                            controller.setExposureIndex(0)
                            // The reticle follows the request, not the tap: no ring
                            // unless the camera actually accepted the point.
                            if (controller.focusOnFraction(offset, frameSize)) focusPoint = offset
                        },
                        onLongPress = { offset ->
                            controller.setExposureIndex(0)
                            if (controller.focusOnFraction(offset, frameSize, lock = true)) {
                                focusPoint = offset
                                focusLocked = true
                            }
                        },
                    )
                },
        )

        if (state.gridEnabled) {
            GridOverlay(modifier = Modifier.fillMaxSize())
        }

        focusPoint?.let { point ->
            FocusReticle(
                position = point,
                locked = focusLocked,
                capabilities = capabilities,
                frameSize = frameSize,
                onExposureIndex = controller::setExposureIndex,
                onFinished = {
                    focusPoint = null
                    // Exposure goes back to automatic with the reticle: a compensation
                    // the traveller can no longer see is one they cannot undo.
                    controller.setExposureIndex(0)
                },
            )
        }

        if (focusLocked) {
            Text(
                text = stringResource(Res.string.camera_ae_af_locked),
                style = StampType.kicker,
                color = InkBrown,
                modifier = Modifier
                    // In the corner rather than centred: the middle of the top edge
                    // is where the framing tip lands, and two gold pills stacked on
                    // one another read as a single unreadable one.
                    .align(Alignment.TopStart)
                    .padding(top = Spacing.md, start = Spacing.md)
                    .clip(Pill)
                    .background(Marigold)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
        }
    }
}

/** Rule of thirds, drawn thin enough to compose against and thin enough to ignore. */
@Composable
private fun GridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.dp.toPx()
        val line = Color.White.copy(alpha = 0.28f)
        for (step in 1..2) {
            val x = size.width * step / 3f
            val y = size.height * step / 3f
            drawLine(line, Offset(x, 0f), Offset(x, size.height), stroke)
            drawLine(line, Offset(0f, y), Offset(size.width, y), stroke)
        }
    }
}

/**
 * Turns a tap into the fraction-of-the-frame that [CameraController.focusOn] expects.
 *
 * Kept beside the gesture rather than inside the controller because only the composable knows
 * how big the preview it drew was — and a zero size means layout has not happened yet, which
 * is a tap that cannot be mapped rather than one at the top-left corner.
 */
private fun CameraController.focusOnFraction(
    offset: Offset,
    frameSize: IntSize,
    lock: Boolean = false,
): Boolean {
    if (frameSize.width <= 0 || frameSize.height <= 0) return false
    return focusOn(
        x = offset.x / frameSize.width,
        y = offset.y / frameSize.height,
        lock = lock,
    )
}
