package org.opensense.app

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.opensense.app.garage.ReminderScheduler
import org.opensense.app.storage.GarageStore
import org.opensense.core.garage.*
import org.opensense.core.model.*
import java.io.File
import java.io.FileOutputStream

/** All records below are synthetic fixtures, restored after every test. No external calls/messages. */
class GarageUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private lateinit var store: GarageStore
    private lateinit var original: GarageData
    private fun model() = ViewModelProvider(rule.activity)[OpenSenseViewModel::class.java]
    @Before fun setup() {
        store = GarageStore.get(rule.activity)
        original = runBlocking { store.snapshot() }
        val now = System.currentTimeMillis()
        val trace = (0..8).map { i -> TrackPoint(.001 * i, .0005 * i + if (i > 4) .001 else 0.0, i * 10000L, 5f) }
        runBlocking { check(store.update { GarageData(
            fuel = listOf(FuelEntry("fixture-a", now - 86400000 * 4, 1000.0, 50.0, 475000, true), FuelEntry("fixture-b", now - 86400000 * 2, 1200.0, 10.0, 95000, false), FuelEntry("fixture-c", now - 86400000, 1500.0, 30.0, 285000, true)),
            reminders = listOf(Reminder(9001, "Insurance renewal", now + 604800000, 86400000)),
            trips = listOf(TripRecord("fixture-route", now - 86400000, now - 86400000 + 900000, 12400.0, 54.0, 720000, 1, trace)),
            parked = ParkedPlace(0.0, 0.0, 8f, now - 3600000, "Level 2 · Bay B14 · synthetic fixture"),
            owner = OwnerRecord(name = "Sample owner", assistanceName = "Saved assistance contact"),
            documents = listOf(GarageDocument("fixture-doc", "Sample insurance.pdf", "content://org.opensense.fixture/document", "application/pdf"))
        ) }) }
        rule.runOnUiThread { model().pause(); model().setDark(true); model().selectProfile(VehicleProfile(InfotainmentFamily.NEW, "W7")) }
        rule.waitForIdle()
    }
    @After fun restore() { if (::store.isInitialized && ::original.isInitialized) runBlocking { store.update { original } }; ReminderScheduler.cancel(rule.activity, 9001) }
    private fun open(route: String) { rule.runOnUiThread { model().routeRequest.value = route }; rule.waitForIdle() }
    @Test fun garageIsReachableAndPortraitIsRequested() {
        Assert.assertEquals(Configuration.ORIENTATION_PORTRAIT, rule.activity.resources.configuration.orientation)
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithText("Your garage").performScrollTo().performClick()
        rule.onNodeWithText("Fuel diary").assertExists()
        rule.onNodeWithText("Reminders").assertExists()
    }
    @Test fun fillUpCanBeAddedEditedAndRemovedWithoutVehicleData() {
        open("Add fuel")
        rule.onNodeWithText("Odometer (km)").performScrollTo().performTextInput("1800")
        rule.onNodeWithText("Fuel added (litres)").performScrollTo().performTextInput("25")
        rule.onNodeWithText("Total amount paid (₹)").performScrollTo().performTextInput("2400.50")
        rule.onNodeWithText("Save fill-up").performScrollTo().performClick()
        rule.waitUntil(10000) { store.data.value.fuel.size == 4 }
        val saved = store.data.value.fuel.first { it.odometerKm == 1800.0 }
        Assert.assertEquals(240050L, saved.costMinor)
        open("Fuel:${saved.id}")
        rule.onNodeWithText("Delete fill-up").performScrollTo().performClick()
        rule.onNodeWithText("Delete", substring = false).performClick()
        rule.waitUntil(10000) { store.data.value.fuel.size == 3 }
    }
    @Test fun reminderCanBeSavedAndMarkedDone() {
        open("Add reminder")
        rule.onNodeWithText("Reminder title").performScrollTo().performTextInput("Synthetic service reminder")
        rule.onNodeWithText("Save reminder").performScrollTo().performClick()
        rule.waitUntil(10000) { store.data.value.reminders.size == 2 }
        val saved = store.data.value.reminders.first { it.id != 9001 }
        // Done/cancel path is verified without posting a notification or waiting for the date.
        runBlocking { store.update { it.copy(reminders = it.reminders.map { r -> if (r.id == saved.id) r.copy(done = true) else r }) } }
        ReminderScheduler.cancel(rule.activity, saved.id)
        open("Reminders")
        rule.onNodeWithText("Completed").performScrollTo().assertExists()
    }
    @Test fun identityAndLocationAreExcludedFromStandardExport() {
        val exported = store.export()
        Assert.assertFalse(exported.contains("Sample owner"))
        Assert.assertFalse(exported.contains("Bay B14"))
        Assert.assertFalse(exported.contains("org.opensense.fixture"))
        Assert.assertTrue(exported.contains("fixture-route"))
        Assert.assertTrue(exported.contains("\"points\": []"))
    }
    @Test fun dataPagesAndEveryPrimaryActionRemainReachable() {
        listOf("Records" to "Save records", "Warning preferences" to "Save preferences", "Data & privacy" to "Export non-identity records", "Parked car" to "Update note only", "Trips" to "Start a trip").forEach { (route, action) ->
            open(route); rule.onNodeWithText(action).performScrollTo().assertIsDisplayed()
        }
        open("Trip:fixture-route"); rule.onNodeWithText("Phone GPS insights").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Trip:fixture-route").assertDoesNotExist()
        rule.onNodeWithText("Trip details").assertExists()
        open("Warning preferences")
        rule.onNodeWithContentDescription("Low-range reference").performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "50 kilometres"))
            .performSemanticsAction(SemanticsActions.SetProgress) { it(80f) }
        rule.onNodeWithContentDescription("Low-range reference").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "80 kilometres"))
        rule.onNodeWithContentDescription("GPS speed reference").performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "80 kilometres per hour"))
        open("Add reminder")
        rule.onNodeWithText("Time", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("Set time").assertIsDisplayed()
        rule.onNodeWithText("Cancel", substring = false).performClick()
        rule.onNodeWithText("Choose date").performScrollTo().performClick()
        rule.onNodeWithText("Set date").assertIsDisplayed()
        rule.onNodeWithText("Cancel", substring = false).performClick()
    }
    @Test fun capturePortraitFeatureMatrix() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "garage-ui-review").apply { mkdirs() }
        val routes = listOf("Your garage", "Fuel diary", "Add fuel", "Reminders", "Add reminder", "Trips", "Trip:fixture-route", "Parked car", "Records", "Documents", "Owner's manual", "Assistance", "Warning preferences", "Data & privacy")
        val metadata = mutableListOf("Synthetic fixture records; API ${android.os.Build.VERSION.SDK_INT}; ${rule.activity.resources.displayMetrics.widthPixels}x${rule.activity.resources.displayMetrics.heightPixels}; font=${rule.activity.resources.configuration.fontScale}")
        routes.forEachIndexed { index, route ->
            open(route)
            rule.waitForIdle(); instrumentation.waitForIdleSync()
            val file = "${index.toString().padStart(2, '0')}-${route.substringBefore(':').replace(Regex("[^A-Za-z0-9]+"), "-")}.png"
            val shot = instrumentation.uiAutomation.takeScreenshot()
            FileOutputStream(File(directory, file)).use { shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            shot.recycle(); metadata += "$file = $route"
        }
        listOf("Records" to "Save records", "Add fuel" to "Save fill-up", "Add reminder" to "Save reminder", "Warning preferences" to "Save preferences", "Data & privacy" to "Export non-identity records").forEach { (route, action) ->
            open(route); rule.onNodeWithText(action).performScrollTo().assertIsDisplayed(); rule.waitForIdle(); instrumentation.waitForIdleSync()
            val file = "bottom-${route.replace(Regex("[^A-Za-z0-9]+"), "-")}.png"
            val shot = instrumentation.uiAutomation.takeScreenshot()
            FileOutputStream(File(directory, file)).use { shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            shot.recycle(); metadata += "$file = $route primary action"
        }
        File(directory, "captures.txt").writeText(metadata.joinToString("\n"))
    }
}
