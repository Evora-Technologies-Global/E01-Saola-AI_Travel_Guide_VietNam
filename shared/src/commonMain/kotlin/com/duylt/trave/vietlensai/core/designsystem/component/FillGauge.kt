package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion

/**
 * How much of a collection is filled in, drawn as a gauge rather than as progress.
 *
 * Two boxes rather than `LinearProgressIndicator`, which since M3 1.3 puts a gap and a
 * stop dot at the leading edge. That is correct for a determinate task — something is
 * running and will finish — and wrong here: nothing is in flight, the bar is a fill
 * level, and the stop dot reads as a target the traveller is falling short of.
 *
 * Shared by the passport and the culture collection, which are the same idea counted
 * along two different axes. They are meant to look like siblings, and the surest way
 * to keep them that way is for there to be one bar rather than two that agree today.
 */
@Composable
fun FillGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Marigold.copy(alpha = GAUGE_TRACK_ALPHA),
    fillColor: Color = Vermilion,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(fillColor),
        )
    }
}

private const val GAUGE_TRACK_ALPHA = 0.32f
