package com.example.habitx.ui

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.habitx.data.local.entity.Habit
import com.example.habitx.data.local.entity.HabitEntry
import com.example.habitx.data.local.entity.User
import com.example.habitx.data.local.entity.ReminderSchedule
import com.example.habitx.data.repository.HabitRepository
import com.example.habitx.notifications.ReminderManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModel(
    application: Application,
    private val repository: HabitRepository
) : AndroidViewModel(application) {

    private val reminderManager = ReminderManager(application)
    private val prefs = application.getSharedPreferences("habitx_prefs", Context.MODE_PRIVATE)

    val loggedInUser: StateFlow<User?> = repository.getLoggedInUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedHabitId = MutableStateFlow<String?>(null)
    val selectedHabitId: StateFlow<String?> = _selectedHabitId.asStateFlow()

    val habits: StateFlow<List<Habit>> = loggedInUser
        .flatMapLatest { user ->
            user?.let { repository.getHabits(it.id) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedHabitEntries: StateFlow<List<HabitEntry>> = _selectedHabitId
        .flatMapLatest { habitId ->
            habitId?.let { repository.getEntriesForHabit(it) } ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedHabit: StateFlow<Habit?> = combine(habits, _selectedHabitId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allReminders: StateFlow<Map<String, ReminderSchedule>> = habits
        .flatMapLatest { habitList ->
            val flows = habitList.map { habit ->
                repository.getReminder(habit.id).map { habit.id to it }
            }
            if (flows.isEmpty()) flowOf(emptyMap())
            else combine(flows) { pairs ->
                pairs.filter { it.second != null }.associate { it.first to it.second!! }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Requirement: Single StateFlow for Notifications
    private val _permissionState = MutableStateFlow(NotifPermissionState.FULLY_GRANTED)
    val permissionState: StateFlow<NotifPermissionState> = _permissionState.asStateFlow()

    private val _showBatteryBanner = MutableStateFlow(!prefs.getBoolean("battery_banner_dismissed", false))
    val showBatteryBanner: StateFlow<Boolean> = _showBatteryBanner.asStateFlow()

    fun refreshPermissionState() {
        val context = getApplication<Application>()
        
        val hasPostNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        // For permanent denial detection, we'd ideally need activity context for shouldShowRequestPermissionRationale
        // But we can approximate state here or handle it in UI logic.

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        } else true

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        _permissionState.value = when {
            !hasPostNotif -> NotifPermissionState.POST_NOTIF_DENIED
            !canScheduleExact -> NotifPermissionState.EXACT_ALARM_DENIED
            !isIgnoringBattery && _showBatteryBanner.value -> NotifPermissionState.BATTERY_OPTIMIZATION_ACTIVE
            else -> NotifPermissionState.FULLY_GRANTED
        }
    }

    fun dismissBatteryBanner() {
        _showBatteryBanner.value = false
        prefs.edit().putBoolean("battery_banner_dismissed", true).apply()
        refreshPermissionState()
    }

    fun authenticate(phoneNumber: String, passwordHash: String, onResult: (Result<User>) -> Unit) {
        viewModelScope.launch {
            val result = repository.authenticate(phoneNumber, passwordHash)
            onResult(result)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _selectedHabitId.value = null
        }
    }

    fun selectHabit(habitId: String) {
        _selectedHabitId.value = habitId
    }

    fun addHabit(name: String, color: Int, weeklyFrequency: Int) {
        val userId = loggedInUser.value?.id ?: return
        viewModelScope.launch {
            repository.addHabit(userId, name, color, weeklyFrequency)
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            repository.updateHabit(habit)
        }
    }

    fun toggleCompletion(habitId: String, date: LocalDate) {
        viewModelScope.launch {
            repository.toggleCompletion(habitId, date)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            if (_selectedHabitId.value == habit.id) {
                _selectedHabitId.value = null
            }
        }
    }

    fun saveReminder(habitId: String, enabled: Boolean, daysOfWeek: List<Int>, hour: Int, minute: Int) {
        viewModelScope.launch {
            val reminder = ReminderSchedule(
                habitId = habitId,
                enabled = enabled,
                daysOfWeek = daysOfWeek,
                timeHour = hour,
                timeMinute = minute,
                updatedAt = Clock.System.now()
            )
            repository.saveReminder(reminder)
            val habit = repository.getHabitById(habitId)
            if (habit != null) {
                if (enabled) {
                    reminderManager.scheduleReminder(reminder, habit.name)
                } else {
                    reminderManager.cancelReminder(habitId)
                }
            }
        }
    }

    // Stats calculations
    val currentStreak: StateFlow<Int> = selectedHabitEntries.map { entries ->
        calculateCurrentStreak(entries.map { it.date })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bestStreak: StateFlow<Int> = selectedHabitEntries.map { entries ->
        calculateBestStreak(entries.map { it.date })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCompletions: StateFlow<Int> = selectedHabitEntries.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completionRate: StateFlow<Float> = selectedHabitEntries.map { entries ->
        if (entries.isEmpty()) 0f else {
            val daysTracked = 365
            entries.size.toFloat() / daysTracked
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val perfectWeeks: StateFlow<Int> = combine(selectedHabit, selectedHabitEntries) { habit, entries ->
        if (habit == null || entries.isEmpty()) return@combine 0
        calculatePerfectWeeks(entries.map { it.date }, habit.weeklyFrequency)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weekRate: StateFlow<String> = combine(selectedHabit, selectedHabitEntries) { habit, entries ->
        if (habit == null) return@combine "0 / 0 weeks"
        calculateWeekRate(entries.map { it.date }, habit.weeklyFrequency)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0 / 0 weeks")

    private val motivationalNudges = listOf(
        "Never miss twice.",
        "One off week doesn't erase progress. Keep hustling.",
        "Champions stay consistent. Get back on it.",
        "Small steps. Big results. Resume today.",
        "Your future self is counting on you."
    )
    private var lastNudgeIndex = -1

    val motivationalSubtitle: StateFlow<String> = combine(selectedHabit, selectedHabitEntries, currentStreak, perfectWeeks) { habit, entries, streak, perfectWks ->
        if (habit == null) return@combine ""
        val dates = entries.map { it.date }.toSet()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        if (entries.isEmpty()) {
            return@combine if (habit.weeklyFrequency == 7) "Start doing ${habit.name} every day"
            else "Start doing ${habit.name} ${habit.weeklyFrequency} times a week"
        }
        val lastMonday = today.minus(today.dayOfWeek.ordinal.toLong() + 7, DateTimeUnit.DAY)
        val completionsLastWeek = (0..6).count { dates.contains(lastMonday.plus(it, DateTimeUnit.DAY)) }
        if (completionsLastWeek < habit.weeklyFrequency && entries.isNotEmpty()) {
            lastNudgeIndex = if (lastNudgeIndex == -1) Random.nextInt(motivationalNudges.size) 
                             else (lastNudgeIndex + 1) % motivationalNudges.size
            return@combine motivationalNudges[lastNudgeIndex]
        }
        val habitStart = entries.minOf { it.createdAt }.toLocalDateTime(TimeZone.currentSystemDefault()).date
        if (today.minus(7, DateTimeUnit.DAY) < habitStart) {
            return@combine "Start doing ${habit.name} ${if(habit.weeklyFrequency == 7) "every day" else "${habit.weeklyFrequency} times a week"} — you've got this"
        }
        if (habit.weeklyFrequency == 7) {
            if (streak > 0) return@combine "Keep doing ${habit.name} — $streak day streak going"
        } else {
            if (perfectWks > 0) return@combine "Keep doing ${habit.name} — $perfectWks perfect week(s) and counting"
        }
        "Completed ${entries.count { it.date.month == today.month && it.date.year == today.year }} days this month"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private fun calculateCurrentStreak(dates: List<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        val sortedDates = dates.distinct().sortedDescending()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        var streak = 0
        var currentDate = if (sortedDates.first() == today) today else if (sortedDates.first() == yesterday) yesterday else return 0
        for (date in sortedDates) {
            if (date == currentDate) {
                streak++
                currentDate = currentDate.minus(1, DateTimeUnit.DAY)
            } else break
        }
        return streak
    }

    private fun calculateBestStreak(dates: List<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        val sortedDates = dates.distinct().sorted()
        var best = 0
        var current = 0
        var lastDate: LocalDate? = null
        for (date in sortedDates) {
            if (lastDate == null || date == lastDate.plus(1, DateTimeUnit.DAY)) {
                current++
            } else {
                best = maxOf(best, current)
                current = 1
            }
            lastDate = date
        }
        return maxOf(best, current)
    }

    private fun calculatePerfectWeeks(dates: List<LocalDate>, target: Int): Int {
        if (dates.isEmpty()) return 0
        val dateSet = dates.toSet()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        var currentMonday = today.minus(today.dayOfWeek.ordinal.toLong(), DateTimeUnit.DAY)
        var perfectWks = 0
        val completionsThisWeek = (0..6).count { dateSet.contains(currentMonday.plus(it, DateTimeUnit.DAY)) }
        if (completionsThisWeek >= target) perfectWks++
        while (true) {
            currentMonday = currentMonday.minus(7, DateTimeUnit.DAY)
            val completions = (0..6).count { dateSet.contains(currentMonday.plus(it, DateTimeUnit.DAY)) }
            if (completions >= target) perfectWks++ else break
        }
        return perfectWks
    }

    private fun calculateWeekRate(dates: List<LocalDate>, target: Int): String {
        if (dates.isEmpty()) return "0 / 0 weeks"
        val dateSet = dates.toSet()
        val sortedDates = dates.sorted()
        val firstDate = sortedDates.first()
        val firstMonday = firstDate.minus(firstDate.dayOfWeek.ordinal.toLong(), DateTimeUnit.DAY)
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        var totalWeeks = 0
        var completedWeeks = 0
        var currentMonday = firstMonday
        while (currentMonday <= today) {
            totalWeeks++
            val completions = (0..6).count { dateSet.contains(currentMonday.plus(it, DateTimeUnit.DAY)) }
            if (completions >= target) completedWeeks++
            currentMonday = currentMonday.plus(7, DateTimeUnit.DAY)
        }
        return "$completedWeeks / $totalWeeks weeks"
    }
}

class HabitViewModelFactory(
    private val application: Application,
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
