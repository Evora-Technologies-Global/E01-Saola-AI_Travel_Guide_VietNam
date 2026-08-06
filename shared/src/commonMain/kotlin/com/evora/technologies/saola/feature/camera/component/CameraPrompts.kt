package com.evora.technologies.saola.feature.camera.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.EmptyState
import com.evora.technologies.saola.core.designsystem.component.PermissionSheet
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.camera_permission_blocked_body
import com.evora.technologies.saola.resources.camera_permission_body
import com.evora.technologies.saola.resources.camera_permission_grant
import com.evora.technologies.saola.resources.camera_permission_title
import com.evora.technologies.saola.resources.location_permission_blocked_body
import com.evora.technologies.saola.resources.location_permission_body
import com.evora.technologies.saola.resources.location_permission_grant
import com.evora.technologies.saola.resources.location_permission_later
import com.evora.technologies.saola.resources.location_permission_title
import com.evora.technologies.saola.resources.permission_open_settings
import org.jetbrains.compose.resources.stringResource

/**
 * The one place the app asks for location.
 *
 * It leads with what the traveller gets — a province filling in on their passport —
 * rather than with what the app wants, because the system dialog that follows says
 * "allow access to this device's location" and nothing about why.
 *
 * The sheet answers to its own buttons and nothing else: a tap outside, a swipe down
 * and the back gesture are all refused. A question that can be brushed off by
 * accident is one the traveller never really answered, and the flag that stops it
 * from coming back is only written by "not now". Leaving is still one tap away —
 * location is a passport nicety, not the price of using the camera.
 */
@Composable
internal fun LocationPermissionSheet(
    deniedForGood: Boolean,
    onAllow: () -> Unit,
    onLater: () -> Unit,
) {
    PermissionSheet(
        title = stringResource(Res.string.location_permission_title),
        body = stringResource(
            if (deniedForGood) {
                Res.string.location_permission_blocked_body
            } else {
                Res.string.location_permission_body
            },
        ),
        confirmLabel = stringResource(
            if (deniedForGood) {
                Res.string.permission_open_settings
            } else {
                Res.string.location_permission_grant
            },
        ),
        dismissLabel = stringResource(Res.string.location_permission_later),
        onConfirm = onAllow,
        onDismiss = onLater,
    )
}

/** What fills the frame while the camera grant is still missing. */
@Composable
internal fun CameraPermissionPrompt(
    deniedForGood: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Outlined.PhotoLibrary,
        title = stringResource(Res.string.camera_permission_title),
        description = stringResource(
            if (deniedForGood) Res.string.camera_permission_blocked_body else Res.string.camera_permission_body,
        ),
        modifier = modifier,
        action = {
            Button(onClick = onRequest) {
                Text(
                    stringResource(
                        if (deniedForGood) {
                            Res.string.permission_open_settings
                        } else {
                            Res.string.camera_permission_grant
                        },
                    ),
                )
            }
        },
    )
}
