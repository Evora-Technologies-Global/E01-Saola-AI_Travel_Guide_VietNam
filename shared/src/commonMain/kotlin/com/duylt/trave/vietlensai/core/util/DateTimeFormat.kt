package com.duylt.trave.vietlensai.core.util

import com.duylt.trave.vietlensai.domain.model.AppLanguage
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Date and time formatting for the app's two languages, written by hand.
 *
 * `DateTimeFormatter` is JVM-only and `NSDateFormatter` is Apple-only, so neither can be
 * named from shared code. Delegating to each platform's own formatter would also mean the
 * journal heading and the chat timestamps read differently on the two builds, which is
 * exactly the kind of drift a shared UI is supposed to remove.
 *
 * Two languages is the whole problem, not a simplification of it: the app ships Vietnamese
 * and English and lets the traveller switch between them in Settings.
 */
private val MONTHS_SHORT_EN = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** Vietnamese writes months numerically — "thg 7", not a name. */
private fun vietnameseShortMonth(monthNumber: Int) = "thg $monthNumber"

/** "24 Jul 2025" / "24 thg 7, 2025" — the medium form, always with the year. */
internal fun LocalDate.mediumLabel(language: AppLanguage): String = when (language) {
    AppLanguage.VIETNAMESE -> "$day ${vietnameseShortMonth(monthNumber)}, $year"
    AppLanguage.ENGLISH -> "$day ${MONTHS_SHORT_EN[monthNumber - 1]} $year"
}

/**
 * "24 Jul" / "24 thg 7", with the year appended only when it is not the current one.
 *
 * The journal heading shares its row with the day's write-story button, and the year is
 * only worth the space once the date is old enough to be genuinely ambiguous.
 */
internal fun LocalDate.shortLabel(language: AppLanguage, currentYear: Int): String {
    val withoutYear = when (language) {
        AppLanguage.VIETNAMESE -> "$day ${vietnameseShortMonth(monthNumber)}"
        AppLanguage.ENGLISH -> "$day ${MONTHS_SHORT_EN[monthNumber - 1]}"
    }
    return if (year == currentYear) withoutYear else mediumLabel(language)
}

/**
 * "12.03.2026" — the date the way an ink stamp carries it.
 *
 * Language-independent on purpose. The passport's province stamp is a facsimile of a
 * border stamp, and those are numeric everywhere precisely so that they can be read by
 * someone who does not speak the language of the country that pressed them.
 */
internal fun LocalDate.stampLabel(): String = "${day.pad2()}.${monthNumber.pad2()}.$year"

/**
 * Clock time in the convention each language reads it in: 24-hour for Vietnamese,
 * 12-hour with a meridiem for English, matching the `en-US` locale the app declares.
 */
internal fun LocalDateTime.timeLabel(language: AppLanguage): String = when (language) {
    AppLanguage.VIETNAMESE -> "${hour.pad2()}:${minute.pad2()}"
    AppLanguage.ENGLISH -> {
        val meridiem = if (hour < 12) "AM" else "PM"
        // 0 and 12 both display as 12 — midnight is 12 AM, noon is 12 PM.
        val hour12 = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }
        "$hour12:${minute.pad2()} $meridiem"
    }
}

private fun Int.pad2(): String = toString().padStart(2, '0')
