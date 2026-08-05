package com.duylt.trave.vietlensai.feature.explore

/**
 * The four numbers a place is described by, written the way they are read aloud.
 *
 * Pulled out of the detail sheet when the tablet arrived, because both a card in a list and
 * the open place print the same distance from the same field: two spellings of "1.2 km" is
 * one screen telling the traveller something slightly different from the screen beside it.
 * Not composables and not in `component/` — they are string formatting, and they are testable
 * without a composition.
 */

/**
 * "4.6" from 4.6499999, rounded rather than truncated.
 *
 * Written out rather than taken from a format string: `String.format` is JVM-only, and a
 * multiplatform formatter would be a dependency for one decimal place.
 */
internal fun Double.oneDecimal(): String {
    // `+ 0.5` before truncating. Truncating alone reported 1.99 km as "1.9 km", which is
    // wrong in the direction that matters — it tells the traveller somewhere is nearer
    // than it is.
    val scaled = ((this * 10) + 0.5).toInt()
    return "${scaled / 10}.${scaled % 10}"
}

/**
 * Metres under a kilometre, kilometres to one decimal above it.
 *
 * "850 m" and "1.2 km" are both read at a glance; "1200 m" makes the reader do the
 * division themselves.
 */
internal fun Int.asDistanceLabel(): String =
    if (this < METERS_PER_KM) "$this m" else "${(this / METERS_PER_KM.toDouble()).oneDecimal()} km"

/** "12.4k" rather than "12403": a readership is a magnitude, not an exact quantity. */
internal fun Int.compact(): String =
    if (this < THOUSAND) toString() else "${(this / THOUSAND.toDouble()).oneDecimal()}k"

/** `coffee_shop;vietnamese` → `Coffee shop · Vietnamese`, as OSM writes cuisine tags. */
internal fun String.asCuisineLabel(): String = split(';')
    .take(MAX_CUISINES)
    .joinToString(" · ") { part ->
        part.trim().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

private const val METERS_PER_KM = 1000
private const val THOUSAND = 1000
private const val MAX_CUISINES = 2
