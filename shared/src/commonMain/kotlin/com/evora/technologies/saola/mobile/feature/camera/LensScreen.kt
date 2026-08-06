package com.evora.technologies.saola.mobile.feature.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.saola.core.designsystem.component.AppSnackbarHost
import com.evora.technologies.saola.core.designsystem.theme.InkBrown
import com.evora.technologies.saola.core.designsystem.theme.InkDarkest
import com.evora.technologies.saola.core.designsystem.theme.Marigold
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.core.util.LocationPermissionState
import com.evora.technologies.saola.core.util.rememberCameraPermissionState
import com.evora.technologies.saola.core.util.rememberLocationPermissionState
import com.evora.technologies.saola.core.window.rememberCanStackVertically
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.LensMode
import com.evora.technologies.saola.feature.camera.CameraCapabilities
import com.evora.technologies.saola.feature.camera.CameraController
import com.evora.technologies.saola.feature.camera.LensHost
import com.evora.technologies.saola.feature.camera.LensIntent
import com.evora.technologies.saola.feature.camera.LensState
import com.evora.technologies.saola.feature.camera.LensViewModel
import com.evora.technologies.saola.feature.camera.component.AnalysingOverlay
import com.evora.technologies.saola.feature.camera.component.CameraPermissionPrompt
import com.evora.technologies.saola.feature.camera.component.CameraToolRow
import com.evora.technologies.saola.feature.camera.component.CaptureErrorSnackbar
import com.evora.technologies.saola.feature.camera.component.CaptureHintBubble
import com.evora.technologies.saola.feature.camera.component.CountdownOverlay
import com.evora.technologies.saola.feature.camera.component.GalleryButton
import com.evora.technologies.saola.feature.camera.component.LocationPermissionSheet
import com.evora.technologies.saola.feature.camera.component.MissingKeySnackbar
import com.evora.technologies.saola.feature.camera.component.ModeChipRow
import com.evora.technologies.saola.feature.camera.component.RecentCaptureCard
import com.evora.technologies.saola.feature.camera.component.ShutterButton
import com.evora.technologies.saola.feature.camera.component.TranslateLanguageBar
import com.evora.technologies.saola.feature.camera.component.Viewfinder
import com.evora.technologies.saola.feature.camera.component.ZoomDial
import com.evora.technologies.saola.feature.camera.component.ZoomDriver
import com.evora.technologies.saola.feature.camera.component.rememberZoomDriver
import com.evora.technologies.saola.platform.rememberPhotoPicker
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.camera_open_journal
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The app's home on a phone: a live viewfinder with one capture button.
 *
 * Everything else on this screen is deliberately subordinate to the shutter — the
 * whole product promise is "point and ask", so nothing may sit between opening the
 * app and taking a photo.
 *
 * Held upright the screen is a column: camera tools above the frame, the frame itself as a
 * card, then the mode chips and the shutter row under it. Two things come out of that. The
 * picture is never covered by a control, and everything the traveller taps sits in the bottom
 * third, where a thumb already is — the mode picker most of all, which used to be a dropdown
 * in the far top corner.
 *
 * **Turned sideways the column stops working, and it is the only phone screen that does.**
 * Measured on a Galaxy A16 at 832 × 384dp: the 214dp of chrome the column needs leaves the
 * frame a 55dp letterbox strip with the zoom dial floating in it. So below
 * [rememberCanStackVertically] the same parts arrange as a row — [SideBySideLens] — and the
 * two arrangements share every piece, including the frame itself. Nine other phone screens
 * were checked at the same size on the same day and none of them needed a second
 * arrangement; the note on `StackableMinHeight` says why each one survives.
 *
 * The large-window arrangement of the same parts is
 * `tablet/feature/camera/LensTabletScreen.kt`; everything all three of them draw comes from
 * `feature/camera/component/`, and everything they *do* comes from [LensHost].
 */
