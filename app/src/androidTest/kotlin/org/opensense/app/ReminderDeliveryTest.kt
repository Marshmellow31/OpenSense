package org.opensense.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.opensense.app.garage.*
import org.opensense.app.storage.GarageStore
import org.opensense.core.garage.Reminder

/** Synthetic due reminder, delivered only to this emulator's app notification tray. */
class ReminderDeliveryTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun dueReminderPostsOnceAndCanBeCancelled() {
        val context = rule.activity
        val store = GarageStore.get(context)
        val original = runBlocking { store.snapshot() }
        val reminder = Reminder(9003, "Synthetic notification test", System.currentTimeMillis() - 1000)
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
            runBlocking { store.update { it.copy(reminders = it.reminders.filterNot { r -> r.id == 9003 } + reminder) } }
            context.sendBroadcast(Intent(context, ReminderReceiver::class.java).putExtra("id", 9003))
            rule.waitUntil(15000) { store.data.value.reminders.firstOrNull { it.id == 9003 }?.notified == true }
            Assert.assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == 9003 && it.tag == "reminder" })
            ReminderScheduler.cancel(context, 9003)
            Assert.assertFalse(context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == 9003 && it.tag == "reminder" })
        } finally { ReminderScheduler.cancel(context, 9003); runBlocking { store.update { original } } }
    }
}
