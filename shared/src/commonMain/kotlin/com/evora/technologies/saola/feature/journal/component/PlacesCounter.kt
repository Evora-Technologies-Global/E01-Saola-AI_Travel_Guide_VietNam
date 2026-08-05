package com.evora.technologies.saola.feature.journal.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.journal_discoveries_label
import org.jetbrains.compose.resources.pluralStringResource

/**
 * How much the traveller has found, as a figure and the word for it.
 *
 * It goes in the `trailing` slot of whichever [com.evora.technologies.saola.core.designsystem.component.PageHeader]
 * the branch composes — the phone's above its day list, the tablet's above the day column.
 * The *contents* of the slot are shared; the header call is not, which is the line `LLM.md`
 * §12 draws around the two headers. A `JournalHeader(…)` wrapping `PageHeader` would put both
 * branches one level away from the call `DesignTokenTest.HEADER_OWNERS` looks for.
 */
@Composable
internal fun PlacesCounter(discoveryCount: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = discoveryCount.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Kicker(
            text = pluralStringResource(Res.plurals.journal_discoveries_label, discoveryCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
