package com.divyang.studymateai.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

import androidx.compose.ui.graphics.Color

// ── Brand ──────────────────────────────────────────────────────
val PrimaryPurple       = Color(0xFF534AB7)   // deep violet — primary (light mode)
val PrimaryPurpleLight  = Color(0xFFB7B0F5)   // light lavender — primary (dark mode)
val AccentCyan          = Color(0xFF06B6D4)   // cyan — accent (light mode)
val AccentCyanDark      = Color(0xFF38C6E0)   // brighter cyan — accent (dark mode)
val AccentCyanLight     = Color(0xFFCCF3FA)   // cyan tint for containers

// ── Surfaces ───────────────────────────────────────────────────
val LightBackground     = Color(0xFFF4F4F8)   // very slightly purple-tinted white
val LightSurface        = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEEDFE)   // purple-tinted card bg

val DarkBackground      = Color(0xFF131316)   // near-black with purple undertone
val DarkSurface         = Color(0xFF1E1E24)   // elevated surface
val DarkSurfaceVariant  = Color(0xFF2A2A33)   // card bg

val StudyMateLightColorScheme = lightColorScheme(
    primary              = PrimaryPurple,
    onPrimary            = Color.White,            // white on #534AB7 ≈ 7:1 ✓
    primaryContainer     = LightSurfaceVariant,    // #EEEDFE
    onPrimaryContainer   = Color(0xFF26215C),
    secondary            = AccentCyan,
    onSecondary          = Color(0xFF00363F),       // dark teal — readable on cyan fills
    secondaryContainer   = AccentCyanLight,
    onSecondaryContainer = Color(0xFF042C53),
    tertiary             = Color(0xFF1D9E75),       // green — success/progress
    onTertiary           = Color.White,
    background           = LightBackground,
    onBackground         = Color(0xFF1A1A2E),
    surface              = LightSurface,
    onSurface            = Color(0xFF1A1A2E),
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = Color(0xFF534AB7),       // branded muted purple
    outline              = Color(0xFFAFA9EC),
    error                = Color(0xFFA32D2D),       // matches the app's danger red
    onError              = Color.White,
    errorContainer       = Color(0xFFFCEBEB),
    onErrorContainer     = Color(0xFF7A1F19),
)

val StudyMateDarkColorScheme = darkColorScheme(
    primary              = PrimaryPurpleLight,      // #B7B0F5 — light enough for dark text
    onPrimary            = Color(0xFF241A54),       // deep indigo on lavender ≈ 8:1 ✓
    primaryContainer     = Color(0xFF3C3489),
    onPrimaryContainer   = Color(0xFFE4E1FB),
    secondary            = AccentCyanDark,          // #38C6E0 — brighter for dark bg
    onSecondary          = Color(0xFF00363F),
    secondaryContainer   = Color(0xFF0E7490),
    onSecondaryContainer = Color(0xFFCCF3FA),
    tertiary             = Color(0xFF5DCAA5),
    onTertiary           = Color(0xFF00382A),
    background           = DarkBackground,
    onBackground         = Color(0xFFEAE9F5),
    surface              = DarkSurface,
    onSurface            = Color(0xFFE8E7FF),
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = Color(0xFFAFA9EC),
    outline              = Color(0xFF4A455F),       // softer neutral divider
    error                = Color(0xFFF2B8B5),
    onError              = Color(0xFF601410),
    errorContainer       = Color(0xFF8C1D18),
    onErrorContainer     = Color(0xFFF9DEDC),
)