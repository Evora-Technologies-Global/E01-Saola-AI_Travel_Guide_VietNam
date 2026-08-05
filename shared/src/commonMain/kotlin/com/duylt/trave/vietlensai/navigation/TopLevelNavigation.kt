package com.duylt.trave.vietlensai.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * What it means to move between the app's four places — the same answer for both shells.
 *
 * This is the half of navigation that is *not* arrangement. A bottom bar and a rail are two
 * ways of drawing the same four destinations, but "never stack a duplicate tab", "remember
 * where the traveller was inside each one" and "the lens with nothing behind it" are the
 * app's behaviour, and they were private to `mobile/navigation/VietLensApp.kt` while it was
 * the only shell. Copied into the tablet shell they would be a second answer to each
 * question, and the second answer is the one nobody re-reads (`LLM.md` §3).
 *
 * They live beside [Routes] rather than in it because that file is the route *table* — a
 * `String` per destination and nothing that needs a controller to mean anything.
 */

/**
 * Whether the destination on screen is one of the four, chrome and all.
 *
 * Both shells hide their navigation on a detail screen rather than pinning it. A discovery
 * page is a reading experience — a story about where the traveller is standing — and a bar
 * or rail sitting beside it invites people to leave before they have read anything.
 *
 * Walks the `hierarchy` rather than comparing `route` directly so a destination nested in a
 * future sub-graph still answers for the tab it belongs to.
 */
fun NavDestination?.isTopLevel(): Boolean =
    this?.hierarchy?.any { destination -> destination.route in Routes.TOP_LEVEL } == true

/**
 * Standard tab behaviour: never stack duplicate tabs, and remember where the traveller was
 * inside each one.
 *
 * @param route one of [Routes.TOP_LEVEL]. Passing anything else saves the state of a screen
 *   no bar or rail can bring back.
 */
fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Back to the lens with nothing behind it — the app as it is on a cold start.
 *
 * [navigateToTopLevel] would not do: it pops the current stack *with* `saveState`, so a
 * passport reached from the journal is filed away under the journal tab and comes back the
 * next time that tab is tapped. The traveller pressed a button that said "open the lens", and
 * a tab they press a minute later should give them the tab, not the screen they left.
 *
 * The saved stacks are dropped first: `popUpTo` clears what is on the stack now, but anything
 * already filed away by an earlier tab switch would still be restored.
 */
fun NavHostController.restartAtLens() {
    Routes.TOP_LEVEL.forEach { route -> clearBackStack(route) }
    navigate(Routes.LENS) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
        launchSingleTop = true
    }
}
