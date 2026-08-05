package com.duylt.trave.vietlensai.feature.discovery.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import com.duylt.trave.vietlensai.core.designsystem.component.dashedBorder
import com.duylt.trave.vietlensai.core.designsystem.theme.Corner
import com.duylt.trave.vietlensai.core.designsystem.theme.Motion
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.discovery_note_add_photo
import com.duylt.trave.vietlensai.resources.discovery_note_open_photo
import com.duylt.trave.vietlensai.resources.discovery_note_remove_photo
import com.duylt.trave.vietlensai.resources.discovery_note_take_photo
import org.jetbrains.compose.resources.stringResource

/**
 * The photos kept with a note, as a strip that runs off the edge of the card.
 *
 * @param onOpen opens the photo at that position full screen. Always available: looking at
 *   what was kept is not an edit, and it is the only way to see a 104dp tile properly.
 * @param onRemove null when the note is at rest — the delete badges only exist inside an edit.
 * @param onCapture takes a new photo; null once the note's photo cap is reached.
 * @param onAdd picks from the gallery; null on the same condition. Both go null together,
 *   which is what removes the trailing tiles rather than leaving buttons that refuse to do
 *   anything.
 */
@Composable
internal fun NotePhotoStrip(
    paths: List<String>,
    onOpen: (index: Int) -> Unit,
    onRemove: ((String) -> Unit)?,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
    onCapture: (() -> Unit)? = null,
) {
    if (paths.isEmpty() && onAdd == null && onCapture == null) return

    val openLabel = stringResource(Res.string.discovery_note_open_photo)

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        itemsIndexed(items = paths, key = { _, path -> path }) { index, path ->
            // Keyed, so removing a photo from the middle slides the ones after it left into
            // the gap instead of teleporting them. Six tiles is few enough that the eye
            // tracks each one, and a strip that reshuffles instantly loses which was which.
            Box(
                modifier = Modifier.animateItem(
                    fadeInSpec = Motion.enter(),
                    placementSpec = Motion.morph(),
                    fadeOutSpec = Motion.exit(),
                ),
            ) {
                AppAsyncImage(
                    model = path,
                    contentDescription = openLabel,
                    shape = MaterialTheme.shapes.medium,
                    // Under the badge, not over it: the two targets overlap in the
                    // corner, and the badge is the smaller and more consequential of
                    // the two, so it has to be the one that wins there.
                    onClick = { onOpen(index) },
                    modifier = Modifier.size(NOTE_PHOTO_SIZE),
                )
                if (onRemove != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(Spacing.xs)
                            .size(BADGE_SIZE)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = BADGE_ALPHA))
                            .clickable { onRemove(path) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.discovery_note_remove_photo),
                            tint = Color.White,
                            modifier = Modifier.size(BADGE_ICON_SIZE),
                        )
                    }
                }
            }
        }

        // Camera before gallery: standing in front of the place is when the photo worth
        // keeping does not exist yet. Both carry a key of their own so that reaching the
        // last photo fades them out where they stand, rather than making that photo
        // appear to have replaced them.
        if (onCapture != null) {
            item(key = CAPTURE_TILE_KEY) {
                PhotoActionTile(
                    icon = Icons.Filled.PhotoCamera,
                    label = stringResource(Res.string.discovery_note_take_photo),
                    onClick = onCapture,
                    modifier = Modifier.animateItem(
                        fadeInSpec = Motion.enter(),
                        placementSpec = Motion.morph(),
                        fadeOutSpec = Motion.exit(),
                    ),
                )
            }
        }

        if (onAdd != null) {
            item(key = ADD_TILE_KEY) {
                PhotoActionTile(
                    icon = Icons.Filled.AddAPhoto,
                    label = stringResource(Res.string.discovery_note_add_photo),
                    onClick = onAdd,
                    modifier = Modifier.animateItem(
                        fadeInSpec = Motion.enter(),
                        placementSpec = Motion.morph(),
                        fadeOutSpec = Motion.exit(),
                    ),
                )
            }
        }
    }
}

/** An empty frame in the strip, waiting to be filled by one of the two ways in. */
@Composable
private fun PhotoActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(NOTE_PHOTO_SIZE)
            .clip(MaterialTheme.shapes.medium)
            .dashedBorder(MaterialTheme.colorScheme.outline, cornerRadius = Corner.medium)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(TILE_ICON_SIZE),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Big enough to recognise a face in, small enough that three fit on the narrowest phone. */
private val NOTE_PHOTO_SIZE = 104.dp

private val BADGE_SIZE = 24.dp
private val BADGE_ICON_SIZE = 14.dp
private val TILE_ICON_SIZE = 20.dp

/** Dark enough to hold a white cross over the pale corner of a photograph. */
private const val BADGE_ALPHA = 0.55f

/**
 * Identities for the two tiles at the end of the photo strip.
 *
 * The photos are keyed by their own paths; these two need keys of their own or the row would
 * treat the tile in slot four as the same item as the photo that later takes that slot, and
 * animate one into the other.
 */
private const val CAPTURE_TILE_KEY = "note-photo-capture"
private const val ADD_TILE_KEY = "note-photo-add"
