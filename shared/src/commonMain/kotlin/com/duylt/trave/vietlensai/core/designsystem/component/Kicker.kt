package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.duylt.trave.vietlensai.core.designsystem.theme.StampType

/**
 * Small caps with wide tracking — the label that names a block without shouting.
 *
 * The style itself is [StampType.kicker] and lives in `Type.kt`; this composable is the
 * thing that draws it, and its one piece of behaviour is the uppercasing. The mono, the
 * weight and the tracking used to be decided here, in a component file, which is how the
 * app ended up with two near-copies of them elsewhere — a text style defined where it is
 * drawn is a text style nobody can find.
 */
@Composable
fun Kicker(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    Text(
        text = text.uppercase(),
        style = StampType.kicker,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
