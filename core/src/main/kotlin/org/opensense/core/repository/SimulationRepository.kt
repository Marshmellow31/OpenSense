package org.opensense.core.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.opensense.core.diagnostics.DiagnosticCode
import org.opensense.core.diagnostics.DiagnosticLog
import org.opensense.core.model.VehicleSnapshot
import org.opensense.core.protocol.DecodeResult
import org.opensense.core.protocol.SimulationStreamDecoder
import org.opensense.core.transport.ConnectionState
import org.opensense.core.transport.ReadOnlyTransport

/** This repository accepts simulation input only; never attach a real vehicle transport. */
class SimulationRepository(
    private val transport: ReadOnlyTransport,
    private val scope: CoroutineScope,
    private val log: DiagnosticLog,
    private val retryMillis: Long = 1000,
    private val maxRetries: Int = 2,
) {
    private val mutableSample = MutableStateFlow<VehicleSnapshot?>(null)
    val sample = mutableSample.asStateFlow()
    private val mutableState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state = mutableState.asStateFlow()
    private var job: Job? = null

    fun connect() {
        if (job?.isActive == true) return
        log.record(DiagnosticCode.DEMO_STARTED)
        job = scope.launch {
            var retries = 0
            while (true) {
                val decoder = SimulationStreamDecoder()
                mutableState.value = ConnectionState.CONNECTING
                try {
                    transport.frames().collect { bytes ->
                        for (result in decoder.feed(bytes)) when (result) {
                            is DecodeResult.Sample -> {
                                mutableSample.value = result.value
                                mutableState.value = ConnectionState.CONNECTED
                                log.record(DiagnosticCode.SAMPLE_ACCEPTED)
                            }
                            is DecodeResult.Rejected -> log.record(DiagnosticCode.FRAME_REJECTED)
                        }
                    }
                    error("stream_closed")
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    mutableSample.value = null
                    if (retries++ >= maxRetries) {
                        mutableState.value = ConnectionState.FAILED
                        log.record(DiagnosticCode.STREAM_FAILED)
                        return@launch
                    }
                    mutableState.value = ConnectionState.RETRYING
                    log.record(DiagnosticCode.RECONNECTING)
                    delay(retryMillis)
                }
            }
        }
    }
    fun disconnect() {
        job?.cancel(); job = null
        mutableSample.value = null
        mutableState.value = ConnectionState.DISCONNECTED
        log.record(DiagnosticCode.DEMO_STOPPED)
    }
}
