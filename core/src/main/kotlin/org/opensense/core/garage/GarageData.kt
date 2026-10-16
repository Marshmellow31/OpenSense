package org.opensense.core.garage

import kotlin.math.*

data class FuelEntry(val id: String, val time: Long, val odometerKm: Double, val litres: Double,
    val costMinor: Long, val fullTank: Boolean, val note: String = "") {
    init { require(odometerKm.isFinite() && odometerKm >= 0 && litres.isFinite() && litres > 0 && litres <= 200 && costMinor in 0..100000000) }
}
data class FuelInterval(val from: String, val to: String, val distance: Double, val litres: Double) {
    val kmPerLitre: Double get() = distance / litres
}
object FuelMath {
    /** Full-to-full: include partial fills AFTER the starting full tank, including the ending fill. */
    fun intervals(entries: List<FuelEntry>): List<FuelInterval> {
        val sorted = entries.sortedBy { it.odometerKm }
        val result = mutableListOf<FuelInterval>()
        var anchor: FuelEntry? = null; var litres = 0.0
        sorted.forEach { entry ->
            if (anchor != null) litres += entry.litres
            if (entry.fullTank) {
                anchor?.let { start -> if (entry.odometerKm > start.odometerKm && litres > 0)
                    result += FuelInterval(start.id, entry.id, entry.odometerKm - start.odometerKm, litres) }
                anchor = entry; litres = 0.0
            }
        }
        return result
    }
}
data class Reminder(val id: Int, val title: String, val dueAt: Long, val leadMillis: Long = 0, val done: Boolean = false, val notified: Boolean = false) {
    init { require(id > 0 && title.isNotBlank() && title.length <= 120 && dueAt > 0 && leadMillis >= 0) }
    val alertAt get() = dueAt - leadMillis
}
data class ParkedPlace(val latitude: Double, val longitude: Double, val accuracy: Float, val time: Long, val note: String = "")
data class GarageDocument(val id: String, val name: String, val uri: String, val mime: String, val manual: Boolean = false)
data class OwnerRecord(val name: String = "", val phone: String = "", val email: String = "", val registration: String = "",
    val insurance: String = "", val policy: String = "", val assistanceName: String = "", val assistancePhone: String = "", val notes: String = "",
    val details: Map<String, String> = emptyMap())
data class WarningPreferences(val doors: Boolean = true, val service: Boolean = true, val brake: Boolean = true, val tyres: Boolean = true,
    val lowRangeKm: Int = 50, val speedLimitKmh: Int = 80)
data class TrackPoint(val latitude: Double, val longitude: Double, val elapsedMillis: Long, val accuracy: Float)
data class TripRecord(val id: String, val startedAt: Long, val endedAt: Long, val distanceMetres: Double, val maxKmh: Double,
    val movingMillis: Long, val harshBrakes: Int, val points: List<TrackPoint>, val interrupted: Boolean = false)
data class GarageData(val fuel: List<FuelEntry> = emptyList(), val reminders: List<Reminder> = emptyList(), val trips: List<TripRecord> = emptyList(),
    val parked: ParkedPlace? = null, val documents: List<GarageDocument> = emptyList(), val owner: OwnerRecord = OwnerRecord(),
    val warnings: WarningPreferences = WarningPreferences())

/** GPS estimates, separate from original head-unit EcoSense scores. No invented efficiency score. */
class TripAccumulator {
    private val trace = mutableListOf<TrackPoint>()
    private var last: TrackPoint? = null
    var distanceMetres = 0.0; private set
    var maxKmh = 0.0; private set
    var movingMillis = 0L; private set
    var harshBrakes = 0; private set
    private var lastSpeed = 0.0
    val points: List<TrackPoint> get() = trace.toList()
    fun add(point: TrackPoint): Boolean {
        if (!point.latitude.isFinite() || !point.longitude.isFinite() || point.latitude !in -90.0..90.0 || point.longitude !in -180.0..180.0 || !point.accuracy.isFinite() || point.accuracy !in 0f..50f) return false
        val previous = last
        if (previous != null) {
            val dt = point.elapsedMillis - previous.elapsedMillis
            if (dt <= 0) return false
            val distance = metres(previous.latitude, previous.longitude, point.latitude, point.longitude)
            val speed = distance / (dt / 1000.0)
            if (speed > 60) return false // reject implausible jumps, do not join across them
            if (dt > 30000) { last = point; lastSpeed = 0.0; trace += point; return true }
            val threshold = max(4.0, min(previous.accuracy, point.accuracy).toDouble() * .35)
            if (distance >= threshold) {
                distanceMetres += distance; maxKmh = max(maxKmh, speed * 3.6)
                if (speed > 1.5) movingMillis += dt
                if (dt in 1000..5000 && lastSpeed > 5 && (speed - lastSpeed) / (dt / 1000.0) < -3.0) harshBrakes++
                lastSpeed = speed
            } else lastSpeed = 0.0
        }
        last = point
        if (trace.size >= 10000) { val reduced = trace.filterIndexed { index, _ -> index % 2 == 0 }; trace.clear(); trace.addAll(reduced) }
        trace += point
        return true
    }
    companion object {
        fun metres(a: Double, b: Double, c: Double, d: Double): Double {
            val lat = Math.toRadians(c - a); val lon = Math.toRadians(d - b)
            val h = sin(lat / 2).pow(2) + cos(Math.toRadians(a)) * cos(Math.toRadians(c)) * sin(lon / 2).pow(2)
            return 6371000 * 2 * atan2(sqrt(h.coerceIn(0.0, 1.0)), sqrt((1 - h).coerceIn(0.0, 1.0)))
        }
    }
}
