package com.evora.technologies.saola.feature.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

/**
 * The raw camera operations, one implementation per platform.
 *
 * Deliberately dumb: it binds a session, pushes an absolute value at the hardware and
 * reports back what the hardware can do. Every decision that is a *policy* — how fast a
 * pinch should move, what a flash toggle means before the camera has finished binding,
 * where along the dial a ratio sits — lives in [CameraController] above it, so both
 * platforms behave identically without either having to re-derive the behaviour.
 *
 * CameraX on Android, AVFoundation on iOS.
 */
internal interface CameraDevice {

    /** What this device can do, for a screen that must not offer controls it cannot honour. */
    val capabilities: StateFlow<CameraCapabilities>

    /**
     * The viewfinder, and the thing that binds the session.
     *
     * A composable member rather than a separate `expect` function: only the device knows
     * what surface its platform wants to draw into — a `PreviewView` inside an `AndroidView`,
     * an `AVCaptureVideoPreviewLayer` inside a `UIKitView` — and only it can map a tap on
     * that surface back to a metering point.
     */
    @Composable
    fun Preview(lensFacing: LensFacing, modifier: Modifier)

    /**
     * Turns the flash and the torch on together, or both off.
     *
     * Two things, not one: the capture flag makes the flash fire when the shutter is
     * pressed; the torch makes it visible *now*, which is what turning the icon on is
     * understood to promise. Without the torch the traveller gets no feedback at all until
     * the photo is already taken.
     */
    fun setFlashAndTorch(enabled: Boolean)

    /** An absolute ratio, already clamped by the caller to the reported range. */
    fun setZoomRatio(ratio: Float)

    /**
     * Focuses and meters exposure on a point, given as a fraction of the preview's width
     * and height.
     *
     * Normalised rather than in pixels because the two platforms measure their preview
     * surfaces differently — device pixels on Android, points on iOS — and a fraction is
     * the one description both can convert from without the caller knowing which it is.
     *
     * @param lock keeps the metering where it was put instead of letting the camera take it
     *   back — a long press, for reframing without the exposure sliding.
     * @return false when the tap could not be turned into a focus request, so the caller can
     *   skip the reticle rather than animate a promise nothing kept.
     */
    fun focusOn(x: Float, y: Float, lock: Boolean): Boolean

    /** Releases a locked focus and hands metering back to the camera. */
    fun clearFocus()

    /** Exposure compensation, in the camera's own index steps, already clamped. */
    fun setExposureIndex(index: Int)

    /** Writes a JPEG to [targetPath]. Returns the path, or null if the capture failed. */
    suspend fun capture(targetPath: String): String?

    fun unbind()
}

/**
 * A lifecycle-aware wrapper over the platform camera.
 *
 * Kept out of the composable so binding survives recomposition and so the capture call can
 * be a plain suspend function — both platforms' capture APIs are callback-based and
 * otherwise awkward to sequence with the recognition request that follows.
 */
