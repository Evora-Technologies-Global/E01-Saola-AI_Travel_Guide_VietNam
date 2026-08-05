package com.duylt.trave.vietlensai.core.designsystem.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The top edge every page starts from, notch included.
 *
 * `statusBarsPadding()` is not enough here, and the reason has outlived the reason it
 * was written for. The status bar is shown — transparent, with the page running under
 * it — so that inset is real and a page does have to move out from under the clock.
 * But the notch or the punch-hole camera is a hole in the glass and does not always sit
 * behind the bar: **in landscape the cutout is on a side edge, where there is no status
 * bar at all**, and only [WindowInsets.displayCutout] reports it. The two are unioned so
 * one modifier answers both, on every rotation.
 *
 * That side padding is also why this is applied to a screen's outermost container rather
 * than to a header — a header takes the top edge and would leave the cutout side bare.
 * The large-window rail is the same argument turned sideways: on a tablet held in
 * landscape the hole is on the edge the rail is standing on.
 *
 * The navigation bar is *not* part of this: it is hidden (`MainActivity.hideNavigationBar`),
 * and the screens that float a control against the bottom edge take
 * `navigationBarsPadding()` themselves so a transiently-swiped bar does not cover it.
 *
 * Exposed as a value as well as a [Modifier] because a few components take insets
 * rather than padding — `NavigationRail` applies them inside its own surface, so the
 * rail's colour still reaches the glass and only its contents move in. One definition
 * either way, so the two spellings cannot come to mean different things.
 */
val ScreenInsets: WindowInsets
    @Composable get() = WindowInsets.statusBars.union(WindowInsets.displayCutout)

/** [ScreenInsets] as padding, which is how a screen's outermost container takes it. */
@Composable
fun Modifier.screenInsetsPadding(): Modifier = windowInsetsPadding(ScreenInsets)
