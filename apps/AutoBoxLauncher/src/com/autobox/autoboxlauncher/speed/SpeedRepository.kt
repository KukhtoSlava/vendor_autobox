package com.autobox.autoboxlauncher.speed

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SpeedRepository(context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Emits vehicle speed in km/h derived from GPS.
     * Emits 0 when GPS has no speed fix yet.
     * Updates at most every MIN_UPDATE_INTERVAL_MS and MIN_UPDATE_DISTANCE_M.
     */
    val speedFlow: Flow<Int> = callbackFlow {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val speedKmh = if (location.hasSpeed()) {
                    (location.speed * MS_TO_KMH).roundToInt().coerceAtLeast(0)
                } else {
                    0
                }
                trySend(speedKmh)
            }

            @Deprecated("Kept for API < 29 compat")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }

        trySend(0) // emit 0 immediately before the first GPS fix arrives

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            MIN_UPDATE_INTERVAL_MS,
            MIN_UPDATE_DISTANCE_M,
            listener,
        )

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }

    companion object {
        private const val MS_TO_KMH = 3.6f
        private const val MIN_UPDATE_INTERVAL_MS = 1_000L
        private const val MIN_UPDATE_DISTANCE_M = 0f  // update on every fix regardless of distance
    }
}
