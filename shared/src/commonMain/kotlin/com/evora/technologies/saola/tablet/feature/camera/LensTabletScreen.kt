package com.evora.technologies.saola.tablet.feature.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.saola.core.designsystem.component.AppSnackbarHost
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.InkBrown
import com.evora.technologies.saola.core.designsystem.theme.InkDarkest
import com.evora.technologies.saola.core.designsystem.theme.Marigold
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.PaneWidth
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.core.util.rememberCameraPermissionState
import com.evora.technologies.saola.core.util.rememberLocationPermissionState
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
import com.evora.technologies.saola.feature.camera.component.ShutterButton
import com.evora.technologies.saola.feature.camera.component.TranslateLanguageBar
import com.evora.technologies.saola.feature.camera.component.Viewfinder
import com.evora.technologies.saola.feature.camera.component.ZoomDial
import com.evora.technologies.saola.feature.camera.component.hintRes
import com.evora.technologies.saola.feature.camera.component.labelRes
import com.evora.technologies.saola.feature.camera.component.rememberZoomDriver
import com.evora.technologies.saola.platform.rememberPhotoPicker
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.camera_ready_to_scan
import com.evora.technologies.saola.resources.camera_recent_scans
import com.evora.technologies.saola.tablet.navigation.TwoPaneScaffold
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The lens on a large window: the picture takes the room, the controls take a column.
 *
 * The phone stacks everything into one narrow column and the shutter ends up under the
 * frame, which is where a thumb is on a phone held one-handed. A tablet is held at its two
 * side edges with both hands, and the design note on the wireframe says why that changes the
 * answer: *"Camera giữ panel riêng bên phải để shutter nằm dưới ngón cái khi cầm tablet hai
 * tay."* So the shutter moves into a fixed [PaneWidth.lensPanel] column on the trailing edge,
 * under the right thumb, and the viewfinder takes everything else.
 *
 * Nothing here is drawn twice. Every control comes from `feature/camera/component/` and is
 * the same composable the phone places, and everything the screen *does* — the camera, the
 * effects, the volume keys — is [LensHost], shared. What this file owns is where the pieces
 * sit, and that is the whole of the difference between the two arrangements.
 *
 * One thing genuinely differs beyond arrangement, and it is argued in
 * [RecentScanList]: the phone's pile of thumbnails becomes a list with names on it, because
 * the reason for the pile was a shortage of vertical space this window does not have.
 */
@Composable
fun LensTabletRoute(
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
        LensTabletScreen(
            state = state,
            controller = controller,
            onIntent = onIntent,
            onOpenJournal = onOpenJournal,
            modifier = modifier,
        )
    }
}

@Composable
private fun LensTabletScreen(
    state: LensState,
    controller: CameraController,
    onIntent: (LensIntent) -> Unit,
    onOpenJournal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPermission = rememberCameraPermissionState()
    val locationPermission = rememberLocationPermissionState()

    val capabilities by controller.capabilities.collectAsStateWithLifecycle()
    val zoom = rememberZoomDriver(controller)

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

    Box(modifier = modifier.fillMaxSize()) {
        TwoPaneScaffold(
            fixedPaneWidth = PaneWidth.lensPanel,
            // The panel sits on the trailing edge, opposite the rail. Put at the start it
            // would stand against the navigation rail and the two columns of controls would
            // read as one 414 dp sidebar with the picture squeezed into what was left.
            fixedPaneAtStart = false,
            fixedPane = {
                LensControlPanel(
                    state = state,
                    onIntent = onIntent,
                    onOpenJournal = onOpenJournal,
                    cameraReady = cameraPermission.isGranted,
                )
            },
            flexiblePane = {
                ViewfinderPane(
                    state = state,
                    controller = controller,
                    capabilities = capabilities,
                    onIntent = onIntent,
                    onPinch = zoom::pinch,
                    onZoomProgress = zoom::dial,
                    onZoomGlideTo = zoom::glideTo,
                    cameraGranted = cameraPermission.isGranted,
                    cameraDeniedForGood = cameraPermission.isDeniedForGood,
                    onRequestCamera = cameraPermission::requestOrOpenSettings,
                    snackbarHostState = snackbarHostState,
                )
            },
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
                onAllow = locationPermission::requestOrOpenSettings,
                onLater = { onIntent(LensIntent.LocationAsked) },
            )
        }
    }
}

/**
 * The picture and the two controls that belong to it.
 *
 * The mode chips share the top row with the camera tools rather than sitting under the frame
 * as they do on a phone: there is a whole row's width spare up there, and the panel's job is
 * the shutter. The chips keep their own gutter and the tools gather at the far end, so the
 * row reads as "what am I looking for" on the left and "how is the camera set" on the right.
 *
 * Both overlays and the snackbar are inside this pane rather than over the window. A scrim
 * across the panel would hide the shutter that cancels the very thing the scrim is reporting,
 * and a notice floated over the whole window would land on the recent scans.
 */
@Composable
private fun ViewfinderPane(
    state: LensState,
    controller: CameraController,
    capabilities: CameraCapabilities,
    onIntent: (LensIntent) -> Unit,
    onPinch: (Float) -> Unit,
    onZoomProgress: (Float) -> Unit,
    onZoomGlideTo: (Float) -> Unit,
    cameraGranted: Boolean,
    cameraDeniedForGood: Boolean,
    onRequestCamera: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Column(modifier = Modifier.fillMaxSize().background(InkDarkest).screenInsetsPadding()) {
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
                // Gathered at the end of the row instead of spread across it. The phone
                // gives this row the full width and `SpaceEvenly` is what fills it; here
                // the chips have taken that width and the tools are the tail of the line.
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            )
        }

        CameraFrame(
            state = state,
            controller = controller,
            capabilities = capabilities,
            onIntent = onIntent,
            onPinch = onPinch,
            onZoomProgress = onZoomProgress,
            onZoomGlideTo = onZoomGlideTo,
            cameraGranted = cameraGranted,
            cameraDeniedForGood = cameraDeniedForGood,
            onRequestCamera = onRequestCamera,
            snackbarHostState = snackbarHostState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = ScreenGutter, end = ScreenGutter, bottom = ScreenGutter),
        )
    }
}

