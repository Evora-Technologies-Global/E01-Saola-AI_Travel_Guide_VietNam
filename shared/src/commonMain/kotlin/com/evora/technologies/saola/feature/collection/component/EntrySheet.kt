package com.evora.technologies.saola.feature.collection.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.util.accentColor
import com.evora.technologies.saola.core.util.label
import com.evora.technologies.saola.domain.model.CollectionEntry
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.collection_go_capture
import com.evora.technologies.saola.resources.collection_view_capture
import org.jetbrains.compose.resources.stringResource

/**
 * What one entry is, and how to recognise it.
 *
 * The hint is the reason this sheet exists. "Bánh xèo" is a name the traveller can already
 * read off the tile; "bánh tráng mỏng vàng nghệ gập đôi hình bán nguyệt" is what lets them
 * spot one on a street they have never walked down.
 *
 * A modal sheet on both form factors, unlike the passport's. This one has nothing behind it
 * worth keeping live — a board of tiles, not a map being compared across — and it is opened
 * from a single tile rather than dragged across a surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntrySheet(
    entry: CollectionEntry,
    onDismiss: () -> Unit,
    onOpenLens: () -> Unit,
    onOpenDiscovery: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter)
                .padding(bottom = PageSpacing.listBottom),
        ) {
            Kicker(
                text = entry.item.category.label(),
                color = entry.item.category.accentColor,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = entry.item.name,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = entry.item.hint,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xl))

            // The sheet is opened from uncollected tiles only, but a photograph taken
            // while it is up flips the entry underneath it — so it has to be able to
            // offer the discovery as well as the camera.
            val discovery = entry.discovery
            Button(
                onClick = {
                    onDismiss()
                    if (discovery != null) onOpenDiscovery(discovery.id) else onOpenLens()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(ICON),
                )
                Text(
                    text = stringResource(
                        if (discovery != null) {
                            Res.string.collection_view_capture
                        } else {
                            Res.string.collection_go_capture
                        },
                    ),
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
    }
}

private val ICON = 18.dp
