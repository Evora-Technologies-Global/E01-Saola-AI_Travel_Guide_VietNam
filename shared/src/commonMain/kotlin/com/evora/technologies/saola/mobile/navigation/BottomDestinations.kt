package com.evora.technologies.saola.mobile.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import org.jetbrains.compose.resources.StringResource
import com.evora.technologies.saola.navigation.Routes
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.nav_explore
import com.evora.technologies.saola.resources.nav_journal
import com.evora.technologies.saola.resources.nav_lens
import com.evora.technologies.saola.resources.nav_settings

/**
 * The four tabs in the bottom bar.
 *
 * The passport and the culture collection are deliberately not among them: both are
 * reached from the journal, because neither means anything until there are captures
 * behind it, and the count sitting next to each is what makes that link obvious.
 * They are also the same kind of thing — a way of looking back over a trip — so
 * putting them side by side is what keeps them from reading as rival features.
 *
 * This lives in `mobile/` rather than beside [Routes] because a bottom bar is one
 * branch's answer to the question, not the app's: the tablet shell puts the same four
 * places on a navigation rail and needs its own list with its own icon sizes.
 *
 * **Which four is not this file's decision** — that is [Routes.TOP_LEVEL], and the entries
 * below must cover it in the same order. `RailDestination` in the tablet branch is held to
 * the same list, which is what keeps a fifth tab from existing on one form factor only.
 */
enum class TopLevelDestination(
    val route: String,
    val labelRes: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    LENS(
        route = Routes.LENS,
        labelRes = Res.string.nav_lens,
        selectedIcon = Icons.Filled.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt,
    ),
    JOURNAL(
        route = Routes.JOURNAL,
        labelRes = Res.string.nav_journal,
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
    ),
    EXPLORE(
        route = Routes.EXPLORE,
        labelRes = Res.string.nav_explore,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
    ),
    SETTINGS(
        route = Routes.SETTINGS,
        labelRes = Res.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
}

/**
 * Which tab to light up — or null on a detail screen, where the bar is not drawn at all.
 *
 * The paired question, "is a bar drawn here", is [com.evora.technologies.saola.navigation
 * .isTopLevel] and is shared: both shells hide their navigation on the same screens.
 */
fun NavDestination?.topLevelDestination(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { top ->
        this?.hierarchy?.any { it.route == top.route } == true
    }
