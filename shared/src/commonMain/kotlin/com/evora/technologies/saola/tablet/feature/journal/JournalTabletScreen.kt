package com.evora.technologies.saola.tablet.feature.journal

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.saola.core.designsystem.component.AppSnackbarHost
import com.evora.technologies.saola.core.designsystem.component.PageHeader
import com.evora.technologies.saola.core.designsystem.component.showError
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.PaneWidth
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.core.mvi.CollectEffects
import com.evora.technologies.saola.core.util.rememberLocationPermissionState
import com.evora.technologies.saola.core.util.userMessage
import com.evora.technologies.saola.feature.collection.CollectionIntent
import com.evora.technologies.saola.feature.collection.CollectionState
import com.evora.technologies.saola.feature.collection.CollectionViewModel
import com.evora.technologies.saola.feature.journal.JournalEffect
import com.evora.technologies.saola.feature.journal.JournalIntent
import com.evora.technologies.saola.feature.journal.JournalState
import com.evora.technologies.saola.feature.journal.JournalViewModel
import com.evora.technologies.saola.feature.journal.component.EmptyFavorites
import com.evora.technologies.saola.feature.journal.component.EmptyJournal
import com.evora.technologies.saola.feature.journal.component.FilterChips
import com.evora.technologies.saola.feature.journal.component.LocationPermissionPrompt
import com.evora.technologies.saola.feature.journal.component.PlacesCounter
import com.evora.technologies.saola.feature.journal.component.ProgressRow
import com.evora.technologies.saola.feature.journal.component.journalDays
import com.evora.technologies.saola.feature.passport.PassportIntent
import com.evora.technologies.saola.feature.passport.PassportState
import com.evora.technologies.saola.feature.passport.PassportViewModel
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.collection_title
import com.evora.technologies.saola.resources.journal_collection_progress
import com.evora.technologies.saola.resources.journal_heading
import com.evora.technologies.saola.resources.journal_kicker
import com.evora.technologies.saola.resources.journal_passport_progress
import com.evora.technologies.saola.resources.journal_stats_open_collection
import com.evora.technologies.saola.resources.journal_stats_open_passport
import com.evora.technologies.saola.resources.passport_title
import com.evora.technologies.saola.tablet.feature.collection.CollectionPane
import com.evora.technologies.saola.tablet.feature.passport.PassportPane
import com.evora.technologies.saola.tablet.navigation.TwoPaneScaffold
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

/** Which of the two ways of looking back over the trip fills the pane beside the days. */
enum class JournalPane { PASSPORT, COLLECTION }

/**
 * The days on the left, the map of the country on the right, both at once.
 *
 * The design note says it plainly: *"Journal becomes master–detail: the list of days stays put
 * while the passport map fills the rest."* This is the only screen in the app whose fixed pane
 * sits at the **start** — the day column is the master half and reads left-to-right into the
 * detail, where the lens and the discovery both put their measured pane on the end.
 *
 * **The passport and the collection are both the right pane, and neither is a screen here.**
 * `Destinations.kt` says why they travel together: *"both are ways of looking back over a
 * trip, so putting them side by side is what keeps them from reading as two rival features."*
 * The same argument decides this — one of them in the pane and the other pushed full-screen
 * over the journal would make exactly the rivalry that reasoning exists to avoid.
 *
 * **Switching the pane is state, not navigation, and that is deliberate.** `LLM.md` §7's rule
 * is that a *ViewModel* never carries a navigation flag — and `JournalEffect` has no
 * navigation case at all; the journal has always navigated through the lambdas its route hands
 * it. What changes between passport and collection here is which composable fills one pane of
 * one screen, which is the same kind of decision `DiscoveryTabletScreen` makes with its own
 * page enum. Doing it with a nested `NavHost` would hand the pane its own controller, and a
 * nested controller registers its back callback *after* the root's — so it would take the
 * system back gesture away from the shell. [BackHandler] below is the same behaviour, opted
 * into explicitly and switched off the moment the pane is back on the passport.
 *
 * The three routes that reach this arrangement — journal, passport and collection — differ
 * only in [initialPane]. Both of the latter stay registered in `TabletNavGraph` because the
 * two shells' graphs must compare equal, and because a deep link has to land somewhere on both
 * form factors.
 */
