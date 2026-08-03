package com.duylt.trave.vietlensai.feature.discovery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.action_cancel
import com.duylt.trave.vietlensai.resources.action_close
import com.duylt.trave.vietlensai.resources.action_delete
import com.duylt.trave.vietlensai.resources.analysing_title
import com.duylt.trave.vietlensai.resources.discovery_ask
import com.duylt.trave.vietlensai.resources.discovery_ask_subtitle
import com.duylt.trave.vietlensai.resources.discovery_confidence
import com.duylt.trave.vietlensai.resources.discovery_delete
import com.duylt.trave.vietlensai.resources.discovery_delete_confirm_body
import com.duylt.trave.vietlensai.resources.discovery_delete_confirm_title
import com.duylt.trave.vietlensai.resources.discovery_favorite_add
import com.duylt.trave.vietlensai.resources.discovery_favorite_saved
import com.duylt.trave.vietlensai.resources.discovery_listen
import com.duylt.trave.vietlensai.resources.discovery_listen_meta
import com.duylt.trave.vietlensai.resources.discovery_listen_title
import com.duylt.trave.vietlensai.resources.discovery_low_confidence
import com.duylt.trave.vietlensai.resources.discovery_model
import com.duylt.trave.vietlensai.resources.discovery_nearby
import com.duylt.trave.vietlensai.resources.discovery_not_found
import com.duylt.trave.vietlensai.resources.discovery_note_add
import com.duylt.trave.vietlensai.resources.discovery_note_add_photo
import com.duylt.trave.vietlensai.resources.discovery_note_camera_flip
import com.duylt.trave.vietlensai.resources.discovery_note_camera_permission_allow
import com.duylt.trave.vietlensai.resources.discovery_note_camera_permission_blocked_body
import com.duylt.trave.vietlensai.resources.discovery_note_camera_permission_body
import com.duylt.trave.vietlensai.resources.discovery_note_camera_permission_deny
import com.duylt.trave.vietlensai.resources.discovery_note_camera_permission_title
import com.duylt.trave.vietlensai.resources.discovery_note_take_photo
import com.duylt.trave.vietlensai.resources.permission_open_settings
import com.duylt.trave.vietlensai.resources.discovery_note_delete
import com.duylt.trave.vietlensai.resources.discovery_note_edit
import com.duylt.trave.vietlensai.resources.discovery_note_edited
import com.duylt.trave.vietlensai.resources.discovery_note_empty_body
import com.duylt.trave.vietlensai.resources.discovery_note_empty_title
import com.duylt.trave.vietlensai.resources.discovery_note_in_diary
import com.duylt.trave.vietlensai.resources.discovery_note_kicker
import com.duylt.trave.vietlensai.resources.discovery_note_open_photo
import com.duylt.trave.vietlensai.resources.discovery_note_photo_count
import com.duylt.trave.vietlensai.resources.discovery_note_photo_position
import com.duylt.trave.vietlensai.resources.discovery_note_placeholder
import com.duylt.trave.vietlensai.resources.discovery_note_remove_photo
import com.duylt.trave.vietlensai.resources.discovery_note_save
import com.duylt.trave.vietlensai.resources.discovery_notice_count
import com.duylt.trave.vietlensai.resources.discovery_photo_label
import com.duylt.trave.vietlensai.resources.discovery_share
import com.duylt.trave.vietlensai.resources.discovery_share_body
import com.duylt.trave.vietlensai.resources.discovery_stop
import com.duylt.trave.vietlensai.resources.discovery_walk_minutes
import com.duylt.trave.vietlensai.core.designsystem.component.ErrorState
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.component.OverlayHeader
import com.duylt.trave.vietlensai.core.designsystem.component.OverlayHeaderStyle
import com.duylt.trave.vietlensai.core.designsystem.component.OverlayIconButton
import com.duylt.trave.vietlensai.core.designsystem.component.LoadingState
import com.duylt.trave.vietlensai.core.designsystem.component.PermissionSheet
import com.duylt.trave.vietlensai.core.util.rememberCameraPermissionState
import com.duylt.trave.vietlensai.feature.camera.CameraPreview
import com.duylt.trave.vietlensai.feature.camera.LensFacing
import com.duylt.trave.vietlensai.feature.camera.rememberCameraController
import com.duylt.trave.vietlensai.core.designsystem.theme.InkBrown
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.Motion
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.Corner
import com.duylt.trave.vietlensai.core.designsystem.theme.Pill
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.StampType
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.mvi.CollectEffects
import com.duylt.trave.vietlensai.core.util.accentColor
import com.duylt.trave.vietlensai.core.util.asRelativeTime
import com.duylt.trave.vietlensai.core.util.label
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.DiscoveryNote
import com.duylt.trave.vietlensai.domain.model.NearbySuggestion
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import kotlinx.coroutines.launch
import com.duylt.trave.vietlensai.platform.rememberPhotoPicker
import com.duylt.trave.vietlensai.platform.rememberTextSharer

@Composable
fun DiscoveryRoute(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoveryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            DiscoveryEffect.NavigateBack -> onBack()
            is DiscoveryEffect.OpenChat -> onOpenChat(effect.discoveryId)
        }
    }

    DiscoveryScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onOpenChat = onOpenChat,
        modifier = modifier,
    )
}

/**
 * The result page, read as a story rather than a data sheet.
 *
 * The order is deliberate: the traveller's own photo, then the name, then a voice
 * they can start and put in their pocket, then the narrative — everything that
 * merely *describes* the record (tags, actions, provenance) waits until after the
 * reading is done.
 */
