package com.duylt.trave.vietlensai.tablet.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.duylt.trave.vietlensai.navigation.Routes
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.nav_explore
import com.duylt.trave.vietlensai.resources.nav_journal
import com.duylt.trave.vietlensai.resources.nav_lens
import com.duylt.trave.vietlensai.resources.nav_settings
import org.jetbrains.compose.resources.StringResource

/**
 * The four places on the rail — the same four the phone puts on its bottom bar.
 *
 * Same routes, same labels, same icons; a separate enum only because a rail and a bar are
 * two arrangements and each owns its own list (`LLM.md` §7). The wireframe's rail carries a
 * fifth item, the sovereignty seal, and it is deliberately **not** an entry here: it opens a
 * statement rather than switching place, so putting it in this enum would light it up as the
 * selected tab and make the rail's selection state lie. It is drawn by
 * [VietLensNavigationRail] on its own, below the four.
 *
 * **Which four is [Routes.TOP_LEVEL]'s decision, not this file's**, and the entries below
 * cover it in the same order. The cost of them drifting is a place that exists on one form
 * factor and not the other: rotating the iPad would then drop the traveller onto a route
 * whose tab is not on the rail, and nothing about it would appear in a build log.
 */
enum class RailDestination(
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
 * Which rail item to light up, or null on a detail screen where no rail is drawn.
 *
 * **This, not `isTopLevel()`, is what decides whether the tablet draws a rail at all**, and
 * the two answers differ on exactly two routes. `Routes.PASSPORT` and `Routes.COLLECTION` are
 * detail screens on a phone — pushed on top of the journal, bar hidden — and on a large window
 * they *are* the journal, with the pane beside the day column set to one or the other. Judged
 * by [com.duylt.trave.vietlensai.navigation.isTopLevel] the rail would vanish the moment a
 * traveller deep-linked into the passport, or turned a phone-sized window holding the
 * collection into a large one, and what they would be left with is a top-level screen with no
 * way off it but the system back gesture. Reproduced on a Pixel Tablet before this existed.
 *
 * It stays out of [Routes.TOP_LEVEL] because membership there is an app fact and the phone's
 * answer is the right one for the phone: adding the two would put them on the bottom bar. This
 * is the *rail's* reading of the same table, which is what §7 means by each arrangement owning
 * its own list.
 */
fun NavDestination?.railDestination(): RailDestination? {
    val routes = this?.hierarchy?.mapNotNull { it.route }?.toList() ?: return null
    RailDestination.entries.firstOrNull { rail -> rail.route in routes }?.let { return it }
    return if (routes.any { it in JOURNAL_FAMILY }) RailDestination.JOURNAL else null
}

/**
 * The routes that are the journal wearing another name on a large window.
 *
 * See `tablet/feature/journal/JournalTabletScreen.kt`: all three open the same two-pane
 * arrangement and differ only in which pane starts open.
 */
private val JOURNAL_FAMILY = setOf(Routes.PASSPORT, Routes.COLLECTION)
