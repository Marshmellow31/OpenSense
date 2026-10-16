package org.opensense.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.*
import org.opensense.app.OpenSenseViewModel
import org.opensense.app.garage.*
import org.opensense.core.garage.*
import java.math.BigDecimal
import java.util.UUID

val garageRoutes = setOf("Your garage", "Fuel diary", "Add fuel", "Reminders", "Add reminder", "Records", "Documents", "Owner's manual", "Parked car", "Trips", "Assistance", "Warning preferences", "Data & privacy")
fun garageParent(route: String): String? = when {
    route == "Your garage" -> null
    route == "Add fuel" || route.startsWith("Fuel:") -> "Fuel diary"
    route == "Add reminder" || route.startsWith("Reminder:") -> "Reminders"
    route.startsWith("Trip:") -> "Trips"
    route in garageRoutes -> "Your garage"
    else -> null
}
fun isGarageRoute(route: String) = route in garageRoutes || route.startsWith("Reminder:") || route.startsWith("Trip:") || route.startsWith("Fuel:")

@Composable fun GarageHub(model: OpenSenseViewModel, open: (String) -> Unit) {
    val data by model.garage.data.collectAsStateWithLifecycle()
    GarageIntro("Your garage", "The everyday side of ownership. Saved privately on this phone.")
    val next = data.reminders.filterNot { it.done }.minByOrNull { it.dueAt }
    if (next != null) TonalPanel {
        Icon(Icons.Outlined.EventAvailable, null, tint = MaterialTheme.colorScheme.primary)
        Text(next.title, style = MaterialTheme.typography.titleLarge)
        Text(dateLabel(next.dueAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { open("Reminders") }) { Text("View reminders") }
    }
    Column {
        DetailRow(Icons.Outlined.LocalGasStation, "Fuel diary", "${data.fuel.size} fill-ups · full-to-full efficiency", onClick = { open("Fuel diary") })
        HorizontalDivider()
        DetailRow(Icons.Outlined.Route, "Trips", "Phone GPS journeys & driving insights", onClick = { open("Trips") })
        HorizontalDivider()
        DetailRow(Icons.Outlined.NotificationsActive, "Reminders", "Service, insurance and important dates", onClick = { open("Reminders") })
        HorizontalDivider()
        DetailRow(Icons.Outlined.NearMe, "Parked car", data.parked?.let { "Saved ${dateLabel(it.time)}" } ?: "Save a place. Find your way back.", onClick = { open("Parked car") })
    }
    Column {
        Section("Keep it together")
        DetailRow(Icons.Outlined.Badge, "Personal & vehicle records", data.owner.registration.ifBlank { "Owner, vehicle and policy details" }, onClick = { open("Records") })
        HorizontalDivider()
        DetailRow(Icons.Outlined.FolderOpen, "Document wallet", "${data.documents.count { !it.manual }} saved document links", onClick = { open("Documents") })
        HorizontalDivider()
        DetailRow(Icons.Outlined.MenuBook, "Owner's manual", "Your own PDF, one tap away", onClick = { open("Owner's manual") })
        HorizontalDivider()
        DetailRow(Icons.Outlined.SupportAgent, "Assistance", "Your contact, dialer and location message", onClick = { open("Assistance") })
    }
    DetailRow(Icons.Outlined.Tune, "Warning preferences", "Thresholds and display preferences", onClick = { open("Warning preferences") })
    DetailRow(Icons.Outlined.PrivacyTip, "Data & privacy", "Export non-identity records or remove saved data", onClick = { open("Data & privacy") })
}

@Composable fun GarageInsights(model: OpenSenseViewModel, open: (String) -> Unit) {
    val data by model.garage.data.collectAsStateWithLifecycle()
    val live by model.vehicleConnection.state.collectAsStateWithLifecycle()
    val eco = live.vehicle.ecoSense
    val intervals = FuelMath.intervals(data.fuel)
    TonalPanel {
        Text("Every fill tells a story", style = MaterialTheme.typography.titleLarge)
        Text(if (intervals.isEmpty()) "Your fuel diary turns full-tank entries into measured efficiency." else "${number(intervals.sumOf { it.distance } / intervals.sumOf { it.litres })} km/L across ${intervals.size} full-to-full intervals.", style = MaterialTheme.typography.bodyMedium)
        if (intervals.isNotEmpty()) EfficiencyChart(intervals.map { it.kmPerLitre })
        Button(onClick = { open("Add fuel") }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Log a fill-up") }
        TextButton(onClick = { open("Fuel diary") }) { Text("View fuel diary") }
    }
    TonalPanel {
        Text("Head-unit EcoSense", style = MaterialTheme.typography.titleLarge)
        if (eco == null) Text("Connect a compatible New BlueSense head unit to receive its own trip score and driving data. Nothing here is generated from the GPS recorder.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else {
            Text(eco.score?.let { "$it / 100" } ?: "Score unavailable", style = MaterialTheme.typography.headlineMedium)
            eco.score?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth()) }
            Text("APK-derived current-trip data · hardware confirmation pending", style = MaterialTheme.typography.labelSmall)
            Text("Distance ${eco.distanceKm?.let { number(it) } ?: "—"} km · duration ${eco.duration ?: "—"}")
            Text("Fuel economy ${eco.economy?.let { number(it) } ?: "—"} · average speed ${eco.averageSpeed ?: "—"} (head-unit units)")
            Text("Acceleration ${eco.accelerationEvents ?: "—"} · braking ${eco.brakingEvents ?: "—"} · overspeed ${eco.overspeedEvents ?: "—"}")
        }
    }
    Column {
        Section("Your journeys")
        DetailRow(Icons.Outlined.Route, "Trips", "${data.trips.size} saved · ${number(data.trips.sumOf { it.distanceMetres } / 1000)} km from phone GPS", onClick = { open("Trips") })
        HorizontalDivider()
        DetailRow(Icons.Outlined.LocationOn, "Parked car", data.parked?.note?.ifBlank { "Saved on this phone" } ?: "Remember your parking place", onClick = { open("Parked car") })
        HorizontalDivider()
        DetailRow(Icons.Outlined.Garage, "Your garage", "Reminders, documents and ownership tools", onClick = { open("Your garage") })
    }
}
@Composable private fun EfficiencyChart(values: List<Double>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(100.dp)) {
        val points = values.takeLast(12); val max = maxOf(1.0, points.maxOrNull() ?: 1.0)
        val slot = size.width / points.size
        points.forEachIndexed { index, value ->
            val h = (size.height * .85 * value / max).toFloat()
            drawRoundRect(color, Offset(index * slot + slot * .15f, size.height - h), androidx.compose.ui.geometry.Size(slot * .7f, h), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
        }
    }
    Text("Most recent full-to-full intervals · km/L", style = MaterialTheme.typography.labelSmall)
}

