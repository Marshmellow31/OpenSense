package org.opensense.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// v2 "Soft daylight": warm sand and charcoal grounds, forest/sage actions, apricot for highlights.
// Amber (tertiary) stays reserved for simulation so demo data is never mistaken for vehicle data.
private val Charcoal = darkColorScheme(
    primary = Color(0xFF8FD1AE), onPrimary = Color(0xFF0F2A1E), primaryContainer = Color(0xFF2E5E48),
    onPrimaryContainer = Color(0xFFEAF4EC), secondary = Color(0xFFC9C2B3), onSecondary = Color(0xFF2A2720),
    secondaryContainer = Color(0xFF38342D), onSecondaryContainer = Color(0xFFE9E3D6),
    tertiary = Color(0xFFF2A65A), onTertiary = Color(0xFF3B2308), tertiaryContainer = Color(0xFF3B2A18),
    onTertiaryContainer = Color(0xFFF7D9B8), background = Color(0xFF14130F), onBackground = Color(0xFFF3EFE6),
    surface = Color(0xFF14130F), onSurface = Color(0xFFF3EFE6), surfaceVariant = Color(0xFF2E2B25),
    onSurfaceVariant = Color(0xFFA39E92), surfaceContainerLowest = Color(0xFF0F0E0B), surfaceContainerLow = Color(0xFF1F1D18),
    surfaceContainer = Color(0xFF26231E), surfaceContainerHigh = Color(0xFF2E2B25), surfaceContainerHighest = Color(0xFF38342D),
    outline = Color(0xFF8A8478), outlineVariant = Color(0xFF34312A), error = Color(0xFFFFB4AB))
private val Sand = lightColorScheme(
    primary = Color(0xFF1F4D3A), onPrimary = Color(0xFFF3EFE6), primaryContainer = Color(0xFFC9E3D3),
    onPrimaryContainer = Color(0xFF12301F), secondary = Color(0xFF5E5A52), secondaryContainer = Color(0xFFE9E3D6),
    onSecondaryContainer = Color(0xFF2A2720), tertiary = Color(0xFF8A5A12), tertiaryContainer = Color(0xFFF7D9B8),
    onTertiaryContainer = Color(0xFF5A3A12), background = Color(0xFFEFEAE0), onBackground = Color(0xFF1C1B19),
    surface = Color(0xFFEFEAE0), onSurface = Color(0xFF1C1B19), onSurfaceVariant = Color(0xFF5E5A52),
    surfaceVariant = Color(0xFFE4DED2), surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFBF9F4),
    surfaceContainer = Color(0xFFF3EEE3), surfaceContainerHigh = Color(0xFFE9E3D6), surfaceContainerHighest = Color(0xFFDDD6C7),
    outline = Color(0xFF7A7568), outlineVariant = Color(0xFFE4DED2), error = Color(0xFFB3261E))

private val Type = Typography().let { t ->
    val serif = FontFamily.Serif
    t.copy(
        displayLarge = t.displayLarge.copy(fontFamily = serif, fontWeight = FontWeight.Light, letterSpacing = (-2).sp, fontFeatureSettings = "tnum"),
        displayMedium = t.displayMedium.copy(fontFamily = serif, fontWeight = FontWeight.Light, letterSpacing = (-1.5).sp, fontFeatureSettings = "tnum"),
        displaySmall = t.displaySmall.copy(fontFamily = serif, fontWeight = FontWeight.Light, letterSpacing = (-1).sp, fontFeatureSettings = "tnum"),
        headlineLarge = t.headlineLarge.copy(fontFamily = serif, fontWeight = FontWeight.Light, letterSpacing = (-0.6).sp),
        headlineMedium = t.headlineMedium.copy(fontFamily = serif, fontWeight = FontWeight.Light, letterSpacing = (-0.4).sp),
        headlineSmall = t.headlineSmall.copy(fontFamily = serif, fontWeight = FontWeight.Light, letterSpacing = (-0.3).sp),
        titleLarge = t.titleLarge.copy(fontFamily = serif, fontWeight = FontWeight.Normal),
        labelLarge = t.labelLarge.copy(fontFeatureSettings = "tnum"))
}
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp))

