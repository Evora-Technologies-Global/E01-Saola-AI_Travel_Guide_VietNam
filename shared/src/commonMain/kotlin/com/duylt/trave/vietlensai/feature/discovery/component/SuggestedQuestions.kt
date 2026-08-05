package com.duylt.trave.vietlensai.feature.discovery.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.discovery_ask_subtitle
import org.jetbrains.compose.resources.stringResource

/**
 * The three questions the model thinks are worth asking, as full-width rows with an arrow.
 *
 * Rows rather than the guide's own pills, and the arrow is why: on the phone tapping one
 * *leaves* this page for the conversation, and a control that navigates has to look like it
 * navigates. The large-window arrangement does not draw this block at all — its guide column
 * offers the same questions as pills, and the same three questions in two places on one screen
 * is a page arguing with itself.
 */
@Composable
internal fun SuggestedQuestions(
    questions: List<String>,
    onAsk: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Kicker(
            text = stringResource(Res.string.discovery_ask_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        questions.forEach { question ->
            Surface(
                onClick = { onAsk(question) },
                shape = MaterialTheme.shapes.medium,
                color = Color.Transparent,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(ARROW_SIZE),
                    )
                }
            }
        }
    }
}

/** A hint at the end of the line, not a second control competing with the row itself. */
private val ARROW_SIZE = 16.dp
