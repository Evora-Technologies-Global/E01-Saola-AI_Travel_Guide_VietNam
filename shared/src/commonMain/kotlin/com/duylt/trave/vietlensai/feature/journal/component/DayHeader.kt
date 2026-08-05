package com.duylt.trave.vietlensai.feature.journal.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.theme.PageSpacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Pill
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.util.asJournalHeading
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.JournalDay
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.journal_day_write_story
import com.duylt.trave.vietlensai.resources.journal_summary_regenerate
import org.jetbrains.compose.resources.stringResource

/**
 * A date, a dashed rule, and — depending on the day — the way to write its story.
 *
 * Today with no story shows nothing here: the end-of-day panel under its finds is making the
 * same offer, and two buttons for one action read as two actions.
 */
@Composable
internal fun DayHeader(
    day: JournalDay,
    language: AppLanguage,
    isToday: Boolean,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasStory = day.summary != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ScreenGutter,
                end = ScreenGutter,
                top = PageSpacing.sectionGap,
                bottom = Spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = day.date.asJournalHeading(language),
            style = MaterialTheme.typography.titleMedium,
        )
        SeamRule(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.md),
        )
        when {
            isGenerating -> CircularProgressIndicator(
                modifier = Modifier.size(SPINNER),
                strokeWidth = SPINNER_STROKE,
            )

            hasStory -> OutlinedButton(
                onClick = onGenerate,
                shape = Pill,
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
                modifier = Modifier.height(BUTTON_HEIGHT),
            ) {
                Text(
                    text = stringResource(Res.string.journal_summary_regenerate),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            !isToday -> Button(
                onClick = onGenerate,
                shape = Pill,
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
                modifier = Modifier.height(BUTTON_HEIGHT),
            ) {
                Text(
                    text = stringResource(Res.string.journal_day_write_story),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * The hairline between a date and its button, drawn dashed so it reads as a seam.
 *
 * Deliberately **not** the design system's [com.duylt.trave.vietlensai.core.designsystem.component.DashedRule].
 * That one is the ruled line of a notebook page — long dashes, wide gaps, round caps — and it
 * is the mark the discovery page and the passport are printed with. This is a seam: a tight
 * stitch closing the gap between two things on one line. Drawn at the same weight so the two
 * do not look like a mistake when a traveller crosses from the journal to a discovery.
 */
@Composable
private fun SeamRule(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier.height(RULE_HEIGHT)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(SEAM_DASH.toPx(), SEAM_GAP.toPx()),
            ),
        )
    }
}

private val RULE_HEIGHT = 1.dp

/** Stitch then gap, in dp so the seam keeps its rhythm at every density. */
private val SEAM_DASH = 4.dp
private val SEAM_GAP = 5.dp

/** A button short enough to sit on one line with a date — a position, not a gap. */
private val BUTTON_HEIGHT = 34.dp

private val SPINNER = 20.dp
private val SPINNER_STROKE = 2.dp
