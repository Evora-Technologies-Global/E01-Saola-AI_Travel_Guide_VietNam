package com.evora.technologies.saola.mobile.feature.discovery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.saola.core.designsystem.component.AppSnackbarHost
import com.evora.technologies.saola.core.designsystem.component.ErrorState
import com.evora.technologies.saola.core.designsystem.component.LoadingState
import com.evora.technologies.saola.core.designsystem.component.OverlayHeader
import com.evora.technologies.saola.core.designsystem.component.OverlayHeaderStyle
import com.evora.technologies.saola.core.designsystem.component.OverlayIconButton
import com.evora.technologies.saola.core.designsystem.component.showError
import com.evora.technologies.saola.core.designsystem.theme.Corner
import com.evora.technologies.saola.core.designsystem.theme.Motion
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.core.mvi.CollectEffects
import com.evora.technologies.saola.core.util.accentColor
import com.evora.technologies.saola.core.util.rememberCameraPermissionState
import com.evora.technologies.saola.core.util.userMessage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.feature.discovery.DiscoveryEffect
import com.evora.technologies.saola.feature.discovery.DiscoveryIntent
import com.evora.technologies.saola.feature.discovery.DiscoveryState
import com.evora.technologies.saola.feature.discovery.DiscoveryViewModel
import com.evora.technologies.saola.feature.discovery.REPORT_RECIPIENT
import com.evora.technologies.saola.feature.discovery.buildReportBody
import com.evora.technologies.saola.feature.discovery.reportSubject
import com.evora.technologies.saola.feature.discovery.component.ContextChips
import com.evora.technologies.saola.feature.discovery.component.DeleteDiscoveryDialog
import com.evora.technologies.saola.feature.discovery.component.DiscoveryFooter
import com.evora.technologies.saola.feature.discovery.component.DiscoveryPhoto
import com.evora.technologies.saola.feature.discovery.component.DiscoveryTitleBlock
import com.evora.technologies.saola.feature.discovery.component.ListenCard
import com.evora.technologies.saola.feature.discovery.component.LowConfidenceNote
import com.evora.technologies.saola.feature.discovery.component.MatchBadge
import com.evora.technologies.saola.feature.discovery.component.NearbyBlock
import com.evora.technologies.saola.feature.discovery.component.NoteBlock
import com.evora.technologies.saola.feature.discovery.component.NoteCameraOverlay
import com.evora.technologies.saola.feature.discovery.component.NoteCameraPermissionSheet
import com.evora.technologies.saola.feature.discovery.component.NotePhotoViewer
import com.evora.technologies.saola.feature.discovery.component.PhotoAlbum
import com.evora.technologies.saola.feature.discovery.component.ReportSheet
import com.evora.technologies.saola.feature.discovery.component.SaveRow
import com.evora.technologies.saola.feature.discovery.component.StoryBody
import com.evora.technologies.saola.feature.discovery.component.SuggestedQuestions
import com.evora.technologies.saola.feature.discovery.component.ThingsToNotice
import com.evora.technologies.saola.feature.discovery.component.rememberLastPresent
import com.evora.technologies.saola.platform.rememberMailSharer
import com.evora.technologies.saola.platform.rememberTextSharer
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.action_close
import com.evora.technologies.saola.resources.analysing_title
import com.evora.technologies.saola.resources.discovery_ask
import com.evora.technologies.saola.resources.discovery_delete
import com.evora.technologies.saola.resources.discovery_not_found
import com.evora.technologies.saola.resources.discovery_share
import com.evora.technologies.saola.resources.discovery_share_body
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DiscoveryRoute(
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoveryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sendMail = rememberMailSharer()

    // The message is resolved off the effect's own payload through `userMessage()`, never
    // read back out of state — the same rule the journal, settings and explore routes follow
    // and for the same reason: an effect is handled one main-queue turn after `sendEffect`,
    // before the frame that would have produced the string. See `LLM.md` §11 row #15.
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            DiscoveryEffect.NavigateBack -> onBack()
            is DiscoveryEffect.OpenChat -> onOpenChat(effect.discoveryId)
            is DiscoveryEffect.ShowMessage -> scope.launch {
                snackbarHostState.showError(effect.error.userMessage())
            }

            // Composed on the effect's own payload for the same reason the message above is,
            // and resolved in a `suspend` builder rather than during a frame — `getString`,
            // never `stringResource`. `state.language` is read from the composition instead,
            // and safely: it is a setting that arrives on its own flow rather than a value
            // produced by the emission that raised this, which is the case row #15 is about.
            is DiscoveryEffect.SendReport -> scope.launch {
                sendMail(
                    REPORT_RECIPIENT,
                    reportSubject(effect.discovery),
                    buildReportBody(effect.report, effect.discovery, state.language),
                    effect.discovery.imagePath,
                )
            }
        }
    }

    DiscoveryScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onOpenChat = onOpenChat,
        modifier = modifier,
    )

    // Over the page and against the bottom edge, not lifted by `PageSpacing.snackbarLift`:
    // this is a detail route, so the bottom bar is already gone and there is nothing down
    // there for a notice to clear. The note composer is the one thing it can land on, which
    // is exactly where a failed save needs to be read.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AppSnackbarHost(
            snackbarHostState,
            modifier = Modifier.padding(ScreenGutter).navigationBarsPadding().imePadding(),
        )
    }
}

