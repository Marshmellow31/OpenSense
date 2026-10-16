package org.opensense.app.garage

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.location.*
import android.os.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.opensense.app.MainActivity
import org.opensense.app.R
import org.opensense.app.storage.GarageStore
import org.opensense.core.garage.*
import java.util.UUID

data class RecordingTrip(val active: Boolean = false, val startedAt: Long = 0, val distance: Double = 0.0, val maxKmh: Double = 0.0,
    val points: List<TrackPoint> = emptyList(), val message: String = "Ready for your next journey")

/** User-started phone GPS recorder. Never claims to reproduce the head-unit EcoSense score. */
class TripService : Service(), LocationListener {
    companion object {
        val state = MutableStateFlow(RecordingTrip())
        fun permitted(context: Context) = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        fun start(context: Context) { context.startForegroundService(Intent(context, TripService::class.java)) }
        fun stop(context: Context) { context.startService(Intent(context, TripService::class.java).setAction("STOP")) }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val accumulator = TripAccumulator()
    private val id = UUID.randomUUID().toString()
    private var started = 0L
    private var lastSaved = 0L
    private var finished = false
    override fun onBind(intent: Intent?) = null
    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { finishTrip(); return START_NOT_STICKY }
        if (started != 0L) return START_NOT_STICKY
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("trip-recording", "Trip recording", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).putExtra("route", "Trips"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, TripService::class.java).setAction("STOP"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, "trip-recording").setSmallIcon(R.drawable.ic_opensense).setContentTitle("Recording your trip")
            .setContentText("Phone GPS · tap to view or stop recording").setContentIntent(open).setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop trip", stop).build()).build()
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(7001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) else startForeground(7001, notification)
            if (!permitted(this)) { state.value = RecordingTrip(message = "Allow location access before recording."); stopSelf(); return START_NOT_STICKY }
            started = System.currentTimeMillis()
            state.value = RecordingTrip(true, startedAt = started, message = "Waiting for an accurate GPS fix…")
            val locations = getSystemService(LocationManager::class.java)
            val precise = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val providers = if (precise) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER) else listOf(LocationManager.NETWORK_PROVIDER)
            var requested = false
            providers.forEach { provider -> if (locations.isProviderEnabled(provider)) { locations.requestLocationUpdates(provider, 2000L, 3f, this); requested = true } }
            if (!requested) { state.value = RecordingTrip(message = "Turn on system Location, then start again."); stopSelf() }
        } catch (_: Exception) { state.value = RecordingTrip(message = "Trip recording could not start. Check Location and app permissions."); stopSelf() }
        return START_NOT_STICKY
    }
    override fun onLocationChanged(location: Location) {
        if (finished || started == 0L || !location.hasAccuracy()) return
        val age = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        if (age !in 0..30_000_000_000L) return
        val accepted = accumulator.add(TrackPoint(location.latitude, location.longitude, location.elapsedRealtimeNanos / 1_000_000, location.accuracy))
        state.value = RecordingTrip(true, started, accumulator.distanceMetres, accumulator.maxKmh, accumulator.points,
            if (accepted) "Recording from phone GPS" else "Waiting for a more accurate GPS fix…")
        if (accepted && SystemClock.elapsedRealtime() - lastSaved > 30000) { lastSaved = SystemClock.elapsedRealtime(); scope.launch { persist(true) } }
    }
    override fun onProviderDisabled(provider: String) { state.value = state.value.copy(message = "GPS paused · turn on system Location to continue") }
    override fun onProviderEnabled(provider: String) {}
    private suspend fun persist(interrupted: Boolean) {
        if (accumulator.points.isEmpty()) return
        val points = accumulator.points
        val stride = maxOf(1, kotlin.math.ceil(points.size / 2000.0).toInt())
        val record = TripRecord(id, started, System.currentTimeMillis(), accumulator.distanceMetres, accumulator.maxKmh, accumulator.movingMillis, accumulator.harshBrakes,
            points.filterIndexed { index, _ -> index % stride == 0 }, interrupted)
        GarageStore.get(this).update { data ->
            val previous = data.trips.firstOrNull { it.id == id }
            if (previous != null && (previous.endedAt > record.endedAt || (interrupted && !previous.interrupted))) data
            else data.copy(trips = (listOf(record) + data.trips.filterNot { it.id == id }).take(50))
        }
    }
    private fun finishTrip() {
        if (finished) return
        finished = true
        getSystemService(LocationManager::class.java).removeUpdates(this)
        scope.launch {
            try { persist(false); state.value = RecordingTrip(message = if (accumulator.points.isEmpty()) "No GPS fixes received; no trip was saved." else "Trip saved on this phone") }
            finally { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
        }
    }
    override fun onDestroy() { getSystemService(LocationManager::class.java).removeUpdates(this); if (!finished) state.value = RecordingTrip(message = "Recording stopped. Any checkpoint is marked interrupted."); scope.cancel(); super.onDestroy() }
}
