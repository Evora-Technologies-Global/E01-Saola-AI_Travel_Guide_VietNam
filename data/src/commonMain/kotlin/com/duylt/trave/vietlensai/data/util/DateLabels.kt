package com.duylt.trave.vietlensai.data.util

import com.duylt.trave.vietlensai.domain.model.AppLanguage
import kotlinx.datetime.LocalDate

/**
 * A long-form date the way each of the app's two languages writes one.
 *
 * Hand-written rather than delegated to a platform formatter. `DateTimeFormatter` is
 * JVM-only and `NSDateFormatter` is Apple-only, so using either would mean the journal's
 * prompt read differently depending on which phone generated it — and this string goes
 * *into* a Gemini prompt, where a stable phrasing is what keeps the day's narrative
 * consistent between platforms.
 *
 * The app ships exactly two languages, so two cases is the whole problem, not a
 * simplification of it.
 */
internal fun LocalDate.longLabel(language: AppLanguage): String = when (language) {
    // "12 tháng 3, 2026" — the order Vietnamese writes dates in.
    AppLanguage.VIETNAMESE -> "$day tháng $monthNumber, $year"
    // "12 March 2026" — day-first, matching the en-GB style the rest of the app uses.
    AppLanguage.ENGLISH -> "$day ${ENGLISH_MONTHS[monthNumber - 1]} $year"
}

private val ENGLISH_MONTHS = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
