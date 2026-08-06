package com.evora.technologies.saola.feature.passport.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.AccentChip
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.domain.model.PassportStamp
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.passport_administers
import com.evora.technologies.saola.resources.passport_merged_from
import org.jetbrains.compose.resources.stringResource

/**
 * The reference notes: what this province used to be called, and what it administers out at
 * sea.
 *
 * Both were above the fold in the previous design, competing with the province's own name.
 * They are footnotes — true, occasionally essential, and never the reason anybody opened the
 * panel.
 */
@Composable
internal fun ProvinceFootnotes(stamp: PassportStamp, modifier: Modifier = Modifier) {
    val merged = stamp.province.mergedFrom
    val archipelagos = stamp.province.archipelagos
    if (merged.isEmpty() && archipelagos.isEmpty()) return

    Spacer(Modifier.height(Spacing.lg))
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = ScreenGutter)) {
        // The 2025 reorganisation folded 63 units into 34, so a traveller looking for
        // "Kiên Giang" needs to be told it is part of An Giang now.
        if (merged.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.passport_merged_from, merged.joinToString(" · ")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Only two provinces reach this, and it explains what the inset box on the
        // right belongs to — otherwise those boxes are a map decoration with no
        // stated connection to anything the traveller can collect.
        if (archipelagos.isNotEmpty()) {
            if (merged.isNotEmpty()) Spacer(Modifier.height(Spacing.sm))
            AccentChip(
                text = stringResource(
                    Res.string.passport_administers,
                    archipelagos.joinToString(" · ") { it.vietnameseName },
                ),
                accent = Vermilion,
            )
        }
    }
}
