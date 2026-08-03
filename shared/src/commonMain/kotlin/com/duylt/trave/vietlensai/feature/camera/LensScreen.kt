package com.duylt.trave.vietlensai.feature.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.action_close
import com.duylt.trave.vietlensai.resources.analysing_history
import com.duylt.trave.vietlensai.resources.analysing_recognising
import com.duylt.trave.vietlensai.resources.analysing_stories
import com.duylt.trave.vietlensai.resources.analysing_title
import com.duylt.trave.vietlensai.resources.camera_ae_af_locked
import com.duylt.trave.vietlensai.resources.camera_cancel_timer
import com.duylt.trave.vietlensai.resources.camera_capture
import com.duylt.trave.vietlensai.resources.camera_flash_off
import com.duylt.trave.vietlensai.resources.camera_flash_on
import com.duylt.trave.vietlensai.resources.camera_grid_hide
import com.duylt.trave.vietlensai.resources.camera_grid_show
import com.duylt.trave.vietlensai.resources.camera_hint_artifact
import com.duylt.trave.vietlensai.resources.camera_hint_auto
import com.duylt.trave.vietlensai.resources.camera_hint_food
import com.duylt.trave.vietlensai.resources.camera_hint_landmark
import com.duylt.trave.vietlensai.resources.camera_hint_translate
import com.duylt.trave.vietlensai.resources.camera_mode_selector
import com.duylt.trave.vietlensai.resources.camera_open_journal
import com.duylt.trave.vietlensai.resources.camera_permission_blocked_body
import com.duylt.trave.vietlensai.resources.camera_permission_body
import com.duylt.trave.vietlensai.resources.camera_permission_grant
import com.duylt.trave.vietlensai.resources.camera_permission_title
import com.duylt.trave.vietlensai.resources.camera_pick_gallery
import com.duylt.trave.vietlensai.resources.camera_switch_lens
import com.duylt.trave.vietlensai.resources.camera_timer_10
import com.duylt.trave.vietlensai.resources.camera_timer_3
import com.duylt.trave.vietlensai.resources.camera_timer_off
import com.duylt.trave.vietlensai.resources.camera_translate_auto
import com.duylt.trave.vietlensai.resources.camera_translate_auto_detect
import com.duylt.trave.vietlensai.resources.camera_translate_from
import com.duylt.trave.vietlensai.resources.camera_translate_pick_from
import com.duylt.trave.vietlensai.resources.camera_translate_pick_to
import com.duylt.trave.vietlensai.resources.camera_translate_swap
import com.duylt.trave.vietlensai.resources.camera_translate_to
import com.duylt.trave.vietlensai.resources.error_missing_api_key
import com.duylt.trave.vietlensai.resources.language_en
import com.duylt.trave.vietlensai.resources.language_es
import com.duylt.trave.vietlensai.resources.language_fr
import com.duylt.trave.vietlensai.resources.language_ja
import com.duylt.trave.vietlensai.resources.language_ko
import com.duylt.trave.vietlensai.resources.language_th
import com.duylt.trave.vietlensai.resources.language_vi
import com.duylt.trave.vietlensai.resources.language_zh
import com.duylt.trave.vietlensai.resources.location_permission_blocked_body
import com.duylt.trave.vietlensai.resources.location_permission_body
import com.duylt.trave.vietlensai.resources.location_permission_grant
import com.duylt.trave.vietlensai.resources.location_permission_later
import com.duylt.trave.vietlensai.resources.location_permission_title
import com.duylt.trave.vietlensai.resources.mode_artifact
import com.duylt.trave.vietlensai.resources.mode_auto
import com.duylt.trave.vietlensai.resources.mode_food
import com.duylt.trave.vietlensai.resources.mode_landmark
import com.duylt.trave.vietlensai.resources.mode_translate
import com.duylt.trave.vietlensai.resources.permission_open_settings
import com.duylt.trave.vietlensai.resources.settings_title
import com.duylt.trave.vietlensai.core.designsystem.component.AppSnackbarHost
import com.duylt.trave.vietlensai.core.designsystem.component.EmptyState
import com.duylt.trave.vietlensai.core.designsystem.component.PermissionSheet
import com.duylt.trave.vietlensai.core.designsystem.component.showError
import com.duylt.trave.vietlensai.core.designsystem.theme.InkBrown
import com.duylt.trave.vietlensai.core.designsystem.theme.InkDarkest
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.core.mvi.CollectEffects
import com.duylt.trave.vietlensai.core.util.rememberCameraPermissionState
import com.duylt.trave.vietlensai.core.util.rememberLocationPermissionState
import com.duylt.trave.vietlensai.core.util.toUserMessage
import com.duylt.trave.vietlensai.platform.rememberPhotoPicker
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.LensMode
import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.util.AppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.compose.resources.StringResource

/**
 * The app's home: a live viewfinder with one capture button.
 *
 * Everything else on this screen is deliberately subordinate to the shutter — the
 * whole product promise is "point and ask", so nothing may sit between opening the
 * app and taking a photo.
 *
 * The screen is laid out as a column rather than as chrome floating over a
 * full-bleed preview: camera tools above the frame, the frame itself as a card, then
 * the mode chips and the shutter row under it. Two things come out of that. The
 * picture is never covered by a control, and everything the traveller taps sits in
 * the bottom third, where a thumb already is — the mode picker most of all, which
 * used to be a dropdown in the far top corner.
 */
