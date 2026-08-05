package com.duylt.trave.vietlensai.mobile.feature.passport

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.core.designsystem.component.EmptyState
import com.duylt.trave.vietlensai.core.designsystem.component.PageHeader
import com.duylt.trave.vietlensai.core.designsystem.component.SovereigntyBanner
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.feature.passport.PassportIntent
import com.duylt.trave.vietlensai.feature.passport.PassportState
import com.duylt.trave.vietlensai.feature.passport.PassportViewModel
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
import org.koin.compose.viewmodel.koinViewModel

/**
 * The travel passport: Vietnam as a puzzle that fills in with the traveller's own
 * photographs, one province at a time.
 *
 * There is no check-in button anywhere on this screen. Every recognised capture
 * already carries the coordinates it was taken at, so pointing the camera at a
 * temple *is* the check-in — asking someone to confirm afterwards would be a second
 * button for something they just did.
 *
 * The page is laid out as a document rather than under a `TopAppBar`: heading, a
 * progress line, the map in its own framed card, and the sovereignty statement
 * pinned to the foot. The red and gold on it are the fixed brand colours rather than
 * scheme roles — this screen is the app's identity in one view, and it should look
 * the same whatever the wallpaper or the theme.
 *
 * The province panel is a *standard* bottom sheet rather than a modal one. A modal
 * sheet dims the map and swallows its gestures, which on a map is precisely the wrong
 * trade: the traveller taps a province to compare it with the ones around it, and had
 * to dismiss the panel and wait out two animations before they could tap the next.
 * Peeking, undimmed, the panel lets the map stay live and swaps its own contents in
 * place as provinces are tapped.
 *
 * **On a large window the passport is not a screen at all** — it is the pane beside the
 * journal's day column, drawn from the same pieces in `feature/passport/component/`. What
 * this file owns is the phone's answer: a page pushed on top of the journal, with a back
 * chip, because a phone has room for one of the two at a time.
 */
@Composable
fun PassportRoute(
    onBack: () -> Unit,
    onOpenLens: () -> Unit,
    onOpenSovereignty: () -> Unit,
    onOpenDiscovery: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PassportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PassportScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onOpenLens = onOpenLens,
        onOpenSovereignty = onOpenSovereignty,
        onOpenDiscovery = onOpenDiscovery,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassportScreen(
    state: PassportState,
    onIntent: (PassportIntent) -> Unit,
    onBack: () -> Unit,
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

    // Keyed on the scaffold's height as well as on the selection, because a hidden sheet is
    // parked below the *scaffold* rather than below the window and this scaffold changes
    // height once on its own: pushed from the journal, it is first measured with the shell's
    // tab bar still on screen, and `VietLensApp` hands that 80 dp back a frame or two later
    // when the bar has slid away. The sheet does not re-settle onto the anchor that moves
    // with it — left alone it stays where the bottom edge *used to* be, and its drag handle
    // stands 80 dp up a map on which nothing has been selected, over the sovereignty banner.
    //
    // Only visible with animations off, which is why looking at it on a phone never showed
    // it: with them on the bar's height comes back long after the sheet has settled. Off is
    // not a rare state — every AVD ships that way, as does a phone in battery saver.
    //
    // `PassportPane` does not carry this and should not gain it by symmetry: the large window
    // stands its navigation on a rail, so that scaffold is one height. `LLM.md` §11 row #29.
    var scaffoldHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedId, scaffoldHeight) {
        when {
            selectedId == null -> sheetState.hide()
            // Only a panel that was closed is opened at its peek. Moving from one
            // province to the next keeps whatever height the traveller had dragged it
            // to: someone reading photographs is still reading photographs a province
            // later, and snapping the panel shut under them would undo a deliberate
            // gesture on every tap of the map.
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

    BackHandler(enabled = selectedId != null) {
        onIntent(PassportIntent.DismissSelection)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize().onSizeChanged { scaffoldHeight = it.height },
        sheetPeekHeight = SheetPeekHeight,
        // Squared off at the bottom, where the edge is off screen anyway. Built from
        // `SheetCorner` rather than from `MaterialTheme.shapes` because `SheetEdge`
        // traces this same curve by hand and the two have to agree to the pixel.
        sheetShape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
        // Explicit: the scheme leaves `surfaceContainerLow` at the M3 baseline, which
        // is tinted violet and reads as a different app sliding up over the warm cream
        // page.
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
            // Kept for the length of the hide animation. Reading the selection straight
            // out of state would empty the panel the instant a province is dropped, and
            // what slid off the bottom would be a blank card.
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
        // The scaffold's padding is deliberately ignored. Its bottom inset is the peek
        // height, and honouring it would shrink the map — and so re-fit the whole
        // country — every time a province was tapped.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .screenInsetsPadding()
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PageHeader(
                    title = stringResource(Res.string.passport_title),
                    subtitle = stringResource(Res.string.passport_subtitle),
                    onBack = onBack,
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
                                // Someone with captures but no stamps has location
                                // switched off. Telling them to go and discover
                                // something would be advice for a problem they do not
                                // have.
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
}
