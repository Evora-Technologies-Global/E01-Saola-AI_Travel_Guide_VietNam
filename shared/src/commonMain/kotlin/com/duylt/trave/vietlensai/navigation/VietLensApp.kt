package com.duylt.trave.vietlensai.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.duylt.trave.vietlensai.feature.camera.LensRoute
import com.duylt.trave.vietlensai.feature.chat.ChatRoute
import com.duylt.trave.vietlensai.feature.collection.CollectionRoute
import com.duylt.trave.vietlensai.feature.discovery.DiscoveryRoute
import com.duylt.trave.vietlensai.feature.explore.ExploreRoute
import com.duylt.trave.vietlensai.feature.journal.JournalRoute
import com.duylt.trave.vietlensai.feature.passport.PassportRoute
import com.duylt.trave.vietlensai.feature.settings.SettingsRoute
import com.duylt.trave.vietlensai.feature.sovereignty.SovereigntyRoute
import com.duylt.trave.vietlensai.feature.translate.TranslationRoute

/**
 * The app shell: four tabs, with detail screens pushed on top full-bleed.
 *
 * The bottom bar hides on detail screens rather than staying pinned. A discovery
 * page is a reading experience — a story about where the traveller is standing —
 * and a persistent tab bar under it invites people to leave before they have read
 * anything. The passport map is pushed the same way, from the journal.
 */
@Composable
fun VietLensApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination.isTopLevel()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // The camera preview must reach the screen edges, so the scaffold does not
        // apply window insets itself; each screen decides what to inset.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                VietLensNavigationBar(
                    current = currentDestination.topLevelDestination(),
                    onSelect = { destination -> navController.navigateToTopLevel(destination) },
                )
            }
        },
    ) { padding ->
        Box(
            // Straight from the scaffold, with no flag of our own in front of it.
            // `showBottomBar` turns false the instant a detail route is pushed, while
            // the bar itself takes another few hundred milliseconds to slide away —
            // and a page that is scrolled to its end jumps by a bar's height the
            // moment the space under it is handed back. The scaffold already reports
            // the bar's real height for as long as it is on screen, animation
            // included, and zero once it is gone.
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
        ) {
            VietLensNavHost(navController = navController)
        }
    }
}

@Composable
private fun VietLensNavigationBar(
    current: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val selected = destination == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}

@Composable
private fun VietLensNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LENS,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(Routes.LENS) {
            LensRoute(
                onDiscoveryCaptured = { id -> navController.navigate(Routes.discovery(id)) },
                onTranslationCaptured = { imagePath, from, to ->
                    navController.navigate(Routes.translation(imagePath, from, to))
                },
                // The recent-capture pile hands over to the journal rather than
                // opening one photo: it is a doorway to the history, not an index.
                onOpenJournal = { navController.navigateToTopLevel(TopLevelDestination.JOURNAL) },
                onOpenSettings = { navController.navigateToTopLevel(TopLevelDestination.SETTINGS) },
            )
        }

        composable(Routes.JOURNAL) {
            JournalRoute(
                onOpenDiscovery = { id -> navController.navigate(Routes.discovery(id)) },
                onOpenLens = { navController.navigateToTopLevel(TopLevelDestination.LENS) },
                onOpenPassport = { navController.navigate(Routes.PASSPORT) },
                onOpenCollection = { navController.navigate(Routes.COLLECTION) },
            )
        }

        composable(Routes.COLLECTION) {
            CollectionRoute(
                onBack = navController::popBackStack,
                // Same reasoning as the passport's: "go and photograph this" ends the
                // errand the traveller was on, so the lens opens with nothing behind
                // it rather than filing the board away under the journal tab.
                onOpenLens = navController::restartAtLens,
                onOpenSovereignty = { navController.navigate(Routes.SOVEREIGNTY) },
                // Pushed on top of the board: coming back should land on the
                // collection where it was left, with the tile still under their thumb.
                onOpenDiscovery = { id -> navController.navigate(Routes.discovery(id)) },
            )
        }

        composable(Routes.PASSPORT) {
            PassportRoute(
                onBack = navController::popBackStack,
                // Not the tab switch the other screens use. "Go and photograph this
                // province" ends the errand the traveller was on: they came in from
                // the journal, drilled into the map, and what they do next is take a
                // picture. Keeping the passport in a saved stack means the journal tab
                // reopens the map instead of the journal.
                onOpenLens = navController::restartAtLens,
                onOpenSovereignty = { navController.navigate(Routes.SOVEREIGNTY) },
                // Pushed on top of the map rather than routed through the journal: the
                // province panel is already a list of that province's discoveries, and
                // coming back should land on the map with the panel still open.
                onOpenDiscovery = { id -> navController.navigate(Routes.discovery(id)) },
            )
        }

        composable(Routes.SOVEREIGNTY) {
            SovereigntyRoute(onBack = navController::popBackStack)
        }

        composable(Routes.EXPLORE) {
            ExploreRoute()
        }

        composable(Routes.SETTINGS) {
            SettingsRoute(
                onOpenSovereignty = { navController.navigate(Routes.SOVEREIGNTY) },
            )
        }

        composable(Routes.DISCOVERY) {
            DiscoveryRoute(
                onBack = navController::popBackStack,
                onOpenChat = { id -> navController.navigate(Routes.chat(id)) },
            )
        }

        composable(Routes.CHAT) {
            ChatRoute(onBack = navController::popBackStack)
        }

        composable(
            route = Routes.TRANSLATION,
            // Declared with defaults so the source may legitimately arrive blank:
            // "detect it from the photo" is a choice, not a missing argument.
            arguments = listOf(
                navArgument(Routes.ARG_IMAGE_PATH) { defaultValue = "" },
                navArgument(Routes.ARG_SOURCE_LANGUAGE) { defaultValue = "" },
                navArgument(Routes.ARG_TARGET_LANGUAGE) { defaultValue = "" },
            ),
        ) {
            TranslationRoute(onBack = navController::popBackStack)
        }
    }
}

/**
 * Standard tab behaviour: never stack duplicate tabs, and remember where the user
 * was inside each one.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Back to the lens with nothing behind it — the app as it is on a cold start.
 *
 * [navigateToTopLevel] would not do: it pops the current stack *with* `saveState`,
 * so a passport reached from the journal is filed away under the journal tab and
 * comes back the next time that tab is tapped. The traveller pressed a button that
 * said "open the lens", and a tab they press a minute later should give them the tab,
 * not the screen they left.
 *
 * The saved stacks are dropped first: `popUpTo` clears what is on the stack now, but
 * anything already filed away by an earlier tab switch would still be restored.
 */
private fun NavHostController.restartAtLens() {
    TopLevelDestination.entries.forEach { clearBackStack(it.route) }
    navigate(Routes.LENS) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}
