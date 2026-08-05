package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The two stitched marks the app rules its paper with.
 *
 * A field notebook is ruled and its blanks are dashed, and that is the idea behind both the
 * discovery page and the passport — the app's account of a place printed on something the
 * traveller then writes on, and a record stamped on a page with a stitched edge. A Material
 * divider and a solid `BorderStroke` say the opposite: finished, and not for you.
 *
 * Drawn rather than declared because `BorderStroke` cannot carry a dash pattern. One file for
 * both marks, because they are one decision: change the dash here and the rule and the blank
 * still agree, which they would not if each screen drew its own.
 *
 * **It lives in the design system rather than in a feature because two features draw it** —
 * the discovery's title block, its footer and its two note blanks, and the passport's province
 * panel, which had a byte-identical private copy of [DashedRule] until the tablet needed the
 * panel in a pane. `LLM.md` §10 puts a composable used by two features here, and this is what
 * that rule is for: the second copy is the one that stops matching.
 *
 * The journal's day header draws a *different* mark and deliberately keeps its own — a tighter
 * dash with a butt cap, marking the seam between a date and its button rather than a line of a
 * notebook. See `DayHeader.kt`.
 */

/** A stitched rule — the ruled line of a field notebook, not a Material divider. */
@Composable
fun DashedRule(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(RULE_HEIGHT)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(RULE_DASH),
        )
    }
}

/**
 * A stitched outline, for the same reason [DashedRule] exists.
 *
 * @param cornerRadius must be the same value as the `clip` on the node it decorates, from
 *   `Corner`. An outline drawn at 20 inside a box clipped at 24 has its corner arcs sliced
 *   off, which is what happened to two discovery cards during the token refactor.
 */
fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    width: Dp = 1.dp,
): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(BORDER_DASH),
        ),
    )
}

/** A hairline, in the only unit a `Canvas` can be given one. */
private val RULE_HEIGHT = 1.dp

/** Dash then gap, in pixels: the rule runs long and open, the blank stitches tighter. */
private val RULE_DASH = floatArrayOf(6f, 10f)
private val BORDER_DASH = floatArrayOf(9f, 9f)
