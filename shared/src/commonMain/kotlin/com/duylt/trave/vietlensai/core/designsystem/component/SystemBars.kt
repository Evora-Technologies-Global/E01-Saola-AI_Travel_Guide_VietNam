package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.runtime.Composable

/**
 * Pins the status and navigation bar icons while a screen is open.
 *
 * For screens that paint their own background regardless of the theme — the cream chat,
 * the lacquer-red sovereignty statement. Left to the theme, those screens get white icons
 * on cream in dark mode, or near-black icons on deep red in light mode.
 *
 * The previous appearance is restored on the way out, so the rest of the app keeps
 * following the traveller's own light/dark preference.
 *
 * @param darkIcons true for a light background, false for a dark one.
 */
@Composable
expect fun PinSystemBarIcons(darkIcons: Boolean)
