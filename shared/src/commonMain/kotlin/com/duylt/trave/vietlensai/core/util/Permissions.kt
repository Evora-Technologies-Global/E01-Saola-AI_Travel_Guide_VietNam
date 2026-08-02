package com.duylt.trave.vietlensai.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Whether the app may read the traveller's location, and what asking again would do.
 *
 * [isDeniedForGood] is the case a prompt cannot ignore. On Android, after the second
 * refusal the launcher returns without ever showing a dialog; on iOS a `denied`
 * authorisation status is permanent until the traveller changes it in Settings. Either
 * way the button has to send them to the app's settings page rather than pretend to ask.
 */
@Stable
class LocationPermissionState internal constructor(
    val isGranted: Boolean,
    val isDeniedForGood: Boolean,
    private val onRequest: () -> Unit,
    private val onOpenAppSettings: () -> Unit,
) {
    /** The only call a prompt needs: ask while the OS still asks, hand over to settings after. */
    fun requestOrOpenSettings() {
        if (isDeniedForGood) onOpenAppSettings() else onRequest()
    }
}

/**
 * Location permission as a piece of screen state, launcher and all.
 *
 * Coarse ("when in use") is what gets requested: naming a province does not need street
 * precision, and the coarse dialog is the easier of the two to say yes to.
 *
 * Both actuals re-read the status when the screen resumes, because the settings link
 * leaves the app without killing the process — a permission granted out there would
 * otherwise never reach the screen.
 */
@Composable
expect fun rememberLocationPermissionState(): LocationPermissionState

/** Whether the camera may be used, and the same ask-or-hand-over rule. */
@Composable
expect fun rememberCameraPermissionState(): LocationPermissionState
