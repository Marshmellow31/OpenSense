package org.opensense.app.storage

import android.content.Context
import org.opensense.core.model.InfotainmentFamily
import org.opensense.core.model.VehicleProfile

/** Preferences only. Never persists device identifiers, telemetry, or location. */
class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("opensense_settings", Context.MODE_PRIVATE)
    var darkTheme: Boolean
        get() = preferences.getBoolean("dark_theme", true)
        set(value) { preferences.edit().putBoolean("dark_theme", value).apply() }
    var profile: VehicleProfile?
        get() = runCatching {
            val family = InfotainmentFamily.valueOf(preferences.getString("family", "") ?: "")
            VehicleProfile(family, preferences.getString("variant", "") ?: "")
        }.getOrNull()
        set(value) {
            preferences.edit().putString("family", value?.family?.name).putString("variant", value?.variant).apply()
        }
}
