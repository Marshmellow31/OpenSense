package org.opensense.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.opensense.app.OpenSenseViewModel
import org.opensense.app.garage.ReminderScheduler
import org.opensense.core.garage.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Composable internal fun FuelDiary(data: GarageData, model: OpenSenseViewModel, open: (String) -> Unit) {
    GarageIntro("Fuel diary", "Your fill-ups, costs and measured full-to-full efficiency.")
    val intervals = FuelMath.intervals(data.fuel)
    if (data.fuel.isNotEmpty()) TonalPanel {
        Text("${money(data.fuel.sumOf { it.costMinor })} across ${data.fuel.size} fill-ups", style = MaterialTheme.typography.titleLarge)
        Text("${number(data.fuel.sumOf { it.litres })} litres logged", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (intervals.isEmpty()) "Add two full-tank entries to calculate efficiency. Partial fills count between them." else "${number(intervals.sumOf { it.distance } / intervals.sumOf { it.litres })} km/L · weighted full-to-full average")
    }
    Button(onClick = { open("Add fuel") }, enabled = data.fuel.size < 1000) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add fill-up") }
    if (data.fuel.isEmpty()) EmptyGarage("Start with your next fill", "Enter the odometer, litres and amount paid. No vehicle connection is needed.")
    data.fuel.sortedByDescending { it.time }.forEach { f ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(dateLabel(f.time), style = MaterialTheme.typography.titleMedium)
            Text("${number(f.litres)} L · ${money(f.costMinor)} · ${number(f.odometerKm, 0)} km", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (f.fullTank) "Full tank" else "Partial fill", style = MaterialTheme.typography.labelMedium)
            intervals.firstOrNull { it.to == f.id }?.let { Text("${number(it.kmPerLitre)} km/L over ${number(it.distance, 0)} km", color = MaterialTheme.colorScheme.primary) }
            if (f.note.isNotBlank()) Text(f.note)
            TextButton(onClick = { open("Fuel:${f.id}") }) { Text("Edit fill-up") }
            HorizontalDivider()
        }
    }
}
@Composable internal fun FuelForm(data: GarageData, model: OpenSenseViewModel, id: String, done: () -> Unit) {
    val existing = data.fuel.firstOrNull { it.id == id }
    var odo by rememberSaveable(existing?.id) { mutableStateOf(existing?.odometerKm?.toString() ?: "") }
    var litres by rememberSaveable(existing?.id) { mutableStateOf(existing?.litres?.toString() ?: "") }
    var cost by rememberSaveable(existing?.id) { mutableStateOf(existing?.let { BigDecimal.valueOf(it.costMinor, 2).toPlainString() } ?: "") }
    var full by rememberSaveable(existing?.id) { mutableStateOf(existing?.fullTank ?: true) }
    var note by rememberSaveable(existing?.id) { mutableStateOf(existing?.note ?: "") }
    var time by rememberSaveable(existing?.id) { mutableLongStateOf(existing?.time ?: System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }; var saving by remember { mutableStateOf(false) }
    val storageProblem by model.garage.problem.collectAsStateWithLifecycle()
    LaunchedEffect(storageProblem) { if (storageProblem != null) saving = false }
    GarageIntro(if (existing == null) "Log a fill-up" else "Edit fill-up", "Use the pump receipt and your current odometer.")
    DateTimeField(time) { time = it }
    FormField("Odometer (km)", odo, { odo = it }, true)
    FormField("Fuel added (litres)", litres, { litres = it }, true)
    FormField("Total amount paid (₹)", cost, { cost = it }, true)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Filled to a full tank", Modifier.weight(1f)); Switch(full, { full = it }, Modifier.semantics { contentDescription = "Full tank" }) }
    FormField("Notes (optional)", note, { note = it }, lines = 2)
    FormError(error)
    Button(onClick = {
        val record = runCatching {
            val amount = BigDecimal(cost.trim()).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
            FuelEntry(existing?.id ?: UUID.randomUUID().toString(), time, odo.toDouble(), litres.toDouble(), amount, full, note.trim())
        }.getOrNull()
        error = when {
            record == null -> "Enter a valid odometer, 0–200 litres and a non-negative amount with up to two decimal places."
            time > System.currentTimeMillis() + 60000 -> "A fill-up cannot be in the future."
            data.fuel.any { it.id != record.id && it.odometerKm == record.odometerKm } -> "A fill-up at this odometer already exists. Edit it instead."
            else -> null
        }
        if (error == null && record != null) { saving = true; model.saveGarage({ it.copy(fuel = it.fuel.filterNot { f -> f.id == record.id } + record) }) { done() }.invokeOnCompletion { saving = false } }
    }, enabled = !saving) { Text(if (saving) "Saving…" else "Save fill-up") }
    if (existing != null) DeleteAction("Delete fill-up") { model.saveGarage({ it.copy(fuel = it.fuel.filterNot { f -> f.id == existing.id }) }, done) }
}

@Composable internal fun ReminderList(data: GarageData, model: OpenSenseViewModel, open: (String) -> Unit) {
    val context = LocalContext.current
    GarageIntro("Reminders", "A little less to remember. Service, insurance and your own dates.")
    if (!ReminderScheduler.allowed(context)) Text("Notifications are off. Reminders are saved, but alerts need notification permission.", color = MaterialTheme.colorScheme.error)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) data.reminders.forEach { ReminderScheduler.schedule(context, it) } }
    if (!ReminderScheduler.allowed(context)) OutlinedButton(onClick = {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
    }) { Text("Enable reminders") }
    Button(onClick = { open("Add reminder") }, enabled = data.reminders.size < 200) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text("Add reminder") }
    Text("Android may deliver an alert later than its selected time because of battery restrictions. No exact-alarm permission is requested.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (data.reminders.isEmpty()) EmptyGarage("Nothing to keep track of yet", "Add a date and choose how early you want a reminder.")
    data.reminders.sortedWith(compareBy<Reminder> { it.done }.thenBy { it.dueAt }).forEach { r ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(r.title, style = MaterialTheme.typography.titleMedium)
            Text(dateLabel(r.dueAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (r.done) Text("Completed", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { open("Reminder:${r.id}") }) { Text("Edit") }
                TextButton(onClick = { model.saveGarage({ d -> d.copy(reminders = d.reminders.map { if (it.id == r.id) it.copy(done = !r.done, notified = false) else it }) }) { if (!r.done) ReminderScheduler.cancel(context, r.id) else ReminderScheduler.schedule(context, r.copy(done = false, notified = false)) } }) { Text(if (r.done) "Reopen" else "Mark done") }
            }
            HorizontalDivider()
        }
    }
}
@Composable internal fun ReminderForm(data: GarageData, model: OpenSenseViewModel, id: Int?, done: () -> Unit) {
    val context = LocalContext.current
    val current = data.reminders.firstOrNull { it.id == id }
    var title by rememberSaveable(current?.id) { mutableStateOf(current?.title ?: "") }
    var due by rememberSaveable(current?.id) { mutableLongStateOf(current?.dueAt ?: (System.currentTimeMillis() + 86400000)) }
    var lead by rememberSaveable(current?.id) { mutableLongStateOf(current?.leadMillis ?: 3600000) }
    var error by remember { mutableStateOf<String?>(null) }
    GarageIntro(if (current == null) "Add a reminder" else "Edit reminder", "Choose the date and when you want the heads-up.")
    FormField("Reminder title", title, { title = it })
    DateTimeField(due) { due = it }
    Section("Remind me")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("At the time" to 0L, "1 hour before" to 3600000L, "1 day before" to 86400000L, "1 week before" to 604800000L, "30 days before" to 2592000000L).forEach { (label, duration) ->
            FilterChip(selected = lead == duration, onClick = { lead = duration }, label = { Text(label) })
        }
    }
    if (due - lead < System.currentTimeMillis()) Text("That advance-alert time has passed. Android will schedule the reminder for the next available moment.", style = MaterialTheme.typography.bodySmall)
    FormError(error)
    Button(onClick = {
        error = if (title.isBlank()) "Give this reminder a title." else if (due <= System.currentTimeMillis()) "Choose a future date and time." else null
        if (error == null) {
            val nextId = current?.id ?: ((data.reminders.maxOfOrNull { it.id } ?: 0) + 1)
            val record = Reminder(nextId, title.trim(), due, lead)
            model.saveGarage({ d -> d.copy(reminders = d.reminders.filterNot { it.id == nextId } + record) }) { ReminderScheduler.schedule(context, record); done() }
        }
    }) { Text("Save reminder") }
    if (current != null) DeleteAction("Delete reminder") { model.saveGarage({ d -> d.copy(reminders = d.reminders.filterNot { it.id == current.id }) }) { ReminderScheduler.cancel(context, current.id); done() } }
}

