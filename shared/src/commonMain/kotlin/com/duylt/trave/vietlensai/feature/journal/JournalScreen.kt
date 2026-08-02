package com.duylt.trave.vietlensai.feature.journal

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import com.duylt.trave.vietlensai.core.designsystem.component.AppSnackbarHost
import com.duylt.trave.vietlensai.core.designsystem.component.showError
import com.duylt.trave.vietlensai.core.util.toUserMessage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.discovery_favorite_add
import com.duylt.trave.vietlensai.resources.discovery_favorite_remove
import com.duylt.trave.vietlensai.resources.journal_day_write_story
import com.duylt.trave.vietlensai.resources.journal_discoveries_label
import com.duylt.trave.vietlensai.resources.journal_empty_action
import com.duylt.trave.vietlensai.resources.journal_empty_body
import com.duylt.trave.vietlensai.resources.journal_empty_title
import com.duylt.trave.vietlensai.resources.journal_end_of_day_action
import com.duylt.trave.vietlensai.resources.journal_end_of_day_body
import com.duylt.trave.vietlensai.resources.journal_end_of_day_kicker
import com.duylt.trave.vietlensai.resources.journal_end_of_day_title
import com.duylt.trave.vietlensai.resources.journal_favorites_empty_body
import com.duylt.trave.vietlensai.resources.journal_favorites_empty_title
import com.duylt.trave.vietlensai.resources.journal_filter_all
import com.duylt.trave.vietlensai.resources.journal_filter_favorites
import com.duylt.trave.vietlensai.resources.journal_heading
import com.duylt.trave.vietlensai.resources.journal_highlights
import com.duylt.trave.vietlensai.resources.journal_kicker
import com.duylt.trave.vietlensai.resources.collection_title
import com.duylt.trave.vietlensai.resources.journal_collection_progress
import com.duylt.trave.vietlensai.resources.journal_passport_progress
import com.duylt.trave.vietlensai.resources.journal_stats_open_collection
import com.duylt.trave.vietlensai.resources.journal_stats_open_passport
import com.duylt.trave.vietlensai.resources.journal_story_collapse
import com.duylt.trave.vietlensai.resources.journal_story_expand
import com.duylt.trave.vietlensai.resources.journal_summary_generating
import com.duylt.trave.vietlensai.resources.journal_summary_regenerate
import com.duylt.trave.vietlensai.resources.journal_tomorrow
import com.duylt.trave.vietlensai.resources.location_permission_blocked_body
import com.duylt.trave.vietlensai.resources.location_permission_body
import com.duylt.trave.vietlensai.resources.location_permission_grant
import com.duylt.trave.vietlensai.resources.location_permission_title
import com.duylt.trave.vietlensai.resources.passport_title
import com.duylt.trave.vietlensai.resources.permission_open_settings
import com.duylt.trave.vietlensai.core.designsystem.component.EmptyState
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.theme.InkBrown
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.core.util.LocationPermissionState
import com.duylt.trave.vietlensai.core.util.accentColor
import com.duylt.trave.vietlensai.core.util.asJournalHeading
import com.duylt.trave.vietlensai.core.util.formatTime
import com.duylt.trave.vietlensai.core.util.label
import com.duylt.trave.vietlensai.core.util.rememberLocationPermissionState
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.JournalDay
import com.duylt.trave.vietlensai.domain.model.TripSummary
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalRoute(
    onOpenDiscovery: (String) -> Unit,
    onOpenLens: () -> Unit,
    onOpenPassport: () -> Unit,
    onOpenCollection: () -> Unit,
    viewModel: JournalViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val locationPermission = rememberLocationPermissionState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Resolved here rather than inside the collector: `toUserMessage` is a composable,
    // and a non-composable collector cannot call one. Same shape as ExploreRoute.
    val errorMessage = state.error?.toUserMessage()

    // "Write story" is a model call that can fail for the same reasons the lens can — no
    // key, no network, a throttled model — and every one of them arrives as an AppError
    // rather than a throw. Without this collector the spinner on the day header simply
    // stopped and nothing else changed, so the traveller was told nothing at all on the
    // screen that hosts one of the app's headline features.
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is JournalEffect.ShowMessage -> scope.launch {
                    snackbarHostState.showError(errorMessage ?: return@launch)
                }
            }
        }
    }

    // Stored as ISO strings rather than LocalDate so the set survives process death
    // through the default saver — an expanded story should still be open when the
    // traveller comes back from the discovery they tapped out of it.
    var expandedStories by rememberSaveable { mutableStateOf(listOf<String>()) }

    val visibleDays = state.visibleDays

    Column(
        modifier = Modifier
            .fillMaxSize()
            .screenInsetsPadding()
    ) {
        if (!locationPermission.isGranted) {
            LocationPermissionPrompt(permission = locationPermission)
        } else if (state.days.isEmpty() && !state.isLoading) {
            EmptyState(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                title = stringResource(Res.string.journal_empty_title),
                description = stringResource(Res.string.journal_empty_body),
                action = {
                    Button(onClick = onOpenLens) {
                        Text(stringResource(Res.string.journal_empty_action))
                    }
                },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item(key = "header") {
                    JournalHeader(
                        discoveryCount = state.stats.totalDiscoveries
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
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }

                item(key = "filters") {
                    FilterRow(
                        favoritesOnly = state.favoritesOnly,
                        onChange = { viewModel.onIntent(JournalIntent.SetFavoritesOnly(it)) },
                    )
                }

                // The filter emptied a journal that is not itself empty. The chips stay
                // above it rather than an "All" button landing here: the way back is
                // already on screen, and a second one would read as a second action.
                if (visibleDays.isEmpty() && !state.isLoading) {
                    item(key = "favorites-empty") {
                        EmptyState(
                            icon = Icons.Outlined.FavoriteBorder,
                            title = stringResource(Res.string.journal_favorites_empty_title),
                            description = stringResource(Res.string.journal_favorites_empty_body),
                            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp),
                        )
                    }
                }

                visibleDays.forEach { day ->
                    val isToday = day.date == today
                    val isGenerating = state.generatingDate == day.date

                    item(key = "header-${day.date}") {
                        DayHeader(
                            day = day,
                            language = state.language,
                            isToday = isToday,
                            isGenerating = isGenerating,
                            onGenerate = {
                                viewModel.onIntent(JournalIntent.GenerateSummary(day.date))
                            },
                        )
                    }

                    day.summary?.let { summary ->
                        val key = day.date.toString()
                        item(key = "summary-$key") {
                            StoryCard(
                                summary = summary,
                                expanded = key in expandedStories,
                                onToggle = {
                                    expandedStories = if (key in expandedStories) {
                                        expandedStories - key
                                    } else {
                                        expandedStories + key
                                    }
                                },
                            )
                        }
                    }

                    items(day.discoveries, key = { it.id }) { discovery ->
                        DiscoveryCard(
                            discovery = discovery,
                            language = state.language,
                            onClick = { onOpenDiscovery(discovery.id) },
                            onToggleFavorite = {
                                viewModel.onIntent(JournalIntent.ToggleFavorite(discovery.id))
                            },
                        )
                    }

                    // Today only, and only until the story exists: once it does,
                    // the heading's "Rewrite" is the whole affordance and this
                    // panel would be asking twice for something already done.
                    if (isToday && day.summary == null) {
                        item(key = "end-of-day-${day.date}") {
                            EndOfDayCard(
                                findCount = day.discoveries.size,
                                isGenerating = isGenerating,
                                onWrite = {
                                    viewModel.onIntent(JournalIntent.GenerateSummary(day.date))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Placed last so it draws over the day list, and lifted clear of the bottom bar —
    // the same placement ExploreRoute uses for the same reason.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AppSnackbarHost(snackbarHostState, modifier = Modifier.padding(bottom = 96.dp))
    }
}

/**
 * The whole screen, standing in for a journal that cannot be stamped yet.
 *
 * It leads with what the traveller gets — a province filling in on their passport —
 * rather than with what the app wants, because the system dialog that follows says
 * "allow access to this device's location" and nothing about why. Once Android stops
 * asking, the same block turns into the way to app settings: the only door left.
 */
@Composable
private fun LocationPermissionPrompt(permission: LocationPermissionState) {
    EmptyState(
        icon = Icons.Outlined.Place,
        title = stringResource(Res.string.location_permission_title),
        description = stringResource(
            if (permission.isDeniedForGood) {
                Res.string.location_permission_blocked_body
            } else {
                Res.string.location_permission_body
            },
        ),
        action = {
            Button(onClick = permission::requestOrOpenSettings) {
                Text(
                    stringResource(
                        if (permission.isDeniedForGood) {
                            Res.string.permission_open_settings
                        } else {
                            Res.string.location_permission_grant
                        },
                    ),
                )
            }
        },
    )
}

@Composable
private fun JournalHeader(discoveryCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenGutter, end = ScreenGutter, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Kicker(
                text = stringResource(Res.string.journal_kicker),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.journal_heading),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = discoveryCount.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Kicker(
                text = pluralStringResource(Res.plurals.journal_discoveries_label, discoveryCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A way of looking at the whole trip, as one row: a mark, a bar, and the count.
 *
 * These sit above the days because they are the only things on this screen that show
 * the trip as a whole — everything below them is one day at a time. There are two, the
 * passport and the collection, and they share a shape deliberately: they answer the
 * same question along different axes, where the traveller has been and what they have
 * found, and drawing them as siblings is what keeps them from reading as rivals.
 */
@Composable
private fun ProgressRow(
    icon: ImageVector,
    title: String,
    progress: Float,
    progressLabel: String,
    openLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(targetValue = progress, label = "rowProgress")

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { animated },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = progressLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = openLabel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun FilterRow(favoritesOnly: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilterChip(
            selected = !favoritesOnly,
            onClick = { onChange(false) },
            label = { Text(stringResource(Res.string.journal_filter_all)) },
            shape = RoundedCornerShape(50),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
            ),
        )
        FilterChip(
            selected = favoritesOnly,
            onClick = { onChange(true) },
            label = { Text(stringResource(Res.string.journal_filter_favorites)) },
            shape = RoundedCornerShape(50),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
    }
}

/**
 * A date, a dashed rule, and — depending on the day — the way to write its story.
 *
 * Today with no story shows nothing here: the end-of-day panel under its finds is
 * making the same offer, and two buttons for one action read as two actions.
 */
@Composable
private fun DayHeader(
    day: JournalDay,
    language: AppLanguage,
    isToday: Boolean,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
) {
    val hasStory = day.summary != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenGutter, end = ScreenGutter, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = day.date.asJournalHeading(language),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        DashedRule(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        when {
            isGenerating -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )

            hasStory -> OutlinedButton(
                onClick = onGenerate,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp),
            ) {
                Text(
                    text = stringResource(Res.string.journal_summary_regenerate),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            !isToday -> Button(
                onClick = onGenerate,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp),
            ) {
                Text(
                    text = stringResource(Res.string.journal_day_write_story),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** The hairline between a date and its button, drawn dashed so it reads as a seam. */
@Composable
private fun DashedRule(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier.height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx())),
        )
    }
}

/**
 * The written day, collapsed to its headline and opening lines.
 *
 * A finished narrative is 100-odd words; left open it would push the day's photos
 * off the screen every time, and the photos are what the traveller scrolls for.
 */
@Composable
private fun StoryCard(summary: TripSummary, expanded: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .animateContentSize()
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = summary.headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) Res.string.journal_story_collapse else Res.string.journal_story_expand,
                    ),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = summary.narrative,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_STORY_LINES,
                overflow = TextOverflow.Ellipsis,
            )

            if (expanded) {
                if (summary.highlights.isNotEmpty()) {
                    StoryList(
                        title = stringResource(Res.string.journal_highlights),
                        items = summary.highlights,
                    )
                }
                if (summary.tomorrowIdeas.isNotEmpty()) {
                    StoryList(
                        title = stringResource(Res.string.journal_tomorrow),
                        items = summary.tomorrowIdeas,
                    )
                }
            }
        }
    }
}

@Composable
private fun StoryList(title: String, items: List<String>) {
    Spacer(Modifier.height(14.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    items.forEach { item ->
        Text(
            text = "• $item",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * The offer to turn today into a story.
 *
 * Fixed ink-and-marigold rather than scheme colours, like the lens controls: this is
 * the one block on a pale page that has to read as a different surface, and a tonal
 * container drawn from the same scheme as the cards around it does not.
 */
@Composable
private fun EndOfDayCard(findCount: Int, isGenerating: Boolean, onWrite: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = InkBrown,
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Kicker(text = stringResource(Res.string.journal_end_of_day_kicker), color = Marigold)
            Spacer(Modifier.height(12.dp))
            Text(
                text = pluralStringResource(
                    Res.plurals.journal_end_of_day_title,
                    findCount,
                    findCount,
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = PaperCream,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.journal_end_of_day_body),
                style = MaterialTheme.typography.bodyMedium,
                color = PaperCream.copy(alpha = 0.78f),
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onWrite,
                enabled = !isGenerating,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Vermilion,
                    contentColor = PaperCream,
                    disabledContainerColor = Vermilion.copy(alpha = 0.5f),
                    disabledContentColor = PaperCream.copy(alpha = 0.7f),
                ),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PaperCream,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(Res.string.journal_summary_generating),
                        style = MaterialTheme.typography.labelLarge,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.journal_end_of_day_action),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoveryCard(
    discovery: Discovery,
    language: AppLanguage,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        AppAsyncImage(
            model = discovery.imagePath,
            contentDescription = discovery.title,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = discovery.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = discovery.createdAt.formatTime(language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = discovery.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Kicker(
                text = discovery.category.label(),
                color = discovery.category.accentColor,
            )
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (discovery.isFavorite) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
                contentDescription = stringResource(
                    if (discovery.isFavorite) {
                        Res.string.discovery_favorite_remove
                    } else {
                        Res.string.discovery_favorite_add
                    },
                ),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private const val COLLAPSED_STORY_LINES = 2
