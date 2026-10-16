package org.opensense.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import org.opensense.app.DemoControls
import org.opensense.app.R
import org.opensense.core.model.VehicleSnapshot

@Composable fun CarStage(modifier: Modifier = Modifier, height: Dp = 240.dp) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth().height(height)) {
        Canvas(Modifier.fillMaxSize()) {
            val imageHeight = minOf(size.height - 15.dp.toPx(), size.width / (1491f / 1055f))
            val imageTop = (size.height - 15.dp.toPx() - imageHeight) / 2f
            val imageWidth = imageHeight * (1491f / 1055f)
            val platformHeight = imageHeight * .19f
            val top = imageTop + imageHeight * .94f - platformHeight + 4.dp.toPx()
            val left = (size.width - imageWidth * .95f) / 2f
            drawOval(colors.surfaceContainerHigh.copy(alpha = 0.3f), Offset(left + 6.dp.toPx(), top + 8.dp.toPx()), Size(imageWidth * .91f, platformHeight))
            drawOval(colors.surfaceContainerHigh, Offset(left, top), Size(imageWidth * .95f, platformHeight))
            drawArc(colors.outlineVariant, 0f, 180f, false, Offset(left, top), Size(imageWidth * .95f, platformHeight), style = Stroke(1.dp.toPx()))
        }
        Image(painterResource(R.drawable.xuv500_cutout), "Illustrative XUV500 exterior; not live vehicle status",
            Modifier.fillMaxWidth().height(height - 15.dp).align(Alignment.TopCenter), contentScale = ContentScale.Fit)
    }
}

@Composable private fun Reading(title: String, value: String, unit: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(value, style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Medium)
            Text(unit, Modifier.padding(bottom = 5.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun VehicleScreen(sample: VehicleSnapshot?, controls: DemoControls, wide: Boolean, short: Boolean, tab: (String) -> Unit, details: (String) -> Unit) {
    if (wide) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        CarStage(Modifier.weight(1.2f), if (short) 180.dp else 240.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (short) 10.dp else 20.dp)) {
            Reading("Estimated range", sample?.rangeKm?.toString() ?: "—", "km", compact = short)
            Reading("Fuel level", sample?.fuelPercent?.toString() ?: "—", "%", compact = short)
            StateNote(sample != null, "No vehicle data. Connection details are available above.")
        }
    } else {
        CarStage()
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Reading("Estimated range", sample?.rangeKm?.toString() ?: "—", "km", Modifier.weight(1f))
            Reading("Fuel level", sample?.fuelPercent?.toString() ?: "—", "%", Modifier.weight(1f))
        }
        StateNote(sample != null, "No vehicle data · waiting for a verified connection")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(onClick = { tab("Climate") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Air, null)
                Text("Cabin climate", style = MaterialTheme.typography.titleSmall)
                Text(sample?.let { "${controls.target}° target" } ?: "Not connected", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Surface(onClick = { tab("Media") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                Text("Your media", style = MaterialTheme.typography.titleSmall)
                Text(if (sample == null) "Not connected" else if (controls.playing) "Demo playing" else "Demo paused", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    Column {
        Section("Vehicle status")
        DetailRow(Icons.Outlined.Garage, "Your garage", "Fuel diary, trips, documents & reminders", onClick = { details("Your garage") })
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(Icons.Outlined.DoorFront, "Doors", if (sample != null) "Simulated · all doors closed" else "Status unknown", onClick = { details("Doors") })
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(Icons.Outlined.TireRepair, "Tyre pressure", "Hardware support unconfirmed", onClick = { details("Tyre pressure") })
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(Icons.Outlined.WarningAmber, "Warnings", "No verified warning data", onClick = { details("Warnings") })
    }
}

@Composable fun InsightsScreen(sample: VehicleSnapshot?) {
    TonalPanel {
        Icon(Icons.Outlined.LocalGasStation, null, tint = MaterialTheme.colorScheme.primary)
        Text("Fuel & range", style = MaterialTheme.typography.titleLarge)
        Text("A little more clarity for every journey.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Reading("Fuel", sample?.fuelPercent?.toString() ?: "—", "%", Modifier.weight(1f))
            Reading("Range", sample?.rangeKm?.toString() ?: "—", "km", Modifier.weight(1f))
        }
        StateNote(sample != null, "Fuel readings need a verified vehicle connection.")
    }
    Column {
        Section("Your journeys")
        DetailRow(Icons.Outlined.Route, "Trips & EcoSense", "Your verified trips will appear here.")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(Icons.Outlined.Park, "Driving efficiency", "No supported trip data yet.")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        DetailRow(Icons.Outlined.LocationOn, "Parked location", "Location recording is not available yet.")
    }
    Text("No trips or locations are being recorded. These features remain unavailable until their data sources are verified.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable fun VehicleDetailScreen(title: String, sample: VehicleSnapshot?) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    if (title == "Doors" || title == "Tyre pressure") {
        val color = MaterialTheme.colorScheme.primary
        val outline = MaterialTheme.colorScheme.outlineVariant
        Canvas(Modifier.fillMaxWidth().height(260.dp).semantics { contentDescription = "Schematic vehicle diagram; ${if (title == "Doors" && sample != null) "simulated doors closed" else "individual sensor readings unknown"}" }) {
            val x = size.width * .35f; val y = size.height * .05f; val w = size.width * .3f; val h = size.height * .9f
            drawRoundRect(outline, Offset(x, y), Size(w, h), CornerRadius(w * .25f), style = Stroke(2.dp.toPx()))
            drawRoundRect(outline, Offset(x + w * .12f, y + h * .2f), Size(w * .76f, h * .18f), CornerRadius(6.dp.toPx()), style = Stroke(2.dp.toPx()))
            listOf(.28f, .7f).forEach { f ->
                listOf(x - 10.dp.toPx(), x + w).forEach { wheelX ->
                    drawRoundRect(if (title == "Doors" && sample != null) color else outline, Offset(wheelX, y + h * f), Size(10.dp.toPx(), 36.dp.toPx()), CornerRadius(4.dp.toPx()))
                }
            }
        }
    }
    StateNote(title == "Doors" && sample != null, "${if (title == "Tyre pressure") "TPMS hardware and pressure units need confirmation." else "No real vehicle status is available."}")
    Text(if (title == "Doors" && sample != null) "Simulated doors are closed. The exterior photograph does not change with door state."
        else "No verified ${title.lowercase()} readings. Unknown does not mean normal or safe.", style = MaterialTheme.typography.bodyLarge)
}
