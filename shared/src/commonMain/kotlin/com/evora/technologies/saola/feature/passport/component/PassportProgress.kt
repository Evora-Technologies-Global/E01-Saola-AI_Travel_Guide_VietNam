package com.evora.technologies.saola.feature.passport.component

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
import com.evora.technologies.saola.core.designsystem.component.FillGauge
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.domain.model.TravelPassport
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.passport_explored
import com.evora.technologies.saola.resources.passport_progress_of
import org.jetbrains.compose.resources.stringResource

/**
 * The count, the share, and the bar.
 *
 * The right-hand figure is the percentage explored rather than a tally of discoveries: this
 * screen is about ground covered, and the number of photographs behind a province is what its
 * own sheet is for.
 */
@Composable
internal fun PassportProgress(passport: TravelPassport, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = passport.progress,
        label = "passportProgress",
    )
    val percent = (passport.progress * PERCENT).toInt()

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = ScreenGutter, vertical = Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = passport.unlockedCount.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = Vermilion,
            )
            Text(
                text = stringResource(Res.string.passport_progress_of, passport.totalCount),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.sm, bottom = Spacing.xs),
            )
            Spacer(Modifier.weight(1f))
            Kicker(
                text = stringResource(Res.string.passport_explored, percent),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        FillGauge(progress = progress)
    }
}

private const val PERCENT = 100
