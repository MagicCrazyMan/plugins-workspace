// Copyright 2019-2023 Tauri Programme within The Commons Conservancy
// SPDX-License-Identifier: Apache-2.0
// SPDX-License-Identifier: MIT

package app.tauri.geolocation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.location.LocationManagerCompat
import app.tauri.Logger
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.os.Handler
import android.os.Looper


public class Geolocation(private val context: Context) {
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val handler = Handler(Looper.getMainLooper())


    fun isLocationServicesEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    @SuppressWarnings("MissingPermission")
    fun sendLocation(
        enableHighAccuracy: Boolean,
        useGMS: Boolean,
        timeout: Long,
        successCallback: (location: Location) -> Unit,
        errorCallback: (error: String) -> Unit
    ) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        val resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (useGMS && resultCode == ConnectionResult.SUCCESS) {

            if (this.isLocationServicesEnabled()) {
                var networkEnabled = false

                try {
                    networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                } catch (_: Exception) {
                    Logger.error("isProviderEnabled failed")
                }

                val lowPrio = if (networkEnabled) Priority.PRIORITY_BALANCED_POWER_ACCURACY else Priority.PRIORITY_LOW_POWER
                val prio = if (enableHighAccuracy) Priority.PRIORITY_HIGH_ACCURACY else lowPrio

                Logger.error(prio.toString())

                val cts = CancellationTokenSource()
                val token = cts.getToken()

                // setup timeout handling
                var isCompleted = false
                val timeoutRunnable = Runnable {
                    if (!isCompleted) {
                        cts.cancel()
                        errorCallback("Location request timeout after ${timeout}ms")
                    }
                }
                handler.postDelayed(timeoutRunnable, timeout)
                
                LocationServices
                    .getFusedLocationProviderClient(context)
                    .getCurrentLocation(prio, token)
                    .addOnFailureListener { e -> 
                        handler.removeCallbacks(timeoutRunnable)
                        if (!isCompleted) {
                            isCompleted = true
                            e.message?.let { errorCallback(it) }
                        }
                    }
                    .addOnSuccessListener { location ->
                        handler.removeCallbacks(timeoutRunnable)
                        if (!isCompleted) {
                            isCompleted = true
                            if (location == null) {
                                errorCallback("Location unavailable.")
                            } else {
                                successCallback(location)
                            }
                        }
                    }
            } else {
                errorCallback("Location disabled.")
            }
        } else {
            // Fallbacks to Android Framework Location API
            val providers = lm.getAllProviders()
            val provider =
                if (enableHighAccuracy && providers.contains(LocationManager.GPS_PROVIDER) && lm.isProviderEnabled(
                        LocationManager.GPS_PROVIDER
                    )
                )
                    LocationManager.GPS_PROVIDER
                else if (providers.contains(LocationManager.FUSED_PROVIDER) && lm.isProviderEnabled(
                        LocationManager.FUSED_PROVIDER
                    )
                )
                    LocationManager.FUSED_PROVIDER
                else if (providers.contains(LocationManager.NETWORK_PROVIDER) && lm.isProviderEnabled(
                        LocationManager.NETWORK_PROVIDER
                    )
                )
                    LocationManager.NETWORK_PROVIDER
                else
                    LocationManager.PASSIVE_PROVIDER

            // 创建 CancellationSignal 用于超时取消
            val cancellationSignal = CancellationSignal()
            var isCompleted = false
            
            // 设置超时取消任务
            val timeoutRunnable = Runnable {
                if (!isCompleted) {
                    cancellationSignal.cancel()
                    errorCallback("Location request timeout after ${timeout}ms")
                }
            }
            handler.postDelayed(timeoutRunnable, timeout)

            lm.getCurrentLocation(
                provider,
                cancellationSignal,
                context.getMainExecutor(),
                { location ->
                    handler.removeCallbacks(timeoutRunnable)
                    if (!isCompleted) {
                        isCompleted = true
                        if (location == null) {
                            errorCallback("Location unavailable.")
                        } else {
                            successCallback(location)
                        }
                    }
                }
            );
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(enableHighAccuracy: Boolean, timeout: Long, successCallback: (location: Location) -> Unit, errorCallback: (error: String) -> Unit) {
        val resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
        if (resultCode == ConnectionResult.SUCCESS) {
            clearLocationUpdates()
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (this.isLocationServicesEnabled()) {
                var networkEnabled = false

                try {
                    networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                } catch (_: Exception) {
                    Logger.error("isProviderEnabled failed")
                }

                val lowPrio = if (networkEnabled) Priority.PRIORITY_BALANCED_POWER_ACCURACY else Priority.PRIORITY_LOW_POWER
                val prio = if (enableHighAccuracy) Priority.PRIORITY_HIGH_ACCURACY else lowPrio

                val locationRequest = LocationRequest.Builder(timeout)
                    .setMaxUpdateDelayMillis(timeout)
                    .setMinUpdateIntervalMillis(timeout)
                    .setPriority(prio)
                    .build()

                locationCallback =
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            val lastLocation = locationResult.lastLocation
                            if (lastLocation == null) {
                                errorCallback("Location unavailable.")
                            } else {
                                successCallback(lastLocation)
                            }
                        }
                    }

                fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback!!, null)
            } else {
                errorCallback("Location disabled.")
            }
        } else {
            errorCallback("Google Play Services not available.")
        }
    }

    fun clearLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient?.removeLocationUpdates(locationCallback!!)
            locationCallback = null
        }
    }

    @SuppressLint("MissingPermission")
    fun getLastLocation(maximumAge: Long): Location? {
        var lastLoc: Location? = null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        for (provider in lm.allProviders) {
            val tmpLoc = lm.getLastKnownLocation(provider)
            if (tmpLoc != null) {
                val locationAge = SystemClock.elapsedRealtimeNanos() - tmpLoc.elapsedRealtimeNanos
                val maxAgeNano = maximumAge * 1000000L
                if (locationAge <= maxAgeNano && (lastLoc == null || lastLoc.elapsedRealtimeNanos > tmpLoc.elapsedRealtimeNanos)) {
                    lastLoc = tmpLoc
                }
            }
        }

        return lastLoc
    }
}
