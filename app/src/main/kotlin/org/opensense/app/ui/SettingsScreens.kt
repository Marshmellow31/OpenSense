package org.opensense.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.opensense.app.OpenSenseViewModel
import org.opensense.core.model.InfotainmentFamily
import org.opensense.core.model.VehicleProfile
import org.opensense.core.transport.ConnectionState
import org.opensense.bluetooth.VehicleLink

@Composable fun SettingsScreen(model: OpenSenseViewModel, profile: () -> Unit, connection: () -> Unit) {
    val selected by model.profile.collectAsStateWithLifecycle()
    val state by model.repository.state.collectAsStateWithLifecycle()
    val dark by model.dark.collectAsStateWithLifecycle()
    val demo = state != ConnectionState.DISCONNECTED && state != ConnectionState.FAILED
    Column {
        Section("Your vehicle")
        DetailRow(Icons.Outlined.Garage, "Your garage", "Fuel diary, trips, reminders & ownership tools", onClick = { model.routeRequest.value = "Your garage" })
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(Icons.Outlined.DirectionsCar, selected?.label ?: "Choose your XUV500", "Vehicle model & infotainment generation", onClick = profile)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(Icons.Outlined.Bluetooth, "Bluetooth connection", "Head-unit connection, permissions & diagnostics", onClick = connection)
    }
    TonalPanel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Science, null, tint = MaterialTheme.colorScheme.tertiary)
            Column(Modifier.weight(1f)) {
                Text("Simulation", style = MaterialTheme.typography.titleMedium)
                Text(if (demo) "Running on this phone" else "Explore without a vehicle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = demo, onCheckedChange = { if (it) model.startDemo() else model.stopDemo() }, modifier = Modifier.semantics { contentDescription = "Simulation mode" })
        }
        Text("Generated readings and interactive climate/media previews. No real vehicle is connected and no commands are sent.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Simulation stops when you leave the app or change vehicle profile.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Column {
        Section("Appearance")
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("Dark theme", style = MaterialTheme.typography.bodyLarge)
                Text("On by default · saved on this phone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = dark, onCheckedChange = model::setDark, modifier = Modifier.semantics { contentDescription = "Dark theme" })
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Section("About OpenSense")
        Text("An independent community companion for XUV500 owners. Version 0.1.0-dev. Not affiliated with Mahindra.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("No account, analytics or automatic cloud sync. Garage records stay in private app storage; GPS is used only when you request parking location or start a trip. Diagnostics stay in memory.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Vehicle image: Akashpbrahmavar / Wikimedia Commons (CC0), edited with AI for background extraction, registration redaction and carrier removal. Illustrative 2018 W11, not an exact image of each selected variant.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun ProfileScreen(current: VehicleProfile?, save: (VehicleProfile?) -> Unit) {
    var familyName by rememberSaveable { mutableStateOf((current?.family ?: InfotainmentFamily.NEW).name) }
    var variant by rememberSaveable { mutableStateOf(current?.variant) }
    val family = InfotainmentFamily.valueOf(familyName)
    Text("Choose your XUV500", style = MaterialTheme.typography.headlineSmall)
    Text("Use the app generation your head unit originally supported. A model selection is research context, not verified compatibility.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Section("Infotainment generation")
        InfotainmentFamily.entries.forEach { option ->
            Row(Modifier.fillMaxWidth().selectable(family == option, onClick = { familyName = option.name; variant = null }, role = Role.RadioButton).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioButton(selected = family == option, onClick = null)
                Column(Modifier.weight(1f)) {
                    Text(if (option == InfotainmentFamily.NEW) "New BlueSense" else "Legacy BlueSense", style = MaterialTheme.typography.titleMedium)
                    Text(option.originalApp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    Column {
        Section("Variant")
        family.variants.forEach { option ->
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).selectable(variant == option, onClick = { variant = option }, role = Role.RadioButton),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioButton(selected = variant == option, onClick = null)
                Text(option)
            }
        }
    }
    Button(onClick = { variant?.let { save(VehicleProfile(family, it)) } }, enabled = variant != null, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Save vehicle") }
    if (current != null) TextButton(onClick = { save(null) }) { Text("Clear vehicle selection") }
}

@Composable fun ConnectionScreen(model: OpenSenseViewModel, discover: () -> Unit, diagnostics: () -> Unit) {
    val state by model.bluetooth.state.collectAsStateWithLifecycle()
    val live by model.vehicleConnection.state.collectAsStateWithLifecycle()
    val paired by model.vehicleConnection.devices.collectAsStateWithLifecycle()
    val profile by model.profile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Text("Your vehicle connection", style = MaterialTheme.typography.headlineSmall)
    Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TonalPanel {
        Text("Connect to BlueSense", style = MaterialTheme.typography.titleLarge)
        Text(profile?.label ?: "Choose your vehicle generation in Settings first.")
        Text(live.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Experimental compatibility build. Use your own vehicle while parked. Connection sends the original app's startup and status requests. Media and supported climate actions send commands when tapped. Readings are not yet confirmed on hardware.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { if (model.vehicleConnection.canConnect()) model.vehicleConnection.refreshPaired() else discover() }) { Text("Refresh paired head units") }
        if (paired.isEmpty()) Text("Pair your head unit in Android Bluetooth settings, allow Nearby devices, then refresh.", style = MaterialTheme.typography.bodySmall)
        paired.forEach { device ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(device.name, Modifier.weight(1f))
                Button(onClick = { model.connectVehicle(device.id) }, enabled = profile != null && live.link in setOf(VehicleLink.DISCONNECTED, VehicleLink.FAILED)) { Text("Connect") }
            }
        }
        if (live.link !in setOf(VehicleLink.DISCONNECTED, VehicleLink.FAILED)) OutlinedButton(onClick = { model.vehicleConnection.disconnect() }) { Text("Disconnect vehicle") }
        Text("Messages received: ${live.received} · rejected: ${live.rejected}", style = MaterialTheme.typography.bodySmall)
    }
    TonalPanel {
        Icon(Icons.Outlined.BluetoothSearching, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
        Text("Find your head unit", style = MaterialTheme.typography.titleLarge)
        Text("While safely parked, pair and identify your vehicle in Android settings, then discover its Bluetooth services here.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = discover, enabled = !state.scanning, modifier = Modifier.fillMaxWidth()) { Text(if (state.scanning) "Discovering…" else "Discover Bluetooth devices") }
        if (state.scanning) OutlinedButton(onClick = { model.bluetooth.stop() }) { Text("Stop discovery") }
        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }) { Text("Android Bluetooth settings") }
        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }) { Text("App permission settings") }
    }
    if (state.devices.isNotEmpty()) {
        Section("Observed devices")
        state.devices.forEach { device ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(Icons.Outlined.Speaker, device.label, if (device.paired) "Paired · identity hidden" else "Nearby · identity hidden")
                OutlinedButton(onClick = { model.bluetooth.inspectServices(device.id) }) { Text("Inspect Bluetooth services") }
                device.services.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
    StateNote(false, "Discovery and service UUIDs do not prove compatibility. Select the original app generation and connect explicitly above. Live data clears when the app leaves the foreground.")
    DetailRow(Icons.Outlined.Terminal, "Session diagnostics", "View redacted event codes", onClick = diagnostics)
}

@Composable fun DiagnosticsScreen(model: OpenSenseViewModel) {
    val events by model.log.events.collectAsStateWithLifecycle()
    Text("Session events", style = MaterialTheme.typography.headlineSmall)
    Text("Up to 100 events. No device identifiers, packets, locations or exception messages are recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedButton(onClick = model.log::clear) { Text("Clear diagnostics") }
    if (events.isEmpty()) Text("No events in this session.")
    events.asReversed().forEach { event ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date(event.timeMillis)), style = MaterialTheme.typography.labelMedium)
            Text(event.code.name.lowercase().replace('_', ' '), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
