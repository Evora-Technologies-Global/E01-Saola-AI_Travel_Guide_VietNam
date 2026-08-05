package com.duylt.trave.vietlensai.feature.discovery.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.discovery_low_confidence
import org.jetbrains.compose.resources.stringResource

/**
 * Summary and sections as one continuous read.
 *
 * The summary keeps full-strength ink and every section after it steps back a shade, so the
 * page has a lede and a body instead of eight equal paragraphs.
 */
@Composable
internal fun StoryBody(discovery: Discovery, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = discovery.summary,
            style = MaterialTheme.typography.bodyLarge,
        )
        discovery.sections.forEach { section ->
            Spacer(Modifier.height(Spacing.xl))
            Kicker(text = section.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = section.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The caveat, drawn only when the lens was not confident.
 *
 * It sits above the story rather than in the footer with the provenance: whether to trust what
 * follows is a thing to know before reading it, not after.
 */
@Composable
internal fun LowConfidenceNote(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = stringResource(Res.string.discovery_low_confidence),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(Spacing.md),
        )
    }
}
