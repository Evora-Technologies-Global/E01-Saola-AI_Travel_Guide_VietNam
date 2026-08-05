package com.evora.technologies.saola.feature.collection.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.SectionHeader
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.util.accentColor
import com.evora.technologies.saola.core.util.label
import com.evora.technologies.saola.domain.model.CollectionEntry
import com.evora.technologies.saola.domain.model.CollectionSection
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.collection_section_progress
import org.jetbrains.compose.resources.stringResource

/**
 * The board, section by section, emitted into whichever list is asking.
 *
 * Chunked into rows rather than nested in a `LazyVerticalGrid`, which would need a fixed
 * height inside a `LazyColumn` and so would have to be told how tall sixty-one tiles are.
 *
 * @param columns how many tiles across. The phone draws three and a large window five: the
 *   tile is the same square either way, and what changes is how many of them fit before the
 *   two-line Vietnamese names start truncating. It is a parameter rather than a constant here
 *   because a board is measured against the width it is given, and the pane the tablet gives
 *   it is roughly twice the phone's page.
 */
internal fun LazyListScope.collectionBoard(
    sections: List<CollectionSection>,
    columns: Int,
    onOpenDiscovery: (String) -> Unit,
    onShowHint: (String) -> Unit,
) {
    sections.forEach { section ->
        item(key = "header-${section.category.name}") {
            SectionHeading(section = section)
        }

        items(
            items = section.entries.chunked(columns),
            key = { row -> "row-${row.first().item.id}" },
        ) { row ->
            CollectionRow(
                row = row,
                columns = columns,
                onOpenDiscovery = onOpenDiscovery,
                onShowHint = onShowHint,
            )
        }
    }
}

/** "Ẩm thực · 4/16", in the category's own colour. */
@Composable
private fun SectionHeading(section: CollectionSection) {
    SectionHeader(
        text = section.category.label(),
        color = section.category.accentColor,
        trailing = {
            Text(
                text = stringResource(
                    Res.string.collection_section_progress,
                    section.collectedCount,
                    section.total,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

/** One row of the board, padded out with gaps so a short last row stays left-aligned. */
@Composable
private fun CollectionRow(
    row: List<CollectionEntry>,
    columns: Int,
    onOpenDiscovery: (String) -> Unit,
    onShowHint: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        row.forEach { entry ->
            CollectionTile(
                entry = entry,
                onOpenDiscovery = onOpenDiscovery,
                onShowHint = onShowHint,
                modifier = Modifier.weight(1f),
            )
        }
        repeat(columns - row.size) {
            Spacer(Modifier.weight(1f))
        }
    }
}
