package com.duylt.trave.vietlensai.feature.discovery.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.duylt.trave.vietlensai.core.designsystem.component.PermissionSheet
import com.duylt.trave.vietlensai.core.util.LocationPermissionState
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.action_cancel
import com.duylt.trave.vietlensai.resources.action_delete
import com.duylt.trave.vietlensai.resources.discovery_delete_confirm_body
import com.duylt.trave.vietlensai.resources.discovery_delete_confirm_title
import com.duylt.trave.vietlensai.resources.discovery_note_camera_permission_allow
import com.duylt.trave.vietlensai.resources.discovery_note_camera_permission_blocked_body
import com.duylt.trave.vietlensai.resources.discovery_note_camera_permission_body
import com.duylt.trave.vietlensai.resources.discovery_note_camera_permission_deny
import com.duylt.trave.vietlensai.resources.discovery_note_camera_permission_title
import com.duylt.trave.vietlensai.resources.permission_open_settings
import org.jetbrains.compose.resources.stringResource

/**
 * The two modals of the discovery page.
 *
 * Both are the whole of a decision rather than a layout, which is why neither is left to an
 * arrangement to compose: the wording of a permission request changes with whether the OS will
 * still ask, and a delete confirmation whose buttons read differently on a tablet is a
 * different promise made to the traveller about the same irreversible act.
 */

/**
 * Asks for the camera, in whichever of its two moods applies.
 *
 * @param permission from `rememberCameraPermissionState()`, read for `isDeniedForGood` alone —
 *   that flag decides both the sentence and what the confirm button does, and splitting it into
 *   two booleans at the call site is how one of them ends up stale. The type is shared with
 *   location because the ask-or-hand-over rule is the same for both.
 */
@Composable
internal fun NoteCameraPermissionSheet(
    permission: LocationPermissionState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    PermissionSheet(
        title = stringResource(Res.string.discovery_note_camera_permission_title),
        body = stringResource(
            if (permission.isDeniedForGood) {
                Res.string.discovery_note_camera_permission_blocked_body
            } else {
                Res.string.discovery_note_camera_permission_body
            },
        ),
        confirmLabel = stringResource(
            if (permission.isDeniedForGood) {
                Res.string.permission_open_settings
            } else {
                Res.string.discovery_note_camera_permission_allow
            },
        ),
        dismissLabel = stringResource(Res.string.discovery_note_camera_permission_deny),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/** The one irreversible act on this page, and the only thing that asks twice. */
@Composable
internal fun DeleteDiscoveryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.discovery_delete_confirm_title)) },
        text = { Text(stringResource(Res.string.discovery_delete_confirm_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