@Composable fun OpenSenseTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Charcoal else Sand, typography = Type, shapes = AppShapes, content = content)
}

/** Connection state pill. Amber dot = simulation, never a verified vehicle warning. */
@Composable fun StatusPill(demo: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = CircleShape, color = if (demo) colors.onSurface else colors.surfaceContainerLow, border = if (demo) null else BorderStroke(1.dp, colors.outlineVariant), onClick = onClick) {
        Row(Modifier.heightIn(min = 40.dp).padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(if (demo) Color(0xFFF2A65A) else colors.outline, CircleShape))
            Text(if (demo) "Demo" else "Offline", style = MaterialTheme.typography.labelLarge, color = if (demo) colors.background else colors.onSurfaceVariant)
        }
    }
}

@Composable fun ScreenHeading(title: String, vehicle: String, demo: Boolean, connection: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(vehicle.removePrefix("XUV500 "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (title == "Home") "My XUV500" else title, modifier = Modifier.testTag("screen-title"), style = MaterialTheme.typography.headlineLarge)
        }
        StatusPill(demo, connection)
    }
}

/** Pill-shaped switch between two or more views of the same destination (e.g. Climate | Media). */
@Composable fun Segmented(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = CircleShape, color = colors.surfaceContainerLow, border = BorderStroke(1.dp, colors.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            options.forEach { option ->
                val on = option == selected
                Surface(onClick = { onSelect(option) }, shape = CircleShape, color = if (on) colors.onSurface else Color.Transparent, modifier = Modifier.weight(1f).testTag("seg-$option")) {
                    Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
                        Text(option, style = MaterialTheme.typography.labelLarge, color = if (on) colors.background else colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Square-ish action tile. [accent] fills with the primary container colour. */
@Composable fun Tile(icon: ImageVector, title: String, detail: String, modifier: Modifier = Modifier, accent: Boolean = false, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(24.dp),
        color = if (accent) colors.primary else colors.surfaceContainerLow,
        border = if (accent) null else BorderStroke(1.dp, colors.outlineVariant)) {
        Column(Modifier.padding(16.dp).heightIn(min = 84.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = if (accent) colors.onPrimary else colors.primary)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (accent) colors.onPrimary else colors.onSurface)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = if (accent) colors.onPrimary.copy(alpha = .8f) else colors.onSurfaceVariant)
            }
        }
    }
}

/** Bottom navigation: the selected destination is a filled pill. */
@Composable fun HomeNav(destinations: List<Pair<String, ImageVector>>, selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().background(colors.background).navigationBarsPadding().padding(top = 8.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
        destinations.forEach { (name, icon) ->
            val on = name == selected
            Column(Modifier.weight(1f).heightIn(min = 56.dp).clickable { onSelect(name) }.testTag("nav-$name"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = CircleShape, color = if (on) colors.onSurface else Color.Transparent) {
                    Box(Modifier.size(width = 60.dp, height = 32.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(20.dp), tint = if (on) colors.background else colors.onSurfaceVariant)
                    }
                }
                Text(name, style = MaterialTheme.typography.labelMedium, color = if (on) colors.onSurface else colors.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable fun Section(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable fun TonalPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable fun StateNote(demo: Boolean, text: String = "Vehicle controls need a verified connection.") {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(if (demo) Icons.Outlined.Science else Icons.Outlined.Info, null, Modifier.size(18.dp), tint = if (demo) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (demo) "Simulated data · no vehicle connected" else text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun DetailRow(icon: ImageVector, title: String, subtitle: String, trailing: String? = null, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).heightIn(min = 64.dp).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelLarge)
        if (onClick != null) Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
