package com.evora.technologies.saola.feature.settings.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.SharedBuildConfig
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.settings_footer
import org.jetbrains.compose.resources.stringResource

/**
 * Where the app is from and which build this is, at the foot of the page.
 *
 * Shared rather than typed twice because it is the one place the version number is rendered,
 * and a second copy is a second place to forget when the string around it changes.
 */
@Composable
internal fun SettingsFooter(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.settings_footer, SharedBuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = Spacing.xl),
    )
}