@Composable internal fun OwnerForm(owner: OwnerRecord, model: OpenSenseViewModel, done: () -> Unit) {
    var name by rememberSaveable(owner) { mutableStateOf(owner.name) }; var phone by rememberSaveable(owner) { mutableStateOf(owner.phone) }
    var email by rememberSaveable(owner) { mutableStateOf(owner.email) }; var registration by rememberSaveable(owner) { mutableStateOf(owner.registration) }
    var insurance by rememberSaveable(owner) { mutableStateOf(owner.insurance) }; var policy by rememberSaveable(owner) { mutableStateOf(owner.policy) }
    var contact by rememberSaveable(owner) { mutableStateOf(owner.assistanceName) }; var contactPhone by rememberSaveable(owner) { mutableStateOf(owner.assistancePhone) }
    var notes by rememberSaveable(owner) { mutableStateOf(owner.notes) }
    val fields = listOf("address" to "Address", "alternate" to "Alternate phone", "dob" to "Date of birth", "licence" to "Driving licence", "place" to "Registration place", "engine" to "Engine / chassis details", "year" to "Model year", "colour" to "Colour", "fuel" to "Fuel type", "tank" to "Tank capacity (litres)")
    val details = remember(owner) { mutableStateMapOf<String, String>().apply { putAll(owner.details) } }
    GarageIntro("Personal & vehicle records", "Private ownership details. Nothing here is uploaded or included in the standard export.")
    Section("Owner")
    FormField("Name", name, { name = it }); FormField("Phone", phone, { phone = it }); FormField("Email", email, { email = it })
    fields.take(4).forEach { (key, label) -> FormField(label, details[key].orEmpty(), { details[key] = it }) }
    Section("Vehicle")
    FormField("Registration number", registration, { registration = it })
    fields.drop(4).forEach { (key, label) -> FormField(label, details[key].orEmpty(), { details[key] = it }) }
    Section("Policy & assistance")
    FormField("Insurance provider", insurance, { insurance = it }); FormField("Policy number", policy, { policy = it })
    FormField("Assistance contact name", contact, { contact = it }); FormField("Assistance contact phone", contactPhone, { contactPhone = it })
    FormField("Other details", notes, { notes = it }, lines = 3)
    Button(onClick = { model.saveGarage({ it.copy(owner = OwnerRecord(name.trim(), phone.trim(), email.trim(), registration.trim(), insurance.trim(), policy.trim(), contact.trim(), contactPhone.trim(), notes.trim(), details.toMap())) }, done) }) { Text("Save records") }
}

