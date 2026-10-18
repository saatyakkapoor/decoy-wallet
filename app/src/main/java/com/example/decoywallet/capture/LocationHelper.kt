package com.example.decoywallet.capture

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

data class CapturedLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val accuracyMeters: Float? = null,
    val provider: String = ""
)

object LocationHelper {

    /** Tries high-accuracy current fix, then last-known, then LocationManager. */
    @SuppressLint("MissingPermission")
    suspend fun current(context: Context): CapturedLocation? {
        val fused = LocationServices.getFusedLocationProviderClient(context)

        val fresh = withTimeoutOrNull(8_000) {
            val token = CancellationTokenSource()
            suspendCancellableCoroutine<Location?> { cont ->
                cont.invokeOnCancellation { token.cancel() }
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
        }

        val lastFused = if (fresh == null) {
            withTimeoutOrNull(3_000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    fused.lastLocation
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }
            }
        } else null

        val singleUpdate = if (fresh == null && lastFused == null) {
            requestSingleUpdate(context)
        } else null

        val lmLast = if (fresh == null && lastFused == null && singleUpdate == null) {
            lastFromLocationManager(context)
        } else null

        val loc = fresh ?: lastFused ?: singleUpdate ?: lmLast ?: return null
        return CapturedLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            address = resolveAddress(context, loc.latitude, loc.longitude),
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
            provider = loc.provider ?: "fused"
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleUpdate(context: Context): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                val done = AtomicBoolean(false)
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                    .setMaxUpdates(1)
                    .setWaitForAccurateLocation(false)
                    .build()
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        if (done.compareAndSet(false, true)) {
                            client.removeLocationUpdates(this)
                            cont.resume(result.lastLocation)
                        }
                    }
                }
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                    .addOnFailureListener {
                        if (done.compareAndSet(false, true)) cont.resume(null)
                    }
                cont.invokeOnCancellation {
                    client.removeLocationUpdates(callback)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastFromLocationManager(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.mapNotNull { p ->
            runCatching { if (lm.isProviderEnabled(p)) lm.getLastKnownLocation(p) else null }
                .getOrNull()
        }.maxByOrNull { it.time }
    }

    private fun resolveAddress(context: Context, lat: Double, lng: Double): String {
        return try {
            if (!Geocoder.isPresent()) {
                return "Lat %.5f, Lng %.5f".format(lat, lng)
            }
            @Suppress("DEPRECATION")
            val entry = Geocoder(context, Locale.getDefault())
                .getFromLocation(lat, lng, 1)
                ?.firstOrNull()
            if (entry != null) {
                listOfNotNull(
                    entry.featureName,
                    entry.thoroughfare,
                    entry.subLocality,
                    entry.locality,
                    entry.adminArea,
                    entry.countryName
                ).distinct().joinToString(", ").ifBlank {
                    "Lat %.5f, Lng %.5f".format(lat, lng)
                }
            } else {
                "Lat %.5f, Lng %.5f".format(lat, lng)
            }
        } catch (_: Exception) {
            "Lat %.5f, Lng %.5f".format(lat, lng)
        }
    }

    fun deviceInfo(): String =
        "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"
}
