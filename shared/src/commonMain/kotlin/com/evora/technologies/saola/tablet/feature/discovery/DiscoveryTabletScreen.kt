package com.evora.technologies.saola.tablet.feature.discovery

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.saola.core.designsystem.component.AppSnackbarHost
import com.evora.technologies.saola.core.designsystem.component.ErrorState
import com.evora.technologies.saola.core.designsystem.component.LoadingState
import com.evora.technologies.saola.core.designsystem.component.OverlayHeader
import com.evora.technologies.saola.core.designsystem.component.OverlayHeaderStyle
import com.evora.technologies.saola.core.designsystem.component.OverlayIconButton
import com.evora.technologies.saola.core.designsystem.component.PageHeader
import com.evora.technologies.saola.core.designsystem.component.PageHeaderDefaults
import com.evora.technologies.saola.core.designsystem.component.showError
import com.evora.technologies.saola.core.designsystem.theme.GuidePalette
import com.evora.technologies.saola.core.designsystem.theme.InkBrown
import com.evora.technologies.saola.core.designsystem.theme.Motion
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.PaneWidth
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.core.mvi.CollectEffects
import com.evora.technologies.saola.core.util.accentColor
import com.evora.technologies.saola.core.util.rememberCameraPermissionState
import com.evora.technologies.saola.core.util.userMessage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.feature.chat.ChatIntent
import com.evora.technologies.saola.feature.chat.ChatState
import com.evora.technologies.saola.feature.chat.ChatViewModel
import com.evora.technologies.saola.feature.chat.component.ChatComposer
import com.evora.technologies.saola.feature.chat.component.ChatThread
import com.evora.technologies.saola.feature.discovery.DiscoveryEffect
import com.evora.technologies.saola.feature.discovery.DiscoveryIntent
import com.evora.technologies.saola.feature.discovery.DiscoveryState
import com.evora.technologies.saola.feature.discovery.DiscoveryViewModel
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
import com.evora.technologies.saola.feature.discovery.component.SaveRow
import com.evora.technologies.saola.feature.discovery.component.StoryBody
import com.evora.technologies.saola.feature.discovery.component.ThingsToNotice
import com.evora.technologies.saola.feature.discovery.component.rememberLastPresent
import com.evora.technologies.saola.platform.rememberTextSharer
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.action_close
import com.evora.technologies.saola.resources.analysing_title
import com.evora.technologies.saola.resources.chat_about
import com.evora.technologies.saola.resources.chat_clear
import com.evora.technologies.saola.resources.chat_subtitle
import com.evora.technologies.saola.resources.chat_title
import com.evora.technologies.saola.resources.discovery_delete
import com.evora.technologies.saola.resources.discovery_not_found
import com.evora.technologies.saola.resources.discovery_share_body
import com.evora.technologies.saola.tablet.navigation.TwoPaneScaffold
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The story on the left, the guide on the right, both at once.
 *
 * This is the arrangement the tablet design exists for. The note on the wireframe puts it
 * plainly: *"The AI guide is a permanent right-hand column instead of a separate screen — you
 * read the story and question it at the same time."* On a phone those are two screens and a
 * floating pill between them; here the pill is gone because there is nowhere for it to go.
 *
 * Everything drawn below comes from `feature/discovery/component/` and `feature/chat/component/`
 * — the same composables `mobile/` places, in a different order and at a different measure.
 * What this file owns is that order.
 *
 * **Two ViewModels, and only one of them is new.** `DiscoveryViewModel` comes from the
 * back-stack entry as it does on the phone. `ChatViewModel` is asked for by name, because the
 * guide is not a destination here and has no route of its own to read an id from — see its
 * KDoc for why the id is passed rather than inferred.
 *
 * @param discoveryId from the route. Passed down rather than read here so the two ViewModels
 *   cannot end up looking at two different discoveries.
 */
