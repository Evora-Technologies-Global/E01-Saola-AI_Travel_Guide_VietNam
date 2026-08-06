package com.evora.technologies.saola.feature.sovereignty.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.Marigold

/**
 * A compass rose reduced to its north point: the mark on an old chart that says which way the
 * map beneath it is oriented.
 *
 * Not the paper seal used further down the page. That one is a stamp of ownership and belongs
 * beside the sentence about where the statement appears; this one opens a map, which is what
 * follows it.
 *
 * Every measurement here is traced by hand on a `Canvas` and is a *position within the mark*
 * rather than a gap between two things — the ring's radius, how far the needle overshoots it,
 * the pip at the centre. They stay literals for the same reason `Corner` exists: a drawing has
 * to agree with itself to the pixel, and there is no token that could say "13 dp from the
 * centre of this particular disc".
 */
@Composable
internal fun CompassMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(MARK_SIZE)
            .clip(CircleShape)
            .background(LacquerChip),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            val ring = RING_RADIUS.toPx()
            drawLine(
                color = Marigold,
                start = Offset(centre.x, centre.y - NEEDLE_REACH.toPx()),
                end = Offset(centre.x, centre.y - ring),
                strokeWidth = STROKE.toPx(),
            )
            drawCircle(
                color = Marigold,
                radius = ring,
                center = centre,
                style = Stroke(width = STROKE.toPx()),
            )
            drawCircle(color = Marigold, radius = PIP_RADIUS.toPx(), center = centre)
        }
    }
}

private val MARK_SIZE = 64.dp
private val RING_RADIUS = 13.dp
private val NEEDLE_REACH = 26.dp
private val PIP_RADIUS = 3.dp
private val STROKE = 1.5.dp
