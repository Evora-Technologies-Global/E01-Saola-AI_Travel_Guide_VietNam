package com.duylt.trave.vietlensai.feature.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.duylt.trave.vietlensai.core.util.log
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureExposureModeAutoExpose
import platform.AVFoundation.AVCaptureExposureModeContinuousAutoExposure
import platform.AVFoundation.AVCaptureFlashModeOff
import platform.AVFoundation.AVCaptureFlashModeOn
import platform.AVFoundation.AVCaptureFocusModeAutoFocus
import platform.AVFoundation.AVCaptureFocusModeContinuousAutoFocus
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.exposureMode
import platform.AVFoundation.exposurePointOfInterest
import platform.AVFoundation.exposurePointOfInterestSupported
import platform.AVFoundation.exposureTargetBias
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.flashMode
import platform.AVFoundation.focusMode
import platform.AVFoundation.focusPointOfInterest
import platform.AVFoundation.focusPointOfInterestSupported
import platform.AVFoundation.hasTorch
import platform.AVFoundation.isExposureModeSupported
import platform.AVFoundation.isFocusModeSupported
import platform.AVFoundation.isFlashAvailable
import platform.AVFoundation.maxAvailableVideoZoomFactor
import platform.AVFoundation.maxExposureTargetBias
import platform.AVFoundation.minAvailableVideoZoomFactor
import platform.AVFoundation.minExposureTargetBias
import platform.AVFoundation.position
import platform.AVFoundation.setExposureTargetBias
import platform.AVFoundation.torchMode
import platform.AVFoundation.videoZoomFactor
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.Foundation.NSError
import platform.Foundation.writeToFile
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_queue_create
import kotlin.math.roundToInt

@Composable
actual fun rememberCameraController(): CameraController =
    remember { CameraController(IosCameraDevice()) }

