package com.evora.technologies.saola.feature.collection.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.CollectionEntry

/**
 * One entry as a square on the board: the face, and its name underneath.
 *
 * Tapping does different things on purpose. A collected tile goes straight to the discovery,
 * because the traveller already knows what it is and wants their own page back. An uncollected
 * one opens the hint, because the only useful thing the app can offer is help finding it. Both
 * of those live on [EntryFace] — this file owns the square and the caption, and nothing else.
 *
 * The board is what the collection *is*: a record, sixty-one photographs deep. What it is not,
 * on its own, is a guide — a name is not enough to spot a thing on a street — which is what
 * [CollectionGuideRow] is for and why the two are one screen with a switch rather than two.
 */
@Composable
internal fun CollectionTile(
    entry: CollectionEntry,
    onOpenDiscovery: (String) -> Unit,
    onShowHint: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        EntryFace(
            entry = entry,
            onOpenDiscovery = onOpenDiscovery,
            onShowHint = onShowHint,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = entry.item.name,
            style = MaterialTheme.typography.labelMedium,
            // Uncollected names are dimmed rather than hidden. Hiding them would make
            // the board a wall of question marks and take away the only thing that
            // tells a traveller what they are supposed to be looking for.
            color = if (entry.isCollected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
