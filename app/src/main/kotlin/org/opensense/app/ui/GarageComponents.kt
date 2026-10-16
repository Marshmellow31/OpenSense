package org.opensense.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.opensense.core.garage.TrackPoint

internal fun dateLabel(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm"))
internal fun number(value: Double, decimals: Int = 1) = String.format(Locale.getDefault(), "%.${decimals}f", value)
internal fun money(minor: Long) = "₹${number(minor / 100.0, 2)}"

@Composable internal fun GarageIntro(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable internal fun FormField(label: String, value: String, change: (String) -> Unit, numeric: Boolean = false, lines: Int = 1) {
    OutlinedTextField(value = value, onValueChange = { change(it.take(if (lines > 1) 1000 else 120)) }, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = lines == 1, minLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text))
}
@Composable internal fun FormError(message: String?) { if (message != null) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
@Composable internal fun EmptyGarage(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.BookmarkBorder, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun DateTimeField(value: Long, change: (Long) -> Unit) {
    var dateOpen by remember { mutableStateOf(false) }; var timeOpen by remember { mutableStateOf(false) }
    Text(dateLabel(value), style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { dateOpen = true }) { Icon(Icons.Outlined.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("Choose date") }
        OutlinedButton(onClick = { timeOpen = true }) { Icon(Icons.Outlined.Schedule, null); Spacer(Modifier.width(8.dp)); Text("Time") }
    }
    if (dateOpen) {
        val local = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault())
        val picker = rememberDatePickerState(local.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { dateOpen = false }, confirmButton = { TextButton(onClick = {
            picker.selectedDateMillis?.let { chosen ->
                val date = Instant.ofEpochMilli(chosen).atZone(ZoneOffset.UTC).toLocalDate()
                change(date.atTime(local.toLocalTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            }; dateOpen = false
        }) { Text("Set date") } }, dismissButton = { TextButton(onClick = { dateOpen = false }) { Text("Cancel") } }) { DatePicker(picker) }
    }
    if (timeOpen) {
        val local = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault())
        val picker = rememberTimePickerState(local.hour, local.minute, true)
        AlertDialog(onDismissRequest = { timeOpen = false }, title = { Text("Reminder time") }, text = { TimePicker(picker) },
            confirmButton = { TextButton(onClick = { change(local.toLocalDate().atTime(picker.hour, picker.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()); timeOpen = false }) { Text("Set time") } },
            dismissButton = { TextButton(onClick = { timeOpen = false }) { Text("Cancel") } })
    }
}
@Composable internal fun DeleteAction(label: String, delete: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    TextButton(onClick = { confirming = true }) { Icon(Icons.Outlined.DeleteOutline, null); Spacer(Modifier.width(8.dp)); Text(label) }
    if (confirming) AlertDialog(onDismissRequest = { confirming = false }, title = { Text(label) }, text = { Text("This removes the saved record from this phone.") },
        confirmButton = { TextButton(onClick = { confirming = false; delete() }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirming = false }) { Text("Keep") } })
}
@Composable internal fun RouteTrace(points: List<TrackPoint>) {
    val color = MaterialTheme.colorScheme.primary
    val neutral = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(Modifier.fillMaxWidth().height(180.dp).semantics { contentDescription = "Recorded GPS path preview, not a street map" }) {
        if (points.size < 2) return@Canvas
        val minLat = points.minOf { it.latitude }; val maxLat = points.maxOf { it.latitude }
        val minLon = points.minOf { it.longitude }; val maxLon = points.maxOf { it.longitude }
        val centreLat = (minLat + maxLat) / 2
        val cos = kotlin.math.cos(Math.toRadians(centreLat))
        val width = (maxLon - minLon) * cos; val height = maxLat - minLat
        val scale = minOf(size.width * .8 / maxOf(width, .00001), size.height * .8 / maxOf(height, .00001))
        fun pos(p: TrackPoint) = Offset((size.width / 2 + (p.longitude - (minLon + maxLon) / 2) * cos * scale).toFloat(),
            (size.height / 2 - (p.latitude - centreLat) * scale).toFloat())
        val path = Path()
        points.forEachIndexed { index, point -> val at = pos(point); if (index == 0 || point.elapsedMillis - points[index - 1].elapsedMillis > 30000) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y) }
        drawPath(path, neutral, style = Stroke(10.dp.toPx()))
        drawPath(path, color, style = Stroke(3.dp.toPx()))
        drawCircle(color, 6.dp.toPx(), pos(points.first())); drawCircle(color, 6.dp.toPx(), pos(points.last()))
    }
}
