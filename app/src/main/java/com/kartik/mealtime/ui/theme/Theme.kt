package com.kartik.mealtime.ui.theme

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// "Linen" DARK — deep warm, soft sage primary, amber-terracotta accent
// ─────────────────────────────────────────────────────────────────────────────
private val PremiumDarkColorScheme = darkColorScheme(
    // Primary — soft sage for interactive elements
    primary = Teal,
    onPrimary = OnSageDark,
    primaryContainer = SageQuietDark,
    onPrimaryContainer = SageDeepDark,

    // Secondary — deeper sage for secondary actions
    secondary = SageDeepDark,
    onSecondary = OnSageDark,
    secondaryContainer = SageQuietDark,
    onSecondaryContainer = Cream,

    // Tertiary — warm terracotta accent (kickers, badges, food highlights)
    tertiary = TerracottaDark,
    onTertiary = OnTerracottaDark,
    tertiaryContainer = TerracottaQuietDark,
    onTertiaryContainer = TerracottaDark,

    // Backgrounds and surfaces
    background = Midnight,
    onBackground = Cream,

    surface = DarkSurface,
    onSurface = Cream,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = MutedGray,

    // Borders and outlines — thin, subtle
    outline = LineDark,
    outlineVariant = Color(0x12_F4E9D2),

    // Error states
    error = HeartDark,
    onError = Midnight,
    errorContainer = Color(0xFF4A1F18),
    onErrorContainer = Color(0xFFF6C9BD)
)

// ─────────────────────────────────────────────────────────────────────────────
// "Linen" LIGHT — warm paper, forest-green primary, terracotta accent
// ─────────────────────────────────────────────────────────────────────────────
private val PremiumLightColorScheme = lightColorScheme(
    // Primary — forest green for main actions
    primary = ForestGreen,
    onPrimary = OnForest,
    primaryContainer = ForestQuiet,
    onPrimaryContainer = ForestDeep,

    // Secondary — deep forest for secondary actions
    secondary = ForestDeep,
    onSecondary = OnForest,
    secondaryContainer = ForestQuiet,
    onSecondaryContainer = ForestDeep,

    // Tertiary — warm terracotta accent (kickers, badges, food highlights)
    tertiary = Terracotta,
    onTertiary = OnTerracotta,
    tertiaryContainer = TerracottaQuiet,
    onTertiaryContainer = AmberDark,

    // Backgrounds and surfaces
    background = Linen,
    onBackground = Graphite,

    surface = White,
    onSurface = Graphite,
    surfaceVariant = LinenSurface2,
    onSurfaceVariant = Stone,

    // Borders and outlines — clean, minimal, warm
    outline = LightGray,
    outlineVariant = Color(0xFFF0E9DC),

    // Error states
    error = Error,
    onError = White,
    errorContainer = Color(0xFFF7E2DC),
    onErrorContainer = Color(0xFF7A2A1B)
)

// ─────────────────────────────────────────────────────────────────────────────
// Shape scale — Linen radii (sm 9 / md 13 / lg 18 / xl 26)
// ─────────────────────────────────────────────────────────────────────────────
private val LinenShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(13.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun MyrecipeAppTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Only use dynamic colors if explicitly enabled AND on Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Use our premium brand color schemes by default
        darkTheme -> PremiumDarkColorScheme
        else -> PremiumLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = LinenShapes,
        content = content
    )
}
