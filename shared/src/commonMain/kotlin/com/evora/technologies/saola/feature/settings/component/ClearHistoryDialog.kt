package com.evora.technologies.saola.feature.settings.component

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.theme.PaneWidth
import com.evora.technologies.saola.feature.settings.SettingsIntent
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.action_cancel
import com.evora.technologies.saola.resources.action_delete
import com.evora.technologies.saola.resources.settings_clear_confirm_body
import com.evora.technologies.saola.resources.settings_clear_confirm_title
import org.jetbrains.compose.resources.stringResource

/**
 * The confirmation in front of the one irreversible act in the app.
 *
 * Driven by `SettingsState.showClearConfirm` rather than by a flag in the composition, unlike
 * the theme picker beside it: cancelling has to reach the ViewModel either way, because the
 * sheet is opened by an intent and a dialog whose open state lived in two places would be able
 * to disagree with itself.
 *
 * Both arrangements draw it, unchanged, and capped at [PaneWidth.sheet] for the reason the
 * picker is — a confirmation stretched across a 1 194 dp window reads as a system failure
 * rather than as a question.
 */
@Composable
internal fun ClearHistoryDialog(onIntent: (SettingsIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(SettingsIntent.CancelClearHistory) },
        modifier = Modifier.widthIn(max = PaneWidth.sheet),
        title = { Text(stringResource(Res.string.settings_clear_confirm_title)) },
        text = { Text(stringResource(Res.string.settings_clear_confirm_body)) },
        confirmButton = {
            TextButton(onClick = { onIntent(SettingsIntent.ConfirmClearHistory) }) {
                Text(
                    text = stringResource(Res.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(SettingsIntent.CancelClearHistory) }) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
