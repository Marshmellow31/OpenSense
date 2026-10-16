package org.opensense.app.garage

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.*
import org.opensense.app.MainActivity
import org.opensense.app.R
import org.opensense.app.storage.GarageStore
import org.opensense.core.garage.Reminder

object ReminderScheduler {
    private const val CHANNEL = "garage-reminders"
    fun allowed(context: Context) = (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) && context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    private fun pending(context: Context, id: Int) = PendingIntent.getBroadcast(context, id,
        Intent(context, ReminderReceiver::class.java).putExtra("id", id), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun schedule(context: Context, reminder: Reminder) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        alarm.cancel(pending(context, reminder.id))
        if (!reminder.done && !reminder.notified) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
            maxOf(System.currentTimeMillis() + 1000, reminder.alertAt), pending(context, reminder.id))
    }
    fun cancel(context: Context, id: Int) { context.getSystemService(AlarmManager::class.java).cancel(pending(context, id)); context.getSystemService(NotificationManager::class.java).cancel("reminder", id) }
    fun post(context: Context, reminder: Reminder): Boolean {
        if (!allowed(context)) return false
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Garage reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, reminder.id, Intent(context, MainActivity::class.java).putExtra("route", "Reminders"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val public = Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_opensense).setContentTitle("OpenSense reminder").build()
        val notification = Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_opensense)
            .setContentTitle(reminder.title).setContentText("Your garage reminder is due. Tap to review.").setContentIntent(open)
            .setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(public).setAutoCancel(true).build()
        try { manager.notify("reminder", reminder.id, notification) } catch (_: SecurityException) { return false }
        return true
    }
}
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val store = GarageStore.get(context)
                val records = store.snapshot().reminders
                if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) records.forEach { ReminderScheduler.schedule(context, it) }
                else records.firstOrNull { it.id == intent.getIntExtra("id", -1) && !it.done && !it.notified && it.alertAt <= System.currentTimeMillis() + 1000 }?.let { reminder ->
                    if (ReminderScheduler.post(context, reminder)) store.update { data -> data.copy(reminders = data.reminders.map { if (it.id == reminder.id) it.copy(notified = true) else it }) }
                }
            } finally { result.finish() }
        }
    }
}
