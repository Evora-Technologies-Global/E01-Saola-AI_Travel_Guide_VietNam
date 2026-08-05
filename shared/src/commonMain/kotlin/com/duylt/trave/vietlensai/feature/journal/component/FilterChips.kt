package com.duylt.trave.vietlensai.feature.journal.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.theme.Pill
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.journal_filter_all
import com.duylt.trave.vietlensai.resources.journal_filter_favorites
import org.jetbrains.compose.resources.stringResource

/**
 * All, or only the ones the traveller kept.
 *
 * Two chips rather than a switch: "Favorites" is a place in the journal, not a setting, and a
 * switch would say the app remembers the choice past this visit — it does not, and should not.
 */
@Composable
internal fun FilterChips(
    favoritesOnly: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        FilterChip(
            selected = !favoritesOnly,
            onClick = { onChange(false) },
            label = { Text(stringResource(Res.string.journal_filter_all)) },
            shape = Pill,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
            ),
        )
        FilterChip(
            selected = favoritesOnly,
            onClick = { onChange(true) },
            label = { Text(stringResource(Res.string.journal_filter_favorites)) },
            shape = Pill,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
            ),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(LEADING_ICON),
                )
            },
        )
    }
}

/** A chip's own leading mark, sized to Material's own chip metrics. */
private val LEADING_ICON = 16.dp
