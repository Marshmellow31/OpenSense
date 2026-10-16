package org.opensense.app.storage

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.opensense.core.garage.*
import java.io.File

/** Private, versioned local records. No cloud, identifiers in diagnostics, or automatic sharing. */
class GarageStore private constructor(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "garage-v1.json"))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val ready = CompletableDeferred<Unit>()
    private val mutable = MutableStateFlow(GarageData())
    val data = mutable.asStateFlow()
    private val mutableProblem = MutableStateFlow<String?>(null)
    val problem = mutableProblem.asStateFlow()
    init {
        scope.launch {
            try { if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
                require(file.baseFile.length() <= 12 * 1024 * 1024)
                mutable.value = decode(JSONObject(file.openRead().bufferedReader().use { it.readText() }))
            } } catch (_: Exception) { mutableProblem.value = "Saved records could not be read. They have been preserved; restart before making changes." }
            finally { ready.complete(Unit) }
        }
    }
    suspend fun update(change: (GarageData) -> GarageData): Boolean {
        ready.await()
        return withContext(Dispatchers.IO) { mutex.withLock {
            if (mutableProblem.value?.startsWith("Saved records") == true) return@withLock false
            val next = change(mutable.value)
            try {
                require(next.fuel.size <= 1000 && next.reminders.size <= 200 && next.trips.size <= 50 && next.documents.size <= 100)
                val bytes = encode(next).toString().toByteArray()
                require(bytes.size <= 12 * 1024 * 1024)
                val output = file.startWrite()
                try { output.write(bytes); file.finishWrite(output) } catch (e: Exception) { file.failWrite(output); throw e }
                mutable.value = next; mutableProblem.value = null; true
            } catch (_: Exception) { mutableProblem.value = "Could not save. Check free storage or remove an old record, then try again."; false }
        } }
    }
    suspend fun snapshot(): GarageData { ready.await(); return mutable.value }
    fun export(): String = encode(mutable.value.copy(owner = OwnerRecord(), documents = emptyList(), parked = null,
        trips = mutable.value.trips.map { it.copy(points = emptyList()) })).toString(2)
    companion object {
        @Volatile private var instance: GarageStore? = null
        fun get(context: Context): GarageStore = instance ?: synchronized(this) { instance ?: GarageStore(context.applicationContext).also { instance = it } }
        private fun array(items: Iterable<JSONObject>) = JSONArray().apply { items.forEach(::put) }
        private fun JSONObject.rows(key: String): List<JSONObject> = optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty()
        private fun encode(d: GarageData): JSONObject = JSONObject().apply {
            put("schema", 1)
            put("fuel", array(d.fuel.map { f -> JSONObject().put("id", f.id).put("time", f.time).put("odo", f.odometerKm).put("litres", f.litres).put("cost", f.costMinor).put("full", f.fullTank).put("note", f.note) }))
            put("reminders", array(d.reminders.map { r -> JSONObject().put("id", r.id).put("title", r.title).put("due", r.dueAt).put("lead", r.leadMillis).put("done", r.done).put("notified", r.notified) }))
            put("documents", array(d.documents.map { v -> JSONObject().put("id", v.id).put("name", v.name).put("uri", v.uri).put("mime", v.mime).put("manual", v.manual) }))
            put("owner", JSONObject().apply { with(d.owner) { put("name", name); put("phone", phone); put("email", email); put("registration", registration); put("insurance", insurance); put("policy", policy); put("assistanceName", assistanceName); put("assistancePhone", assistancePhone); put("notes", notes); put("details", JSONObject(details)) } })
            d.parked?.let { p -> put("parked", JSONObject().put("lat", p.latitude).put("lon", p.longitude).put("accuracy", p.accuracy.toDouble()).put("time", p.time).put("note", p.note)) }
            put("warnings", JSONObject().apply { with(d.warnings) { put("doors", doors); put("service", service); put("brake", brake); put("tyres", tyres); put("range", lowRangeKm); put("speed", speedLimitKmh) } })
            put("trips", array(d.trips.map { t -> JSONObject().apply {
                put("id", t.id); put("start", t.startedAt); put("end", t.endedAt); put("distance", t.distanceMetres); put("max", t.maxKmh); put("moving", t.movingMillis); put("brakes", t.harshBrakes); put("interrupted", t.interrupted)
                put("points", array(t.points.map { p -> JSONObject().put("lat", p.latitude).put("lon", p.longitude).put("elapsed", p.elapsedMillis).put("accuracy", p.accuracy.toDouble()) }))
            } }))
        }
        private fun decode(j: JSONObject): GarageData {
            require(j.getInt("schema") == 1)
            val o = j.optJSONObject("owner") ?: JSONObject(); val w = j.optJSONObject("warnings") ?: JSONObject()
            val parked = j.optJSONObject("parked")?.let { ParkedPlace(it.getDouble("lat"), it.getDouble("lon"), it.getDouble("accuracy").toFloat(), it.getLong("time"), it.optString("note")) }
            return GarageData(
                fuel = j.rows("fuel").take(1000).map { FuelEntry(it.getString("id"), it.getLong("time"), it.getDouble("odo"), it.getDouble("litres"), it.getLong("cost"), it.getBoolean("full"), it.optString("note")) },
                reminders = j.rows("reminders").take(200).map { Reminder(it.getInt("id"), it.getString("title"), it.getLong("due"), it.getLong("lead"), it.getBoolean("done"), it.optBoolean("notified")) },
                documents = j.rows("documents").take(100).map { GarageDocument(it.getString("id"), it.getString("name"), it.getString("uri"), it.getString("mime"), it.optBoolean("manual")) },
                owner = OwnerRecord(o.optString("name"), o.optString("phone"), o.optString("email"), o.optString("registration"), o.optString("insurance"), o.optString("policy"), o.optString("assistanceName"), o.optString("assistancePhone"), o.optString("notes"),
                    o.optJSONObject("details")?.let { a -> a.keys().asSequence().associateWith { a.getString(it) } }.orEmpty()),
                warnings = WarningPreferences(w.optBoolean("doors", true), w.optBoolean("service", true), w.optBoolean("brake", true), w.optBoolean("tyres", true), w.optInt("range", 50).coerceIn(10, 200), w.optInt("speed", 80).coerceIn(30, 120)),
                parked = parked,
                trips = j.rows("trips").take(50).map { t -> TripRecord(t.getString("id"), t.getLong("start"), t.getLong("end"), t.getDouble("distance"), t.getDouble("max"), t.getLong("moving"), t.getInt("brakes"),
                    t.rows("points").take(2000).map { TrackPoint(it.getDouble("lat"), it.getDouble("lon"), it.getLong("elapsed"), it.getDouble("accuracy").toFloat()) }, t.optBoolean("interrupted")) }
            )
        }
    }
}
