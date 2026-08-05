package com.duylt.trave.vietlensai.feature.camera.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.domain.model.Discovery

/**
 * One recent capture, as a bordered thumbnail.
 *
 * The single card both arrangements are built from. The phone lays several of them on top
 * of one another into a pile; the tablet, which has a 310 dp column of vertical space the
 * phone never had, puts one at the head of each row of a list. Same photograph, same gold
 * border, same lacquer placeholder — only the [depth] and what is drawn beside it differ.
 *
 * [onClick] is nullable because of that second use: in a list the whole row is the target,
 * name and time included, so the card itself takes no click and the row it sits in owns one.
 *
 * @param depth how far back in a pile this card sits. Zero means it stands alone, which is
 *   both the top of the pile and the only value the list ever passes.
 */
@Composable
internal fun RecentCaptureCard(
    discovery: Discovery,
    depth: Int,
    modifier: Modifier = Modifier,
    clickLabel: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    AppAsyncImage(
        model = discovery.imagePath,
        contentDescription = discovery.title,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            width = 1.5.dp,
            color = Marigold.copy(alpha = 0.9f - depth * 0.15f),
        ),
        // Lacquer red under the photograph rather than black: the pile is what a
        // stack of passport pages looks like in this app, and an image still
        // loading should look like part of the design instead of a hole in it.
        placeholderColor = Vermilion,
        onClick = onClick,
        onClickLabel = clickLabel,
        clickEnabled = enabled,
        modifier = modifier
            // Deeper cards slide left and tilt a little, so the pile fans out just
            // enough to read as several photos instead of a smudged edge.
            .offset(x = -(FAN_STEP * depth))
            .rotate(-FAN_TILT_DEGREES * depth)
            .size(RECENT_CARD_SIZE),
    )
}

/** How far each card behind the top one slides out of the pile. */
private val FAN_STEP = 9.dp

/** How far each card behind the top one tilts, in degrees. */
private const val FAN_TILT_DEGREES = 4f

private val RECENT_CARD_SIZE = 46.dp
