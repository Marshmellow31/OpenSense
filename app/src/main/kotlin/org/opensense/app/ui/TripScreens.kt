package org.opensense.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.opensense.app.OpenSenseViewModel
import org.opensense.app.garage.TripService
import org.opensense.core.garage.*

@Composable internal fun TripsScreen(data: GarageData, model: OpenSenseViewModel, id: String, open: (String) -> Unit) {
    val context = LocalContext.current
    val recording by TripService.state.collectAsStateWithLifecycle()
    var error by remember { mutableStateOf<String?>(null) }
    fun start() { try { TripService.start(context) } catch (_: Exception) { error = "Could not start recording. Keep the app visible, allow Location and try again." } }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (TripService.permitted(context)) start() else error = "Location access is needed to record your route. Other garage tools still work."
    }
    val trip = data.trips.firstOrNull { it.id == id }
    if (trip != null) {
        GarageIntro("A journey remembered", dateLabel(trip.startedAt))
        RouteTrace(trip.points)
        Text("${number(trip.distanceMetres / 1000)} km", style = MaterialTheme.typography.headlineLarge)
        Text("${((trip.endedAt - trip.startedAt) / 60000).coerceAtLeast(0)} min elapsed · ${trip.movingMillis / 60000} min moving", color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text("Phone GPS insights", style = MaterialTheme.typography.titleLarge)
        Text("Maximum estimated speed: ${number(trip.maxKmh, 0)} km/h")
        Text("${trip.harshBrakes} sharp slowing estimates")
        Text("Your speed reference: ${data.warnings.speedLimitKmh} km/h")
        if (trip.interrupted) Text("Recording was interrupted. This is the most recent saved checkpoint.", color = MaterialTheme.colorScheme.error)
        Text("Route preview is not a street map. GPS-derived distance, speed and slowing events are estimates; they are separate from the infotainment system's EcoSense score.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DeleteAction("Delete trip") { model.saveGarage({ it.copy(trips = it.trips.filterNot { t -> t.id == trip.id }) }) { open("Trips") } }
        return
    }
    GarageIntro("Your journeys", "Record from your phone. Keep the moments and the distance, privately.")
    TonalPanel {
        Icon(if (recording.active) Icons.Outlined.MyLocation else Icons.Outlined.Route, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Text(if (recording.active) "A trip is being recorded" else "Ready when you are", style = MaterialTheme.typography.titleLarge)
        Text(recording.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (recording.active) {
            Text("${number(recording.distance / 1000)} km", style = MaterialTheme.typography.headlineMedium)
            if (recording.points.size > 1) RouteTrace(recording.points)
            Button(onClick = { TripService.stop(context) }) { Text("Stop & save trip") }
        } else Button(onClick = {
            if (TripService.permitted(context)) start() else permissions.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) + if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray())
        }) { Icon(Icons.Outlined.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Start a trip") }
        Text("A user-started location service continues with a recording indicator when you leave the app. Stop it here or from its notification. Trip recording is separate from Bluetooth and simulation.", style = MaterialTheme.typography.bodySmall)
    }
    FormError(error)
    if (data.trips.isEmpty()) EmptyGarage("Your first route starts here", "Start recording before setting off, then put the phone away. Your trip appears here after you stop.")
    else {
        Section("Saved trips")
        data.trips.sortedByDescending { it.startedAt }.forEach { t ->
            DetailRow(Icons.Outlined.Route, "${number(t.distanceMetres / 1000)} km · ${((t.endedAt - t.startedAt) / 60000).coerceAtLeast(0)} min", "${dateLabel(t.startedAt)}${if (t.interrupted) " · interrupted" else ""}", onClick = { open("Trip:${t.id}") })
            HorizontalDivider()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Head-unit EcoSense", style = MaterialTheme.typography.titleMedium)
        Text("Original EcoSense scores and transmission-specific trip records need the compatible head unit's data. Phone GPS insights are available now; an original EcoSense score is never invented.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
