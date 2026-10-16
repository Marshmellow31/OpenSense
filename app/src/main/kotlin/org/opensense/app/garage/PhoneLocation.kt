package org.opensense.app.garage

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlin.coroutines.resume

/** One explicit foreground request; no ongoing tracking for parked-car lookup. */
object PhoneLocation {
    @SuppressLint("MissingPermission")
    suspend fun current(context: Context): Location? = withContext(Dispatchers.Main) {
        if (!TripService.permitted(context)) return@withContext null
        withTimeoutOrNull(20000) {
            suspendCancellableCoroutine { continuation ->
                val manager = context.getSystemService(LocationManager::class.java)
                val precise = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val providers = (if (precise) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER) else listOf(LocationManager.NETWORK_PROVIDER)).filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
                if (providers.isEmpty()) { continuation.resume(null); return@suspendCancellableCoroutine }
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (!continuation.isActive || !location.hasAccuracy()) return
                        val age = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
                        if (age in 0..30_000_000_000L) { manager.removeUpdates(this); continuation.resume(location) }
                    }
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                continuation.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
                try { providers.forEach { manager.requestLocationUpdates(it, 1000, 0f, listener, Looper.getMainLooper()) } }
                catch (_: Exception) { manager.removeUpdates(listener); if (continuation.isActive) continuation.resume(null) }
            }
        }
    }
}