@Composable
fun LensRoute(
    onDiscoveryCaptured: (String) -> Unit,
    /** Photo path, source language code (blank for detect), target language code. */
    onTranslationCaptured: (String, String, String) -> Unit,
    onOpenJournal: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LensViewModel = koinViewModel(),
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
            is LensEffect.ShowMessage -> Unit // rendered inline by ErrorBanner
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

    LensScreen(
        state = state,
        controller = controller,
        onIntent = viewModel::onIntent,
        onOpenJournal = onOpenJournal,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
private fun LensScreen(
    state: LensState,
    controller: CameraController,
    onIntent: (LensIntent) -> Unit,
    onOpenJournal: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Both grants, and what asking again would do, behind one platform-neutral state.
    // `requestOrOpenSettings` is the whole decision the prompts need: ask while the OS still
    // asks, and hand over to the app's own settings page once it has stopped.
    val cameraPermission = rememberCameraPermissionState()
    val locationPermission = rememberLocationPermissionState()

    val capabilities by controller.capabilities.collectAsStateWithLifecycle()
    val zoom = rememberZoomDriver(controller)

    val snackbarHostState = remember { SnackbarHostState() }
    MissingKeySnackbar(
        hasApiKey = state.hasApiKey,
        hostState = snackbarHostState,
        onOpenSettings = onOpenSettings,
    )
    CaptureErrorSnackbar(
        error = state.error,
        hostState = snackbarHostState,
        onDismiss = { onIntent(LensIntent.DismissError) },
    )

    Box(modifier = modifier.fillMaxSize().background(InkDarkest)) {
        Column(
            modifier = Modifier.fillMaxSize().screenInsetsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CameraToolRow(
                state = state,
                capabilities = capabilities,
                onIntent = onIntent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenGutter, vertical = 8.dp),
            )

            Box(
                modifier = Modifier
                    // The frame takes the same gutter as every other row and all the
                    // height the tools and the shutter leave. It is deliberately not
                    // pinned to the sensor's 4:3: letterboxed to that ratio it came
                    // out narrower than the chips under it, and a viewfinder that
                    // does not line up with its own controls reads as a bug. What
                    // the shape costs — the preview no longer being the whole sensor
                    // frame — is paid back by the view port in `CameraController`,
                    // which crops the photo to exactly this rectangle.
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = ScreenGutter)
                    .clip(FRAME_SHAPE)
                    .background(InkBrown)
                    .border(width = 1.dp, color = Marigold.copy(alpha = 0.22f), shape = FRAME_SHAPE),
            ) {
                if (cameraPermission.isGranted) {
                    Viewfinder(
                        controller = controller,
                        state = state,
                        capabilities = capabilities,
                        onPinch = zoom::pinch,
                        modifier = Modifier.fillMaxSize(),
                    )

                    CaptureHintBubble(
                        mode = state.mode,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
                    )
                } else {
                    CameraPermissionPrompt(
                        deniedForGood = cameraPermission.isDeniedForGood,
                        onRequest = cameraPermission::requestOrOpenSettings,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    )
                }

                // The two controls that belong to the picture rather than to the
                // phone — what is being read, and how close — sit inside the frame,
                // where the eye already is. The camera's own settings stay up in the
                // tool row, off the image entirely.
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedVisibility(
                        visible = state.mode == LensMode.TRANSLATE,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        TranslateLanguageBar(
                            from = state.translateFrom,
                            to = state.translateTo,
                            onIntent = onIntent,
                            // Carried by the bar rather than by the column's spacing:
                            // an arrangement gap would also be held open by the
                            // collapsed bar, and lift the dial off the frame's edge
                            // in every mode but this one.
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }

                    if (capabilities.canZoom) {
                        ZoomDial(
                            capabilities = capabilities,
                            onZoomProgress = zoom::dial,
                            onZoomGlideTo = zoom::glideTo,
                        )
                    }
                }
            }

            ModeChipRow(
                modes = state.availableModes,
                selected = state.mode,
                onSelect = { onIntent(LensIntent.SelectMode(it)) },
                modifier = Modifier.padding(top = 14.dp, bottom = 14.dp),
            )

            ShutterRow(
                state = state,
                onIntent = onIntent,
                onOpenJournal = onOpenJournal,
                cameraReady = cameraPermission.isGranted,
            )
        }

        if (state.isCountingDown) {
            CountdownOverlay(seconds = state.countdown, modifier = Modifier.fillMaxSize())
        }

        AnimatedVisibility(
            visible = state.isAnalysing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            AnalysingOverlay(stage = state.analysisStage)
        }

        // Every transient message — a failure as much as a warning — goes through
        // this one host, so two of them can never land on top of each other above
        // the controls. Floated over the layout rather than placed in it: pushed
        // into the column, a notice would shrink the frame it is reporting on and
        // resize the live preview with it.
        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MESSAGES_BOTTOM_INSET, start = ScreenGutter, end = ScreenGutter),
        )

        // Asked here rather than beside the camera permission, and only once the
        // viewfinder is actually live: two system dialogs stacked on a cold start
        // are two things to dismiss before the app has shown itself doing anything,
        // and the second one gets refused on reflex.
        if (cameraPermission.isGranted && !locationPermission.isGranted &&
            !state.hasAskedLocation && !state.isBusy
        ) {
            LocationPermissionSheet(
                deniedForGood = locationPermission.isDeniedForGood,
                // Coarse only. It is enough to tell one province from another, which is all
                // the passport asks of it; on Android 12+ it spares the traveller the
                // precise/approximate choice for an answer the app would not use, and on iOS
                // it is the "when in use" grant rather than "always".
                onAllow = locationPermission::requestOrOpenSettings,
                // The only answer that puts the question away for good. A refusal at
                // the system dialog leaves the sheet up, so the flag stays unwritten
                // until the traveller says so here.
                onLater = { onIntent(LensIntent.LocationAsked) },
            )
        }
    }
}

/**
 * Surfaces the missing-key warning as a snackbar instead of a permanent banner.
 *
 * `showError` dismisses whatever is on screen before this one goes up. The warning
 * also comes down the moment a key arrives: it is a standing condition rather than
 * an event, so leaving it up would be reporting a problem the traveller just fixed.
 */
@Composable
private fun MissingKeySnackbar(
    hasApiKey: Boolean,
    hostState: SnackbarHostState,
    onOpenSettings: () -> Unit,
) {
    val message = stringResource(Res.string.error_missing_api_key)
    val action = stringResource(Res.string.settings_title)

    LaunchedEffect(hasApiKey) {
        if (hasApiKey) {
            hostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }

        val result = hostState.showError(
            message = message,
            actionLabel = action,
            withDismissAction = true,
        )
        if (result == SnackbarResult.ActionPerformed) onOpenSettings()
    }
}

/**
 * Surfaces a capture or recognition failure as the app's red snackbar.
 *
 * The error is cleared once the notice leaves the screen, however it left — by
 * timeout, by swipe or by the dismiss action. Left in state it would be a failure
 * the traveller can no longer see and no longer dismiss, and the next identical
 * failure would never re-announce itself.
 */
@Composable
private fun CaptureErrorSnackbar(
    error: AppError?,
    hostState: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    val message = error?.toUserMessage()

    LaunchedEffect(error) {
        if (message == null) return@LaunchedEffect
        hostState.showError(message = message, withDismissAction = true)
        onDismiss()
    }
}

/**
 * The viewfinder and everything drawn *inside* the frame: grid, pinch, focus ring.
 *
 * Binding lives here rather than in the parent so a lens switch is expressed the
 * declarative way — the facing is a key, and changing it rebinds.
 */
