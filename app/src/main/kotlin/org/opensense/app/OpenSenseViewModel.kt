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
    fun selectProfile(value: VehicleProfile?) { pause(); store.profile = value; mutableProfile.value = value }
    fun setDark(value: Boolean) { store.darkTheme = value; mutableDark.value = value }
    fun startDemo() { vehicleConnection.disconnect(); bluetooth.close(); mutableControls.value = DemoControls(); repository.connect() }
    fun stopDemo() { repository.disconnect(); mutableControls.value = DemoControls() }
    fun discover() { vehicleConnection.disconnect(); repository.disconnect(); bluetooth.start(); vehicleConnection.refreshPaired() }
    fun connectVehicle(id: String) { profile.value?.let { stopDemo(); bluetooth.stop(); vehicleConnection.connect(id, it.family) } }
    fun pause() { stopDemo(); bluetooth.close(); vehicleConnection.disconnect() }
    override fun onCleared() { pause() }
}
