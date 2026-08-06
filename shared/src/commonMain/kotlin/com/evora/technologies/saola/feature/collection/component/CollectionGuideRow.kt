package com.evora.technologies.saola.feature.collection.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.CollectionEntry
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.collection_locked_hint
import com.evora.technologies.saola.resources.collection_open_discovery
import org.jetbrains.compose.resources.stringResource

/**
 * One entry as a sentence: the face, the name, and how to recognise the thing.
 *
 * **This is what turns the collection from a record into a guide.** The catalogue has carried a
 * hint for every one of its sixty-one entries since it was written — `CatalogItem.hint` says so
 * in as many words, "the part that makes this a guide rather than a checklist" — and until this
 * row existed the only way to read one was to tap a tile and open a sheet, sixty-one times. A
 * traveller standing on a street they have never walked down does not do that. "Bánh xèo" is a
 * name they can already read off the board; "bánh tráng mỏng vàng nghệ gập đôi hình bán nguyệt"
 * is what lets them spot one.
 *
 * A row rather than a caption under the tile, and the length of the hints is the whole argument:
 * they run 63 to 103 characters in Vietnamese, median 83. Three tiles across on a phone gives
 * each about 121 dp — four or five lines of `bodySmall` — so a hint on the board is either a
 * tile four times as tall as it is wide, or a sentence cut off before it says anything. Given
 * the full width of the page it is two lines.
 *
 * The whole row is the affordance, and [EntryFace] inside it is deliberately not: two nested
 * click regions would give one row two answers, and a screen reader two nodes for one entry.
 * Where the tap goes is the same decision the board makes — a collected entry opens the
 * traveller's own photograph, an uncollected one opens the sheet with the camera button on it.
 */
@Composable
internal fun CollectionGuideRow(
    entry: CollectionEntry,
    onOpenDiscovery: (String) -> Unit,
    onShowHint: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val discovery = entry.discovery
    val action = stringResource(
        if (discovery != null) Res.string.collection_open_discovery else Res.string.collection_locked_hint,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = action) {
                if (discovery != null) onOpenDiscovery(discovery.id) else onShowHint(entry.item.id)
            }
            .padding(horizontal = ScreenGutter, vertical = Spacing.sm),
    ) {
        EntryFace(
            entry = entry,
            onOpenDiscovery = onOpenDiscovery,
            onShowHint = onShowHint,
            modifier = Modifier.size(FACE),
            isClickable = false,
        )

        Spacer(Modifier.width(Spacing.md))

        Column(
            modifier = Modifier
                .weight(1f)
                // The row speaks once, with the name and then the hint. Left to merge on its
                // own it would also read out the face's own description — which is the name
                // again — so a traveller listening would hear "Bánh xèo, Bánh xèo, bánh tráng
                // mỏng…" on every one of sixty-one rows.
                .clearAndSetSemantics {
                    contentDescription = "${entry.item.name}. ${entry.item.hint}"
                },
        ) {
            Text(
                text = entry.item.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (entry.isCollected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = entry.item.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A size, not a gap.
 *
 * Measured against the two lines beside it: a title over a two-line `bodySmall` hint is about
 * 62 dp of text on a phone, and a thumbnail shorter than its own caption reads as an
 * afterthought rather than as the same square the board is made of.
 */
private val FACE = 64.dp
