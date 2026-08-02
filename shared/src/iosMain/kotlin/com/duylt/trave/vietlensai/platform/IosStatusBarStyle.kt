package com.duylt.trave.vietlensai.platform

import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent

/**
 * The status bar style the topmost screen has asked for.
 *
 * Android lets a composable flip the bar icons through a window flag; iOS resolves the
 * style by asking the view controller hierarchy, so a composable can only publish a
 * request and let the hosting controller answer with it. `VietLensViewController` reads
 * [current] from its `preferredStatusBarStyle` override.
 *
 * A stack rather than a single value: screens nest — the chat opens over the journal, the
 * sovereignty statement over Explore — and popping has to restore whatever the screen
 * underneath had asked for, not the app default.
 *
 * Single-threaded by construction: every caller is a Compose effect, and Compose effects
 * on iOS run on the main thread.
 */
object IosStatusBarStyle {

    private val requests = mutableListOf<UIStatusBarStyle>()

    /** Called by the hosting view controller so it can re-ask iOS for the bar style. */
    var onChanged: (() -> Unit)? = null

    /**
     * Light content by default: the app is dark-first, and the viewfinder behind the
     * status bar is a camera preview.
     */
    val current: UIStatusBarStyle
        get() = requests.lastOrNull() ?: UIStatusBarStyleLightContent

    internal fun push(darkContent: Boolean) {
        requests += if (darkContent) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
        onChanged?.invoke()
    }

    internal fun pop() {
        requests.removeLastOrNull()
        onChanged?.invoke()
    }
}
