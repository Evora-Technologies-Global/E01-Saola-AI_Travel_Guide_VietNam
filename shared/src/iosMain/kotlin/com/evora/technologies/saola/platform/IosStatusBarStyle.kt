package com.evora.technologies.saola.platform

import platform.UIKit.UIStatusBarStyle
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent

/**
 * The status bar style the topmost screen has asked for.
 *
 * Android lets a composable flip the bar icons through a window flag; iOS resolves the
 * style by asking the view controller hierarchy, so a composable can only publish a
 * request and let the hosting controller answer with it. `SaolaViewController` reads
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

    /**
     * What an empty stack falls back to: the theme's own answer, published by
     * `DefaultSystemBarIcons`.
     *
     * Held beside the stack rather than at the bottom of it. A theme change has to be able
     * to replace this while a screen is pinned, and `pop` removes the last entry — so a base
     * pushed like a request would be popped by whichever screen closed next.
     *
     * Light content until the theme resolves, which is the app's dark-first default and the
     * value the launch screen is drawn with.
     */
    private var base: UIStatusBarStyle = UIStatusBarStyleLightContent

    /** Called by the hosting view controller so it can re-ask iOS for the bar style. */
    var onChanged: (() -> Unit)? = null

    val current: UIStatusBarStyle
        get() = requests.lastOrNull() ?: base

    internal fun setBase(darkContent: Boolean) {
        val style = if (darkContent) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
        if (style == base) return
        base = style
        onChanged?.invoke()
    }

    internal fun push(darkContent: Boolean) {
        requests += if (darkContent) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
        onChanged?.invoke()
    }

    internal fun pop() {
        requests.removeLastOrNull()
        onChanged?.invoke()
    }
}
