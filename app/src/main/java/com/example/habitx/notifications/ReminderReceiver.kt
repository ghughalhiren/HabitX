package com.example.habitx.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.habitx.HabitApplication
import com.example.habitx.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.random.Random

class ReminderReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habitId") ?: return
        val habitName = intent.getStringExtra("habitName") ?: "Habit Reminder"

        scope.launch {
            val nudges = listOf(
                "Never miss twice.",
                "One off week doesn't erase progress. Keep hustling.",
                "Champions stay consistent. Get back on it.",
                "Small steps. Big results. Resume today.",
                "Your future self is counting on you."
            )
            val body = nudges[Random.nextInt(nudges.size)]

            showNotification(context, habitId, habitName, body)
            
            // Reschedule for next week
            ReminderManager(context).rescheduleAlarm(habitId)
        }
    }

    private fun showNotification(context: Context, habitId: String, title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "habit_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Habit Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("habitId", habitId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            habitId.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(habitId.hashCode(), notification)
    }
}
