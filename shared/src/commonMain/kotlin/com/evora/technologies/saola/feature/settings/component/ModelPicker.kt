package com.evora.technologies.saola.feature.settings.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.GeminiModel
import com.evora.technologies.saola.feature.settings.SettingsIntent
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.settings_model_balanced
import com.evora.technologies.saola.resources.settings_model_balanced_summary
import com.evora.technologies.saola.resources.settings_model_quick
import com.evora.technologies.saola.resources.settings_model_quick_summary
import com.evora.technologies.saola.resources.settings_model_scholar
import com.evora.technologies.saola.resources.settings_model_scholar_summary
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The three models, named for what they are for rather than for what they are.
 *
 * "Gemini 3.1 Flash Lite" is a fact about Google's catalogue; "Quick — signs, labels, prices"
 * is a fact about the traveller's afternoon. The technical names stay in the domain layer,
 * where the fallback chain needs them.
 *
 * Three loose cards rather than rows on a [SettingsCard], on both form factors. They are the
 * one choice on this screen that changes what the guide *says*, and a card each is what gives
 * the summary under each name room to be read before it is chosen.
 */
@Composable
internal fun ModelPicker(
    selected: GeminiModel,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        GeminiModel.entries.forEach { model ->
            ModelOption(
                label = stringResource(model.labelRes),
                summary = stringResource(model.summaryRes),
                selected = model == selected,
                onClick = { onIntent(SettingsIntent.SelectModel(model)) },
            )
        }
    }
}

@Composable
private fun ModelOption(
    label: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = SELECTED_CONTAINER_ALPHA)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        // The chosen model is the one sentence of this screen the traveller will want to check
        // at a glance later, so it is outlined as well as tinted.
        border = if (selected) {
            BorderStroke(SELECTED_BORDER, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                // Null: the whole surface is the target, and a second one inside it would let
                // a screen reader announce the row twice.
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline,
                ),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val GeminiModel.labelRes: StringResource
    get() = when (this) {
        GeminiModel.FLASH_3_5 -> Res.string.settings_model_balanced
        GeminiModel.FLASH_LITE_3_1 -> Res.string.settings_model_quick
        GeminiModel.PRO_3 -> Res.string.settings_model_scholar
    }

private val GeminiModel.summaryRes: StringResource
    get() = when (this) {
        GeminiModel.FLASH_3_5 -> Res.string.settings_model_balanced_summary
        GeminiModel.FLASH_LITE_3_1 -> Res.string.settings_model_quick_summary
        GeminiModel.PRO_3 -> Res.string.settings_model_scholar_summary
    }

private const val SELECTED_CONTAINER_ALPHA = 0.55f

/** Half a step above a hairline, so the outline reads as chosen rather than as an edge. */
private val SELECTED_BORDER = 1.5.dp
