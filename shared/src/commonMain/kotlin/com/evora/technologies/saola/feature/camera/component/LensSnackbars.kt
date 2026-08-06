package com.evora.technologies.saola.feature.camera.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.evora.technologies.saola.core.designsystem.component.showError
import com.evora.technologies.saola.core.util.toUserMessage
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.error_missing_api_key
import org.jetbrains.compose.resources.stringResource

/**
 * Surfaces the missing-key warning as a snackbar instead of a permanent banner.
 *
 * `showError` dismisses whatever is on screen before this one goes up. The warning
 * also comes down the moment a key arrives: it is a standing condition rather than
 * an event, so leaving it up would be reporting a problem the traveller just fixed.
 *
 * **No action on it since 06.08.2026, and that is the honest shape now.** It used to carry a
 * "Settings" button, because Settings was where a key could be pasted; the key is the build's
 * from that date, so the button led to a page that could not fix what the notice was about.
 * A snackbar action that resolves nothing is worse than no action — it is the app pointing at
 * itself. What is left says what is wrong, and whoever builds the app is the one who can act
 * on it.
 */
@Composable
internal fun MissingKeySnackbar(
    hasApiKey: Boolean,
    hostState: SnackbarHostState,
) {
    val message = stringResource(Res.string.error_missing_api_key)

    LaunchedEffect(hasApiKey) {
        if (hasApiKey) {
            hostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }

        hostState.showError(message = message, withDismissAction = true)
    }
}

/**
 * Surfaces a capture or recognition failure as the app's red snackbar.
 *
 * The error is cleared once the notice leaves the screen, however it left — by
 * timeout, by swipe or by the dismiss action. Left in state it would be a failure
 * the traveller can no longer see and no longer dismiss, and the next identical
 * failure would never re-announce itself.
 */
@Composable
internal fun CaptureErrorSnackbar(
    error: AppError?,
    hostState: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    val message = error?.toUserMessage()

    LaunchedEffect(error) {
        if (message == null) return@LaunchedEffect
        hostState.showError(message = message, withDismissAction = true)
        onDismiss()
    }
}
