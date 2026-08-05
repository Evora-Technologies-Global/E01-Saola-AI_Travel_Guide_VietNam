package com.duylt.trave.vietlensai.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.theme.Pill
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.feature.settings.SettingsIntent
import com.duylt.trave.vietlensai.feature.settings.SettingsState
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.settings_api_key
import com.duylt.trave.vietlensai.resources.settings_api_key_active
import com.duylt.trave.vietlensai.resources.settings_api_key_clear
import com.duylt.trave.vietlensai.resources.settings_api_key_default
import com.duylt.trave.vietlensai.resources.settings_api_key_hint
import com.duylt.trave.vietlensai.resources.settings_api_key_inactive
import com.duylt.trave.vietlensai.resources.settings_api_key_none
import com.duylt.trave.vietlensai.resources.settings_api_key_save
import com.duylt.trave.vietlensai.resources.settings_api_key_set
import org.jetbrains.compose.resources.stringResource

/**
 * Key entry, masked by default.
 *
 * The stored key is never rendered back — only a status line saying which source is in use. A
 * key on screen is a key that ends up in a screenshot or a demo recording, and there is no
 * reason to ever show one back to its owner.
 *
 * Save and Clear appear only when there is something to save or something to clear, so the
 * card sits at one line of status and an empty field the rest of the time.
 *
 * It reads and writes nothing itself: the draft lives in [SettingsState] and both buttons send
 * a [SettingsIntent]. The key goes to storage through `SaveApiKeyUseCase` and comes back as
 * `hasApiKey`, and no arrangement of this screen is allowed to shorten that path.
 */
@Composable
internal fun ApiKeyCard(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }

    SettingsCard(modifier = modifier, contentPadding = PaddingValues(Spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.settings_api_key),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            StatusPill(active = state.hasUsableKey)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(
                when {
                    state.settings.hasApiKey -> Res.string.settings_api_key_set
                    state.hasUsableKey -> Res.string.settings_api_key_default
                    else -> Res.string.settings_api_key_none
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = state.apiKeyDraft,
            onValueChange = { onIntent(SettingsIntent.ApiKeyDraftChanged(it)) },
            placeholder = {
                Text(
                    text = stringResource(Res.string.settings_api_key_hint),
                    // Monospaced, like the key itself: it tells the traveller what shape of
                    // thing belongs in the field before they paste one.
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            visualTransformation = if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        val canSave = state.apiKeyDraft.isNotBlank()
        if (canSave || state.settings.hasApiKey) {
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (canSave) {
                    Button(onClick = { onIntent(SettingsIntent.SaveApiKey) }) {
                        Text(stringResource(Res.string.settings_api_key_save))
                    }
                }
                if (state.settings.hasApiKey) {
                    TextButton(onClick = { onIntent(SettingsIntent.ClearApiKey) }) {
                        Text(stringResource(Res.string.settings_api_key_clear))
                    }
                }
            }
        }
    }
}

/** A dot and a word saying whether the app can call the model at all. */
@Composable
private fun StatusPill(active: Boolean) {
    val accent = if (active) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(
        shape = Pill,
        color = accent.copy(alpha = PILL_BACKGROUND_ALPHA),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(DOT_SIZE)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(Spacing.xs))
            Kicker(
                text = stringResource(
                    if (active) Res.string.settings_api_key_active else Res.string.settings_api_key_inactive,
                ),
                color = accent,
            )
        }
    }
}

private const val PILL_BACKGROUND_ALPHA = 0.14f

/** A printer's dot beside the word, not a control — sized to the type it sits against. */
private val DOT_SIZE = 6.dp
