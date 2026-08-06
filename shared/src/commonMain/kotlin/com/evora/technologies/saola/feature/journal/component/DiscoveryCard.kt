package com.evora.technologies.saola.feature.journal.component

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.AppAsyncImage
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.util.accentColor
import com.evora.technologies.saola.core.util.formatTime
import com.evora.technologies.saola.core.util.label
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.discovery_favorite_add
import com.evora.technologies.saola.resources.discovery_favorite_remove
import org.jetbrains.compose.resources.stringResource

/**
 * One find, as a line in the day it belongs to: the photograph, what it was, and when.
 *
 * The heart is a control of its own rather than part of the tap target. Opening a discovery
 * and keeping one are different intentions, and on a list this dense a single target would
 * make the second an accident of where the thumb landed.
 */
@Composable
internal fun DiscoveryCard(
    discovery: Discovery,
    language: AppLanguage,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = Spacing.xs)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        AppAsyncImage(
            model = discovery.imagePath,
            contentDescription = discovery.title,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(THUMBNAIL),
        )
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = discovery.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = discovery.createdAt.formatTime(language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = Spacing.xxs),
                )
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = discovery.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.sm))
            Kicker(
                text = discovery.category.label(),
                color = discovery.category.accentColor,
            )
        }
        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(HEART_TARGET)) {
            Icon(
                imageVector = if (discovery.isFavorite) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
                contentDescription = stringResource(
                    if (discovery.isFavorite) {
                        Res.string.discovery_favorite_remove
                    } else {
                        Res.string.discovery_favorite_add
                    },
                ),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(HEART),
            )
        }
    }
}

/** The card's own geometry: a square photograph and the heart's target beside it. */
private val THUMBNAIL = 72.dp
private val HEART_TARGET = 36.dp
private val HEART = 20.dp