/**
 * The result page, read as a story rather than a data sheet.
 *
 * The order is deliberate: the traveller's own photo, then the name, then a voice they can
 * start and put in their pocket, then the narrative — everything that merely *describes* the
 * record (tags, actions, provenance) waits until after the reading is done. One column, because
 * that is what a phone is; the large-window arrangement sets the same blocks in two columns
 * with the guide beside them, and draws every one of them from `feature/discovery/component/`.
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

    // `requestOrOpenSettings` is the whole decision the sheet needs: ask while the OS still
    // asks, hand over to the app's settings page once it has stopped.
    val cameraPermission = rememberCameraPermissionState()
    var showPermissionSheet by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    // Set only when the traveller asked for the camera and is waiting on an answer, so a
    // permission granted for some other reason never opens a viewfinder unasked.
    var awaitingPermission by remember { mutableStateOf(false) }

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
                    NotePhotoViewer(album = open, onClose = { openPhotos = null })
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
                        StoryColumn(
                            state = state,
                            discovery = shown,
                            listState = listState,
                            onIntent = onIntent,
                            // The sheet's own title is the place, not the word "Share".
                            onShare = { shareText(shown.title, shareBody) },
                            onCapture = requestCamera,
                            onOpenPhoto = { paths, index -> openPhotos = PhotoAlbum(paths, index) },
                        )

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
                        scaleIn(Motion.enter(Motion.QUICK_MILLIS), initialScale = POP_SCALE),
                    exit = fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = POP_SCALE),
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
                        scaleIn(Motion.enter(Motion.QUICK_MILLIS), initialScale = POP_SCALE),
                    exit = fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = POP_SCALE),
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
        NoteCameraPermissionSheet(
            permission = cameraPermission,
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
        DeleteDiscoveryDialog(
            onConfirm = { onIntent(DiscoveryIntent.ConfirmDelete) },
            onDismiss = { onIntent(DiscoveryIntent.CancelDelete) },
        )
    }

    // Over the page rather than in place of it, so the account being objected to is still
    // there to be quoted from. The tablet renders the identical call for the same reason.
    state.reportDraft?.let { draft ->
        ReportSheet(
            draft = draft,
            isSubmitting = state.isSubmittingReport,
            hasPhoto = discovery?.imagePath != null,
            onIntent = onIntent,
        )
    }
}

/**
 * The article, top to bottom, in the one column a phone has.
 *
 * Every block below comes from `feature/discovery/component/`; what this composable owns is the
 * order they are read in and the gutter they are read at. That is the whole of what the phone
 * decides about this page, and it is why the large-window arrangement can rearrange the same
 * pieces without a single one of them being drawn twice.
 */
