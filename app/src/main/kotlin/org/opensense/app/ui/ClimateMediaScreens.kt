package org.opensense.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import org.opensense.app.DemoControls
import org.opensense.core.model.VehicleSnapshot

@Composable private fun CabinDiagram(active: Boolean, controls: DemoControls, modifier: Modifier = Modifier, height: Dp = 240.dp) {
    val colors = MaterialTheme.colorScheme
    // Compose animation APIs follow the platform animator-duration scale, including Remove animations.
    val phase = if (active && controls.fan > 0 && controls.ac) {
        val transition = rememberInfiniteTransition(label = "Cabin airflow")
        val value by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "Airflow phase")
        value
    } else 0f
    Canvas(modifier.fillMaxWidth().height(height).semantics { contentDescription = "Cabin schematic. ${if (active) "Simulated fan level ${controls.fan}, ${controls.airflow.lowercase()} airflow" else "Vehicle climate unavailable"}" }) {
        val w = size.width * .48f; val h = size.height * .92f; val x = (size.width - w) / 2; val y = size.height * .04f
        drawRoundRect(colors.surfaceContainer, Offset(x, y), Size(w, h), CornerRadius(w * .25f), style = Stroke(2.dp.toPx()))
        drawRoundRect(colors.outlineVariant, Offset(x + w * .16f, y + h * .08f), Size(w * .68f, h * .14f), CornerRadius(12.dp.toPx()), style = Stroke(1.dp.toPx()))
        listOf(.29f, .66f).forEach { row ->
            listOf(.16f, .59f).forEach { col ->
                drawRoundRect(colors.surfaceContainerHigh, Offset(x + w * col, y + h * row), Size(w * .25f, h * .23f), CornerRadius(8.dp.toPx()))
                drawRoundRect(colors.outlineVariant, Offset(x + w * col, y + h * row), Size(w * .25f, h * .23f), CornerRadius(8.dp.toPx()), style = Stroke(1.dp.toPx()))
                drawLine(colors.outlineVariant, Offset(x + w * col, y + h * (row + .06f)), Offset(x + w * (col + .25f), y + h * (row + .06f)), 1.dp.toPx())
            }
        }
        if (active && controls.fan > 0 && controls.ac) {
            repeat(if (controls.rear || controls.airflow == "Both") 6 else 3) { index ->
                val startX = x + w * (.28f + (index % 3) * .22f)
                val startY = y + h * (if (index >= 3) .58f else if (controls.airflow == "Feet") .4f else .18f)
                val length = h * .2f
                val wave = Path().apply {
                    moveTo(startX, startY)
                    cubicTo(startX - 8.dp.toPx(), startY + length * .35f, startX + 8.dp.toPx(), startY + length * .65f, startX, startY + length)
                }
                drawPath(wave, colors.primary.copy(alpha = .35f + phase * .5f), style = Stroke((1.4f + controls.fan * .15f).dp.toPx()))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun ClimateScreen(sample: VehicleSnapshot?, controls: DemoControls, wide: Boolean, short: Boolean, change: ((DemoControls) -> DemoControls) -> Unit) {
    val active = sample != null
    if (!short) StateNote(active)
    val temperature: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (short) 5.dp else 14.dp)) {
            Text("Target temperature", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (short) 12.dp else 22.dp)) {
                FilledTonalIconButton(onClick = { change { it.copy(target = (it.target - 1).coerceAtLeast(16)) } }, enabled = active && controls.target > 16,
                    modifier = Modifier.size(if (short) 48.dp else 52.dp)) { Icon(Icons.Outlined.Remove, "Decrease temperature") }
                Text(if (active) "${controls.target}°" else "—°", Modifier.testTag("target-temperature"), style = if (short) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light)
                FilledTonalIconButton(onClick = { change { it.copy(target = (it.target + 1).coerceAtMost(30)) } }, enabled = active && controls.target < 30,
                    modifier = Modifier.size(if (short) 48.dp else 52.dp)) { Icon(Icons.Outlined.Add, "Increase temperature") }
            }
            Text("Cabin ${sample?.cabinCelsius?.let { "$it°" } ?: "—"}   ·   Outside ${sample?.outsideCelsius?.let { "$it°" } ?: "—"}",
                style = if (short) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (wide) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            temperature()
            if (short) FanControl(active, controls, change)
        }
        CabinDiagram(active, controls, Modifier.weight(1f), if (short) 170.dp else 240.dp)
    } else { temperature(); CabinDiagram(active, controls) }
    if (!short) FanControl(active, controls, change)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Section("Air direction")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Face", "Feet", "Both").forEach { mode ->
                FilterChip(selected = controls.airflow == mode, onClick = { change { it.copy(airflow = mode) } }, enabled = active,
                    modifier = Modifier.heightIn(min = 48.dp), label = { Text(mode, maxLines = 1, softWrap = false) }, leadingIcon = { Icon(if (mode == "Face") Icons.Outlined.Air else Icons.Outlined.South, null, Modifier.size(18.dp)) })
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Section("Comfort controls")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControlButton("AC", Icons.Outlined.AcUnit, controls.ac, active, Modifier.weight(1f)) { change { it.copy(ac = !it.ac) } }
            ControlButton("Auto", Icons.Outlined.AutoMode, controls.auto, active, Modifier.weight(1f)) { change { it.copy(auto = !it.auto, fan = if (!it.auto) 3 else it.fan) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControlButton("Recirculate", Icons.Outlined.Loop, controls.recirculate, active, Modifier.weight(1f)) { change { it.copy(recirculate = !it.recirculate) } }
            ControlButton("Rear air", Icons.Outlined.AirlineSeatReclineNormal, controls.rear, active, Modifier.weight(1f)) { change { it.copy(rear = !it.rear) } }
        }
        ControlButton("Demist", Icons.Outlined.Waves, controls.defrost, active, Modifier.fillMaxWidth()) { change { it.copy(defrost = !it.defrost) } }
    }
    Text(if (active) "These controls change a local simulation only. Rear-air fitment and vehicle commands remain unverified."
        else "Climate preview only. Simulation can be enabled in Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun FanControl(active: Boolean, controls: DemoControls, change: ((DemoControls) -> DemoControls) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Fan speed", style = MaterialTheme.typography.titleMedium)
            Text(if (active) "${controls.fan} / 7" else "—", color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = controls.fan.toFloat(), onValueChange = { value -> change { it.copy(fan = value.toInt(), auto = false) } }, enabled = active,
            valueRange = 0f..7f, steps = 6, modifier = Modifier.semantics { contentDescription = "Simulated fan speed" })
    }
}

