package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.runtime.Composable

/**
 * The app-wide default appearance of the system bar icons.
 *
 * Called once, by `VietLensTheme`, with the answer the theme just resolved: dark icons on
 * a light scheme, light icons on a dark one. The status bar is transparent and every page
 * runs underneath it, so the icons are read against the page's own background — which is
 * the scheme's, on every screen that does not paint its own.
 *
 * **The base, not a request.** It is deliberately not the same call as [PinSystemBarIcons]:
 * a screen's pin sits *over* this and pops back to it, so the traveller's current theme is
 * what a screen lands on when it closes rather than a value hardcoded years ago.
 *
 * @param darkIcons true in light mode, false in dark mode.
 */
@Composable
expect fun DefaultSystemBarIcons(darkIcons: Boolean)

/**
 * Pins the status and navigation bar icons while a screen is open, over [DefaultSystemBarIcons].
 *
 * For screens that paint their own background regardless of the theme — the cream chat,
 * the lacquer-red sovereignty statement. Left to the theme, those screens get white icons
 * on cream in dark mode, or near-black icons on deep red in light mode.
 *
 * The previous appearance is restored on the way out, so the rest of the app keeps
 * following the traveller's own light/dark preference.
 *
 * **One case this does not cover, stated rather than hidden:** if the theme itself changes
 * while a pinned screen is open — the system flipping to dark under `ThemePreference.SYSTEM`,
 * which is the only way it can happen without navigating away — the new base is applied over
 * the pin and those icons are wrong until the screen is left. Cosmetic, on two screens, and
 * the alternative is a depth-aware stack that would cost more than the defect.
 *
 * @param darkIcons true for a light background, false for a dark one.
 */
@Composable
expect fun PinSystemBarIcons(darkIcons: Boolean)
