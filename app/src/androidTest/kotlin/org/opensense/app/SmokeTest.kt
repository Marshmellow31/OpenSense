package org.opensense.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Before

class SmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun chooseVehicle() {
        if (rule.onAllNodesWithText("Choose your XUV500").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText("Choose New BlueSense").performScrollTo().performClick()
            rule.onNodeWithText("W7").performScrollTo().performClick()
            rule.onNodeWithText("Continue with this vehicle").performScrollTo().performClick()
            rule.onNodeWithText("Start simulated vehicle").assertIsDisplayed()
        }
    }
    @Test fun previewStartsWithoutVehicleDataAndSimulationIsExplicit() {
        rule.onNodeWithText("RESEARCH PREVIEW · No vehicle data").assertExists()
        rule.onNodeWithText("Start simulated vehicle").performScrollTo().performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText("SIMULATED SNAPSHOT", substring = true).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("All numbers below are generated examples. Your vehicle is not connected.").assertExists()
        rule.onNodeWithText("Stop simulation").performScrollTo().performClick()
        rule.onNodeWithText("RESEARCH PREVIEW · No vehicle data").assertExists()
    }
    @Test fun connectionHasNoVehicleSocketAndFeaturesStayUnavailable() {
        rule.onNodeWithText("Connect", useUnmergedTree = true).performClick()
        rule.onNodeWithText("VEHICLE DATA CONNECTION BLOCKED").performScrollTo().assertExists()
        rule.onNodeWithText("Features", useUnmergedTree = true).performClick()
        rule.onAllNodesWithText("Unavailable · needs confirmation").assertCountEquals(7)
    }
    @Test fun canSwitchToLegacyVehicleProfile() {
        rule.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Change vehicle profile").performScrollTo().performClick()
        rule.onNodeWithText("Choose Legacy").performScrollTo().performClick()
        rule.onNodeWithText("W8").performScrollTo().performClick()
        rule.onNodeWithText("Continue with this vehicle").performScrollTo().performClick()
        rule.onNodeWithText("XUV500 W8 · Legacy").assertExists()
        rule.onNodeWithText("Start simulated vehicle").assertIsDisplayed()
        rule.onNodeWithText("RESEARCH PREVIEW · No vehicle data").assertExists()
    }
}
