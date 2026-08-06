package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.DashedRule
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.util.asRelativeTime
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.discovery_model
import org.jetbrains.compose.resources.stringResource

/**
 * When this was captured, and which model wrote what is above it.
 *
 * Provenance rather than decoration: everything on this page except the note is an AI's account
 * of a place, and the traveller is entitled to know which one and when. It sits at the foot
 * because that is where a colophon goes — worth finding, not worth reading first.
 */
@Composable
internal fun DiscoveryFooter(
    discovery: Discovery,
    language: AppLanguage,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DashedRule(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        Kicker(
            text = discovery.createdAt.asRelativeTime(language),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        discovery.modelUsed?.let { model ->
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(Res.string.discovery_model, model),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
