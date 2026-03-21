package com.example.habitx.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.habitx.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun AuthScreen(
    viewModel: HabitViewModel,
    onAuthSuccess: () -> Unit
) {
    var isCreateAccount by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. Background Animation: Pulsing Heatmap Grid
        BackgroundHeatmap()

        // 2. Floating Blue Glow Orb
        GlowOrb()

        // 3. Radial Fade Overlay (Draws eye left)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.8f)),
                        center = Offset(0f, 1000f),
                        radius = 2000f
                    )
                )
        )

        // Main Content Card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animating card entry
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(1000))
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = buildAnnotatedString {
                            append("Track every ")
                            withStyle(SpanStyle(color = BrightBlue)) {
                                append("streak.")
                            }
                        },
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Color.White,
                            lineHeight = 40.sp,
                            letterSpacing = (-1).sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Build consistency, one square at a time.",
                        style = MaterialTheme.typography.bodyLarge.copy(color = White55),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    // Tab Switcher (Pill)
                    AuthTabSwitcher(
                        isCreateAccount = isCreateAccount,
                        onTabSelected = { isCreateAccount = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Form Fields
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        // Sliding Name Row
                        AnimatedVisibility(
                            visible = isCreateAccount,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AuthTextField(
                                    value = firstName,
                                    onValueChange = { firstName = it; nameError = null },
                                    label = "FIRST NAME",
                                    placeholder = "John",
                                    modifier = Modifier.weight(1f),
                                    isError = nameError != null
                                )
                                AuthTextField(
                                    value = lastName,
                                    onValueChange = { lastName = it; nameError = null },
                                    label = "LAST NAME",
                                    placeholder = "Doe",
                                    modifier = Modifier.weight(1f),
                                    isError = nameError != null
                                )
                            }
                        }

                        AuthTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it; phoneError = null },
                            label = "PHONE NUMBER",
                            placeholder = "000 000 0000",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = phoneError != null,
                            errorText = phoneError
                        )

                        AuthTextField(
                            value = password,
                            onValueChange = { password = it; passwordError = null },
                            label = "PASSWORD",
                            placeholder = "••••••••",
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility",
                                        tint = White35
                                    )
                                }
                            },
                            isError = passwordError != null,
                            errorText = passwordError
                        )
                    }

                    if (generalError != null) {
                        Text(
                            text = generalError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // CTA Button with Shimmer
                    ShimmerButton(
                        text = if (isCreateAccount) "Create Account" else "Sign In",
                        isLoading = isLoading,
                        onClick = {
                            // Validation
                            var hasError = false
                            if (phoneNumber.length < 10) {
                                phoneError = "Invalid phone number"
                                hasError = true
                            }
                            if (password.length < 8) {
                                passwordError = "Min. 8 characters required"
                                hasError = true
                            }
                            if (isCreateAccount && firstName.isBlank()) {
                                nameError = "Name required"
                                hasError = true
                            }

                            if (!hasError) {
                                isLoading = true
                                generalError = null
                                viewModel.authenticate(phoneNumber, password) { result ->
                                    isLoading = false
                                    result.onSuccess { onAuthSuccess() }
                                    .onFailure { generalError = it.message ?: "Auth failed" }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BackgroundHeatmap() {
    val columns = 12
    val rows = 20
    
    Box(modifier = Modifier.fillMaxSize().alpha(0.15f)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(rows) { r ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(columns) { c ->
                        // Faint H shape logic
                        val isHShape = (c == 2 || c == 6) || (r == 10 && c in 2..6)
                        
                        PulsingCell(isHShape)
                    }
                }
            }
        }
    }
}

@Composable
fun PulsingCell(isHighlight: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val delay = remember { Random.nextInt(0, 3000) }
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = if (isHighlight) 0.6f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = delay, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isHighlight) BrightBlue.copy(alpha = alpha) else White05.copy(alpha = alpha))
    )
}

@Composable
fun GlowOrb() {
    val infiniteTransition = rememberInfiniteTransition()
    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -50f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .offset(x = (200 + xOffset).dp, y = (400 + yOffset).dp)
            .size(300.dp)
            .blur(80.dp)
            .background(GlowBlue.copy(alpha = 0.2f), CircleShape)
    )
}

@Composable
fun AuthTabSwitcher(
    isCreateAccount: Boolean,
    onTabSelected: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(White05)
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.width(240.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (!isCreateAccount) White05 else Color.Transparent)
                    .clickable { onTabSelected(false) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sign In", color = if (!isCreateAccount) Color.White else White55, style = MaterialTheme.typography.labelSmall)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (isCreateAccount) White05 else Color.Transparent)
                    .clickable { onTabSelected(true) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Create Account", color = if (isCreateAccount) Color.White else White55, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = White55
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = White35, style = MaterialTheme.typography.bodyLarge) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = InputBackground,
                unfocusedContainerColor = InputBackground,
                focusedBorderColor = if (isError) Color.Red else BrightBlue,
                unfocusedBorderColor = if (isError) Color.Red else Color.Transparent,
                focusedTextColor = InputText,
                unfocusedTextColor = InputText,
                cursorColor = BrightBlue
            ),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            trailingIcon = trailingIcon,
            singleLine = true,
            isError = isError
        )
        if (isError && errorText != null) {
            Text(
                text = errorText,
                color = Color.Red,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun ShimmerButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawWithContent {
                drawContent()
                // Shimmer sweep
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0f)
                        ),
                        start = Offset(shimmerX, 0f),
                        end = Offset(shimmerX + 200f, 200f)
                    )
                )
            },
        colors = ButtonDefaults.buttonColors(containerColor = BrightBlue),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
