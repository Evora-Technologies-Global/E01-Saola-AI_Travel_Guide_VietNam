package com.evora.technologies.saola.core.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.category_architecture
import com.evora.technologies.saola.resources.category_artifact
import com.evora.technologies.saola.resources.category_culture
import com.evora.technologies.saola.resources.category_food
import com.evora.technologies.saola.resources.category_landmark
import com.evora.technologies.saola.resources.category_nature
import com.evora.technologies.saola.resources.category_other
import com.evora.technologies.saola.resources.date_today
import com.evora.technologies.saola.resources.resource_language
import com.evora.technologies.saola.resources.date_yesterday
import com.evora.technologies.saola.resources.time_hours_ago
import com.evora.technologies.saola.resources.time_just_now
import com.evora.technologies.saola.resources.time_minutes_ago
import com.evora.technologies.saola.resources.time_yesterday
import com.evora.technologies.saola.core.designsystem.theme.CategoryColors
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.mediumLabel
import com.evora.technologies.saola.domain.model.shortLabel
import com.evora.technologies.saola.domain.model.DiscoveryCategory
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun DiscoveryCategory.label(): String = stringResource(
    when (this) {
        DiscoveryCategory.LANDMARK -> Res.string.category_landmark
        DiscoveryCategory.FOOD -> Res.string.category_food
        DiscoveryCategory.ARTIFACT -> Res.string.category_artifact
        DiscoveryCategory.ARCHITECTURE -> Res.string.category_architecture
        DiscoveryCategory.NATURE -> Res.string.category_nature
        DiscoveryCategory.CULTURE -> Res.string.category_culture
        DiscoveryCategory.OTHER -> Res.string.category_other
    },
)

/**
 * The language the interface is actually being drawn in.
 *
 * Resolved by asking the string table itself which of its files was picked, because
 * Compose Resources follows the device locale and offers no way to read that back.
 *
 * Now the same answer as `AppSettings.language`, which follows the device too, but still
 * the one to reach for inside composition. This reads the resolution Compose actually
 * made; the other recomputes it from the platform locale API for code that cannot call a
 * composable. Anything that has to sit *beside* a `stringResource` — the culture
 * catalogue's names and recognition hints, which ship in an asset rather than in the
 * string table — belongs to this one, so it cannot disagree with the labels around it
 * even if the two ever drift.
 */
@Composable
fun uiLanguage(): AppLanguage {
    val marker = stringResource(Res.string.resource_language)
    // `else` rather than an exhaustive map: the marker is whatever the resolved
    // strings.xml happens to carry, and a file added with a typo in it should degrade
    // to English rather than crash the screen drawing it.
    return AppLanguage.entries.firstOrNull { it.code == marker } ?: AppLanguage.ENGLISH
}

val DiscoveryCategory.accentColor: Color
    get() = when (this) {
        DiscoveryCategory.LANDMARK -> CategoryColors.landmark
        DiscoveryCategory.FOOD -> CategoryColors.food
        DiscoveryCategory.ARTIFACT -> CategoryColors.artifact
        DiscoveryCategory.ARCHITECTURE -> CategoryColors.architecture
        DiscoveryCategory.NATURE -> CategoryColors.nature
        DiscoveryCategory.CULTURE -> CategoryColors.culture
        DiscoveryCategory.OTHER -> CategoryColors.other
    }

/**
 * "Just now" / "2 hours ago" / an absolute date once it stops being useful.
 *
 * Relative time is only informative for about a day; past that, "37 days ago"
 * makes the reader do arithmetic that a date would have saved them.
 */
@Composable
fun Instant.asRelativeTime(language: AppLanguage): String {
    val minutes = (Clock.System.now() - this).inWholeMinutes
    return when {
        minutes < 1 -> stringResource(Res.string.time_just_now)
        minutes < 60 -> stringResource(Res.string.time_minutes_ago, minutes)
        minutes < 60 * 24 -> stringResource(Res.string.time_hours_ago, minutes / 60)
        minutes < 60 * 24 * 2 -> stringResource(Res.string.time_yesterday)
        else -> formatDate(language)
    }
}

fun Instant.formatDate(language: AppLanguage): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).date.mediumLabel(language)

fun Instant.formatTime(language: AppLanguage): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).timeLabel(language)

/**
 * "Today" / "Yesterday" / "24 Jul" — the label above one day of the journal.
 *
 * Short rather than a full "Thursday, 24 July 2025": the heading shares its row with
 * the day's write-story button, and the year is only worth the space once the date
 * is old enough to be genuinely ambiguous.
 */
@Composable
fun LocalDate.asJournalHeading(language: AppLanguage): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return when {
        this == today -> stringResource(Res.string.date_today)
        this == today.minus(1, DateTimeUnit.DAY) -> stringResource(Res.string.date_yesterday)
        else -> shortLabel(language, currentYear = today.year)
    }
}
