package com.evora.technologies.saola.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject

/**
 * iOS has no "shouldShowRequestPermissionRationale" equivalent, and does not need one: the
 * authorisation status itself distinguishes the three cases. `notDetermined` means the OS
 * will still show a dialog, `denied`/`restricted` means it never will again, and anything
 * else is granted — so [LocationPermissionState.isDeniedForGood] maps straight onto it.
 */
@Composable
actual fun rememberLocationPermissionState(): LocationPermissionState {
    // Held across recompositions: CoreLocation keeps only a weak reference to its delegate,
    // and the authorisation callback is the only way to learn the traveller's answer.
    val manager = remember { CLLocationManager() }
    var status by remember { mutableStateOf(manager.authorizationStatus) }

    DisposableEffect(manager) {
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                status = manager.authorizationStatus
            }
        }
        manager.delegate = delegate
        onDispose { manager.delegate = null }
    }

    // Re-read on resume: the settings link leaves the app without terminating it, so an
    // authorisation granted out there would never reach the screen.
    LifecycleResumeEffect(manager) {
        status = manager.authorizationStatus
        onPauseOrDispose {}
    }

    val isGranted = status == kCLAuthorizationStatusAuthorizedWhenInUse ||
        status == kCLAuthorizationStatusAuthorizedAlways

    return remember(isGranted, status) {
        LocationPermissionState(
            isGranted = isGranted,
            isDeniedForGood = status == kCLAuthorizationStatusDenied ||
                status == kCLAuthorizationStatusRestricted,
            // "When in use" rather than "always": the app only ever asks for a fix while
            // the traveller is pointing the camera at something.
            onRequest = { manager.requestWhenInUseAuthorization() },
            onOpenAppSettings = { openAppSettings() },
        )
    }
}

@Composable
actual fun rememberCameraPermissionState(): LocationPermissionState {
    var status by remember {
        mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo))
    }

    LifecycleResumeEffect(Unit) {
        status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        onPauseOrDispose {}
    }

    return remember(status) {
        LocationPermissionState(
            isGranted = status == AVAuthorizationStatusAuthorized,
            isDeniedForGood = status == AVAuthorizationStatusDenied ||
                status == AVAuthorizationStatusRestricted,
            onRequest = {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    status = if (granted) {
                        AVAuthorizationStatusAuthorized
                    } else {
                        AVAuthorizationStatusDenied
                    }
                }
            },
            onOpenAppSettings = { openAppSettings() },
        )
    }
}

/** The app's own page in Settings — the only way back from a permanent denial. */
private fun openAppSettings() {
    NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>()) { }
    }
}
