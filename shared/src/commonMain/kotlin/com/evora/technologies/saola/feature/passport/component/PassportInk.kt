package com.evora.technologies.saola.feature.passport.component

import androidx.compose.ui.graphics.Color
import com.evora.technologies.saola.core.designsystem.theme.Marigold

/**
 * The gold hairline the passport rules everything with.
 *
 * The frame around the map, the border of the hint card, the stitched rules inside the
 * province panel and the lip of the panel itself are all this one colour at this one weight —
 * that is what makes the screen read as pages of a single document rather than as four cards
 * that happen to share a page. It used to be `Marigold.copy(alpha = 0.35f)` typed at five call
 * sites in one file, which was fine while they were in one file; the tablet puts three of them
 * in a pane and the other two somewhere else.
 *
 * Fixed brand gold rather than a scheme role, for the reason `PassportScreen`'s KDoc gives:
 * this screen is the app's identity in one view and should look the same whatever the theme.
 */
internal val PassportHairline: Color = Marigold.copy(alpha = HAIRLINE_ALPHA)

/** Faint enough to be a rule rather than a border, dark enough to survive the dark scheme. */
private const val HAIRLINE_ALPHA = 0.35f