@Composable
fun DiscoveryTabletRoute(
    discoveryId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoveryViewModel = koinViewModel(),
    // Keyed by the discovery, so the guide's thread is tied to the place it is about rather
    // than to whichever route happens to host this pane. The back-stack entry is per discovery
    // today and the default key would also be correct; the point of naming it is that the
    // failure if it ever stops being true is silent — the traveller reads about one temple and
    // is answered about another, with nothing in a log to say so.
    chatViewModel: ChatViewModel = koinViewModel(
        key = discoveryId,
        parameters = { parametersOf(discoveryId) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val chatState by chatViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            DiscoveryEffect.NavigateBack -> onBack()

            // On the phone this navigates to the chat screen. Here the guide is already on
            // screen, so the question is put to it instead — the effect is the ViewModel
            // saying "ask this", and where that lands is the arrangement's business.
            is DiscoveryEffect.OpenChat -> effect.prefill?.let { question ->
                chatViewModel.onIntent(ChatIntent.SubmitQuestion(question))
            }

            // Resolved off the effect's payload rather than out of state, per `LLM.md` §11
            // row #15. Both arrangements say the same thing in the same words; where the
            // notice sits is the only difference between them, which is the whole rule.
            is DiscoveryEffect.ShowMessage -> scope.launch {
                snackbarHostState.showError(effect.error.userMessage())
            }
        }
    }

    // No collector for `chatViewModel.effects`, deliberately. `ChatEffect` is an empty sealed
    // interface — nothing is ever sent — and `docs/android-mvi-best-practices.md` §4 is
    // explicit that the four features with empty effect sets correctly have no collector. A
    // `CollectEffects` here would be a `when` with no branches.

    DiscoveryTabletScreen(
        state = state,
        chatState = chatState,
        onIntent = viewModel::onIntent,
        onChatIntent = chatViewModel::onIntent,
        onBack = onBack,
        modifier = modifier,
    )

    // Centred on the window rather than tucked into the story pane. A failure here belongs to
    // whichever pane the traveller was working in — the note composer is in the story column,
    // the delete is in its header — and a notice pinned inside one of them would be read as
    // being about that column alone. It clears the window's own bottom edge and no bar: the
    // discovery is a detail route on this branch too, so the rail is not drawn.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AppSnackbarHost(
            snackbarHostState,
            modifier = Modifier
                .widthIn(max = PaneWidth.sheet)
                .padding(ScreenGutter)
                .navigationBarsPadding()
                .imePadding(),
        )
    }
}

/**
 * Internal rather than private so an instrumented test can drive the two panes.
 *
 * The same exemption `TranslationScreen` takes, and for a claim of the same shape: what the
 * story pane's scroll position does when the window is taken over and handed back is a fact
 * about *this* composable's `remember` placement, and a probe built out of a `Column` and a
 * `TwoPaneScaffold` would prove only that the probe was written correctly.
 */
@VisibleForTesting
@Composable
internal fun DiscoveryTabletScreen(
    state: DiscoveryState,
    chatState: ChatState,
    onIntent: (DiscoveryIntent) -> Unit,
    onChatIntent: (ChatIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPermission = rememberCameraPermissionState()
    var showPermissionSheet by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var awaitingPermission by remember { mutableStateOf(false) }
    var openPhotos by remember { mutableStateOf<PhotoAlbum?>(null) }

    // Held above the pane for the same reason the phone hoists its list state: the viewfinder
    // and the photo viewer take the whole window, and a scroll position owned by the story
    // column would go back to the top every time one of them closed.
    val storyScroll = rememberScrollState()

    val requestCamera = {
        if (cameraPermission.isGranted) showCamera = true else showPermissionSheet = true
    }

    LaunchedEffect(cameraPermission.isGranted) {
        if (awaitingPermission && cameraPermission.isGranted) {
            awaitingPermission = false
            showCamera = true
        }
    }

    val page = when {
        state.isLoading -> TabletPage.ANALYSING
        state.discovery == null -> TabletPage.MISSING
        showCamera -> TabletPage.VIEWFINDER
        openPhotos != null -> TabletPage.PHOTOS
        else -> TabletPage.PANES
    }

    BackHandler(enabled = openPhotos != null) { openPhotos = null }
    val album = rememberLastPresent(openPhotos)

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = page,
            // The same slight scale the phone uses when something opens over the article, for
            // the same reason: a live viewfinder or the traveller's own photograph pushed
            // around in its first frames reads as the picture failing rather than as motion.
            transitionSpec = {
                (fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = OVERLAY_SCALE))
                    .togetherWith(fadeOut(Motion.exit()))
            },
            modifier = Modifier.fillMaxSize(),
            label = "discoveryTabletPage",
        ) { target ->
            when (target) {
                TabletPage.ANALYSING ->
                    LoadingState(message = stringResource(Res.string.analysing_title))

                TabletPage.MISSING ->
                    ErrorState(message = stringResource(Res.string.discovery_not_found))

                // Full window, both panes covered. Each of these brings its own way out and
                // means something else by "close", so leaving the story visible beside them
                // would offer two ways back that do different things.
                TabletPage.VIEWFINDER -> NoteCameraOverlay(
                    onCaptured = { path ->
                        showCamera = false
                        onIntent(DiscoveryIntent.NotePhotoCaptured(path))
                    },
                    onClose = { showCamera = false },
                )

                TabletPage.PHOTOS -> album?.let { open ->
                    NotePhotoViewer(album = open, onClose = { openPhotos = null })
                }

                TabletPage.PANES -> {
                    val shown = state.discovery ?: return@AnimatedContent
                    TwoPaneScaffold(
                        fixedPaneWidth = PaneWidth.guide,
                        // The guide sits on the trailing edge, opposite the rail. It is the
                        // column being talked *to*, and the story it is about has to be the
                        // thing the eye lands on first.
                        fixedPaneAtStart = false,
                        fixedPane = {
                            GuidePane(
                                title = shown.title,
                                state = chatState,
                                onIntent = onChatIntent,
                            )
                        },
                        flexiblePane = {
                            StoryPane(
                                state = state,
                                discovery = shown,
                                scrollState = storyScroll,
                                onIntent = onIntent,
                                onBack = onBack,
                                onCapture = requestCamera,
                                onOpenPhoto = { paths, index ->
                                    openPhotos = PhotoAlbum(paths, index)
                                },
                            )
                        },
                    )
                }
            }
        }
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
}

