package com.duylt.trave.vietlensai.feature.passport

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.core.designsystem.component.AccentChip
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import com.duylt.trave.vietlensai.core.designsystem.component.BackChip
import com.duylt.trave.vietlensai.core.designsystem.component.EmptyState
import com.duylt.trave.vietlensai.core.designsystem.component.FillGauge
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.component.SovereigntyBanner
import com.duylt.trave.vietlensai.core.designsystem.theme.FlagRed
import com.duylt.trave.vietlensai.core.designsystem.theme.FlagYellow
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.core.util.asJournalHeading
import com.duylt.trave.vietlensai.core.util.formatDate
import com.duylt.trave.vietlensai.core.util.stampLabel
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.PassportStamp
import com.duylt.trave.vietlensai.domain.model.TravelPassport
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.passport_administers
import com.duylt.trave.vietlensai.resources.passport_discovery_count
import com.duylt.trave.vietlensai.resources.passport_empty_action
import com.duylt.trave.vietlensai.resources.passport_empty_body
import com.duylt.trave.vietlensai.resources.passport_empty_title
import com.duylt.trave.vietlensai.resources.passport_explored
import com.duylt.trave.vietlensai.resources.passport_merged_from
import com.duylt.trave.vietlensai.resources.passport_no_location_body
import com.duylt.trave.vietlensai.resources.passport_no_location_title
import com.duylt.trave.vietlensai.resources.passport_progress_of
import com.duylt.trave.vietlensai.resources.passport_province_locked
import com.duylt.trave.vietlensai.resources.passport_province_locked_action
import com.duylt.trave.vietlensai.resources.passport_sheet_expand
import com.duylt.trave.vietlensai.resources.passport_sheet_first_visit
import com.duylt.trave.vietlensai.resources.passport_sheet_last_visit
import com.duylt.trave.vietlensai.resources.passport_sheet_no_stamp
import com.duylt.trave.vietlensai.resources.passport_sheet_open_discovery
import com.duylt.trave.vietlensai.resources.passport_sheet_unlock_order
import com.duylt.trave.vietlensai.resources.passport_status_locked
import com.duylt.trave.vietlensai.resources.passport_status_unlocked
import com.duylt.trave.vietlensai.resources.passport_subtitle
import com.duylt.trave.vietlensai.resources.passport_title
import com.duylt.trave.vietlensai.resources.passport_unavailable_body
import com.duylt.trave.vietlensai.resources.passport_unavailable_title
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Instant

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
 */
@OptIn(ExperimentalMaterial3Api::class)
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
            .collect { viewModel.onIntent(PassportIntent.DismissSelection) }
    }

    BackHandler(enabled = selectedId != null) {
        viewModel.onIntent(PassportIntent.DismissSelection)
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        modifier = modifier.fillMaxSize(),
        sheetPeekHeight = SheetPeekHeight,
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
                PassportHeader(onBack = onBack)

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
                            onSelect = { viewModel.onIntent(PassportIntent.SelectProvince(it)) },
                            modifier = Modifier.weight(1f),
                        )

                        if (state.passport.unlockedCount == 0) {
                            EmptyHint(
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
                        top = 14.dp,
                        bottom = 16.dp,
                    ),
                )
            }
        }
    }
}

