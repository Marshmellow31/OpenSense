package org.opensense.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.opensense.app.ui.*
import org.opensense.bluetooth.*
import org.opensense.core.model.*
import java.io.File
import java.io.FileOutputStream

/** Explicit synthetic states. These tests never connect a Bluetooth device. */
class LiveUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun show(tab: String, family: InfotainmentFamily, vehicle: LiveVehicle, celsius: Boolean? = null) {
        rule.runOnUiThread {
            val model = ViewModelProvider(rule.activity)[OpenSenseViewModel::class.java]
            model.pause()
            rule.activity.setContent {
                OpenSenseTheme(true) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            LiveScreen(tab, VehicleLinkState(VehicleLink.ACTIVE, vehicle, "Synthetic test fixture · no vehicle", celsius = celsius),
                                VehicleProfile(family, family.variants.first()), model)
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
    }
    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "live-ui-review").apply { mkdirs() }
        val shot = instrumentation.uiAutomation.takeScreenshot()
        FileOutputStream(File(dir, "$name.png")).use { shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        shot.recycle()
    }
    @Test fun legacyClimateIsStatusOnlyAndDemoDoesNotLeak() {
        show("Climate", InfotainmentFamily.LEGACY, LiveVehicle(climate = LiveClimate(target = 22.5, ac = true, fan = 3)))
        rule.onNodeWithText("22.5 °C").assertExists()
        rule.onNodeWithText("− 0.5°").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("The legacy app provides climate information.", substring = true).assertExists()
        capture("legacy-climate-synthetic")
    }
    @Test fun newerClimateNeedsReportedUnitsAndDoesNotOptimisticallyUpdate() {
        show("Climate", InfotainmentFamily.NEW, LiveVehicle(climate = LiveClimate(target = 22.5, ac = true)), true)
        rule.onNodeWithText("+ 0.5°").performScrollTo().assertIsEnabled().performClick()
        rule.onNodeWithText("22.5 °C").assertExists()
        rule.onNodeWithText("23.0 °C").assertDoesNotExist()
        capture("new-climate-synthetic")
    }
    @Test fun missingMediaAndUnknownUnitsKeepControlsDisabled() {
        show("Climate", InfotainmentFamily.NEW, LiveVehicle(climate = LiveClimate(target = 22.5)))
        rule.onNodeWithText("+ 0.5°").performScrollTo().assertIsNotEnabled()
        show("Media", InfotainmentFamily.NEW, LiveVehicle())
        rule.onNodeWithText("Play", substring = false).performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Bluetooth", substring = false).performScrollTo().assertIsNotEnabled()
        capture("media-unknown-synthetic")
    }
    @Test fun unavailableDeviceFailsWithoutInventingConnectionOrReadings() {
        rule.runOnUiThread {
            val model = ViewModelProvider(rule.activity)[OpenSenseViewModel::class.java]
            model.vehicleConnection.connect("not-a-device", InfotainmentFamily.NEW)
            check(model.vehicleConnection.state.value.link == VehicleLink.FAILED)
            check(model.vehicleConnection.state.value.vehicle == LiveVehicle())
            model.pause()
            check(model.vehicleConnection.state.value.link == VehicleLink.DISCONNECTED)
        }
    }
    @Test fun newMediaOnlyEnablesReportedSources() {
        show("Media", InfotainmentFamily.NEW, LiveVehicle(media = LiveMedia(source = 3, volume = 12, availableSources = setOf(3))))
        rule.onNodeWithText("Bluetooth", substring = false).performScrollTo().assertIsEnabled()
        rule.onNodeWithText("USB", substring = false).performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Play", substring = false).performScrollTo().assertIsEnabled().performClick()
        rule.onNodeWithText("12", substring = false).assertExists()
        capture("media-known-synthetic")
    }
    @Test fun connectionPickerShowsItsExperimentalScopeWithoutAnyPeer() {
        rule.onNodeWithContentDescription("Connection details").performClick()
        rule.onNodeWithText("Connect to BlueSense").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Refresh paired head units").performScrollTo().assertIsDisplayed()
        capture("connection-offline")
    }
}
