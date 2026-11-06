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
    var shareDiagnostics: Boolean
        get() = preferences.getBoolean("share_diagnostics", false)
        set(value) { preferences.edit().putBoolean("share_diagnostics", value).apply() }
    var consentAsked: Boolean
        get() = preferences.getBoolean("consent_asked", false)
        set(value) { preferences.edit().putBoolean("consent_asked", value).apply() }
    /** Random, resettable, unrelated to the phone or vehicle. Created only once sharing is on. */
    val installId: String
        get() = preferences.getString("install_id", null) ?: java.util.UUID.randomUUID().toString().also { preferences.edit().putString("install_id", it).apply() }
    fun resetInstallId() { preferences.edit().remove("install_id").apply() }
    var profile: VehicleProfile?
        get() = runCatching {
            val family = InfotainmentFamily.valueOf(preferences.getString("family", "") ?: "")
            VehicleProfile(family, preferences.getString("variant", "") ?: "")
        }.getOrNull()
        set(value) {
            preferences.edit().putString("family", value?.family?.name).putString("variant", value?.variant).apply()
        }
}
