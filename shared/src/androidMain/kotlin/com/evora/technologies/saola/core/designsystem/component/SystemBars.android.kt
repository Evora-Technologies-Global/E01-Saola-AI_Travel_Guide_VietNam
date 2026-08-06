package com.evora.technologies.saola.core.designsystem.component

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.evora.technologies.saola.core.util.findActivity

@Composable
actual fun DefaultSystemBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    // Keyed rather than a `SideEffect`, which would re-apply on every recomposition of the
    // theme and overwrite a screen's pin each time. This has to be written once, when the
    // answer changes.
    DisposableEffect(view, darkIcons) {
        val controller = view.insetsController()
        controller?.isAppearanceLightStatusBars = darkIcons
        controller?.isAppearanceLightNavigationBars = darkIcons
        // Nothing to hand back: this is the base the pins sit on, and it is owned by the
        // theme, which lives as long as the window does.
        onDispose { }
    }
}

@Composable
actual fun PinSystemBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view, darkIcons) {
        val controller = view.insetsController()
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = darkIcons
        controller?.isAppearanceLightNavigationBars = darkIcons
        onDispose {
            // No safe call: a previous value exists only if the controller did.
            previousStatus?.let { controller.isAppearanceLightStatusBars = it }
            previousNavigation?.let { controller.isAppearanceLightNavigationBars = it }
        }
    }
}

/**
 * Null off a window — a Compose view hosted somewhere other than an activity, which the
 * previews and the device tests are.
 */
private fun View.insetsController(): WindowInsetsControllerCompat? =
    context.findActivity()?.window?.let { WindowCompat.getInsetsController(it, this) }
