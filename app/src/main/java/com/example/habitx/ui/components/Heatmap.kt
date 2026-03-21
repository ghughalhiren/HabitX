package com.example.habitx.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habitx.data.local.entity.HabitEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Heatmap(
    entries: List<HabitEntry>,
    habitColor: Color,
    targetFrequency: Int,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditMode by remember { mutableStateOf(false) }
    var lastInteractedDate by remember { mutableStateOf<LocalDate?>(null) }
    
    LaunchedEffect(entries) {
        lastInteractedDate = null
    }
    
    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            lastInteractedDate = null
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    
    val fullYearStart = today.minus(364, DateTimeUnit.DAY)
    var filterStartDate by remember { mutableStateOf<LocalDate?>(null) }
    var filterEndDate by remember { mutableStateOf<LocalDate?>(null) }

    val activeStartDate = filterStartDate ?: fullYearStart
    val activeEndDate = filterEndDate ?: today

    val alignedStartDate = remember(activeStartDate) {
        var date = activeStartDate
        while (date.dayOfWeek != DayOfWeek.MONDAY) {
            date = date.minus(1, DateTimeUnit.DAY)
        }
        date
    }

    val completedDates = entries.associateBy { it.date }
    
    val daysInRange = alignedStartDate.daysUntil(activeEndDate) + 1
    val allDays = (0 until daysInRange).map { alignedStartDate.plus(it, DateTimeUnit.DAY) }
    val weeks = allDays.chunked(7)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    var isScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isScrolling = true
        } else {
            delay(1500)
            isScrolling = false
        }
    }

    LaunchedEffect(weeks.size) {
        if (weeks.isNotEmpty()) {
            listState.scrollToItem(weeks.size - 1)
        }
    }

    val totalDaysSelected = activeStartDate.daysUntil(activeEndDate)
    val cellSize = when {
        totalDaysSelected <= 31 -> 32.dp
        totalDaysSelected <= 92 -> 20.dp
        else -> 14.dp
    }
    val cellSpacing = if (totalDaysSelected <= 31) 8.dp else 4.dp

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = if (filterStartDate == null) "365 Days" else "Filtered Range",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                AnimatedVisibility(
                    visible = filterStartDate != null,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    AssistChip(
                        onClick = { },
                        label = {
                            val startStr = filterStartDate?.let { "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth}" }
                            val endStr = filterEndDate?.let { "${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${it.dayOfMonth}" }
                            Text("$startStr – $endStr", style = MaterialTheme.typography.labelSmall)
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear filter",
                                modifier = Modifier.size(14.dp).clickable {
                                    filterStartDate = null
                                    filterEndDate = null
                                }
                            )
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            Row {
                IconButton(
                    onClick = { showDatePicker = true },
                    colors = if (filterStartDate != null) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Filter Range", modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { isEditMode = !isEditMode },
                    colors = if (isEditMode) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Toggle Edit Mode", modifier = Modifier.size(20.dp))
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.CenterStart) {
            lastInteractedDate?.let { date ->
                val isCompleted = completedDates.containsKey(date)
                val dateFormatted = "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}"
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "$dateFormatted: ${if (isCompleted) "Checked" else "Unchecked"}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(cellSpacing),
                modifier = Modifier.padding(end = 8.dp).padding(top = 20.dp)
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                    Text(
                        text = label, 
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (totalDaysSelected <= 31) 12.sp else 9.sp), 
                        modifier = Modifier.height(cellSize)
                    )
                }
            }

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(cellSpacing)
            ) {
                itemsIndexed(weeks) { _, week ->
                    val completionsInWeek = week.count { completedDates.containsKey(it) }
                    
                    val proportionalTarget = (targetFrequency * (week.size / 7.0))
                    val diff = proportionalTarget - completionsInWeek
                    
                    val weekIntensityAlpha = when {
                        diff <= 0 -> 1f
                        diff <= 1 -> 0.6f
                        diff <= 2 -> 0.3f
                        else -> 0.05f
                    }
                    
                    val firstDayOfWeek = week.first()
                    val showMonthLabel = firstDayOfWeek.dayOfMonth <= 7
                    
                    Column(horizontalAlignment = Alignment.Start) {
                        Box(modifier = Modifier.height(16.dp).fillMaxWidth()) {
                            if (showMonthLabel) {
                                Text(
                                    text = firstDayOfWeek.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(cellSpacing),
                            modifier = Modifier
                                .background(habitColor.copy(alpha = weekIntensityAlpha * 0.15f), RoundedCornerShape(4.dp))
                                .padding(2.dp)
                        ) {
                            week.forEach { date ->
                                val isCompleted = completedDates.containsKey(date)
                                val isToday = date == today
                                val isOutRange = date > activeEndDate
                                
                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .background(
                                            color = when {
                                                isOutRange -> Color.Transparent
                                                isCompleted -> habitColor.copy(alpha = weekIntensityAlpha.coerceAtLeast(0.4f))
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(if (totalDaysSelected <= 31) 4.dp else 2.dp)
                                        )
                                        .then(
                                            if (isToday && !isOutRange) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(if (totalDaysSelected <= 31) 4.dp else 2.dp))
                                            else Modifier
                                        )
                                        .clickable(enabled = !isOutRange) {
                                            lastInteractedDate = date
                                            if (isEditMode) {
                                                onDateClick(date)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isEditMode && !isOutRange) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = if (totalDaysSelected <= 31) 11.sp else 8.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isCompleted) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (week.size < 7) {
                                repeat(7 - week.size) {
                                    Spacer(modifier = Modifier.size(cellSize))
                                }
                            }
                        }
                    }
                }
            }
        }

        val scrollAlpha by animateFloatAsState(
            targetValue = if (isScrolling) 1f else 0f,
            animationSpec = tween(durationMillis = 500)
        )
        
        var sliderWidthPx by remember { mutableIntStateOf(0) }
        
        if (weeks.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 24.dp, end = 8.dp)
                    .height(12.dp)
                    .alpha(scrollAlpha)
                    .onGloballyPositioned { sliderWidthPx = it.size.width }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            if (sliderWidthPx > 0) {
                                val scrollDelta = (delta / sliderWidthPx) * weeks.size
                                coroutineScope.launch {
                                    listState.scrollToItem(
                                        (listState.firstVisibleItemIndex + scrollDelta.toInt()).coerceIn(0, weeks.size - 1)
                                    )
                                }
                            }
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                )
                
                val scrollOffset by remember {
                    derivedStateOf {
                        if (weeks.isNotEmpty()) {
                            listState.firstVisibleItemIndex.toFloat() / weeks.size.toFloat()
                        } else 0f
                    }
                }
                
                val indicatorWidthFraction = 0.15f
                val thumbOffset = with(density) {
                    (scrollOffset * sliderWidthPx * (1f - indicatorWidthFraction)).toDp()
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(indicatorWidthFraction)
                        .height(6.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = thumbOffset)
                        .background(habitColor, CircleShape)
                )
            }
        }

        if (isEditMode) {
            Text(
                text = "Edit Mode Active: Tap boxes to toggle",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    filterStartDate = datePickerState.selectedStartDateMillis?.let {
                        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                    }
                    filterEndDate = datePickerState.selectedEndDateMillis?.let {
                        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                    }
                    showDatePicker = false
                }) {
                    Text("Apply Filter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DateRangePicker(
                state = datePickerState,
                title = { Text("Filter Heatmap Range", modifier = Modifier.padding(16.dp)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
