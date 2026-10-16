package org.opensense.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.opensense.core.model.InfotainmentFamily
import org.opensense.core.model.VehicleProfile
import java.io.File

/** Opt-in emulator capture of actual application UI; no original APK or vehicle is involved. */
class UiCaptureTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        rule.waitForIdle()
        when {
            name.startsWith("vehicle") -> rule.onNodeWithTag("screen-title").assertTextEquals("My XUV500")
            name.startsWith("climate") -> rule.onNodeWithTag("screen-title").assertTextEquals("Climate")
            name.startsWith("media-sources") -> rule.onNodeWithText("Demo media source").assertExists()
            name.startsWith("media") -> rule.onNodeWithTag("screen-title").assertTextEquals("Media")
            name.startsWith("insights") -> rule.onNodeWithTag("screen-title").assertTextEquals("Insights")
        }
        if (name in listOf("vehicle-offline-dark", "vehicle-demo-dark", "climate-dark", "media-dark", "insights-dark", "vehicle-light", "climate-light")) {
            rule.onNodeWithTag("screen-title").assertIsDisplayed()
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        Thread.sleep(350) // Allow a real emulator vsync after Compose settles, not a synthetic render.
        val prefix = InstrumentationRegistry.getArguments().getString("capturePrefix") ?: "phone"
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "ui-review").apply { mkdirs() }
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$prefix-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        File(directory, "$prefix-captures.txt").appendText("$prefix-$name.png: ${bitmap.width}x${bitmap.height}; density=${instrumentation.targetContext.resources.displayMetrics.densityDpi}; fontScale=${instrumentation.targetContext.resources.configuration.fontScale}\n")
        bitmap.recycle()
    }
    @Test fun captureSupportedSurfaces() {
        rule.runOnUiThread {
            ViewModelProvider(rule.activity)[OpenSenseViewModel::class.java].apply {
                pause(); setDark(true); selectProfile(VehicleProfile(InfotainmentFamily.NEW, "W7"))
            }
        }
        capture("vehicle-offline-dark")
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithContentDescription("Simulation mode").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText("Demo", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        capture("vehicle-demo-dark")
        rule.onNodeWithText("Warnings").performScrollTo()
        capture("vehicle-status-dark")
        rule.onNodeWithTag("nav-Climate").assertIsDisplayed().performClick()
        capture("climate-dark")
        rule.onNodeWithText("Both").performScrollTo()
        capture("climate-direction-dark")
        rule.onNodeWithText("Demist").performScrollTo()
        capture("climate-controls-dark")
        rule.onNodeWithTag("nav-Media").assertIsDisplayed().performClick()
        capture("media-dark")
        rule.onNodeWithText("Source · Bluetooth").performScrollTo().performClick()
        capture("media-sources-dark")
        rule.onNodeWithText("AUX").performScrollTo().assertIsDisplayed()
        capture("media-sources-bottom-dark")
        rule.onNodeWithText("AUX").performClick()
        var currentSource = "AUX"
        listOf("USB", "Radio", "Bluetooth").forEach { source ->
            rule.onNodeWithText("Source · $currentSource").performScrollTo().performClick()
            rule.onNodeWithText(source).performScrollTo().assertIsDisplayed().performClick()
            currentSource = source
        }
        rule.onNodeWithTag("nav-Insights").assertIsDisplayed().performClick()
        capture("insights-dark")
        rule.onNodeWithContentDescription("Settings").performClick()
        capture("settings-dark")
        rule.onNodeWithContentDescription("Dark theme").performScrollTo()
        capture("settings-appearance-dark")
        rule.onNodeWithContentDescription("Dark theme").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithTag("nav-Vehicle").assertIsDisplayed().performClick()
        capture("vehicle-light")
        rule.onNodeWithTag("nav-Climate").assertIsDisplayed().performClick()
        capture("climate-light")
        rule.runOnUiThread { ViewModelProvider(rule.activity)[OpenSenseViewModel::class.java].apply { pause(); setDark(true) } }
    }
}
