package com.duylt.trave.vietlensai.feature.collection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import com.duylt.trave.vietlensai.core.designsystem.component.FillGauge
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.component.PageHeader
import com.duylt.trave.vietlensai.core.designsystem.component.SectionHeader
import com.duylt.trave.vietlensai.core.designsystem.component.SovereigntyBanner
import com.duylt.trave.vietlensai.core.designsystem.theme.PageSpacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Pill
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.core.util.accentColor
import com.duylt.trave.vietlensai.core.util.label
import com.duylt.trave.vietlensai.core.util.uiLanguage
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.CollectionEntry
import com.duylt.trave.vietlensai.domain.model.CollectionSection
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.collection_collected_badge
import com.duylt.trave.vietlensai.resources.collection_collected_percent
import com.duylt.trave.vietlensai.resources.collection_go_capture
import com.duylt.trave.vietlensai.resources.collection_locked_hint
import com.duylt.trave.vietlensai.resources.collection_open_discovery
import com.duylt.trave.vietlensai.resources.collection_progress_of
import com.duylt.trave.vietlensai.resources.collection_section_progress
import com.duylt.trave.vietlensai.resources.collection_sovereignty_subtitle
import com.duylt.trave.vietlensai.resources.collection_subtitle
import com.duylt.trave.vietlensai.resources.collection_title
import com.duylt.trave.vietlensai.resources.collection_view_capture

/**
 * The culture collection: everything worth finding in Vietnam, and how much of it the
 * traveller has photographed.
 *
 * Reached from the journal, beneath the passport row, and pushed rather than given a
 * tab — for the same reason the passport is. Both are ways of looking back over a
 * trip, so they belong behind the screen that *is* the trip; and a board of sixty-one
 * tiles means nothing until there are photographs to put in it, which is exactly what
 * the journal is a pile of.
 *
 * A tile fills with the traveller's *own* photograph rather than with stock imagery.
 * That is the whole difference between a checklist and a collection, and it is why the
 * catalogue ships names and recognition hints but no pictures at all.
 */
@Composable
fun CollectionRoute(
    onBack: () -> Unit,
    onOpenLens: () -> Unit,
    onOpenSovereignty: () -> Unit,
    onOpenDiscovery: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The catalogue's own language, which is the interface's rather than the guide's:
    // these names are captions under headings that came from the string table, and a
    // traveller narrating in English on a Vietnamese phone must not get one tile in
    // each language.
    val language = uiLanguage()

    Column(
        modifier = modifier
            .fillMaxSize()
            .screenInsetsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = PageSpacing.listBottom),
        ) {
            item(key = "header") {
                PageHeader(
                    title = stringResource(Res.string.collection_title),
                    subtitle = stringResource(Res.string.collection_subtitle),
                    onBack = onBack,
                )
            }

            item(key = "progress") {
                CollectionProgress(
                    collected = state.collection.collectedCount,
                    total = state.collection.total,
                    progress = state.collection.progress,
                )
            }

            item(key = "sovereignty") {
                SovereigntyBanner(
                    onClick = onOpenSovereignty,
                    subtitle = stringResource(Res.string.collection_sovereignty_subtitle),
                    showReadLabel = false,
                    modifier = Modifier.padding(horizontal = ScreenGutter, vertical = Spacing.sm),
                )
            }

            state.collection.sections.forEach { section ->
                item(key = "header-${section.category.name}") {
                    SectionHeading(section = section)
                }

                // Chunked into rows rather than nested in a `LazyVerticalGrid`, which
                // would need a fixed height inside a `LazyColumn` and so would have to
                // be told how tall sixty-one tiles are.
                items(
                    items = section.entries.chunked(COLUMNS),
                    key = { row -> "row-${row.first().item.id}" },
                ) { row ->
                    CollectionRow(
                        row = row,
                        language = language,
                        onOpenDiscovery = onOpenDiscovery,
                        onShowHint = { id -> viewModel.onIntent(CollectionIntent.Select(id)) },
                    )
                }
            }
        }
    }

    state.selected?.let { entry ->
        EntrySheet(
            entry = entry,
            language = language,
            onDismiss = { viewModel.onIntent(CollectionIntent.DismissSelection) },
            onOpenLens = onOpenLens,
            onOpenDiscovery = onOpenDiscovery,
        )
    }
}

/**
 * The count, the share, and the bar — the passport's progress block, counting objects
 * instead of provinces.
 *
 * The big figure is what has been found rather than a percentage, and the percentage
 * is demoted to the kicker on the right. A collection is counted in things: "57 left"
 * is a number someone can act on in an afternoon, where "7% collected" is not.
 */
@Composable
private fun CollectionProgress(collected: Int, total: Int, progress: Float) {
    val animated by animateFloatAsState(targetValue = progress, label = "collectionProgress")
    val percent = (progress * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = collected.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = Vermilion,
            )
            Text(
                text = stringResource(Res.string.collection_progress_of, total),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.sm, bottom = Spacing.xs),
            )
            Spacer(Modifier.weight(1f))
            Kicker(
                text = stringResource(Res.string.collection_collected_percent, percent),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        FillGauge(progress = animated)
    }
}

