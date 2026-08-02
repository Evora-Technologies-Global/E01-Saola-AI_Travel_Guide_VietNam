package com.duylt.trave.vietlensai.data.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.duylt.trave.vietlensai.data.util.log
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.repository.LocationRepository
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Best-effort position for grounding prompts.
 *
 * Deliberately timeout-bounded and failure-tolerant: location only sharpens the answer,
 * so waiting on a cold GPS fix inside a museum would trade a real feature for a marginal
 * one. A stale last-known fix is more than accurate enough to tell Hanoi from Hội An.
 */
internal class FusedLocationRepository(
    private val context: Context,
) : LocationRepository {

    // Built here rather than injected: the fused client is a thin handle over a system
    // service, and threading it through DI only to hand it one Context is ceremony.
    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override fun hasLocationPermission(): Boolean =
        PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

    override suspend fun currentLocation(): AppResult<GeoPoint> {
        if (!hasLocationPermission()) return AppResult.Failure(AppError.LocationUnavailable)

        return try {
            val location = withTimeoutOrNull(LOCATION_FIX_TIMEOUT_MILLIS) {
                @Suppress("MissingPermission")
                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .await()
            } ?: run {
                @Suppress("MissingPermission")
                fusedLocationClient.lastLocation.await()
            }

            location
                ?.let { AppResult.Success(GeoPoint(it.latitude, it.longitude)) }
                ?: AppResult.Failure(AppError.LocationUnavailable)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            log.w(e) { "Location permission revoked mid-request" }
            AppResult.Failure(AppError.LocationUnavailable)
        } catch (e: Exception) {
            log.w(e) { "Could not obtain a location fix" }
            AppResult.Failure(AppError.LocationUnavailable)
        }
    }

    private companion object {
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
