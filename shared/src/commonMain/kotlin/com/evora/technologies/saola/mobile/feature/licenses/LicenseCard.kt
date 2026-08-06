package com.evora.technologies.saola.mobile.feature.licenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.licenses_open
import org.jetbrains.compose.resources.stringResource

/**
 * One source: what it gave the app, and the way through to its terms.
 *
 * Split out of `LicensesScreen.kt` rather than kept private at the foot of it because four
 * calls to the same shape is exactly when a screen-local helper stops being screen-local —
 * `LLM.md` §10. It stays under `mobile/feature/licenses/` beside the only screen that draws
 * it: it is not a design-system component, since nothing outside this page has a use for a
 * card whose only affordance is "read the licence".
 *
 * The card itself is not clickable and the button is. A whole-card tap would put an outbound
 * jump to a browser under the same gesture as reading the paragraph, and this is the one page
 * in the app where the traveller is meant to read before they leave.
 */
@Composable
internal fun LicenseCard(
    title: String,
    body: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            TextButton(
                onClick = onOpen,
                // Pulled back to the text above it: a `TextButton` carries its own start
                // padding, and left alone the label sits inset from the paragraph it belongs
                // to. An offset rather than a negative padding, which `padding` rejects.
                modifier = Modifier.offset(x = -BUTTON_INSET),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(GLYPH),
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(stringResource(Res.string.licenses_open))
            }
        }
    }
}

/** Material's own text-button padding, which is what is being cancelled. */
private val BUTTON_INSET = 12.dp

/** Sized against the label beside it, not against the spacing scale. */
private val GLYPH = 16.dp
