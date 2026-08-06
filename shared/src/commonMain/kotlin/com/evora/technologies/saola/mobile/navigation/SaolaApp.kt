package com.evora.technologies.saola.mobile.navigation

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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.evora.technologies.saola.mobile.feature.camera.LensRoute
import com.evora.technologies.saola.mobile.feature.chat.ChatRoute
import com.evora.technologies.saola.mobile.feature.collection.CollectionRoute
import com.evora.technologies.saola.mobile.feature.discovery.DiscoveryRoute
import com.evora.technologies.saola.mobile.feature.explore.ExploreRoute
import com.evora.technologies.saola.mobile.feature.journal.JournalRoute
import com.evora.technologies.saola.mobile.feature.passport.PassportRoute
import com.evora.technologies.saola.mobile.feature.licenses.LicensesRoute
import com.evora.technologies.saola.mobile.feature.settings.SettingsRoute
import com.evora.technologies.saola.mobile.feature.sovereignty.SovereigntyRoute
import com.evora.technologies.saola.mobile.feature.translate.TranslationRoute
import com.evora.technologies.saola.navigation.Routes
import com.evora.technologies.saola.navigation.isTopLevel
import com.evora.technologies.saola.navigation.navigateToTopLevel
import com.evora.technologies.saola.navigation.restartAtLens

/**
 * The phone shell: four tabs, with detail screens pushed on top full-bleed.
 *
 * The bottom bar hides on detail screens rather than staying pinned. A discovery
 * page is a reading experience — a story about where the traveller is standing —
 * and a persistent tab bar under it invites people to leave before they have read
 * anything. The passport map is pushed the same way, from the journal.
 *
 * [navController] has no default, and that is the point: it is created above the
 * branch fork in [com.evora.technologies.saola.navigation.SaolaRoot] and handed to
 * whichever shell is on screen. A default here would let a caller create a second
 * controller by omission, and the traveller would lose their place every time the
 * window changed size.
 */
@Composable
fun SaolaApp(navController: NavHostController) {
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
                SaolaNavigationBar(
                    current = currentDestination.topLevelDestination(),
                    onSelect = { destination -> navController.navigateToTopLevel(destination.route) },
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
            SaolaNavHost(navController = navController)
        }
    }
}

@Composable
private fun SaolaNavigationBar(
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
private fun SaolaNavHost(navController: NavHostController) {
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
                onOpenJournal = { navController.navigateToTopLevel(Routes.JOURNAL) },
            )
        }

        composable(Routes.JOURNAL) {
            JournalRoute(
                onOpenDiscovery = { id -> navController.navigate(Routes.discovery(id)) },
                onOpenLens = { navController.navigateToTopLevel(Routes.LENS) },
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
                onOpenLicenses = { navController.navigate(Routes.LICENSES) },
            )
        }

        composable(Routes.LICENSES) {
            LicensesRoute(onBack = navController::popBackStack)
        }

        composable(Routes.DISCOVERY) {
            DiscoveryRoute(
                onBack = navController::popBackStack,
                onOpenChat = { id -> navController.navigate(Routes.chat(id)) },
                onOpenCollection = { navController.navigate(Routes.COLLECTION) },
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
