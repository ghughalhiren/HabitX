package com.example.habitx.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.habitx.HabitApplication
import com.example.habitx.data.local.entity.ReminderSchedule
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar

class ReminderManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(reminder: ReminderSchedule, habitName: String) {
        if (!reminder.enabled) {
            cancelReminder(reminder.habitId)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        reminder.daysOfWeek.forEach { dayOfWeek ->
            val alarmTime = calculateNextOccurrence(dayOfWeek, reminder.timeHour, reminder.timeMinute)
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("habitId", reminder.habitId)
                putExtra("habitName", habitName)
                action = "com.example.habitx.ACTION_REMINDER_${reminder.habitId}_$dayOfWeek"
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getUniqueRequestCode(reminder.habitId, dayOfWeek),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
        }
    }

    suspend fun rescheduleAlarm(habitId: String) {
        val repository = (context.applicationContext as HabitApplication).repository
        val reminder = repository.getReminder(habitId).firstOrNull()
        if (reminder != null && reminder.enabled) {
            val habit = repository.getHabitById(habitId)
            if (habit != null) {
                scheduleReminder(reminder, habit.name)
            }
        }
    }

    fun cancelReminder(habitId: String) {
        (1..7).forEach { day ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.habitx.ACTION_REMINDER_${habitId}_$day"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                getUniqueRequestCode(habitId, day),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun calculateNextOccurrence(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            val calendarDay = when(dayOfWeek) {
                1 -> Calendar.MONDAY
                2 -> Calendar.TUESDAY
                3 -> Calendar.WEDNESDAY
                4 -> Calendar.THURSDAY
                5 -> Calendar.FRIDAY
                6 -> Calendar.SATURDAY
                7 -> Calendar.SUNDAY
                else -> Calendar.MONDAY
            }
            set(Calendar.DAY_OF_WEEK, calendarDay)
        }

        if (target.before(now)) {
            target.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun getUniqueRequestCode(habitId: String, dayOfWeek: Int): Int {
        return (habitId.hashCode() + dayOfWeek).hashCode()
    }
}