@Composable private fun ControlButton(label: String, icon: ImageVector, selected: Boolean, enabled: Boolean, modifier: Modifier, click: () -> Unit) {
    FilledTonalButton(onClick = click, modifier = modifier.heightIn(min = 80.dp), enabled = enabled, contentPadding = PaddingValues(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(22.dp)); Text(label, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MediaScreen(sample: VehicleSnapshot?, controls: DemoControls, short: Boolean, change: ((DemoControls) -> DemoControls) -> Unit) {
    val active = sample != null
    var sourcePicker by remember { mutableStateOf(false) }
    if (!short) StateNote(active)
    val colors = MaterialTheme.colorScheme
    val record: @Composable () -> Unit = {
      Box(Modifier.fillMaxWidth().height(if (short) 160.dp else 210.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = colors.surfaceContainer, modifier = Modifier.size(if (short) 148.dp else 196.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(if (short) 128.dp else 174.dp)) {
                    repeat(5) { drawCircle(colors.outlineVariant.copy(alpha = .65f), size.minDimension * (.26f + it * .055f), style = Stroke(1.dp.toPx())) }
                }
                Surface(shape = CircleShape, color = colors.primaryContainer, modifier = Modifier.size(86.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.MusicNote, null, Modifier.size(36.dp), tint = colors.onPrimaryContainer) }
                }
            }
        }
      }
    }
    val player: @Composable () -> Unit = {
      Column(verticalArrangement = Arrangement.spacedBy(if (short) 8.dp else 24.dp)) {
      Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (active) listOf("Open road", "Late afternoon", "Night drive")[controls.track] else "Your next soundtrack", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
        Text(if (active) "Demo audio · ${controls.source}" else "Connect a supported vehicle to view media", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (short) 12.dp else 24.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { change { it.copy(track = (it.track + 2) % 3, playing = true) } }, enabled = active, modifier = Modifier.size(52.dp)) { Icon(Icons.Outlined.SkipPrevious, "Previous demo track", Modifier.size(32.dp)) }
        FilledIconButton(onClick = { change { it.copy(playing = !it.playing) } }, enabled = active, modifier = Modifier.size(if (short) 56.dp else 76.dp)) {
            Icon(if (controls.playing && active) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (controls.playing && active) "Pause demo playback" else "Play demo playback", Modifier.size(36.dp))
        }
        IconButton(onClick = { change { it.copy(track = (it.track + 1) % 3, playing = true) } }, enabled = active, modifier = Modifier.size(52.dp)) { Icon(Icons.Outlined.SkipNext, "Next demo track", Modifier.size(32.dp)) }
    }
    Text(if (active) if (controls.playing) "Simulated playback · Playing" else "Simulated playback · Paused" else "Playback unavailable", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
      }
    }
    if (short) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(Modifier.weight(0.8f)) { record() }
        Box(Modifier.weight(1.2f)) { player() }
    } else { record(); player() }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Outlined.VolumeDown, null)
        Slider(value = controls.volume, onValueChange = { value -> change { it.copy(volume = value) } }, enabled = active,
            modifier = Modifier.weight(1f).semantics { contentDescription = "Simulated media volume" })
        Icon(Icons.Outlined.VolumeUp, null)
    }
    OutlinedButton(onClick = { sourcePicker = true }, enabled = active, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Icon(Icons.Outlined.SettingsInputComponent, null); Spacer(Modifier.width(10.dp)); Text(if (active) "Source · ${controls.source}" else "Source unavailable")
    }
    if (active) Text("Playback and volume are visual simulation only. This app is not playing audio or controlling the head unit.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    if (sourcePicker) ModalBottomSheet(onDismissRequest = { sourcePicker = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text("Demo media source", style = MaterialTheme.typography.titleLarge)
            listOf("Bluetooth", "USB", "Radio", "AUX").forEach { source ->
                TextButton(onClick = { change { it.copy(source = source) }; sourcePicker = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(source) }
            }
        }
    }
}
