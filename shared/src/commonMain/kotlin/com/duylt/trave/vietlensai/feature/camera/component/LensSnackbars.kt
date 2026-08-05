package com.duylt.trave.vietlensai.feature.camera.component

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.duylt.trave.vietlensai.core.designsystem.component.showError
import com.duylt.trave.vietlensai.core.util.toUserMessage
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.error_missing_api_key
import com.duylt.trave.vietlensai.resources.settings_title
import org.jetbrains.compose.resources.stringResource

/**
 * Surfaces the missing-key warning as a snackbar instead of a permanent banner.
 *
 * `showError` dismisses whatever is on screen before this one goes up. The warning
 * also comes down the moment a key arrives: it is a standing condition rather than
 * an event, so leaving it up would be reporting a problem the traveller just fixed.
 */
@Composable
internal fun MissingKeySnackbar(
    hasApiKey: Boolean,
    hostState: SnackbarHostState,
    onOpenSettings: () -> Unit,
) {
    val message = stringResource(Res.string.error_missing_api_key)
    val action = stringResource(Res.string.settings_title)

    LaunchedEffect(hasApiKey) {
        if (hasApiKey) {
            hostState.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }

        val result = hostState.showError(
            message = message,
            actionLabel = action,
            withDismissAction = true,
        )
        if (result == SnackbarResult.ActionPerformed) onOpenSettings()
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
