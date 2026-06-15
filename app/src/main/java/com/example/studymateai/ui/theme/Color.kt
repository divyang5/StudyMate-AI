package com.example.studymateai.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color



val LightBluePrimary = Color(0xFF03A9F4)
val LightBlueSecondary = Color(0xFFB3E5FC)
val LightBlueTertiary = Color(0xFFE1F5FE)

val DarkBluePrimary = Color(0xFF058FCE)
val DarkBlueSecondary = Color(0xFF80D8FF)
val DarkBlueTertiary = Color(0xFF01579B)

val StudyMateLightBlueColorScheme = lightColorScheme(
    primary = LightBluePrimary,
    onPrimary = Color.White,
    primaryContainer = LightBlueTertiary,
    onPrimaryContainer = Color(0xFF00204D),
    secondary = LightBlueSecondary
)

val StudyMateDarkBlueColorScheme = darkColorScheme(
    primary = DarkBluePrimary,
    onPrimary = Color.White,
    primaryContainer = DarkBlueTertiary,
    onPrimaryContainer = Color(0xFF00204D),
    secondary = DarkBlueSecondary
)