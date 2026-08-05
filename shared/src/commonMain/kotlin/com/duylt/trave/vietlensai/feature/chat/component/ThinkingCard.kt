package com.duylt.trave.vietlensai.feature.chat.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.theme.GuidePalette
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.chat_thinking
import org.jetbrains.compose.resources.stringResource

/**
 * The guide's bubble before the guide has answered.
 *
 * Drawn as the last row of the thread rather than as a spinner in the composer, so the wait
 * happens where the answer will appear. It carries the guide's own corners and border for the
 * same reason: what replaces it is the same shape in the same place.
 */
@Composable
internal fun ThinkingCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(
            topStart = BubbleCorner,
            topEnd = BubbleCorner,
            bottomStart = BubbleTail,
            bottomEnd = BubbleCorner,
        ),
        color = GuidePalette.card,
        contentColor = GuidePalette.inkMuted,
        border = BorderStroke(1.dp, GuidePalette.cardBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(SPINNER_SIZE),
                strokeWidth = SPINNER_STROKE,
                color = Vermilion,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = stringResource(Res.string.chat_thinking),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Cap height of the label beside it, so the spinner reads as part of the line of type. */
private val SPINNER_SIZE = 14.dp
private val SPINNER_STROKE = 2.dp
