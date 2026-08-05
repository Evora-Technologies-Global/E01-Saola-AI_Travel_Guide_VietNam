package com.duylt.trave.vietlensai.data.platform

import com.duylt.trave.vietlensai.domain.model.AppLanguage

/**
 * The language the phone is set to, as one of the eight the guide speaks.
 *
 * This is what the whole app narrates in: there is no in-app language setting, so a
 * Japanese phone gets a Japanese interface *and* Japanese answers, and a Vietnamese one
 * gets both in Vietnamese.
 *
 * It has to give the same answer as `uiLanguage()` in `:shared`, which reads the *string
 * table* to find out which file Compose Resources actually picked. That is the more
 * accurate source, but it is only readable inside composition; this one is the answer
 * for everything below the UI — repositories, prompts and the TTS voice. They agree
 * because both resolve to a shipped language or else English, and English is where
 * Compose Resources sends every locale the app has no `values-*` for. A phone in German
 * draws English labels, so it must be narrated to in English too.
 */
internal expect fun deviceLanguage(): AppLanguage

/**
 * Shared by both actuals so the fallback rule is written once.
 *
 * Only the language subtag is compared: a platform answers with a full BCP-47 tag —
 * "vi-VN", "zh-Hans-CN", "pt-BR" — and the app ships one table per language, not per
 * region or script, so everything after the first separator is dropped. What remains is
 * matched *whole*, never as a prefix. Prefix matching would accept "vie" for Vietnamese,
 * but it would equally accept "kok" (Konkani) as Korean and "the" as Thai, narrating to
 * someone in a language they never asked for. Both platforms hand over two-letter codes
 * here, so there is nothing to gain and a real way to be wrong — see `DeviceLanguageTest`.
 *
 * Anything the app does not ship falls to English, which is the table Compose Resources
 * will have resolved for that locale anyway.
 */
internal fun languageForTag(tag: String?): AppLanguage {
    val normalised = tag?.replace('_', '-')?.substringBefore('-')?.lowercase()
    return AppLanguage.entries.firstOrNull { it.code == normalised } ?: AppLanguage.ENGLISH
}