@Composable
private fun Viewfinder(
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
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = InkBrown,
                modifier = Modifier
                    // In the corner rather than centred: the middle of the top edge
                    // is where the framing tip lands, and two gold pills stacked on
                    // one another read as a single unreadable one.
                    .align(Alignment.TopStart)
                    .padding(top = 14.dp, start = 14.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Marigold)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
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
 * The tap-to-focus reticle, with the exposure slider the stock camera puts beside it.
 *
 * It snaps in, holds, and fades out on its own — unless the focus is locked or the
 * traveller is still dragging the sun, either of which means they are not finished.
 */
@Composable
private fun FocusReticle(
    position: Offset,
    locked: Boolean,
    capabilities: CameraCapabilities,
    frameSize: IntSize,
    onExposureIndex: (Int) -> Unit,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val ringPx = with(density) { FOCUS_RING_SIZE.toPx() }
    val trackPx = with(density) { EV_TRACK_HEIGHT.toPx() }
    val trackWidthPx = with(density) { EV_TRACK_WIDTH.toPx() }

    val scale = remember { Animatable(FOCUS_RING_OVERSHOOT) }
    val fade = remember { Animatable(1f) }
    var adjusting by remember(position) { mutableStateOf(false) }
    var evAccumulator by remember(position) { mutableFloatStateOf(0f) }

    LaunchedEffect(position) {
        scale.snapTo(FOCUS_RING_OVERSHOOT)
        scale.animateTo(1f, tween(durationMillis = 200))
    }
    LaunchedEffect(position, locked, adjusting) {
        fade.snapTo(1f)
        if (locked || adjusting) return@LaunchedEffect
        delay(FOCUS_RETICLE_HOLD_MILLIS)
        fade.animateTo(0f, tween(durationMillis = 350))
        onFinished()
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (position.x - ringPx / 2f).roundToInt(),
                    y = (position.y - ringPx / 2f).roundToInt(),
                )
            }
            .size(FOCUS_RING_SIZE)
            .scale(scale.value)
            .alpha(fade.value)
            .border(
                width = if (locked) 2.5.dp else 1.5.dp,
                color = Marigold,
                shape = CircleShape,
            ),
    )

    if (!capabilities.canAdjustExposure) return

    // Beside the ring, and on whichever side has room — a slider half off the screen
    // cannot be dragged.
    val onRight = position.x + ringPx / 2f + trackWidthPx < frameSize.width
    val trackX = if (onRight) {
        position.x + ringPx / 2f + with(density) { 4.dp.toPx() }
    } else {
        position.x - ringPx / 2f - trackWidthPx - with(density) { 4.dp.toPx() }
    }
    val span = (capabilities.exposureRange.last - capabilities.exposureRange.first).toFloat()

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = trackX.roundToInt(),
                    y = (position.y - trackPx / 2f).roundToInt(),
                )
            }
            .size(width = EV_TRACK_WIDTH, height = EV_TRACK_HEIGHT)
            .alpha(fade.value)
            // Swallows taps so adjusting exposure never doubles as a refocus on a
            // point a few pixels from the one already metered.
            .pointerInput(Unit) { detectTapGestures { } }
            .pointerInput(capabilities.exposureRange, position) {
                if (span <= 0f) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { adjusting = true },
                    onDragEnd = { adjusting = false },
                    onDragCancel = { adjusting = false },
                ) { change, dragAmount ->
                    change.consume()
                    // Up is brighter, the way every camera and every lift button works.
                    evAccumulator = (evAccumulator - dragAmount * span / trackPx)
                        .coerceIn(
                            capabilities.exposureRange.first.toFloat(),
                            capabilities.exposureRange.last.toFloat(),
                        )
                    onExposureIndex(evAccumulator.roundToInt())
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        val progress = if (span <= 0f) {
            0.5f
        } else {
            (capabilities.exposureIndex - capabilities.exposureRange.first) / span
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val x = size.width / 2f
            drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }

        Icon(
            imageVector = Icons.Filled.WbSunny,
            contentDescription = null,
            tint = Marigold,
            modifier = Modifier
                .offset { IntOffset(0, ((1f - progress) * (trackPx - trackWidthPx)).roundToInt()) }
                .size(EV_TRACK_WIDTH)
                .padding(2.dp),
        )

        if (capabilities.exposureIndex != 0) {
            Text(
                text = capabilities.exposureEv.formatEv(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = InkBrown,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Allowed past the track's width, which is only as wide as the
                    // sun: "+1.0" folded into two lines otherwise.
                    .wrapContentWidth(unbounded = true)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Marigold)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/**
 * Flash, grid, lens switch and self-timer, spread across one row above the frame.
 *
 * A row rather than the rail that used to run down the side of the preview: these
 * four are the phone's settings rather than the picture's, so they belong off the
 * image entirely — and a rail over the frame covered the one thing this screen is
 * for. Controls the hardware cannot honour are absent rather than greyed out; a
 * phone with one camera should not be told it is missing something.
 */
@Composable
private fun CameraToolRow(
    state: LensState,
    capabilities: CameraCapabilities,
    onIntent: (LensIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (capabilities.hasFlash) {
            ToolButton(
                icon = if (state.flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                description = stringResource(
                    if (state.flashEnabled) Res.string.camera_flash_off else Res.string.camera_flash_on,
                ),
                active = state.flashEnabled,
                role = Role.Switch,
                onClick = { onIntent(LensIntent.ToggleFlash) },
            )
        }

        ToolButton(
            icon = Icons.Filled.Grid3x3,
            description = stringResource(
                if (state.gridEnabled) Res.string.camera_grid_hide else Res.string.camera_grid_show,
            ),
            active = state.gridEnabled,
            role = Role.Switch,
            onClick = { onIntent(LensIntent.ToggleGrid) },
        )

        if (capabilities.hasFrontCamera) {
            ToolButton(
                icon = Icons.Filled.Cameraswitch,
                description = stringResource(Res.string.camera_switch_lens),
                active = state.lensFacing == LensFacing.FRONT,
                onClick = { onIntent(LensIntent.SwitchLens) },
            )
        }

        ToolButton(
            icon = if (state.timer == CaptureTimer.OFF) Icons.Filled.TimerOff else Icons.Filled.Timer,
            description = stringResource(state.timer.labelRes()),
            active = state.timer != CaptureTimer.OFF,
            badge = if (state.timer == CaptureTimer.OFF) null else "${state.timer.seconds}",
            onClick = { onIntent(LensIntent.CycleTimer) },
        )
    }
}

/**
 * One round camera control.
 *
 * Active is marigold-on-ink rather than a subtle tint: whether the flash is armed is
 * the kind of thing a traveller checks at a glance in a dark temple, and a state
 * that has to be looked for twice is a state that gets missed.
 */
@Composable
private fun ToolButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Button,
    badge: String? = null,
) {
    val content = if (active) InkBrown else PaperCream
    Box(
        modifier = modifier
            .size(TOOL_BUTTON_SIZE)
            .clip(CircleShape)
            .background(if (active) Marigold else PaperCream.copy(alpha = 0.12f))
            .clickable(role = role, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = content,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 5.dp, bottom = 3.dp),
            )
        }
    }
}

/**
 * The mode picker, as a row of chips between the frame and the shutter.
 *
 * It was a dropdown in the top-left corner, which is the one part of a phone a hand
 * holding it cannot reach — and it hid four of the five modes behind a tap, so a
 * traveller who never opened it never learned the app could read a menu. As chips
 * the whole set is legible at rest and every one of them is under the thumb that is
 * about to press the shutter. Scrollable because a longer translation of five
 * labels can outrun a narrow screen.
 */
@Composable
private fun ModeChipRow(
    modes: List<LensMode>,
    selected: LensMode,
    onSelect: (LensMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.camera_mode_selector)
    val listState = rememberLazyListState()

    // Five labels of unknown length in whatever language the phone is set to will
    // eventually outrun a narrow screen, so the row scrolls — and then has to make
    // sure the chosen mode is not the one off the edge. This matters on the way *in*
    // rather than on a tap: coming back to the tab restores a mode the traveller
    // picked ages ago, and a screen that opens claiming "Auto" while it is set to
    // translate is worse than one that scrolls.
    LaunchedEffect(selected, modes) {
        val index = modes.indexOf(selected)
        if (index < 0) return@LaunchedEffect
        val info = listState.layoutInfo
        val chip = info.visibleItemsInfo.firstOrNull { it.index == index }
        val whollyVisible = chip != null &&
            chip.offset >= info.viewportStartOffset &&
            chip.offset + chip.size <= info.viewportEndOffset
        if (!whollyVisible) listState.animateScrollToItem(index)
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = ScreenGutter),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(modes) { mode ->
            val active = mode == selected
            Text(
                text = stringResource(mode.labelRes()),
                // A step under the app's label size, which is set for reading
                // paragraphs: five chips have to share one line, and this is a row of
                // switches rather than something anybody reads.
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = if (active) InkBrown else PaperCream,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (active) Marigold else PaperCream.copy(alpha = 0.12f))
                    .clickable(
                        role = Role.RadioButton,
                        onClickLabel = label,
                        onClick = { onSelect(mode) },
                    )
                    // Spoken as well as coloured: the chip row is a single choice, and
                    // TalkBack has no way to hear a gold background.
                    .semantics { this.selected = active }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

/**
 * The last row: the camera roll, the shutter, and the pile of recent captures.
 *
 * Three things, and the middle one is the point of the screen — which is why the two
 * either side are given equal weight rather than sized to their contents. The
 * shutter stays on the screen's axis however wide the pile grows.
 *
 * Only the shutter answers to `cameraReady`. The other two never touch the camera —
 * the roll goes out to the system picker and the pile opens the journal — so a
 * refused camera grant must not take them down with it. A traveller who said no, or
 * who has not been asked yet, can still hand the app a photo they already have.
 */
@Composable
private fun ShutterRow(
    state: LensState,
    onIntent: (LensIntent) -> Unit,
    onOpenJournal: () -> Unit,
    /** Whether the camera permission is granted — the shutter's business alone. */
    cameraReady: Boolean,
    modifier: Modifier = Modifier,
) {
    // Hands back a path already copied into app storage, so a picked photo travels the
    // same pipeline as a captured one. One at a time: recognition is about the single
    // thing the traveller is standing in front of.
    val pickPhoto = rememberPhotoPicker(maxItems = 1) { paths ->
        paths.firstOrNull()?.let { onIntent(LensIntent.PhotoPicked(it)) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = ScreenGutter, end = ScreenGutter, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            GalleryButton(
                // Deliberately not gated on the camera grant: the picker asks the
                // system for one photo and hands back a copy, which needs no
                // permission of its own on either platform.
                enabled = !state.isBusy,
                onClick = pickPhoto,
            )
        }

        ShutterButton(
            // Stays live through a countdown: while the number is on screen the
            // shutter is the only way to call the photo off. Not `!isBusy` for
            // exactly that reason.
            enabled = cameraReady && !state.isAnalysing && !state.isCapturing,
            counting = state.isCountingDown,
            onClick = { onIntent(LensIntent.ShutterPressed) },
        )

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            RecentCaptureStack(
                discoveries = state.recentDiscoveries,
                // Blocking pointers under the scrim does nothing for TalkBack,
                // which navigates by semantics rather than by touch.
                enabled = !state.isBusy,
                onClick = onOpenJournal,
            )
        }
    }
}

/**
 * The "from → to" pair, on screen only while translate is the chosen mode.
 *
 * It sits at the foot of the frame rather than in the tool row because it belongs to
 * the picture, not to the camera: it is the last thing checked before pressing, and
 * the top of the frame is usually where the sign itself is. One capsule holds both
 * ends and the swap between them, so the pair reads as a single sentence instead of
 * as three unrelated controls.
 */
@Composable
private fun TranslateLanguageBar(
    from: TranslateLanguage?,
    to: TranslateLanguage,
    onIntent: (LensIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Nothing to exchange until the source is a real language: swapping "detect it"
    // into the target would ask the translator to produce text in no language at all.
    val canSwap = from != null

    Row(
        modifier = modifier
            // The frame's width less a gutter, with equal halves either side of
            // the swap, so the button stays on the frame's axis. Sized to the two
            // names instead, it slid left and right as they changed length — and
            // the one control the traveller reaches for without looking was never
            // twice in the same place.
            .fillMaxWidth()
            .padding(horizontal = TRANSLATE_BAR_MARGIN)
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LanguageChip(
            modifier = Modifier.weight(1f),
            selected = from,
            // The other end is struck off rather than left selectable: translating
            // English into English is not a request anyone means to make, and a
            // picker that silently reshuffled the far side to fix it would move a
            // choice the traveller had already made.
            unavailable = to,
            autoAllowed = true,
            description = stringResource(Res.string.camera_translate_from),
            sheetTitle = stringResource(Res.string.camera_translate_pick_from),
            onSelect = { onIntent(LensIntent.SelectTranslateFrom(it)) },
        )

        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (canSwap) 0.16f else 0.06f))
                .clickable(
                    enabled = canSwap,
                    role = Role.Button,
                    onClick = { onIntent(LensIntent.SwapTranslateLanguages) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = stringResource(Res.string.camera_translate_swap),
                tint = Color.White.copy(alpha = if (canSwap) 1f else 0.35f),
                modifier = Modifier.size(20.dp),
            )
        }

        LanguageChip(
            modifier = Modifier.weight(1f),
            selected = to,
            // Null while the source is being detected, which strikes nothing off:
            // any target is still a sensible answer until the sign is read.
            unavailable = from,
            autoAllowed = false,
            description = stringResource(Res.string.camera_translate_to),
            sheetTitle = stringResource(Res.string.camera_translate_pick_to),
            // Never null with the auto entry withheld, so the target keeps its
            // non-null type rather than being made nullable to suit the picker.
            onSelect = { language -> language?.let { onIntent(LensIntent.SelectTranslateTo(it)) } },
        )
    }
}

/**
 * One end of the translation, as a flag and a name that opens the picker.
 *
 * A sheet rather than a dropdown: the list is nine entries with a gloss on most of
 * them, which a menu anchored to a chip renders as a narrow column of clipped
 * names over the live preview. The sheet gets a title saying which end is being
 * chosen — the one thing a menu hanging off the chip never had room to say.
 */
@Composable
private fun LanguageChip(
    selected: TranslateLanguage?,
    /** The language the other end has taken; listed but not choosable. */
    unavailable: TranslateLanguage?,
    autoAllowed: Boolean,
    description: String,
    sheetTitle: String,
    onSelect: (TranslateLanguage?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }

    // Centred in the half the bar gives it: the chip is what moves when a name
    // changes length, and the swap button beside it does not.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .clickable(role = Role.Button, onClickLabel = description) { picking = true }
                .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected == null) {
                // A globe rather than a flag: "work it out" is not a country.
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(text = selected.flag, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = selected?.displayName ?: stringResource(Res.string.camera_translate_auto),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Capped a little under the half the bar allots, so the longest
                // name still leaves the chevron beside it its full width.
                modifier = Modifier.widthIn(max = 80.dp),
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (picking) {
        LanguageSheet(
            title = sheetTitle,
            selected = selected,
            unavailable = unavailable,
            autoAllowed = autoAllowed,
            onSelect = onSelect,
            onDismiss = { picking = false },
        )
    }
}

/**
 * The language list, as a sheet.
 *
 * Choosing dismisses it rather than waiting for a confirm button: there is one
 * decision here, and a picker that needs a second tap to agree with the first is
 * asking the traveller to say it twice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSheet(
    title: String,
    selected: TranslateLanguage?,
    unavailable: TranslateLanguage?,
    autoAllowed: Boolean,
    onSelect: (TranslateLanguage?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Slid away rather than switched off: leaving the composition unhides whatever
    // is behind the sheet in the same frame, which reads as the screen flickering.
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = ScreenGutter, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = close) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.action_close),
                )
            }
        }

        Column(
            // Scrollable because the list outgrows a half-height sheet on a short
            // phone, and a language that cannot be reached cannot be chosen.
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        ) {
            if (autoAllowed) {
                LanguageSheetRow(
                    flag = null,
                    name = stringResource(Res.string.camera_translate_auto_detect),
                    gloss = null,
                    selected = selected == null,
                    enabled = true,
                    onClick = {
                        onSelect(null)
                        close()
                    },
                )
            }
            TranslateLanguage.entries.forEach { language ->
                val localised = stringResource(language.nameRes())
                LanguageSheetRow(
                    flag = language.flag,
                    name = language.displayName,
                    // Only where the two differ: "Tiếng Việt (Tiếng Việt)" tells
                    // a Vietnamese reader nothing they cannot already see.
                    gloss = localised.takeIf { it != language.displayName },
                    selected = language == selected,
                    // Greyed in place rather than dropped from the list: a list
                    // whose length changes with the other picker is one the
                    // traveller has to re-read every time they open it.
                    enabled = language != unavailable,
                    onClick = {
                        onSelect(language)
                        close()
                    },
                )
            }
        }
    }
}

@Composable
private fun LanguageSheetRow(
    flag: String?,
    name: String,
    gloss: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            // Faded whole rather than per-element: the flag is a colour emoji and
            // ignores content colour, so dimming the text alone left a bright flag
            // beside a greyed-out name.
            .alpha(if (enabled) 1f else 0.38f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            )
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            if (flag == null) {
                Icon(imageVector = Icons.Filled.Language, contentDescription = null)
            } else {
                Text(text = flag, style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
        if (gloss == null) {
            Spacer(Modifier.weight(1f))
        } else {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "($gloss)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Takes the whole remainder so the tick stays pinned to the edge
                // whether or not the row carries a gloss.
                modifier = Modifier.weight(1f),
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Drives the zoom so a tap glides and a gesture does not.
 *
 * Jumping straight to 2× reads as a glitch rather than a zoom; ramping over a
 * quarter of a second is the whole difference. A finger already on the glass is a
 * different matter — it owns the ratio outright, so any glide still running is
 * abandoned rather than fought.
 */
@Composable
private fun rememberZoomDriver(controller: CameraController): ZoomDriver {
    val scope = rememberCoroutineScope()
    return remember(controller, scope) { ZoomDriver(controller, scope) }
}

private class ZoomDriver(
    private val controller: CameraController,
    private val scope: CoroutineScope,
) {
    private var glide: Job? = null

    fun glideTo(ratio: Float) {
        glide?.cancel()
        glide = scope.launch {
            animate(
                initialValue = controller.requestedZoom(),
                targetValue = ratio,
                animationSpec = tween(ZOOM_GLIDE_MILLIS, easing = FastOutSlowInEasing),
            ) { value, _ -> controller.setZoomRatio(value) }
        }
    }

    fun pinch(scale: Float) {
        glide?.cancel()
        controller.zoomBy(scale)
    }

    fun dial(progressDelta: Float) {
        glide?.cancel()
        controller.zoomByProgress(progressDelta)
    }
}

/**
 * A zoom dial, borrowed from the pro modes: an arc of ticks that turns under a
 * fixed pointer.
 *
 * It is the whole zoom UI — a row of preset chips beside it would have said the
 * same thing twice — so it has to answer all three questions at once: where the
 * zoom is (the number over the pointer), where it can go (the labelled ticks), and
 * how to get there (turn it, or tap a tick to glide).
 */
@Composable
private fun ZoomDial(
    capabilities: CameraCapabilities,
    onZoomProgress: (Float) -> Unit,
    onZoomGlideTo: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    // Shadowed instead of panelled: with the dial down to a couple of hundred dp, a
    // backdrop behind it became a floating grey slab. Each mark carries its own
    // legibility over a bright wall instead.
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = Color.White,
        shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = 8f),
    )
    val stops = remember(capabilities.minZoomRatio, capabilities.maxZoomRatio) {
        capabilities.zoomStops()
    }
    val progress = capabilities.zoomProgress()

    // Haptics per notch, so the dial can be turned by feel while the eye stays on
    // the subject — the reason a physical dial has detents at all.
    var lastNotch by remember { mutableIntStateOf(-1) }
    LaunchedEffect(progress) {
        val notch = (progress * DIAL_NOTCHES).roundToInt()
        if (lastNotch >= 0 && notch != lastNotch) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastNotch = notch
    }

    Box(
        modifier = modifier
            // Narrow rather than full-bleed: a rail running the whole width read as
            // chrome bolted to the screen. A drag that starts here keeps being
            // delivered past the edges, so the shorter dial costs no reach.
            .width(DIAL_WIDTH)
            .height(DIAL_HEIGHT)
            .pointerInput(capabilities.minZoomRatio, capabilities.maxZoomRatio) {
                val radius = size.width * DIAL_RADIUS_FACTOR
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    // Dragging the dial left brings the higher numbers round to the
                    // pointer, exactly as turning a physical ring would.
                    onZoomProgress(-dragAmount / (radius * DIAL_SWEEP_RADIANS))
                }
            }
            .pointerInput(capabilities.minZoomRatio, capabilities.maxZoomRatio, progress) {
                val radius = size.width * DIAL_RADIUS_FACTOR
                val centre = Offset(size.width / 2f, DIAL_ARC_TOP.toPx() + radius)
                detectTapGestures { tap ->
                    // Tapping a tick is the gesture the preset chips used to serve:
                    // land on it by gliding, so the frame travels rather than jumps.
                    val angle = atan2(tap.x - centre.x, centre.y - tap.y)
                    val target = (progress + angle / DIAL_SWEEP_RADIANS).coerceIn(0f, 1f)
                    onZoomGlideTo(capabilities.zoomRatioAt(target))
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.width * DIAL_RADIUS_FACTOR
            val centre = Offset(size.width / 2f, DIAL_ARC_TOP.toPx() + radius)

            fun directionAt(value: Float): Offset? {
                val angle = (value - progress) * DIAL_SWEEP_RADIANS
                if (abs(angle) > DIAL_VISIBLE_RADIANS) return null
                return Offset(sin(angle), -cos(angle))
            }

            fun fadeAt(value: Float): Float {
                val angle = (value - progress) * DIAL_SWEEP_RADIANS
                return (1f - (abs(angle) / DIAL_VISIBLE_RADIANS).pow(2)).coerceIn(0f, 1f)
            }

            /** Draws a mark twice: a dark, wider stroke first, so it reads over anything. */
            fun drawMark(direction: Offset, length: Dp, width: Dp, alpha: Float) {
                val start = centre + direction * radius
                val end = centre + direction * (radius - length.toPx())
                drawLine(
                    color = Color.Black.copy(alpha = 0.4f * alpha),
                    start = start,
                    end = end,
                    strokeWidth = width.toPx() + 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = start,
                    end = end,
                    strokeWidth = width.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // The arc itself, drawn whether or not there are ticks over it. At 1× the
            // range runs out to the left of the pointer, and without a rail under it
            // the empty half reads as a UI that failed to load rather than a dial
            // wound to its stop.
            drawArc(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = 0.4f),
                    1f to Color.Transparent,
                ),
                startAngle = -90f - DIAL_VISIBLE_DEGREES,
                sweepAngle = DIAL_VISIBLE_DEGREES * 2f,
                useCenter = false,
                topLeft = Offset(centre.x - radius, centre.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 1.dp.toPx()),
            )

            for (notch in 0..DIAL_NOTCHES) {
                val value = notch / DIAL_NOTCHES.toFloat()
                val direction = directionAt(value) ?: continue
                drawMark(direction, length = 9.dp, width = 1.5.dp, alpha = 0.8f * fadeAt(value))
            }

            stops.forEach { stop ->
                val value = capabilities.zoomProgressOf(stop)
                val direction = directionAt(value) ?: return@forEach
                val fade = fadeAt(value)
                drawMark(direction, length = 15.dp, width = 2.dp, alpha = 0.95f * fade)
                // The stop under the pointer is left unlabelled: the readout above it
                // already says the same number, live.
                if (abs((value - progress) * DIAL_SWEEP_RADIANS) < DIAL_LABEL_GAP_RADIANS) {
                    return@forEach
                }
                val layout = measurer.measure(AnnotatedString(stop.formatZoom()), labelStyle)
                // Just under its own tick rather than a third of the way down the
                // face: hung lower, the labels needed a band of canvas below the
                // ticks that was empty the rest of the time, and it held the whole
                // dial off the bottom of the frame.
                val anchor = centre + direction * (radius - 24.dp.toPx())
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(
                        x = anchor.x - layout.size.width / 2f,
                        y = anchor.y - layout.size.height / 2f,
                    ),
                    alpha = fade,
                )
            }

            val up = Offset(0f, -1f)
            drawLine(
                color = Color.Black.copy(alpha = 0.4f),
                start = centre + up * (radius + 5.dp.toPx()),
                end = centre + up * (radius - 18.dp.toPx()),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Marigold,
                start = centre + up * (radius + 5.dp.toPx()),
                end = centre + up * (radius - 18.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // The live ratio, over the pointer. Without the chips this is the only place
        // the traveller can read what the zoom is actually doing.
        Text(
            text = capabilities.zoomRatio.formatZoom(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = InkBrown,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(percent = 50))
                .background(Marigold)
                .padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

/**
 * Whether the framing tip has already had its turn this run.
 *
 * Process-scoped rather than held in the ViewModel: the tip answers "what do I
 * point this at", which is worth one appearance when the app opens and is noise
 * every time the traveller comes back from the journal — and a back-stack-scoped
 * ViewModel is recreated on exactly those returns.
 */
private object CaptureHintSession {
    var shown = false
}

/**
 * The framing tip, shown once at the head of the frame and then gone.
 *
 * It used to sit permanently under the shutter, where a sentence that is only ever
 * read once cost a band of the screen for the whole session. As a bubble it lands
 * where the eye already is — on the picture — says its piece, and gives the frame
 * back. Above the subject rather than across it, so the corners keep saying where to
 * aim while it is read. Marked as shown the moment it starts so a recomposition
 * mid-fade cannot hand the traveller a second one.
 */
@Composable
private fun CaptureHintBubble(mode: LensMode, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (CaptureHintSession.shown) return@LaunchedEffect
        CaptureHintSession.shown = true
        visible = true
        delay(CAPTURE_HINT_VISIBLE_MILLIS)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 220)),
        exit = fadeOut(tween(durationMillis = 320)),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(mode.hintRes()),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = InkBrown,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = ScreenGutter)
                .clip(RoundedCornerShape(percent = 50))
                .background(Marigold)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

/**
 * The camera roll, as a film-canister slot.
 *
 * A bare icon: it sits opposite a pile of photographs that also opens something, and
 * the two are told apart by their shapes — this one goes out to the phone's own
 * library, the other into the journal.
 */
@Composable
private fun GalleryButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(GALLERY_SLOT_SIZE)
            .clip(shape)
            .background(PaperCream.copy(alpha = 0.10f))
            .border(width = 1.dp, color = Marigold.copy(alpha = 0.45f), shape = shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = stringResource(Res.string.camera_pick_gallery),
            tint = PaperCream,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * The shutter, carrying the face of a Đông Sơn drum: a sunburst around a red centre.
 *
 * This is the button the traveller looks at more than any other, and a plain white
 * circle said nothing about where they were standing — the bronze drums are two and
 * a half thousand years of Vietnamese metalwork, and their face is already a target
 * with a bullseye, which is exactly what a shutter should look like. Drawn rather
 * than shipped as an asset so the rays stay crisp at any density.
 */
@Composable
private fun ShutterButton(enabled: Boolean, counting: Boolean, onClick: () -> Unit) {
    val label = stringResource(
        if (counting) Res.string.camera_cancel_timer else Res.string.camera_capture,
    )
    Box(
        modifier = Modifier
            .size(SHUTTER_SIZE)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                onClickLabel = label,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            val outer = size.minDimension / 2f

            // Rim, then a dark line, then the face: the gap is what stops the drum
            // from bleeding into the backdrop it sits on.
            drawCircle(color = Marigold, radius = outer, center = centre)
            drawCircle(color = InkBrown, radius = outer - 3.dp.toPx(), center = centre)
            val face = outer - 5.dp.toPx()
            drawCircle(color = PaperCream, radius = face, center = centre)

            // The rays, from the inner ring out to the rim. Kept thin and half
            // transparent: the drum is a decoration on a control, not a picture, and
            // a solid sunburst would fight the red centre for the eye.
            val ray = Marigold.copy(alpha = 0.55f)
            val inner = face * 0.44f
            for (index in 0 until DRUM_RAYS) {
                val angle = index * 2f * PI.toFloat() / DRUM_RAYS
                val direction = Offset(sin(angle), -cos(angle))
                drawLine(
                    color = ray,
                    start = centre + direction * inner,
                    end = centre + direction * (face - 2.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(
                color = ray,
                radius = inner,
                center = centre,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = ray,
                radius = face - 1.dp.toPx(),
                center = centre,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        if (counting) {
            // A stop square, the one shape nobody has to be taught during a countdown.
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(InkBrown),
            )
        } else {
            Box(
                modifier = Modifier.size(DRUM_CENTRE).clip(CircleShape).background(Vermilion),
            )
        }
    }
}

/**
 * Recent captures as a shuffled pile rather than a scrolling strip.
 *
 * The strip claimed a full row of the viewfinder to show history the traveller
 * mostly ignores while shooting. A pile says the same thing — "your last few are
 * saved" — in the width of one thumbnail, and mirrors the gallery button so the
 * shutter still reads as the centre of the row. Tapping it hands over to the
 * journal, which is the screen built for looking back.
 */
@Composable
private fun RecentCaptureStack(
    discoveries: List<Discovery>,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (discoveries.isEmpty()) return

    val label = stringResource(Res.string.camera_open_journal)
    // Laid down oldest first so the newest capture ends up on top of the pile.
    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
        discoveries.asReversed().forEachIndexed { index, discovery ->
            RecentCaptureCard(
                discovery = discovery,
                depth = discoveries.lastIndex - index,
                clickLabel = label,
                enabled = enabled,
                onClick = onClick,
            )
        }
    }
}

@Composable
private fun RecentCaptureCard(
    discovery: Discovery,
    depth: Int,
    clickLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    AppAsyncImage(
        model = discovery.imagePath,
        contentDescription = discovery.title,
        shape = shape,
        border = BorderStroke(
            width = 1.5.dp,
            color = Marigold.copy(alpha = 0.9f - depth * 0.15f),
        ),
        // Lacquer red under the photograph rather than black: the pile is what a
        // stack of passport pages looks like in this app, and an image still
        // loading should look like part of the design instead of a hole in it.
        placeholderColor = Vermilion,
        onClick = onClick,
        onClickLabel = clickLabel,
        clickEnabled = enabled,
        modifier = Modifier
            // Deeper cards slide left and tilt a little, so the pile fans out just
            // enough to read as several photos instead of a smudged edge.
            .offset(x = -(FAN_STEP * depth))
            .rotate(-FAN_TILT_DEGREES * depth)
            .size(46.dp),
    )
}

/** The self-timer, counted down over the live frame so the pose can be adjusted. */
@Composable
private fun CountdownOverlay(seconds: Int, modifier: Modifier = Modifier) {
    val pulse = remember { Animatable(COUNTDOWN_PULSE) }
    LaunchedEffect(seconds) {
        pulse.snapTo(COUNTDOWN_PULSE)
        pulse.animateTo(1f, tween(durationMillis = 400))
    }

    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = seconds.toString(),
            // Shadowed rather than scrimmed: the countdown is read against whatever
            // is being framed, and dimming the whole frame to make one digit legible
            // would hide the thing the traveller is still composing.
            style = MaterialTheme.typography.displayLarge.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset.Zero,
                    blurRadius = 24f,
                ),
            ),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.scale(pulse.value),
        )
    }
}

/**
 * The recognition wait, over the frozen viewfinder.
 *
 * Recognition only — translate leaves this screen the moment the shutter closes,
 * so there is no translate copy here to choose between any more.
 */
@Composable
private fun AnalysingOverlay(stage: Int) {
    // Dropped `analysing_nearby` along with the schema field it described: the
    // ticker reaches the last line inside the wait it is covering, so a line
    // promising work the request no longer asks for would be read as the reason
    // the traveller is still waiting.
    val stages = listOf(
        Res.string.analysing_recognising,
        Res.string.analysing_history,
        Res.string.analysing_stories,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            // A scrim that reads as "hands off" has to behave like one. A Box with
            // only a background takes part in no pointer input at all, so taps carry
            // straight through it to the lens switch and the recent stack beneath.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.analysing_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(stages[stage.coerceIn(0, stages.lastIndex)]),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = ScreenGutter),
            )
        }
    }
}

