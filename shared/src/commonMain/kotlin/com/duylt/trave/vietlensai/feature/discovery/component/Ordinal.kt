package com.duylt.trave.vietlensai.feature.discovery.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.StampType

/**
 * "01", "02" — the mono counter that runs down every numbered list on this page.
 *
 * Padded from the top so the digits sit on the first line of the text beside them rather than
 * on its centre: the entries are one to three lines long, and a centred counter would drift
 * down the block as the entry got longer.
 */
@Composable
internal fun Ordinal(
    number: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    Text(
        text = number.toString().padStart(ORDINAL_DIGITS, '0'),
        style = StampType.ordinal,
        color = color,
        modifier = modifier.padding(top = Spacing.xxs),
    )
}

/** Two, always: "1" beside "10" would set the column ragged. */
private const val ORDINAL_DIGITS = 2
