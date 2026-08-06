package com.evora.technologies.saola.data.platform

import com.evora.technologies.saola.data.util.log
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.repository.LocationRepository
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * CoreLocation behind the same contract Play services fills on Android.
 *
 * `requestLocation()` rather than `startUpdatingLocation()`: this repository is asked
 * for one fix per capture, and a continuous stream would keep the GPS warm — and the
 * location arrow in the status bar lit — for the whole time the app is open.
 *
 * The manager and its delegate are held for the object's lifetime. CoreLocation keeps
 * only a weak reference to its delegate, so a per-call delegate would be collected
 * before the callback arrived and the fix would silently never appear.
 */
@OptIn(ExperimentalForeignApi::class)
internal class CoreLocationRepository : LocationRepository {

    private val manager = CLLocationManager().apply {
        desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    private val delegate = FixDelegate().also { manager.delegate = it }

    override fun hasLocationPermission(): Boolean =
        manager.authorizationStatus in GRANTED_STATUSES

    override suspend fun currentLocation(): AppResult<GeoPoint> {
        if (!hasLocationPermission()) return AppResult.Failure(AppError.LocationUnavailable)

        // The cached fix first: CoreLocation keeps the last one for free, and it is
        // more than accurate enough to tell one province from another.
        manager.location?.let { return AppResult.Success(it.toGeoPoint()) }

        return try {
            val pending = delegate.awaitNextFix()
            manager.requestLocation()
            val fix = withTimeoutOrNull(LOCATION_FIX_TIMEOUT_MILLIS) { pending.await() }
            fix?.let { AppResult.Success(it.toGeoPoint()) }
                ?: AppResult.Failure(AppError.LocationUnavailable)
        } catch (e: Exception) {
            log.w(e) { "Could not obtain a location fix" }
            AppResult.Failure(AppError.LocationUnavailable)
        }
    }

    /**
     * Bridges the delegate callbacks onto a coroutine.
     *
     * One in-flight request at a time is enough — every caller asks for a single fix
     * per capture — so a superseded request is completed with null rather than queued.
     */
    private class FixDelegate : NSObject(), CLLocationManagerDelegateProtocol {

        private var pending: CompletableDeferred<CLLocation?>? = null

        fun awaitNextFix(): CompletableDeferred<CLLocation?> {
            pending?.complete(null)
            return CompletableDeferred<CLLocation?>().also { pending = it }
        }

        override fun locationManager(
            manager: CLLocationManager,
            didUpdateLocations: List<*>,
        ) {
            val fix = didUpdateLocations.filterIsInstance<CLLocation>().lastOrNull()
            pending?.complete(fix)
            pending = null
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            log.w { "CoreLocation failed: ${didFailWithError.localizedDescription}" }
            pending?.complete(null)
            pending = null
        }
    }

    private fun CLLocation.toGeoPoint(): GeoPoint = coordinate.useContents {
        GeoPoint(latitude = latitude, longitude = longitude)
    }

    private companion object {
        val GRANTED_STATUSES: Set<CLAuthorizationStatus> = setOf(
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways,
        )
    }
}
