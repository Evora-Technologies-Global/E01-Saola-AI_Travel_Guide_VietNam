package com.evora.technologies.saola.data.platform

import com.evora.technologies.saola.domain.model.AppLanguage
import java.util.Locale

/**
 * `Locale.getDefault()` rather than a `Context` configuration, for two reasons. It needs
 * no injection, so this stays a plain function like the rest of `data/platform/`. And it
 * already accounts for Android 13's per-app language: when one is set, the framework
 * applies it to the default locale of the process, so a traveller who has pinned this app
 * to English on a Vietnamese phone gets English answers rather than the phone's answer.
 */
internal actual fun deviceLanguage(): AppLanguage =
    languageForTag(Locale.getDefault().language)
