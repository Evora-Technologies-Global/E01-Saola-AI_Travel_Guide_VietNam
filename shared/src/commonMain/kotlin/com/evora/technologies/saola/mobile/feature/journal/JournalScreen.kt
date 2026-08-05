package com.evora.technologies.saola.mobile.feature.journal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.saola.core.designsystem.component.AppSnackbarHost
import com.evora.technologies.saola.core.designsystem.component.PageHeader
import com.evora.technologies.saola.core.designsystem.component.showError
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.core.mvi.CollectEffects
import com.evora.technologies.saola.core.util.rememberLocationPermissionState
import com.evora.technologies.saola.core.util.userMessage
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
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.collection_title
import com.evora.technologies.saola.resources.journal_collection_progress
import com.evora.technologies.saola.resources.journal_heading
import com.evora.technologies.saola.resources.journal_kicker
import com.evora.technologies.saola.resources.journal_passport_progress
import com.evora.technologies.saola.resources.journal_stats_open_collection
import com.evora.technologies.saola.resources.journal_stats_open_passport
import com.evora.technologies.saola.resources.passport_title
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

/**
 * The trip so far, grouped by day, each day able to write its own story.
 *
 * The AI summary is generated on demand rather than automatically: it costs a
 * model call, and a day is only worth summarising once it is actually over. Today
 * asks for it through the end-of-day panel — a day still in progress deserves an
 * invitation rather than a button in a row of dates — while any past day carries a
 * plain "Write story" on its own heading.
 *
 * Without location permission the screen shows nothing but the ask. The journal is
 * the trip placed on a map — the passport row above the days, the province under each
 * find — and half of that is missing before the permission is answered.
 *
 * **One column, and that is the phone's answer rather than the app's.** Everything drawn
 * below comes from `feature/journal/component/`; what this file owns is the order — and in
 * particular that the passport and the collection are two rows the traveller *leaves* the
 * journal through, because there is no room to show either of them here. On a large window
 * they are the pane beside the days and the same two rows become the switch for it. See
 * `tablet/feature/journal/JournalTabletScreen.kt`.
 */
@Composable
fun JournalRoute(
    onOpenDiscovery: (String) -> Unit,
    onOpenLens: () -> Unit,
    onOpenPassport: () -> Unit,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locationPermission = rememberLocationPermissionState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // "Write story" is a model call that can fail for the same reasons the lens can — no
    // key, no network, a throttled model — and every one of them arrives as an AppError
    // rather than a throw. Without this the spinner on the day header simply stopped and
    // nothing else changed, so the traveller was told nothing at all on the screen that
    // hosts one of the app's headline features.
    //
    // The message is resolved from the effect through `userMessage()`, not read out of
    // state. Reading it from state is what silently lost it: the route resolved
    // `state.error?.toUserMessage()` during composition, and by the time the effect was
    // handled — one main-queue turn after `sendEffect`, before the next frame — that value
    // was still the null from before the failure.
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is JournalEffect.ShowMessage -> scope.launch {
                snackbarHostState.showError(effect.error.userMessage())
            }
        }
    }

    JournalScreen(
        state = state,
        today = Clock.System.todayIn(TimeZone.currentSystemDefault()),
        hasLocationPermission = locationPermission.isGranted,
        isPermissionBlocked = locationPermission.isDeniedForGood,
        onIntent = viewModel::onIntent,
        onRequestPermission = locationPermission::requestOrOpenSettings,
        onOpenDiscovery = onOpenDiscovery,
        onOpenLens = onOpenLens,
        onOpenPassport = onOpenPassport,
        onOpenCollection = onOpenCollection,
        modifier = modifier,
    )

    // Placed last so it draws over the day list, and lifted clear of the bottom bar —
    // the same placement ExploreRoute uses for the same reason.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AppSnackbarHost(
            snackbarHostState,
            modifier = Modifier.padding(bottom = PageSpacing.snackbarLift),
        )
    }
}

@Composable
private fun JournalScreen(
    state: JournalState,
    today: LocalDate,
    hasLocationPermission: Boolean,
    isPermissionBlocked: Boolean,
    onIntent: (JournalIntent) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenDiscovery: (String) -> Unit,
    onOpenLens: () -> Unit,
    onOpenPassport: () -> Unit,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Stored as ISO strings rather than LocalDate so the set survives process death
    // through the default saver — an expanded story should still be open when the
    // traveller comes back from the discovery they tapped out of it.
    var expandedStories by rememberSaveable { mutableStateOf(listOf<String>()) }

    val visibleDays = state.visibleDays

    Column(
        modifier = modifier
            .fillMaxSize()
            .screenInsetsPadding()
    ) {
        if (!hasLocationPermission) {
            LocationPermissionPrompt(
                isBlocked = isPermissionBlocked,
                onGrant = onRequestPermission,
            )
        } else if (state.days.isEmpty() && !state.isLoading) {
            EmptyJournal(onOpenLens = onOpenLens)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
                        onClick = onOpenPassport,
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
                        onClick = onOpenCollection,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }

                item(key = "filters") {
                    FilterChips(
                        favoritesOnly = state.favoritesOnly,
                        onChange = { onIntent(JournalIntent.SetFavoritesOnly(it)) },
                    )
                }

                // The filter emptied a journal that is not itself empty. The chips stay
                // above it rather than an "All" button landing here: the way back is
                // already on screen, and a second one would read as a second action.
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
    }
}
