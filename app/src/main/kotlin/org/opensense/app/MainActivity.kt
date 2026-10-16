package org.opensense.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.opensense.app.ui.*
import org.opensense.bluetooth.VehicleLink

class MainActivity : ComponentActivity() {
    private val model: OpenSenseViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.routeRequest.value = intent.getStringExtra("route")
        enableEdgeToEdge()
        setContent { OpenSenseApp(model) }
    }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); model.routeRequest.value = intent.getStringExtra("route") }
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) model.pause()
    }
}

private data class Destination(val name: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("Vehicle", Icons.Outlined.DirectionsCar), Destination("Climate", Icons.Outlined.Air),
    Destination("Media", Icons.Outlined.MusicNote), Destination("Insights", Icons.Outlined.Insights),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSenseApp(model: OpenSenseViewModel) {
    val dark by model.dark.collectAsStateWithLifecycle()
    val sample by model.repository.sample.collectAsStateWithLifecycle()
    val controls by model.controls.collectAsStateWithLifecycle()
    val profile by model.profile.collectAsStateWithLifecycle()
    val live by model.vehicleConnection.state.collectAsStateWithLifecycle()
    val liveSession = live.link in setOf(VehicleLink.CONNECTING, VehicleLink.WAITING, VehicleLink.ACTIVE)
    val activity = LocalActivity.current as? ComponentActivity
    SideEffect {
        val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
    var tab by rememberSaveable { mutableStateOf("Vehicle") }
    var route by rememberSaveable { mutableStateOf<String?>(null) }
    val requestedRoute by model.routeRequest.collectAsStateWithLifecycle()
    LaunchedEffect(requestedRoute) { requestedRoute?.let { route = it; model.routeRequest.value = null } }
    val scroll = remember(tab, route) { ScrollState(0) }
    fun back() {
        route = when { route == "Diagnostics" -> "Connection"; route == "Vehicle profile" -> "Settings"; route != null && isGarageRoute(route!!) -> garageParent(route!!); else -> null }
    }
    BackHandler(enabled = route != null || tab != "Vehicle") { if (route != null) back() else tab = "Vehicle" }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (model.bluetooth.hasPermissions()) model.discover() else model.bluetooth.permissionDenied()
    }
    OpenSenseTheme(dark) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 600.dp
            val short = wide && maxHeight < 480.dp
            Scaffold(
                topBar = {
                    TopAppBar(title = {
                        if (short && route == null) Column {
                            Text(if (tab == "Vehicle") "My XUV500" else tab, Modifier.testTag("screen-title"), style = MaterialTheme.typography.titleLarge)
                            Text("${profile?.variant ?: "XUV500"} · ${if (liveSession) "Vehicle session" else if (sample != null) "Demo · no vehicle connected" else "Offline"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else Text(when {
                            route?.startsWith("Trip:") == true -> "Trip details"
                            route?.startsWith("Fuel:") == true -> "Edit fill-up"
                            route?.startsWith("Reminder:") == true -> "Edit reminder"
                            else -> route ?: "OpenSense"
                        }, style = MaterialTheme.typography.titleLarge)
                    },
                        navigationIcon = { if (route != null) IconButton(onClick = ::back) { Icon(Icons.Outlined.ArrowBack, "Back") } },
                        actions = {
                            if (route == null) {
                                IconButton(onClick = { route = "Connection" }) { Icon(Icons.Outlined.Bluetooth, "Connection details") }
                                IconButton(onClick = { route = "Settings" }) { Icon(Icons.Outlined.Settings, "Settings") }
                            }
                        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
                },
                bottomBar = {
                    if (!wide && route == null) NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        destinations.forEach { destination ->
                            NavigationBarItem(selected = tab == destination.name, onClick = { tab = destination.name }, modifier = Modifier.testTag("nav-${destination.name}"),
                                icon = { Icon(destination.icon, null) }, label = { Text(destination.name, maxLines = 1) })
                        }
                    }
                }
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if (wide && route == null) {
                        if (short) {
                            Column(Modifier.width(72.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                                destinations.forEach { destination ->
                                    FilledTonalIconButton(onClick = { tab = destination.name }, modifier = Modifier.size(48.dp).testTag("nav-${destination.name}"),
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = if (tab == destination.name) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)) {
                                        Icon(destination.icon, destination.name)
                                    }
                                }
                            }
                        } else NavigationRail(containerColor = MaterialTheme.colorScheme.background) {
                            destinations.forEach { destination ->
                                NavigationRailItem(selected = tab == destination.name, onClick = { tab = destination.name }, modifier = Modifier.testTag("nav-${destination.name}"),
                                    icon = { Icon(destination.icon, null) }, label = { Text(destination.name) })
                            }
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(scroll)
                        .padding(horizontal = if (wide) 36.dp else 24.dp).padding(bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(if (short) 16.dp else 24.dp)) {
                        if (route == null) {
                            if (liveSession) {
                                Text(if (tab == "Vehicle") "My XUV500" else tab, Modifier.testTag("screen-title"), style = MaterialTheme.typography.headlineMedium)
                                if (tab == "Insights") GarageInsights(model) { route = it }
                                else LiveScreen(tab, live, profile, model)
                            } else {
                            if (!short) ScreenHeading(tab, profile?.label ?: "XUV500", sample != null) { route = "Connection" }
                            when (tab) {
                                "Vehicle" -> VehicleScreen(sample, controls, wide, short, { tab = it }, { route = it })
                                "Climate" -> ClimateScreen(sample, controls, wide, short, model::changeDemo)
                                "Media" -> MediaScreen(sample, controls, short, model::changeDemo)
                                "Insights" -> GarageInsights(model) { route = it }
                            }
                            }
                        } else when (route) {
                            "Settings" -> SettingsScreen(model, { route = "Vehicle profile" }, { route = "Connection" })
                            "Vehicle profile" -> ProfileScreen(profile) { model.selectProfile(it); route = "Settings" }
                            "Connection" -> ConnectionScreen(model, {
                                if (model.bluetooth.hasPermissions()) model.discover() else permission.launch(model.bluetooth.requiredPermissions())
                            }, { route = "Diagnostics" })
                            "Diagnostics" -> DiagnosticsScreen(model)
                            else -> if (isGarageRoute(route!!)) GarageRoute(route!!, model) { route = it } else VehicleDetailScreen(route!!, sample)
                        }
                    }
                }
            }
        }
    }
}
