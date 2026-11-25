package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.data.model.RoutePoint
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface LocationUpdateEvent {
    data class Point(val routePoint: RoutePoint, val speedMps: Float) : LocationUpdateEvent
    data class LowAccuracy(val accuracyMeters: Float) : LocationUpdateEvent
    data class Error(val message: String) : LocationUpdateEvent
}

class LocationTracker(private val context: Context) {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownOrCurrentLocation(): RoutePoint? {
        if (!hasLocationPermission()) return null

        val cached = suspendCancellableCoroutine<Location?> { cont ->
            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
        if (cached != null && cached.latitude.isFinite() && cached.longitude.isFinite()) {
            return cached.toRoutePoint()
        }

        val fresh = suspendCancellableCoroutine<Location?> { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            try {
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
        return if (fresh != null && fresh.latitude.isFinite() && fresh.longitude.isFinite()) {
            fresh.toRoutePoint()
        } else null
    }

    private fun Location.toRoutePoint(): RoutePoint = RoutePoint(
        lat = latitude,
        lng = longitude,
        alt = if (hasAltitude()) altitude else 0.0,
        acc = if (hasAccuracy()) accuracy.toDouble() else 25.0,
        ts = time.takeIf { it > 0L } ?: System.currentTimeMillis()
    )

    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMs: Long = 2000L): Flow<LocationUpdateEvent> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(LocationUpdateEvent.Error("Location permission denied. Grant foreground location access or use Manual Entry."))
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(1200L)
            .setMinUpdateDistanceMeters(1.5f)
            .build()

        var lastAccepted: Location? = null

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    if (!loc.latitude.isFinite() || !loc.longitude.isFinite()) continue
                    val accuracy = if (loc.hasAccuracy()) loc.accuracy else 999f
                    if (accuracy > 50f) {
                        trySend(LocationUpdateEvent.LowAccuracy(accuracy))
                        continue
                    }
                    val prev = lastAccepted
                    if (prev != null &&
                        prev.latitude == loc.latitude &&
                        prev.longitude == loc.longitude &&
                        loc.time <= prev.time
                    ) {
                        continue
                    }
                    lastAccepted = loc
                    trySend(
                        LocationUpdateEvent.Point(
                            routePoint = loc.toRoutePoint(),
                            speedMps = if (loc.hasSpeed()) loc.speed.coerceAtLeast(0f) else 0f
                        )
                    )
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { e ->
                    trySend(LocationUpdateEvent.Error(e.localizedMessage ?: "GPS provider unavailable"))
                }
        } catch (_: SecurityException) {
            trySend(LocationUpdateEvent.Error("Location permission revoked"))
        } catch (e: Exception) {
            trySend(LocationUpdateEvent.Error(e.localizedMessage ?: "Unable to start GPS"))
        }

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }
}
