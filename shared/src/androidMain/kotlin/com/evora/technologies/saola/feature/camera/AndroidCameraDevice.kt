package com.evora.technologies.saola.feature.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ZoomState
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.evora.technologies.saola.core.util.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
actual fun rememberCameraController(): CameraController {
    val context = LocalContext.current
    return remember(context) { CameraController(AndroidCameraDevice(context)) }
}

/** CameraX behind the shared [CameraDevice] contract. */
internal class AndroidCameraDevice(
    private val context: Context,
) : CameraDevice {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: androidx.camera.core.Camera? = null

    /**
     * The view the preview is currently drawn into.
     *
     * Held because tap-to-focus has to ask *it* to translate a touch: only the PreviewView
     * knows how its surface was scaled and rotated to fill the screen, and a metering point
     * built from raw view pixels would focus somewhere else entirely on a cropped,
     * full-bleed preview.
     */
    private var previewView: PreviewView? = null

    private val _capabilities = MutableStateFlow(CameraCapabilities())
    override val capabilities: StateFlow<CameraCapabilities> = _capabilities.asStateFlow()

    private var observedZoom: LiveData<ZoomState>? = null
    private val zoomObserver = Observer<ZoomState> { zoom ->
        _capabilities.update {
            it.copy(
                zoomRatio = zoom.zoomRatio,
                minZoomRatio = zoom.minZoomRatio,
                maxZoomRatio = zoom.maxZoomRatio,
            )
        }
    }

    @Composable
    override fun Preview(lensFacing: LensFacing, modifier: Modifier) {
        val viewContext = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val view = remember(viewContext) {
            PreviewView(viewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // COMPATIBLE, not PERFORMANCE: a TextureView composites correctly under the
                // rounded corners and blurred overlays the lens screen draws on top, which a
                // SurfaceView punches straight through.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }

        // Keyed on facing so flipping the camera rebinds, and on the view so a recreated
        // surface gets a session again.
        LaunchedEffect(view, lensFacing) {
            runCatching { bind(lifecycleOwner, view, lensFacing) }
                .onFailure { log.e(it) { "Could not bind the $lensFacing camera" } }
        }

        DisposableEffect(view) { onDispose { unbind() } }

        AndroidView(factory = { view }, modifier = modifier)
    }

    /**
     * Binds (or rebinds) the camera.
     *
     * @throws IllegalStateException if the device has no camera on that side.
     */
    private suspend fun bind(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: LensFacing,
    ) {
        val provider = awaitCameraProvider()
        cameraProvider = provider
        this.previewView = previewView

        val selector = lensFacing.toSelector()
        check(provider.hasCamera(selector)) { "No camera facing $lensFacing" }

        // One selector shared by both use cases, pinned to the sensor's native 4:3 —
        // the largest, least interpolated frame the hardware offers. It is no longer
        // the shape of what gets saved: the view port below cuts both use cases down
        // to the rectangle the traveller is actually looking at.
        val resolution = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolution)
            .build()
            .apply { surfaceProvider = previewView.surfaceProvider }

        val capture = ImageCapture.Builder()
            .setResolutionSelector(resolution)
            // Latency wins over the last few percent of quality: the image is
            // downscaled to 1024 px before upload anyway.
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        // What the traveller sees is what gets saved.
        //
        // The viewfinder is whatever rectangle the screen has room for, which is not
        // the sensor's 4:3, so the preview surface is scaled to fill it and the edges
        // that fall outside are simply not shown. A view port makes CameraX apply that
        // same crop to the still: without one the JPEG keeps the parts that were cut
        // off on screen, and the traveller gets a photo — and a recognition — of a
        // scene they never framed. Null only if the view has not been measured, which
        // the screen already guards against by binding after layout.
        val group = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(capture)
            .apply { previewView.viewPort?.let { setViewPort(it) } }
            .build()

        releaseZoomObserver()
        provider.unbindAll()
        camera = provider.bindToLifecycle(lifecycleOwner, selector, group)
        imageCapture = capture

        readCapabilities(provider)

        // Worth a line in the log: a fallback ratio the device silently substituted,
        // or a view port that arrived null, is invisible on screen but shows up in
        // every photo the traveller keeps.
        log.d {
            "Bound $lensFacing at 4:3 — preview ${preview.resolutionInfo?.resolution}, " +
                "capture ${capture.resolutionInfo?.resolution} " +
                "crop ${capture.resolutionInfo?.cropRect}, " +
                "view port ${previewView.viewPort?.aspectRatio ?: "none"}"
        }
    }

    /**
     * Bridges CameraX's `ListenableFuture` into a coroutine.
     *
     * Written by hand rather than pulled in with `kotlinx-coroutines-guava` — one
     * suspension point is not worth another dependency on the app's classpath.
     */
    private suspend fun awaitCameraProvider(): ProcessCameraProvider {
        val future = ProcessCameraProvider.getInstance(context)
        return suspendCancellableCoroutine { continuation ->
            future.addListener(
                {
                    try {
                        continuation.resume(future.get())
                    } catch (e: ExecutionException) {
                        // Unwrapped: the screen shows `message` to the traveller, and
                        // "java.util.concurrent.ExecutionException" is not a sentence.
                        continuation.resumeWithException(e.cause ?: e)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
            continuation.invokeOnCancellation { future.cancel(false) }
        }
    }

    /**
     * Reads what the freshly bound camera can do.
     *
     * Zoom arrives as a `LiveData` rather than a one-off value because CameraX also
     * changes it on its own — switching to the ultrawide, or clamping a ratio the
     * lens cannot reach — and the zoom row has to show the number that is really
     * in force, not the one that was asked for.
     */
    private fun readCapabilities(provider: ProcessCameraProvider) {
        val info = camera?.cameraInfo ?: return
        val exposure = info.exposureState
        _capabilities.update {
            it.copy(
                hasFlash = info.hasFlashUnit(),
                hasFrontCamera = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA),
                // Read fresh on every bind: the range differs between the two cameras,
                // and a rebind resets the compensation the traveller had dialled in.
                exposureRange = if (exposure.isExposureCompensationSupported) {
                    exposure.exposureCompensationRange.lower..exposure.exposureCompensationRange.upper
                } else {
                    IntRange.EMPTY
                },
                exposureStepEv = if (exposure.isExposureCompensationSupported) {
                    exposure.exposureCompensationStep.toFloat()
                } else {
                    0f
                },
                exposureIndex = exposure.exposureCompensationIndex,
            )
        }
        val zoomState = info.zoomState
        zoomState.observeForever(zoomObserver)
        observedZoom = zoomState
    }

    private fun releaseZoomObserver() {
        observedZoom?.removeObserver(zoomObserver)
        observedZoom = null
    }

    override fun setFlashAndTorch(enabled: Boolean) {
        imageCapture?.flashMode =
            if (enabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        // Always pushed, even when off: a front camera has no torch to turn on but
        // the back one may still be lit from before the switch.
        camera?.cameraControl?.enableTorch(enabled)
    }

    override fun setZoomRatio(ratio: Float) {
        val control = camera?.cameraControl ?: return
        // The future is deliberately dropped: during a pinch nearly every request is
        // superseded by the next frame's, and CameraX reports that as a failure.
        runCatching { control.setZoomRatio(ratio) }
            .onFailure { log.w(it) { "Zoom request rejected" } }
    }

    /**
     * Both AF and AE, because a traveller tapping the shaded carving under a temple
     * roof means "expose for this", not only "sharpen this".
     */
    override fun focusOn(x: Float, y: Float, lock: Boolean): Boolean {
        val view = previewView ?: return false
        val control = camera?.cameraControl ?: return false
        // Before frames flow — the first moments after a bind or a lens switch — the
        // view has no transform to map the touch through and rejects every point.
        if (view.previewStreamState.value != PreviewView.StreamState.STREAMING) return false

        // The contract hands over fractions; the metering factory wants view pixels.
        val point = view.meteringPointFactory.createPoint(x * view.width, y * view.height)
        val action = FocusMeteringAction
            .Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .apply {
                // Otherwise hand control back after a few seconds: a locked focus the
                // traveller cannot see is worse than an automatic one.
                if (lock) {
                    disableAutoCancel()
                } else {
                    setAutoCancelDuration(FOCUS_AUTO_CANCEL_SECONDS, TimeUnit.SECONDS)
                }
            }
            .build()
        return runCatching { control.startFocusAndMetering(action) }
            .onFailure { log.w(it) { "Focus request rejected" } }
            .isSuccess
    }

    override fun clearFocus() {
        camera?.cameraControl?.cancelFocusAndMetering()
    }

    /**
     * Applied optimistically to [capabilities] because there is no LiveData for it:
     * the slider has to follow the finger, not the round trip to the sensor.
     */
    override fun setExposureIndex(index: Int) {
        val control = camera?.cameraControl ?: return
        _capabilities.update { it.copy(exposureIndex = index) }
        runCatching { control.setExposureCompensationIndex(index) }
            .onFailure { log.w(it) { "Exposure request rejected" } }
    }

    override suspend fun capture(targetPath: String): String? {
        val capture = imageCapture ?: return null
        val target = File(targetPath)

        // Deliberately no `isReversedHorizontal` for the front camera: it only writes
        // a mirrored EXIF orientation, and the capture store reads just the three
        // rotate values — a mirrored tag would fall through to zero degrees and hand
        // Gemini a sideways selfie.
        val options = ImageCapture.OutputFileOptions.Builder(target).build()

        return suspendCancellableCoroutine { continuation ->
            capture.takePicture(
                options,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        if (continuation.isActive) continuation.resume(target.absolutePath)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        log.e(exception) { "Capture failed" }
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }
    }

    override fun unbind() {
        releaseZoomObserver()
        cameraProvider?.unbindAll()
        cameraProvider = null
        imageCapture = null
        camera = null
        previewView = null
        _capabilities.value = CameraCapabilities()
    }

    private fun LensFacing.toSelector(): CameraSelector = when (this) {
        LensFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        LensFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
    }

    private companion object {
        const val FOCUS_AUTO_CANCEL_SECONDS = 4L
    }
}
