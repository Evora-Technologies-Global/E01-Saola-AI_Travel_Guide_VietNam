package com.evora.technologies.saola.feature.settings.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.theme.PaneWidth
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.ThemePreference
import com.evora.technologies.saola.feature.settings.SettingsIntent
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.action_cancel
import com.evora.technologies.saola.resources.settings_theme
import com.evora.technologies.saola.resources.settings_theme_dark
import com.evora.technologies.saola.resources.settings_theme_light
import com.evora.technologies.saola.resources.settings_theme_system
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Light, dark or whatever the device says — the row, and the picker it opens.
 *
 * Light or dark and nothing else. The lacquer-and-gold palette is fixed on purpose: it is what
 * makes this read as a Vietnamese guide rather than as whatever wallpaper the phone happens to
 * have, and the passport and sovereignty marks are drawn in it outright.
 *
 * **The row and the dialog are one component rather than two.** Whether the picker is open is
 * the screen's own business, not the ViewModel's — it survives nothing but a rotation, and a
 * dialog reopening after process death would be a surprise rather than a restoration — and
 * that is a fact about this control, not about either arrangement. Split across the two
 * branches, the flag would be declared twice and the second declaration is the one that would
 * miss the next change to it.
 */
@Composable
internal fun ThemeRow(
    selected: ThemePreference,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }

    ValueRow(
        title = stringResource(Res.string.settings_theme),
        value = stringResource(selected.labelRes),
        onClick = { picking = true },
        modifier = modifier,
    )

    if (picking) {
        ChoiceDialog(
            title = stringResource(Res.string.settings_theme),
            options = ThemePreference.entries,
            selected = selected,
            label = { stringResource(it.labelRes) },
            onSelect = {
                onIntent(SettingsIntent.SelectTheme(it))
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

/**
 * One list of mutually exclusive options, for the rows that open a picker.
 *
 * Capped at [PaneWidth.sheet], which is the wireframe's one measured width for a modal that
 * floats over the arrangement rather than covering it. On a phone the cap does nothing — the
 * dialog is already narrower than 440 dp — and on a 1 194 dp window it is the difference
 * between a picker and three radio buttons with four hundred dp of nothing beside them.
 * `widthIn` rather than `width` precisely so the phone is left alone.
 */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = PaneWidth.sheet),
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.width(Spacing.md))
                        Text(text = label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

private val ThemePreference.labelRes: StringResource
    get() = when (this) {
        ThemePreference.SYSTEM -> Res.string.settings_theme_system
        ThemePreference.LIGHT -> Res.string.settings_theme_light
        ThemePreference.DARK -> Res.string.settings_theme_dark
    }