/**
 * The one place the app asks for location.
 *
 * It leads with what the traveller gets — a province filling in on their passport —
 * rather than with what the app wants, because the system dialog that follows says
 * "allow access to this device's location" and nothing about why.
 *
 * The sheet answers to its own buttons and nothing else: a tap outside, a swipe down
 * and the back gesture are all refused. A question that can be brushed off by
 * accident is one the traveller never really answered, and the flag that stops it
 * from coming back is only written by "not now". Leaving is still one tap away —
 * location is a passport nicety, not the price of using the camera.
 */
@Composable
private fun LocationPermissionSheet(
    deniedForGood: Boolean,
    onAllow: () -> Unit,
    onLater: () -> Unit,
) {
    PermissionSheet(
        title = stringResource(Res.string.location_permission_title),
        body = stringResource(
            if (deniedForGood) {
                Res.string.location_permission_blocked_body
            } else {
                Res.string.location_permission_body
            },
        ),
        confirmLabel = stringResource(
            if (deniedForGood) {
                Res.string.permission_open_settings
            } else {
                Res.string.location_permission_grant
            },
        ),
        dismissLabel = stringResource(Res.string.location_permission_later),
        onConfirm = onAllow,
        onDismiss = onLater,
    )
}

@Composable
private fun CameraPermissionPrompt(
    deniedForGood: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Outlined.PhotoLibrary,
        title = stringResource(Res.string.camera_permission_title),
        description = stringResource(
            if (deniedForGood) Res.string.camera_permission_blocked_body else Res.string.camera_permission_body,
        ),
        modifier = modifier,
        action = {
            Button(onClick = onRequest) {
                Text(
                    stringResource(
                        if (deniedForGood) {
                            Res.string.permission_open_settings
                        } else {
                            Res.string.camera_permission_grant
                        },
                    ),
                )
            }
        },
    )
}

