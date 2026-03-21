package com.example.habitx.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.habitx.R

// Assuming fonts are added to res/font
// If not, it will fallback to system fonts but code is ready for Syne and DM Sans
val Syne = FontFamily(
    Font(resId = 0 /* R.font.syne_bold */, weight = FontWeight.Bold),
    Font(resId = 0 /* R.font.syne_extra_bold */, weight = FontWeight.ExtraBold)
)

val DMSans = FontFamily(
    Font(resId = 0 /* R.font.dm_sans_regular */, weight = FontWeight.Normal),
    Font(resId = 0 /* R.font.dm_sans_medium */, weight = FontWeight.Medium)
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default, // Syne if available
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default, // Syne if available
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, // DM Sans if available
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default, // DM Sans if available
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
