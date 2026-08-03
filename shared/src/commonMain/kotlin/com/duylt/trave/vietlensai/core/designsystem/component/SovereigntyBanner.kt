package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.sovereignty_open
import com.duylt.trave.vietlensai.resources.sovereignty_read
import com.duylt.trave.vietlensai.resources.sovereignty_seal
import com.duylt.trave.vietlensai.resources.sovereignty_title
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.StampType
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion

/**
 * The statement, kept on the page rather than tucked into an About screen.
 *
 * Fixed vermilion with a hatched ground and a seal: it is a declaration, and a tonal
 * container drawn from the surrounding scheme would read as one more row of app
 * furniture.
 *
 * Shared between the passport and the explore tab rather than owned by either. The
 * passport carries it as the foot of the map; explore carries it above the first
 * suggestion — same object, same colours, so it reads as the app's own voice in both
 * places instead of as two similar banners.
 *
 * @param subtitle a second line under the statement, for surfaces that show it
 *   without the map beside it. Null on the passport, where the map is the context.
 * @param showReadLabel spells the affordance out beside the chevron. On, the banner
 *   is one line and the word has room; off, the subtitle has already taken that
 *   line and a bare chevron is enough.
 */
@Composable
fun SovereigntyBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showReadLabel: Boolean = true,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Vermilion,
        contentColor = PaperCream,
    ) {
        Row(
            modifier = Modifier
                .lacquerHatch()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SovereigntySeal()
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                // One language only: the app already follows the device locale, and a
                // statement printed twice reads as a caption rather than as a claim.
                Text(
                    text = stringResource(Res.string.sovereignty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = PaperCream,
                )
                if (subtitle != null) {
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = PaperCream.copy(alpha = SUBTITLE_ALPHA),
                    )
                }
            }
            Spacer(Modifier.width(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showReadLabel) {
                    Kicker(text = stringResource(Res.string.sovereignty_read), color = Marigold)
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(Res.string.sovereignty_open),
                    tint = Marigold,
                    modifier = Modifier.size(if (showReadLabel) 18.dp else 20.dp),
                )
            }
        }
    }
}

/**
 * A double-ringed stamp, the way a sovereignty mark is printed on a paper map.
 *
 * Shared rather than private to the banner: the statement page repeats it at the
 * foot of the page, and a second seal drawn by hand would drift from this one the
 * first time either was touched.
 */
@Composable
fun SovereigntySeal(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(
        modifier = modifier
            .size(size)
            .border(1.dp, Marigold.copy(alpha = SEAL_OUTER_ALPHA), CircleShape)
            .padding(3.dp)
            .border(1.dp, Marigold, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.sovereignty_seal).uppercase(),
            style = StampType.seal,
            color = Marigold,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The 45° hatching that marks a surface as part of the sovereignty statement.
 *
 * Drawn from left of the box so the first stripe still crosses the top-left corner.
 * Only ever laid over the fixed vermilion, which is why the stroke is a fixed
 * cream rather than a colour role.
 */
fun Modifier.lacquerHatch(alpha: Float = HATCH_ALPHA): Modifier = drawBehind {
    val step = HATCH_STEP_DP.dp.toPx()
    val stroke = HATCH_WIDTH_DP.dp.toPx()
    var x = -size.height
    while (x < size.width) {
        drawLine(
            color = PaperCream.copy(alpha = alpha),
            start = Offset(x, size.height),
            end = Offset(x + size.height, 0f),
            strokeWidth = stroke,
        )
        x += step
    }
}

private const val HATCH_ALPHA = 0.06f
private const val HATCH_STEP_DP = 12f
private const val HATCH_WIDTH_DP = 3f
private const val SEAL_OUTER_ALPHA = 0.55f
private const val SUBTITLE_ALPHA = 0.72f
