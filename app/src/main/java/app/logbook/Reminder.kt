package app.logbook

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

fun scheduleReminder(context: Context, todo: Todo) {
    val due = todo.due ?: return
    if (todo.completed || due <= System.currentTimeMillis()) return
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).putExtra("text", todo.text).putExtra("id", todo.id)
    val pending = PendingIntent.getBroadcast(
        context, todo.id.hashCode(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    manager.cancel(pending)
    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pending)
}

fun cancelReminder(context: Context, id: String) {
    val intent = Intent(context, ReminderReceiver::class.java).putExtra("id", id)
    val pending = PendingIntent.getBroadcast(
        context, id.hashCode(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    pending.cancel()
}

fun cancelAllReminders(context: Context, todos: List<Todo>) {
    todos.forEach { cancelReminder(context, it.id) }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel("logbook_reminders", "Logbook reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val text = intent.getStringExtra("text")?.take(140) ?: "A note to yourself is ready."
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, "logbook_reminders")
        } else {
            @Suppress("DEPRECATION") Notification.Builder(context)
        }.setSmallIcon(R.drawable.ic_logbook)
            .setContentTitle("A note to yourself")
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        manager.notify(intent.getStringExtra("id")?.hashCode() ?: 0, notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val store = LocalStore(context.applicationContext)
            try {
                store.todos().filter { !it.completed }.forEach { scheduleReminder(context, it) }
            } catch (_: Exception) {
                // A boot-time reminder recovery must never prevent the device from finishing boot.
            } finally {
                store.close()
                pendingResult.finish()
            }
        }
    }
}
