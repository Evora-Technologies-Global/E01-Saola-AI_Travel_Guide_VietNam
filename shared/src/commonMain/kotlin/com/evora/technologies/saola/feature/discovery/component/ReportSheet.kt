package com.evora.technologies.saola.feature.discovery.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.domain.model.DiscoveryReport
import com.evora.technologies.saola.domain.model.ReportReason
import com.evora.technologies.saola.feature.discovery.DiscoveryIntent
import com.evora.technologies.saola.feature.discovery.ReportDraft
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.report_attachment_none
import com.evora.technologies.saola.resources.report_attachment_photo
import com.evora.technologies.saola.resources.report_body
import com.evora.technologies.saola.resources.report_cancel
import com.evora.technologies.saola.resources.report_note_count
import com.evora.technologies.saola.resources.report_note_placeholder
import com.evora.technologies.saola.resources.report_send
import com.evora.technologies.saola.resources.report_title
import org.jetbrains.compose.resources.stringResource

/**
 * The form: what kind of wrong, and in the traveller's own words.
 *
 * A sheet rather than a screen because a report is an aside — the page it is about has to stay
 * behind it, since half of what makes a complaint specific is being able to look at the thing
 * while writing it. Unlike [NoteCameraPermissionSheet] it *can* be dismissed by tapping away
 * and by the back gesture: nothing is spent by abandoning a report, and a form that traps the
 * traveller until they either accuse the app or find the right button is a worse thing to have
 * built than no form at all.
 *
 * The four reasons are chips rather than radio rows, and that is a claim about how they are
 * read: they are short, mutually exclusive and all four fit on two lines, so the whole choice
 * is visible at once instead of being a list to work down. [ReportReason.OTHER] is offered
 * beside the three specific ones rather than being implied by leaving them blank — a report
 * with no reason at all is one nobody can sort, and asking for a tap is cheaper than asking
 * for a sentence.
 *
 * The line above the button names what leaves with the message. It is stated rather than
 * offered as a choice: the photograph and the result's own details are the whole evidence, a
 * report without them is a sentence with nothing to check it against, and a traveller who does
 * not want to send them can decline the share sheet that opens next — where they can see
 * exactly what is attached before anything goes anywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportSheet(
    draft: ReportDraft,
    isSubmitting: Boolean,
    hasPhoto: Boolean,
    onIntent: (DiscoveryIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onIntent(DiscoveryIntent.CancelReport) },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter)
                // The note field is the last thing above the keyboard on a phone, and the send
                // button sits below it — without these two the traveller types the sentence
                // they are being asked for from behind the keyboard, and cannot reach send.
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = Spacing.xl),
        ) {
            Text(
                text = stringResource(Res.string.report_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(Res.string.report_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.lg))
            ReasonChips(
                selected = draft.reason,
                enabled = !isSubmitting,
                onSelect = { onIntent(DiscoveryIntent.ReportReasonSelected(it)) },
            )

            Spacer(Modifier.height(Spacing.lg))
            OutlinedTextField(
                value = draft.note,
                // Capped here as well as in `SubmitReportUseCase`, and the two are not
                // redundant: the use case's `take` is the guarantee for every caller, and this
                // is what stops the traveller watching themselves type a sentence that would
                // be silently cut off at the moment they pressed send.
                onValueChange = { typed ->
                    if (typed.length <= DiscoveryReport.MAX_NOTE_LENGTH) {
                        onIntent(DiscoveryIntent.ReportNoteChanged(typed))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(NOTE_FIELD_HEIGHT),
                enabled = !isSubmitting,
                placeholder = {
                    Text(
                        text = stringResource(Res.string.report_note_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = MaterialTheme.shapes.medium,
            )

            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(
                    Res.string.report_note_count,
                    draft.noteLength,
                    DiscoveryReport.MAX_NOTE_LENGTH,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = stringResource(
                    if (hasPhoto) Res.string.report_attachment_photo else Res.string.report_attachment_none,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.lg))
            SheetActions(
                canSubmit = draft.canSubmit && !isSubmitting,
                isSubmitting = isSubmitting,
                onCancel = { onIntent(DiscoveryIntent.CancelReport) },
                onSubmit = { onIntent(DiscoveryIntent.SubmitReport) },
            )
        }
    }
}

@Composable
private fun ReasonChips(
    selected: ReportReason?,
    enabled: Boolean,
    onSelect: (ReportReason) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // `entries` rather than a hand-written list, so a fifth reason added to the enum
        // appears here instead of being silently unofferable.
        ReportReason.entries.forEach { reason ->
            FilterChip(
                selected = reason == selected,
                onClick = { onSelect(reason) },
                enabled = enabled,
                label = {
                    Text(
                        text = stringResource(reason.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Vermilion,
                    selectedLabelColor = PaperCream,
                ),
            )
        }
    }
}

/**
 * The way out and the way on, at the trailing end — the same order [PermissionSheet] uses.
 *
 * The send button carries the spinner rather than covering the sheet with one: everything else
 * here stays readable while the row is being written, and the traveller can see that what they
 * typed is still there if it fails.
 */
@Composable
private fun SheetActions(
    canSubmit: Boolean,
    isSubmitting: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onCancel,
            enabled = !isSubmitting,
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            Text(
                text = stringResource(Res.string.report_cancel),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Button(
            onClick = onSubmit,
            enabled = canSubmit,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Vermilion,
                contentColor = PaperCream,
            ),
            contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.sm),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SPINNER_SIZE),
                    color = PaperCream,
                    strokeWidth = SPINNER_STROKE,
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text = stringResource(Res.string.report_send),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * Three lines of room before it scrolls.
 *
 * A measured position rather than a gap: it is what a specific complaint takes — "the date in
 * the second paragraph is wrong, it was rebuilt in 1954" — and a field one line tall invites
 * the four-word version of that.
 */
private val NOTE_FIELD_HEIGHT = 120.dp

private val SPINNER_SIZE = 16.dp
private val SPINNER_STROKE = 2.dp