/** "Ẩm thực · 4/16", in the category's own colour. */
@Composable
private fun SectionHeading(section: CollectionSection) {
    SectionHeader(
        text = section.category.label(),
        color = section.category.accentColor,
        trailing = {
            Text(
                text = stringResource(
                    Res.string.collection_section_progress,
                    section.collectedCount,
                    section.total,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

/** One row of the board, padded out with gaps so a short last row stays left-aligned. */
@Composable
private fun CollectionRow(
    row: List<CollectionEntry>,
    language: AppLanguage,
    onOpenDiscovery: (String) -> Unit,
    onShowHint: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        row.forEach { entry ->
            CollectionTile(
                entry = entry,
                language = language,
                onOpenDiscovery = onOpenDiscovery,
                onShowHint = onShowHint,
                modifier = Modifier.weight(1f),
            )
        }
        repeat(COLUMNS - row.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * One thing to find.
 *
 * Collected and uncollected are deliberately the same silhouette — same square, same
 * corners, same caption underneath — so the board reads as one grid with parts of it
 * filled in, rather than as two different lists interleaved. What changes is only
 * what is inside: a photograph, or the hatching that stands for one not taken yet.
 *
 * Tapping does different things on purpose. A collected tile goes straight to the
 * discovery, because the traveller already knows what it is and wants their own page
 * back. An uncollected one opens the hint, because the only useful thing the app can
 * offer is help finding it.
 */
@Composable
private fun CollectionTile(
    entry: CollectionEntry,
    language: AppLanguage,
    onOpenDiscovery: (String) -> Unit,
    onShowHint: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = entry.item.displayName(language)
    val openLabel = stringResource(Res.string.collection_open_discovery)
    val hintLabel = stringResource(Res.string.collection_locked_hint)

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val discovery = entry.discovery
            if (discovery != null) {
                AppAsyncImage(
                    model = discovery.imagePath,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialTheme.shapes.medium,
                    contentScale = ContentScale.Crop,
                    onClick = { onOpenDiscovery(discovery.id) },
                    onClickLabel = openLabel,
                )
                CollectedBadge(
                    accent = entry.item.category.accentColor,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Spacing.xs),
                )
            } else {
                LockedTile(
                    onClick = { onShowHint(entry.item.id) },
                    onClickLabel = hintLabel,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            // Uncollected names are dimmed rather than hidden. Hiding them would make
            // the board a wall of question marks and take away the only thing that
            // tells a traveller what they are supposed to be looking for.
            color = if (entry.isCollected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The hatched square that stands in for a photograph not taken yet.
 *
 * Hatching rather than a flat grey block, and no padlock: a locked tile would say the
 * app is withholding something, when in fact nothing is locked at all — the traveller
 * simply has not been there. The magnifier reads as "go and look", which is the actual
 * instruction.
 */
@Composable
private fun LockedTile(
    onClick: () -> Unit,
    onClickLabel: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val ground = MaterialTheme.colorScheme.surfaceContainerHigh
    val stripe = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = STRIPE_ALPHA)

    Surface(
        onClick = onClick,
        // `Surface` has no click-label parameter, so the label is put on the node
        // directly — without it a screen reader announces the name and leaves what
        // the tap does to guesswork.
        modifier = modifier.semantics { onClick(label = onClickLabel, action = null) },
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(ground)
                    // 45° hatching, started a full height to the left so the first
                    // stripe still crosses the top-left corner.
                    val step = STRIPE_STEP_DP.dp.toPx()
                    val width = STRIPE_WIDTH_DP.dp.toPx()
                    var x = -size.height
                    while (x < size.width) {
                        drawLine(
                            color = stripe,
                            start = Offset(x, size.height),
                            end = Offset(x + size.height, 0f),
                            strokeWidth = width,
                        )
                        x += step
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ICON_ALPHA),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** A tick in the category's colour, so a filled tile reads as collected at a glance. */
@Composable
private fun CollectedBadge(accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(Pill)
            .background(accent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = stringResource(Res.string.collection_collected_badge),
            tint = Color.White,
            modifier = Modifier.size(13.dp),
        )
    }
}

/**
 * What one entry is, and how to recognise it.
 *
 * The hint is the reason this sheet exists. "Bánh xèo" is a name the traveller can
 * already read off the tile; "bánh tráng mỏng vàng nghệ gập đôi hình bán nguyệt" is
 * what lets them spot one on a street they have never walked down.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntrySheet(
    entry: CollectionEntry,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onOpenLens: () -> Unit,
    onOpenDiscovery: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter)
                .padding(bottom = PageSpacing.listBottom),
        ) {
            Kicker(
                text = entry.item.category.label(),
                color = entry.item.category.accentColor,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = entry.item.displayName(language),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = entry.item.displayHint(language),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xl))

            // The sheet is opened from uncollected tiles only, but a photograph taken
            // while it is up flips the entry underneath it — so it has to be able to
            // offer the discovery as well as the camera.
            val discovery = entry.discovery
            Button(
                onClick = {
                    onDismiss()
                    if (discovery != null) onOpenDiscovery(discovery.id) else onOpenLens()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(
                        if (discovery != null) {
                            Res.string.collection_view_capture
                        } else {
                            Res.string.collection_go_capture
                        },
                    ),
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
    }
}

/**
 * Three across.
 *
 * Four would put a tile at about 70dp on a small phone, which is below the width the
 * two-line Vietnamese names need — "Nhà thờ Công giáo" and "Ruộng bậc thang" would
 * both be truncated on every row.
 */
private const val COLUMNS = 3

private const val STRIPE_ALPHA = 0.11f
private const val STRIPE_STEP_DP = 11f
private const val STRIPE_WIDTH_DP = 4f
private const val ICON_ALPHA = 0.45f
