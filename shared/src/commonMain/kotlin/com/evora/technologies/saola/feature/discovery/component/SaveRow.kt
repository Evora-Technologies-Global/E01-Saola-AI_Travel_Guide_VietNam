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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.discovery_favorite_add
import com.evora.technologies.saola.resources.discovery_favorite_saved
import com.evora.technologies.saola.resources.discovery_share
import org.jetbrains.compose.resources.stringResource

/**
 * Keeping and passing on: the two things left to do once the page has been read.
 *
 * Favouriting gets the wide button because it is the one the traveller comes back for; sharing
 * sits beside it as an icon, since it leaves the app either way.
 *
 * It fills whatever width it is given, and each arrangement decides how much that is. The
 * phone spends the page on it, at the point in the scroll where the reading is done. The
 * tablet gives it a fixed measure in the toolbar above the story, because the wireframe puts
 * the two actions at the top of a page that no longer has a bottom the traveller reaches.
 */
@Composable
internal fun SaveRow(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
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

        Surface(
            onClick = onShare,
            modifier = Modifier.size(BUTTON_HEIGHT),
            shape = MaterialTheme.shapes.medium,
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(Res.string.discovery_share),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ICON_SIZE),
                )
            }
        }
    }
}

/** A comfortable target for a thumb, and the side of the square the share button is. */
private val BUTTON_HEIGHT = 56.dp
private val ICON_SIZE = 20.dp
