package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.discovery_notice_count
import org.jetbrains.compose.resources.pluralStringResource

/**
 * The facts worth carrying into the visit, numbered so they can be counted off.
 *
 * A card rather than a run of paragraphs, because these are the part of the page a traveller
 * reads standing in front of the thing rather than sitting down afterwards — and a numbered
 * list on its own ground is what can be glanced at twice.
 */
@Composable
internal fun ThingsToNotice(facts: List<String>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Kicker(
                text = pluralStringResource(
                    Res.plurals.discovery_notice_count,
                    facts.size,
                    facts.size,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            facts.forEachIndexed { index, fact ->
                Spacer(Modifier.height(if (index == 0) Spacing.lg else Spacing.md))
                Row {
                    Ordinal(index + 1)
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        text = fact,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
