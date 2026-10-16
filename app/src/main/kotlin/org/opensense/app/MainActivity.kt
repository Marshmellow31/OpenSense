package org.opensense.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.opensense.core.model.Feature
import org.opensense.core.model.Support
import org.opensense.core.model.VehicleSnapshot
import org.opensense.core.model.InfotainmentFamily
import org.opensense.core.model.VehicleProfile
import org.opensense.core.model.capabilities
import org.opensense.core.transport.ConnectionState

class MainActivity : ComponentActivity() {
    private val model: OpenSenseViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OpenSenseApp(model) }
    }
    override fun onStop() {
        super.onStop()
        // Stop radio work and synthetic updates when the app leaves the foreground.
        if (!isChangingConfigurations) model.pause()
    }
}

private val Night = darkColorScheme(primary = Color(0xFFB4E8C6), secondary = Color(0xFFA6C6EE),
    background = Color(0xFF101613), surface = Color(0xFF17201B), surfaceVariant = Color(0xFF25332B))
private val Day = lightColorScheme(primary = Color(0xFF24623E), background = Color(0xFFF5F7F2), surface = Color(0xFFFAFCF7))

@Composable
fun OpenSenseApp(model: OpenSenseViewModel) {
    val dark by model.dark.collectAsStateWithLifecycle()
    val activity = androidx.activity.compose.LocalActivity.current as? ComponentActivity
    SideEffect {
        val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
    val profile by model.profile.collectAsStateWithLifecycle()
    val sample by model.repository.sample.collectAsStateWithLifecycle()
    val connection by model.repository.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("Vehicle") }
    var selectedFeature by rememberSaveable { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()
    LaunchedEffect(page, profile?.label, selectedFeature) { scroll.scrollTo(0) }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.all { it } && model.bluetooth.hasPermissions()) model.discover()
        else model.bluetooth.permissionDenied()
    }
    val tabs = listOf("Vehicle", "Connect", "Features", "Diagnostics", "Settings")
    MaterialTheme(colorScheme = if (dark) Night else Day) {
        Scaffold(
            bottomBar = {
                if (profile != null) NavigationBar {
                    tabs.forEachIndexed { index, title ->
                        NavigationBarItem(selected = page == title, onClick = { page = title; selectedFeature = null },
                            icon = { Text(listOf("◈", "⇄", "▦", "≡", "⚙")[index]) },
                            label = { Text(if (title == "Diagnostics") "Logs" else title, maxLines = 1, style = MaterialTheme.typography.labelSmall) })
                    }
                }
            }
        ) { padding ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                val wide = maxWidth >= 600.dp
                Column(Modifier.fillMaxSize().verticalScroll(scroll)
                    .padding(horizontal = if (wide) 40.dp else 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("OPENSENSE", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                            Text(selectedFeature?.let { Feature.valueOf(it).title } ?: page,
                                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                        }
                        Text("0.1 / DEV", style = MaterialTheme.typography.labelSmall)
                    }
                    if (profile == null) {
                        ProfileSelector(model::selectProfile)
                        return@Column
                    }
                    Text(profile!!.label, style = MaterialTheme.typography.labelLarge)
                    Notice(if (connection != ConnectionState.DISCONNECTED) "SIMULATION · ${connection.name.lowercase()}"
                        else "RESEARCH PREVIEW · No vehicle data", if (sample != null)
                        "All numbers below are generated examples. Your vehicle is not connected."
                        else "Your selected profile is a research candidate. Vehicle compatibility has not been verified.")
                    when (page) {
                        "Vehicle" -> Dashboard(sample, wide, model::startDemo, { model.repository.disconnect() },
                            { page = "Connect" }, { page = "Features"; selectedFeature = it.name })
                        "Connect" -> ConnectionScreen(model) {
                            if (model.bluetooth.hasPermissions()) model.discover()
                            else permissions.launch(model.bluetooth.requiredPermissions())
                        }
                        "Features" -> FeatureScreen(sample, selectedFeature, { selectedFeature = it?.name })
                        "Diagnostics" -> DiagnosticsScreen(model)
                        "Settings" -> {
                            Panel {
                                Text("Your vehicle", style = MaterialTheme.typography.titleLarge)
                                Text(profile!!.label)
                                OutlinedButton(onClick = { model.selectProfile(null); page = "Vehicle" }) { Text("Change vehicle profile") }
                            }
                            Panel {
                                Text("Appearance", style = MaterialTheme.typography.titleLarge)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Dark theme", Modifier.weight(1f))
                                    Switch(checked = dark, onCheckedChange = model::setDark)
                                }
                            }
                            Panel {
                                Text("Private by design", style = MaterialTheme.typography.titleLarge)
                                Text("No account, analytics, cloud sync, or Internet permission. Only your theme and selected vehicle profile are stored. Diagnostic events stay in memory.")
                                Text("Backgrounding the app ends discovery and simulation. Reconnection is always initiated by you.")
                            }
                            Panel {
                                Text("Community project", style = MaterialTheme.typography.titleLarge)
                                Text("OpenSense is unofficial and has no affiliation with Mahindra. Bluetooth discovery does not prove compatibility. Vehicle controls are unavailable.")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun Notice(title: String, body: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ProfileSelector(select: (VehicleProfile) -> Unit) {
    var familyName by rememberSaveable { mutableStateOf<String?>(null) }
    var variant by rememberSaveable { mutableStateOf<String?>(null) }
    Text("Choose your XUV500", style = MaterialTheme.typography.headlineMedium)
    Text("Select the infotainment app your car used, then its variant. Model year alone cannot identify the head unit, especially after a replacement.")
    InfotainmentFamily.entries.forEach { family ->
        Panel {
            Text(family.title, style = MaterialTheme.typography.titleLarge)
            Text(family.originalApp)
            Text(if (family == InfotainmentFamily.LEGACY) "Older head-unit generation. W8 is the historical research lead."
                else "New Age and facelift research profiles, including the 2018 W7 test vehicle.")
            OutlinedButton(onClick = { familyName = family.name; variant = null }) {
                Text(if (familyName == family.name) "Selected" else "Choose ${if (family == InfotainmentFamily.LEGACY) "Legacy" else "New BlueSense"}")
            }
        }
    }
    familyName?.let { name ->
        val family = InfotainmentFamily.valueOf(name)
        Panel {
            Text("Select variant", style = MaterialTheme.typography.titleLarge)
            family.variants.forEach { option ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = variant == option, onClick = { variant = option })
                    TextButton(onClick = { variant = option }) { Text(option) }
                }
            }
            Text("Both families are in research. Choosing a profile does not enable unverified vehicle controls.")
            Button(onClick = { variant?.let { select(VehicleProfile(family, it)) } }, enabled = variant != null) {
                Text("Continue with this vehicle")
            }
        }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun Dashboard(sample: VehicleSnapshot?, wide: Boolean, startDemo: () -> Unit, stopDemo: () -> Unit,
    connect: () -> Unit, feature: (Feature) -> Unit) {
    Panel {
        Text("A clearer view of your drive.", style = MaterialTheme.typography.headlineSmall)
        Text("INDEPENDENT COMMUNITY PROJECT", style = MaterialTheme.typography.labelLarge)
        if (sample == null) {
            Text("Explore the interface with a simulated vehicle, or discover your head unit to gather compatibility evidence.")
            Button(onClick = startDemo) { Text("Start simulated vehicle") }
            OutlinedButton(onClick = connect) { Text("Explore Bluetooth discovery") }
        } else {
            Text("SIMULATED SNAPSHOT #${sample.sequence}", color = MaterialTheme.colorScheme.primary)
            OutlinedButton(onClick = stopDemo) { Text("Stop simulation") }
        }
    }
    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f)) { Metric("Fuel", sample?.fuelPercent?.let { "$it %" } ?: "—", "Simulated fuel level") }
        Box(Modifier.weight(1f)) { Metric("Range", sample?.rangeKm?.let { "$it km" } ?: "—", "Simulated distance to empty") }
    } else {
        Metric("Fuel", sample?.fuelPercent?.let { "$it %" } ?: "—", "Simulated fuel level")
        Metric("Range", sample?.rangeKm?.let { "$it km" } ?: "—", "Simulated distance to empty")
    }
    Panel {
        Text("Cabin & comfort", style = MaterialTheme.typography.titleLarge)
        Text(sample?.let { "Simulated cabin ${it.cabinCelsius} °C · Outside ${it.outsideCelsius} °C" }
            ?: "Climate data unavailable until protocol verification.")
        TextButton(onClick = { feature(Feature.CLIMATE) }) { Text("View climate capability") }
    }
    Panel {
        Text("Media", style = MaterialTheme.typography.titleLarge)
        Text(sample?.let { if (it.mediaPlaying) "Simulated playback · Playing" else "Simulated playback · Paused" }
            ?: "No verified media service.")
        TextButton(onClick = { feature(Feature.MEDIA) }) { Text("View media capability") }
    }
}

@Composable
private fun Metric(title: String, value: String, description: String) {
    Panel {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Light)
        Text(if (value == "—") "Unavailable · vehicle not connected" else description, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ConnectionScreen(model: OpenSenseViewModel, discover: () -> Unit) {
    val state by model.bluetooth.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    Panel {
        Text("Find your head unit", style = MaterialTheme.typography.titleLarge)
        Text("Park safely before testing. Pair the vehicle in Android Bluetooth settings first. This screen performs Classic discovery and optional standard service discovery only; the vehicle may use a different transport.")
        Text(state.message)
        Button(onClick = discover, enabled = !state.scanning) { Text(if (state.scanning) "Discovering…" else "Discover Bluetooth devices") }
        if (state.scanning) OutlinedButton(onClick = { model.bluetooth.stop() }) { Text("Stop discovery") }
        TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }) { Text("Open Android Bluetooth settings") }
        TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:${context.packageName}")))
        }) { Text("Open app permission settings") }
    }
    if (state.devices.isEmpty()) Text("No devices listed. Grant permission, enable Bluetooth, then start discovery.")
    state.devices.forEach { device ->
        Panel {
            Text(device.label, style = MaterialTheme.typography.titleMedium)
            Text(if (device.paired) "Paired in Android · Identity hidden" else "Nearby · Identity hidden")
            Text("Identify your head unit in Android settings. OpenSense deliberately hides names and addresses.")
            OutlinedButton(onClick = { model.bluetooth.inspectServices(device.id) }) { Text("Inspect Bluetooth services") }
            device.services.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
    Notice("VEHICLE DATA CONNECTION BLOCKED", "Static APK protocol findings still need validation on your vehicle. OpenSense does not open a vehicle data socket or send control commands.")
}

@Composable
private fun FeatureScreen(sample: VehicleSnapshot?, selected: String?, select: (Feature?) -> Unit) {
    if (selected == null) capabilities(sample != null).forEach { capability ->
        Panel {
            Text(capability.feature.title, style = MaterialTheme.typography.titleLarge)
            Text(if (capability.support == Support.SIMULATED) "Simulated display only" else "Unavailable · needs confirmation")
            Text(capability.feature.requirement)
            TextButton(onClick = { select(capability.feature) }) { Text("View details") }
        }
    } else {
        val feature = Feature.valueOf(selected)
        TextButton(onClick = { select(null) }) { Text("Back to all features") }
        Panel {
            Text(feature.requirement)
            val value = when (feature) {
                Feature.FUEL -> sample?.let { "Simulated fuel ${it.fuelPercent}% · Range ${it.rangeKm} km" }
                Feature.CLIMATE -> sample?.let { "Simulated cabin ${it.cabinCelsius} °C · Outside ${it.outsideCelsius} °C" }
                Feature.DOORS -> sample?.let { if (it.doorsClosed) "Simulated doors closed" else "Simulated door open" }
                Feature.MEDIA -> sample?.let { if (it.mediaPlaying) "Simulated playback: playing" else "Simulated playback: paused" }
                else -> null
            }
            Text(value ?: "No data available. Vehicle hardware, firmware, and protocol support need confirmation.")
            Text("Vehicle controls are disabled. No real vehicle telemetry has been decoded.")
        }
    }
}

@Composable
private fun DiagnosticsScreen(model: OpenSenseViewModel) {
    val events by model.log.events.collectAsStateWithLifecycle()
    Panel {
        Text("Session events", style = MaterialTheme.typography.titleLarge)
        Text("Up to 100 structured events. Payloads, Bluetooth identifiers, locations, and exception messages are never recorded.")
        OutlinedButton(onClick = { model.log.clear() }) { Text("Clear diagnostics") }
    }
    if (events.isEmpty()) Text("No diagnostic events in this session.")
    events.asReversed().forEach { event ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date(event.timeMillis)),
                style = MaterialTheme.typography.labelMedium)
            Text(event.code.name.lowercase().replace('_', ' '), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
