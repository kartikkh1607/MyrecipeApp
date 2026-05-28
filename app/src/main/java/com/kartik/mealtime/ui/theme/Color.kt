package com.kartik.mealtime.ui.theme

import androidx.compose.ui.graphics.Color

// PREMIUM BRAND IDENTITY - Intentional Minimalism
// Earthy, natural, and premium aesthetic

// Light Theme Palette - Approved Brand Colors
val Linen = Color(0xFFFAF7F5)               // Background - Warm, inviting off-white
val Graphite = Color(0xFF2E2E2E)            // Text Primary - Deep, soft charcoal
val Stone = Color(0xFF6B6B6B)               // Text Secondary - Neutral gray for subtitles (AA on Linen: 5.1:1)
val ForestGreen = Color(0xFF2D5A5A)         // Primary Accent - Elegant, deep green
val White = Color(0xFFFFFFFF)               // Surfaces (Cards) - Clean, pure white

// Dark Theme Palette - Approved Brand Colors
val Midnight = Color(0xFF1B1D21)            // Background - Sophisticated dark blue-gray
val Cream = Color(0xFFF0EBE8)               // Text Primary - Soft, warm off-white
val MutedGray = Color(0xFF9A9A9A)           // Text Secondary - Gentle gray (AA on Midnight: 6.4:1)
val Teal = Color(0xFF66B5B5)                // Primary Accent - Vibrant teal for accents
val DarkSurface = Color(0xFF24262B)         // Surfaces (Cards) - Lighter charcoal

// Warm Amber / Saffron — ratings, calories, and food highlights
val Amber          = Color(0xFFF59E0B)    // Core amber accent
val AmberLight     = Color(0xFFFEF3C7)    // Light container (light theme)
val AmberDark      = Color(0xFF92400E)    // Dark on-container text
val AmberContainer = Color(0xFF3D2800)    // Dark theme container

// Error state — used by Theme.kt error color slot
val Error = Color(0xFFB3261E)               // AA on Linen: 6.3:1; AA white-on-error: 6.7:1

// Success — inline validation (e.g. "passwords match", strong password meter)
val Success = Color(0xFF2E7D32)

// Light border color — used by Theme.kt outline slot
val LightGray = Color(0xFFE5E5E5)
