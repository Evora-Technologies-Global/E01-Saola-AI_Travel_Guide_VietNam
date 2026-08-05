package com.duylt.trave.vietlensai.feature.settings.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.SovereigntySeal
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.settings_sovereignty
import com.duylt.trave.vietlensai.resources.settings_sovereignty_summary
import org.jetbrains.compose.resources.stringResource

/**
 * The statement, as the last card on the page: a seal, a claim, and a way in.
 *
 * It stays under "About" on a large window too, and it stays even though the rail already
 * carries a seal of its own. The two are not a duplication to be tidied up: the seal at the
 * foot of the rail is an ambient mark a traveller may never read as a button, and this row is
 * the one that says in words what it opens. Losing it would make the statement reachable only
 * by guessing.
 */
@Composable
internal fun SovereigntyCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        // Outlined in the fixed vermilion rather than filled with it: filled is what the
        // banner on the explore tab does, and this row sits under a heading that has already
        // said "About".
        border = BorderStroke(HAIRLINE, Vermilion.copy(alpha = SOVEREIGNTY_BORDER_ALPHA)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(DISC_SIZE)
                    .clip(CircleShape)
                    .background(Vermilion),
                contentAlignment = Alignment.Center,
            ) {
                SovereigntySeal(size = SEAL_SIZE)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_sovereignty),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = stringResource(Res.string.settings_sovereignty_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(CHEVRON_SIZE),
            )
        }
    }
}

private const val SOVEREIGNTY_BORDER_ALPHA = 0.45f

private val HAIRLINE = 1.dp

/** The disc the seal is printed on, at the size of a list row's leading icon. */
private val DISC_SIZE = 48.dp
private val SEAL_SIZE = 40.dp
private val CHEVRON_SIZE = 20.dp
