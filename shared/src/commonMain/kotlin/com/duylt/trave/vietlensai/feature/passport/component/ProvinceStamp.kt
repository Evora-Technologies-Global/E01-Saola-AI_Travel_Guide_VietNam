package com.duylt.trave.vietlensai.feature.passport.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.duylt.trave.vietlensai.core.designsystem.theme.FlagRed
import com.duylt.trave.vietlensai.core.designsystem.theme.FlagYellow
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.StampType
import com.duylt.trave.vietlensai.domain.model.PassportStamp
import com.duylt.trave.vietlensai.domain.model.stampLabel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The ink stamp: the one place in the app where the passport metaphor is drawn rather than
 * merely named.
 *
 * Off-square by seven degrees because a stamp pressed by hand never lands straight, and a
 * perfectly axis-aligned one would read as another card. The rotation is a draw transform, so
 * it costs no layout — which is why the box around it carries the padding the tilted corners
 * need.
 */
@Composable
internal fun ProvinceStamp(stamp: PassportStamp, accent: Color, modifier: Modifier = Modifier) {
    val dashes = remember { PathEffect.dashPathEffect(STAMP_DASH) }
    val date = stamp.firstVisitAt
        ?.toLocalDateTime(TimeZone.currentSystemDefault())
        ?.date
        ?.stampLabel()

    Box(
        modifier = modifier
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
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VietnamFlag()
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stamp.province.name.uppercase(),
                style = StampType.ordinal,
                color = accent,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                // An em dash where the date goes: the record keeps its shape whether or
                // not it has been earned, which is what a blank page in a passport looks
                // like — a frame waiting for a date, not an absence.
                text = date ?: "—",
                style = StampType.caption,
                color = accent.copy(alpha = STAMP_DATE_ALPHA),
                maxLines = 1,
            )
        }
    }
}

/**
 * Cờ đỏ sao vàng, where the national emblem sits on a real entry stamp.
 *
 * Drawn rather than shipped as an asset: at this size a raster would need four densities to
 * stay crisp, and the flag is two shapes and two colours.
 *
 * Proportioned to the standard — three by two, and the distance from the star's centre to a
 * point is a fifth of the flag's length, which is what puts the star across three fifths of
 * the height. Getting that ratio wrong is the difference between the flag and something that
 * merely resembles it.
 *
 * It shares this file rather than taking one of its own because it has exactly one caller and
 * is part of the stamp's own drawing: a flag at any other size on any other surface would be a
 * different decision, and there is no such place in the app.
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

/** The stamp's own geometry — a pressed mark, measured rather than stepped. */
private val StampWidth = 108.dp
private val StampHeight = 100.dp
private val StampCorner = 14.dp
private val StampBorder = 2.dp
private val StampInnerBorder = 1.dp
private val StampInnerInset = 5.dp

private val FlagWidth = 24.dp
private val FlagHeight = 16.dp

/** Dash then gap, in pixels: the inner rule of a rubber stamp, tighter than a page rule. */
private val STAMP_DASH = floatArrayOf(9f, 7f)

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
