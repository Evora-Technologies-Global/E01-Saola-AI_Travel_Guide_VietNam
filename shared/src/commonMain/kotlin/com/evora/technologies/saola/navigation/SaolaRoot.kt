package com.evora.technologies.saola.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.evora.technologies.saola.core.window.WindowClass
import com.evora.technologies.saola.core.window.rememberWindowClass
import com.evora.technologies.saola.mobile.navigation.SaolaApp
import com.evora.technologies.saola.tablet.navigation.SaolaTabletApp

/**
 * The one composable both platforms call, and the only place the branch is chosen.
 *
 * `MainActivity` and `MainViewController` each call this and nothing below it, for the same
 * reason both pass exactly `appModules(isDebug)` (`LLM.md` §6): a fork written twice is a
 * fork that ends up meaning two different things, and the second one is found by a traveller
 * rather than by a build.
 *
 * **The `NavHostController` is created above the fork on purpose.** Both shells register the
 * same `Routes`, so handing them one controller means resizing the window changes the
 * arrangement and nothing else — a traveller reading a discovery who rotates the iPad is
 * still reading that discovery afterwards, with their back stack intact. Two independent
 * `NavHost`s would send them back to the lens every time the device turned.
 *
 * `BoxWithConstraints` measures the window rather than asking the platform what kind of
 * device this is, which is also the only form the question takes in `commonMain` — see
 * [rememberWindowClass]. It is one extra measure pass, at the root, outside every scroll
 * container.
 */
@Composable
fun SaolaRoot(navController: NavHostController = rememberNavController()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        when (rememberWindowClass(maxWidth, maxHeight)) {
            WindowClass.COMPACT -> SaolaApp(navController)
            WindowClass.EXPANDED -> SaolaTabletApp(navController)
        }
    }
}
