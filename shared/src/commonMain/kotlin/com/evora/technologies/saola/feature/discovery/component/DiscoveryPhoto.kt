package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.AppAsyncImage
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.discovery_photo_label
import org.jetbrains.compose.resources.stringResource

/**
 * The photograph the traveller took, or a titled panel where it should have been.
 *
 * The picture and nothing else — no gradient, no badge, no rounded lip. Those belong to the
 * arrangement: the phone lays the page over the foot of a full-bleed shot and needs the
 * darkened edges to hold white type, while the large window sets the same photograph as a
 * card in a column beside the story, where a scrim would only make it murky.
 *
 * A record can outlive its file — a capture is stored by name and resolved late (`LLM.md`
 * §12) — so the missing case is a real state rather than a defensive branch, and it says
 * which discovery is missing its picture instead of leaving a grey hole.
 */
@Composable
internal fun DiscoveryPhoto(discovery: Discovery, modifier: Modifier = Modifier) {
    if (discovery.imagePath != null) {
        AppAsyncImage(
            model = discovery.imagePath,
            contentDescription = discovery.title,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Kicker(
                text = stringResource(Res.string.discovery_photo_label, discovery.title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // Two lines and a gutter, because the frame this lands in is 296 dp on a
                // tablet and a place name plus its label does not fit on one at that width.
                maxLines = PLACEHOLDER_MAX_LINES,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }
    }
}

private const val PLACEHOLDER_MAX_LINES = 2
