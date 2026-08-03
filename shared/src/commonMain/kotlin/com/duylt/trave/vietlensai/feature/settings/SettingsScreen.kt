package com.duylt.trave.vietlensai.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.SharedBuildConfig
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.action_cancel
import com.duylt.trave.vietlensai.resources.action_delete
import com.duylt.trave.vietlensai.resources.settings_api_key
import com.duylt.trave.vietlensai.resources.settings_api_key_active
import com.duylt.trave.vietlensai.resources.settings_api_key_clear
import com.duylt.trave.vietlensai.resources.settings_api_key_default
import com.duylt.trave.vietlensai.resources.settings_api_key_hint
import com.duylt.trave.vietlensai.resources.settings_api_key_inactive
import com.duylt.trave.vietlensai.resources.settings_api_key_none
import com.duylt.trave.vietlensai.resources.settings_api_key_save
import com.duylt.trave.vietlensai.resources.settings_api_key_saved
import com.duylt.trave.vietlensai.resources.settings_api_key_set
import com.duylt.trave.vietlensai.resources.settings_clear_confirm_body
import com.duylt.trave.vietlensai.resources.settings_clear_confirm_title
import com.duylt.trave.vietlensai.resources.settings_clear_history
import com.duylt.trave.vietlensai.resources.settings_clear_history_summary
import com.duylt.trave.vietlensai.resources.settings_cleared
import com.duylt.trave.vietlensai.resources.settings_footer
import com.duylt.trave.vietlensai.resources.settings_kicker
import com.duylt.trave.vietlensai.resources.settings_language
import com.duylt.trave.vietlensai.resources.settings_model_balanced
import com.duylt.trave.vietlensai.resources.settings_model_balanced_summary
import com.duylt.trave.vietlensai.resources.settings_model_quick
import com.duylt.trave.vietlensai.resources.settings_model_quick_summary
import com.duylt.trave.vietlensai.resources.settings_model_scholar
import com.duylt.trave.vietlensai.resources.settings_model_scholar_summary
import com.duylt.trave.vietlensai.resources.settings_section_about
import com.duylt.trave.vietlensai.resources.settings_section_ai
import com.duylt.trave.vietlensai.resources.settings_section_appearance
import com.duylt.trave.vietlensai.resources.settings_section_data
import com.duylt.trave.vietlensai.resources.settings_section_experience
import com.duylt.trave.vietlensai.resources.settings_sovereignty
import com.duylt.trave.vietlensai.resources.settings_sovereignty_summary
import com.duylt.trave.vietlensai.resources.settings_speak
import com.duylt.trave.vietlensai.resources.settings_speak_summary
import com.duylt.trave.vietlensai.resources.settings_theme
import com.duylt.trave.vietlensai.resources.settings_theme_dark
import com.duylt.trave.vietlensai.resources.settings_theme_light
import com.duylt.trave.vietlensai.resources.settings_theme_system
import com.duylt.trave.vietlensai.resources.settings_title
import com.duylt.trave.vietlensai.core.designsystem.component.AppSnackbarHost
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.component.SectionHeader
import com.duylt.trave.vietlensai.core.designsystem.component.PageHeader
import com.duylt.trave.vietlensai.core.designsystem.component.SovereigntySeal
import com.duylt.trave.vietlensai.core.designsystem.component.showError
import com.duylt.trave.vietlensai.core.designsystem.component.showMessage
import com.duylt.trave.vietlensai.core.designsystem.theme.PageSpacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Pill
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.core.mvi.CollectEffects
import com.duylt.trave.vietlensai.core.util.userMessage
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.GeminiModel
import com.duylt.trave.vietlensai.domain.model.ThemePreference
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

/**
 * Settings as a page, not as a list of platform preferences.
 *
 * Laid out the way the journal and the explore tab are — a kicker, a headline, then
 * blocks of grouped cards — rather than under a `TopAppBar`. The traveller moves
 * between the four tabs constantly, and a bar appearing on exactly one of them reads
 * as a different app.
 *
 * The three things that change what the guide *says* — key, model, language — are
 * given the most room, in that order. Everything below them is furniture: how the app
 * looks, what it may use, and what it will forget on request.
 */