@Composable
fun LensRoute(
    onDiscoveryCaptured: (String) -> Unit,
    /** Photo path, source language code (blank for detect), target language code. */
    onTranslationCaptured: (String, String, String) -> Unit,
    onOpenJournal: () -> Unit,
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
    modifier: Modifier = Modifier,
) {
    // Both grants, and what asking again would do, behind one platform-neutral state.
    // `requestOrOpenSettings` is the whole decision the prompts need: ask while the OS still
    // asks, and hand over to the app's own settings page once it has stopped.
    val cameraPermission = rememberCameraPermissionState()
    val locationPermission = rememberLocationPermissionState()

    val capabilities by controller.capabilities.collectAsStateWithLifecycle()
    val zoom = rememberZoomDriver(controller)

    // Hands back a path already copied into app storage, so a picked photo travels the
    // same pipeline as a captured one. One at a time: recognition is about the single
    // thing the traveller is standing in front of. Held here rather than inside either
    // arrangement so rotating does not tear down a picker that is already open.
    val pickPhoto = rememberPhotoPicker(maxItems = 1) { paths ->
        paths.firstOrNull()?.let { onIntent(LensIntent.PhotoPicked(it)) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    MissingKeySnackbar(
        hasApiKey = state.hasApiKey,
        hostState = snackbarHostState,
    )
    CaptureErrorSnackbar(
        error = state.error,
        hostState = snackbarHostState,
        onDismiss = { onIntent(LensIntent.DismissError) },
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(InkDarkest)) {
        val stacked = rememberCanStackVertically(maxHeight)

        if (stacked) {
            StackedLens(
                state = state,
                controller = controller,
                capabilities = capabilities,
                onIntent = onIntent,
                onPickPhoto = pickPhoto,
                onOpenJournal = onOpenJournal,
                onPinch = zoom::pinch,
                onZoomProgress = zoom::dial,
                onZoomGlideTo = zoom::glideTo,
                cameraPermission = cameraPermission,
            )
        } else {
            SideBySideLens(
                state = state,
                controller = controller,
                capabilities = capabilities,
                onIntent = onIntent,
                onPickPhoto = pickPhoto,
                onOpenJournal = onOpenJournal,
                onPinch = zoom::pinch,
                onZoomProgress = zoom::dial,
                onZoomGlideTo = zoom::glideTo,
                cameraPermission = cameraPermission,
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
                .padding(
                    // Stacked, there are two rows of controls under the frame to clear.
                    // Side by side there are none — they are off to the trailing edge — so
                    // a notice lifted by 160 would hang in the middle of the picture.
                    bottom = if (stacked) STACKED_MESSAGES_INSET else Spacing.lg,
                    start = ScreenGutter,
                    end = ScreenGutter,
                ),
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
 * The upright arrangement: tools, frame, chips, shutter, straight down the page.
 *
 * This is the phone's own order and the one the product was designed around — the shutter is
 * the last thing on the page because that is where the thumb of the hand holding it rests.
 */
@Composable
private fun StackedLens(
    state: LensState,
    controller: CameraController,
    capabilities: CameraCapabilities,
    onIntent: (LensIntent) -> Unit,
    onPickPhoto: () -> Unit,
    onOpenJournal: () -> Unit,
    onPinch: (Float) -> Unit,
    onZoomProgress: (Float) -> Unit,
    onZoomGlideTo: (Float) -> Unit,
    cameraPermission: LocationPermissionState,
) {
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

        CameraFrame(
            state = state,
            controller = controller,
            capabilities = capabilities,
            onIntent = onIntent,
            onPinch = onPinch,
            onZoomProgress = onZoomProgress,
            onZoomGlideTo = onZoomGlideTo,
            cameraPermission = cameraPermission,
            // The frame takes the same gutter as every other row and all the
            // height the tools and the shutter leave. It is deliberately not
            // pinned to the sensor's 4:3: letterboxed to that ratio it came
            // out narrower than the chips under it, and a viewfinder that
            // does not line up with its own controls reads as a bug. What
            // the shape costs — the preview no longer being the whole sensor
            // frame — is paid back by the view port in `CameraController`,
            // which crops the photo to exactly this rectangle.
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter),
        )

        ModeChipRow(
            modes = state.availableModes,
            selected = state.mode,
            onSelect = { onIntent(LensIntent.SelectMode(it)) },
            modifier = Modifier.padding(vertical = Spacing.md),
        )

        // Three things, and the middle one is the point of the screen — which is why the two
        // either side are given equal weight rather than sized to their contents. The
        // shutter stays on the screen's axis however wide the pile grows.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ScreenGutter, end = ScreenGutter, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                GalleryButton(enabled = !state.isBusy, onClick = onPickPhoto)
            }

            Shutter(state = state, onIntent = onIntent, cameraReady = cameraPermission.isGranted)

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                RecentCaptureStack(
                    discoveries = state.recentDiscoveries,
                    enabled = !state.isBusy,
                    onClick = onOpenJournal,
                )
            }
        }
    }
}

/**
 * The sideways arrangement: the picture takes the width, the shutter takes the trailing edge.
 *
 * Same conclusion the tablet reached and a different reason for it. The tablet moves the
 * shutter to a 310dp column because a tablet is held at both side edges and that is where the
 * right thumb is; a phone in landscape is held the same way, but the argument here is simply
 * that there is no height left — 384dp with 214 of chrome is a 55dp slot for the picture.
 *
 * The two rows that survive at the top are the ones that cost the least height and would be
 * unusable narrow: the chips are a `LazyRow` of five labels and the tools are four 44dp
 * buttons, and either of them in a 110dp column would scroll horizontally to show four items.
 * So they take the width, which landscape has to spare, and the trailing column carries only
 * the three controls that are genuinely a stack.
 *
 * **The column claims no width of its own.** It measures to its widest child — the 78dp
 * shutter — plus [ScreenGutter] either side, so there is no landscape twin of
 * `PaneWidth.lensPanel` to keep in step with anything, and a change to the shutter's size
 * moves the frame's edge with it.
 */
@Composable
private fun SideBySideLens(
    state: LensState,
    controller: CameraController,
    capabilities: CameraCapabilities,
    onIntent: (LensIntent) -> Unit,
    onPickPhoto: () -> Unit,
    onOpenJournal: () -> Unit,
    onPinch: (Float) -> Unit,
    onZoomProgress: (Float) -> Unit,
    onZoomGlideTo: (Float) -> Unit,
    cameraPermission: LocationPermissionState,
) {
    Column(modifier = Modifier.fillMaxSize().screenInsetsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModeChipRow(
                modes = state.availableModes,
                selected = state.mode,
                onSelect = { onIntent(LensIntent.SelectMode(it)) },
                modifier = Modifier.weight(1f),
            )
            CameraToolRow(
                state = state,
                capabilities = capabilities,
                onIntent = onIntent,
                modifier = Modifier.padding(start = Spacing.sm, end = ScreenGutter),
                // Gathered at the end rather than spread across the row: the chips have
                // taken the width that `SpaceEvenly` exists to fill, and the tools are the
                // tail of the line. Same call the tablet makes, for the same reason.
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            )
        }

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CameraFrame(
                state = state,
                controller = controller,
                capabilities = capabilities,
                onIntent = onIntent,
                onPinch = onPinch,
                onZoomProgress = onZoomProgress,
                onZoomGlideTo = onZoomGlideTo,
                cameraPermission = cameraPermission,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = ScreenGutter, bottom = ScreenGutter),
            )

            // The stacked row's equal-weights rule stood upright, and weights rather than
            // `SpaceBetween` for the same reason it uses them: the pile draws nothing at all
            // until there is a capture to show, and `SpaceBetween` with one child missing
            // pushes the shutter to whichever end is left. It has to be in the same place on
            // an empty install as on a full one.
            //
            // The pile is at the top and the roll at the bottom because that is where the
            // phone's own rotation puts them: turned a quarter clockwise, the upright row's
            // left edge becomes this column's foot, so both controls stay under the finger
            // that was already reaching for them.
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = ScreenGutter, vertical = Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                    RecentCaptureStack(
                        discoveries = state.recentDiscoveries,
                        enabled = !state.isBusy,
                        onClick = onOpenJournal,
                    )
                }

                Shutter(state = state, onIntent = onIntent, cameraReady = cameraPermission.isGranted)

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                    GalleryButton(enabled = !state.isBusy, onClick = onPickPhoto)
                }
            }
        }
    }
}

