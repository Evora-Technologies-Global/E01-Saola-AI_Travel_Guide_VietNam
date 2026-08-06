package com.evora.technologies.saola.navigation

/**
 * Every route in the app.
 *
 * String routes with explicit builders rather than type-safe serializable routes:
 * the graph is small, and a plain `String` route keeps deep-linking from a
 * notification or the journal trivial.
 *
 * Shared by both presentation branches. `mobile/` registers these routes under a
 * bottom bar and `tablet/` under a navigation rail, but the route strings themselves
 * are one set — declaring them twice is how a deep link ends up working on one form
 * factor and not the other.
 */
object Routes {
    const val LENS = "lens"
    const val JOURNAL = "journal"
    const val PASSPORT = "passport"
    const val COLLECTION = "collection"
    const val SOVEREIGNTY = "sovereignty"
    const val EXPLORE = "explore"
    const val SETTINGS = "settings"

    /**
     * Where the data credits are stated in full.
     *
     * Reached from Settings only. Deliberately not in [TOP_LEVEL] and deliberately not a
     * pane of anything: it is a licence obligation the traveller reads once, not a place
     * they come back to — and both shells open the same document, which is why it is one
     * of the two routes registered identically rather than mapped per branch.
     */
    const val LICENSES = "licenses"

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

    /**
     * The four places the traveller can always get back to, in the order both shells show them.
     *
     * Membership is an app fact, not an arrangement one — the phone puts these on a bottom bar
     * and the tablet on a rail, but a route that is a destination on one form factor and a
     * pushed detail screen on the other would give the same back button two meanings. It lives
     * here so `isTopLevel()` has one answer, and so `TopLevelDestination` and `RailDestination`
     * are two renderings of a list rather than two lists.
     *
     * The passport and the collection are deliberately absent: both are reached from the
     * journal, and neither means anything until there are captures behind it.
     */
    val TOP_LEVEL: List<String> = listOf(LENS, JOURNAL, EXPLORE, SETTINGS)

    fun discovery(id: String): String = "$DISCOVERY_BASE/$id"
    fun chat(discoveryId: String): String = "$CHAT_BASE/$discoveryId"

    /** @param from empty for "detect it from the photo". */
    fun translation(imagePath: String, from: String, to: String): String =
        "$TRANSLATION_BASE?$ARG_IMAGE_PATH=${imagePath.urlEncoded()}" +
            "&$ARG_SOURCE_LANGUAGE=$from" +
            "&$ARG_TARGET_LANGUAGE=$to"
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
