package com.duylt.trave.vietlensai.tablet.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.duylt.trave.vietlensai.mobile.feature.translate.TranslationRoute
import com.duylt.trave.vietlensai.navigation.Routes
import com.duylt.trave.vietlensai.navigation.navigateToTopLevel
import com.duylt.trave.vietlensai.navigation.restartAtLens
import com.duylt.trave.vietlensai.tablet.feature.camera.LensTabletRoute
import com.duylt.trave.vietlensai.tablet.feature.discovery.DiscoveryTabletRoute
import com.duylt.trave.vietlensai.tablet.feature.explore.ExploreTabletRoute
import com.duylt.trave.vietlensai.tablet.feature.journal.JournalPane
import com.duylt.trave.vietlensai.tablet.feature.journal.JournalTabletRoute
import com.duylt.trave.vietlensai.tablet.feature.settings.SettingsTabletRoute
import com.duylt.trave.vietlensai.tablet.feature.sovereignty.SovereigntyTabletRoute

/**
 * The same graph as the phone's, and it has to stay the same graph.
 *
 * Every route, every argument and every default below is a copy of the one in
 * `mobile/navigation/VietLensApp.kt`, and that is not tidiness — it is what makes QĐ-4 work.
 * `NavController.setGraph` compares the incoming graph with the one it holds **structurally**:
 * matching routes and arguments take the update-in-place path, which swaps each destination's
 * composable and leaves `backQueue` alone. Anything that differs — a route only one shell
 * declares, a `navArgument` default typed differently — fails that comparison, and the
 * controller answers by clearing the back stack. The symptom is not a crash: it is the
 * traveller being dropped back on the lens every time they turn the iPad, which is precisely
 * what [com.duylt.trave.vietlensai.navigation.VietLensRoot] hands one controller to both
 * shells to prevent.
 *
 * **Destinations still pointing at the phone's screen are deliberate and temporary.** They
 * make the tablet usable from phase 03 rather than after five more, and they mean the rail
 * can be judged on a real device now. Phases 04–08 replaced them one at a time — lens (04),
 * discovery and chat (05), journal, passport and collection (06), explore (07), settings and
 * sovereignty (08) — and the route table above is what stayed fixed while they did.
 * `Routes.TRANSLATION` is the last one still on the phone's screen: it is a full-bleed
 * photograph with the Vietnamese on it replaced in place, which is the same picture at any
 * window size, and it is phase 10's to look at in landscape rather than this plan's.
 *
 * It has a file of its own, unlike the phone's, for the same reason: those five phases run in
 * parallel and each one rewrites a `composable` block here. One file per concern means five
 * branches editing five neighbourhoods rather than five branches editing the shell.
 */
@Composable
internal fun VietLensTabletNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.LENS,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(Routes.LENS) {
            LensTabletRoute(
                onDiscoveryCaptured = { id -> navController.navigate(Routes.discovery(id)) },
                onTranslationCaptured = { imagePath, from, to ->
                    navController.navigate(Routes.translation(imagePath, from, to))
                },
                onOpenJournal = { navController.navigateToTopLevel(Routes.JOURNAL) },
                onOpenSettings = { navController.navigateToTopLevel(Routes.SETTINGS) },
            )
        }

        // Three routes, one arrangement, differing only in which pane starts open.
        //
        // On a large window the passport and the collection are not screens: they are the
        // flexible pane of the journal, because all three are the same act of looking back
        // over a trip and pushing one of them full-screen over the other two would make
        // exactly the rivalry `Destinations.kt` put them together to avoid.
        //
        // All three stay registered, and that is not decoration. The two shells' graphs have
        // to compare structurally equal or `setGraph` clears the back stack on every resize —
        // and a deep link into the passport has to land somewhere on both form factors.
        composable(Routes.JOURNAL) {
            JournalTabletRoute(
                initialPane = JournalPane.PASSPORT,
                onOpenDiscovery = { id -> navController.navigate(Routes.discovery(id)) },
                onOpenLens = { navController.navigateToTopLevel(Routes.LENS) },
                onOpenSovereignty = { navController.navigate(Routes.SOVEREIGNTY) },
            )
        }

        composable(Routes.COLLECTION) {
            JournalTabletRoute(
                initialPane = JournalPane.COLLECTION,
                onOpenDiscovery = { id -> navController.navigate(Routes.discovery(id)) },
                // `restartAtLens`, not a tab switch, for the reason the phone's collection
                // gives: "go and photograph this" ends the errand the traveller was on.
                onOpenLens = navController::restartAtLens,
                onOpenSovereignty = { navController.navigate(Routes.SOVEREIGNTY) },
            )
        }

        composable(Routes.PASSPORT) {
            JournalTabletRoute(
                initialPane = JournalPane.PASSPORT,
                onOpenDiscovery = { id -> navController.navigate(Routes.discovery(id)) },
                onOpenLens = navController::restartAtLens,
                onOpenSovereignty = { navController.navigate(Routes.SOVEREIGNTY) },
            )
        }

        composable(Routes.SOVEREIGNTY) {
            SovereigntyTabletRoute(onClose = navController::popBackStack)
        }

        composable(Routes.EXPLORE) {
            ExploreTabletRoute()
        }

        composable(Routes.SETTINGS) {
            SettingsTabletRoute(
                onOpenSovereignty = { navController.navigate(Routes.SOVEREIGNTY) },
            )
        }

        composable(Routes.DISCOVERY) { entry ->
            DiscoveryTabletRoute(
                discoveryId = entry.discoveryId(),
                onBack = navController::popBackStack,
            )
        }

        // The chat route stays, and it opens the two-pane discovery rather than a chat screen.
        //
        // It has to stay for two reasons. A deep link into a conversation must land somewhere
        // on both form factors, and — the one that matters more — a traveller reading the chat
        // on a phone who turns the device into a large window arrives here. Dropping the route
        // would fail the structural comparison `setGraph` makes between the two shells and
        // clear the back stack on every resize, which is precisely what QĐ-4 exists to prevent.
        //
        // What it opens is the discovery it names, with the guide already beside it. That is
        // the same conversation, at the same place in it, in the arrangement this window has.
        composable(Routes.CHAT) { entry ->
            DiscoveryTabletRoute(
                discoveryId = entry.discoveryId(),
                onBack = navController::popBackStack,
            )
        }

        composable(
            route = Routes.TRANSLATION,
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
 * The discovery this entry names, read once and handed to both of the screen's ViewModels.
 *
 * Read here rather than inside the screen because two routes reach the same arrangement —
 * `discovery/{id}` and `chat/{id}` — and this is the one place that knows which of them was
 * matched. `savedStateHandle` rather than `arguments`: it is the same handle Koin hands the
 * `DiscoveryViewModel`, so the pane and its ViewModel cannot read two different answers, and
 * it is typed rather than a platform bundle.
 */
private fun NavBackStackEntry.discoveryId(): String =
    checkNotNull(savedStateHandle.get<String>(Routes.ARG_DISCOVERY_ID)) {
        "Route ${destination.route} reached the discovery pane without a ${Routes.ARG_DISCOVERY_ID}"
    }
