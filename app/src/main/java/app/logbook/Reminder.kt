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
import java.util.Calendar
import kotlin.random.Random

private const val CHANNEL_ID = "logbook_reminders"
private const val ACTION_NUDGE = "app.logbook.FRIENDLY_NUDGE"

fun scheduleReminder(context: Context, todo: Todo) {
    val due = todo.due ?: return
    if (todo.completed || due <= System.currentTimeMillis()) return
    val intent = Intent(context, ReminderReceiver::class.java).putExtra("text", todo.text).putExtra("id", todo.id)
    val pending = PendingIntent.getBroadcast(context, todo.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    manager.cancel(pending)
    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pending)
}

fun cancelReminder(context: Context, id: String) {
    val intent = Intent(context, ReminderReceiver::class.java).putExtra("id", id)
    val pending = PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    pending.cancel()
}

fun cancelAllReminders(context: Context, todos: List<Todo>) = todos.forEach { cancelReminder(context, it.id) }

fun scheduleFriendlyNudge(context: Context, kind: String, hour: Int, minute: Int, enabled: Boolean) {
    val id = "nudge_$kind"
    val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_NUDGE).putExtra("kind", kind).putExtra("id", id)
    val pending = PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    manager.cancel(pending)
    if (!enabled) return
    val next = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23)); set(Calendar.MINUTE, minute.coerceIn(0, 59)); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }
    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pending)
}

private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "logbook reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}

private fun nudgeText(context: Context, kind: String): String {
    val store = LocalStore(context.applicationContext)
    val open: Int
    try { open = store.todos().count { todo: Todo -> !todo.completed } } finally { store.close() }
    val prefs = context.getSharedPreferences("settings", 0)
    val pool = if (kind == "morning") {
        if (open > 0) listOf("Good morning. You have $open things in motion today.", "Morning. $open small things are waiting for you—take them one at a time.", "Good morning. Your day has $open things on the board and room to breathe.")
        else listOf("Good morning. No weights to lift today—make it a good one.", "Good morning. Your list is clear, so take the day at your own pace.", "Morning. Nothing heavy on the board today—enjoy your day.")
    } else {
        listOf("Hey, how did your day treat you?", "The day is winding down. Anything you want to remember?", "You made it through today. Want to leave a thought here?", "Anything to carry forward from today?")
    }
    val key = "last_nudge_$kind"
    var index = Random.nextInt(pool.size)
    if (pool.size > 1 && index == prefs.getInt(key, -1)) index = (index + 1) % pool.size
    prefs.edit().putInt(key, index).apply()
    return pool[index]
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ensureChannel(context)
        val isNudge = intent.action == ACTION_NUDGE
        val id = intent.getStringExtra("id") ?: "logbook"
        val text = if (isNudge) nudgeText(context, intent.getStringExtra("kind") ?: "morning") else intent.getStringExtra("text")?.take(140) ?: "A note to yourself is ready."
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, CHANNEL_ID) else Notification.Builder(context)
        val notification = builder.setSmallIcon(R.drawable.ic_logbook).setContentTitle(if (isNudge) "logbook" else "A note to yourself").setContentText(text).setAutoCancel(true).build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id.hashCode(), notification)
        if (isNudge) {
            val prefs = context.getSharedPreferences("settings", 0)
            val kind = intent.getStringExtra("kind") ?: "morning"
            val hour = prefs.getInt("${kind}_hour", if (kind == "morning") 8 else 20)
            val minute = prefs.getInt("${kind}_minute", 0)
            scheduleFriendlyNudge(context, kind, hour, minute, prefs.getBoolean("${kind}_enabled", true))
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val store = LocalStore(context.applicationContext)
            try {
                store.todos().filter { !it.completed }.forEach { scheduleReminder(context, it) }
                val prefs = context.getSharedPreferences("settings", 0)
                scheduleFriendlyNudge(context, "morning", prefs.getInt("morning_hour", 8), prefs.getInt("morning_minute", 0), prefs.getBoolean("morning_enabled", true))
                scheduleFriendlyNudge(context, "evening", prefs.getInt("evening_hour", 20), prefs.getInt("evening_minute", 0), prefs.getBoolean("evening_enabled", true))
            } catch (_: Exception) { } finally { store.close(); pendingResult.finish() }
        }
    }
}
