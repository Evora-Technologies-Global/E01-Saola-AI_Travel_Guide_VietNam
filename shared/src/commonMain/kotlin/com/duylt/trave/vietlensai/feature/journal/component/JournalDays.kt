package com.duylt.trave.vietlensai.feature.journal.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.JournalDay
import kotlinx.datetime.LocalDate

/**
 * The trip so far, day by day, emitted into whichever list is asking.
 *
 * A `LazyListScope` extension rather than a composable, because the two branches put these
 * items in two different lists — the phone's runs the width of the page under the progress
 * rows, the tablet's is the master column of a two-pane screen — and a composable would have
 * to nest a second scrolling container inside the first.
 *
 * The order inside a day is the rule this file exists to hold in one place: heading, the
 * written story if there is one, the finds, and — only for today, and only until the story
 * exists — the invitation to write it. Get that last condition wrong on one branch and the
 * traveller is asked twice for something already done, on one form factor.
 *
 * @param expandedStories the dates, as ISO strings, whose narrative is open. Hoisted rather
 *   than remembered here so it survives the list: on the phone it must live through opening a
 *   discovery and coming back, and the branch is what knows to save it.
 */
internal fun LazyListScope.journalDays(
    days: List<JournalDay>,
    language: AppLanguage,
    today: LocalDate,
    generatingDate: LocalDate?,
    expandedStories: List<String>,
    onToggleStory: (String) -> Unit,
    onGenerate: (LocalDate) -> Unit,
    onOpenDiscovery: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    days.forEach { day ->
        val isToday = day.date == today
        val isGenerating = generatingDate == day.date

        item(key = "header-${day.date}") {
            DayHeader(
                day = day,
                language = language,
                isToday = isToday,
                isGenerating = isGenerating,
                onGenerate = { onGenerate(day.date) },
            )
        }

        day.summary?.let { summary ->
            val key = day.date.toString()
            item(key = "summary-$key") {
                StoryCard(
                    summary = summary,
                    expanded = key in expandedStories,
                    onToggle = { onToggleStory(key) },
                )
            }
        }

        items(day.discoveries, key = { it.id }) { discovery ->
            DiscoveryCard(
                discovery = discovery,
                language = language,
                onClick = { onOpenDiscovery(discovery.id) },
                onToggleFavorite = { onToggleFavorite(discovery.id) },
            )
        }

        // Today only, and only until the story exists: once it does, the heading's "Rewrite"
        // is the whole affordance and this panel would be asking twice for something already
        // done.
        if (isToday && day.summary == null) {
            item(key = "end-of-day-${day.date}") {
                EndOfDayCard(
                    findCount = day.discoveries.size,
                    isGenerating = isGenerating,
                    onWrite = { onGenerate(day.date) },
                )
            }
        }
    }
}
