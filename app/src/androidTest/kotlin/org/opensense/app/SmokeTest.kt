package org.opensense.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun resetSession() {
        rule.runOnUiThread { ViewModelProvider(rule.activity)[OpenSenseViewModel::class.java].pause() }
    }
    private fun settings() = rule.onNodeWithContentDescription("Settings").performClick()
    private fun back() = rule.onNodeWithContentDescription("Back").performClick()
    private fun demo() {
        settings()
        rule.onNodeWithContentDescription("Simulation mode").performScrollTo().performClick()
        back()
        rule.waitUntil(10000) { rule.onAllNodesWithText("Demo", substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun homeOpensDirectlyAndConfigurationLivesInSettings() {
        rule.onNodeWithText("My XUV500").assertIsDisplayed()
        rule.onNodeWithText("Choose your XUV500").assertDoesNotExist()
        rule.onNodeWithContentDescription("Simulation mode").assertDoesNotExist()
        rule.onNodeWithTag("nav-Climate").assertExists()
        settings()
        rule.onNodeWithContentDescription("Simulation mode").assertExists()
        rule.onNodeWithText("Your vehicle").assertExists()
    }

    @Test fun simulationCanOnlyBeStartedInSettingsAndClearsOnStop() {
        demo()
        rule.onNodeWithText("412").assertExists()
        rule.onNodeWithText("Simulated data · no vehicle connected").assertExists()
        settings()
        rule.onNodeWithContentDescription("Simulation mode").performScrollTo().performClick()
        back()
        rule.onNodeWithText("Offline", substring = true).assertExists()
        rule.onNodeWithText("412").assertDoesNotExist()
    }

    @Test fun climateAndMediaRespondOnlyInDemo() {
        rule.onNodeWithTag("nav-Climate").performClick()
        rule.onNodeWithContentDescription("Increase temperature").assertIsNotEnabled()
        demo()
        rule.onNodeWithContentDescription("Increase temperature").performScrollTo().performClick()
        rule.onNodeWithTag("target-temperature").assertTextEquals("23°")
        rule.onNodeWithTag("nav-Media").performClick()
        rule.onNodeWithContentDescription("Play demo playback").performScrollTo().performClick()
        rule.onNodeWithText("Simulated playback · Playing").assertExists()
        rule.onNodeWithContentDescription("Next demo track").performClick()
        rule.onNodeWithText("Late afternoon").assertExists()
    }

    @Test fun vehicleProfileCanBeSelectedInSettings() {
        settings()
        rule.onNodeWithText("Vehicle model & infotainment generation").performScrollTo().performClick()
        rule.onNodeWithText("Legacy BlueSense").performScrollTo().performClick()
        rule.onNodeWithText("W8").performScrollTo().performClick()
        rule.onNodeWithText("Save vehicle").performScrollTo().performClick()
        rule.onNodeWithText("XUV500 W8 · Legacy").assertExists()
        back()
        rule.onNodeWithText("My XUV500").assertIsDisplayed()
    }

    @Test fun unknownTyresAndConnectionStayHonest() {
        rule.onNodeWithText("Tyre pressure").performScrollTo().performClick()
        rule.onNodeWithText("No verified tyre pressure readings. Unknown does not mean normal or safe.").assertExists()
        back()
        rule.onNodeWithContentDescription("Connection details").performClick()
        rule.onNodeWithText("Connect to BlueSense").performScrollTo().assertExists()
        rule.onNodeWithText("Experimental compatibility build.", substring = true).assertExists()
        rule.onNodeWithText("Refresh paired head units").performScrollTo().assertIsDisplayed()
    }
}
