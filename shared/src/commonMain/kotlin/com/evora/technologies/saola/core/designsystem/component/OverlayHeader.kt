package com.evora.technologies.saola.core.designsystem.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding

/**
 * How an [OverlayHeader] stays legible over what is behind it.
 *
 * Two treatments rather than one, because "over content" is two different problems. A
 * photograph and a camera feed are dark and unpredictable, and the answer there is white
 * type on a gradient. A map is light, mostly pale, and covered in the map's own labels —
 * a black gradient over it reads as a bruise, and white type on it disappears into the
 * first road name it crosses.
 */
enum class OverlayHeaderStyle {
    /** White on a top-down black gradient. Photographs, the camera, the photo viewer. */
    Scrim,

    /** The heading in a translucent card in scheme colours. The map. */
    Card,

    /**
     * Nothing behind it at all — for content that already darkens its own top edge.
     *
     * The discovery page's photograph carries a three-stop gradient of its own, because
     * only its top and bottom are darkened and the middle is left alone: it is the one
     * picture the traveller took, and a full scrim over it flattens it. A second gradient
     * laid on top would double the first 35% of that and undo the argument.
     * [OverlayIconButton] holds its own legibility, which is what makes this safe.
     */
    Plain,
}

/**
 * The top of an immersive screen: one that draws over a photograph, a camera feed or a
 * map rather than starting a column of its own.
 *
 * ## This one *does* apply the top inset, and that is the point
 *
 * A document screen's header sits in a column whose outermost container took the inset
 * already — see [PageHeader]. An overlay header has no such column: it is positioned
 * against the top edge of a `Box` that is deliberately full-bleed, so if it does not take
 * the inset itself, nothing does.
 *
 * Which is exactly what went wrong. The discovery page reached for `statusBarsPadding()`
 * in five places instead. The app hid the system bars at the time, so that inset was zero —
 * but the notch is a hole in the glass and was still there, so on a cutout phone the page's
 * close and delete, the photo viewer's close, and the note camera's close and flip all sat
 * *under* the camera. The status bar has since come back, which fixes the phone held upright
 * and nothing else: turn it sideways and the cutout is on an edge no bar reports.
 * `screenInsetsPadding()` was written for precisely this and unions the cutout in; putting
 * it inside this component is what stops the next overlay from getting it wrong again.
 *
 * @param subtitle drawn as a [Kicker] under the title — on an overlay it is metadata
 *   ("14 nearby"), never a second sentence.
 * @param busy shows a small spinner beside the subtitle. The screens this component
 *   serves all sit on top of something being fetched, and a header that cannot say
 *   "still working" pushes each of them into hand-rolling the row again.
 * @param trailing the actions at the far end. A slot, not a `List<OverlayAction>`: the
 *   discovery page wraps each of its buttons in its own `AnimatedVisibility` with its own
 *   enter and exit, which no data class could carry — and a `List` parameter is unstable,
 *   which would cost this header its skippability on the two screens that recompose most.
 */
@Composable
fun OverlayHeader(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    style: OverlayHeaderStyle = OverlayHeaderStyle.Scrim,
    busy: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    // Both Scrim and Plain sit on something dark; only Card sits in scheme colours.
    val onDark = style != OverlayHeaderStyle.Card
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (style == OverlayHeaderStyle.Scrim) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = SCRIM_ALPHA), Color.Transparent),
                        ),
                    )
                } else {
                    Modifier
                },
            )
            .screenInsetsPadding()
            // The gutter, like everything else the app floats against an edge —
            // `Dimens.kt` names floating controls specifically. Translation used to
            // inset its glass buttons by 8, which is why its back arrow sat closer to
            // the edge than the discovery's close button one screen away.
            .padding(horizontal = ScreenGutter, vertical = Spacing.sm),
        // Top rather than centre: explore stacks two map controls in its trailing slot,
        // and centring would drop the heading half a control's height down the map every
        // time a second one appeared.
        verticalAlignment = Alignment.Top,
    ) {
        leading?.invoke()

        // Always weighted, even with nothing in it: it is what pushes `trailing` to the
        // far edge on the screens that have buttons at both corners and no title at all.
        Column(modifier = Modifier.weight(1f)) {
            if (title != null || subtitle != null) {
                HeadingBlock(
                    title = title,
                    subtitle = subtitle,
                    onDark = onDark,
                    busy = busy,
                )
            }
        }

        trailing?.invoke(this)
    }
}

