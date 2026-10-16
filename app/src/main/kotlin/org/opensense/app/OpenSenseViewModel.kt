package org.opensense.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opensense.app.storage.SettingsStore
import org.opensense.bluetooth.BluetoothDiscovery
import org.opensense.core.diagnostics.DiagnosticLog
import org.opensense.core.repository.SimulationRepository
import org.opensense.core.simulation.SimulatedTransport
import org.opensense.core.model.VehicleProfile

class OpenSenseViewModel(application: Application) : AndroidViewModel(application) {
    val log = DiagnosticLog()
    val bluetooth = BluetoothDiscovery(application, log)
    val repository = SimulationRepository(SimulatedTransport(), viewModelScope, log)
    private val store = SettingsStore(application)
    private val mutableDark = MutableStateFlow(store.darkTheme)
    val dark = mutableDark.asStateFlow()
    private val mutableProfile = MutableStateFlow(store.profile)
    val profile = mutableProfile.asStateFlow()
    fun selectProfile(value: VehicleProfile?) { pause(); store.profile = value; mutableProfile.value = value }
    fun setDark(value: Boolean) { store.darkTheme = value; mutableDark.value = value }
    fun startDemo() { bluetooth.close(); repository.connect() }
    fun discover() { repository.disconnect(); bluetooth.start() }
    fun pause() { repository.disconnect(); bluetooth.close() }
    override fun onCleared() { pause() }
}
