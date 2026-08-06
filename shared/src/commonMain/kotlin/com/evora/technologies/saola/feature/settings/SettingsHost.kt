package com.evora.technologies.saola.feature.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.saola.core.designsystem.component.showError
import com.evora.technologies.saola.core.designsystem.component.showMessage
import com.evora.technologies.saola.core.mvi.CollectEffects
import com.evora.technologies.saola.core.util.userMessage
import com.evora.technologies.saola.platform.rememberUrlOpener
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.settings_cleared
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Everything Settings does that is not layout, in one place both arrangements call.
 *
 * The third host in the app, after `feature/camera/LensHost.kt` and
 * `feature/explore/ExploreHost.kt`, and it is here for the reason `LLM.md` §5 gives for both:
 * the arrangement is per branch, the ViewModel wiring is shared whenever it is more than a
 * call. What follows is three effect arms and a rule about how they are shown — behaviour, not
 * placement — and copied into a second screen it is the copy that would miss the next fix.
 *
 * @param content the arrangement. It receives the state, one way to write back, the snackbar
 *   host it must place, and the way off the app for the two legal links; everything else it
 *   decides for itself. The opener is here rather than in each Route because leaving the app
 *   is platform behaviour — an `Intent` on Android, `UIApplication.openURL` on iOS — and §3
 *   forbids a branch from owning any.
 */
@Composable
internal fun SettingsHost(
    viewModel: SettingsViewModel,
    content: @Composable (
        state: SettingsState,
        onIntent: (SettingsIntent) -> Unit,
        snackbarHostState: SnackbarHostState,
        onOpenUrl: (String) -> Unit,
    ) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val openUrl = rememberUrlOpener()
    val clearedMessage = stringResource(Res.string.settings_cleared)

    // Shown from `scope` rather than from the collector itself, the same way the explore and
    // journal routes do it. `showSnackbar` suspends for the length of the snackbar, so a
    // collector that awaited it would hold the next effect behind the current notice — a
    // clear that failed and one that landed can arrive within a second of each other on a
    // retry, and queued they would tell the traveller about the first act while they are
    // looking at the result of the second. Launched separately, each message reaches
    // `showLatest`, which dismisses what is on screen before showing, so the newer notice
    // replaces the older one.
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            SettingsEffect.HistoryCleared -> scope.launch {
                snackbarHostState.showMessage(clearedMessage)
            }
            // From the effect's own payload, not from `state.error`: the state value has not
            // been through a recomposition yet when this runs, so a write that failed used to
            // say nothing at all the first time. See `AppError.userMessage`.
            is SettingsEffect.ShowMessage -> scope.launch {
                snackbarHostState.showError(effect.error.userMessage())
            }
        }
    }

    content(state, viewModel::onIntent, snackbarHostState, openUrl)
}
