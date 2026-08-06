package com.evora.technologies.saola.mobile.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.AppSnackbarHost
import com.evora.technologies.saola.core.designsystem.component.PageHeader
import com.evora.technologies.saola.core.designsystem.component.SectionHeader
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.feature.settings.SettingsHost
import com.evora.technologies.saola.feature.settings.SettingsIntent
import com.evora.technologies.saola.feature.settings.SettingsState
import com.evora.technologies.saola.feature.settings.SettingsViewModel
import com.evora.technologies.saola.feature.settings.component.ApiKeyCard
import com.evora.technologies.saola.feature.settings.component.ClearHistoryDialog
import com.evora.technologies.saola.feature.settings.component.DestructiveRow
import com.evora.technologies.saola.feature.settings.component.ModelPicker
import com.evora.technologies.saola.feature.settings.component.NavRow
import com.evora.technologies.saola.feature.settings.component.SettingsCard
import com.evora.technologies.saola.feature.settings.component.SettingsFooter
import com.evora.technologies.saola.feature.settings.component.SovereigntyCard
import com.evora.technologies.saola.feature.settings.component.SwitchRow
import com.evora.technologies.saola.feature.settings.component.ThemeRow
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.settings_clear_history
import com.evora.technologies.saola.resources.settings_clear_history_summary
import com.evora.technologies.saola.resources.settings_kicker
import com.evora.technologies.saola.resources.settings_licenses
import com.evora.technologies.saola.resources.settings_licenses_summary
import com.evora.technologies.saola.resources.settings_section_about
import com.evora.technologies.saola.resources.settings_section_ai
import com.evora.technologies.saola.resources.settings_section_appearance
import com.evora.technologies.saola.resources.settings_section_data
import com.evora.technologies.saola.resources.settings_section_experience
import com.evora.technologies.saola.resources.settings_speak
import com.evora.technologies.saola.resources.settings_speak_summary
import com.evora.technologies.saola.resources.settings_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings as a page, not as a list of platform preferences.
 *
 * Laid out the way the journal and the explore tab are — a kicker, a headline, then blocks of
 * grouped cards — rather than under a `TopAppBar`. The traveller moves between the four tabs
 * constantly, and a bar appearing on exactly one of them reads as a different app.
 *
 * The two things that change what the guide *says* — key and model — are given the most room,
 * in that order. Everything below them is furniture: how the app looks, what it may use, and
 * what it will forget on request. Language is not among them any more: it follows the phone.
 *
 * One column, top to bottom, and that is the phone's whole contribution. Every card in it is a
 * component in `feature/settings/component/`, shared with the large window's two-column
 * arrangement — see `tablet/feature/settings/SettingsTabletScreen.kt`.
 */
@Composable
fun SettingsRoute(
    onOpenSovereignty: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    SettingsHost(viewModel) { state, onIntent, snackbarHostState ->
        SettingsScreen(
            state = state,
            onIntent = onIntent,
            snackbarHostState = snackbarHostState,
            onOpenSovereignty = onOpenSovereignty,
            onOpenLicenses = onOpenLicenses,
            modifier = modifier,
        )
    }
}

@Composable
private fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onOpenSovereignty: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        // The status bar inset sits on the page itself, the way the explore tab and the
        // journal take it: one modifier on the outermost container, so every row below it
        // starts from the same edge on all four tabs.
        modifier = modifier
            .fillMaxSize()
            .screenInsetsPadding(),
        // The shell already reserves the tab bar; taking system insets again here would leave
        // a bar-sized gap under the last card.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = PageSpacing.listBottom),
        ) {
            item(key = "header") {
                PageHeader(
                    title = stringResource(Res.string.settings_title),
                    kicker = stringResource(Res.string.settings_kicker),
                )
            }

            item(key = "section-ai") { SectionHeader(stringResource(Res.string.settings_section_ai)) }
            item(key = "api-key") { ApiKeyCard(state = state, onIntent = onIntent) }
            item(key = "model") {
                ModelPicker(selected = state.settings.preferredModel, onIntent = onIntent)
            }

            item(key = "section-experience") {
                SectionHeader(stringResource(Res.string.settings_section_experience))
            }
            item(key = "experience") {
                SettingsCard {
                    // No language row here either, for the same reason as location below: the
                    // phone already has that switch. Narration now follows the device
                    // language, so an in-app picker could only ever disagree with the
                    // interface around it — an English screen reading itself out in
                    // Vietnamese. Changed in the phone's settings, it changes here.
                    SwitchRow(
                        title = stringResource(Res.string.settings_speak),
                        summary = stringResource(Res.string.settings_speak_summary),
                        checked = state.settings.speakAnswers,
                        onChange = { onIntent(SettingsIntent.SetSpeakAnswers(it)) },
                    )
                    // No location switch here on purpose. Android's own permission is the off
                    // switch, and a second one beside it was worse than redundant: it
                    // defaulted to on, the permission was never requested anywhere in the app,
                    // and the two together read as "location is working" while every capture
                    // was in fact being saved without coordinates. The camera screen asks for
                    // the permission, and the system settings page revokes it.
                }
            }

            item(key = "section-appearance") {
                SectionHeader(stringResource(Res.string.settings_section_appearance))
            }
            item(key = "appearance") {
                SettingsCard {
                    ThemeRow(selected = state.settings.darkTheme, onIntent = onIntent)
                }
            }

            item(key = "section-data") { SectionHeader(stringResource(Res.string.settings_section_data)) }
            item(key = "data") {
                SettingsCard {
                    DestructiveRow(
                        title = stringResource(Res.string.settings_clear_history),
                        summary = stringResource(Res.string.settings_clear_history_summary),
                        onClick = { onIntent(SettingsIntent.RequestClearHistory) },
                    )
                }
            }

            item(key = "section-about") {
                SectionHeader(stringResource(Res.string.settings_section_about))
            }
            item(key = "sovereignty") {
                // Here rather than in "Experience": it is not a setting. It is the way back to
                // something the app has already said once, on first launch, and nothing about
                // it can be turned off.
                SovereigntyCard(onClick = onOpenSovereignty)
            }

            item(key = "licenses") {
                // Under "About" for the same reason, and it is the app's end of a bargain
                // rather than a preference: ODbL, CC BY-SA and Commons' per-file terms all
                // ask to be credited, and this row is where the credit is stated in full.
                SettingsCard {
                    NavRow(
                        title = stringResource(Res.string.settings_licenses),
                        summary = stringResource(Res.string.settings_licenses_summary),
                        onClick = onOpenLicenses,
                    )
                }
            }

            item(key = "footer") { SettingsFooter() }
        }
    }

    if (state.showClearConfirm) {
        ClearHistoryDialog(onIntent = onIntent)
    }
}
