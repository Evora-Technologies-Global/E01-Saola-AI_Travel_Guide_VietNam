package com.duylt.trave.vietlensai.tablet.feature.passport

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.duylt.trave.vietlensai.core.designsystem.component.EmptyState
import com.duylt.trave.vietlensai.core.designsystem.component.PageHeader
import com.duylt.trave.vietlensai.core.designsystem.component.SovereigntyBanner
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.feature.passport.PassportIntent
import com.duylt.trave.vietlensai.feature.passport.PassportState
import com.duylt.trave.vietlensai.feature.passport.component.PassportEmptyHint
import com.duylt.trave.vietlensai.feature.passport.component.PassportMap
import com.duylt.trave.vietlensai.feature.passport.component.PassportProgress
import com.duylt.trave.vietlensai.feature.passport.component.ProvinceSheet
import com.duylt.trave.vietlensai.feature.passport.component.SheetCorner
import com.duylt.trave.vietlensai.feature.passport.component.SheetEdge
import com.duylt.trave.vietlensai.feature.passport.component.SheetPeekHeight
import com.duylt.trave.vietlensai.feature.passport.component.SheetShadow
import com.duylt.trave.vietlensai.feature.passport.component.rememberLastSelection
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.passport_subtitle
import com.duylt.trave.vietlensai.resources.passport_title
import com.duylt.trave.vietlensai.resources.passport_unavailable_body
import com.duylt.trave.vietlensai.resources.passport_unavailable_title
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Vietnam filling in with the traveller's own photographs, beside the days that produced them.
 *
 * The same pieces the phone's passport screen stacks into a page — the progress line, the
 * framed map, the province panel and the sovereignty statement at the foot — arranged into the
 * flexible pane of the journal. What it does **not** have is a back chip: on the phone the
 * passport is pushed on top of the journal and there is a journal to go back to, and here the
 * journal is the column to the left.
 *
 * The province panel stays a *standard* bottom sheet rather than becoming a third pane. Every
 * argument the phone's version makes for it holds harder at this size: the traveller taps a
 * province to compare it with the ones around it, so the map underneath must stay live and
 * undimmed, and the panel must be able to swap its own contents in place. It peeks from the
 * bottom of the pane rather than the window, which is what a `BottomSheetScaffold` filling one
 * pane does by itself — the sheet is measured against the scaffold it is in, not the display.
 *
 * Stateless, and that is what makes it a pane rather than a route: `JournalTabletRoute` owns
 * the ViewModel, per `LLM.md` §5, because a large window shows two features at once and only
 * one of them can be the destination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PassportPane(
    state: PassportState,
    onIntent: (PassportIntent) -> Unit,
    onOpenLens: () -> Unit,
    onOpenSovereignty: () -> Unit,
    onOpenDiscovery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // `skipHiddenState = false` is what makes "no province selected" expressible at
    // all: a standard sheet otherwise has no state below its peek and would sit on
    // screen permanently, covering the sovereignty banner with an empty panel.
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val scope = rememberCoroutineScope()
    val selectedId = state.selectedProvinceId

    LaunchedEffect(selectedId) {
        when {
            selectedId == null -> sheetState.hide()
            // Only a panel that was closed is opened at its peek. Moving from one province to
            // the next keeps whatever height the traveller had dragged it to.
            sheetState.currentValue == SheetValue.Hidden -> sheetState.partialExpand()
        }
    }

    // The panel can also be dismissed by flinging it off the bottom edge, and the map
    // has to hear about that — otherwise the province keeps its selected outline with
    // nothing open to explain why. `drop(1)` skips the Hidden it starts life in.
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }
            .drop(1)
            .filter { it == SheetValue.Hidden }
            .collect { onIntent(PassportIntent.DismissSelection) }
    }

    // Registered deeper than the journal's own pane handler and so ahead of it: with a
    // province open, back closes the province. With none open this is off, and back is the
    // shell's again.
    BackHandler(enabled = selectedId != null) {
        onIntent(PassportIntent.DismissSelection)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        sheetPeekHeight = SheetPeekHeight,
        // Squared off at the bottom, where the edge is off screen anyway. Built from
        // `SheetCorner` rather than from `MaterialTheme.shapes` because `SheetEdge` traces
        // this same curve by hand and the two have to agree to the pixel.
        sheetShape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShadowElevation = SheetShadow,
        sheetDragHandle = {
            SheetEdge(
                onClick = {
                    scope.launch {
                        if (sheetState.currentValue == SheetValue.Expanded) sheetState.partialExpand()
                        else sheetState.expand()
                    }
                },
            )
        },
        sheetContent = {
            // Kept for the length of the hide animation. Reading the selection straight out of
            // state would empty the panel the instant a province is dropped, and what slid off
            // the bottom would be a blank card.
            val sheet = rememberLastSelection(state)
            if (sheet != null) {
                ProvinceSheet(
                    sheet = sheet,
                    language = state.language,
                    onExpand = { scope.launch { sheetState.expand() } },
                    onOpenDiscovery = onOpenDiscovery,
                    onOpenLens = onOpenLens,
                )
            }
        },
    ) { _ ->
        // The scaffold's padding is deliberately ignored, exactly as on the phone: its bottom
        // inset is the peek height, and honouring it would re-fit the whole country every time
        // a province was tapped.
        Column(modifier = Modifier.fillMaxSize().screenInsetsPadding()) {
            PageHeader(
                title = stringResource(Res.string.passport_title),
                subtitle = stringResource(Res.string.passport_subtitle),
            )

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = Vermilion) }

                state.passport.stamps.isEmpty() -> Box(modifier = Modifier.weight(1f)) {
                    EmptyState(
                        icon = Icons.Outlined.Map,
                        title = stringResource(Res.string.passport_unavailable_title),
                        description = stringResource(Res.string.passport_unavailable_body),
                    )
                }

                else -> {
                    PassportProgress(state.passport)

                    PassportMap(
                        state = state,
                        onSelect = { onIntent(PassportIntent.SelectProvince(it)) },
                        modifier = Modifier.weight(1f),
                    )

                    if (state.passport.unlockedCount == 0) {
                        PassportEmptyHint(
                            noLocation = state.hasDiscoveriesButNoLocation,
                            onOpenLens = onOpenLens,
                        )
                    }
                }
            }

            SovereigntyBanner(
                onClick = onOpenSovereignty,
                modifier = Modifier.padding(
                    start = ScreenGutter,
                    end = ScreenGutter,
                    top = Spacing.md,
                    bottom = Spacing.lg,
                ),
            )
        }
    }
}