private fun LensMode.labelRes(): StringResource = when (this) {
    LensMode.AUTO -> Res.string.mode_auto
    LensMode.LANDMARK -> Res.string.mode_landmark
    LensMode.FOOD -> Res.string.mode_food
    LensMode.ARTIFACT -> Res.string.mode_artifact
    LensMode.TRANSLATE -> Res.string.mode_translate
}

private fun LensMode.hintRes(): StringResource = when (this) {
    LensMode.AUTO -> Res.string.camera_hint_auto
    LensMode.LANDMARK -> Res.string.camera_hint_landmark
    LensMode.FOOD -> Res.string.camera_hint_food
    LensMode.ARTIFACT -> Res.string.camera_hint_artifact
    LensMode.TRANSLATE -> Res.string.camera_hint_translate
}

/** The language's name in the app's language, used as a gloss beside its own. */
private fun TranslateLanguage.nameRes(): StringResource = when (this) {
    TranslateLanguage.VIETNAMESE -> Res.string.language_vi
    TranslateLanguage.ENGLISH -> Res.string.language_en
    TranslateLanguage.JAPANESE -> Res.string.language_ja
    TranslateLanguage.KOREAN -> Res.string.language_ko
    TranslateLanguage.CHINESE -> Res.string.language_zh
    TranslateLanguage.FRENCH -> Res.string.language_fr
    TranslateLanguage.SPANISH -> Res.string.language_es
    TranslateLanguage.THAI -> Res.string.language_th
}