@Composable
private fun DiscoveryScreen(
    state: DiscoveryState,
    onIntent: (DiscoveryIntent) -> Unit,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val discovery = state.discovery
    val shareText = rememberTextSharer()
    val shareLabel = stringResource(Res.string.discovery_share)

    // `requestOrOpenSettings` is the whole decision the sheet needs: ask while the OS still
    // asks, hand over to the app's settings page once it has stopped.
    val cameraPermission = rememberCameraPermissionState()
    var showPermissionSheet by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    // Set only when the traveller asked for the camera and is waiting on an answer, so a
    // permission granted for some other reason never opens a viewfinder unasked.
    var awaitingPermission by remember { mutableStateOf(false) }

    // The strip is snapshotted on the tap rather than read live while open, because the
    // viewer covers the page it came from: nothing can add or remove a photo behind it,
    // and a list that cannot change is one less thing for the pager to be paging through.
    var openPhotos by remember { mutableStateOf<PhotoAlbum?>(null) }

    // The viewfinder replaces the page rather than covering it, so the scroll position has
    // to be held out here — left to the LazyColumn it would reset every time the camera opened.
    val listState = rememberLazyListState()

    val requestCamera = {
        if (cameraPermission.isGranted) showCamera = true else showPermissionSheet = true
    }

    // Granted through the system dialog, or out in Settings and back again: either way the
    // camera opens without making them find the button a second time.
    LaunchedEffect(cameraPermission.isGranted) {
        if (awaitingPermission && cameraPermission.isGranted) {
            awaitingPermission = false
            showCamera = true
        }
    }

    // One key for everything this screen can be showing, so that arriving at the finished
    // page and opening the viewfinder over it are transitions rather than swaps. Derived
    // rather than stored: the four states are already implied by the ones above, and a
    // second copy of them is a second thing that can be wrong.
    val page = when {
        state.isLoading -> DiscoveryPage.ANALYSING
        discovery == null -> DiscoveryPage.MISSING
        showCamera -> DiscoveryPage.VIEWFINDER
        openPhotos != null -> DiscoveryPage.PHOTOS
        else -> DiscoveryPage.STORY
    }

    // Same reason the viewfinder holds it: a full-screen thing opened over the page is
    // what back should close, and without this the press pops the whole screen and the
    // traveller loses their place in the article as well as the photo.
    BackHandler(enabled = openPhotos != null) { openPhotos = null }

    // The album outlives the state that named it by the length of the closing transition.
    val album = rememberLastPresent(openPhotos)

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = page,
            transitionSpec = { pageTransform() },
            modifier = Modifier.fillMaxSize(),
            label = "discoveryPage",
        ) { target ->
            when (target) {
                DiscoveryPage.ANALYSING ->
                    LoadingState(message = stringResource(Res.string.analysing_title))

                DiscoveryPage.MISSING ->
                    ErrorState(message = stringResource(Res.string.discovery_not_found))

                // Ahead of the page, not on top of it: a viewfinder drawn over a scrollable
                // sheet still lets the sheet scroll underneath the finger.
                DiscoveryPage.VIEWFINDER -> NoteCameraOverlay(
                    onCaptured = { path ->
                        showCamera = false
                        onIntent(DiscoveryIntent.NotePhotoCaptured(path))
                    },
                    onClose = { showCamera = false },
                )

                DiscoveryPage.PHOTOS -> album?.let { open ->
                    NotePhotoViewer(
                        paths = open.paths,
                        initialIndex = open.index,
                        onClose = { openPhotos = null },
                    )
                }

                DiscoveryPage.STORY -> {
                    // The record can be deleted out from under a page that is still fading:
                    // there is nothing left to draw for those frames, and nothing to say
                    // about it either — the screen is already on its way back.
                    val shown = discovery ?: return@AnimatedContent
                    val shareBody = stringResource(
                        Res.string.discovery_share_body,
                        shown.title,
                        shown.summary,
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            // Edge-to-edge means the keyboard arrives as an inset rather than
                            // resizing the window, so the list has to give way itself — without
                            // this the note composer is typed into from behind the keyboard.
                            modifier = Modifier.fillMaxSize().imePadding(),
                            contentPadding = PaddingValues(bottom = COMPOSER_CLEARANCE),
                        ) {
                            item { PhotoHeader(discovery = shown) }

                            item { TitleBlock(discovery = shown) }

                            item {
                                ListenCard(
                                    discovery = shown,
                                    isSpeaking = state.isSpeaking,
                                    language = state.language,
                                    onToggle = { onIntent(DiscoveryIntent.ToggleSpeech) },
                                )
                            }

                            if (!shown.isConfident) {
                                item { LowConfidenceNote() }
                            }

                            item { StoryBlock(discovery = shown) }

                            if (shown.funFacts.isNotEmpty()) {
                                item { NoticeCard(facts = shown.funFacts) }
                            }

                            if (shown.tags.isNotEmpty()) {
                                item { TagFlow(tags = shown.tags, accent = shown.category.accentColor) }
                            }

                            item {
                                ActionRow(
                                    isFavorite = shown.isFavorite,
                                    onToggleFavorite = { onIntent(DiscoveryIntent.ToggleFavorite) },
                                    onShare = { shareText(shown.title, shareBody) },
                                )
                            }

                            // Placed with keeping and sharing rather than with the narrative:
                            // writing a note is something done *to* the record, once the
                            // reading is finished.
                            item {
                                NoteBlock(
                                    note = state.note,
                                    editor = state.noteEditor,
                                    isSaving = state.isSavingNote,
                                    language = state.language,
                                    onCapture = requestCamera,
                                    onOpenPhoto = { paths, index ->
                                        openPhotos = PhotoAlbum(paths, index)
                                    },
                                    onIntent = onIntent,
                                )
                            }

                            if (shown.nearbySuggestions.isNotEmpty()) {
                                item { NearbyBlock(suggestions = shown.nearbySuggestions) }
                            }

                            if (shown.suggestedQuestions.isNotEmpty()) {
                                item {
                                    SuggestedQuestions(
                                        questions = shown.suggestedQuestions,
                                        onAsk = { onIntent(DiscoveryIntent.AskSuggested(it)) },
                                    )
                                }
                            }

                            item { Footer(discovery = shown, state = state) }
                        }

                        // Stood down while a note is being written: it floats over the
                        // composer's own save button, and asking the guide a question is not
                        // what the traveller is in the middle of doing. It leaves downwards,
                        // the way it would fall out of the frame, rather than dissolving on
                        // the spot — the composer is opening underneath it and two things
                        // fading in the same place at once read as a glitch.
                        AnimatedVisibility(
                            visible = !state.isEditingNote,
                            enter = fadeIn(Motion.enter()) +
                                slideInVertically(Motion.enter()) { it / 2 },
                            exit = fadeOut(Motion.exit()) +
                                slideOutVertically(Motion.exit()) { it / 2 },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(ScreenGutter),
                        ) {
                            AskPill(onClick = { onOpenChat(shown.id) })
                        }
                    }
                }
            }
        }

        // Close on the left, delete on the right: the two ways out of this page,
        // as far apart as the frame allows so neither is hit by accident. Both stand down
        // for anything opened over the page — the viewfinder and the photo viewer each
        // bring their own way out and mean something else by "close", and leaving these
        // would offer two X buttons a thumb's width apart. They shrink away rather than
        // blink out, so the swap reads as the same buttons stepping aside.
        val pageControls = !showCamera && openPhotos == null

        // One header rather than two corner-pinned buttons, so the notch inset is taken
        // once, by the component that owns it. Each button keeps its own animation: they
        // come and go independently, which a typed action list could not express.
        OverlayHeader(
            style = OverlayHeaderStyle.Plain,
            modifier = Modifier.align(Alignment.TopCenter),
            leading = {
                AnimatedVisibility(
                    visible = pageControls,
                    enter = fadeIn(Motion.enter(Motion.QUICK_MILLIS)) +
                        scaleIn(Motion.enter(Motion.QUICK_MILLIS), initialScale = 0.8f),
                    exit = fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = 0.8f),
                ) {
                    OverlayIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.action_close),
                        onClick = onBack,
                    )
                }
            },
            trailing = {
                AnimatedVisibility(
                    visible = pageControls && discovery != null,
                    enter = fadeIn(Motion.enter(Motion.QUICK_MILLIS)) +
                        scaleIn(Motion.enter(Motion.QUICK_MILLIS), initialScale = 0.8f),
                    exit = fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = 0.8f),
                ) {
                    OverlayIconButton(
                        icon = Icons.Filled.Delete,
                        contentDescription = stringResource(Res.string.discovery_delete),
                        onClick = { onIntent(DiscoveryIntent.RequestDelete) },
                    )
                }
            },
        )
    }

    if (showPermissionSheet) {
        PermissionSheet(
            title = stringResource(Res.string.discovery_note_camera_permission_title),
            body = stringResource(
                if (cameraPermission.isDeniedForGood) {
                    Res.string.discovery_note_camera_permission_blocked_body
                } else {
                    Res.string.discovery_note_camera_permission_body
                },
            ),
            confirmLabel = stringResource(
                if (cameraPermission.isDeniedForGood) {
                    Res.string.permission_open_settings
                } else {
                    Res.string.discovery_note_camera_permission_allow
                },
            ),
            dismissLabel = stringResource(Res.string.discovery_note_camera_permission_deny),
            onConfirm = {
                showPermissionSheet = false
                awaitingPermission = true
                cameraPermission.requestOrOpenSettings()
            },
            onDismiss = {
                showPermissionSheet = false
                awaitingPermission = false
            },
        )
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { onIntent(DiscoveryIntent.CancelDelete) },
            title = { Text(stringResource(Res.string.discovery_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.discovery_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { onIntent(DiscoveryIntent.ConfirmDelete) }) {
                    Text(stringResource(Res.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(DiscoveryIntent.CancelDelete) }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

/** Everything this screen can be showing at once, as one value it can be animated between. */
private enum class DiscoveryPage { ANALYSING, MISSING, VIEWFINDER, PHOTOS, STORY }

/**
 * A note's photo strip, frozen at the moment one of its photos was tapped.
 *
 * @param index which of [paths] to open on.
 */
private data class PhotoAlbum(val paths: List<String>, val index: Int)

/** Both of these open *over* the page, so both come forward out of it and fall back into it. */
private val OVERLAY_PAGES = setOf(DiscoveryPage.VIEWFINDER, DiscoveryPage.PHOTOS)

/**
 * Anything opened over the article grows forward out of it and closes back down into it.
 *
 * The scale is deliberately slight. The viewfinder is a live image and the viewer is the
 * traveller's own photo, and anything that pushes either around noticeably in its first
 * frames reads as the picture failing rather than as a transition. Everything else on this
 * screen is a crossfade, because the analysing spinner, the missing-record notice and the
 * finished page all occupy the same rectangle and there is no direction to arrive from.
 */
private fun AnimatedContentTransitionScope<DiscoveryPage>.pageTransform(): ContentTransform {
    val transform = when {
        targetState in OVERLAY_PAGES ->
            (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 0.94f)) togetherWith
                fadeOut(Motion.exit())

        initialState in OVERLAY_PAGES ->
            fadeIn(Motion.enter()) togetherWith
                (fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = 0.94f))

        else ->
            fadeIn(Motion.enter(Motion.EMPHASISED_MILLIS)) togetherWith
                fadeOut(Motion.exit(Motion.STANDARD_MILLIS))
    }
    // Every page here fills the frame, so this is not animating a size change so much as
    // declining to clip one of them to the other's bounds while they overlap.
    return transform using SizeTransform(clip = false)
}

/**
 * The traveller's own photo, with the page's paper edge lapping over its foot.
 *
 * The rounded lip is what makes the rest of the screen read as a sheet laid on top
 * of the shot rather than as a second, unrelated panel below it.
 */
@Composable
private fun PhotoHeader(discovery: Discovery) {
    Box(modifier = Modifier.fillMaxWidth().height(HEADER_HEIGHT)) {
        if (discovery.imagePath != null) {
            AppAsyncImage(
                model = discovery.imagePath,
                contentDescription = discovery.title,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Kicker(
                    text = stringResource(Res.string.discovery_photo_label, discovery.title),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Only the top and bottom edges are darkened: the middle is the photo, and
        // a full scrim over it would flatten the one image the traveller took.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.42f),
                        0.35f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.45f),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = ScreenGutter, bottom = SHEET_LIP + Spacing.lg)
                .clip(Pill)
                .background(Vermilion)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PaperCream))
            Spacer(Modifier.width(Spacing.sm))
            Kicker(
                text = "${discovery.category.label()} · " + stringResource(
                    Res.string.discovery_confidence,
                    (discovery.confidence * 100).toInt(),
                ),
                color = PaperCream,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(SHEET_LIP)
                .clip(RoundedCornerShape(topStart = Corner.extraLarge, topEnd = Corner.extraLarge))
                .background(MaterialTheme.colorScheme.background),
        )
    }
}

/** Place and name, opened by a mono kicker that runs into a dashed rule. */
@Composable
private fun TitleBlock(discovery: Discovery) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter)) {
        val place = discovery.placeHint?.takeIf { it.isNotBlank() }
        if (place != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(Spacing.xs))
                Kicker(text = place, color = MaterialTheme.colorScheme.primary)
                DashedRule(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = Spacing.md).weight(1f),
                )
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        Text(
            text = discovery.title,
            style = MaterialTheme.typography.headlineLarge,
        )

        val localName = discovery.localName?.takeIf { it.isNotBlank() && it != discovery.title }
        val subtitle = listOfNotNull(localName, discovery.category.label()).joinToString(" · ")
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The narration, offered as a thing to press rather than an icon to find.
 *
 * Fixed ink-and-marigold rather than scheme colours: this is the one block on a
 * pale page that has to read as a different object — a player, not a paragraph.
 */