/**
 * The panel's top edge: the drag handle, with a hairline drawn along the rounded lip
 * above it.
 *
 * The line is not decoration, it is the whole boundary. This scheme sets `surface` and
 * `background` to the same cream, so an undimmed panel sliding up over the page is
 * literally the same colour as the page behind it — the only thing marking the join
 * was the corner radius, and a rounded corner alone reads as a rendering artefact
 * rather than as an edge. Modal sheets get this for free from their scrim; this one
 * has no scrim by design, and has to draw its own.
 *
 * Gold at the same weight as the map's frame and the hint cards, so the panel joins a
 * vocabulary the screen already speaks instead of introducing a divider of its own.
 * It also carries the dark theme on its own, where the drop shadow is invisible.
 *
 * Riding in the drag handle slot is what puts it on the true top edge — the handle is
 * the first thing inside the sheet's surface — while leaving the default handle, and
 * the expand/collapse semantics attached to it, exactly as Material built them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetEdge(onClick: () -> Unit) {
    val hairline = Marigold.copy(alpha = SHEET_EDGE_ALPHA)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // No ripple: this strip is the full width of the panel, and a wash of
            // colour across all of it would be a far larger response than the 32 dp
            // bar the finger was aiming at.
            .clickable(
                interactionSource = null,
                indication = null,
                onClickLabel = stringResource(Res.string.passport_sheet_expand),
                onClick = onClick,
            )
            .drawBehind {
                val stroke = SheetHairline.toPx()
                val half = stroke / 2f
                val radius = SheetCorner.toPx() - half
                // Clipped to the depth of the corner, which is exactly where the curve
                // straightens out. Left to run, the rectangle's sides would trace two
                // vertical lines down the screen edges and then stop in mid-panel;
                // ending them on the tangent is the lip of the sheet and nothing more.
                clipRect(bottom = radius + half) {
                    drawRoundRect(
                        color = hairline,
                        topLeft = Offset(half, half),
                        size = Size(size.width - stroke, size.height * 2),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(width = stroke),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        BottomSheetDefaults.DragHandle()
    }
}

/**
 * Everything the province panel draws, frozen at the moment it was selected.
 *
 * A snapshot rather than a set of lookups so the panel can keep rendering while the
 * selection is being cleared underneath it.
 */
private data class ProvinceSheetData(
    val stamp: PassportStamp,
    val discoveries: List<Discovery>,
    /** Which number in the collection this province was, or null while it is locked. */
    val unlockOrder: Int?,
)

/** The last province that was open, held through the panel's own dismissal. */
@Composable
private fun rememberLastSelection(state: PassportState): ProvinceSheetData? {
    val holder = remember { mutableStateOf<ProvinceSheetData?>(null) }
    val stamp = state.selected
    // Written but not read on this path, so keeping it current invalidates nothing that
    // has already composed; the fallback below is the only reader, and only once the
    // live selection is gone.
    if (stamp != null) {
        val data = ProvinceSheetData(
            stamp = stamp,
            discoveries = state.selectedDiscoveries,
            unlockOrder = state.selectedUnlockOrder,
        )
        holder.value = data
        return data
    }
    return holder.value
}

