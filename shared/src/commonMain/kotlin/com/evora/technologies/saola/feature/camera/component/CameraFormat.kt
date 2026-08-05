package com.evora.technologies.saola.feature.camera.component

import kotlin.math.abs
import kotlin.math.roundToInt

/** "+1.0" / "-0.7": exposure is read as a signed number of stops, never as an index. */
internal fun Float.formatEv(): String {
    val sign = if (this > 0f) "+" else if (this < 0f) "-" else ""
    return sign + abs(this).toOneDecimal()
}

/** "1x", "2x", "2.4x" — a decimal only when there is one. */
internal fun Float.formatZoom(): String {
    val rounded = (this * 10f).roundToInt() / 10f
    return if (abs(rounded - rounded.roundToInt()) < 0.05f) {
        "${rounded.roundToInt()}x"
    } else {
        "${rounded.toOneDecimal()}x"
    }
}

/**
 * One decimal place, always with a dot.
 *
 * `String.format` is JVM-only, and the fixed locale it was pinned to mattered: zoom and
 * exposure are read as numbers on a dial, and a comma decimal in the middle of the row reads
 * as a second value rather than as a fraction. Building the string by hand keeps that
 * guarantee on both platforms instead of borrowing it from a locale.
 */
private fun Float.toOneDecimal(): String {
    val tenths = (abs(this) * 10f).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}
