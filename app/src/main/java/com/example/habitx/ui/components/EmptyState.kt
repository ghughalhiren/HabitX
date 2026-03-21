package com.example.habitx.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habitx.ui.theme.White55

@Composable
fun EmptyState(onAddHabit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(initialOffsetY = { 20 })
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Track your consistency with a visual heatmap.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = "No habits yet. Add one to start building your streak.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = White55),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                Button(
                    onClick = onAddHabit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Add Your First Habit", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
