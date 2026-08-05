package com.duylt.trave.vietlensai.feature.explore.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.action_retry
import org.jetbrains.compose.resources.stringResource

/**
 * A card floated over the map when there is something to say but the map still stands.
 *
 * Distinct from the three covers in `MapCovers.kt`, and the difference is whether the
 * traveller still has a map: a search that came back empty leaves a perfectly good picture of
 * where they are, and replacing it with a full-screen state to report "nothing here" would
 * take away the one thing that still works.
 */
@Composable
internal fun MapNotice(
    title: String,
    body: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = Spacing.xxl),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onRetry != null) {
                Spacer(Modifier.height(Spacing.md))
                FilledTonalButton(onClick = onRetry) {
                    Text(stringResource(Res.string.action_retry))
                }
            }
        }
    }
}
