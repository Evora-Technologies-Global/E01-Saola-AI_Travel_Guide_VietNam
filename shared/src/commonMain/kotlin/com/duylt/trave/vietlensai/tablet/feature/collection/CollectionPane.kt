package com.duylt.trave.vietlensai.tablet.feature.collection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.duylt.trave.vietlensai.core.designsystem.component.PageHeader
import com.duylt.trave.vietlensai.core.designsystem.component.SovereigntyBanner
import com.duylt.trave.vietlensai.core.designsystem.theme.PageSpacing
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.feature.collection.CollectionIntent
import com.duylt.trave.vietlensai.feature.collection.CollectionState
import com.duylt.trave.vietlensai.feature.collection.component.CollectionProgress
import com.duylt.trave.vietlensai.feature.collection.component.EntrySheet
import com.duylt.trave.vietlensai.feature.collection.component.collectionBoard
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.collection_sovereignty_subtitle
import com.duylt.trave.vietlensai.resources.collection_subtitle
import com.duylt.trave.vietlensai.resources.collection_title
import org.jetbrains.compose.resources.stringResource

/**
 * The culture collection, filling the pane the passport usually has.
 *
 * The same board the phone draws, five tiles across instead of three. The pane is roughly
 * twice the phone's page — 1194 less the rail and the day column — so at three the tiles come
 * out over 200 dp square and the board stops reading as a board: a checklist wants to be seen
 * whole, and a grid of nine enormous squares is a gallery.
 *
 * No back chip, for the same reason [PassportPane] has none: this is a pane, not a pushed
 * screen, and a header that offers to go back from one half of a screen reads as if the other
 * half were somewhere else. The pair of `ProgressRow`s in the day column is the switch — it
 * carries the selected outline, so it already says which pane is open — and the system's own
 * back returns to the passport from `JournalTabletScreen`.
 *
 * Stateless: `JournalTabletRoute` owns the ViewModel, per `LLM.md` §5.
 */
@Composable
internal fun CollectionPane(
    state: CollectionState,
    onIntent: (CollectionIntent) -> Unit,
    onOpenLens: () -> Unit,
    onOpenSovereignty: () -> Unit,
    onOpenDiscovery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().screenInsetsPadding()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = PageSpacing.listBottom),
        ) {
            item(key = "header") {
                PageHeader(
                    title = stringResource(Res.string.collection_title),
                    subtitle = stringResource(Res.string.collection_subtitle),
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
 * Five across.
 *
 * Measured against the pane rather than chosen: the wireframe's inner frame is 1218, the rail
 * takes 104 and the day column 392, so the collection has about 718 dp. Less two gutters and
 * four gaps that is a tile a little over 130 dp — the same size the phone's three tiles come
 * out at, which is what keeps the two-line Vietnamese names off the ellipsis.
 */
private const val COLUMNS = 5
