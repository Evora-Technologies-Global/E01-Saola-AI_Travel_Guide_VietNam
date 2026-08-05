package com.duylt.trave.vietlensai.feature.passport.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.passport_empty_action
import com.duylt.trave.vietlensai.resources.passport_empty_body
import com.duylt.trave.vietlensai.resources.passport_empty_title
import com.duylt.trave.vietlensai.resources.passport_no_location_body
import com.duylt.trave.vietlensai.resources.passport_no_location_title
import org.jetbrains.compose.resources.stringResource

/**
 * Why the map is still empty, and what to do about it.
 *
 * @param noLocation someone with captures but no stamps has location switched off. Telling
 *   them to go and discover something would be advice for a problem they do not have — so the
 *   card says the other thing and drops the button, because the way out is in system settings
 *   rather than in the camera.
 */
@Composable
internal fun PassportEmptyHint(
    noLocation: Boolean,
    onOpenLens: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = ScreenGutter, end = ScreenGutter, top = Spacing.lg)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(FRAME, PassportHairline, MaterialTheme.shapes.large)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Kicker(
                text = stringResource(
                    if (noLocation) Res.string.passport_no_location_title
                    else Res.string.passport_empty_title,
                ),
                color = Vermilion,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(
                    if (noLocation) Res.string.passport_no_location_body
                    else Res.string.passport_empty_body,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!noLocation) {
            Spacer(Modifier.width(Spacing.md))
            LensButton(
                onClick = onOpenLens,
                label = stringResource(Res.string.passport_empty_action),
            )
        }
    }
}

private val FRAME = 1.dp
