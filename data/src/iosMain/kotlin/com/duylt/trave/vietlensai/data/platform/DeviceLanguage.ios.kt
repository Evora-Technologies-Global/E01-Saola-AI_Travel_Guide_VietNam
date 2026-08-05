package com.duylt.trave.vietlensai.data.platform

import com.duylt.trave.vietlensai.domain.model.AppLanguage
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

/**
 * `NSBundle.mainBundle.preferredLocalizations` first, and only then the device list.
 *
 * The two differ in exactly the case that matters. `NSLocale.preferredLanguages` is what
 * the *traveller* has asked for — on a phone set to German it starts with "de" — while
 * `preferredLocalizations` is what the app can actually honour, resolved against the
 * localizations it ships. Compose Resources answers the same question the same way, so
 * reading the bundle keeps narration on whichever string table the screen is drawing
 * from. Asking the device directly would answer "de", and [languageForTag] would send
 * that to English anyway; but it would also answer "de" for a phone whose *second*
 * preference is one of the eight, where the bundle resolves to that language and the
 * screen is drawn in it.
 */
internal actual fun deviceLanguage(): AppLanguage {
    val fromBundle = NSBundle.mainBundle.preferredLocalizations.firstOrNull() as? String
    val tag = fromBundle ?: NSLocale.preferredLanguages.firstOrNull() as? String
    return languageForTag(tag)
}
