package org.opensense.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.opensense.app.OpenSenseViewModel
import org.opensense.bluetooth.VehicleLink
import org.opensense.bluetooth.VehicleLinkState
import org.opensense.core.model.*
import org.opensense.core.protocol.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable private fun LiveReading(label: String, value: Any?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value?.toString() ?: "—", style = MaterialTheme.typography.titleMedium)
    }
}
@Composable fun LiveScreen(tab: String, state: VehicleLinkState, profile: VehicleProfile?, model: OpenSenseViewModel) {
    val v = state.vehicle
    val garage by model.garage.data.collectAsStateWithLifecycle()
    val preferences = garage.warnings
    val active = state.link == VehicleLink.ACTIVE && !state.busy
    TonalPanel {
        Text(if (active) "Vehicle session" else "Connecting to your vehicle", style = MaterialTheme.typography.titleMedium)
        Text(state.message, style = MaterialTheme.typography.bodySmall)
        if (state.busy) Text("The head unit reports that it is busy. Controls will return when it is ready.", style = MaterialTheme.typography.bodySmall)
        Text("APK-derived readings · confirm against your head unit", style = MaterialTheme.typography.labelSmall)
    }
    when (tab) {
        "Vehicle" -> {
            CarStage(Modifier.fillMaxWidth())
            TonalPanel {
                LiveReading("Distance to empty", v.range?.let { "$it km" })
                LiveReading("Fuel economy (head-unit units)", v.economy)
                LiveReading("Speed (head-unit units)", v.speed)
                if (preferences.doors) LiveReading("Door status code", v.doors)
                if (preferences.service) LiveReading("Service status code", v.service)
                if (preferences.brake) LiveReading("Brake-pad status code", v.brake)
                LiveReading("Park lamp status code", v.parkLamp)
                LiveReading("Lamp status code", v.lamps)
                LiveReading("Wiper status code", v.wipers)
            }
            if ((v.range ?: Int.MAX_VALUE) <= preferences.lowRangeKm) Text("Reported range is at or below your ${preferences.lowRangeKm} km reference.", color = MaterialTheme.colorScheme.tertiary)
            OutlinedButton(onClick = { model.routeRequest.value = "Your garage" }) { Text("Open your garage") }
            Text("A blank reading means the vehicle has not supplied a supported value. Fuel percentage is unavailable; it is never inferred from distance to empty.", style = MaterialTheme.typography.bodySmall)
        }
        "Climate" -> {
            val c = v.climate
            val control = active && profile?.family == InfotainmentFamily.NEW && c?.hasStatus == true
            val temperatureControl = control && state.celsius == true
            TonalPanel {
                Text("Climate status", style = MaterialTheme.typography.titleLarge)
                LiveReading("Set temperature", c?.target?.let { "$it ${if (profile?.family == InfotainmentFamily.LEGACY || state.celsius == true) "°C" else "(head-unit units)"}" })
                LiveReading("Outside", c?.outside?.let { "$it ${if (profile?.family == InfotainmentFamily.LEGACY || state.celsius == true) "°C" else "(head-unit units)"}" })
                LiveReading("Blower status code", c?.fan)
                LiveReading("Vent mode code", c?.mode)
                Text(if (profile?.family == InfotainmentFamily.LEGACY) "The legacy app provides climate information. Remote climate controls are unavailable for this profile." else "Controls become available after climate status is received. Changes are shown only after a head-unit response.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { c?.target?.let { model.vehicleConnection.temperature((it - .5).coerceIn(17.0, 32.0)) } }, enabled = temperatureControl && c?.target != null) { Text("− 0.5°") }
                    OutlinedButton(onClick = { c?.target?.let { model.vehicleConnection.temperature((it + .5).coerceIn(17.0, 32.0)) } }, enabled = temperatureControl && c?.target != null) { Text("+ 0.5°") }
                }
                if (control && !temperatureControl) Text("Temperature writes wait for a Celsius unit report from the head unit.", style = MaterialTheme.typography.bodySmall)
            }
            TonalPanel {
                listOf(Triple("Air conditioning", ClimateSwitch.AC, c?.ac), Triple("Automatic", ClimateSwitch.AUTO, c?.auto),
                    Triple("Econ", ClimateSwitch.ECON, c?.econ), Triple("Recirculation", ClimateSwitch.RECIRCULATE, c?.recirculate),
                    Triple("Rear AC", ClimateSwitch.REAR, c?.rear)).forEach { (label, selector, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$label${if (value == null) " · unavailable" else ""}", Modifier.weight(1f))
                        Switch(checked = value == true, enabled = control && value != null,
                            onCheckedChange = { model.vehicleConnection.climate(selector, it) }, modifier = Modifier.semantics { contentDescription = label })
                    }
                }
                Section("Blower")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..7).forEach { level -> OutlinedButton(onClick = { model.vehicleConnection.fan(level) }, enabled = control) { Text(if (level == 0) "Off" else "$level") } }
                }
                Section("Vent mode")
                Text("Original head-unit mode numbers; icon meanings still need confirmation.", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..5).forEach { mode -> OutlinedButton(onClick = { model.vehicleConnection.vent(mode) }, enabled = control) { Text("$mode") } }
                }
            }
        }
        "Media" -> {
            val media = v.media
            val maxVolume = if (profile?.family == InfotainmentFamily.LEGACY) 45 else 30
            TonalPanel {
                Text(media?.title ?: "Waiting for track information", style = MaterialTheme.typography.headlineSmall)
                LiveReading("Source code", media?.source)
                LiveReading("Playback status code", media?.playback)
                LiveReading("Volume", media?.volume)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val radio = profile?.family == InfotainmentFamily.NEW && media?.source == 4
                    listOf("Previous" to MediaAction.PREVIOUS, (if (radio) "Unmute" else "Play") to MediaAction.PLAY, (if (radio) "Mute" else "Pause") to MediaAction.PAUSE, "Next" to MediaAction.NEXT).forEach { (label, action) ->
                        if (radio && action in setOf(MediaAction.PLAY, MediaAction.PAUSE)) {
                            OutlinedButton(onClick = { model.vehicleConnection.radioMute(action == MediaAction.PAUSE) }, enabled = active) { Text(label) }
                        } else {
                        OutlinedButton(onClick = { model.vehicleConnection.media(action) }, enabled = active && media != null) { Text(label) }
                        }
                    }
                }
                if (profile?.family == InfotainmentFamily.LEGACY) Text("Play and Pause both send the legacy app's playback toggle.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { media?.volume?.let { model.vehicleConnection.volume((it - 1).coerceIn(0, maxVolume)) } }, enabled = active && media?.volume in 1..maxVolume) { Text("Volume −") }
                    OutlinedButton(onClick = { media?.volume?.let { model.vehicleConnection.volume((it + 1).coerceIn(0, maxVolume)) } }, enabled = active && media?.volume in 0 until maxVolume) { Text("Volume +") }
                }
                Section("Source")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Triple("iPod", MediaAction.IPOD, 0), Triple("USB", MediaAction.USB, 1), Triple("CD", MediaAction.CD, 2),
                        Triple("Bluetooth", MediaAction.BLUETOOTH, 3), Triple("Radio", MediaAction.RADIO, 4), Triple("AUX", MediaAction.AUX, 5)).forEach { (label, action, code) ->
                        OutlinedButton(onClick = { model.vehicleConnection.media(action) }, enabled = active && media != null &&
                            (profile?.family == InfotainmentFamily.LEGACY || code in media.availableSources)) { Text(label) }
                    }
                }
                Text("Sources are enabled from reported availability on New BlueSense. A command write is not confirmation that playback changed.", style = MaterialTheme.typography.bodySmall)
            }
            if (profile?.family == InfotainmentFamily.NEW && media?.source == 4) TonalPanel {
                Text("Radio", style = MaterialTheme.typography.titleLarge)
                Text(media.station ?: "Waiting for station information", style = MaterialTheme.typography.headlineMedium)
                Text(listOf("FM", "FM1", "FM2", "Auto FM", "AM", "AM1", "AM2", "Auto AM").getOrNull(media.radioBand ?: -1) ?: "Band not reported", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { model.vehicleConnection.nextRadioBand() }, enabled = active) { Text("Next band") }
                Section("Presets")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..6).forEach { position -> FilterChip(selected = media.preset == position, onClick = { model.vehicleConnection.radioPreset(position) }, enabled = active, label = { Text("$position") }) }
                }
                Text("Save the current station to a preset", style = MaterialTheme.typography.bodyMedium)
                var radioMessage by remember { mutableStateOf<String?>(null) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..6).forEach { position -> OutlinedButton(onClick = {
                        radioMessage = if (model.vehicleConnection.storeRadioPreset(position)) "Preset save requested; wait for the head-unit response." else "Wait for a numeric station frequency and a ready radio connection, then retry."
                    }, enabled = active && media.station != null) { Text("Save $position") } }
                }
                radioMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        "Insights" -> {
            TonalPanel {
                Text("Tyre status", style = MaterialTheme.typography.titleLarge)
                val positions = listOf("Front left", "Front right", "Rear right", "Rear left", "Spare")
                positions.forEachIndexed { i, position ->
                    LiveReading("$position pressure code", v.tyrePressures?.getOrNull(i))
                    LiveReading("$position temperature code", v.tyreTemperatures?.getOrNull(i))
                }
                v.newTyres?.forEach { tyre ->
                    LiveReading("${tyre.position} pressure (head-unit units)", tyre.pressure)
                    LiveReading("${tyre.position} temperature (head-unit units)", tyre.temperature)
                }
                Text("Raw tyre codes have no confirmed units. Missing TPMS values do not establish hardware fitment or tyre safety.", style = MaterialTheme.typography.bodySmall)
            }
            Text("Trips, EcoSense, parking location and reminders have not been ported. This build does not yet have complete original-app feature parity.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
