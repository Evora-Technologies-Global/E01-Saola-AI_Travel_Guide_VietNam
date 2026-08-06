package com.evora.technologies.saola.feature.journal.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.TripSummary
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.journal_highlights
import com.evora.technologies.saola.resources.journal_story_collapse
import com.evora.technologies.saola.resources.journal_story_expand
import com.evora.technologies.saola.resources.journal_tomorrow
import org.jetbrains.compose.resources.stringResource

/**
 * The written day, collapsed to its headline and opening lines.
 *
 * A finished narrative is 100-odd words; left open it would push the day's photos off the
 * screen every time, and the photos are what the traveller scrolls for.
 */
@Composable
internal fun StoryCard(
    summary: TripSummary,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onToggle,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = Spacing.xs),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.lg)
                .animateContentSize()
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = summary.headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) Res.string.journal_story_collapse else Res.string.journal_story_expand,
                    ),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(CHEVRON),
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = summary.narrative,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_STORY_LINES,
                overflow = TextOverflow.Ellipsis,
            )

            if (expanded) {
                if (summary.highlights.isNotEmpty()) {
                    StoryList(
                        title = stringResource(Res.string.journal_highlights),
                        items = summary.highlights,
                    )
                }
                if (summary.tomorrowIdeas.isNotEmpty()) {
                    StoryList(
                        title = stringResource(Res.string.journal_tomorrow),
                        items = summary.tomorrowIdeas,
                    )
                }
            }
        }
    }
}

/**
 * One of the story's two lists, and it shares this file because it has no other caller.
 *
 * Highlights and tomorrow's ideas are the same block seen twice: change the bullet or the
 * gap above the heading and both have to move together, which is the argument `LLM.md` §5
 * makes for a shared file rather than two.
 */
@Composable
private fun StoryList(title: String, items: List<String>) {
    Spacer(Modifier.height(Spacing.md))
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
    items.forEach { item ->
        Text(
            text = "• $item",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

private val CHEVRON = 22.dp

private const val COLLAPSED_STORY_LINES = 2
