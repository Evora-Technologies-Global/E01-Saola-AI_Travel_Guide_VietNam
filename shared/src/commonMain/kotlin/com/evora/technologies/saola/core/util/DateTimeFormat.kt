package com.evora.technologies.saola.core.util

import com.evora.technologies.saola.domain.model.AppLanguage
import kotlinx.datetime.LocalDateTime

/**
 * Clock time, the one piece of date formatting that is not shared with `:data`.
 *
 * The dates themselves moved to `domain/model/DateNames.kt` when the app went from two
 * languages to eight: the journal heading on screen and the date inside a Gemini prompt
 * have to name the same month, and two hand-written copies of twelve month names in
 * eight languages is a drift waiting to happen. Time never enters a prompt, so it stayed
 * here.
 */
internal fun LocalDateTime.timeLabel(language: AppLanguage): String =
    if (!language.uses12HourClock) {
        "${hour.pad2()}:${minute.pad2()}"
    } else {
        val meridiem = if (hour < 12) "AM" else "PM"
        // 0 and 12 both display as 12 — midnight is 12 AM, noon is 12 PM.
        val hour12 = when (val h = hour % 12) {
            0 -> 12
            else -> h
        }
        "$hour12:${minute.pad2()} $meridiem"
    }

private fun Int.pad2(): String = toString().padStart(2, '0')
