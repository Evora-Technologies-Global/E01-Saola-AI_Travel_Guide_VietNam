package com.evora.technologies.saola.domain.model

import kotlinx.datetime.LocalDate

/**
 * Written dates in each of the app's eight languages, by hand.
 *
 * `DateTimeFormatter` is JVM-only and `NSDateFormatter` Apple-only, so neither can be
 * named from shared code, and delegating to each platform's own formatter would make the
 * journal heading read differently on the two builds. It lives in `:domain` rather than
 * beside either caller because both `:shared` (screen labels) and `:data` (the date that
 * goes *into* a Gemini prompt) need the same answer, and a month name that disagreed
 * between them would put one date on the screen and another in the story about it.
 *
 * Thai deliberately counts years in the Common Era, not the Buddhist Era its calendar
 * normally uses. Every other date the app shows — EXIF timestamps, the passport stamp,
 * what the traveller sees in their photo gallery — is CE, and 2569 beside 2026 would read
 * as a bug rather than as a convention.
 */

private val MONTHS_LONG: Map<AppLanguage, List<String>> = mapOf(
    AppLanguage.VIETNAMESE to (1..12).map { "tháng $it" },
    AppLanguage.ENGLISH to listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    ),
    AppLanguage.JAPANESE to (1..12).map { "${it}月" },
    AppLanguage.KOREAN to (1..12).map { "${it}월" },
    AppLanguage.CHINESE to (1..12).map { "${it}月" },
    AppLanguage.FRENCH to listOf(
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre",
    ),
    AppLanguage.SPANISH to listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    ),
    AppLanguage.THAI to listOf(
        "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
        "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม",
    ),
)

private val MONTHS_SHORT: Map<AppLanguage, List<String>> = mapOf(
    // Vietnamese writes months numerically — "thg 7", not a name.
    AppLanguage.VIETNAMESE to (1..12).map { "thg $it" },
    AppLanguage.ENGLISH to listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    ),
    // CJK months are already as short as they get; abbreviating further would only
    // remove the 月 / 월 that says "month" at all.
    AppLanguage.JAPANESE to (1..12).map { "${it}月" },
    AppLanguage.KOREAN to (1..12).map { "${it}월" },
    AppLanguage.CHINESE to (1..12).map { "${it}月" },
    AppLanguage.FRENCH to listOf(
        "janv.", "févr.", "mars", "avr.", "mai", "juin",
        "juil.", "août", "sept.", "oct.", "nov.", "déc.",
    ),
    AppLanguage.SPANISH to listOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic",
    ),
    AppLanguage.THAI to listOf(
        "ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
        "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.",
    ),
)

/** English falls back for a language whose table is somehow missing — never a crash. */
private fun AppLanguage.months(long: Boolean): List<String> =
    (if (long) MONTHS_LONG else MONTHS_SHORT).let { it[this] ?: it.getValue(AppLanguage.ENGLISH) }

fun AppLanguage.monthLong(month: Int): String = months(long = true)[month - 1]

fun AppLanguage.monthShort(month: Int): String = months(long = false)[month - 1]

/** "12 March 2026" / "12 tháng 3, 2026" / "2026年3月12日" — always with the year. */
fun LocalDate.longLabel(language: AppLanguage): String =
    language.assemble(day, language.monthLong(monthNumber), year)

/** The same order, with the month abbreviated: "12 Mar 2026" / "12 thg 3, 2026". */
fun LocalDate.mediumLabel(language: AppLanguage): String =
    language.assemble(day, language.monthShort(monthNumber), year)

/**
 * The medium form with the year dropped: "12 Mar" / "3月12日".
 *
 * The journal heading shares its row with the day's write-story button, and the year is
 * only worth the space once the date is old enough to be genuinely ambiguous.
 */
fun LocalDate.shortLabel(language: AppLanguage, currentYear: Int): String {
    if (year != currentYear) return mediumLabel(language)
    val month = language.monthShort(monthNumber)
    return when (language.dateStyle) {
        DateStyle.YEAR_FIRST_CJK ->
            if (language == AppLanguage.KOREAN) "$month ${day}일" else "$month${day}日"
        DateStyle.DAY_DE_MONTH -> "$day de $month"
        else -> "$day $month"
    }
}

/**
 * "12.03.2026" — the date the way an ink stamp carries it.
 *
 * Language-independent on purpose. The passport's province stamp is a facsimile of a
 * border stamp, and those are numeric everywhere precisely so that they can be read by
 * someone who does not speak the language of the country that pressed them.
 */
fun LocalDate.stampLabel(): String = "${day.pad2()}.${monthNumber.pad2()}.$year"

private fun AppLanguage.assemble(day: Int, month: String, year: Int): String =
    when (dateStyle) {
        DateStyle.DAY_FIRST -> "$day $month $year"
        DateStyle.DAY_FIRST_COMMA -> "$day $month, $year"
        DateStyle.DAY_DE_MONTH -> "$day de $month de $year"
        // The month name already carries its own 月 / 월. Korean spaces its parts;
        // Japanese and Chinese run them together.
        DateStyle.YEAR_FIRST_CJK ->
            if (this == AppLanguage.KOREAN) "${year}년 $month ${day}일" else "${year}年$month${day}日"
    }

private fun Int.pad2(): String = toString().padStart(2, '0')