private fun CaptureTimer.labelRes(): StringResource = when (this) {
    CaptureTimer.OFF -> Res.string.camera_timer_off
    CaptureTimer.THREE -> Res.string.camera_timer_3
    CaptureTimer.TEN -> Res.string.camera_timer_10
}

/**
 * "1×", "2×", "2.4×" — a decimal only when there is one.
 *
 * Forced to a fixed locale: zoom is read as a number on a dial, and a comma
 * decimal in the middle of the row reads as a second value rather than a fraction.
 */
/** "+1.0" / "-0.7": exposure is read as a signed number of stops, never as an index. */
private fun Float.formatEv(): String {
    val sign = if (this > 0f) "+" else if (this < 0f) "-" else ""
    return sign + abs(this).toOneDecimal()
}

private fun Float.formatZoom(): String {
    val rounded = (this * 10f).roundToInt() / 10f
    return if (abs(rounded - rounded.roundToInt()) < 0.05f) {
        "${rounded.roundToInt()}x"
    } else {
        "${rounded.toOneDecimal()}x"
    }
}

/**
 * One decimal place, always with a dot.
 *
 * `String.format` is JVM-only, and the fixed locale it was pinned to mattered: zoom and
 * exposure are read as numbers on a dial, and a comma decimal in the middle of the row reads
 * as a second value rather than as a fraction. Building the string by hand keeps that
 * guarantee on both platforms instead of borrowing it from a locale.
 */
