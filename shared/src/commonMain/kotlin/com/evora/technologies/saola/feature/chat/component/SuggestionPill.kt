package com.evora.technologies.saola.feature.chat.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion

/**
 * A question the traveller could ask, offered rather than prescribed.
 *
 * Left-aligned and content-width rather than a full-bleed row, so the three of them read as
 * things somebody might say, not as a settings list. The tablet's guide column is 352 dp and
 * the phone's page is wider, and the pill hugging its own text is what lets the same three
 * questions sit correctly in both without either arrangement re-deciding the shape.
 */
@Composable
internal fun SuggestionPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = Pill,
        color = Color.Transparent,
        contentColor = Vermilion,
        border = BorderStroke(1.dp, Vermilion.copy(alpha = BORDER_ALPHA)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
        )
    }
}

/** Enough edge to read as a button, faint enough not to compete with the guide's answers. */
private const val BORDER_ALPHA = 0.45f
