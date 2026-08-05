package com.evora.technologies.saola.feature.explore.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.feature.explore.ExploreIntent
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.explore_recenter
import com.evora.technologies.saola.resources.explore_refresh
import org.jetbrains.compose.resources.stringResource

/**
 * The map's two controls, and the disc both are drawn on.
 *
 * One file because they are one decision seen twice: a round, surface-coloured button floated
 * over the map. **Not [com.evora.technologies.saola.core.designsystem.component.OverlayIconButton]**,
 * which is the app's affordance for a button over a *photograph* — black glass with a white
 * glyph. `OverlayHeaderStyle` already makes the same distinction and gives the reason: a map is
 * pale and full of its own labels, and the dark treatment laid over it reads as a bruise. The
 * two branches draw these buttons in different places and the treatment stays theirs.
 *
 * Each carries its own intent rather than taking an `onClick`, so which button means "centre
 * on me" and which means "search again" is decided here, once, for both arrangements.
 */
@Composable
internal fun RecenterButton(onIntent: (ExploreIntent) -> Unit, modifier: Modifier = Modifier) {
    MapControl(
        icon = Icons.Filled.MyLocation,
        contentDescription = stringResource(Res.string.explore_recenter),
        onClick = { onIntent(ExploreIntent.RecenterOnUser) },
        modifier = modifier,
    )
}

@Composable
internal fun RefreshButton(onIntent: (ExploreIntent) -> Unit, modifier: Modifier = Modifier) {
    MapControl(
        icon = Icons.Filled.Refresh,
        contentDescription = stringResource(Res.string.explore_refresh),
        onClick = { onIntent(ExploreIntent.Refresh(force = true)) },
        modifier = modifier,
    )
}

@Composable
private fun MapControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(CONTROL_SIDE).clip(CircleShape).clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(CONTROL_GLYPH_SIDE),
            )
        }
    }
}

/** A hit target and a glyph inside it — sizes, not gaps. 44 dp is the minimum, not a choice. */
private val CONTROL_SIDE = 44.dp
private val CONTROL_GLYPH_SIDE = 20.dp