@Composable internal fun WarningForm(current: WarningPreferences, model: OpenSenseViewModel) {
    var preferences by remember(current) { mutableStateOf(current) }
    GarageIntro("Warning preferences", "Choose the information you want to see. These settings do not replace the vehicle's warning systems.")
    listOf("Door status" to preferences.doors, "Service reminders" to preferences.service, "Brake-pad status" to preferences.brake, "Tyre status" to preferences.tyres).forEachIndexed { index, (label, checked) ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, Modifier.weight(1f)); Switch(checked, { value -> preferences = when (index) { 0 -> preferences.copy(doors = value); 1 -> preferences.copy(service = value); 2 -> preferences.copy(brake = value); else -> preferences.copy(tyres = value) } }, Modifier.semantics { contentDescription = label })
        }
    }
    Text("Low range: ${preferences.lowRangeKm} km", style = MaterialTheme.typography.titleMedium)
    Slider(preferences.lowRangeKm.toFloat(), { preferences = preferences.copy(lowRangeKm = it.toInt()) }, valueRange = 10f..200f, steps = 18,
        modifier = Modifier.semantics { contentDescription = "Low-range reference"; stateDescription = "${preferences.lowRangeKm} kilometres" })
    Text("GPS trip speed reference: ${preferences.speedLimitKmh} km/h", style = MaterialTheme.typography.titleMedium)
    Slider(preferences.speedLimitKmh.toFloat(), { preferences = preferences.copy(speedLimitKmh = it.toInt()) }, valueRange = 30f..120f, steps = 8,
        modifier = Modifier.semantics { contentDescription = "GPS speed reference"; stateDescription = "${preferences.speedLimitKmh} kilometres per hour" })
    Text("Preferences affect the in-app display. Unconfirmed signal meanings do not trigger automatic safety alerts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    var saved by remember { mutableStateOf(false) }
    Button(onClick = { model.saveGarage({ it.copy(warnings = preferences) }) { saved = true } }) { Text("Save preferences") }
    if (saved) Text("Preferences saved", color = MaterialTheme.colorScheme.primary)
}
