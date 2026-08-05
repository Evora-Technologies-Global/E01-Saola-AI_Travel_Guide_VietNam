package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.core.util.label
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.discovery_confidence
import org.jetbrains.compose.resources.stringResource

/**
 * What the lens decided, and how sure it was — the one thing on the page the app is
 * accountable for.
 *
 * A lit dot beside the reading rather than a percentage on its own, because the number needs
 * to say *the model looked and answered* rather than read as a score the place was given.
 * Vermilion on cream in both schemes: it sits over a photograph on the phone and over the
 * page on a tablet, and a scheme colour would be legible on one and not the other.
 */
@Composable
internal fun MatchBadge(discovery: Discovery, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(Pill)
            .background(Vermilion)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(DOT_SIZE).clip(CircleShape).background(PaperCream))
        Spacer(Modifier.width(Spacing.sm))
        Kicker(
            text = "${discovery.category.label()} · " + stringResource(
                Res.string.discovery_confidence,
                (discovery.confidence * PERCENT).toInt(),
            ),
            color = PaperCream,
        )
    }
}

/** A lit indicator, not a bullet — small enough that the type stays the thing being read. */
private val DOT_SIZE = 6.dp

private const val PERCENT = 100
