package com.evora.technologies.saola.tablet.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.AppSnackbarHost
import com.evora.technologies.saola.core.designsystem.component.PageHeader
import com.evora.technologies.saola.core.designsystem.component.SectionHeader
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.feature.settings.PRIVACY_POLICY_URL
import com.evora.technologies.saola.feature.settings.SettingsHost
import com.evora.technologies.saola.feature.settings.SettingsIntent
import com.evora.technologies.saola.feature.settings.SettingsState
import com.evora.technologies.saola.feature.settings.SettingsViewModel
import com.evora.technologies.saola.feature.settings.TERMS_OF_SERVICE_URL
import com.evora.technologies.saola.feature.settings.component.ClearHistoryDialog
import com.evora.technologies.saola.feature.settings.component.DestructiveRow
import com.evora.technologies.saola.feature.settings.component.ExternalRow
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
import com.evora.technologies.saola.resources.settings_legal_summary
import com.evora.technologies.saola.resources.settings_licenses
import com.evora.technologies.saola.resources.settings_licenses_summary
import com.evora.technologies.saola.resources.settings_privacy_policy
import com.evora.technologies.saola.resources.settings_section_about
import com.evora.technologies.saola.resources.settings_section_appearance
import com.evora.technologies.saola.resources.settings_section_data
import com.evora.technologies.saola.resources.settings_section_experience
import com.evora.technologies.saola.resources.settings_speak
import com.evora.technologies.saola.resources.settings_speak_summary
import com.evora.technologies.saola.resources.settings_terms_of_service
import com.evora.technologies.saola.resources.settings_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The same settings, in two columns, because one column this wide is not a page.
 *
 * **The reason is the row, not the page.** Every group here is built from `ValueRow`,
 * `SwitchRow` and `DestructiveRow`, all of which put the label at the start and the value or
 * the switch at the far end. Given the whole 1 114 dp of content width, "Theme" would sit at
 * one edge and "System" at the other with nine hundred dp of nothing between them, and reading
 * a row would mean crossing the window. The wireframe lays the groups out with `display:flex`
 * for exactly this reason.
 *
 * The split is by group and never by height. Two columns balanced by measuring them would tear
 * a group in half the first time a translation ran long — and the eight languages this app
 * ships in do not agree about how long a settings label is.
 *
 * What is in each column is the phone's own order, cut once: what the traveller can change on
 * the left, what the app has to say for itself on the right. The cut used to fall one group
 * higher — "Intelligence" alone on the left — and moved on 06.08.2026 when that group was
 * removed, because a page whose first column starts empty is not a two-column page.
 */
@Composable
fun SettingsTabletRoute(
    onOpenSovereignty: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    SettingsHost(viewModel) { state, onIntent, snackbarHostState, onOpenUrl ->
        SettingsTabletScreen(
            state = state,
            onIntent = onIntent,
            snackbarHostState = snackbarHostState,
            onOpenSovereignty = onOpenSovereignty,
            onOpenLicenses = onOpenLicenses,
            onOpenUrl = onOpenUrl,
            modifier = modifier,
        )
    }
}

@Composable
private fun SettingsTabletScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    onOpenSovereignty: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenInsetsPadding()
                // One scroll for the page rather than one per column. Two independent
                // scrollers side by side would let the traveller put the two halves of one
                // page at two different heights, and nothing on screen would explain why the
                // headings no longer line up.
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(max = ContentWidth),
            ) {
                PageHeader(
                    title = stringResource(Res.string.settings_title),
                    kicker = stringResource(Res.string.settings_kicker),
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        ExperienceGroup(state = state, onIntent = onIntent)
                        AppearanceGroup(state = state, onIntent = onIntent)
                        DataGroup(onIntent = onIntent)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        AboutGroup(
                            onOpenSovereignty = onOpenSovereignty,
                            onOpenLicenses = onOpenLicenses,
                            onOpenUrl = onOpenUrl,
                        )
                    }
                }
            }
        }

        // `listBottom`, not `PageSpacing.snackbarLift`: that one is the height of a navigation
        // bar plus a gutter, and this window puts its navigation on a rail.
        AppSnackbarHost(
            snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = PageSpacing.listBottom),
        )
    }

    if (state.showClearConfirm) {
        ClearHistoryDialog(onIntent = onIntent)
    }
}

@Composable
private fun ColumnScope.ExperienceGroup(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    SectionHeader(stringResource(Res.string.settings_section_experience))
    SettingsCard {
        SwitchRow(
            title = stringResource(Res.string.settings_speak),
            summary = stringResource(Res.string.settings_speak_summary),
            checked = state.settings.speakAnswers,
            onChange = { onIntent(SettingsIntent.SetSpeakAnswers(it)) },
        )
    }
}

@Composable
private fun ColumnScope.AppearanceGroup(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    SectionHeader(stringResource(Res.string.settings_section_appearance))
    SettingsCard {
        ThemeRow(selected = state.settings.darkTheme, onIntent = onIntent)
    }
}

@Composable
private fun ColumnScope.DataGroup(onIntent: (SettingsIntent) -> Unit) {
    SectionHeader(stringResource(Res.string.settings_section_data))
    SettingsCard {
        DestructiveRow(
            title = stringResource(Res.string.settings_clear_history),
            summary = stringResource(Res.string.settings_clear_history_summary),
            onClick = { onIntent(SettingsIntent.RequestClearHistory) },
        )
    }
}

/**
 * The statement, the paperwork and the version — the whole of what the app says about itself.
 *
 * The gap under the sovereignty card is the same `Spacing.md` the phone puts there and for the
 * same reason: these two cards are the only pair on the page inside one section, so nothing
 * else separates them, and edge to edge they read as one card with a seal on it.
 */
@Composable
private fun ColumnScope.AboutGroup(
    onOpenSovereignty: () -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    SectionHeader(stringResource(Res.string.settings_section_about))
    SovereigntyCard(onClick = onOpenSovereignty)
    Spacer(Modifier.height(Spacing.md))
    SettingsCard {
        NavRow(
            title = stringResource(Res.string.settings_licenses),
            summary = stringResource(Res.string.settings_licenses_summary),
            onClick = onOpenLicenses,
        )
        ExternalRow(
            title = stringResource(Res.string.settings_privacy_policy),
            summary = stringResource(Res.string.settings_legal_summary),
            onClick = { onOpenUrl(PRIVACY_POLICY_URL) },
        )
        ExternalRow(
            title = stringResource(Res.string.settings_terms_of_service),
            summary = stringResource(Res.string.settings_legal_summary),
            onClick = { onOpenUrl(TERMS_OF_SERVICE_URL) },
        )
    }
    SettingsFooter()
}

/**
 * How wide the two columns are allowed to be together.
 *
 * A measured position rather than a gap, so it is a named value and not a step on the
 * [com.evora.technologies.saola.core.designsystem.theme.Spacing] scale — the argument
 * `Dimens.kt` makes for `PaneWidth`. It is that file's own arithmetic: the tablet wireframe's
 * inner frame is 1 218 px and the rail takes 104 of it, which leaves 1 114 for content.
 *
 * It is a **cap**, not a width, and the cap is what makes it worth naming. At 1 194 dp the
 * columns come out at 557 each and the number does nothing; on the 1 664 dp window this app
 * has already been run on they would reach 780 apiece, which is back inside the range where a
 * row's label and its value stop being read as one line. Capped and centred, a wider window
 * gives the page more margin rather than longer rows.
 */
private val ContentWidth = 1114.dp
