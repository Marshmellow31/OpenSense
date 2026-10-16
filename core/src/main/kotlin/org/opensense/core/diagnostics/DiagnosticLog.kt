package org.opensense.core.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DiagnosticCode { DEMO_STARTED, DEMO_STOPPED, SAMPLE_ACCEPTED, FRAME_REJECTED, RECONNECTING, STREAM_FAILED, PERMISSION_DENIED, DISCOVERY_STARTED, DISCOVERY_STOPPED, SERVICES_OBSERVED, ADAPTER_UNAVAILABLE, VEHICLE_SOCKET_CONNECTED, VEHICLE_MESSAGE_SENT, VEHICLE_SESSION_FAILED }
data class DiagnosticEvent(val timeMillis: Long, val code: DiagnosticCode)

/** Allowlisted event codes only. No raw packets, names, MACs, VINs, locations or exception text. */
class DiagnosticLog(private val now: () -> Long = System::currentTimeMillis) {
    private val mutable = MutableStateFlow<List<DiagnosticEvent>>(emptyList())
    val events = mutable.asStateFlow()
    @Synchronized fun record(code: DiagnosticCode) { mutable.value = (mutable.value + DiagnosticEvent(now(), code)).takeLast(100) }
    @Synchronized fun clear() { mutable.value = emptyList() }
}
