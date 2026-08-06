package com.evora.technologies.saola.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.evora.technologies.saola.platform.IosStatusBarStyle

@Composable
actual fun DefaultSystemBarIcons(darkIcons: Boolean) {
    // Assigned rather than pushed. On the stack it would be popped by the *next* screen to
    // close after a theme change, because a list of requests cannot tell whose entry is
    // whose — and the traveller would be left with whatever the app shipped with.
    DisposableEffect(darkIcons) {
        IosStatusBarStyle.setBase(darkContent = darkIcons)
        onDispose { }
    }
}

@Composable
actual fun PinSystemBarIcons(darkIcons: Boolean) {
    // iOS resolves the status bar style through the hosting UIViewController rather than
    // through a window flag, so the request is published here and applied by
    // `SaolaViewController`, which overrides `preferredStatusBarStyle`.
    DisposableEffect(darkIcons) {
        IosStatusBarStyle.push(darkContent = darkIcons)
        onDispose { IosStatusBarStyle.pop() }
    }
}