/**
 * AVFoundation behind the shared [CameraDevice] contract.
 *
 * The session runs on its own serial queue, as AVFoundation requires: `startRunning` blocks
 * for a noticeable fraction of a second while the hardware spins up, and doing that on the
 * main thread drops frames out of the Compose UI that is drawing the shutter button.
 *
 * Everything that reconfigures the device — zoom, torch, focus, exposure — has to be bracketed
 * by `lockForConfiguration`; without the lock iOS throws rather than ignoring the write, which
 * is why each setter goes through [withDeviceLock].
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosCameraDevice : CameraDevice {

    private val session = AVCaptureSession().apply {
        // Photo, not high-quality video: the still is what this app is for, and the preset is
        // what lets the sensor hand over its full-resolution frame.
        setSessionPreset(AVCaptureSessionPresetPhoto)
    }

    private val sessionQueue = dispatch_queue_create("vietlens.camera.session", null)
    private val photoOutput = AVCapturePhotoOutput()

    private var device: AVCaptureDevice? = null
    private var input: AVCaptureDeviceInput? = null
    private var previewView: CameraPreviewView? = null

    /** Held for the length of a capture: AVFoundation keeps only a weak reference to it. */
    private var captureDelegate: PhotoCaptureDelegate? = null

    private var flashEnabled = false

    private val _capabilities = MutableStateFlow(CameraCapabilities())
    override val capabilities: StateFlow<CameraCapabilities> = _capabilities.asStateFlow()

    @Composable
    override fun Preview(lensFacing: LensFacing, modifier: Modifier) {
        // A UIView subclass rather than a bare container: the preview is a CALayer, which is not
        // auto-layout aware, so something has to resize it when the host changes — and
        // `layoutSubviews` is where UIKit says that belongs.
        val view = remember { CameraPreviewView() }

        LaunchedEffect(view, lensFacing) { configure(lensFacing, view) }
        DisposableEffect(view) { onDispose { unbind() } }

        UIKitView(factory = { view }, modifier = modifier)
    }

    private fun configure(lensFacing: LensFacing, view: CameraPreviewView) {
        val position = when (lensFacing) {
            LensFacing.BACK -> AVCaptureDevicePositionBack
            LensFacing.FRONT -> AVCaptureDevicePositionFront
        }

        val camera = AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo)
            .filterIsInstance<AVCaptureDevice>()
            .firstOrNull { it.position == position }
            ?: run {
                log.e { "No camera facing $lensFacing" }
                return
            }

        session.beginConfiguration()
        input?.let { session.removeInput(it) }

        val newInput = AVCaptureDeviceInput.deviceInputWithDevice(camera, null)
        if (newInput == null || !session.canAddInput(newInput)) {
            log.e { "Could not add the $lensFacing camera to the session" }
            session.commitConfiguration()
            return
        }
        session.addInput(newInput)
        if (session.canAddOutput(photoOutput) && !session.outputs.contains(photoOutput)) {
            session.addOutput(photoOutput)
        }
        session.commitConfiguration()

        device = camera
        input = newInput

        val layer = view.attachPreviewLayer(session)
        previewView = view

        readCapabilities(camera)
        applyTorch()

        // startRunning blocks; the docs are explicit that it must not be on the main queue.
        dispatch_async(sessionQueue) {
            if (!session.isRunning()) session.startRunning()
        }
        layer.videoGravity = AVLayerVideoGravityResizeAspectFill
    }

    /**
     * Reads what this camera can do.
     *
     * Exposure is reported as an index over a shared EV step so the screen can render "+1.0" the
     * same way on both platforms — iOS exposes a continuous bias in EV, while CameraX counts in
     * device steps, and [EXPOSURE_STEP_EV] is the third of a stop both conventions land on.
     */
    private fun readCapabilities(camera: AVCaptureDevice) {
        val maxZoom = camera.maxAvailableVideoZoomFactor.toFloat().coerceAtMost(MAX_ZOOM_RATIO)
        val minBias = camera.minExposureTargetBias
        val maxBias = camera.maxExposureTargetBias

        _capabilities.update {
            it.copy(
                hasFlash = camera.hasTorch || camera.isFlashAvailable(),
                hasFrontCamera = AVCaptureDevice.devicesWithMediaType(AVMediaTypeVideo)
                    .filterIsInstance<AVCaptureDevice>()
                    .any { candidate -> candidate.position == AVCaptureDevicePositionFront },
                zoomRatio = camera.videoZoomFactor.toFloat(),
                minZoomRatio = camera.minAvailableVideoZoomFactor.toFloat(),
                maxZoomRatio = maxZoom,
                exposureRange = if (maxBias > minBias) {
                    (minBias / EXPOSURE_STEP_EV).roundToInt()..(maxBias / EXPOSURE_STEP_EV).roundToInt()
                } else {
                    IntRange.EMPTY
                },
                exposureStepEv = if (maxBias > minBias) EXPOSURE_STEP_EV else 0f,
                exposureIndex = (camera.exposureTargetBias / EXPOSURE_STEP_EV).roundToInt(),
            )
        }
    }

    override fun setFlashAndTorch(enabled: Boolean) {
        flashEnabled = enabled
        applyTorch()
    }

    /**
     * The torch is what makes a flash toggle visible before the shutter is pressed; the per-shot
     * flash mode is set on the settings object in [capture].
     */
    private fun applyTorch() {
        val camera = device ?: return
        if (!camera.hasTorch) return
        withDeviceLock(camera) {
            it.torchMode = if (flashEnabled) AVCaptureTorchModeOn else AVCaptureTorchModeOff
        }
    }

    override fun setZoomRatio(ratio: Float) {
        val camera = device ?: return
        withDeviceLock(camera) { it.videoZoomFactor = ratio.toDouble() }
        _capabilities.update { it.copy(zoomRatio = ratio) }
    }

    override fun focusOn(x: Float, y: Float, lock: Boolean): Boolean {
        val camera = device ?: return false
        val layer = previewView?.previewLayer ?: return false

        // The contract hands over fractions of the preview; AVFoundation wants a point in the
        // *device's* own coordinate space, and only the preview layer knows how its aspect-fill
        // crop maps between the two.
        val viewPoint = layer.bounds.useContents {
            CGPointMake(origin.x + size.width * x, origin.y + size.height * y)
        }
        val devicePoint = layer.captureDevicePointOfInterestForPoint(viewPoint)

        var applied = false
        withDeviceLock(camera) { locked ->
            // Both focus and exposure, because a traveller tapping the shaded carving under a
            // temple roof means "expose for this", not only "sharpen this".
            if (locked.focusPointOfInterestSupported) {
                locked.focusPointOfInterest = devicePoint
                // One-shot auto-focus stays where it landed; continuous keeps hunting, which is
                // what "not locked" means here.
                val mode = if (lock) {
                    AVCaptureFocusModeAutoFocus
                } else {
                    AVCaptureFocusModeContinuousAutoFocus
                }
                if (locked.isFocusModeSupported(mode)) locked.focusMode = mode
                applied = true
            }
            if (locked.exposurePointOfInterestSupported) {
                locked.exposurePointOfInterest = devicePoint
                val mode = if (lock) {
                    AVCaptureExposureModeAutoExpose
                } else {
                    AVCaptureExposureModeContinuousAutoExposure
                }
                if (locked.isExposureModeSupported(mode)) locked.exposureMode = mode
                applied = true
            }
        }
        return applied
    }

    override fun clearFocus() {
        val camera = device ?: return
        val centre = CGPointMake(0.5, 0.5)
        withDeviceLock(camera) { locked ->
            // Back to the centre and back to hunting — the iOS equivalent of cancelling a
            // metering action.
            if (locked.focusPointOfInterestSupported) {
                locked.focusPointOfInterest = centre
                if (locked.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) {
                    locked.focusMode = AVCaptureFocusModeContinuousAutoFocus
                }
            }
            if (locked.exposurePointOfInterestSupported) {
                locked.exposurePointOfInterest = centre
                if (locked.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure)) {
                    locked.exposureMode = AVCaptureExposureModeContinuousAutoExposure
                }
            }
        }
    }

    override fun setExposureIndex(index: Int) {
        val camera = device ?: return
        // Applied optimistically so the slider follows the finger rather than the round trip to
        // the sensor — the same reason the Android device does it.
        _capabilities.update { it.copy(exposureIndex = index) }
        val bias = (index * EXPOSURE_STEP_EV)
            .coerceIn(camera.minExposureTargetBias, camera.maxExposureTargetBias)
        withDeviceLock(camera) { it.setExposureTargetBias(bias, null) }
    }

    override suspend fun capture(targetPath: String): String? {
        val settings = AVCapturePhotoSettings.photoSettings()
        if (photoOutput.supportedFlashModes.isNotEmpty()) {
            settings.flashMode =
                if (flashEnabled) AVCaptureFlashModeOn else AVCaptureFlashModeOff
        }

        val pending = CompletableDeferred<String?>()
        val delegate = PhotoCaptureDelegate(targetPath, pending)
        captureDelegate = delegate
        photoOutput.capturePhotoWithSettings(settings, delegate)

        return try {
            pending.await()
        } finally {
            captureDelegate = null
        }
    }

    override fun unbind() {
        dispatch_async(sessionQueue) {
            if (session.isRunning()) session.stopRunning()
        }
        device?.let { camera ->
            if (camera.hasTorch) {
                withDeviceLock(camera) { it.torchMode = AVCaptureTorchModeOff }
            }
        }
        previewView?.detachPreviewLayer()
        previewView = null
        input?.let { session.removeInput(it) }
        input = null
        device = null
        flashEnabled = false
        _capabilities.value = CameraCapabilities()
    }

    /**
     * Every device write has to sit inside `lockForConfiguration`.
     *
     * Failures are logged rather than thrown: a zoom request that loses the lock to another
     * subsystem is exactly the kind of thing that happens sixty times a second during a pinch,
     * and it is not worth failing a gesture over.
     */
    private inline fun withDeviceLock(camera: AVCaptureDevice, block: (AVCaptureDevice) -> Unit) {
        if (!camera.lockForConfiguration(null)) {
            log.w { "Could not lock the camera for configuration" }
            return
        }
        try {
            block(camera)
        } catch (e: Exception) {
            log.w(e) { "Camera configuration rejected" }
        } finally {
            camera.unlockForConfiguration()
        }
    }

    private companion object {
        /**
         * A third of a stop, the step both platforms' exposure rows are drawn against.
         *
         * CameraX reports its own device step; iOS has none, so this is the value that makes
         * "+1.0 EV" three taps on either platform.
         */
        const val EXPOSURE_STEP_EV = 1f / 3f

        /**
         * Capped well below the hardware maximum, which on recent iPhones runs past 100× and is
         * entirely digital past about 10×.
         */
        const val MAX_ZOOM_RATIO = 10f
    }
}