@Composable
fun JournalTabletRoute(
    initialPane: JournalPane,
    onOpenDiscovery: (String) -> Unit,
    onOpenLens: () -> Unit,
    onOpenSovereignty: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = koinViewModel(),
    // The other two panes' ViewModels, resolved here rather than inside the panes for the
    // reason `DiscoveryTabletRoute` resolves the chat's: §5 lets a Route touch a ViewModel and
    // nothing below it, and a pane that reached for its own would be a Route without a
    // destination. Both are scoped to this back-stack entry, so switching the pane back and
    // forth keeps the passport's decoded covers rather than paying for them twice.
    passportViewModel: PassportViewModel = koinViewModel(),
    collectionViewModel: CollectionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val passportState by passportViewModel.state.collectAsStateWithLifecycle()
    val collectionState by collectionViewModel.state.collectAsStateWithLifecycle()
    val locationPermission = rememberLocationPermissionState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // The journal's one failure path, and the same argument as the phone's: the message is
    // resolved from the effect rather than read back out of state, because state has not
    // recomposed yet at the moment this runs. See `LLM.md` §11 row #15.
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is JournalEffect.ShowMessage -> scope.launch {
                snackbarHostState.showError(effect.error.userMessage())
            }
        }
    }

    // No collector for the passport's or the collection's effects, deliberately:
    // `PassportEffect` and `CollectionEffect` are both empty sealed interfaces, and the MVI
    // doc §4 is explicit that a feature with no effects correctly has no collector.

    JournalTabletScreen(
        state = state,
        passportState = passportState,
        collectionState = collectionState,
        today = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        initialPane = initialPane,
        hasLocationPermission = locationPermission.isGranted,
        isPermissionBlocked = locationPermission.isDeniedForGood,
        onIntent = viewModel::onIntent,
        onPassportIntent = passportViewModel::onIntent,
        onCollectionIntent = collectionViewModel::onIntent,
        onRequestPermission = locationPermission::requestOrOpenSettings,
        onOpenDiscovery = onOpenDiscovery,
        onOpenLens = onOpenLens,
        onOpenSovereignty = onOpenSovereignty,
        modifier = modifier,
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AppSnackbarHost(
            snackbarHostState,
            modifier = Modifier.padding(bottom = PageSpacing.listBottom),
        )
    }
}

/**
 * Internal rather than private so an instrumented test can drive the pane switch.
 *
 * The comment on [dayList] below says the hoist is a guard against a future `AnimatedContent`
 * around the panes rather than something today's code needs — which is exactly the kind of
 * claim that decays without a test holding it, so the test needs to be able to name this.
 */
@VisibleForTesting
@Composable
internal fun JournalTabletScreen(
    state: JournalState,
    passportState: PassportState,
    collectionState: CollectionState,
    today: LocalDate,
    initialPane: JournalPane,
    hasLocationPermission: Boolean,
    isPermissionBlocked: Boolean,
    onIntent: (JournalIntent) -> Unit,
    onPassportIntent: (PassportIntent) -> Unit,
    onCollectionIntent: (CollectionIntent) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenDiscovery: (String) -> Unit,
    onOpenLens: () -> Unit,
    onOpenSovereignty: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pane by rememberSaveable(initialPane, stateSaver = JournalPaneSaver) {
        mutableStateOf(initialPane)
    }

    // Hoisted above the pane switch, which is the whole point of it being here rather than
    // inside the day column: the traveller taps "Collection", reads a shelf of tiles, comes
    // back, and the days are still where they left them. A `rememberLazyListState()` down in
    // the column would survive too — the column never leaves composition — but nothing in the
    // file would say so, and the first person to wrap the panes in an `AnimatedContent` would
    // break it without a failing test to notice.
    val dayList = rememberLazyListState()

    // Only while the pane is somewhere other than home. The passport is where this screen
    // starts, so on it the handler is off and back belongs to the shell — which is what keeps
    // the rail's Journal tab from becoming a place the traveller cannot leave.
    BackHandler(enabled = pane != JournalPane.PASSPORT) { pane = JournalPane.PASSPORT }

    if (!hasLocationPermission) {
        // The whole window, not the day column. The journal is the trip placed on a map and
        // the pane beside it *is* that map, so with the permission unanswered there is
        // nothing to put on either side.
        LocationPermissionPrompt(
            isBlocked = isPermissionBlocked,
            onGrant = onRequestPermission,
            modifier = modifier.fillMaxSize().screenInsetsPadding(),
        )
        return
    }

    TwoPaneScaffold(
        fixedPaneWidth = PaneWidth.journalList,
        // The one screen in the app that puts its measured pane first. A list of days is the
        // index and the pane beside it is what the index opens; reversed, the eye would have
        // to cross the detail to reach the thing that chooses it.
        fixedPaneAtStart = true,
        fixedPane = {
            JournalDayPane(
                state = state,
                today = today,
                pane = pane,
                listState = dayList,
                onIntent = onIntent,
                onSelectPane = { pane = it },
                onOpenDiscovery = onOpenDiscovery,
                onOpenLens = onOpenLens,
            )
        },
        flexiblePane = {
            when (pane) {
                JournalPane.PASSPORT -> PassportPane(
                    state = passportState,
                    onIntent = onPassportIntent,
                    onOpenLens = onOpenLens,
                    onOpenSovereignty = onOpenSovereignty,
                    onOpenDiscovery = onOpenDiscovery,
                )

                JournalPane.COLLECTION -> CollectionPane(
                    state = collectionState,
                    onIntent = onCollectionIntent,
                    onOpenLens = onOpenLens,
                    onOpenSovereignty = onOpenSovereignty,
                    onOpenDiscovery = onOpenDiscovery,
                )
            }
        },
        modifier = modifier,
    )
}