@Composable
private fun ListenCard(
    discovery: Discovery,
    isSpeaking: Boolean,
    language: AppLanguage,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = Spacing.xl),
        shape = MaterialTheme.shapes.large,
        color = InkBrown,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Marigold),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isSpeaking) Res.string.discovery_stop else Res.string.discovery_listen,
                    ),
                    tint = InkBrown,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.discovery_listen_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = PaperCream,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = stringResource(
                        Res.string.discovery_listen_meta,
                        discovery.narrationMinutes(),
                        language.displayName,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = PaperCream.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Waveform(isPlaying = isSpeaking)
        }
    }
}

/** Four bars that only move while the voice is actually speaking. */
@Composable
private fun Waveform(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val heights = WAVEFORM_BARS.mapIndexed { index, resting ->
        val animated by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520 + index * 130),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bar$index",
        )
        if (isPlaying) animated else resting
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        modifier = Modifier.height(28.dp),
    ) {
        heights.forEachIndexed { index, fraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(28.dp * fraction)
                    .clip(Pill)
                    .background(if (index % 2 == 0) Marigold else PaperCream.copy(alpha = 0.55f)),
            )
        }
    }
}

@Composable
private fun LowConfidenceNote() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = Spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.discovery_low_confidence),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(Spacing.md),
        )
    }
}

