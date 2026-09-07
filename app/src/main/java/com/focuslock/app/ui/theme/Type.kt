package com.focuslock.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif

val FocusTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold,
        fontSize = 52.sp, lineHeight = 56.sp, letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp
    )
)
