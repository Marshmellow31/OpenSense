package org.opensense.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Midnight = darkColorScheme(
    primary = Color(0xFFA9C7FF), onPrimary = Color(0xFF12305C), primaryContainer = Color(0xFF294775),
    onPrimaryContainer = Color(0xFFD8E5FF), secondary = Color(0xFFB9C7DF), onSecondary = Color(0xFF243247),
    secondaryContainer = Color(0xFF303E54), onSecondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFFE8C383), onTertiary = Color(0xFF412D06), tertiaryContainer = Color(0xFF58431D),
    onTertiaryContainer = Color(0xFFFFDEA7), background = Color(0xFF0C111A), onBackground = Color(0xFFE3E8F2),
    surface = Color(0xFF101620), onSurface = Color(0xFFE3E8F2), surfaceVariant = Color(0xFF273344),
    onSurfaceVariant = Color(0xFFB5C0D1), surfaceContainer = Color(0xFF18212E),
    surfaceContainerHigh = Color(0xFF212D3D), surfaceContainerLow = Color(0xFF121B27),
    outline = Color(0xFF8391A6), outlineVariant = Color(0xFF364355), error = Color(0xFFFFB4AB))
private val Cloud = lightColorScheme(
    primary = Color(0xFF355F9A), onPrimary = Color.White, primaryContainer = Color(0xFFD8E5FF),
    onPrimaryContainer = Color(0xFF17365F), secondary = Color(0xFF536079), secondaryContainer = Color(0xFFE0E8F6),
    onSecondaryContainer = Color(0xFF26374F), tertiary = Color(0xFF775A20), tertiaryContainer = Color(0xFFFFE5AD),
    onTertiaryContainer = Color(0xFF4F3808), background = Color(0xFFF5F7FC), onBackground = Color(0xFF172131),
    surface = Color(0xFFFAFBFF), onSurface = Color(0xFF172131), onSurfaceVariant = Color(0xFF4F5D70),
    surfaceVariant = Color(0xFFE0E6EF), surfaceContainer = Color(0xFFE9EEF7),
    surfaceContainerHigh = Color(0xFFDEE6F2), surfaceContainerLow = Color(0xFFF0F3FA), outlineVariant = Color(0xFFC3CDDC))

@Composable fun OpenSenseTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Midnight else Cloud, content = content)
}

@Composable fun ScreenHeading(title: String, vehicle: String, demo: Boolean, connection: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (title == "Vehicle") "My XUV500" else title, modifier = Modifier.testTag("screen-title"), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Medium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(vehicle.removePrefix("XUV500 "), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(50), color = if (demo) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer, onClick = connection) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (demo) Icons.Outlined.Science else Icons.Outlined.BluetoothDisabled, null, Modifier.size(16.dp))
                    Text(if (demo) "Demo" else "Offline", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable fun Section(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable fun TonalPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
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
        Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelLarge)
        if (onClick != null) Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
