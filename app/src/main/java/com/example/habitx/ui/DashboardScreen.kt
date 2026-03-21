package com.example.habitx.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.habitx.data.local.entity.Habit
import com.example.habitx.data.local.entity.ReminderSchedule
import com.example.habitx.ui.components.Heatmap
import com.example.habitx.ui.components.EmptyState
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HabitViewModel,
    onSignOut: () -> Unit
) {
    val habits by viewModel.habits.collectAsState()
    val selectedHabitId by viewModel.selectedHabitId.collectAsState()
    val selectedHabit by viewModel.selectedHabit.collectAsState()
    val entries by viewModel.selectedHabitEntries.collectAsState()
    val allReminders by viewModel.allReminders.collectAsState()
    
    val currentStreak by viewModel.currentStreak.collectAsState()
    val bestStreak by viewModel.bestStreak.collectAsState()
    val totalCompletions by viewModel.totalCompletions.collectAsState()
    val completionRate by viewModel.completionRate.collectAsState()
    val perfectWeeks by viewModel.perfectWeeks.collectAsState()
    val weekRate by viewModel.weekRate.collectAsState()
    val motivationalSubtitle by viewModel.motivationalSubtitle.collectAsState()

    // Requirement: Centralized Permission State
    val permissionState by viewModel.permissionState.collectAsState()
    val showBatteryBanner by viewModel.showBatteryBanner.collectAsState()

    var showAddHabitDialog by remember { mutableStateOf(false) }
    var showEditHabitDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
    
    var showReminderSheet by remember { mutableStateOf(false) }
    var habitForReminder by remember { mutableStateOf<Habit?>(null) }
    
    var habitWithActionsVisible by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HabitX", style = MaterialTheme.typography.headlineSmall)
                    
                    var showOverflow by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showOverflow = false
                                    scope.launch { 
                                        drawerState.close() 
                                        onSignOut()
                                    }
                                }
                            )
                        }
                    }
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Habit Heatmap") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddHabitDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit")
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { habitWithActionsVisible = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Requirement: Centralized Battery Optimization Banner
                    if (showBatteryBanner && permissionState == NotifPermissionState.BATTERY_OPTIMIZATION_ACTIVE) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.BatteryAlert, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Battery optimization may delay reminders.", style = MaterialTheme.typography.labelMedium)
                                    TextButton(onClick = {
                                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }, contentPadding = PaddingValues(0.dp)) {
                                        Text("Fix it", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                IconButton(onClick = { viewModel.dismissBatteryBanner() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                                }
                            }
                        }
                    }

                    selectedHabit?.let { habit ->
                        HabitDetail(
                            habit = habit,
                            entries = entries,
                            currentStreak = currentStreak,
                            bestStreak = bestStreak,
                            totalCompletions = totalCompletions,
                            completionRate = completionRate,
                            perfectWeeks = perfectWeeks,
                            weekRate = weekRate,
                            motivationalSubtitle = motivationalSubtitle,
                            onToggleDate = { viewModel.toggleCompletion(habit.id, it) }
                        )
                    } ?: Box(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Select a habit from the list below", style = MaterialTheme.typography.bodyLarge)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Your Habits",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    
                    if (habits.isEmpty()) {
                        EmptyState(onAddHabit = { showAddHabitDialog = true })
                    } else {
                        HabitList(
                            habits = habits,
                            selectedHabitId = selectedHabitId,
                            allReminders = allReminders,
                            habitWithActionsVisible = habitWithActionsVisible,
                            onHabitClick = { 
                                habitWithActionsVisible = null
                                viewModel.selectHabit(it.id) 
                            },
                            onLongClickHabit = { habitWithActionsVisible = it.id },
                            onEditHabit = { 
                                habitToEdit = it
                                showEditHabitDialog = true 
                                habitWithActionsVisible = null
                            },
                            onDeleteHabit = { 
                                habitToDelete = it
                                showDeleteConfirmDialog = true 
                                habitWithActionsVisible = null
                            },
                            onSetReminder = {
                                habitForReminder = it
                                showReminderSheet = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    RemindersSection(
                        habits = habits,
                        allReminders = allReminders,
                        permissionState = permissionState,
                        onReminderClick = { habit ->
                            habitForReminder = habit
                            showReminderSheet = true
                        }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showAddHabitDialog) {
        HabitDialog(
            title = "New Habit",
            onDismiss = { showAddHabitDialog = false },
            onConfirm = { name, color, freq ->
                viewModel.addHabit(name, color, freq)
                showAddHabitDialog = false
            }
        )
    }

    if (showEditHabitDialog && habitToEdit != null) {
        HabitDialog(
            title = "Edit Habit",
            initialName = habitToEdit!!.name,
            initialColor = habitToEdit!!.color,
            initialFrequency = habitToEdit!!.weeklyFrequency,
            onDismiss = { 
                showEditHabitDialog = false
                habitToEdit = null
            },
            onConfirm = { name, color, freq ->
                viewModel.updateHabit(habitToEdit!!.copy(name = name, color = color, weeklyFrequency = freq))
                showEditHabitDialog = false
                habitToEdit = null
            }
        )
    }

    if (showDeleteConfirmDialog && habitToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmDialog = false
                habitToDelete = null
            },
            title = { Text("Delete Habit") },
            text = { Text("Are you sure you want to delete '${habitToDelete!!.name}'? This will also remove all its history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHabit(habitToDelete!!)
                        showDeleteConfirmDialog = false
                        habitToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirmDialog = false
                    habitToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReminderSheet && habitForReminder != null) {
        val existingReminder = allReminders[habitForReminder!!.id]
        ReminderBottomSheet(
            habit = habitForReminder!!,
            existingReminder = existingReminder,
            onDismiss = { showReminderSheet = false },
            onSave = { enabled, days, hour, minute ->
                viewModel.saveReminder(habitForReminder!!.id, enabled, days, hour, minute)
                showReminderSheet = false
            }
        )
    }
}

@Composable
fun RemindersSection(
    habits: List<Habit>,
    allReminders: Map<String, ReminderSchedule>,
    permissionState: NotifPermissionState,
    onReminderClick: (Habit) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("Reminders", style = MaterialTheme.typography.titleMedium)
        
        PermissionStatusRow(permissionState)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        habits.forEach { habit ->
            val reminder = allReminders[habit.id]
            if (reminder != null && reminder.enabled) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onReminderClick(habit) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(habit.color), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(habit.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", reminder.timeHour, reminder.timeMinute)
                            val daysStr = reminder.daysOfWeek.joinToString(", ") { day ->
                                when(day) {
                                    1 -> "Mon"
                                    2 -> "Tue"
                                    3 -> "Wed"
                                    4 -> "Thu"
                                    5 -> "Fri"
                                    6 -> "Sat"
                                    7 -> "Sun"
                                    else -> ""
                                }
                            }
                            Text("$timeStr on $daysStr", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionStatusRow(permissionState: NotifPermissionState) {
    val context = LocalContext.current
    val allGranted = permissionState == NotifPermissionState.FULLY_GRANTED

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (allGranted) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("All systems go", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
        } else {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = {
                    when (permissionState) {
                        NotifPermissionState.POST_NOTIF_DENIED, NotifPermissionState.POST_NOTIF_PERMANENTLY_DENIED -> {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                        NotifPermissionState.EXACT_ALARM_DENIED -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        }
                        NotifPermissionState.BATTERY_OPTIMIZATION_ACTIVE -> {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                        else -> {}
                    }
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Fix permissions", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC107))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderBottomSheet(
    habit: Habit,
    existingReminder: ReminderSchedule?,
    onDismiss: () -> Unit,
    onSave: (Boolean, List<Int>, Int, Int) -> Unit
) {
    var enabled by remember { mutableStateOf(existingReminder?.enabled ?: true) }
    var selectedDays by remember { mutableStateOf(existingReminder?.daysOfWeek?.toSet() ?: emptySet<Int>()) }
    val initialHour = existingReminder?.timeHour ?: 9
    val initialMinute = existingReminder?.timeMinute ?: 0
    
    val context = LocalContext.current
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val timePickerState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Set Reminder for ${habit.name}", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled")
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Days of week", style = MaterialTheme.typography.labelMedium)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1, 2, 3, 4, 5, 6, 7).forEach { day ->
                    val label = when(day) {
                        1 -> "M"
                        2 -> "T"
                        3 -> "W"
                        4 -> "T"
                        5 -> "F"
                        6 -> "S"
                        7 -> "S"
                        else -> ""
                    }
                    FilterChip(
                        selected = selectedDays.contains(day),
                        onClick = {
                            selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                        },
                        label = { Text(label) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Time", style = MaterialTheme.typography.labelMedium)
            
            TimePicker(state = timePickerState, modifier = Modifier.align(Alignment.CenterHorizontally))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onSave(enabled, selectedDays.toList().sorted(), timePickerState.hour, timePickerState.minute)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedDays.isNotEmpty() || !enabled
            ) {
                Text("Save Reminder")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitList(
    habits: List<Habit>,
    selectedHabitId: String?,
    allReminders: Map<String, ReminderSchedule>,
    habitWithActionsVisible: String?,
    onHabitClick: (Habit) -> Unit,
    onLongClickHabit: (Habit) -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onSetReminder: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        habits.forEach { habit ->
            val isSelected = habit.id == selectedHabitId
            val showActions = habitWithActionsVisible == habit.id
            val hasReminder = allReminders[habit.id]?.enabled == true

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onHabitClick(habit) },
                        onLongClick = { onLongClickHabit(habit) }
                    ),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                tonalElevation = if (isSelected) 4.dp else 0.dp,
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(habit.color), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(onClick = { onSetReminder(habit) }) {
                        Icon(
                            if (hasReminder) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                            contentDescription = "Reminder",
                            tint = if (hasReminder) Color(habit.color) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showActions) {
                        Row {
                            IconButton(onClick = { onSetReminder(habit) }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Set Reminder", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { onEditHabit(habit) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { onDeleteHabit(habit) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun HabitDetail(
    habit: Habit,
    entries: List<com.example.habitx.data.local.entity.HabitEntry>,
    currentStreak: Int,
    bestStreak: Int,
    totalCompletions: Int,
    completionRate: Float,
    perfectWeeks: Int,
    weekRate: String,
    motivationalSubtitle: String,
    onToggleDate: (kotlinx.datetime.LocalDate) -> Unit
) {
    val habitColor = Color(habit.color)
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val isCompletedToday = entries.any { it.date == today }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = habit.name, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = motivationalSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (habit.weeklyFrequency == 7) {
                StatCard("Streak", "$currentStreak", modifier = Modifier.weight(1f))
                StatCard("Best", "$bestStreak", modifier = Modifier.weight(1f))
                StatCard("Total", "$totalCompletions", modifier = Modifier.weight(1f))
                StatCard("Rate", "${(completionRate * 100).toInt()}%", modifier = Modifier.weight(1f))
            } else {
                StatCard("Perfect Wks", "$perfectWeeks", modifier = Modifier.weight(1f))
                StatCard("Max Days", "$bestStreak", modifier = Modifier.weight(1f))
                StatCard("Total", "$totalCompletions", modifier = Modifier.weight(1f))
                StatCard("Week Rate", weekRate, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Heatmap(
            entries = entries,
            habitColor = habitColor,
            targetFrequency = habit.weeklyFrequency,
            onDateClick = onToggleDate,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onToggleDate(today) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = habitColor)
        ) {
            Text(if (isCompletedToday) "Mark Incomplete for Today" else "Complete Today")
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 1, textAlign = TextAlign.Center)
            Text(text = value, style = MaterialTheme.typography.titleMedium, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDialog(
    title: String,
    initialName: String = "",
    initialColor: Int? = null,
    initialFrequency: Int = 7,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val colors = listOf(
        0xFF4CAF50.toInt(),
        0xFF2196F3.toInt(),
        0xFFFFC107.toInt(),
        0xFFE91E63.toInt(),
        0xFF9C27B0.toInt()
    )
    var selectedColor by remember { mutableIntStateOf(initialColor ?: colors[0]) }
    val frequencies = listOf(3, 4, 5, 6, 7)
    var selectedFrequency by remember { mutableIntStateOf(initialFrequency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("How many times a week?", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    frequencies.forEach { freq ->
                        val label = if (freq == 7) "Everyday" else "${freq}x"
                        FilterChip(
                            selected = selectedFrequency == freq,
                            onClick = { selectedFrequency = freq },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Choose a color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { colorInt ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(colorInt), RoundedCornerShape(4.dp))
                                .clickable { selectedColor = colorInt }
                                .then(
                                    if (selectedColor == colorInt) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                    else Modifier
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor, selectedFrequency) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