@Composable
private fun PassportHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenGutter, end = ScreenGutter, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackChip(onClick = onBack)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = stringResource(Res.string.passport_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(Res.string.passport_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The count, the share, and the bar.
 *
 * The right-hand figure is the percentage explored rather than a tally of
 * discoveries: this screen is about ground covered, and the number of photographs
 * behind a province is what its own sheet is for.
 */
@Composable
private fun PassportProgress(passport: TravelPassport) {
    val progress by animateFloatAsState(
        targetValue = passport.progress,
        label = "passportProgress",
    )
    val percent = (passport.progress * 100).toInt()

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = passport.unlockedCount.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Vermilion,
            )
            Text(
                text = stringResource(Res.string.passport_progress_of, passport.totalCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            Kicker(
                text = stringResource(Res.string.passport_explored, percent),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        FillGauge(progress = progress)
    }
}

/**
 * The map in a framed card.
 *
 * The card is a container only — the drawing inside it, its colours and its gestures
 * are untouched. What it buys is a boundary: pinch-zoomed, the country now clearly
 * lives inside a panel instead of running loose between the heading and the banner.
 */
@Composable
private fun PassportMap(
    state: PassportState,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, Marigold.copy(alpha = BORDER_ALPHA), shape)
            .padding(2.dp),
    ) {
        VietnamMapCanvas(
            stamps = state.passport.stamps,
            covers = state.covers,
            selectedProvinceId = state.selectedProvinceId,
            onSelect = onSelect,
            lockedFill = MaterialTheme.colorScheme.surfaceContainerHighest,
            // Not `outlineVariant`: against `surfaceContainerHighest` in
            // this app's own light scheme that measures 1.03:1, so the
            // first-launch map — 34 locked provinces and no photos yet —
            // renders as a single grey silhouette with no internal
            // borders at all, which is exactly the moment the 34 pieces
            // need to be legible.
            lockedStroke = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            unlockedStroke = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            insetLabel = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedStroke = Vermilion,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun EmptyHint(noLocation: Boolean, onOpenLens: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenGutter, end = ScreenGutter, top = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, Marigold.copy(alpha = BORDER_ALPHA), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Kicker(
                text = stringResource(
                    if (noLocation) Res.string.passport_no_location_title
                    else Res.string.passport_empty_title,
                ),
                color = Vermilion,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    if (noLocation) Res.string.passport_no_location_body
                    else Res.string.passport_empty_body,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!noLocation) {
            Spacer(Modifier.width(14.dp))
            LensButton(
                onClick = onOpenLens,
                label = stringResource(Res.string.passport_empty_action)
            )
        }
    }
}

/**
 * One province, laid out as a page of the passport it belongs to.
 *
 * The order is set by where the peek height cuts. First the head — stamp, name, tally
 * — which answers "which province, and have I been?" with nothing dragged. Then,
 * deliberately straddling the fold, the photographs: the bottom edge slices through a
 * row of the traveller's own pictures, and a photograph cut in half is the one signal
 * that needs no chevron and no sentence to explain it. The dates and the footnotes,
 * which reward a second look rather than prompting one, sit under that.
 *
 * A panel whose peek ended on a clean boundary tested as a panel nobody dragged: it
 * looked finished, and the 32 dp handle was doing all the work of saying otherwise.
 *
 * `AnimatedContent` on the province id is what makes tapping across the map feel like
 * one panel rather than a series of them: the frame stays put and its contents cross-
 * fade, so the eye is not asked to re-find the panel after every tap.
 */
@Composable
private fun ProvinceSheet(
    sheet: ProvinceSheetData,
    language: AppLanguage,
    onExpand: () -> Unit,
    onOpenDiscovery: (String) -> Unit,
    onOpenLens: () -> Unit,
) {
    AnimatedContent(
        targetState = sheet,
        transitionSpec = {
            fadeIn(tween(SHEET_SWAP_MILLIS)) togetherWith fadeOut(tween(SHEET_SWAP_MILLIS))
        },
        contentKey = { it.stamp.province.id },
        label = "provinceSheet",
    ) { data ->
        val stamp = data.stamp
        val accent = if (stamp.isUnlocked) Vermilion else MaterialTheme.colorScheme.onSurfaceVariant

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Swallows every tap the panel's own controls did not want.
                //
                // A Material `Surface` with no click of its own is transparent to
                // pointer events, and the sheet's drag gesture only consumes drags —
                // so a tap on any blank part of this panel fell straight through to
                // the map underneath it. Landing on sea or on a province border, that
                // reached the map's own handler as "nothing selected", and the panel
                // the traveller had just tapped dismissed itself.
                //
                // `detectTapGestures` rather than a no-op `clickable`: a click modifier
                // would publish a click action to the accessibility tree, and a screen
                // reader announcing the whole panel as tappable would be a lie.
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
        ) {
            ProvinceHead(
                stamp = stamp,
                unlockOrder = data.unlockOrder,
                accent = accent,
                onExpand = onExpand,
            )

            if (stamp.isUnlocked) {
                Spacer(Modifier.height(14.dp))
                DiscoveryStrip(discoveries = data.discoveries, onOpen = onOpenDiscovery)
                Spacer(Modifier.height(20.dp))
                DashedRule(
                    color = Marigold.copy(alpha = BORDER_ALPHA),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
                )
                VisitRow(stamp = stamp, language = language)
                DashedRule(
                    color = Marigold.copy(alpha = BORDER_ALPHA),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
                )
            } else {
                Spacer(Modifier.height(12.dp))
                DashedRule(
                    color = Marigold.copy(alpha = BORDER_ALPHA),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.passport_province_locked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = ScreenGutter),
                )
                Spacer(Modifier.height(18.dp))
                LensButton(
                    onClick = onOpenLens,
                    label = stringResource(Res.string.passport_province_locked_action),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
                )
            }

            ProvinceFootnotes(stamp = stamp)
        }
    }
}

/**
 * The band the peek height stops at: the stamp, the name, and the tally.
 *
 * Fixed in height whether the province is stamped or not. A locked province used to
 * produce a much shorter panel than a stamped one, so dragging across the map made
 * the sheet jump up and down as much as it changed what it said.
 */
@Composable
private fun ProvinceHead(
    stamp: PassportStamp,
    unlockOrder: Int?,
    accent: Color,
    onExpand: () -> Unit,
) {
    Row(
        // Tappable as well as draggable. Material gives the drag handle expand and
        // collapse actions for accessibility services but no pointer click at all, so
        // until this the only way to open the panel with a finger was to drag a 32 dp
        // bar — discoverable if you already knew, invisible if you did not.
        //
        // The vertical padding is the room the tilt needs: rotation is a draw
        // transform and claims no layout, so without it the stamp's raised corner
        // would be cut off by whatever sits above.
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(Res.string.passport_sheet_expand),
                onClick = onExpand,
            )
            .padding(horizontal = ScreenGutter, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProvinceStamp(stamp = stamp, accent = accent)
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Kicker(
                text = stringResource(
                    if (stamp.isUnlocked) Res.string.passport_status_unlocked
                    else Res.string.passport_status_locked,
                ),
                color = accent,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stamp.province.displayName,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (stamp.isUnlocked) {
                    val tally = pluralStringResource(
                        Res.plurals.passport_discovery_count,
                        stamp.discoveryCount,
                        stamp.discoveryCount,
                    )
                    // The rank is only ever missing for a stamp whose discoveries all
                    // predate the passport and were backfilled without a timestamp.
                    unlockOrder
                        ?.let {
                            "$tally · ${
                                stringResource(
                                    Res.string.passport_sheet_unlock_order,
                                    it
                                )
                            }"
                        }
                        ?: tally
                } else {
                    stringResource(Res.string.passport_sheet_no_stamp)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The ink stamp: the one place in the app where the passport metaphor is drawn rather
 * than merely named.
 *
 * Off-square by seven degrees because a stamp pressed by hand never lands straight,
 * and a perfectly axis-aligned one would read as another card. The rotation is a draw
 * transform, so it costs no layout — which is why the box around it carries the
 * padding the tilted corners need.
 */
@Composable
private fun ProvinceStamp(stamp: PassportStamp, accent: Color) {
    val dashes = remember { PathEffect.dashPathEffect(floatArrayOf(9f, 7f)) }
    val date = stamp.firstVisitAt
        ?.toLocalDateTime(TimeZone.currentSystemDefault())
        ?.date
        ?.stampLabel()

    Box(
        modifier = Modifier
            .size(width = StampWidth, height = StampHeight)
            .rotate(STAMP_TILT_DEGREES)
            .drawBehind {
                val corner = StampCorner.toPx()
                val inset = StampInnerInset.toPx()
                val radius = CornerRadius(corner, corner)
                if (stamp.isUnlocked) {
                    drawRoundRect(
                        color = accent.copy(alpha = STAMP_FILL_ALPHA),
                        cornerRadius = radius
                    )
                }
                drawRoundRect(
                    color = accent,
                    cornerRadius = radius,
                    style = Stroke(width = StampBorder.toPx()),
                )
                // The inner rule is what makes it read as a stamp rather than as a
                // bordered chip — every entry stamp in a real passport has one.
                drawRoundRect(
                    color = accent.copy(alpha = STAMP_INNER_ALPHA),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(corner - inset, corner - inset),
                    style = Stroke(width = StampInnerBorder.toPx(), pathEffect = dashes),
                )
            }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VietnamFlag()
            Spacer(Modifier.height(7.dp))
            Text(
                text = stamp.province.name.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = accent,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                // An em dash where the date goes: the record keeps its shape whether or
                // not it has been earned, which is what a blank page in a passport looks
                // like — a frame waiting for a date, not an absence.
                text = date ?: "—",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = accent.copy(alpha = STAMP_DATE_ALPHA),
                maxLines = 1,
            )
        }
    }
}

/**
 * Cờ đỏ sao vàng, where the national emblem sits on a real entry stamp.
 *
 * Drawn rather than shipped as an asset: at this size a raster would need four
 * densities to stay crisp, and the flag is two shapes and two colours.
 *
 * Proportioned to the standard — three by two, and the distance from the star's centre
 * to a point is a fifth of the flag's length, which is what puts the star across three
 * fifths of the height. Getting that ratio wrong is the difference between the flag and
 * something that merely resembles it.
 */
@Composable
private fun VietnamFlag() {
    Canvas(modifier = Modifier.size(width = FlagWidth, height = FlagHeight)) {
        drawRect(color = FlagRed)
        drawStar(
            center = Offset(size.width / 2f, size.height / 2f),
            outerRadius = size.width * STAR_RADIUS_OF_LENGTH,
            color = FlagYellow,
        )
    }
}

/** A five-pointed star with one point straight up. */
private fun DrawScope.drawStar(center: Offset, outerRadius: Float, color: Color) {
    // The waist of a regular pentagram, and the only value that makes the five points
    // meet at the angles a star is supposed to have.
    val innerRadius = outerRadius * PENTAGRAM_WAIST
    val path = Path()
    repeat(STAR_VERTICES) { i ->
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        // Starts at twelve o'clock and steps by a tenth of a turn, alternating between
        // the points and the valleys between them.
        val angle = -PI.toFloat() / 2f + i * PI.toFloat() / 5f
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

/**
 * When the traveller first arrived, and when they were last here.
 *
 * Two different registers on purpose. The first visit is the record, so it is written
 * out in full with its year; the latest is news, so it is written the way the journal
 * writes it — "Today", "Yesterday", "24 Jul".
 */
@Composable
private fun VisitRow(stamp: PassportStamp, language: AppLanguage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = 14.dp),
    ) {
        VisitCell(
            label = stringResource(Res.string.passport_sheet_first_visit),
            value = stamp.firstVisitAt?.formatDate(language) ?: "—",
            modifier = Modifier.weight(1f),
        )
        VisitCell(
            label = stringResource(Res.string.passport_sheet_last_visit),
            value = stamp.lastVisitAt?.asDayHeading(language) ?: "—",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun VisitCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Kicker(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(5.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Instant.asDayHeading(language: AppLanguage): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).date.asJournalHeading(language)

/**
 * The photographs behind the tally.
 *
 * This strip is the whole reason the panel exists in this form. It used to end on the
 * sentence "4 discoveries here", which stated a number the traveller could not open —
 * the one screen in the app that knew where their photos were and would not show them.
 *
 * Its height is fixed rather than measured from the tiles. The strip now straddles the
 * peek fold, so it is on screen before its query has returned: left to wrap, it would
 * be nothing at all for the first frames after a province is tapped, and the panel
 * would visibly grow under the traveller's eyes as the photographs landed.
 */
@Composable
private fun DiscoveryStrip(discoveries: List<Discovery>, onOpen: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(StripHeight),
        contentPadding = PaddingValues(horizontal = ScreenGutter),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(discoveries, key = { it.id }) { discovery ->
            DiscoveryTile(discovery = discovery, onClick = { onOpen(discovery.id) })
        }
    }
}

@Composable
private fun DiscoveryTile(discovery: Discovery, onClick: () -> Unit) {
    Column(modifier = Modifier.width(TileWidth)) {
        AppAsyncImage(
            model = discovery.imagePath,
            contentDescription = stringResource(
                Res.string.passport_sheet_open_discovery,
                discovery.title,
            ),
            shape = RoundedCornerShape(16.dp),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Spacer(Modifier.height(7.dp))
        Text(
            text = discovery.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The reference notes: what this province used to be called, and what it administers
 * out at sea.
 *
 * Both were above the fold in the previous design, competing with the province's own
 * name. They are footnotes — true, occasionally essential, and never the reason
 * anybody opened the panel.
 */
@Composable
private fun ProvinceFootnotes(stamp: PassportStamp) {
    val merged = stamp.province.mergedFrom
    val archipelagos = stamp.province.archipelagos
    if (merged.isEmpty() && archipelagos.isEmpty()) return

    Spacer(Modifier.height(18.dp))
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter)) {
        // The 2025 reorganisation folded 63 units into 34, so a traveller looking for
        // "Kiên Giang" needs to be told it is part of An Giang now.
        if (merged.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.passport_merged_from, merged.joinToString(" · ")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Only two provinces reach this, and it explains what the inset box on the
        // right belongs to — otherwise those boxes are a map decoration with no
        // stated connection to anything the traveller can collect.
        if (archipelagos.isNotEmpty()) {
            if (merged.isNotEmpty()) Spacer(Modifier.height(10.dp))
            AccentChip(
                text = stringResource(
                    Res.string.passport_administers,
                    archipelagos.joinToString(" · ") { it.vietnameseName },
                ),
                accent = Vermilion,
            )
        }
    }
}

/** A stitched rule — the ruled line of a passport page, not a Material divider. */
@Composable
private fun DashedRule(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f)),
        )
    }
}

/** The one call to action this screen makes, in the brand's own red. */
@Composable
private fun LensButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Vermilion,
            contentColor = PaperCream,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

private const val BORDER_ALPHA = 0.35f

/**
 * How much of the panel stands above the bottom edge before it is dragged.
 *
 * Measured on a device rather than derived: the drag handle (48), the head band (116)
 * and the gap under it (14) come to 178, so this leaves the bottom edge falling 46 dp
 * into a 104 dp row of photographs. Enough of each picture to be a picture, not enough
 * to be the whole thing.
 *
 * Cutting through content rather than between it is the entire point. A panel that
 * ends on a clean boundary looks complete, and a panel that looks complete is one
 * nobody thinks to drag.
 */
private val SheetPeekHeight = 224.dp

private val SheetCorner = 28.dp
private val SheetHairline = 1.dp

/**
 * Deeper than a sheet normally sits, because it is doing work a scrim usually does.
 * On the light scheme this is the only cue that reads at a glance from across the
 * room; the hairline is what makes it exact.
 */
private val SheetShadow = 16.dp

private const val SHEET_EDGE_ALPHA = 0.45f

private val StampWidth = 108.dp
private val StampHeight = 100.dp
private val StampCorner = 14.dp
private val StampBorder = 2.dp
private val StampInnerBorder = 1.dp
private val StampInnerInset = 5.dp
private val TileWidth = 104.dp

/** A square tile, the gap, and two lines of caption — held whether or not both are used. */
private val StripHeight = 147.dp

private val FlagWidth = 24.dp
private val FlagHeight = 16.dp

private const val SHEET_SWAP_MILLIS = 160
private const val STAMP_TILT_DEGREES = -7f

/** Centre to point, as a fraction of the flag's length. The standard's own ratio. */
private const val STAR_RADIUS_OF_LENGTH = 1f / 5f

/** (3 − √5) / 2 — the inner radius of a regular pentagram. */
private const val PENTAGRAM_WAIST = 0.381966f

/** Five points and the five valleys between them. */
private const val STAR_VERTICES = 10
private const val STAMP_FILL_ALPHA = 0.07f
private const val STAMP_INNER_ALPHA = 0.55f
private const val STAMP_DATE_ALPHA = 0.75f
