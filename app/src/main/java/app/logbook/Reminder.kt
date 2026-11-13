package app.logbook

import android.app.*
import android.content.*
import android.os.Build

fun scheduleReminder(context:Context,t:Todo){val due=t.due?:return;val mgr=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager;val intent=Intent(context,ReminderReceiver::class.java).putExtra("text",t.text).putExtra("id",t.id);val pi=PendingIntent.getBroadcast(context,t.id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);if(due>System.currentTimeMillis())mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,due,pi)}
class ReminderReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){val nm=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(NotificationChannel("logbook_reminders","Logbook reminders",NotificationManager.IMPORTANCE_DEFAULT));val n=Notification.Builder(c,"logbook_reminders").setSmallIcon(R.drawable.ic_logbook).setContentTitle("A note to yourself").setContentText(i.getStringExtra("text")?:"Your reminder is due").setAutoCancel(true).build();nm.notify(i.getStringExtra("id").hashCode(),n)}}
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){runCatching{LocalStore(c).todos().filter{!it.completed}.forEach{scheduleReminder(c,it)}}}}
