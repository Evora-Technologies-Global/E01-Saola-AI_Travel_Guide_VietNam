package com.duylt.trave.vietlensai.feature.collection.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.duylt.trave.vietlensai.core.designsystem.component.FillGauge
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.collection_collected_percent
import com.duylt.trave.vietlensai.resources.collection_progress_of
import org.jetbrains.compose.resources.stringResource

/**
 * The count, the share, and the bar — the passport's progress block, counting objects instead
 * of provinces.
 *
 * The big figure is what has been found rather than a percentage, and the percentage is
 * demoted to the kicker on the right. A collection is counted in things: "57 left" is a number
 * someone can act on in an afternoon, where "7% collected" is not.
 */
@Composable
internal fun CollectionProgress(
    collected: Int,
    total: Int,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(targetValue = progress, label = "collectionProgress")
    val percent = (progress * PERCENT).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = collected.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = Vermilion,
            )
            Text(
                text = stringResource(Res.string.collection_progress_of, total),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.sm, bottom = Spacing.xs),
            )
            Spacer(Modifier.weight(1f))
            Kicker(
                text = stringResource(Res.string.collection_collected_percent, percent),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        FillGauge(progress = animated)
    }
}

private const val PERCENT = 100
