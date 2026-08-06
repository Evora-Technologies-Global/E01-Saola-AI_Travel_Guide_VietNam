package com.evora.technologies.saola.feature.settings.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter

/**
 * The container every group of rows shares.
 *
 * Grouping is the layout's only structure: rows that belong together sit on one card, and the
 * section label above it says what "together" means. Nothing on this screen is a full-bleed
 * list row.
 *
 * It takes the gutter itself rather than leaving it to the caller, which is what lets the same
 * card be an item in the phone's one long list and a block in one of the tablet's two columns
 * without either arrangement typing an edge. On a large window the column is what has an edge,
 * and the gutter is measured from that.
 */
@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = { content() })
    }
}
