package com.evora.technologies.saola.tablet.feature.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.StampType
import com.evora.technologies.saola.core.util.formatTime
import com.evora.technologies.saola.core.util.uiLanguage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.feature.camera.component.RecentCaptureCard
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.camera_open_journal
import org.jetbrains.compose.resources.stringResource

/**
 * Recent captures as a list of named rows, which is the one place the tablet deliberately
 * shows the same data differently from the phone.
 *
 * The phone stacks them into a pile, and its reason is written into `RecentCaptureStack`:
 * a scrolling strip would claim a whole row of a viewfinder that has no rows to spare. That
 * argument is about a phone. The panel here is 310 dp wide and as tall as the window, and
 * most of it is empty below the shutter — so the constraint the pile was answering does not
 * exist, and spending the space on names and times gives the traveller something the pile
 * never could: which of the last few captures is which, without opening the journal.
 *
 * Everything drawn is still the phone's: [RecentCaptureCard] is the same thumbnail the pile
 * lays down, at [depth][RecentCaptureCard] zero. Only the arrangement around it is new.
 */
@Composable
internal fun RecentScanList(
    discoveries: List<Discovery>,
    enabled: Boolean,
    onOpenJournal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.camera_open_journal)
    val language = uiLanguage()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(discoveries, key = { it.id }) { discovery ->
            RecentScanRow(
                discovery = discovery,
                time = discovery.createdAt.formatTime(language),
                clickLabel = label,
                enabled = enabled,
                onClick = onOpenJournal,
            )
        }
    }
}

/**
 * One row: the capture, what it turned out to be, and when.
 *
 * The whole row is the target rather than the thumbnail alone — a name the traveller can
 * read but not press is an invitation to press it — so [RecentCaptureCard] is handed no
 * click of its own here.
 */
@Composable
private fun RecentScanRow(
    discovery: Discovery,
    time: String,
    clickLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = clickLabel,
                onClick = onClick,
            )
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        RecentCaptureCard(discovery = discovery, depth = 0)

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(
                text = discovery.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = time,
                style = StampType.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
