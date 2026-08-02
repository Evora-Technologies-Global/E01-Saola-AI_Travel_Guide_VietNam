package com.duylt.trave.vietlensai.navigation

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
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.nav_explore
import com.duylt.trave.vietlensai.resources.nav_journal
import com.duylt.trave.vietlensai.resources.nav_lens
import com.duylt.trave.vietlensai.resources.nav_settings

/**
 * Every route in the app.
 *
 * String routes with explicit builders rather than type-safe serializable routes:
 * the graph is small, and a plain `String` route keeps deep-linking from a
 * notification or the journal trivial.
 */
object Routes {
    const val LENS = "lens"
    const val JOURNAL = "journal"
    const val PASSPORT = "passport"
    const val COLLECTION = "collection"
    const val SOVEREIGNTY = "sovereignty"
    const val EXPLORE = "explore"
    const val SETTINGS = "settings"

    private const val DISCOVERY_BASE = "discovery"
    private const val CHAT_BASE = "chat"
    private const val TRANSLATION_BASE = "translation"

    const val ARG_DISCOVERY_ID = "discoveryId"
    const val ARG_IMAGE_PATH = "imagePath"
    const val ARG_SOURCE_LANGUAGE = "from"
    const val ARG_TARGET_LANGUAGE = "to"

    const val DISCOVERY = "$DISCOVERY_BASE/{$ARG_DISCOVERY_ID}"
    const val CHAT = "$CHAT_BASE/{$ARG_DISCOVERY_ID}"

    /**
     * Translation opens on a photo, not on a saved result.
     *
     * Query parameters rather than path segments because a file path is full of
     * slashes: as a path segment it would have to be escaped, and an escaped slash
     * stops the route matching at all.
     */
    const val TRANSLATION =
        "$TRANSLATION_BASE?$ARG_IMAGE_PATH={$ARG_IMAGE_PATH}" +
            "&$ARG_SOURCE_LANGUAGE={$ARG_SOURCE_LANGUAGE}" +
            "&$ARG_TARGET_LANGUAGE={$ARG_TARGET_LANGUAGE}"

    fun discovery(id: String): String = "$DISCOVERY_BASE/$id"
    fun chat(discoveryId: String): String = "$CHAT_BASE/$discoveryId"

    /** @param from empty for "detect it from the photo". */
    fun translation(imagePath: String, from: String, to: String): String =
        "$TRANSLATION_BASE?$ARG_IMAGE_PATH=${imagePath.urlEncoded()}" +
            "&$ARG_SOURCE_LANGUAGE=$from" +
            "&$ARG_TARGET_LANGUAGE=$to"
}

/**
 * The four tabs in the bottom bar.
 *
 * The passport and the culture collection are deliberately not among them: both are
 * reached from the journal, because neither means anything until there are captures
 * behind it, and the count sitting next to each is what makes that link obvious.
 * They are also the same kind of thing — a way of looking back over a trip — so
 * putting them side by side is what keeps them from reading as rival features.
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
 * The bar is hidden on immersive screens (result, chat, translation) so nothing
 * competes with the content the traveller came for.
 */
fun NavDestination?.isTopLevel(): Boolean =
    this?.hierarchy?.any { destination ->
        TopLevelDestination.entries.any { it.route == destination.route }
    } == true

fun NavDestination?.topLevelDestination(): TopLevelDestination? =
    TopLevelDestination.entries.firstOrNull { top ->
        this?.hierarchy?.any { it.route == top.route } == true
    }

/**
 * Percent-encodes a value for a query parameter.
 *
 * `Uri.encode` is Android-only, so the rule is spelled out: everything outside the
 * unreserved set of RFC 3986 is escaped, byte by byte over the UTF-8 encoding. A capture
 * path is full of slashes, and an unescaped one would end the query value early and leave
 * the route unmatched.
 */
private fun String.urlEncoded(): String = buildString {
    for (byte in this@urlEncoded.encodeToByteArray()) {
        val char = byte.toInt().toChar()
        if (char.isLetterOrDigit() && char.code < 128 || char in "-_.~") {
            append(char)
        } else {
            append('%')
            append(HEX[(byte.toInt() shr 4) and 0xF])
            append(HEX[byte.toInt() and 0xF])
        }
    }
}

private const val HEX = "0123456789ABCDEF"
