package com.example.habitx.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.habitx.HabitApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = (context.applicationContext as HabitApplication).repository
            val reminderManager = ReminderManager(context)
            
            scope.launch {
                val reminders = repository.getAllEnabledReminders()
                reminders.forEach { reminder ->
                    val habit = repository.getHabitById(reminder.habitId)
                    if (habit != null) {
                        reminderManager.scheduleReminder(reminder, habit.name)
                    }
                }
            }
        }
    }
}
