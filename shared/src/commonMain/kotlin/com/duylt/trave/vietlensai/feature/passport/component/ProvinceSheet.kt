package com.duylt.trave.vietlensai.feature.passport.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.DashedRule
import com.duylt.trave.vietlensai.core.designsystem.theme.Corner
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.PageSpacing
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.PassportStamp
import com.duylt.trave.vietlensai.feature.passport.PassportState
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.passport_province_locked
import com.duylt.trave.vietlensai.resources.passport_province_locked_action
import com.duylt.trave.vietlensai.resources.passport_sheet_expand
import org.jetbrains.compose.resources.stringResource

/**
 * Everything the province panel draws, frozen at the moment it was selected.
 *
 * A snapshot rather than a set of lookups so the panel can keep rendering while the selection
 * is being cleared underneath it.
 */
internal data class ProvinceSheetData(
    val stamp: PassportStamp,
    val discoveries: List<Discovery>,
    /** Which number in the collection this province was, or null while it is locked. */
    val unlockOrder: Int?,
)

/** The last province that was open, held through the panel's own dismissal. */
@Composable
internal fun rememberLastSelection(state: PassportState): ProvinceSheetData? {
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

/**
 * One province, laid out as a page of the passport it belongs to.
 *
 * The order is set by where the peek height cuts. First the head — stamp, name, tally — which
 * answers "which province, and have I been?" with nothing dragged. Then, deliberately
 * straddling the fold, the photographs: the bottom edge slices through a row of the
 * traveller's own pictures, and a photograph cut in half is the one signal that needs no
 * chevron and no sentence to explain it. The dates and the footnotes, which reward a second
 * look rather than prompting one, sit under that.
 *
 * A panel whose peek ended on a clean boundary tested as a panel nobody dragged: it looked
 * finished, and the 32 dp handle was doing all the work of saying otherwise.
 *
 * `AnimatedContent` on the province id is what makes tapping across the map feel like one
 * panel rather than a series of them: the frame stays put and its contents cross-fade, so the
 * eye is not asked to re-find the panel after every tap.
 */
@Composable
internal fun ProvinceSheet(
    sheet: ProvinceSheetData,
    language: AppLanguage,
    onExpand: () -> Unit,
    onOpenDiscovery: (String) -> Unit,
    onOpenLens: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = sheet,
        transitionSpec = {
            fadeIn(tween(SHEET_SWAP_MILLIS)) togetherWith fadeOut(tween(SHEET_SWAP_MILLIS))
        },
        contentKey = { it.stamp.province.id },
        label = "provinceSheet",
        modifier = modifier,
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
                .padding(bottom = PageSpacing.listBottom),
        ) {
            ProvinceHead(
                stamp = stamp,
                unlockOrder = data.unlockOrder,
                accent = accent,
                onExpand = onExpand,
            )

            if (stamp.isUnlocked) {
                Spacer(Modifier.height(Spacing.md))
                DiscoveryStrip(discoveries = data.discoveries, onOpen = onOpenDiscovery)
                Spacer(Modifier.height(Spacing.xl))
                DashedRule(
                    color = PassportHairline,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
                )
                VisitRow(stamp = stamp, language = language)
                DashedRule(
                    color = PassportHairline,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
                )
            } else {
                Spacer(Modifier.height(Spacing.md))
                DashedRule(
                    color = PassportHairline,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
                )
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = stringResource(Res.string.passport_province_locked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = ScreenGutter),
                )
                Spacer(Modifier.height(Spacing.lg))
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
 * The panel's top edge: the drag handle, with a hairline drawn along the rounded lip above it.
 *
 * The line is not decoration, it is the whole boundary. This scheme sets `surface` and
 * `background` to the same cream, so an undimmed panel sliding up over the page is literally
 * the same colour as the page behind it — the only thing marking the join was the corner
 * radius, and a rounded corner alone reads as a rendering artefact rather than as an edge.
 * Modal sheets get this for free from their scrim; this one has no scrim by design, and has to
 * draw its own.
 *
 * Gold at the same weight as the map's frame and the hint cards, so the panel joins a
 * vocabulary the screen already speaks instead of introducing a divider of its own. It also
 * carries the dark theme on its own, where the drop shadow is invisible.
 *
 * Riding in the drag handle slot is what puts it on the true top edge — the handle is the
 * first thing inside the sheet's surface — while leaving the default handle, and the
 * expand/collapse semantics attached to it, exactly as Material built them.
 *
 * It shares this file with the panel because it redraws the panel's own corner by hand, and
 * the two have to agree to the pixel — the same argument `LLM.md` §13.3 makes for `Corner`
 * existing as numbers beside `MaterialTheme.shapes`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetEdge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hairline = Marigold.copy(alpha = SHEET_EDGE_ALPHA)
    Box(
        modifier = modifier
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
 * How much of the panel stands above the bottom edge before it is dragged.
 *
 * Measured on a device rather than derived: the drag handle (48), the head band (116) and the
 * gap under it (14) come to 178, so this leaves the bottom edge falling 46 dp into a 104 dp row
 * of photographs. Enough of each picture to be a picture, not enough to be the whole thing.
 *
 * Cutting through content rather than between it is the entire point. A panel that ends on a
 * clean boundary looks complete, and a panel that looks complete is one nobody thinks to drag.
 */
internal val SheetPeekHeight = 224.dp

/** The app's `extraLarge` corner. [SheetEdge] redraws this curve, so it is shared. */
internal val SheetCorner = Corner.extraLarge

/**
 * Deeper than a sheet normally sits, because it is doing work a scrim usually does. On the
 * light scheme this is the only cue that reads at a glance from across the room; the hairline
 * is what makes it exact.
 */
internal val SheetShadow = 16.dp

private val SheetHairline = 1.dp

private const val SHEET_EDGE_ALPHA = 0.45f
private const val SHEET_SWAP_MILLIS = 160
