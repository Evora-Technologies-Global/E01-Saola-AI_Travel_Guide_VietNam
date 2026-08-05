package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.Motion
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.domain.model.DiscoveryNote
import com.evora.technologies.saola.feature.discovery.DiscoveryIntent
import com.evora.technologies.saola.feature.discovery.NoteEditor
import com.evora.technologies.saola.platform.rememberPhotoPicker
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.action_cancel
import com.evora.technologies.saola.resources.discovery_note_delete
import com.evora.technologies.saola.resources.discovery_note_in_diary
import com.evora.technologies.saola.resources.discovery_note_photo_count
import com.evora.technologies.saola.resources.discovery_note_placeholder
import com.evora.technologies.saola.resources.discovery_note_save
import org.jetbrains.compose.resources.stringResource

/**
 * The composer.
 *
 * Photos sit above the text field because that is the order the memory arrives in — the shot
 * is already on the phone, the words come after looking at it.
 *
 * @param hasSavedNote the live note, not the retained one. A note deleted and then started
 *   again must not be offered a delete button for a record that is no longer there.
 */
@Composable
internal fun NoteComposer(
    editor: NoteEditor,
    hasSavedNote: Boolean,
    isSaving: Boolean,
    onCapture: () -> Unit,
    onOpenPhoto: (index: Int) -> Unit,
    onIntent: (DiscoveryIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The picker is told how many slots are left, so its own selection counter stops the
    // traveller at the limit instead of the app quietly discarding what did not fit.
    val pickPhotos = rememberPhotoPicker(
        maxItems = DiscoveryNote.MAX_PHOTOS - editor.photoPaths.size,
    ) { paths ->
        onIntent(DiscoveryIntent.NotePhotoPicked(paths))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            NotePhotoStrip(
                paths = editor.photoPaths,
                onOpen = onOpenPhoto,
                onRemove = { path -> onIntent(DiscoveryIntent.NotePhotoRemoved(path)) },
                onAdd = pickPhotos.takeIf { editor.canAddPhoto },
                onCapture = onCapture.takeIf { editor.canAddPhoto },
            )

            Spacer(Modifier.height(Spacing.xs))
            Kicker(
                text = stringResource(
                    Res.string.discovery_note_photo_count,
                    editor.photoPaths.size,
                    DiscoveryNote.MAX_PHOTOS,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = editor.body,
                onValueChange = { onIntent(DiscoveryIntent.NoteBodyChanged(it)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = FIELD_MIN_HEIGHT),
                placeholder = {
                    Text(
                        text = stringResource(Res.string.discovery_note_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Vermilion,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            Spacer(Modifier.height(Spacing.sm))
            // Says out loud what the note is *for*: the diary the guide writes each evening
            // reads these, which is not something the traveller could guess from a text box.
            Text(
                text = stringResource(Res.string.discovery_note_in_diary),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.lg))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // A write to the database is usually over in a frame or two, and a button
                // that goes grey and back in that time reads as a flicker rather than as
                // work being done. Fading it holds the two states apart.
                val saveColor by animateColorAsState(
                    targetValue = if (isSaving) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        Vermilion
                    },
                    animationSpec = Motion.morph(Motion.QUICK_MILLIS),
                    label = "noteSaveColor",
                )

                Surface(
                    onClick = { onIntent(DiscoveryIntent.SaveNote) },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f).height(SAVE_HEIGHT),
                    shape = MaterialTheme.shapes.medium,
                    color = saveColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.discovery_note_save),
                            style = MaterialTheme.typography.titleSmall,
                            color = PaperCream,
                        )
                    }
                }

                TextButton(
                    onClick = { onIntent(DiscoveryIntent.CancelEditNote) },
                    enabled = !isSaving,
                ) {
                    Text(
                        text = stringResource(Res.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Folds up out of the card rather than vanishing: it is the last row, so its
            // going takes the card's foot with it, and a height that jumps looks like the
            // note was cut off.
            AnimatedVisibility(
                visible = hasSavedNote,
                enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
                exit = fadeOut(Motion.exit()) + shrinkVertically(Motion.exit()),
            ) {
                TextButton(
                    onClick = { onIntent(DiscoveryIntent.DeleteNote) },
                    enabled = !isSaving,
                ) {
                    Text(
                        text = stringResource(Res.string.discovery_note_delete),
                        style = MaterialTheme.typography.labelLarge,
                        color = Vermilion,
                    )
                }
            }
        }
    }
}

/**
 * How much blank the field opens with.
 *
 * A measured position rather than a gap: about four lines of this app's body scale, which is
 * long enough to read as an invitation to write a paragraph and short enough that the save
 * button stays on screen above the keyboard on the smallest phone.
 */
private val FIELD_MIN_HEIGHT = 120.dp

private val SAVE_HEIGHT = 48.dp