/**
 * The article, in the two columns the wireframe sets it in.
 *
 * The picture, the player and the tags go in a fixed [PHOTO_COLUMN] column on the left and the
 * words take the rest. That is not decoration: at 1114 dp of content a single column of body
 * text runs to about 140 characters a line, which is roughly twice a comfortable measure. The
 * left column is what brings the reading back down to a page width, and the things put in it
 * are the three that do not need one.
 *
 * A plain `verticalScroll` rather than a `LazyColumn`: there are five blocks here, not a list,
 * and laziness over five items buys nothing while costing the note composer its ability to be
 * scrolled to while the keyboard is up.
 */
@Composable
private fun StoryPane(
    state: DiscoveryState,
    discovery: Discovery,
    scrollState: ScrollState,
    onIntent: (DiscoveryIntent) -> Unit,
    onBack: () -> Unit,
    onCapture: () -> Unit,
    onOpenPhoto: (paths: List<String>, index: Int) -> Unit,
) {
    val shareText = rememberTextSharer()
    val shareBody = stringResource(
        Res.string.discovery_share_body,
        discovery.title,
        discovery.summary,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        StoryToolbar(
            discovery = discovery,
            onBack = onBack,
            onShare = { shareText(discovery.title, shareBody) },
            onIntent = onIntent,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = ScreenGutter)
                .padding(bottom = PageSpacing.listBottom),
            verticalArrangement = Arrangement.spacedBy(PageSpacing.sectionGap),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                Column(
                    modifier = Modifier.width(PHOTO_COLUMN),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    DiscoveryPhoto(
                        discovery = discovery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PHOTO_HEIGHT)
                            .clip(MaterialTheme.shapes.large),
                    )
                    ListenCard(
                        discovery = discovery,
                        isSpeaking = state.isSpeaking,
                        language = state.language,
                        onToggle = { onIntent(DiscoveryIntent.ToggleSpeech) },
                    )
                    if (discovery.tags.isNotEmpty()) {
                        ContextChips(
                            tags = discovery.tags,
                            accent = discovery.category.accentColor,
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    DiscoveryTitleBlock(discovery = discovery)
                    if (!discovery.isConfident) LowConfidenceNote()
                    StoryBody(discovery = discovery)
                    if (discovery.funFacts.isNotEmpty()) {
                        ThingsToNotice(facts = discovery.funFacts)
                    }
                }
            }

            NoteBlock(
                note = state.note,
                editor = state.noteEditor,
                isSaving = state.isSavingNote,
                language = state.language,
                onCapture = onCapture,
                onOpenPhoto = onOpenPhoto,
                onIntent = onIntent,
            )

            if (discovery.nearbySuggestions.isNotEmpty()) {
                NearbyBlock(suggestions = discovery.nearbySuggestions)
            }

            // `SuggestedQuestions` is deliberately absent. The guide column beside this one
            // already offers the discovery's questions under TRY ASKING, and the same three
            // questions twice on one screen is a page arguing with itself. Asking one still
            // works — `DiscoveryEffect.OpenChat` is routed into the guide.

            DiscoveryFooter(discovery = discovery, language = state.language)
        }
    }
}

