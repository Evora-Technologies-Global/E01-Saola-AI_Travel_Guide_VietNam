package com.evora.technologies.saola.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.evora.technologies.saola.core.designsystem.theme.LacquerRed

/**
 * Marks a message as a failure rather than a confirmation.
 *
 * The distinction lives in the visuals rather than in a second host so that one
 * queue orders every transient message on a screen: a red notice and a "saved"
 * notice can never end up drawn on top of each other.
 */
class ErrorSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Long,
) : SnackbarVisuals

/**
 * The host every screen uses, so a failure looks the same wherever it surfaces.
 *
 * Errors come up in lacquer red with black text — read at arm's length in sunlight,
 * that pairing is the one the traveller already reads as "something went wrong".
 * Anything else keeps the theme's own snackbar colours.
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data: SnackbarData ->
        val isError = data.visuals is ErrorSnackbarVisuals
        Snackbar(
            snackbarData = data,
            shape = MaterialTheme.shapes.medium,
            containerColor = if (isError) LacquerRed else SnackbarDefaults.color,
            contentColor = if (isError) Color.Black else SnackbarDefaults.contentColor,
            actionColor = if (isError) Color.Black else SnackbarDefaults.actionColor,
            actionContentColor = if (isError) Color.Black else SnackbarDefaults.actionContentColor,
            dismissActionContentColor = if (isError) {
                Color.Black
            } else {
                SnackbarDefaults.dismissActionContentColor
            },
        )
    }
}

/**
 * Shows [message] as the app's red error snackbar.
 *
 * Goes through [showLatest], so a failure always replaces whatever notice it
 * arrives on top of instead of waiting behind it.
 */
suspend fun SnackbarHostState.showError(
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    duration: SnackbarDuration = SnackbarDuration.Long,
): SnackbarResult = showLatest(
    ErrorSnackbarVisuals(
        message = message,
        actionLabel = actionLabel,
        withDismissAction = withDismissAction,
        duration = duration,
    ),
)

/**
 * Shows an ordinary confirmation — "copied", "saved" — in the theme's own colours.
 *
 * Goes through [showLatest] for the same reason [showError] does.
 */
suspend fun SnackbarHostState.showMessage(
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    duration: SnackbarDuration = if (actionLabel == null) {
        SnackbarDuration.Short
    } else {
        SnackbarDuration.Indefinite
    },
): SnackbarResult = showLatest(
    object : SnackbarVisuals {
        override val message = message
        override val actionLabel = actionLabel
        override val withDismissAction = withDismissAction
        override val duration = duration
    },
)

/**
 * The one way this app puts a snackbar on screen: dismiss what is showing, then show.
 *
 * `SnackbarHostState` queues by default, and a queued message appears seconds late —
 * after the traveller has moved on — so it reads as a reply to whatever they did
 * next rather than to what caused it. Every helper above funnels through here rather
 * than calling `showSnackbar` itself, so the rule cannot be forgotten at a call site.
 */
suspend fun SnackbarHostState.showLatest(visuals: SnackbarVisuals): SnackbarResult {
    currentSnackbarData?.dismiss()
    return showSnackbar(visuals)
}
