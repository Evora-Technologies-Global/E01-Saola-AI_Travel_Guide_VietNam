package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.duylt.trave.vietlensai.platform.IosStatusBarStyle

@Composable
actual fun PinSystemBarIcons(darkIcons: Boolean) {
    // iOS resolves the status bar style through the hosting UIViewController rather than
    // through a window flag, so the request is published here and applied by
    // `VietLensViewController`, which overrides `preferredStatusBarStyle`.
    DisposableEffect(darkIcons) {
        IosStatusBarStyle.push(darkContent = darkIcons)
        onDispose { IosStatusBarStyle.pop() }
    }
}