class CameraController internal constructor(
    internal val device: CameraDevice,
) {

    /** What this device can do, for a screen that must not offer controls it cannot honour. */
    val capabilities: StateFlow<CameraCapabilities> get() = device.capabilities

    /**
     * The flash state the traveller asked for, kept here rather than only on the device.
     *
     * The screen can toggle flash before the camera has finished binding — and it always
     * does on a cold start, because binding is asynchronous while the composable runs
     * immediately. Remembering the request means it can be replayed onto the session once
     * there is one, instead of being silently dropped.
     */
    private var flashRequested: Boolean = false

    /**
     * The ratio last asked for, read back synchronously.
     *
     * A pinch computes its next ratio from the current one sixty times a second, while
     * [capabilities] only catches up when the camera reports back. Compounding against that
     * lagging value makes the zoom stutter and undershoot, so the gesture multiplies what
     * was requested instead of what has arrived.
     */
    private var requestedZoomRatio: Float? = null

    fun setFlash(enabled: Boolean) {
        flashRequested = enabled
        applyFlash()
    }

    /** Re-asserts the requested flash onto whatever session is bound now. */
    internal fun applyFlash() {
        device.setFlashAndTorch(flashRequested && capabilities.value.hasFlash)
    }

    /** Sets an absolute zoom ratio, clamped to what this camera supports. */
    fun setZoomRatio(ratio: Float) {
        val current = capabilities.value
        if (!current.canZoom) return
        val clamped = ratio.coerceIn(current.minZoomRatio, current.maxZoomRatio)
        requestedZoomRatio = clamped
        device.setZoomRatio(clamped)
    }

    /** Where the zoom is heading, which during a gesture is ahead of [capabilities]. */
    fun requestedZoom(): Float = requestedZoomRatio ?: capabilities.value.zoomRatio

    /**
     * Applies a pinch: [scale] is the gesture's frame-to-frame multiplier.
     *
     * Doubled before it is applied, the same way CameraX's own pinch handler does it. Raw
     * finger distance maps to a zoom range so slowly that the gesture feels broken next to
     * the phone's stock camera.
     */
    fun zoomBy(scale: Float) {
        val accelerated = if (scale > 1f) {
            1f + (scale - 1f) * PINCH_ACCELERATION
        } else {
            // Floored: a hard inward pinch would otherwise multiply by a negative.
            (1f - (1f - scale) * PINCH_ACCELERATION).coerceAtLeast(MIN_PINCH_FACTOR)
        }
        setZoomRatio(requestedZoom() * accelerated)
    }

    /** Moves the zoom by a fraction of the whole range — the dial's unit of work. */
    fun zoomByProgress(delta: Float) {
        val current = capabilities.value
        if (!current.canZoom) return
        val progress = current.zoomProgressOf(requestedZoom())
        setZoomRatio(current.zoomRatioAt(progress + delta))
    }

    /**
     * @param x fraction of the preview's width, 0 at the left edge.
     * @param y fraction of its height, 0 at the top.
     */
    fun focusOn(x: Float, y: Float, lock: Boolean = false): Boolean =
        device.focusOn(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f), lock)

    fun clearFocus() {
        device.clearFocus()
    }

    /**
     * Nudges the exposure the camera settled on, in its own index steps.
     *
     * Clamped and short-circuited here so the slider can follow the finger without the
     * device seeing a request per frame that it has already honoured.
     */
    fun setExposureIndex(index: Int) {
        val current = capabilities.value
        if (!current.canAdjustExposure) return
        val clamped = index.coerceIn(current.exposureRange.first, current.exposureRange.last)
        if (clamped == current.exposureIndex) return
        device.setExposureIndex(clamped)
    }

    /** Returns the absolute path of the written JPEG, or null if the capture failed. */
    suspend fun capture(targetPath: String): String? {
        // Re-assert the flag: the session may have been rebound (rotation, resume, a lens
        // switch) since the toggle was pressed.
        applyFlash()
        return device.capture(targetPath)
    }

    fun unbind() {
        requestedZoomRatio = null
        device.unbind()
    }

    private companion object {
        /** How much faster the zoom moves than the fingers. Matches CameraX's own handler. */
        const val PINCH_ACCELERATION = 2f
        const val MIN_PINCH_FACTOR = 0.1f
    }
}

/** Creates the controller for this platform, tied to the current composition. */
@Composable
expect fun rememberCameraController(): CameraController

/**
 * The viewfinder.
 *
 * Binding happens inside the platform device, which also un-binds when the composable
 * leaves — so the screen only has to say where the preview goes and which way the lens
 * points.
 */
@Composable
fun CameraPreview(
    controller: CameraController,
    lensFacing: LensFacing,
    modifier: Modifier = Modifier,
) {
    controller.device.Preview(lensFacing = lensFacing, modifier = modifier)
}
