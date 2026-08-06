package com.evora.technologies.saola.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.Spacing

/**
 * The credit a map owes the people who surveyed it.
 *
 * In the design system rather than in either feature because two of them draw it, which is
 * the line §10 draws — and because the two would otherwise word it differently. The passport
 * traces its 34 outlines from OpenStreetMap's boundary relations and the Around-you map plots
 * OpenStreetMap's places; both are the same database, and ODbL §4.3 asks for a notice
 * "reasonably calculated" to reach whoever is looking at the result. A line in a settings page
 * three taps away is not that. A line on the map is.
 *
 * Deliberately a pill in `surface` rather than plain text: it sits over tiles the app does not
 * control — a pale delta, a dark satellite night — and unbacked text is legible on one and
 * invisible on the other. The colour comes from the scheme, so it follows the theme like
 * everything else; only the alpha is fixed, and it is fixed high enough to read against Google's
 * own labels rather than low enough to disappear politely.
 *
 * The full text — which licence, which parts of the app, and what the other three sources are —
 * is the licences screen this credit is the short form of.
 */
@Composable
fun MapSourceNote(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = Pill,
        color = MaterialTheme.colorScheme.surface.copy(alpha = BACKING_ALPHA),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        )
    }
}

/** Enough backing to read over map tiles, little enough to stay out of the way of them. */
private const val BACKING_ALPHA = 0.82f
