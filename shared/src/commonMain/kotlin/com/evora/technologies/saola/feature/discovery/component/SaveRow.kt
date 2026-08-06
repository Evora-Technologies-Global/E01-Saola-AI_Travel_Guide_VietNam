package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.discovery_favorite_add
import com.evora.technologies.saola.resources.discovery_favorite_saved
import com.evora.technologies.saola.resources.discovery_share
import com.evora.technologies.saola.resources.report_again
import com.evora.technologies.saola.resources.report_title
import org.jetbrains.compose.resources.stringResource

/**
 * Keeping, passing on, and objecting: the three things left to do once the page has been read.
 *
 * Favouriting gets the wide button because it is the one the traveller comes back for; sharing
 * and reporting sit beside it as squares, since both leave the app either way — one to a
 * messaging app, the other to a mail composer.
 *
 * Reporting was a quiet line of its own under the colophon until 06.08.2026, on the argument
 * that disputing the page should not be offered as loudly as keeping it. It reads as the same
 * square as sharing now because it was asked for that way, and the two do belong together:
 * they are the only actions here that hand the record to something outside the app. The
 * outlined square is the quieter of the two shapes on this row regardless — the filled
 * vermilion slab beside it is still what the eye lands on.
 *
 * Once a report has been filed the square fills and the flag solidifies. That is the entire
 * acknowledgement a report can honestly get here — there is no server to hear it back from —
 * and it is also what stops the same objection being invited again on every visit.
 *
 * It fills whatever width it is given, and each arrangement decides how much that is. The
 * phone spends the page on it, at the point in the scroll where the reading is done. The
 * tablet gives it a fixed measure in the toolbar above the story, because the wireframe puts
 * these actions at the top of a page that no longer has a bottom the traveller reaches.
 */
@Composable
internal fun SaveRow(
    isFavorite: Boolean,
    hasReported: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onToggleFavorite,
            modifier = Modifier.weight(1f).height(BUTTON_HEIGHT),
            shape = MaterialTheme.shapes.medium,
            color = if (isFavorite) MaterialTheme.colorScheme.surfaceVariant else Vermilion,
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val content = if (isFavorite) MaterialTheme.colorScheme.primary else PaperCream
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(ICON_SIZE),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(
                        if (isFavorite) {
                            Res.string.discovery_favorite_saved
                        } else {
                            Res.string.discovery_favorite_add
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = content,
                )
            }
        }

        SquareAction(
            icon = Icons.Filled.Share,
            contentDescription = stringResource(Res.string.discovery_share),
            onClick = onShare,
        )

        SquareAction(
            // Hollow until it has been used, then solid — the same pair the favourite beside
            // it is drawn from, and taken from the *filled* set for the same reason
            // `FavoriteBorder` is: one package, so the two variants cannot be imported under
            // one ambiguous name. The fill behind it says the same thing again, for anyone
            // who does not read a hollow flag as different from a solid one.
            icon = if (hasReported) Icons.Filled.Flag else Icons.Filled.OutlinedFlag,
            // The sheet's own title, not the page's old invitation to complain: an icon's
            // description is read out as a label for what pressing it does, and "Something
            // wrong here?" was written to be a line of prose the traveller reads past.
            contentDescription = stringResource(
                if (hasReported) Res.string.report_again else Res.string.report_title,
            ),
            onClick = onReport,
            filled = hasReported,
        )
    }
}

/**
 * One of the two square actions on the row, so that "the same style as share" stays true by
 * construction rather than by two call sites happening to agree today.
 */
@Composable
private fun SquareAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    filled: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(BUTTON_HEIGHT),
        shape = MaterialTheme.shapes.medium,
        color = if (filled) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        // Kept in both states: the outline is what makes this a square of the same weight as
        // the one beside it, and dropping it when the fill arrives would change the silhouette
        // of a button whose meaning has not changed.
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(ICON_SIZE),
            )
        }
    }
}

/** A comfortable target for a thumb, and the side of the square the share button is. */
private val BUTTON_HEIGHT = 56.dp
private val ICON_SIZE = 20.dp