@Composable
private fun StoryColumn(
    state: DiscoveryState,
    discovery: Discovery,
    listState: LazyListState,
    onIntent: (DiscoveryIntent) -> Unit,
    onShare: () -> Unit,
    onCapture: () -> Unit,
    onOpenPhoto: (paths: List<String>, index: Int) -> Unit,
) {
    val gutter = Modifier.padding(horizontal = ScreenGutter)

    LazyColumn(
        state = listState,
        // Edge-to-edge means the keyboard arrives as an inset rather than resizing the
        // window, so the list has to give way itself — without this the note composer is
        // typed into from behind the keyboard.
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(bottom = COMPOSER_CLEARANCE),
    ) {
        item { PhotoHeader(discovery = discovery) }

        item { DiscoveryTitleBlock(discovery = discovery, modifier = gutter) }

        item {
            ListenCard(
                discovery = discovery,
                isSpeaking = state.isSpeaking,
                language = state.language,
                onToggle = { onIntent(DiscoveryIntent.ToggleSpeech) },
                modifier = Modifier.padding(horizontal = ScreenGutter, vertical = Spacing.xl),
            )
        }

        if (!discovery.isConfident) {
            item {
                LowConfidenceNote(
                    modifier = Modifier.padding(horizontal = ScreenGutter, vertical = Spacing.xs),
                )
            }
        }

        item {
            StoryBody(
                discovery = discovery,
                modifier = Modifier.padding(horizontal = ScreenGutter, vertical = Spacing.xs),
            )
        }

        if (discovery.funFacts.isNotEmpty()) {
            item {
                ThingsToNotice(
                    facts = discovery.funFacts,
                    modifier = Modifier.padding(horizontal = ScreenGutter, vertical = Spacing.xl),
                )
            }
        }

        if (discovery.tags.isNotEmpty()) {
            item {
                ContextChips(
                    tags = discovery.tags,
                    accent = discovery.category.accentColor,
                    modifier = gutter,
                )
            }
        }

        // Keeping, passing on and objecting, in one row. Reporting used to be a quiet line of
        // its own under the colophon; it is a square beside share now because that is where it
        // was asked for, and the two do belong together — they are the only actions on this
        // page that hand the record to something outside the app.
        item {
            SaveRow(
                isFavorite = discovery.isFavorite,
                hasReported = state.report != null,
                onToggleFavorite = { onIntent(DiscoveryIntent.ToggleFavorite) },
                onShare = onShare,
                onReport = { onIntent(DiscoveryIntent.StartReport) },
                modifier = Modifier.padding(horizontal = ScreenGutter, vertical = Spacing.xl),
            )
        }

        // Placed with keeping and sharing rather than with the narrative: writing a note is
        // something done *to* the record, once the reading is finished.
        item {
            NoteBlock(
                note = state.note,
                editor = state.noteEditor,
                isSaving = state.isSavingNote,
                language = state.language,
                onCapture = onCapture,
                onOpenPhoto = onOpenPhoto,
                onIntent = onIntent,
                modifier = Modifier.padding(horizontal = ScreenGutter, vertical = Spacing.md),
            )
        }

        if (discovery.nearbySuggestions.isNotEmpty()) {
            item {
                NearbyBlock(
                    suggestions = discovery.nearbySuggestions,
                    modifier = Modifier.padding(horizontal = ScreenGutter, vertical = Spacing.sm),
                )
            }
        }

        if (discovery.suggestedQuestions.isNotEmpty()) {
            item {
                SuggestedQuestions(
                    questions = discovery.suggestedQuestions,
                    onAsk = { onIntent(DiscoveryIntent.AskSuggested(it)) },
                    modifier = Modifier.padding(horizontal = ScreenGutter, vertical = Spacing.xl),
                )
            }
        }

        item {
            DiscoveryFooter(
                discovery = discovery,
                language = state.language,
                modifier = Modifier.padding(horizontal = ScreenGutter, vertical = Spacing.sm),
            )
        }
    }
}

