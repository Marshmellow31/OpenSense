package org.opensense.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opensense.app.storage.SettingsStore
import org.opensense.bluetooth.BluetoothDiscovery
import org.opensense.bluetooth.VehicleConnection
import org.opensense.core.diagnostics.DiagnosticLog
import org.opensense.core.repository.SimulationRepository
import org.opensense.core.simulation.SimulatedTransport
import org.opensense.core.model.VehicleProfile
import org.opensense.app.storage.GarageStore
import org.opensense.app.storage.DiagnosticReporter
import org.opensense.bluetooth.VehicleLink
import kotlinx.coroutines.launch

data class DemoControls(
    val target: Int = 22, val fan: Int = 3, val ac: Boolean = true, val auto: Boolean = false,
    val recirculate: Boolean = true, val rear: Boolean = false, val defrost: Boolean = false,
    val airflow: String = "Face", val playing: Boolean = false, val volume: Float = 0.45f,
    val source: String = "Bluetooth", val track: Int = 0,
)

class OpenSenseViewModel(application: Application) : AndroidViewModel(application) {
    val log = DiagnosticLog()
    val bluetooth = BluetoothDiscovery(application, log)
    val vehicleConnection = VehicleConnection(application, viewModelScope, log)
    val garage = GarageStore.get(application)
    val routeRequest = MutableStateFlow<String?>(null)
    fun saveGarage(change: (org.opensense.core.garage.GarageData) -> org.opensense.core.garage.GarageData, saved: () -> Unit = {}) =
        viewModelScope.launch { if (garage.update(change)) saved() }
    val repository = SimulationRepository(SimulatedTransport(), viewModelScope, log)
    private val store = SettingsStore(application)
    private val mutableDark = MutableStateFlow(store.darkTheme)
    val dark = mutableDark.asStateFlow()
    private val mutableProfile = MutableStateFlow(store.profile)
    val profile = mutableProfile.asStateFlow()
    private val mutableControls = MutableStateFlow(DemoControls())
    val controls = mutableControls.asStateFlow()
    fun changeDemo(change: (DemoControls) -> DemoControls) {
        if (repository.sample.value != null) mutableControls.value = change(mutableControls.value)
    }
    private val mutableShare = MutableStateFlow(store.shareDiagnostics)
    val shareDiagnostics = mutableShare.asStateFlow()
    private val mutableAsk = MutableStateFlow(DiagnosticReporter.configured && !store.consentAsked)
    val askConsent = mutableAsk.asStateFlow()
    fun answerConsent(allow: Boolean) { store.consentAsked = true; mutableAsk.value = false; setShareDiagnostics(allow) }
    private var reported = 0L
    fun setShareDiagnostics(value: Boolean) {
        store.shareDiagnostics = value; mutableShare.value = value
        if (!value) store.resetInstallId()
    }
    /** Opt-in, codes-only upload when a vehicle session ends. */
    private fun reportSession() {
        if (!store.shareDiagnostics || !DiagnosticReporter.configured) return
        val fresh = log.events.value.filter { it.timeMillis > reported }
        if (fresh.isEmpty()) return
        reported = fresh.last().timeMillis
        val version = runCatching { getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0).versionName }.getOrNull() ?: "?"
        val body = DiagnosticReporter.payload(store.installId, store.profile?.family?.name, version, fresh)
        viewModelScope.launch { DiagnosticReporter.send(body) }
    }
    init {
        viewModelScope.launch {
            var last = vehicleConnection.state.value.link
            vehicleConnection.state.collect { s ->
                val ended = s.link in setOf(VehicleLink.DISCONNECTED, VehicleLink.FAILED) && last !in setOf(VehicleLink.DISCONNECTED, VehicleLink.FAILED)
                last = s.link
                if (ended) reportSession()
            }
        }
    }
    fun selectProfile(value: VehicleProfile?) { pause(); store.profile = value; mutableProfile.value = value }
    fun setDark(value: Boolean) { store.darkTheme = value; mutableDark.value = value }
    fun startDemo() { vehicleConnection.disconnect(); bluetooth.close(); mutableControls.value = DemoControls(); repository.connect() }
    fun stopDemo() { repository.disconnect(); mutableControls.value = DemoControls() }
    fun discover() { vehicleConnection.disconnect(); repository.disconnect(); bluetooth.start(); vehicleConnection.refreshPaired() }
    fun connectVehicle(id: String) { profile.value?.let { stopDemo(); bluetooth.stop(); vehicleConnection.connect(id, it.family) } }
    fun pause() { stopDemo(); bluetooth.close(); vehicleConnection.disconnect() }
    override fun onCleared() { pause() }
}
