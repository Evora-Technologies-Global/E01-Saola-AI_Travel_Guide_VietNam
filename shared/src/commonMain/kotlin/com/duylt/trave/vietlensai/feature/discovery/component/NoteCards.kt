package com.duylt.trave.vietlensai.feature.discovery.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.component.dashedBorder
import com.duylt.trave.vietlensai.core.designsystem.theme.Corner
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.util.asRelativeTime
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.DiscoveryNote
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.discovery_note_add
import com.duylt.trave.vietlensai.resources.discovery_note_edited
import com.duylt.trave.vietlensai.resources.discovery_note_empty_body
import com.duylt.trave.vietlensai.resources.discovery_note_empty_title
import org.jetbrains.compose.resources.stringResource

/**
 * The note at rest, in its two states: not yet written, and written.
 *
 * One file because they are the same card seen twice — [NoteBlock] animates directly between
 * them and the two have to keep the same corner, the same surface and the same inner padding
 * or the swap reads as one card being replaced by a different object.
 */

/** A blank ruled off in the notebook: dashed, unfilled, and obviously meant for the reader. */
@Composable
internal fun EmptyNoteCard(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .dashedBorder(MaterialTheme.colorScheme.outlineVariant, cornerRadius = Corner.large)
            .clickable(onClick = onStart)
            .padding(Spacing.xl),
    ) {
        Text(
            text = stringResource(Res.string.discovery_note_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(Res.string.discovery_note_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.AddAPhoto,
                contentDescription = null,
                tint = Vermilion,
                modifier = Modifier.size(HINT_ICON_SIZE),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = stringResource(Res.string.discovery_note_add),
                style = MaterialTheme.typography.titleSmall,
                color = Vermilion,
            )
        }
    }
}

/** A written note at rest: their photos, their words, and when they wrote them. */
@Composable
internal fun SavedNote(
    note: DiscoveryNote,
    language: AppLanguage,
    onOpenPhoto: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            if (note.photoPaths.isNotEmpty()) {
                NotePhotoStrip(
                    paths = note.photoPaths,
                    onOpen = onOpenPhoto,
                    onRemove = null,
                )
                Spacer(Modifier.height(if (note.body.isNotBlank()) Spacing.lg else Spacing.xs))
            }
            if (note.body.isNotBlank()) {
                Text(text = note.body, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(Spacing.md))
            }
            Kicker(
                text = buildString {
                    append(note.updatedAt.asRelativeTime(language))
                    if (note.wasEdited) {
                        append(" · ").append(stringResource(Res.string.discovery_note_edited))
                    }
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Cap height of the line it labels, so the camera sits on the text rather than above it. */
private val HINT_ICON_SIZE = 18.dp