/**
 * The master column: the trip's heading, the two ways of looking at it whole, and the days.
 *
 * The two [ProgressRow]s are the phone's own — same mark, same bar, same count — doing a
 * second job here. On a phone a tap on one of them leaves the journal, so there is nothing for
 * a selected state to describe; in a pair beside a pane they are the switch for it, and the
 * `selected` flag is what tells the traveller which of the two they are looking at. Reusing
 * them rather than inventing a two-tab control is the project's constraint working as
 * intended: the tablet re-arranges the phone's components instead of drawing new ones.
 */
@Composable
private fun JournalDayPane(
    state: JournalState,
    today: LocalDate,
    pane: JournalPane,
    listState: LazyListState,
    onIntent: (JournalIntent) -> Unit,
    onSelectPane: (JournalPane) -> Unit,
    onOpenDiscovery: (String) -> Unit,
    onOpenLens: () -> Unit,
) {
    var expandedStories by rememberSaveable { mutableStateOf(listOf<String>()) }
    val visibleDays = state.visibleDays

    if (state.days.isEmpty() && !state.isLoading) {
        EmptyJournal(
            onOpenLens = onOpenLens,
            modifier = Modifier.fillMaxSize().screenInsetsPadding(),
        )
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().screenInsetsPadding(),
        contentPadding = PaddingValues(bottom = PageSpacing.listBottom),
    ) {
        item(key = "header") {
            PageHeader(
                title = stringResource(Res.string.journal_heading),
                kicker = stringResource(Res.string.journal_kicker),
                trailing = { PlacesCounter(state.stats.totalDiscoveries) },
            )
        }

        item(key = "passport") {
            ProgressRow(
                icon = Icons.Outlined.Place,
                title = stringResource(Res.string.passport_title),
                progress = state.passportProgress,
                progressLabel = stringResource(
                    Res.string.journal_passport_progress,
                    state.provincesUnlocked,
                    state.provincesTotal,
                ),
                openLabel = stringResource(Res.string.journal_stats_open_passport),
                onClick = { onSelectPane(JournalPane.PASSPORT) },
                selected = pane == JournalPane.PASSPORT,
            )
        }

        item(key = "collection") {
            ProgressRow(
                icon = Icons.Outlined.GridView,
                title = stringResource(Res.string.collection_title),
                progress = state.collectionProgress,
                progressLabel = stringResource(
                    Res.string.journal_collection_progress,
                    state.itemsCollected,
                    state.itemsTotal,
                ),
                openLabel = stringResource(Res.string.journal_stats_open_collection),
                onClick = { onSelectPane(JournalPane.COLLECTION) },
                modifier = Modifier.padding(top = Spacing.sm),
                selected = pane == JournalPane.COLLECTION,
            )
        }

        item(key = "filters") {
            FilterChips(
                favoritesOnly = state.favoritesOnly,
                onChange = { onIntent(JournalIntent.SetFavoritesOnly(it)) },
            )
        }

        if (visibleDays.isEmpty() && !state.isLoading) {
            item(key = "favorites-empty") { EmptyFavorites() }
        }

        journalDays(
            days = visibleDays,
            language = state.language,
            today = today,
            generatingDate = state.generatingDate,
            expandedStories = expandedStories,
            onToggleStory = { key ->
                expandedStories = if (key in expandedStories) {
                    expandedStories - key
                } else {
                    expandedStories + key
                }
            },
            onGenerate = { date -> onIntent(JournalIntent.GenerateSummary(date)) },
            onOpenDiscovery = onOpenDiscovery,
            onToggleFavorite = { id -> onIntent(JournalIntent.ToggleFavorite(id)) },
        )
    }
}

/**
 * Saved as its own name rather than through the default saver.
 *
 * `rememberSaveable` has to hand the value to a platform store, and what an enum can be
 * written as differs between the two: on Android it rides the bundle as a `Serializable`, and
 * there is no such guarantee on iOS. A `String` is storable everywhere, which is the only
 * property this needs.
 */
private val JournalPaneSaver: Saver<JournalPane, String> = Saver(
    save = { it.name },
    restore = { JournalPane.valueOf(it) },
)
