package org.opensense.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opensense.core.diagnostics.DiagnosticCode
import org.opensense.core.diagnostics.DiagnosticLog

data class ObservedDevice(val id: String, val label: String, val paired: Boolean, val services: List<String> = emptyList())
data class DiscoveryState(val devices: List<ObservedDevice> = emptyList(), val scanning: Boolean = false,
    val message: String = "Ready to discover nearby devices or explicitly connect a paired head unit.")

/** Classic discovery/SDP only. No socket, pairing automation, GATT writes, or protocol commands. */
class BluetoothDiscovery(context: Context, private val log: DiagnosticLog) {
    private val context = context.applicationContext
    private val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val mutable = MutableStateFlow(DiscoveryState())
    val state = mutable.asStateFlow()
    private val devices = linkedMapOf<String, BluetoothDevice>()
    private var registered = false
    private val handler = Handler(Looper.getMainLooper())
    private val stopTimeout = Runnable { stop() }

    fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= 31)
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    fun hasPermissions() = requiredPermissions().all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    fun permissionDenied() {
        stop(); devices.clear()
        mutable.value = DiscoveryState(message = "Bluetooth permission denied. Enable Nearby devices (or Location on Android 8–11) in app settings to retry.")
        log.record(DiagnosticCode.PERMISSION_DENIED)
    }

    @SuppressLint("MissingPermission")
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                when (intent.action) {
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        handler.removeCallbacks(stopTimeout)
                        mutable.value = mutable.value.copy(scanning = false, message = "Discovery finished. A visible device does not prove BlueSense compatibility.")
                        log.record(DiagnosticCode.DISCOVERY_STOPPED)
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> if (!hasPermissions() || adapter?.isEnabled != true) {
                        stop(); devices.clear(); mutable.value = DiscoveryState(message = "Bluetooth is off or unavailable.")
                    }
                    BluetoothDevice.ACTION_FOUND -> device(intent)?.let { addDevice(it) }
                    BluetoothDevice.ACTION_UUID -> device(intent)?.let { found ->
                        if (devices.values.any { it.address == found.address }) {
                            val uuids = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID, ParcelUuid::class.java)?.toList()
                            else @Suppress("DEPRECATION") (intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)?.filterIsInstance<ParcelUuid>())
                            val id = devices.entries.firstOrNull { it.value.address == found.address }?.key
                            mutable.value = mutable.value.copy(devices = mutable.value.devices.map {
                                if (it.id == id) it.copy(services = uuids.orEmpty().map { uuid -> uuid.toString() }.distinct()) else it
                            }, message = "Service discovery returned. UUIDs are observations, not a verified vehicle protocol.")
                            log.record(DiagnosticCode.SERVICES_OBSERVED)
                        }
                    }
                }
            } catch (_: SecurityException) { permissionDenied() }
        }
    }

    private fun device(intent: Intent): BluetoothDevice? = if (Build.VERSION.SDK_INT >= 33)
        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

    @SuppressLint("MissingPermission")
    private fun addDevice(device: BluetoothDevice) {
        val existing = devices.entries.firstOrNull { it.value.address == device.address }?.key
        if (existing != null || devices.size >= 50) return
        val id = "device-${devices.size + 1}"
        devices[id] = device
        // Names and addresses remain private even in the on-screen diagnostic record.
        mutable.value = mutable.value.copy(devices = mutable.value.devices + ObservedDevice(
            id, "Nearby device ${devices.size}", device.bondState == BluetoothDevice.BOND_BONDED))
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermissions()) { permissionDenied(); return }
        try {
            val current = adapter
            if (current == null || !current.isEnabled) {
                mutable.value = DiscoveryState(message = "Turn on Bluetooth in Android settings. This phone may not have a Bluetooth adapter.")
                log.record(DiagnosticCode.ADAPTER_UNAVAILABLE); return
            }
            register()
            current.cancelDiscovery()
            devices.clear(); mutable.value = DiscoveryState()
            current.bondedDevices.forEach(::addDevice)
            val started = current.startDiscovery()
            mutable.value = mutable.value.copy(scanning = started, message = if (started)
                "Discovering nearby Classic devices. Device names are hidden; use Android settings to identify and pair your head unit."
                else "Discovery could not start. On Android 8–11 also enable system Location, then retry.")
            handler.removeCallbacks(stopTimeout)
            if (started) { handler.postDelayed(stopTimeout, 20000); log.record(DiagnosticCode.DISCOVERY_STARTED) }
        } catch (_: SecurityException) { permissionDenied() }
        catch (_: IllegalStateException) { mutable.value = DiscoveryState(message = "Bluetooth adapter unavailable. Retry after enabling Bluetooth.") }
    }

    @SuppressLint("MissingPermission")
    fun inspectServices(id: String) {
        if (!hasPermissions()) { permissionDenied(); return }
        try {
            stop()
            val device = devices[id] ?: return
            val started = device.fetchUuidsWithSdp()
            mutable.value = mutable.value.copy(message = if (started) "Requesting standard Bluetooth service discovery. No vehicle commands are sent."
                else "Service discovery could not start. Pair in Android settings and retry.")
        } catch (_: SecurityException) { permissionDenied() }
    }

    private fun register() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND); addAction(BluetoothDevice.ACTION_UUID)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        // Bluetooth broadcasts may originate from a privileged process with a different UID.
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        else context.registerReceiver(receiver, filter)
        registered = true
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        handler.removeCallbacks(stopTimeout)
        try { if (hasPermissions()) adapter?.cancelDiscovery() } catch (_: SecurityException) { /* permission revoked */ }
        mutable.value = mutable.value.copy(scanning = false)
    }
    fun close() {
        stop()
        if (registered) { context.unregisterReceiver(receiver); registered = false }
        devices.clear(); mutable.value = DiscoveryState()
    }
}
