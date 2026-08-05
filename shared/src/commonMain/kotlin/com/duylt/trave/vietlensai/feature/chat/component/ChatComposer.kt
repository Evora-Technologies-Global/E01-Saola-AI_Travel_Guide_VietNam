package com.duylt.trave.vietlensai.feature.chat.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.theme.GuidePalette
import com.duylt.trave.vietlensai.core.designsystem.theme.InkBrown
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.feature.chat.ChatIntent
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.chat_input_hint
import com.duylt.trave.vietlensai.resources.chat_send
import org.jetbrains.compose.resources.stringResource

/**
 * Where the traveller writes.
 *
 * Takes the two values it draws rather than the whole `ChatState`: the composer sits at the
 * foot of a thread that grows on every turn, and a parameter carrying the message list would
 * re-run this — with its text field and its focus — each time an answer landed.
 *
 * @param canSend from `ChatState.canSend`, never recomputed here. Whether a question may be
 *   sent is a rule about blank input and a request already in flight, and a screen that works
 *   it out for itself is a screen that will disagree with the ViewModel that enforces it.
 */
@Composable
internal fun ChatComposer(
    input: String,
    canSend: Boolean,
    onIntent: (ChatIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = GuidePalette.composer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { onIntent(ChatIntent.InputChanged(it)) },
                placeholder = {
                    Text(
                        text = stringResource(Res.string.chat_input_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = INPUT_MAX_LINES,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = GuidePalette.page,
                    unfocusedContainerColor = GuidePalette.page,
                    focusedIndicatorColor = Vermilion.copy(alpha = FOCUS_ALPHA),
                    unfocusedIndicatorColor = GuidePalette.hairline,
                    cursorColor = Vermilion,
                    focusedTextColor = InkBrown,
                    unfocusedTextColor = InkBrown,
                    focusedPlaceholderColor = GuidePalette.inkMuted,
                    unfocusedPlaceholderColor = GuidePalette.inkMuted,
                ),
            )
            Spacer(Modifier.width(Spacing.md))
            SendButton(
                enabled = canSend,
                onClick = { onIntent(ChatIntent.SubmitQuestion()) },
            )
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    // Dimmed rather than hidden while there is nothing to send: the control keeps
    // its place in the row, and a tap that would do nothing does not invite one.
    val background = if (enabled) Vermilion else Vermilion.copy(alpha = DISABLED_ALPHA)
    Box(
        modifier = Modifier
            .size(SEND_SIZE)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = stringResource(Res.string.chat_send),
            tint = PaperCream,
            modifier = Modifier.size(SEND_ICON_SIZE),
        )
    }
}

/** Four lines of a question before the field scrolls — past that it is an essay, not a question. */
private const val INPUT_MAX_LINES = 4

private const val FOCUS_ALPHA = 0.45f
private const val DISABLED_ALPHA = 0.35f

/**
 * The send disc, measured against the text field beside it rather than chosen.
 *
 * `OutlinedTextField` at this app's body scale comes out a little over 50 dp tall, and the
 * disc matching it is what keeps the row reading as one control with a button on the end.
 */
private val SEND_SIZE = 52.dp
private val SEND_ICON_SIZE = 22.dp
