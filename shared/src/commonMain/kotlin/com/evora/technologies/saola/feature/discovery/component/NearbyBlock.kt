package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.util.accentColor
import com.evora.technologies.saola.domain.model.NearbySuggestion
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.discovery_nearby
import com.evora.technologies.saola.resources.discovery_walk_minutes
import org.jetbrains.compose.resources.stringResource

/**
 * Where to go next, in the model's own order of preference.
 *
 * Numbered rather than bulleted because the order is the recommendation — and each ordinal
 * carries the colour of the category it names, which is the one place on the page where the
 * accent palette does any work beyond decoration.
 */
@Composable
internal fun NearbyBlock(suggestions: List<NearbySuggestion>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Kicker(
            text = stringResource(Res.string.discovery_nearby),
            color = MaterialTheme.colorScheme.primary,
        )
        suggestions.forEachIndexed { index, suggestion ->
            Spacer(Modifier.height(Spacing.lg))
            Row(verticalAlignment = Alignment.Top) {
                Ordinal(index + 1, color = suggestion.category.accentColor)
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = suggestion.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = suggestion.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    suggestion.walkingMinutes?.let { minutes ->
                        Spacer(Modifier.height(Spacing.xs))
                        Kicker(
                            text = stringResource(Res.string.discovery_walk_minutes, minutes),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}