/**
 * The frame itself, and everything that floats on it.
 *
 * A composable of its own rather than a `Box` inside [ViewfinderPane], and not only for
 * length: nested in the pane's `Column`, the two full-frame overlays below resolve to
 * `ColumnScope.AnimatedVisibility` — an overload that animates a column's height and cannot
 * take the `BoxScope` alignment they need. Giving the frame its own composable takes the
 * column scope out of view, which is the honest fix; the alternative is an import alias that
 * the next person has to work out from scratch.
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
    cameraGranted: Boolean,
    cameraDeniedForGood: Boolean,
    onRequestCamera: () -> Unit,
    snackbarHostState: SnackbarHostState,
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
        if (cameraGranted) {
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
                deniedForGood = cameraDeniedForGood,
                onRequest = onRequestCamera,
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
            )
        }

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
                    modifier = Modifier
                        .widthIn(max = TRANSLATE_BAR_MAX_WIDTH)
                        .padding(bottom = Spacing.sm),
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

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = MESSAGES_BOTTOM_INSET, start = ScreenGutter, end = ScreenGutter),
        )
    }
}

/**
 * The fixed column: what the lens is set to do, the button that does it, and what it did.
 *
 * Reading top to bottom it answers three questions in the order a traveller asks them —
 * *what will this recognise*, *how do I take it*, *what did I just take* — which is why the
 * kicker and the mode's own hint sit above the shutter rather than beside the chips that set
 * them. The chips are a control; this is the sentence they produce.
 *
 * The gallery button sits beside the shutter in the phone's own order, left of it. The
 * wireframe leaves it out, but dropping it would mean an iPad cannot recognise a photograph
 * already in the library — a feature lost by turning the device sideways.
 */
@Composable
private fun LensControlPanel(
    state: LensState,
    onIntent: (LensIntent) -> Unit,
    onOpenJournal: () -> Unit,
    /** Whether the camera permission is granted — the shutter's business alone. */
    cameraReady: Boolean,
) {
    // Hands back a path already copied into app storage, so a picked photo travels the
    // same pipeline as a captured one.
    val pickPhoto = rememberPhotoPicker(maxItems = 1) { paths ->
        paths.firstOrNull()?.let { onIntent(LensIntent.PhotoPicked(it)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .screenInsetsPadding()
            .padding(horizontal = ScreenGutter),
        verticalArrangement = Arrangement.spacedBy(PageSpacing.sectionGap),
    ) {
        Column(
            modifier = Modifier.padding(top = PageSpacing.headerTop),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Kicker(
                text = stringResource(Res.string.camera_ready_to_scan),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(state.mode.labelRes()),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(state.mode.hintRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GalleryButton(enabled = !state.isBusy, onClick = pickPhoto)

            ShutterButton(
                // Stays live through a countdown: while the number is on screen the
                // shutter is the only way to call the photo off.
                enabled = cameraReady && !state.isAnalysing && !state.isCapturing,
                counting = state.isCountingDown,
                onClick = { onIntent(LensIntent.ShutterPressed) },
            )
        }

        if (state.recentDiscoveries.isNotEmpty()) {
            // Weighted so the list, and only the list, absorbs whatever height is left and
            // scrolls inside it. Unweighted it would be measured against the whole column
            // and push the shutter up as captures arrive — the one control on this panel
            // that must never move.
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Kicker(
                    text = stringResource(Res.string.camera_recent_scans),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RecentScanList(
                    discoveries = state.recentDiscoveries,
                    // Blocking pointers under the scrim does nothing for TalkBack,
                    // which navigates by semantics rather than by touch.
                    enabled = !state.isBusy,
                    onOpenJournal = onOpenJournal,
                )
            }
        }
    }
}

/**
 * How far a message is lifted off the foot of the frame.
 *
 * A measured position rather than a gap, so it is not on the `Spacing` scale — §13.2. It is
 * measured against what already sits on that edge: the zoom dial is 68 dp tall and stands
 * 8 dp clear of the border, with the translate bar and its own gap above it in translate
 * mode. A notice landing on either would cover the control the traveller is reaching for
 * while they read it.
 *
 * `PageSpacing.snackbarLift` is the phone's answer to the same question and is deliberately
 * not reused: that one is measured against a navigation bar, and this window has none.
 */
private val MESSAGES_BOTTOM_INSET = 144.dp

/**
 * How wide the translate bar is allowed to grow here.
 *
 * A measured position, not a gap. `TranslateLanguageBar` fills the width it is given and
 * splits it in equal halves either side of the swap button, which is what keeps that button
 * on the frame's axis instead of sliding about as language names change length. On a phone
 * the width it is given is a 411 dp frame less its own 24 dp margins, so the capsule comes
 * out at roughly this; across a 900 dp viewfinder the same rule stretched the two chips to
 * opposite ends of the picture with a swap button marooned in the middle.
 *
 * Capping the width rather than changing the component is the whole point: the bar still
 * decides how it lays itself out, and the arrangement decides only how much room it gets.
 */
private val TRANSLATE_BAR_MAX_WIDTH = 380.dp
