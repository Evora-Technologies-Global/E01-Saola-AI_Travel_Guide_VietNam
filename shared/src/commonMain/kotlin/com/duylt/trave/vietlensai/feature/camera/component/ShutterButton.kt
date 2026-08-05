package com.duylt.trave.vietlensai.feature.camera.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.theme.InkBrown
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.camera_cancel_timer
import com.duylt.trave.vietlensai.resources.camera_capture
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource

/**
 * The shutter, carrying the face of a Đông Sơn drum: a sunburst around a red centre.
 *
 * This is the button the traveller looks at more than any other, and a plain white
 * circle said nothing about where they were standing — the bronze drums are two and
 * a half thousand years of Vietnamese metalwork, and their face is already a target
 * with a bullseye, which is exactly what a shutter should look like. Drawn rather
 * than shipped as an asset so the rays stay crisp at any density.
 *
 * [SHUTTER_SIZE] is not a parameter, on either form factor. The tablet wireframe draws the
 * drum a little larger inside its 310 dp panel, and taking that measurement would have made
 * the size a caller's choice — at which point the two branches own two shutters that drift
 * apart on the next change. The project's constraint is that the tablet re-arranges the
 * phone's components rather than re-drawing them, and a size parameter is the first step in
 * re-drawing one.
 */
@Composable
internal fun ShutterButton(
    enabled: Boolean,
    counting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        if (counting) Res.string.camera_cancel_timer else Res.string.camera_capture,
    )
    Box(
        modifier = modifier
            .size(SHUTTER_SIZE)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                onClickLabel = label,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            val outer = size.minDimension / 2f

            // Rim, then a dark line, then the face: the gap is what stops the drum
            // from bleeding into the backdrop it sits on.
            drawCircle(color = Marigold, radius = outer, center = centre)
            drawCircle(color = InkBrown, radius = outer - 3.dp.toPx(), center = centre)
            val face = outer - 5.dp.toPx()
            drawCircle(color = PaperCream, radius = face, center = centre)

            // The rays, from the inner ring out to the rim. Kept thin and half
            // transparent: the drum is a decoration on a control, not a picture, and
            // a solid sunburst would fight the red centre for the eye.
            val ray = Marigold.copy(alpha = 0.55f)
            val inner = face * 0.44f
            for (index in 0 until DRUM_RAYS) {
                val angle = index * 2f * PI.toFloat() / DRUM_RAYS
                val direction = Offset(sin(angle), -cos(angle))
                drawLine(
                    color = ray,
                    start = centre + direction * inner,
                    end = centre + direction * (face - 2.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(
                color = ray,
                radius = inner,
                center = centre,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = ray,
                radius = face - 1.dp.toPx(),
                center = centre,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        if (counting) {
            // A stop square, the one shape nobody has to be taught during a countdown.
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(InkBrown),
            )
        } else {
            Box(
                modifier = Modifier.size(DRUM_CENTRE).clip(CircleShape).background(Vermilion),
            )
        }
    }
}

/** The drum: its overall size, its red centre, and how many rays the sunburst has. */
private val SHUTTER_SIZE = 78.dp
private val DRUM_CENTRE = 22.dp
private const val DRUM_RAYS = 16
