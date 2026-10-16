package org.opensense.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import org.opensense.core.diagnostics.*
import org.opensense.core.model.*
import org.opensense.core.protocol.*
import java.io.IOException
import java.util.UUID

enum class VehicleLink { DISCONNECTED, CONNECTING, WAITING, ACTIVE, FAILED }
data class VehicleLinkState(val link: VehicleLink = VehicleLink.DISCONNECTED, val vehicle: LiveVehicle = LiveVehicle(),
    val message: String = "No vehicle connected", val received: Int = 0, val rejected: Int = 0, val celsius: Boolean? = null, val busy: Boolean = false)
data class PairedHeadUnit(val id: String, val name: String)

/** Classic RFCOMM only. Device IDs never persisted/logged. Every session is explicit/cancellable. */
class VehicleConnection(context: Context, private val scope: CoroutineScope, private val log: DiagnosticLog) {
    private val context = context.applicationContext
    private val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val mutable = MutableStateFlow(VehicleLinkState())
    val state = mutable.asStateFlow()
    private val mutableDevices = MutableStateFlow<List<PairedHeadUnit>>(emptyList())
    val devices = mutableDevices.asStateFlow()
    private val paired = mutableMapOf<String, BluetoothDevice>()
    private var job: Job? = null
    @Volatile private var socket: BluetoothSocket? = null
    private var outgoing: Channel<ByteArray>? = null
    private var family: InfotainmentFamily? = null
    private var lastCommand = 0L
    private var session: VehicleSession? = null
    @Volatile private var generation = 0L
    private fun permission() = Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    fun canConnect() = permission()

