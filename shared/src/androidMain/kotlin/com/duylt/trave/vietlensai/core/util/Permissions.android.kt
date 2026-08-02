package com.duylt.trave.vietlensai.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** The permission APIs want the hosting activity, which Compose only hands us as a context. */
internal fun Context.findActivity(): Activity? = generateSequence(this) {
    (it as? ContextWrapper)?.baseContext
}.filterIsInstance<Activity>().firstOrNull()

/** The app's own page in system settings — the only way back from a permanent denial. */
private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ),
    )
}

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** Either grade of location is enough to name a province, so either one counts. */
private fun Context.hasLocationPermission(): Boolean =
    isGranted(Manifest.permission.ACCESS_COARSE_LOCATION) ||
        isGranted(Manifest.permission.ACCESS_FINE_LOCATION)

@Composable
actual fun rememberLocationPermissionState(): LocationPermissionState =
    // Coarse is what gets requested: naming a province does not need street precision,
    // and the coarse dialog is the easier of the two to say yes to.
    rememberPermissionState(
        permission = Manifest.permission.ACCESS_COARSE_LOCATION,
        isCurrentlyGranted = { it.hasLocationPermission() },
    )

@Composable
actual fun rememberCameraPermissionState(): LocationPermissionState =
    rememberPermissionState(
        permission = Manifest.permission.CAMERA,
        isCurrentlyGranted = { it.isGranted(Manifest.permission.CAMERA) },
    )

@Composable
private fun rememberPermissionState(
    permission: String,
    isCurrentlyGranted: (Context) -> Boolean,
): LocationPermissionState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var isGranted by remember(permission) { mutableStateOf(isCurrentlyGranted(context)) }
    var isDeniedForGood by rememberSaveable(permission) { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        isGranted = granted
        isDeniedForGood = !granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    // Re-read on resume: the prompt's settings link leaves the app without killing
    // the process, so a permission granted out there would never reach the screen.
    LifecycleResumeEffect(permission) {
        isGranted = isCurrentlyGranted(context)
        if (isGranted) isDeniedForGood = false
        onPauseOrDispose {}
    }

    return remember(isGranted, isDeniedForGood, launcher, context) {
        LocationPermissionState(
            isGranted = isGranted,
            isDeniedForGood = isDeniedForGood,
            onRequest = { launcher.launch(permission) },
            onOpenAppSettings = { context.openAppSettings() },
        )
    }
}
