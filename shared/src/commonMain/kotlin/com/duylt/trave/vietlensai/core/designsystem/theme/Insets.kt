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
 * `statusBarsPadding()` is not enough here. The app hides the status and navigation
 * bars, which takes that inset to zero — but the notch or the punch-hole camera is a
 * hole in the glass, and it is still there whether or not a bar is drawn over it.
 * Only [WindowInsets.displayCutout] reports it.
 *
 * The two are unioned rather than swapped: when the traveller swipes the bars back
 * into view, the status bar inset returns and the page has to move out from under it
 * again. On a phone with a flat top edge and hidden bars this is 0 on all sides, which
 * is exactly the full-bleed look the design asks for.
 *
 * In landscape the cutout moves to one side, and the union carries that side padding
 * too — which is why this is applied to a screen's outermost container rather than to
 * a header.
 */
@Composable
fun Modifier.screenInsetsPadding(): Modifier =
    windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
