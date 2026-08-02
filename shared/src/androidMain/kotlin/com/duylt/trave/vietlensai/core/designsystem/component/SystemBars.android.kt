package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.duylt.trave.vietlensai.core.util.findActivity

@Composable
actual fun PinSystemBarIcons(darkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view, darkIcons) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = darkIcons
        controller?.isAppearanceLightNavigationBars = darkIcons
        onDispose {
            previousStatus?.let { controller?.isAppearanceLightStatusBars = it }
            previousNavigation?.let { controller?.isAppearanceLightNavigationBars = it }
        }
    }
}
