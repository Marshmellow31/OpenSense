package org.opensense.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.*
import org.opensense.app.OpenSenseViewModel
import org.opensense.app.garage.*
import org.opensense.core.garage.*
import java.util.UUID

@Composable internal fun DocumentWallet(data: GarageData, model: OpenSenseViewModel, manual: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val record = withContext(Dispatchers.IO) { runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: "Saved document"
                GarageDocument(UUID.randomUUID().toString(), name.take(120), uri.toString(), context.contentResolver.getType(uri) ?: "application/pdf", manual)
            }.getOrNull() }
            if (record == null) error = "This file provider could not grant lasting access. Choose a local PDF or image and try again."
            else model.saveGarage({ it.copy(documents = it.documents.filterNot { d -> d.uri == record.uri && d.manual == record.manual } + record) })
        }
    }
    GarageIntro(if (manual) "Owner's manual" else "Document wallet", if (manual) "Keep your own manual close. Add a PDF you are entitled to use." else "Registration, insurance and the documents you need. Files remain with your chosen provider.")
    Button(onClick = { picker.launch(if (manual) arrayOf("application/pdf") else arrayOf("application/pdf", "image/*")) }, enabled = data.documents.size < 100) { Icon(Icons.Outlined.NoteAdd, null); Spacer(Modifier.width(8.dp)); Text(if (manual) "Add manual PDF" else "Add a document") }
    FormError(error)
    val documents = data.documents.filter { it.manual == manual }
    if (documents.isEmpty()) EmptyGarage(if (manual) "Your manual, at hand" else "A place for your paperwork", "Nothing is bundled or uploaded. Add a file from your phone to get started.")
    documents.forEach { document ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (document.mime.startsWith("image")) Icons.Outlined.Image else Icons.Outlined.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
            Text(document.name, style = MaterialTheme.typography.titleMedium)
            Text(if (document.mime.startsWith("image")) "Image · linked privately" else "PDF · linked privately", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = {
                try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(document.uri), document.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                catch (_: Exception) { error = "This document is unavailable or there is no compatible viewer. Re-add the file or install a PDF/image viewer." }
            }) { Text("Open document") }
            DeleteAction("Remove document link") { model.saveGarage({ it.copy(documents = it.documents.filterNot { d -> d.id == document.id }) }) {
                if (data.documents.none { it.id != document.id && it.uri == document.uri }) runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(document.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            } }
            HorizontalDivider()
        }
    }
    Text("Removing a link does not delete the original file. A cloud file provider may need a connection to open its file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable internal fun ParkingScreen(data: GarageData, model: OpenSenseViewModel) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var note by rememberSaveable(data.parked?.time) { mutableStateOf(data.parked?.note ?: "") }
    var busy by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }
    var distance by remember { mutableStateOf<Double?>(null) }
    fun locate(save: Boolean) {
        if (busy) return
        scope.launch {
            busy = true; message = "Getting this phone's location…"
            val fix = PhoneLocation.current(context)
            if (fix == null) message = "No fresh location fix. Turn on system Location, check permission and retry outdoors."
            else if (save) {
                val place = ParkedPlace(fix.latitude, fix.longitude, fix.accuracy, System.currentTimeMillis(), note.trim())
                model.saveGarage({ it.copy(parked = place) }) { message = "Parking place saved on this phone" }
            } else data.parked?.let { parked -> distance = TripAccumulator.metres(fix.latitude, fix.longitude, parked.latitude, parked.longitude); message = "Straight-line estimate · accuracy ±${fix.accuracy.toInt()} m" }
            busy = false
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) locate(true) else message = "Location access was denied. Your saved place is unchanged."
    }
    GarageIntro("Find your way back", "A parking note and a place saved by you. No background parking tracking.")
    Icon(Icons.Outlined.NearMe, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
    data.parked?.let { parked ->
        Text("Saved ${dateLabel(parked.time)}", style = MaterialTheme.typography.titleLarge)
        Text("Accuracy ±${parked.accuracy.toInt()} m", color = MaterialTheme.colorScheme.onSurfaceVariant)
        distance?.let { Text(if (it >= 1000) "${number(it / 1000)} km away" else "${number(it, 0)} m away", style = MaterialTheme.typography.headlineMedium) }
        OutlinedButton(onClick = {
            val uri = Uri.parse("geo:${parked.latitude},${parked.longitude}?q=${parked.latitude},${parked.longitude}(Parked+car)")
            try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { message = "Install a maps app to open directions." }
        }) { Icon(Icons.Outlined.DirectionsWalk, null); Spacer(Modifier.width(8.dp)); Text("Open in maps") }
        OutlinedButton(onClick = { if (TripService.permitted(context)) locate(false) else message = "Allow Location in app settings, then check distance again." }, enabled = !busy) { Text("Check distance from here") }
    } ?: EmptyGarage("No parking place yet", "Save your location after parking. Add a floor, landmark or bay number to make it easier to find.")
    FormField("Parking note", note, { note = it }, lines = 2)
    Button(onClick = { if (TripService.permitted(context)) locate(true) else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, enabled = !busy) { Text(if (busy) "Finding location…" else if (data.parked == null) "Save this parking place" else "Replace with my location") }
    if (data.parked != null) OutlinedButton(onClick = { model.saveGarage({ d -> d.copy(parked = d.parked?.copy(note = note.trim())) }) { message = "Parking note updated" } }) { Text("Update note only") }
    message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    if (data.parked != null) DeleteAction("Forget parking place") { model.saveGarage({ it.copy(parked = null) }) { note = ""; distance = null } }
}

@Composable internal fun AssistanceScreen(data: GarageData, open: (String) -> Unit) {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    GarageIntro("Help, within reach", "Your assistance contact and a location message you can review before sending.")
    Icon(Icons.Outlined.SupportAgent, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
    Text(data.owner.assistanceName.ifBlank { "Add someone you can contact" }, style = MaterialTheme.typography.titleLarge)
    Text(data.owner.assistancePhone.ifBlank { "No assistance number saved" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Button(onClick = {
        try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", data.owner.assistancePhone, null))) } catch (_: Exception) { message = "No phone dialer is available on this device." }
    }, enabled = data.owner.assistancePhone.matches(Regex("[+0-9 ()-]{3,25}"))) { Icon(Icons.Outlined.Call, null); Spacer(Modifier.width(8.dp)); Text("Open contact in dialer") }
    OutlinedButton(onClick = { open("Records") }) { Text("Edit assistance contact") }
    OutlinedButton(onClick = {
        try { context.startActivity(Intent(Intent.ACTION_DIAL)) } catch (_: Exception) { message = "No phone dialer is available on this device." }
    }) { Text("Open phone dialer") }
    data.parked?.let { parked ->
        Text("The message uses your saved parking place from ${dateLabel(parked.time)}. It is not a live location.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = {
            val text = "My saved parking place (${dateLabel(parked.time)}): https://maps.google.com/?q=${parked.latitude},${parked.longitude}" + if (parked.note.isBlank()) "" else "\n${parked.note}"
            try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Review location message")) }
            catch (_: Exception) { message = "No sharing app is available." }
        }) { Icon(Icons.Outlined.Share, null); Spacer(Modifier.width(8.dp)); Text("Prepare a location message") }
    }
    Text("OpenSense does not automatically call, send SMS or detect a crash. Use the vehicle's own emergency system or your phone when needed.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FormError(message)
}

@Composable internal fun DataPrivacyScreen(data: GarageData, model: OpenSenseViewModel) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            result = withContext(Dispatchers.IO) { runCatching { model.garage.snapshot(); context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(model.garage.export().toByteArray()) }; "Export saved" }.getOrElse { "Could not save the export. Choose another location and retry." } }
        }
    }
    GarageIntro("Your data stays yours", "Records remain in this app's private storage. No account, analytics or automatic cloud sync.")
    Text("${data.fuel.size} fill-ups · ${data.reminders.size} reminders · ${data.trips.size} trips · ${data.documents.size} document links", style = MaterialTheme.typography.titleMedium)
    Text("The standard export includes fuel entries, reminders, warning preferences and trip summaries. It excludes personal/vehicle identity fields, document links, parking coordinates and GPS traces. Notes and reminder titles are included; review before sharing.")
    Button(onClick = { export.launch("OpenSense-records.json") }) { Icon(Icons.Outlined.FileDownload, null); Spacer(Modifier.width(8.dp)); Text("Export non-identity records") }
    result?.let { Text(it) }
    val recording by TripService.state.collectAsStateWithLifecycle()
    if (recording.active) Text("Stop the active trip before deleting garage data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    else DeleteAction("Delete saved garage data") { model.saveGarage({ GarageData() }) {
        data.reminders.forEach { ReminderScheduler.cancel(context, it.id) }
        data.documents.map { it.uri }.distinct().forEach { runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(it), Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
        result = "Saved garage data removed. Theme and selected vehicle remain."
    } }
    Text("Deleting garage data does not delete the original files in your document provider. Uninstalling OpenSense removes its private records.", style = MaterialTheme.typography.bodySmall)
}
