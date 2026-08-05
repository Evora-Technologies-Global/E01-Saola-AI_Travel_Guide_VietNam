package com.duylt.trave.vietlensai.feature.passport.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.util.asJournalHeading
import com.duylt.trave.vietlensai.core.util.formatDate
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.PassportStamp
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.passport_sheet_first_visit
import com.duylt.trave.vietlensai.resources.passport_sheet_last_visit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

/**
 * When the traveller first arrived, and when they were last here.
 *
 * Two different registers on purpose. The first visit is the record, so it is written out in
 * full with its year; the latest is news, so it is written the way the journal writes it —
 * "Today", "Yesterday", "24 Jul".
 */
@Composable
internal fun VisitRow(stamp: PassportStamp, language: AppLanguage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = Spacing.md),
    ) {
        VisitCell(
            label = stringResource(Res.string.passport_sheet_first_visit),
            value = stamp.firstVisitAt?.formatDate(language) ?: "—",
            modifier = Modifier.weight(1f),
        )
        VisitCell(
            label = stringResource(Res.string.passport_sheet_last_visit),
            value = stamp.lastVisitAt?.asDayHeading(language) ?: "—",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun VisitCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Kicker(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Instant.asDayHeading(language: AppLanguage): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).date.asJournalHeading(language)