/**
 * The frame itself, and everything that floats on it.
 *
 * A composable of its own rather than a `Box` written out twice, and not only to share it:
 * nested directly in either arrangement's `Column`, the `AnimatedVisibility` below resolves to
 * `ColumnScope.AnimatedVisibility` — an overload that animates a column's height and cannot
 * take the `BoxScope` alignment it needs. Giving the frame its own composable takes the column
 * scope out of view, which is the honest fix; the alternative is an import alias the next
 * person has to work out from scratch.
 */
@Composable
private fun CameraFrame(
    state: LensState,
    controller: CameraController,
    capabilities: CameraCapabilities,
    onIntent: (LensIntent) -> Unit,
    onPinch: (Float) -> Unit,
    onZoomProgress: (Float) -> Unit,
    onZoomGlideTo: (Float) -> Unit,
    cameraPermission: LocationPermissionState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
                onPinch = onPinch,
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
                    onZoomProgress = onZoomProgress,
                    onZoomGlideTo = onZoomGlideTo,
                )
            }
        }
    }
}

/**
 * The shutter, with the one rule both arrangements have to state the same way.
 *
 * Only the shutter answers to `cameraReady`. The gallery button and the recent pile never
 * touch the camera — the roll goes out to the system picker and the pile opens the journal —
 * so a refused camera grant must not take them down with it. A traveller who said no, or who
 * has not been asked yet, can still hand the app a photo they already have.
 */
@Composable
private fun Shutter(
    state: LensState,
    onIntent: (LensIntent) -> Unit,
    /** Whether the camera permission is granted — the shutter's business alone. */
    cameraReady: Boolean,
) {
    ShutterButton(
        // Stays live through a countdown: while the number is on screen the
        // shutter is the only way to call the photo off. Not `!isBusy` for
        // exactly that reason.
        enabled = cameraReady && !state.isAnalysing && !state.isCapturing,
        counting = state.isCountingDown,
        onClick = { onIntent(LensIntent.ShutterPressed) },
    )
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
 * rows to spare — is an argument about a phone, and it is *more* true sideways than upright,
 * which is why both phone arrangements draw it.
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
                // Blocking pointers under the scrim does nothing for TalkBack,
                // which navigates by semantics rather than by touch.
                enabled = enabled,
                onClick = onClick,
            )
        }
    }
}

/** Clears the mode chips and the shutter row, so messages sit above them. */
private val STACKED_MESSAGES_INSET: Dp = 160.dp
