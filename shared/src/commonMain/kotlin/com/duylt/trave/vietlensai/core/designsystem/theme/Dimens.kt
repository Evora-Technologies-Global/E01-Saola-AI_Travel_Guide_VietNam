package com.duylt.trave.vietlensai.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * The gutter every screen keeps between the display edge and anything drawn on it.
 *
 * One value for the whole app rather than a per-screen choice: the traveller moves
 * between the lens, the journal and a discovery in seconds, and edges that shift by
 * a few dp from screen to screen read as a mistake even when nobody can say which
 * screen is the wrong one.
 *
 * It applies to the outermost run of a screen's content — page columns, list
 * content padding, floating controls, full-bleed banners. Padding *inside* a card
 * or a chip is that component's own business and is not this number.
 */
val ScreenGutter = 16.dp
