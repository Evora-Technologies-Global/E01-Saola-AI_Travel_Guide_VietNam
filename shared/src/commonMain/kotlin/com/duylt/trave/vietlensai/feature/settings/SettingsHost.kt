package com.duylt.trave.vietlensai.feature.settings

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.core.designsystem.component.showError
import com.duylt.trave.vietlensai.core.designsystem.component.showMessage
import com.duylt.trave.vietlensai.core.mvi.CollectEffects
import com.duylt.trave.vietlensai.core.util.userMessage
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.settings_api_key_saved
import com.duylt.trave.vietlensai.resources.settings_cleared
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
 * @param content the arrangement. It receives the state, one way to write back, and the
 *   snackbar host it must place; everything else it decides for itself.
 */
@Composable
internal fun SettingsHost(
    viewModel: SettingsViewModel,
    content: @Composable (
        state: SettingsState,
        onIntent: (SettingsIntent) -> Unit,
        snackbarHostState: SnackbarHostState,
    ) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(Res.string.settings_api_key_saved)
    val clearedMessage = stringResource(Res.string.settings_cleared)

    // Shown from `scope` rather than from the collector itself, the same way the explore and
    // journal routes do it. `showSnackbar` suspends for the length of the snackbar, so a
    // collector that awaited it would hold the next effect behind the current notice — and
    // saving a key and clearing history can land seconds apart, which would tell the traveller
    // about the first act while they are looking at the result of the second. Launched
    // separately, each message reaches `showLatest`, which dismisses what is on screen before
    // showing, so the newer notice replaces the older one.
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

    content(state, viewModel::onIntent, snackbarHostState)
}