@Composable fun GarageRoute(route: String, model: OpenSenseViewModel, open: (String) -> Unit) {
    val data by model.garage.data.collectAsStateWithLifecycle()
    val error by model.garage.problem.collectAsStateWithLifecycle()
    FormError(error)
    when {
        route == "Your garage" -> GarageHub(model, open)
        route == "Fuel diary" -> FuelDiary(data, model, open)
        route == "Add fuel" || route.startsWith("Fuel:") -> FuelForm(data, model, route.substringAfter(':', "")) { open("Fuel diary") }
        route == "Reminders" -> ReminderList(data, model, open)
        route == "Add reminder" || route.startsWith("Reminder:") -> ReminderForm(data, model, route.substringAfter(':', "").toIntOrNull()) { open("Reminders") }
        route == "Records" -> OwnerForm(data.owner, model) { open("Your garage") }
        route == "Documents" || route == "Owner's manual" -> DocumentWallet(data, model, route == "Owner's manual")
        route == "Parked car" -> ParkingScreen(data, model)
        route == "Trips" || route.startsWith("Trip:") -> TripsScreen(data, model, route.substringAfter(':', ""), open)
        route == "Assistance" -> AssistanceScreen(data, open)
        route == "Warning preferences" -> WarningForm(data.warnings, model)
        route == "Data & privacy" -> DataPrivacyScreen(data, model)
    }
}
