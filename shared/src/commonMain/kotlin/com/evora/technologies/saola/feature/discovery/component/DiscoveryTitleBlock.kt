package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.DashedRule
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.util.label
import com.evora.technologies.saola.domain.model.Discovery

/**
 * Place and name, opened by a mono kicker that runs into a stitched rule.
 *
 * The rule is what makes the kicker read as a dateline rather than as a caption: it takes the
 * rest of the line, so the eye finds the title underneath rather than beside it. That holds at
 * both widths, which is why the rule is weighted rather than a fixed length.
 */
@Composable
internal fun DiscoveryTitleBlock(discovery: Discovery, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        val place = discovery.placeHint?.takeIf { it.isNotBlank() }
        if (place != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(PIN_SIZE),
                )
                Spacer(Modifier.width(Spacing.xs))
                Kicker(text = place, color = MaterialTheme.colorScheme.primary)
                DashedRule(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = Spacing.md).weight(1f),
                )
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        Text(
            text = discovery.title,
            style = MaterialTheme.typography.headlineLarge,
        )

        val localName = discovery.localName?.takeIf { it.isNotBlank() && it != discovery.title }
        val subtitle = listOfNotNull(localName, discovery.category.label()).joinToString(" · ")
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Cap height of the kicker beside it, so the pin sits on the line rather than above it. */
private val PIN_SIZE = 13.dp
