package com.duylt.trave.vietlensai.mobile.feature.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.core.designsystem.component.AppSnackbarHost
import com.duylt.trave.vietlensai.core.designsystem.theme.InkBrown
import com.duylt.trave.vietlensai.core.designsystem.theme.InkDarkest
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.core.util.rememberCameraPermissionState
import com.duylt.trave.vietlensai.core.util.rememberLocationPermissionState
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.LensMode
import com.duylt.trave.vietlensai.feature.camera.CameraController
import com.duylt.trave.vietlensai.feature.camera.LensHost
import com.duylt.trave.vietlensai.feature.camera.LensIntent
import com.duylt.trave.vietlensai.feature.camera.LensState
import com.duylt.trave.vietlensai.feature.camera.LensViewModel
import com.duylt.trave.vietlensai.feature.camera.component.AnalysingOverlay
import com.duylt.trave.vietlensai.feature.camera.component.CameraPermissionPrompt
import com.duylt.trave.vietlensai.feature.camera.component.CameraToolRow
import com.duylt.trave.vietlensai.feature.camera.component.CaptureErrorSnackbar
import com.duylt.trave.vietlensai.feature.camera.component.CaptureHintBubble
import com.duylt.trave.vietlensai.feature.camera.component.CountdownOverlay
import com.duylt.trave.vietlensai.feature.camera.component.GalleryButton
import com.duylt.trave.vietlensai.feature.camera.component.LocationPermissionSheet
import com.duylt.trave.vietlensai.feature.camera.component.MissingKeySnackbar
import com.duylt.trave.vietlensai.feature.camera.component.ModeChipRow
import com.duylt.trave.vietlensai.feature.camera.component.RecentCaptureCard
import com.duylt.trave.vietlensai.feature.camera.component.ShutterButton
import com.duylt.trave.vietlensai.feature.camera.component.TranslateLanguageBar
import com.duylt.trave.vietlensai.feature.camera.component.Viewfinder
import com.duylt.trave.vietlensai.feature.camera.component.ZoomDial
import com.duylt.trave.vietlensai.feature.camera.component.rememberZoomDriver
import com.duylt.trave.vietlensai.platform.rememberPhotoPicker
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.camera_open_journal
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The app's home on a phone: a live viewfinder with one capture button.
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
 *
 * The large-window arrangement of the same parts is
 * `tablet/feature/camera/LensTabletScreen.kt`; everything both of them draw comes from
 * `feature/camera/component/`, and everything both of them *do* comes from [LensHost].
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
    LensHost(
        viewModel = viewModel,
        onDiscoveryCaptured = onDiscoveryCaptured,
        onTranslationCaptured = onTranslationCaptured,
    ) { state, controller, onIntent ->
        LensScreen(
            state = state,
            controller = controller,
            onIntent = onIntent,
            onOpenJournal = onOpenJournal,
            onOpenSettings = onOpenSettings,
            modifier = modifier,
        )
    }
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
                    .padding(horizontal = ScreenGutter, vertical = Spacing.sm),
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
                    .clip(MaterialTheme.shapes.large)
                    .background(InkBrown)
                    .border(
                        width = 1.dp,
                        color = Marigold.copy(alpha = 0.22f),
                        shape = MaterialTheme.shapes.large,
                    ),
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
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = Spacing.md),
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
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.sm),
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
                            modifier = Modifier.padding(bottom = Spacing.sm),
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
                modifier = Modifier.padding(vertical = Spacing.md),
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
            .padding(start = ScreenGutter, end = ScreenGutter, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            GalleryButton(enabled = !state.isBusy, onClick = pickPhoto)
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
 * Recent captures as a shuffled pile rather than a scrolling strip.
 *
 * The strip claimed a full row of the viewfinder to show history the traveller
 * mostly ignores while shooting. A pile says the same thing — "your last few are
 * saved" — in the width of one thumbnail, and mirrors the gallery button so the
 * shutter still reads as the centre of the row. Tapping it hands over to the
 * journal, which is the screen built for looking back.
 *
 * The pile is the phone's answer and stays here rather than in `feature/camera/component/`.
 * The tablet has a column of vertical space to spend and lists the same captures with their
 * names; the argument for a pile — that a strip costs a full row of a viewfinder that has no
 * rows to spare — is an argument about a phone, and it does not carry across.
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

/** Clears the mode chips and the shutter row, so messages sit above them. */
private val MESSAGES_BOTTOM_INSET = 160.dp
