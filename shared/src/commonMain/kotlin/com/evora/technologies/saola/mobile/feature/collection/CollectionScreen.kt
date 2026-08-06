package com.evora.technologies.saola.mobile.feature.collection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.saola.core.designsystem.component.PageHeader
import com.evora.technologies.saola.core.designsystem.component.SovereigntyBanner
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.feature.collection.CollectionIntent
import com.evora.technologies.saola.feature.collection.CollectionState
import com.evora.technologies.saola.feature.collection.CollectionViewModel
import com.evora.technologies.saola.feature.collection.component.CollectionProgress
import com.evora.technologies.saola.feature.collection.component.CollectionViewToggle
import com.evora.technologies.saola.feature.collection.component.EntrySheet
import com.evora.technologies.saola.feature.collection.component.collectionBoard
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.collection_sovereignty_subtitle
import com.evora.technologies.saola.resources.collection_subtitle
import com.evora.technologies.saola.resources.collection_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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
 *
 * **Three tiles across, which is the phone's answer.** On a large window the board is the
 * pane beside the journal's day column and runs five across — see
 * `tablet/feature/collection/CollectionPane.kt`. Everything drawn below comes from
 * `feature/collection/component/`; what this file owns is the page and the number.
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

    // No language threaded through this screen any more. `CatalogAssetSource` resolves
    // each entry's name and hint while parsing the asset, so a tile arrives already in
    // the right language — and it uses the same device language the string table above
    // it was resolved by, which is what stops a heading and its caption disagreeing.

    CollectionScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onOpenLens = onOpenLens,
        onOpenSovereignty = onOpenSovereignty,
        onOpenDiscovery = onOpenDiscovery,
        modifier = modifier,
    )
}

@Composable
private fun CollectionScreen(
    state: CollectionState,
    onIntent: (CollectionIntent) -> Unit,
    onBack: () -> Unit,
    onOpenLens: () -> Unit,
    onOpenSovereignty: () -> Unit,
    onOpenDiscovery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    trailing = {
                        CollectionViewToggle(
                            isGuide = state.isGuide,
                            onToggle = { onIntent(CollectionIntent.ToggleView) },
                        )
                    },
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

            collectionBoard(
                sections = state.collection.sections,
                columns = COLUMNS,
                isGuide = state.isGuide,
                onOpenDiscovery = onOpenDiscovery,
                onShowHint = { id -> onIntent(CollectionIntent.Select(id)) },
            )
        }
    }

    state.selected?.let { entry ->
        EntrySheet(
            entry = entry,
            onDismiss = { onIntent(CollectionIntent.DismissSelection) },
            onOpenLens = onOpenLens,
            onOpenDiscovery = onOpenDiscovery,
        )
    }
}

/**
 * Three across.
 *
 * Four would put a tile at about 70dp on a small phone, which is below the width the two-line
 * Vietnamese names need — "Nhà thờ Công giáo" and "Ruộng bậc thang" would both be truncated on
 * every row.
 */
private const val COLUMNS = 3