/**
 * Summary and sections as one continuous read.
 *
 * The summary keeps full-strength ink and every section after it steps back a
 * shade, so the page has a lede and a body instead of eight equal paragraphs.
 */
@Composable
private fun StoryBlock(discovery: Discovery) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = Spacing.xs)) {
        Text(
            text = discovery.summary,
            style = MaterialTheme.typography.bodyLarge,
        )
        discovery.sections.forEach { section ->
            Spacer(Modifier.height(Spacing.xl))
            Kicker(text = section.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = section.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The facts worth carrying into the visit, numbered so they can be counted off. */
@Composable
private fun NoticeCard(facts: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = Spacing.xl),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Kicker(
                text = pluralStringResource(
                    Res.plurals.discovery_notice_count,
                    facts.size,
                    facts.size,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            facts.forEachIndexed { index, fact ->
                Spacer(Modifier.height(if (index == 0) Spacing.lg else Spacing.md))
                Row {
                    Ordinal(index + 1)
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = fact,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** "01", "02" — the mono counter that runs down every numbered list in the app. */
@Composable
private fun Ordinal(number: Int, color: Color = MaterialTheme.colorScheme.secondary) {
    Text(
        text = number.toString().padStart(2, '0'),
        style = StampType.ordinal,
        color = color,
        modifier = Modifier.padding(top = Spacing.xxs),
    )
}

@Composable
private fun TagFlow(tags: List<String>, accent: Color) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        tags.forEachIndexed { index, tag ->
            val tinted = index == 0
            Text(
                text = tag,
                style = MaterialTheme.typography.bodySmall,
                color = if (tinted) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(Pill)
                    .background(
                        if (tinted) {
                            accent.copy(alpha = 0.13f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
    }
}

/**
 * Keeping and passing on: the two things left to do once the page has been read.
 *
 * Favouriting gets the wide button because it is the one the traveller comes back
 * for; sharing sits beside it as an icon, since it leaves the app either way.
 */
@Composable
private fun ActionRow(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = Spacing.xl),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onToggleFavorite,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = MaterialTheme.shapes.medium,
            color = if (isFavorite) MaterialTheme.colorScheme.surfaceVariant else Vermilion,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val content = if (isFavorite) MaterialTheme.colorScheme.primary else PaperCream
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(
                        if (isFavorite) {
                            Res.string.discovery_favorite_saved
                        } else {
                            Res.string.discovery_favorite_add
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = content,
                )
            }
        }

        Surface(
            onClick = onShare,
            modifier = Modifier.size(56.dp),
            shape = MaterialTheme.shapes.medium,
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(Res.string.discovery_share),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * The traveller's own note — the only thing on this page the app did not write.
 *
 * Everything above is Gemini's account of a place, and it all looks alike because it is
 * all the same voice. This block is deliberately the odd one out: dashed and empty until
 * it is filled, then the traveller's photos at full width above their own words. It is
 * what turns the record of *what was recognised* into a record of *what was lived*.
 */
@Composable
private fun NoteBlock(
    note: DiscoveryNote?,
    editor: NoteEditor?,
    isSaving: Boolean,
    language: AppLanguage,
    onCapture: () -> Unit,
    onOpenPhoto: (paths: List<String>, index: Int) -> Unit,
    onIntent: (DiscoveryIntent) -> Unit,
) {
    val card = when {
        editor != null -> NoteCard.COMPOSER
        note != null -> NoteCard.WRITTEN
        else -> NoteCard.BLANK
    }

    // The card being replaced stays composed for the length of the transition, and by then
    // the state it was drawn from is already gone — the editor is null the instant Cancel is
    // pressed, the note the instant it is deleted. Reading through the last value there was
    // is what lets the outgoing card leave with its own words and photos still on it, instead
    // of blanking on the first frame and dissolving an empty box.
    val leaving = rememberLastPresent(editor)
    val written = rememberLastPresent(note)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = Spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Kicker(
                text = stringResource(Res.string.discovery_note_kicker),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            // Only offered once something is there to edit; the empty card is its own
            // invitation. It collapses towards the edge it sits against rather than fading
            // in place, so the row never holds a gap where a button used to be.
            AnimatedVisibility(
                visible = card == NoteCard.WRITTEN,
                enter = fadeIn(Motion.enter()) +
                    expandHorizontally(Motion.enter(), expandFrom = Alignment.End),
                exit = fadeOut(Motion.exit()) +
                    shrinkHorizontally(Motion.exit(), shrinkTowards = Alignment.End),
            ) {
                TextButton(onClick = { onIntent(DiscoveryIntent.StartEditNote) }) {
                    Text(
                        text = stringResource(Res.string.discovery_note_edit),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.sm))

        // The three cards are the same object in three states, not three different things,
        // so the block grows and shrinks between them rather than cutting. It is the one
        // animation on this page the traveller will see over and over — every note is
        // written, saved, reopened and saved again through exactly this swap.
        AnimatedContent(
            targetState = card,
            transitionSpec = { noteCardTransform() },
            modifier = Modifier.fillMaxWidth(),
            label = "noteCard",
        ) { target ->
            when (target) {
                NoteCard.COMPOSER -> leaving?.let { open ->
                    NoteComposer(
                        editor = open,
                        // The live note, not the retained one: a note deleted and then
                        // started again would otherwise be offered a delete button for a
                        // record that is no longer there.
                        hasSavedNote = note != null,
                        isSaving = isSaving,
                        onCapture = onCapture,
                        onOpenPhoto = { index -> onOpenPhoto(open.photoPaths, index) },
                        onIntent = onIntent,
                    )
                }

                NoteCard.WRITTEN -> written?.let { saved ->
                    SavedNote(
                        note = saved,
                        language = language,
                        onOpenPhoto = { index -> onOpenPhoto(saved.photoPaths, index) },
                    )
                }

                NoteCard.BLANK -> EmptyNoteCard(
                    onStart = { onIntent(DiscoveryIntent.StartEditNote) },
                )
            }
        }
    }
}

/** Which of the note block's three faces is showing. */
private enum class NoteCard { BLANK, COMPOSER, WRITTEN }

/**
 * The card swaps by growing into the new one, not by sliding it in from a side.
 *
 * There is no left or right here — the composer *is* the saved note with its edges lifted —
 * so the only honest movement is the height change, which [SizeTransform] carries. The
 * slight scale on the way in stops the arriving card from looking like it was always there
 * and merely uncovered.
 */
private fun AnimatedContentTransitionScope<NoteCard>.noteCardTransform(): ContentTransform {
    val transform = (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 0.96f))
        .togetherWith(fadeOut(Motion.exit()))
    return transform using SizeTransform { _, _ -> Motion.morph() }
}

/**
 * The value, or the last one there was.
 *
 * For content that outlives the state behind it: anything animating out is composed for a
 * few hundred milliseconds after the thing it describes has already been set to null, and
 * without a value to fall back on it spends that time drawing nothing.
 */
@Composable
private fun <T : Any> rememberLastPresent(value: T?): T? {
    val holder = remember { mutableStateOf(value) }
    // Written but not read on this path, so nothing that has already composed is invalidated
    // by keeping it current; the fallback below is the only reader, and only once the live
    // value is gone.
    if (value != null) {
        holder.value = value
        return value
    }
    return holder.value
}

/** A blank ruled off in the notebook: dashed, unfilled, and obviously meant for the reader. */
@Composable
private fun EmptyNoteCard(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .dashedBorder(MaterialTheme.colorScheme.outlineVariant, cornerRadius = Corner.large)
            .clickable(onClick = onStart)
            .padding(Spacing.xl),
    ) {
        Text(
            text = stringResource(Res.string.discovery_note_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(Res.string.discovery_note_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AddAPhoto,
                contentDescription = null,
                tint = Vermilion,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = stringResource(Res.string.discovery_note_add),
                style = MaterialTheme.typography.titleSmall,
                color = Vermilion,
            )
        }
    }
}

/** A written note at rest: their photos, their words, and when they wrote them. */
@Composable
private fun SavedNote(
    note: DiscoveryNote,
    language: AppLanguage,
    onOpenPhoto: (index: Int) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            if (note.photoPaths.isNotEmpty()) {
                NotePhotoStrip(
                    paths = note.photoPaths,
                    onOpen = onOpenPhoto,
                    onRemove = null,
                )
                Spacer(Modifier.height(if (note.body.isNotBlank()) Spacing.lg else Spacing.xs))
            }
            if (note.body.isNotBlank()) {
                Text(text = note.body, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(Spacing.md))
            }
            Kicker(
                text = buildString {
                    append(note.updatedAt.asRelativeTime(language))
                    if (note.wasEdited) {
                        append(" · ").append(stringResource(Res.string.discovery_note_edited))
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The composer.
 *
 * Photos sit above the text field because that is the order the memory arrives in — the
 * shot is already on the phone, the words come after looking at it.
 */
@Composable
private fun NoteComposer(
    editor: NoteEditor,
    hasSavedNote: Boolean,
    isSaving: Boolean,
    onCapture: () -> Unit,
    onOpenPhoto: (index: Int) -> Unit,
    onIntent: (DiscoveryIntent) -> Unit,
) {
    // The picker is told how many slots are left, so its own selection counter stops the
    // traveller at the limit instead of the app quietly discarding what did not fit.
    val pickPhotos = rememberPhotoPicker(
        maxItems = DiscoveryNote.MAX_PHOTOS - editor.photoPaths.size,
    ) { paths ->
        onIntent(DiscoveryIntent.NotePhotoPicked(paths))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            NotePhotoStrip(
                paths = editor.photoPaths,
                onOpen = onOpenPhoto,
                onRemove = { path -> onIntent(DiscoveryIntent.NotePhotoRemoved(path)) },
                onAdd = pickPhotos.takeIf { editor.canAddPhoto },
                onCapture = onCapture.takeIf { editor.canAddPhoto },
            )

            Spacer(Modifier.height(Spacing.xs))
            Kicker(
                text = stringResource(
                    Res.string.discovery_note_photo_count,
                    editor.photoPaths.size,
                    DiscoveryNote.MAX_PHOTOS,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = editor.body,
                onValueChange = { onIntent(DiscoveryIntent.NoteBodyChanged(it)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = {
                    Text(
                        text = stringResource(Res.string.discovery_note_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Vermilion,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            Spacer(Modifier.height(Spacing.sm))
            // Says out loud what the note is *for*: the diary the guide writes each evening
            // reads these, which is not something the traveller could guess from a text box.
            Text(
                text = stringResource(Res.string.discovery_note_in_diary),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.lg))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // A write to the database is usually over in a frame or two, and a button
                // that goes grey and back in that time reads as a flicker rather than as
                // work being done. Fading it holds the two states apart.
                val saveColor by animateColorAsState(
                    targetValue = if (isSaving) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        Vermilion
                    },
                    animationSpec = Motion.morph(Motion.QUICK_MILLIS),
                    label = "noteSaveColor",
                )

                Surface(
                    onClick = { onIntent(DiscoveryIntent.SaveNote) },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = saveColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.discovery_note_save),
                            style = MaterialTheme.typography.titleSmall,
                            color = PaperCream,
                        )
                    }
                }

                TextButton(
                    onClick = { onIntent(DiscoveryIntent.CancelEditNote) },
                    enabled = !isSaving,
                ) {
                    Text(
                        text = stringResource(Res.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Folds up out of the card rather than vanishing: it is the last row, so its
            // going takes the card's foot with it, and a height that jumps looks like the
            // note was cut off.
            AnimatedVisibility(
                visible = hasSavedNote,
                enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
            ) {
                TextButton(
                    onClick = { onIntent(DiscoveryIntent.DeleteNote) },
                    enabled = !isSaving,
                ) {
                    Text(
                        text = stringResource(Res.string.discovery_note_delete),
                        style = MaterialTheme.typography.labelLarge,
                        color = Vermilion,
                    )
                }
            }
        }
    }
}

/**
 * The photos kept with a note, as a strip that runs off the edge of the card.
 *
 * @param onOpen opens the photo at that position full screen. Always available: looking at
 *   what was kept is not an edit, and it is the only way to see a 104dp tile properly.
 * @param onRemove null when the note is at rest — the delete badges only exist inside an edit.
 * @param onCapture takes a new photo; null once [DiscoveryNote.MAX_PHOTOS] is reached.
 * @param onAdd picks from the gallery; null on the same condition. Both go null together,
 *   which is what removes the trailing tiles rather than leaving buttons that refuse to do
 *   anything.
 */
@Composable
private fun NotePhotoStrip(
    paths: List<String>,
    onOpen: (index: Int) -> Unit,
    onRemove: ((String) -> Unit)?,
    onAdd: (() -> Unit)? = null,
    onCapture: (() -> Unit)? = null,
) {
    if (paths.isEmpty() && onAdd == null && onCapture == null) return

    val openLabel = stringResource(Res.string.discovery_note_open_photo)

    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        itemsIndexed(items = paths, key = { _, path -> path }) { index, path ->
            // Keyed, so removing a photo from the middle slides the ones after it left into
            // the gap instead of teleporting them. Six tiles is few enough that the eye
            // tracks each one, and a strip that reshuffles instantly loses which was which.
            Box(
                modifier = Modifier.animateItem(
                    fadeInSpec = Motion.enter(),
                    placementSpec = Motion.morph(),
                    fadeOutSpec = Motion.exit(),
                ),
            ) {
                AppAsyncImage(
                    model = path,
                    contentDescription = openLabel,
                    shape = MaterialTheme.shapes.medium,
                    // Under the badge, not over it: the two targets overlap in the
                    // corner, and the badge is the smaller and more consequential of
                    // the two, so it has to be the one that wins there.
                    onClick = { onOpen(index) },
                    modifier = Modifier.size(NOTE_PHOTO_SIZE),
                )
                if (onRemove != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Spacing.xs)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onRemove(path) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.discovery_note_remove_photo),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        // Camera before gallery: standing in front of the place is when the photo worth
        // keeping does not exist yet. Both carry a key of their own so that reaching the
        // sixth photo fades them out where they stand, rather than making the last photo
        // appear to have replaced them.
        if (onCapture != null) {
            item(key = CAPTURE_TILE_KEY) {
                PhotoActionTile(
                    icon = Icons.Filled.PhotoCamera,
                    label = stringResource(Res.string.discovery_note_take_photo),
                    onClick = onCapture,
                    modifier = Modifier.animateItem(
                        fadeInSpec = Motion.enter(),
                        placementSpec = Motion.morph(),
                        fadeOutSpec = Motion.exit(),
                    ),
                )
            }
        }

        if (onAdd != null) {
            item(key = ADD_TILE_KEY) {
                PhotoActionTile(
                    icon = Icons.Filled.AddAPhoto,
                    label = stringResource(Res.string.discovery_note_add_photo),
                    onClick = onAdd,
                    modifier = Modifier.animateItem(
                        fadeInSpec = Motion.enter(),
                        placementSpec = Motion.morph(),
                        fadeOutSpec = Motion.exit(),
                    ),
                )
            }
        }
    }
}

/** An empty frame in the strip, waiting to be filled by one of the two ways in. */
@Composable
private fun PhotoActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(NOTE_PHOTO_SIZE)
            .clip(MaterialTheme.shapes.medium)
            .dashedBorder(MaterialTheme.colorScheme.outline, cornerRadius = Corner.medium)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A note's photos at full size, the way they were meant to be looked at.
 *
 * The strip is a set of 104dp thumbnails — enough to know which photo is which, not enough
 * to see anything in one. This is where a face, a menu or a plaque actually becomes legible,
 * so it is a viewer and nothing else: no delete, no share, no edit. Removing a photo stays
 * with the badge on the strip, where the traveller can see what is left of the note while
 * they do it.
 */
@Composable
private fun NotePhotoViewer(
    paths: List<String>,
    initialIndex: Int,
    onClose: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { paths.size }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Room to breathe between photos, so a half-swipe shows black rather than two
            // images touching — nothing else on this screen tells the eye where one ends.
            pageSpacing = Spacing.md,
        ) { page ->
            ZoomablePhoto(
                path = paths[page],
                // Swiping away from a photo leaves it magnified underneath, and coming back
                // to it later at 3× on some corner is disorienting. Each one is put back the
                // way it was found.
                isSettled = pagerState.currentPage == page,
            )
        }

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
        )

        // Shown even for a note with a single photo, where it says nothing new. On a screen
        // that is otherwise edge-to-edge photograph it is the one mark that this is the app
        // and not the phone's own gallery — and with more than one it is also the only clue
        // that swiping does anything at all.
        Kicker(
            text = stringResource(
                Res.string.discovery_note_photo_position,
                pagerState.currentPage + 1,
                paths.size,
            ),
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = Spacing.xxl)
                .clip(Pill)
                // Dark enough to hold white text over a white photo, which the strip is full
                // of: half of what a traveller keeps is a screenshot or a menu.
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
    }
}

/**
 * One photo, pinchable and pannable, that still lets the pager have its swipe.
 *
 * `canPan` is the whole trick: at rest a one-finger drag never passes this gesture's touch
 * slop, so it is never consumed here and reaches the pager. Once the photo is magnified the
 * same drag becomes a pan, which is right — there is now somewhere to drag *to*, and moving
 * to the next photo while looking closely at this one is not what the finger meant.
 */
@Composable
private fun ZoomablePhoto(path: String, isSettled: Boolean) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var frame by remember { mutableStateOf(IntSize.Zero) }

    /** Keeps the magnified photo's edges from being dragged inside the frame. */
    fun clamp(candidate: Offset, atScale: Float): Offset {
        if (atScale <= 1f) return Offset.Zero
        val limitX = frame.width * (atScale - 1f) / 2f
        val limitY = frame.height * (atScale - 1f) / 2f
        return Offset(
            x = candidate.x.coerceIn(-limitX, limitX),
            y = candidate.y.coerceIn(-limitY, limitY),
        )
    }

    val transformState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        val next = (scale * zoomChange).coerceIn(1f, MAX_PHOTO_ZOOM)
        // Zoom about the point between the fingers, not the middle of the screen: pinching
        // on a face in the corner should bring that face closer, not push it off the edge.
        val fromCentre = centroid - Offset(frame.width / 2f, frame.height / 2f)
        val anchored = fromCentre - (fromCentre - offset) * (next / scale)
        offset = clamp(anchored + panChange, next)
        scale = next
    }

    LaunchedEffect(isSettled) {
        if (!isSettled) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    // Snapped while the fingers are down and animated the rest of the time. Both matter:
    // a pinch that lags behind the hand feels broken, and a double-tap that arrives at 2.5×
    // in one frame is a jump cut rather than a zoom.
    val live = transformState.isTransformInProgress
    val zoom by animateFloatAsState(
        targetValue = scale,
        animationSpec = if (live) snap() else Motion.morph(Motion.QUICK_MILLIS),
        label = "photoZoom",
    )
    val pan by animateOffsetAsState(
        targetValue = offset,
        animationSpec = if (live) snap() else Motion.morph(Motion.QUICK_MILLIS),
        label = "photoPan",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { frame = it }
            .transformable(
                state = transformState,
                canPan = { scale > 1f },
                lockRotationOnZoomPan = true,
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    // The shortcut for people who never pinch: one tap in, one tap back.
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = DOUBLE_TAP_ZOOM
                            offset = Offset.Zero
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AppAsyncImage(
            model = path,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            // Barely-there white rather than the surface a card would use: this photo
            // hangs on black, and a sand-coloured rectangle filling the display while
            // the file is read would be the brightest thing the viewer ever shows.
            placeholderColor = Color.White.copy(alpha = VIEWER_PLACEHOLDER_ALPHA),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = pan.x
                    translationY = pan.y
                },
        )
    }
}

/**
 * The viewfinder, for a photo that belongs to a note rather than to recognition.
 *
 * Deliberately not the lens screen: that one is an instrument for pointing at a thing and
 * asking what it is, with zoom, flash, timers and modes to match. This is a shutter and a
 * way to turn the camera around, because the traveller already knows what they are looking at.
 *
 * [CaptureStore] is taken from Koin here rather than reached through the ViewModel. Unlike the
 * lens, this shutter is pressed inside the overlay and the ViewModel never learns of the press,
 * so there is no effect for a path to travel on — and the alternative was a `newCapturePath`
 * lambda threaded down five levels from the route, plus a second public method on a ViewModel
 * whose only entry point is meant to be `onIntent`. It is the store, not the ViewModel: nothing
 * below the route sees the ViewModel, and where captures are written stays the store's business.
 */
@Composable
private fun NoteCameraOverlay(
    onCaptured: (String) -> Unit,
    onClose: () -> Unit,
) {
    val captureStore: CaptureStore = koinInject()
    val controller = rememberCameraController()
    val capabilities by controller.capabilities.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var lensFacing by remember { mutableStateOf(LensFacing.BACK) }
    var isCapturing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
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
                    enter = fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 0.8f),
                    exit = fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = 0.8f),
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
            targetValue = if (isCapturing) 44.dp else 58.dp,
            animationSpec = Motion.morph(Motion.QUICK_MILLIS),
            label = "shutterDisc",
        )
        val shutterRing by animateFloatAsState(
            targetValue = if (isCapturing) 0.35f else 0.25f,
            animationSpec = Motion.morph(Motion.QUICK_MILLIS),
            label = "shutterRing",
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = SHUTTER_INSET)
                .size(76.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = shutterRing))
                .border(3.dp, Color.White, CircleShape)
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

/**
 * A stitched outline, for the same reason [DashedRule] exists.
 *
 * `BorderStroke` cannot carry a dash pattern, so the border is drawn rather than declared —
 * the alternative is a solid hairline that reads as a disabled control instead of a blank
 * waiting to be filled.
 */
private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    width: Dp = 1.dp,
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 9f)),
        ),
    )
}

@Composable
private fun NearbyBlock(suggestions: List<NearbySuggestion>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = Spacing.sm)) {
        Kicker(
            text = stringResource(Res.string.discovery_nearby),
            color = MaterialTheme.colorScheme.primary,
        )
        suggestions.forEachIndexed { index, suggestion ->
            Spacer(Modifier.height(Spacing.lg))
            Row(verticalAlignment = Alignment.Top) {
                Ordinal(index + 1, color = suggestion.category.accentColor)
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = suggestion.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = suggestion.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    suggestion.walkingMinutes?.let { minutes ->
                        Spacer(Modifier.height(Spacing.xs))
                        Kicker(
                            text = stringResource(Res.string.discovery_walk_minutes, minutes),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestedQuestions(questions: List<String>, onAsk: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = Spacing.xl)) {
        Kicker(
            text = stringResource(Res.string.discovery_ask_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        questions.forEach { question ->
            Surface(
                onClick = { onAsk(question) },
                shape = MaterialTheme.shapes.medium,
                color = Color.Transparent,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * The way into the conversation, floating over the read.
 *
 * Pale rather than a full accent fill: it sits on top of body text for the whole
 * scroll, and a solid vermilion slab there competes with the words underneath it.
 */
@Composable
private fun AskPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = Pill,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                start = Spacing.sm,
                end = Spacing.xl,
                top = Spacing.sm,
                bottom = Spacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Vermilion),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = PaperCream,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = stringResource(Res.string.discovery_ask),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Footer(discovery: Discovery, state: DiscoveryState) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = Spacing.sm)) {
        DashedRule(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.md))
        Kicker(
            text = discovery.createdAt.asRelativeTime(state.language),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        discovery.modelUsed?.let { model ->
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(Res.string.discovery_model, model),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A stitched rule — the ruled line of a field notebook, not a Material divider. */
@Composable
private fun DashedRule(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f)),
        )
    }
}

/**
 * How long the voice guide will talk, rounded to whole minutes.
 *
 * Estimated from the text rather than measured, because the engine only knows once
 * it has finished speaking — and by then the number is no longer a decision aid.
 */
private fun Discovery.narrationMinutes(): Int {
    val characters = summary.length +
        sections.sumOf { it.title.length + it.body.length } +
        funFacts.sumOf { it.length }
    return (characters / CHARACTERS_PER_MINUTE).coerceAtLeast(1)
}

private val HEADER_HEIGHT = 320.dp

/**
 * How far the story list stops short of the bottom edge.
 *
 * The room the floating note composer occupies, measured against it rather than
 * chosen from the spacing scale — it is a position, not a gap, and snapping it onto a
 * 4 dp step would put the last note under the field used to write it.
 */
private val COMPOSER_CLEARANCE = 132.dp

/** Where the shutter sits above the navigation bar. A hit target, so it is not snapped. */
private val SHUTTER_INSET = 40.dp

/** How far the paper sheet laps over the foot of the photo. */
private val SHEET_LIP = 26.dp

/** Big enough to recognise a face in, small enough that three fit on the narrowest phone. */
private val NOTE_PHOTO_SIZE = 104.dp

/** The ground the viewer's shimmer sweeps across, laid over its black. */
private const val VIEWER_PLACEHOLDER_ALPHA = 0.07f

/**
 * Identities for the two tiles at the end of the photo strip.
 *
 * The photos are keyed by their own paths; these two need keys of their own or the row
 * would treat the tile in slot four as the same item as the photo that later takes that
 * slot, and animate one into the other.
 */
private const val CAPTURE_TILE_KEY = "note-photo-capture"
private const val ADD_TILE_KEY = "note-photo-add"

/**
 * How far into a note photo the viewer will go.
 *
 * Four is past what a phone camera has detail for, which is the point: the ceiling should be
 * reached because the photo ran out, not because the app said so.
 */
private const val MAX_PHOTO_ZOOM = 4f

/** Close enough to read a sign, far enough that a second tap is still worth having. */
private const val DOUBLE_TAP_ZOOM = 2.5f

/** Resting heights of the waveform bars, as a fraction of the row. */
private val WAVEFORM_BARS = listOf(0.45f, 0.75f, 0.55f, 0.9f)

/** Roughly 150 words a minute at an average five characters plus a space. */
private const val CHARACTERS_PER_MINUTE = 800
