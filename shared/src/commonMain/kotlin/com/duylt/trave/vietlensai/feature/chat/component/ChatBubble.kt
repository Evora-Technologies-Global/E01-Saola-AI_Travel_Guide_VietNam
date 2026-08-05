package com.duylt.trave.vietlensai.feature.chat.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.theme.Corner
import com.duylt.trave.vietlensai.core.designsystem.theme.GuidePalette
import com.duylt.trave.vietlensai.core.designsystem.theme.InkBrown
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.Pill
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.domain.model.ChatMessage
import com.duylt.trave.vietlensai.domain.model.ChatRole
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.discovery_listen
import com.duylt.trave.vietlensai.resources.discovery_stop
import org.jetbrains.compose.resources.stringResource

/**
 * One turn of the conversation — the traveller's question or the guide's answer.
 *
 * @param isSpeaking whether *this* message is the one the engine is reading. The flag is per
 *   message rather than per screen because the row underneath is a toggle: while an answer is
 *   being read its own control says "Stop", and every other answer still says "Listen".
 */
@Composable
internal fun ChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == ChatRole.USER
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = BUBBLE_MAX_WIDTH),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Surface(
                color = if (isUser) Vermilion else GuidePalette.card,
                contentColor = if (isUser) PaperCream else InkBrown,
                border = if (isUser) null else BorderStroke(1.dp, GuidePalette.cardBorder),
                // The app's `large` corner on three sides and its `extraSmall` on the
                // one nearest the speaker: the clipped corner is what points a bubble at
                // whoever said it, and it is the only place these two radii ever meet.
                shape = RoundedCornerShape(
                    topStart = BubbleCorner,
                    topEnd = BubbleCorner,
                    bottomStart = if (isUser) BubbleCorner else BubbleTail,
                    bottomEnd = if (isUser) BubbleTail else BubbleCorner,
                ),
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(
                        horizontal = Spacing.lg,
                        vertical = Spacing.md,
                    ),
                )
            }
            // Only the guide's answers are worth reading aloud.
            if (!isUser) {
                Row(
                    modifier = Modifier
                        .clip(Pill)
                        .clickable(onClick = onSpeak)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isSpeaking) {
                            Icons.Filled.Stop
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = null,
                        tint = Vermilion,
                        modifier = Modifier.size(SPEAK_ICON_SIZE),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(
                            if (isSpeaking) Res.string.discovery_stop else Res.string.discovery_listen,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = Vermilion,
                    )
                }
            }
        }
    }
}

/**
 * How wide a bubble is allowed to grow.
 *
 * A measured position rather than a gap: it is a line length, chosen so a paragraph of the
 * guide's prose breaks at a comfortable measure and so a short question still reads as a
 * bubble rather than as a full-width band. It is deliberately an absolute rather than a
 * fraction — the guide column on a tablet is 352 dp and a phone is 411, and a percentage
 * would set two different measures for the same prose.
 */
private val BUBBLE_MAX_WIDTH = 320.dp

/** Small enough to sit under a bubble as a footnote rather than as a second control. */
private val SPEAK_ICON_SIZE = 16.dp

/**
 * `VietLensShapes.large` and `extraSmall`, as numbers so one bubble can carry both.
 *
 * `internal` rather than private because the thinking card is a bubble too — it stands in the
 * place the answer will occupy, and a placeholder whose corners do not match the thing that
 * replaces it reads as the answer arriving in a different shape.
 */
internal val BubbleCorner = Corner.large
internal val BubbleTail = Corner.extraSmall
