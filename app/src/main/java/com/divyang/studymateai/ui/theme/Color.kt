package com.divyang.studymateai.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

import androidx.compose.ui.graphics.Color

//
//val LightBluePrimary = Color(0xFF03A9F4)
//val LightBlueSecondary = Color(0xFFB3E5FC)
//val LightBlueTertiary = Color(0xFFE1F5FE)
//
//val DarkBluePrimary = Color(0xFF058FCE)
//val DarkBlueSecondary = Color(0xFF80D8FF)
//val DarkBlueTertiary = Color(0xFF01579B)
//
//val LightBackground = Color(0xFFF2F2F7)
//val DarkBackground = Color(0xFF1C1C1E)
//
//val StudyMateLightBlueColorScheme = lightColorScheme(
//    primary = LightBluePrimary,
//    onPrimary = Color.White,
//    primaryContainer = LightBlueTertiary,
//    onPrimaryContainer = Color(0xFF00204D),
//    secondary = LightBlueSecondary,
//    background = LightBackground
//)
//
//val StudyMateDarkBlueColorScheme = darkColorScheme(
//    primary = DarkBluePrimary,
//    onPrimary = Color.White,
//    primaryContainer = DarkBlueTertiary,
//    onPrimaryContainer = Color(0xFF00204D),
//    secondary = DarkBlueSecondary,
//    background = DarkBackground
//)





// ── Brand ──────────────────────────────────────────────────────
val PrimaryPurple       = Color(0xFF534AB7)   // deep violet — primary actions
val PrimaryPurpleLight  = Color(0xFF7F77DD)   // lighter variant for light mode
val AccentCyan          = Color(0xFF06B6D4)   // cyan — AI/highlight accent
val AccentCyanLight     = Color(0xFFCCF3FA)   // cyan tint for containers

// ── Surfaces ───────────────────────────────────────────────────
val LightBackground     = Color(0xFFF4F4F8)   // very slightly purple-tinted white
val LightSurface        = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEEDFE)   // purple-tinted card bg

val DarkBackground      = Color(0xFF131316)   // near-black with purple undertone
val DarkSurface         = Color(0xFF1E1E24)   // elevated surface
val DarkSurfaceVariant  = Color(0xFF2A2A33)   // card bg

// ── On-colors ──────────────────────────────────────────────────
val OnPrimary           = Color.White
val OnPrimaryContainer  = Color(0xFF26215C)   // deep purple text on light containers

val StudyMateLightColorScheme = lightColorScheme(
    primary              = PrimaryPurple,
    onPrimary            = OnPrimary,
    primaryContainer     = LightSurfaceVariant,   // #EEEDFE
    onPrimaryContainer   = OnPrimaryContainer,
    secondary            = AccentCyan,
    onSecondary          = Color.White,
    secondaryContainer   = AccentCyanLight,
    onSecondaryContainer = Color(0xFF042C53),
    tertiary             = Color(0xFF1D9E75),      // teal — success/progress
    background           = LightBackground,
    onBackground         = Color(0xFF1A1A2E),
    surface              = LightSurface,
    onSurface            = Color(0xFF1A1A2E),
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = Color(0xFF534AB7),
    outline              = Color(0xFFAFA9EC),
)

val StudyMateDarkColorScheme = darkColorScheme(
    primary              = PrimaryPurpleLight,     // slightly lighter for dark bg
    onPrimary            = OnPrimary,
    primaryContainer     = Color(0xFF3C3489),
    onPrimaryContainer   = Color(0xFFCECBF6),
    secondary            = AccentCyan,
    onSecondary          = Color(0xFF042C53),
    secondaryContainer   = Color(0xFF0E7490),
    onSecondaryContainer = Color(0xFFCCF3FA),
    tertiary             = Color(0xFF5DCAA5),
    background           = DarkBackground,
    onBackground         = Color(0xFFEEEDFE),
    surface              = DarkSurface,
    onSurface            = Color(0xFFE8E7FF),
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = Color(0xFFAFA9EC),
    outline              = Color(0xFF534AB7),
)