private fun Float.toOneDecimal(): String {
    val tenths = (abs(this) * 10f).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

/** How far each card behind the top one slides out of the pile. */
private val FAN_STEP = 9.dp

/** How far each card behind the top one tilts, in degrees. */
private const val FAN_TILT_DEGREES = 4f

/** Clears the mode chips and the shutter row, so messages sit above them. */
private val MESSAGES_BOTTOM_INSET = 160.dp

/**
 * The gutter either side of the language bar, inside the frame.
 *
 * Wide enough that the bar reads as floating on the picture rather than bolted to
 * the frame: at a dozen dp it ran almost edge to edge while the dial above it sat
 * a hundred dp in from either side, and the two stopped looking like one stack.
 */
private val TRANSLATE_BAR_MARGIN = 24.dp

/** How long the framing tip stays up before fading out, once per app run. */
private const val CAPTURE_HINT_VISIBLE_MILLIS = 1_500L

private val TOOL_BUTTON_SIZE = 44.dp
private val GALLERY_SLOT_SIZE = 52.dp

/** The drum: its overall size, its red centre, and how many rays the sunburst has. */
private val SHUTTER_SIZE = 78.dp
private val DRUM_CENTRE = 22.dp
private const val DRUM_RAYS = 16

private val FOCUS_RING_SIZE = 72.dp
private const val FOCUS_RING_OVERSHOOT = 1.4f

/** Long enough to dial in exposure, short enough not to sit over the shot. */
private const val FOCUS_RETICLE_HOLD_MILLIS = 2_600L

private val EV_TRACK_HEIGHT = 168.dp
private val EV_TRACK_WIDTH = 28.dp

private const val COUNTDOWN_PULSE = 1.35f

/** How long a tap on a tick takes to travel there. */
private const val ZOOM_GLIDE_MILLIS = 260

private val DIAL_WIDTH = 164.dp

/**
 * Only as tall as the arc actually reaches.
 *
 * The canvas below the ticks is empty — the circle carries on downwards off the
 * screen — so every dp of it was pure gap between the dial and the frame's edge.
 */
private val DIAL_HEIGHT = 68.dp

/** Rounded hard enough to read as a card of its own against the backdrop. */
private val FRAME_SHAPE = RoundedCornerShape(24.dp)

/**
 * The dial's geometry: a circle far below the screen, of which only the top arc shows.
 *
 * A wide radius is what makes it read as a dial rather than a bent slider — the ticks
 * stay nearly upright, and the curve is felt more than seen.
 */
private const val DIAL_RADIUS_FACTOR = 1.35f

/**
 * Radians the whole zoom range spans, and how much of it is on screen at once.
 *
 * Measured on the device rather than guessed. The narrower dial turns the same
 * angle over a third of the pixels, so the sweep widens to keep the feel: about
 * half the range crosses the face in one drag, which still puts two labelled stops
 * in view — the thing that tells the traveller where to go next now that the chips
 * are gone.
 */
private const val DIAL_SWEEP_RADIANS = 1.4f
private const val DIAL_VISIBLE_RADIANS = 0.35f
private const val DIAL_VISIBLE_DEGREES = 20f

/** How close to the pointer a tick label has to be before the readout speaks for it. */
private const val DIAL_LABEL_GAP_RADIANS = 0.1f
private const val DIAL_NOTCHES = 40

/** Leaves room above the arc for the live-ratio readout that sits over the pointer. */
private val DIAL_ARC_TOP = 32.dp

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
