package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.Spacing

/**
 * The place's tags, with the first one carrying the category's own colour.
 *
 * Only the first: the accent says *what kind of thing this is*, and repeating it across five
 * chips turns a single fact into a wall. A `FlowRow` rather than a scrolling row because these
 * are read, not browsed — and because it is the one block that lays itself out correctly in
 * both a full-width page and a 296 dp column without either arrangement telling it how.
 */
@Composable
internal fun ContextChips(tags: List<String>, accent: Color, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        tags.forEachIndexed { index, tag ->
            val tinted = index == 0
            Text(
                text = tag,
                style = MaterialTheme.typography.bodySmall,
                color = if (tinted) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(Pill)
                    .background(
                        if (tinted) {
                            accent.copy(alpha = TINT_ALPHA)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    )
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
    }
}

/** Enough colour to name the category, faint enough to stay behind the word on it. */
private const val TINT_ALPHA = 0.13f
