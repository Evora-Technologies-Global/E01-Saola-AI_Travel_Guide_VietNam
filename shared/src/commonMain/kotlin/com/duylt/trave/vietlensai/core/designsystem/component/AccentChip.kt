package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.duylt.trave.vietlensai.core.designsystem.theme.Pill
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing

/** A pill that carries a category's own colour, used across cards and detail headers. */
@Composable
fun AccentChip(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(Pill)
            .background(accent.copy(alpha = CHIP_BACKGROUND_ALPHA))
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
        )
    }
}

private const val CHIP_BACKGROUND_ALPHA = 0.14f