@Composable
fun SettingsRoute(
    onOpenSovereignty: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(Res.string.settings_api_key_saved)
    val clearedMessage = stringResource(Res.string.settings_cleared)

    // Which picker is open is the screen's own business, not the ViewModel's: it
    // survives nothing but a rotation, and a dialog reopening after process death
    // would be a surprise rather than a restoration.
    var pickingLanguage by rememberSaveable { mutableStateOf(false) }
    var pickingTheme by rememberSaveable { mutableStateOf(false) }

    // Shown from `scope` rather than from the collector itself, the same way the explore
    // and journal routes do it. `showSnackbar` suspends for the length of the snackbar, so
    // a collector that awaited it would hold the next effect behind the current notice —
    // and saving a key and clearing history can land seconds apart, which would tell the
    // traveller about the first act while they are looking at the result of the second.
    // Launched separately, each message reaches `showLatest`, which dismisses what is on
    // screen before showing, so the newer notice replaces the older one.
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SettingsEffect.ApiKeySaved -> scope.launch {
                snackbarHostState.showMessage(savedMessage)
            }
            SettingsEffect.HistoryCleared -> scope.launch {
                snackbarHostState.showMessage(clearedMessage)
            }
            // From the effect's own payload, not from `state.error`: the state value has not
            // been through a recomposition yet when this runs, so a key that failed to save
            // used to say nothing at all the first time. See `AppError.userMessage`.
            is SettingsEffect.ShowMessage -> scope.launch {
                snackbarHostState.showError(effect.error.userMessage())
            }
        }
    }

    Scaffold(
        // The status bar inset sits on the page itself, the way the explore tab and
        // the journal take it: one modifier on the outermost container, so every row
        // below it starts from the same edge on all four tabs.
        modifier = modifier
            .fillMaxSize()
            .screenInsetsPadding(),
        // The shell already reserves the tab bar; taking system insets again here
        // would leave a bar-sized gap under the last card.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = PageSpacing.listBottom),
        ) {
            item(key = "header") { SettingsHeader() }

            item(key = "section-ai") { SectionHeader(stringResource(Res.string.settings_section_ai)) }
            item(key = "api-key") { ApiKeyCard(state = state, onIntent = viewModel::onIntent) }
            item(key = "model") {
                ModelPicker(
                    selected = state.settings.preferredModel,
                    onIntent = viewModel::onIntent
                )
            }

            item(key = "section-experience") {
                SectionHeader(stringResource(Res.string.settings_section_experience))
            }
            item(key = "experience") {
                SettingsCard {
                    ValueRow(
                        title = stringResource(Res.string.settings_language),
                        value = state.settings.language.displayName,
                        onClick = { pickingLanguage = true },
                    )
                    RowDivider()
                    SwitchRow(
                        title = stringResource(Res.string.settings_speak),
                        summary = stringResource(Res.string.settings_speak_summary),
                        checked = state.settings.speakAnswers,
                        onChange = { viewModel.onIntent(SettingsIntent.SetSpeakAnswers(it)) },
                    )
                    // No location switch here on purpose. Android's own permission is
                    // the off switch, and a second one beside it was worse than
                    // redundant: it defaulted to on, the permission was never
                    // requested anywhere in the app, and the two together read as
                    // "location is working" while every capture was in fact being
                    // saved without coordinates. The camera screen asks for the
                    // permission, and the system settings page revokes it.
                }
            }

            item(key = "section-appearance") {
                SectionHeader(stringResource(Res.string.settings_section_appearance))
            }
            item(key = "appearance") {
                SettingsCard {
                    // Light or dark, and nothing else. The lacquer-and-gold palette is
                    // fixed on purpose: it is what makes this read as a Vietnamese
                    // guide rather than as whatever wallpaper the phone happens to
                    // have, and the passport and sovereignty marks are drawn in it
                    // outright.
                    ValueRow(
                        title = stringResource(Res.string.settings_theme),
                        value = stringResource(state.settings.darkTheme.labelRes),
                        onClick = { pickingTheme = true },
                    )
                }
            }

            item(key = "section-data") { SectionHeader(stringResource(Res.string.settings_section_data)) }
            item(key = "data") {
                SettingsCard {
                    DestructiveRow(
                        title = stringResource(Res.string.settings_clear_history),
                        summary = stringResource(Res.string.settings_clear_history_summary),
                        onClick = { viewModel.onIntent(SettingsIntent.RequestClearHistory) },
                    )
                }
            }

            item(key = "section-about") {
                SectionHeader(stringResource(Res.string.settings_section_about))
            }
            item(key = "sovereignty") {
                // The one row here rather than in "Experience": it is not a setting.
                // It is the way back to something the app has already said once, on
                // first launch, and nothing about it can be turned off.
                SovereigntyCard(onClick = onOpenSovereignty)
            }

            item(key = "footer") {
                Text(
                    text = stringResource(Res.string.settings_footer, SharedBuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = ScreenGutter, vertical = Spacing.xl),
                )
            }
        }
    }

    if (pickingLanguage) {
        ChoiceDialog(
            title = stringResource(Res.string.settings_language),
            options = AppLanguage.entries,
            selected = state.settings.language,
            label = { it.displayName },
            onSelect = {
                viewModel.onIntent(SettingsIntent.SelectLanguage(it))
                pickingLanguage = false
            },
            onDismiss = { pickingLanguage = false },
        )
    }

    if (pickingTheme) {
        ChoiceDialog(
            title = stringResource(Res.string.settings_theme),
            options = ThemePreference.entries,
            selected = state.settings.darkTheme,
            label = { stringResource(it.labelRes) },
            onSelect = {
                viewModel.onIntent(SettingsIntent.SelectTheme(it))
                pickingTheme = false
            },
            onDismiss = { pickingTheme = false },
        )
    }

    if (state.showClearConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(SettingsIntent.CancelClearHistory) },
            title = { Text(stringResource(Res.string.settings_clear_confirm_title)) },
            text = { Text(stringResource(Res.string.settings_clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(SettingsIntent.ConfirmClearHistory) }) {
                    Text(
                        text = stringResource(Res.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onIntent(SettingsIntent.CancelClearHistory) }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsHeader() {
    PageHeader(
        title = stringResource(Res.string.settings_title),
        kicker = stringResource(Res.string.settings_kicker),
    )
}

/**
 * Key entry, masked by default.
 *
 * The stored key is never rendered back — only a status line saying which source
 * is in use. A key on screen is a key that ends up in a screenshot or a demo
 * recording, and there is no reason to ever show one back to its owner.
 *
 * Save and Clear appear only when there is something to save or something to clear,
 * so the card sits at one line of status and an empty field the rest of the time.
 */
@Composable
private fun ApiKeyCard(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    var visible by remember { mutableStateOf(false) }

    SettingsCard(contentPadding = PaddingValues(Spacing.lg)) {
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
                    // Monospaced, like the key itself: it tells the traveller what
                    // shape of thing belongs in the field before they paste one.
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
                    .size(6.dp)
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

/**
 * The three models, named for what they are for rather than for what they are.
 *
 * "Gemini 3.1 Flash Lite" is a fact about Google's catalogue; "Quick — signs,
 * labels, prices" is a fact about the traveller's afternoon. The technical names
 * stay in the domain layer, where the fallback chain needs them.
 */
@Composable
private fun ModelPicker(selected: GeminiModel, onIntent: (SettingsIntent) -> Unit) {
    Column(
        modifier = Modifier
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
        // The chosen model is the one sentence of this screen the traveller will
        // want to check at a glance later, so it is outlined as well as tinted.
        border = if (selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
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
                // Null: the whole surface is the target, and a second one inside it
                // would let a screen reader announce the row twice.
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

/** The statement, as the last card on the page: a seal, a claim, and a way in. */
@Composable
private fun SovereigntyCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        // Outlined in the fixed vermilion rather than filled with it: filled is what
        // the banner on the explore tab does, and this row sits under a heading that
        // has already said "About".
        border = BorderStroke(1.dp, Vermilion.copy(alpha = SOVEREIGNTY_BORDER_ALPHA)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Vermilion),
                contentAlignment = Alignment.Center,
            ) {
                SovereigntySeal(size = 40.dp)
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_sovereignty),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = stringResource(Res.string.settings_sovereignty_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * The container every group of rows shares.
 *
 * Grouping is the layout's only structure: rows that belong together sit on one
 * card, and the section label above it says what "together" means. Nothing on this
 * screen is a full-bleed list row.
 */
@Composable
private fun SettingsCard(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter),
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = { content() })
    }
}

/** A row that opens a picker: what it is on the left, what it is set to on the right. */
@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.xs))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun DestructiveRow(title: String, summary: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA),
    )
}

/** One list of mutually exclusive options, for the rows that open a picker. */
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

private val ThemePreference.labelRes: StringResource
    get() = when (this) {
        ThemePreference.SYSTEM -> Res.string.settings_theme_system
        ThemePreference.LIGHT -> Res.string.settings_theme_light
        ThemePreference.DARK -> Res.string.settings_theme_dark
    }

private const val PILL_BACKGROUND_ALPHA = 0.14f
private const val SELECTED_CONTAINER_ALPHA = 0.55f
private const val SOVEREIGNTY_BORDER_ALPHA = 0.45f
private const val DIVIDER_ALPHA = 0.6f
