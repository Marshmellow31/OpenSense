package org.opensense.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Rule
import org.junit.Test
import org.opensense.core.diagnostics.DiagnosticCode
import org.opensense.core.model.InfotainmentFamily

/** Verifies the actual diagnostics screen without connecting to a vehicle. */
class DiagnosticsTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun openDiagnostics() {
        rule.onNodeWithContentDescription("Connection details").performClick()
        rule.onNodeWithText("Session diagnostics").performScrollTo().performClick()
    }

    @Test fun simulationEventsAreAutomaticallyRecordedVisibleAndClearable() {
        rule.runOnUiThread {
            val model = ViewModelProvider(rule.activity)[OpenSenseViewModel::class.java]
            model.pause()
            model.log.clear()
        }
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithContentDescription("Simulation mode").performScrollTo().performClick()
        rule.waitUntil(10000) {
            ViewModelProvider(rule.activity)[OpenSenseViewModel::class.java].log.events.value
                .any { it.code == DiagnosticCode.SAMPLE_ACCEPTED }
        }
        rule.onNodeWithContentDescription("Simulation mode").performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        openDiagnostics()
        rule.onNodeWithText("demo started").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("demo stopped").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Clear diagnostics").performScrollTo().performClick()
        rule.onNodeWithText("No events in this session.").assertIsDisplayed()
    }

    @Test fun failedConnectionIsLoggedWithoutDeviceIdentifiers() {
        rule.runOnUiThread {
            val model = ViewModelProvider(rule.activity)[OpenSenseViewModel::class.java]
            model.pause()
            model.log.clear()
            model.vehicleConnection.connect("private-test-device", InfotainmentFamily.NEW)
            check(model.log.events.value.single().code == DiagnosticCode.VEHICLE_SESSION_FAILED)
        }
        openDiagnostics()
        rule.onNodeWithText("vehicle session failed").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("private-test-device", substring = true).assertDoesNotExist()
    }
}
