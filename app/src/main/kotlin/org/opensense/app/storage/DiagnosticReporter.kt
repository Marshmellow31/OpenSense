package org.opensense.app.storage

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.opensense.core.diagnostics.DiagnosticEvent
import java.net.HttpURLConnection
import java.net.URL

/**
 * Opt-in, codes-only report. Sends allowlisted event codes plus app/Android/phone model.
 * Never sends packets, Bluetooth names or addresses, VINs, locations or exception text.
 */
object DiagnosticReporter {
    /** Set to the deployed collector URL (see workers/log-collector). Blank disables uploading. */
    const val ENDPOINT = "https://xuv-log-collector.harshil3105.workers.dev/"

    val configured get() = ENDPOINT.isNotBlank()

    fun payload(installId: String, family: String?, versionName: String, events: List<DiagnosticEvent>): String {
        val start = events.firstOrNull()?.timeMillis ?: 0L
        return JSONObject()
            .put("installId", installId)
            .put("app", versionName)
            .put("android", Build.VERSION.SDK_INT)
            .put("phone", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("family", family ?: "unset")
            .put("events", JSONArray(events.map { JSONObject().put("t", it.timeMillis - start).put("c", it.code.name) }))
            .toString()
    }

    suspend fun send(body: String): Boolean = withContext(Dispatchers.IO) {
        if (!configured) return@withContext false
        runCatching {
            val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
                connection.responseCode in 200..299
            } finally { connection.disconnect() }
        }.getOrDefault(false)
    }
}
