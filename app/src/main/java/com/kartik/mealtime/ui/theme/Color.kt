package com.kartik.mealtime.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// MealTime — "Linen" redesign palette (Direction A)
// Refined earthy-minimal, premium. Warm paper backgrounds, forest-green primary,
// terracotta accent, thin rules. Light + dark.
// Names are preserved from the previous palette so existing screen code keeps
// compiling; only the values changed to the Linen tokens.
// ─────────────────────────────────────────────────────────────────────────────

// ── LIGHT — warm paper ──────────────────────────────────────────────────────
val Linen = Color(0xFFF4EEE5)               // Background — warm paper
val White = Color(0xFFFFFFFF)               // Surface — clean white
val Graphite = Color(0xFF23201B)            // Text primary — warm near-black
val Stone = Color(0xFF7C756A)               // Text secondary — warm muted gray
val ForestGreen = Color(0xFF2D5A4E)         // Primary — elegant deep green
val LightGray = Color(0xFFE8E0D3)           // Outline / thin rules

// Linen light — extended design tokens
val LinenSurface2 = Color(0xFFFBF7F0)       // Subtle raised / inset surface
val LinenFaint = Color(0xFFA79E91)          // Faintest text (placeholders, hints)
val OnForest = Color(0xFFFBF8F2)            // On-primary text (warm white)
val ForestQuiet = Color(0xFFE6EEE9)         // Primary container / quiet green
val ForestDeep = Color(0xFF1F4136)          // Deepest green (gradients, emphasis)
val Terracotta = Color(0xFFBC6B30)          // Accent — warm terracotta
val OnTerracotta = Color(0xFFFFFFFF)        // On-accent text
val TerracottaQuiet = Color(0xFFF4E7D9)     // Accent container
val StarGold = Color(0xFFC8861C)            // Ratings / stars
val Heart = Color(0xFFC0492F)               // Saved / favorite / destructive

// ── DARK — deep warm ────────────────────────────────────────────────────────
val Midnight = Color(0xFF17140D)            // Background — aged-linen-in-shadow
val DarkSurface = Color(0xFF251F16)         // Surface — raised warm charcoal
val Cream = Color(0xFFF3EEE4)               // Text primary — warm off-white
val MutedGray = Color(0xFFA89E8B)           // Text secondary — warm gray
val Teal = Color(0xFF8FCBAE)                // Primary — luminous sage (dark)

// Linen dark — extended design tokens
val DarkSurface2 = Color(0xFF322A1E)        // Subtle raised / inset surface (dark)
val DarkFaint = Color(0xFF7C7361)           // Faintest text (dark)
val OnSageDark = Color(0xFF0D1A13)          // On-primary text (dark)
val SageQuietDark = Color(0x29_8FCBAE)      // Primary container (≈16% sage)
val SageDeepDark = Color(0xFFB4DEC8)        // Deepest sage (dark emphasis)
val TerracottaDark = Color(0xFFE9AE66)      // Accent (dark) — warm glowing amber
val OnTerracottaDark = Color(0xFF2A1602)    // On-accent text (dark)
val TerracottaQuietDark = Color(0x2E_E9AE66)// Accent container (≈18%)
val StarGoldDark = Color(0xFFE7BB5E)        // Ratings / stars (dark)
val HeartDark = Color(0xFFE78C70)           // Saved / favorite (dark)
val LineDark = Color(0x21_F4E9D2)           // Outline — warm paper-tinted hairline (≈13%)

// ── Shared semantic ─────────────────────────────────────────────────────────
val Success = Color(0xFF3C7A4E)             // Inline success (light)
val SuccessDark = Color(0xFF85C89B)         // Inline success (dark)
val Error = Color(0xFFC0492F)               // Error / destructive (matches heart)

// ── Amber aliases (kept for backwards-compat with tertiary slots) ────────────
val Amber          = StarGold               // Ratings / calories / highlights
val AmberLight     = Color(0xFFF4E7D9)      // Light container
val AmberDark      = Color(0xFF6E3B12)      // On-container text (dark text on light)
val AmberContainer = Color(0x2E_E9AE66)     // Dark theme container
