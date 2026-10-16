package org.opensense.app

import android.Manifest
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.opensense.app.garage.TripService
import org.opensense.app.storage.GarageStore
import java.io.File

/** Host supplies emulator geo fixes after the ready marker. No physical GPS/car evidence. */
class TripRecorderTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun foregroundRecorderPersistsEmulatorLocationAndStopsCleanly() {
        val context = rule.activity
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = GarageStore.get(context)
        val original = runBlocking { store.snapshot() }
        val marker = File(context.getExternalFilesDir(null), "gps-test-ready")
        try {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
            if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
            rule.runOnUiThread { TripService.start(context) }
            rule.waitUntil(20000) { TripService.state.value.active }
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            Assert.assertTrue("The user-started recorder must stay active after the Activity backgrounds", TripService.state.value.active)
            marker.writeText("Synthetic emulator GPS test ready")
            try { rule.waitUntil(45000) {
                val current = TripService.state.value
                marker.writeText("points=${current.points.size}; distance=${current.distance}; active=${current.active}; ${current.message}")
                current.points.size >= 2 && current.distance > 3
            } } catch (failure: Exception) {
                val current = TripService.state.value
                throw AssertionError("Emulator GPS: points=${current.points.size}, distance=${current.distance}, active=${current.active}, message=${current.message}", failure)
            }
            rule.runOnUiThread { TripService.stop(context) }
            rule.waitUntil(15000) { !TripService.state.value.active }
            val saved = store.data.value.trips.firstOrNull { candidate -> original.trips.none { it.id == candidate.id } }
            Assert.assertNotNull("Location fixes should produce a persisted trip", saved)
            Assert.assertTrue(saved!!.distanceMetres > 3)
            Assert.assertFalse(saved.interrupted)
        } finally {
            marker.delete()
            context.stopService(Intent(context, TripService::class.java))
            runBlocking { store.update { original } }
        }
    }
}