    @SuppressLint("MissingPermission")
    fun refreshPaired() {
        if (job?.isActive == true) return
        paired.clear(); mutableDevices.value = emptyList()
        if (!permission()) { fail("Allow Nearby devices permission to list paired head units."); return }
        try {
            adapter?.bondedDevices.orEmpty().sortedBy { it.name.orEmpty() }.take(50).forEachIndexed { i, device -> paired["paired-$i"] = device }
            mutableDevices.value = paired.map { (id, device) -> PairedHeadUnit(id, device.name?.take(80)?.filter { !it.isISOControl() } ?: "Paired device") }
        } catch (_: SecurityException) { fail("Bluetooth permission was revoked.") }
    }
    @SuppressLint("MissingPermission")
    @Synchronized fun connect(id: String, selectedFamily: InfotainmentFamily) {
        disconnect()
        if (!permission()) { fail("Allow Nearby devices permission, then try again."); return }
        val device = paired[id] ?: run { fail("Refresh paired devices and select your head unit."); return }
        family = selectedFamily
        val sessionGeneration = generation
        mutable.value = VehicleLinkState(VehicleLink.CONNECTING, message = "Connecting to the selected head unit…")
        val queue = Channel<ByteArray>(32); outgoing = queue
        job = scope.launch(Dispatchers.IO) {
            var ownSocket: BluetoothSocket? = null
            try {
                coroutineScope {
                    val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
                    val connection = if (selectedFamily == InfotainmentFamily.LEGACY) device.createRfcommSocketToServiceRecord(uuid)
                        else device.createInsecureRfcommSocketToServiceRecord(uuid)
                    ownSocket = connection
                    synchronized(this@VehicleConnection) {
                        if (sessionGeneration != generation) { connection.close(); throw CancellationException() }
                        socket = connection
                    }
                    val connectDeadline = launch { delay(15000); runCatching { connection.close() } }
                    try { connection.connect() } finally { connectDeadline.cancel() }
                    ensureActive()
                    synchronized(this@VehicleConnection) {
                        if (sessionGeneration != generation) throw CancellationException()
                        mutable.value = VehicleLinkState(VehicleLink.WAITING, message = "Bluetooth connected · waiting for BlueSense messages")
                    }
                    log.record(DiagnosticCode.VEHICLE_SOCKET_CONNECTED)
                    val protocol = VehicleSession(selectedFamily, SystemClock::elapsedRealtime)
                    synchronized(this@VehicleConnection) {
                        if (sessionGeneration != generation) throw CancellationException()
                        session = protocol
                    }
                    val output = connection.outputStream
                    launch {
                        try { for (bytes in queue) { output.write(bytes); output.flush(); log.record(DiagnosticCode.VEHICLE_MESSAGE_SENT) } }
                        catch (e: Exception) { runCatching { connection.close() }; throw e }
                    }
                    val scheduled = Semaphore(64)
                    fun schedule(writes: List<SessionWrite>) {
                        writes.forEach { write ->
                            if (!scheduled.tryAcquire()) { connection.close(); throw IOException("too_many_pending_messages") }
                            launch { try { delay(write.delayMillis); queue.send(write.bytes) } finally { scheduled.release() } }
                        }
                    }
                    schedule(protocol.start())
                    if (selectedFamily == InfotainmentFamily.LEGACY) launch {
                        repeat(4) { delay(20000); if (!protocol.ready) schedule(protocol.start()) }
                    }
                    val lastRx = java.util.concurrent.atomic.AtomicLong(SystemClock.elapsedRealtime())
                    launch {
                        while (isActive) {
                            delay(1000)
                            if (!permission()) { connection.close(); throw IOException("permission_revoked") }
                            val timeout = if (selectedFamily == InfotainmentFamily.LEGACY && !protocol.ready) 105000 else 20000
                            if (SystemClock.elapsedRealtime() - lastRx.get() > timeout) {
                                connection.close(); throw IOException("session_timeout")
                            }
                            synchronized(this@VehicleConnection) {
                                if (sessionGeneration == generation) mutable.update { it.copy(vehicle = protocol.expire()) }
                            }
                        }
                    }
                    val input = connection.inputStream
                    val buffer = ByteArray(1024)
                    while (isActive) {
                        val count = input.read(buffer)
                        if (count < 0) throw IOException("stream_closed")
                        if (count == 0) continue
                        val update = synchronized(this@VehicleConnection) {
                            if (sessionGeneration != generation) throw CancellationException()
                            val result = protocol.receive(buffer.copyOf(count))
                            if (result.fatal) throw IOException("protocol_rejected")
                            if (result.accepted > 0) lastRx.set(SystemClock.elapsedRealtime())
                            val old = mutable.value
                            mutable.value = VehicleLinkState(if (protocol.ready) VehicleLink.ACTIVE else VehicleLink.WAITING,
                                protocol.vehicle, if (protocol.ready) "Receiving BlueSense messages · mappings need vehicle confirmation" else "Waiting for the BlueSense startup response",
                                old.received + result.accepted, old.rejected + result.rejected, protocol.celsius, protocol.busy)
                            result
                        }
                        schedule(update.writes)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { synchronized(this@VehicleConnection) { if (sessionGeneration == generation) fail("Connection ended or protocol did not respond. Check the selected app generation, Bluetooth pairing and ignition; reconnect while parked.") } }
            finally { runCatching { ownSocket?.close() }; synchronized(this@VehicleConnection) { if (sessionGeneration == generation) { socket = null; session = null } }; queue.close() }
        }
    }
    @Synchronized private fun send(bytes: ByteArray): Boolean {
        if (!permission()) { disconnect(); fail("Bluetooth permission was revoked."); return false }
        if (mutable.value.link != VehicleLink.ACTIVE || session?.busy == true) return false
        val now = SystemClock.elapsedRealtime()
        if (now - lastCommand < 300) return false
        val queued = outgoing?.trySend(bytes)?.isSuccess == true
        if (queued) lastCommand = now
        return queued
        // A successful socket write is not a head-unit acknowledgement; no optimistic readings.
    }
    fun media(action: MediaAction) {
        val media = mutable.value.vehicle.media ?: return
        if (media.source == null && media.playback == null && media.volume == null) return
        family?.let { send(VehicleCommands.media(it, action)) }
    }
    fun volume(value: Int) { family?.let { send(VehicleCommands.volume(it, value)) } }
    fun radioMute(muted: Boolean) { family?.let { send(VehicleCommands.radioMute(it, muted)) } }
    private fun radioAvailable() = family == InfotainmentFamily.NEW && mutable.value.vehicle.media?.source == 4
    fun radioPreset(position: Int) { if (radioAvailable()) send(VehicleCommands.radioPreset(position)) }
    fun nextRadioBand() { if (radioAvailable()) send(VehicleCommands.nextRadioBand()) }
    fun storeRadioPreset(position: Int): Boolean {
        if (!radioAvailable()) return false
        val station = mutable.value.vehicle.media?.station?.trim() ?: return false
        if (!station.matches(Regex("[0-9]{2,4}(\\.[0-9]{1,2})?"))) return false
        val parts = station.split('.')
        return send(VehicleCommands.storeRadioPreset(position, parts[0].toInt(), parts.getOrNull(1)?.toInt() ?: 0))
    }
    fun climate(switch: ClimateSwitch, enabled: Boolean) { if (family == InfotainmentFamily.NEW && mutable.value.vehicle.climate?.hasStatus == true) send(VehicleCommands.climate(switch, enabled)) }
    fun fan(level: Int) { if (family == InfotainmentFamily.NEW && mutable.value.vehicle.climate?.hasStatus == true) send(VehicleCommands.fan(level)) }
    fun temperature(value: Double) { if (family == InfotainmentFamily.NEW && mutable.value.vehicle.climate?.hasStatus == true && session?.celsius == true) send(VehicleCommands.temperature(value)) }
    fun vent(mode: Int) { if (family == InfotainmentFamily.NEW && mutable.value.vehicle.climate?.hasStatus == true) send(VehicleCommands.vent(mode)) }
    private fun fail(message: String) { mutable.value = VehicleLinkState(VehicleLink.FAILED, message = message); log.record(DiagnosticCode.VEHICLE_SESSION_FAILED) }
    @Synchronized fun disconnect() {
        generation++
        job?.cancel(); job = null
        runCatching { socket?.close() }; socket = null
        outgoing?.close(); outgoing = null; family = null; session = null; lastCommand = 0
        mutable.value = VehicleLinkState()
    }
}