/**
 * The traveller's own photo, with the page's paper edge lapping over its foot.
 *
 * The rounded lip is what makes the rest of the screen read as a sheet laid on top of the shot
 * rather than as a second, unrelated panel below it — and it is the phone's own answer, which
 * is why it stays here. A large window sets the same photograph as a card in the column beside
 * the story, where there is no sheet for an edge to lap over.
 */
@Composable
private fun PhotoHeader(discovery: Discovery) {
    Box(modifier = Modifier.fillMaxWidth().height(HEADER_HEIGHT)) {
        DiscoveryPhoto(discovery = discovery, modifier = Modifier.fillMaxSize())

        // Only the top and bottom edges are darkened: the middle is the photo, and
        // a full scrim over it would flatten the one image the traveller took.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = SCRIM_TOP_ALPHA),
                        SCRIM_CLEAR_STOP to Color.Transparent,
                        1f to Color.Black.copy(alpha = SCRIM_FOOT_ALPHA),
                    ),
                ),
        )

        MatchBadge(
            discovery = discovery,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = ScreenGutter, bottom = SHEET_LIP + Spacing.lg),
        )

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

/**
 * The way into the conversation, floating over the read.
 *
 * Pale rather than a full accent fill: it sits on top of body text for the whole scroll, and a
 * solid vermilion slab there competes with the words underneath it.
 *
 * The phone's alone, and it is the clearest statement of what the two arrangements differ on:
 * this button exists because the guide is a screen away. On a large window the guide is already
 * on the right-hand side of the same screen, so there is nothing for a pill to open.
 */
@Composable
private fun AskPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = Pill,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = PILL_SHADOW,
        tonalElevation = PILL_TONAL,
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
                modifier = Modifier.size(PILL_ICON_DISC).clip(CircleShape).background(Vermilion),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = PaperCream,
                    modifier = Modifier.size(PILL_ICON_SIZE),
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

/** Everything this screen can be showing at once, as one value it can be animated between. */
private enum class DiscoveryPage { ANALYSING, MISSING, VIEWFINDER, PHOTOS, STORY }

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
            (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = OVERLAY_SCALE)) togetherWith
                fadeOut(Motion.exit())

        initialState in OVERLAY_PAGES ->
            fadeIn(Motion.enter()) togetherWith
                (fadeOut(Motion.exit()) + scaleOut(Motion.exit(), targetScale = OVERLAY_SCALE))

        else ->
            fadeIn(Motion.enter(Motion.EMPHASISED_MILLIS)) togetherWith
                fadeOut(Motion.exit(Motion.STANDARD_MILLIS))
    }
    // Every page here fills the frame, so this is not animating a size change so much as
    // declining to clip one of them to the other's bounds while they overlap.
    return transform using SizeTransform(clip = false)
}

/** How tall the photograph runs before the page laps over it. */
private val HEADER_HEIGHT = 320.dp

/**
 * How far the story list stops short of the bottom edge.
 *
 * The room the floating note composer occupies, measured against it rather than
 * chosen from the spacing scale — it is a position, not a gap, and snapping it onto a
 * 4 dp step would put the last note under the field used to write it.
 */
private val COMPOSER_CLEARANCE = 132.dp

/** How far the paper sheet laps over the foot of the photo. */
private val SHEET_LIP = 26.dp

/** The photo's own three-stop gradient: dark at the head for the controls, at the foot for the badge. */
private const val SCRIM_TOP_ALPHA = 0.42f
private const val SCRIM_FOOT_ALPHA = 0.45f
private const val SCRIM_CLEAR_STOP = 0.35f

/** Slight, for the reasons in [pageTransform] and on the header buttons. */
private const val OVERLAY_SCALE = 0.94f
private const val POP_SCALE = 0.8f

private val PILL_ICON_DISC = 36.dp
private val PILL_ICON_SIZE = 18.dp
private val PILL_SHADOW = 8.dp
private val PILL_TONAL = 3.dp