/**
 * Close and the reading on the left, what can be done with it on the right.
 *
 * The wireframe puts favouriting and sharing at the top rather than at the foot of the scroll,
 * and on a window this size that is the right call for a reason the phone does not have: the
 * article no longer ends anywhere the traveller reliably reaches, so an action parked after it
 * is an action behind a scroll. It is the same [SaveRow] the phone draws, given a measure
 * instead of a page.
 */
@Composable
private fun StoryToolbar(
    discovery: Discovery,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onIntent: (DiscoveryIntent) -> Unit,
) {
    OverlayHeader(
        // Nothing behind it: this pane is a scheme surface rather than a photograph, so there
        // is no gradient to lay and nothing for one to improve.
        style = OverlayHeaderStyle.Plain,
        leading = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OverlayIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.action_close),
                    onClick = onBack,
                    // The scheme's own ink, which is what this override exists for: the disc
                    // is black glass over a photograph and would be a hole punched in a page.
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                MatchBadge(discovery = discovery)
            }
        },
        trailing = {
            SaveRow(
                isFavorite = discovery.isFavorite,
                onToggleFavorite = { onIntent(DiscoveryIntent.ToggleFavorite) },
                onShare = onShare,
                modifier = Modifier.width(ACTIONS_WIDTH),
            )
            Row(modifier = Modifier.padding(start = Spacing.md)) {
                OverlayIconButton(
                    icon = Icons.Filled.Delete,
                    contentDescription = stringResource(Res.string.discovery_delete),
                    onClick = { onIntent(DiscoveryIntent.RequestDelete) },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    )
}

/**
 * The guide, resident.
 *
 * The same three pieces the phone's chat screen stacks into a `Scaffold` — a `PageHeader`, the
 * thread, the composer — and the same fixed cream palette, because it is the same conversation.
 * What it does not have is a back chip: on the phone the story is a screen behind the chat, and
 * here it is the column to the left. There is nowhere to go back to.
 *
 * The system bar icons are **not** pinned dark as they are on the phone. This column is 352 of
 * 1194 dp and the rest of the window follows the colour scheme, so pinning them to suit the
 * cream would put dark glyphs over a dark rail.
 */
@Composable
private fun GuidePane(
    title: String,
    state: ChatState,
    onIntent: (ChatIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(GuidePalette.page)) {
        Surface(color = GuidePalette.header) {
            PageHeader(
                modifier = Modifier.screenInsetsPadding(),
                title = stringResource(Res.string.chat_title),
                subtitle = stringResource(Res.string.chat_about, title),
                colors = PageHeaderDefaults.colors(
                    title = InkBrown,
                    subtitle = GuidePalette.inkMuted,
                    kicker = GuidePalette.inkMuted,
                    backContainer = GuidePalette.backChip,
                    backContent = InkBrown,
                ),
                trailing = if (state.messages.isNotEmpty()) {
                    {
                        IconButton(onClick = { onIntent(ChatIntent.ClearThread) }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(Res.string.chat_clear),
                                tint = GuidePalette.inkMuted,
                            )
                        }
                    }
                } else {
                    null
                },
            )
        }

        ChatThread(
            state = state,
            onIntent = onIntent,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            // Tighter than the phone's page gutter: 352 dp less two 16 dp gutters is already a
            // narrow measure for a paragraph, and the bubbles carry inner padding of their own.
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.lg),
        )

        ChatComposer(
            input = state.input,
            canSend = state.canSend,
            onIntent = onIntent,
            modifier = Modifier.imePadding(),
        )
    }
}

/** Everything this screen can be showing at once, as one value it can be animated between. */
private enum class TabletPage { ANALYSING, MISSING, VIEWFINDER, PHOTOS, PANES }

/**
 * How wide the picture column runs.
 *
 * A measured position read off the wireframe's 1218 px inner frame: 296 of the 1114 the rail
 * leaves, which is what brings the paragraph column beside it back to a readable measure. Not
 * a [PaneWidth] — that object names the widths `TwoPaneScaffold` divides a *window* into, and
 * this is a column inside one of those panes.
 */
private val PHOTO_COLUMN = 296.dp

/** The photograph's own height in that column, from the same frame. */
private val PHOTO_HEIGHT = 216.dp

/**
 * How much of the toolbar the two actions take.
 *
 * Measured against [SaveRow]'s own parts rather than chosen: the share button is a 56 dp
 * square and the favourite needs room for "Saved to favourites" in Vietnamese and French
 * without wrapping. Wider and it starts competing with the badge on the other end of the row.
 */
private val ACTIONS_WIDTH = 300.dp

/** Slight, for the reason given on the transition above. */
private const val OVERLAY_SCALE = 0.94f