/**
 * The title and its kicker — bare over something dark, in a translucent card over a map.
 *
 * The card is a `Surface` rather than a rounded background so it picks up the scheme's
 * own elevation tint: over a map the heading has to read as the app's furniture sitting
 * on somebody else's picture, and a flat panel reads as part of the picture.
 */
@Composable
private fun HeadingBlock(
    title: String?,
    subtitle: String?,
    onDark: Boolean,
    busy: Boolean,
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = if (onDark) {
                Modifier.padding(start = Spacing.sm)
            } else {
                Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
            },
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (onDark) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (subtitle != null) {
                if (title != null) Spacer(Modifier.height(Spacing.xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Kicker(
                        text = subtitle,
                        color = if (onDark) {
                            Color.White.copy(alpha = SUBTITLE_ALPHA)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    if (busy) {
                        Spacer(Modifier.width(Spacing.sm))
                        CircularProgressIndicator(
                            modifier = Modifier.size(SPINNER_SIZE),
                            strokeWidth = SPINNER_STROKE,
                        )
                    }
                }
            }
        }
    }

    if (onDark) {
        content()
    } else {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface.copy(alpha = CARD_ALPHA),
            tonalElevation = CARD_ELEVATION,
            content = content,
        )
    }
}

/**
 * The one back, close, flip and delete affordance on every immersive screen.
 *
 * There used to be four components doing this job: `BackChip` on the pushed document
 * screens, `GlassButton` on translation, this on discovery, and `CloseChip` on the
 * sovereignty statement. Four is what happens when each screen answers "how do I put a
 * button over a photograph?" for itself, and the answers drifted — different discs,
 * different borders, one with a ripple and one without.
 *
 * [BackChip] survives, for [PageHeader] only: a chip on a page in scheme colours and a
 * disc over a photograph are genuinely two things.
 *
 * @param interactionSource exposed because translation's "show the original" is a
 *   press-and-hold read off this button's own press state rather than a second gesture
 *   detector competing with the click for the same finger.
 * @param containerColor overridden only by the sovereignty statement, which is a solid
 *   sheet of lacquer red rather than a photograph: black glass on it would read as a hole
 *   punched in the page. Same disc, same 40 dp target, its own ink — which is the whole
 *   difference between one component with a colour parameter and four components.
 */
@Composable
fun OverlayIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    containerColor: Color = Color.Black.copy(alpha = BUTTON_ALPHA),
    contentColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .size(BUTTON_SIZE)
            .clip(CircleShape)
            .background(containerColor)
            // A hairline of the ink itself, so the disc still has an edge when it lands on
            // a dark part of the photograph and the fill stops separating it from it.
            .border(1.dp, contentColor.copy(alpha = BUTTON_BORDER_ALPHA), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(ICON_SIZE),
        )
    }
}

private const val SCRIM_ALPHA = 0.6f
private const val SUBTITLE_ALPHA = 0.8f
private const val CARD_ALPHA = 0.92f
private const val BUTTON_ALPHA = 0.45f
private const val BUTTON_BORDER_ALPHA = 0.15f
private val CARD_ELEVATION = 2.dp
private val BUTTON_SIZE = 40.dp
private val ICON_SIZE = 20.dp
private val SPINNER_SIZE = 12.dp
private val SPINNER_STROKE = 1.5.dp