/**
 * A view whose only job is to keep an `AVCaptureVideoPreviewLayer` the size of itself.
 *
 * `layoutSubviews` rather than a Compose resize callback: the layer is not a view, so auto-layout
 * will not touch it, and UIKit calls this on every bounds change — rotation included — which is
 * exactly when the preview would otherwise be left at its old size.
 */
@OptIn(ExperimentalForeignApi::class)
private class CameraPreviewView : UIView(frame = platform.CoreGraphics.CGRectMake(0.0, 0.0, 0.0, 0.0)) {

    var previewLayer: AVCaptureVideoPreviewLayer? = null
        private set

    fun attachPreviewLayer(session: AVCaptureSession): AVCaptureVideoPreviewLayer {
        previewLayer?.let { return it }
        val created = AVCaptureVideoPreviewLayer(session = session)
        created.setFrame(bounds)
        layer.addSublayer(created)
        previewLayer = created
        return created
    }

    fun detachPreviewLayer() {
        previewLayer?.removeFromSuperlayer()
        previewLayer = null
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.setFrame(bounds)
    }
}

/**
 * Bridges the photo-capture callback onto a coroutine and writes the JPEG.
 *
 * The write happens here rather than in the caller because `fileDataRepresentation()` is only
 * valid inside the callback — the photo object is released once it returns.
 */
@OptIn(ExperimentalForeignApi::class)
private class PhotoCaptureDelegate(
    private val targetPath: String,
    private val pending: CompletableDeferred<String?>,
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {

    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?,
    ) {
        if (error != null) {
            log.e { "Capture failed: ${error.localizedDescription}" }
            pending.complete(null)
            return
        }
        val data = didFinishProcessingPhoto.fileDataRepresentation()
        if (data == null) {
            log.e { "Capture produced no data" }
            pending.complete(null)
            return
        }
        pending.complete(if (data.writeToFile(targetPath, true)) targetPath else null)
    }
}

/** Referenced so the CoreGraphics import reads as the frame type it is. */
@OptIn(ExperimentalForeignApi::class)
private typealias PreviewFrame = CGRect
