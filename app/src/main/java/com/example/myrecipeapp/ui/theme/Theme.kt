package com.example.myrecipeapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// PREMIUM BRAND DARK THEME - Sophisticated Midnight Palette
private val PremiumDarkColorScheme = darkColorScheme(
    // Primary - Teal accent for interactive elements
    primary = Teal,
    onPrimary = Midnight,
    primaryContainer = Color(0xFF4A9999),     // Darker teal for containers
    onPrimaryContainer = Cream,
    
    // Secondary - Forest green for secondary actions
    secondary = ForestGreen,
    onSecondary = Cream,
    secondaryContainer = Color(0xFF234848),   // Darker forest green
    onSecondaryContainer = Cream,
    
    // Tertiary - Subtle accent
    tertiary = MutedGray,
    onTertiary = Cream,
    
    // Backgrounds and surfaces
    background = Midnight,
    onBackground = Cream,
    
    surface = DarkSurface,
    onSurface = Cream,
    surfaceVariant = Color(0xFF2A2C31),       // Slightly lighter than surface
    onSurfaceVariant = MutedGray,
    
    // Borders and outlines - minimal and refined
    outline = Color(0xFF3A3C41),              // Subtle borders
    outlineVariant = Color(0xFF2F3135),       // Even more subtle
    
    // Error states
    error = Color(0xFFFF6B6B),                // Softer red for dark theme
    onError = Midnight,
    errorContainer = Color(0xFF4A1A1A),
    onErrorContainer = Color(0xFFFFCDD2)
)

// PREMIUM BRAND LIGHT THEME - Elegant Linen Palette  
private val PremiumLightColorScheme = lightColorScheme(
    // Primary - Forest green for main actions
    primary = ForestGreen,
    onPrimary = White,
    primaryContainer = Color(0xFFE8F5F5),     // Very light green container
    onPrimaryContainer = ForestGreen,
    
    // Secondary - Softer green for secondary actions
    secondary = Color(0xFF4A7C7C),            // Lighter forest green
    onSecondary = White,
    secondaryContainer = Color(0xFFF0F8F8),   // Very light secondary container
    onSecondaryContainer = ForestGreen,
    
    // Tertiary - Stone for subtle accents
    tertiary = Stone,
    onTertiary = White,
    
    // Backgrounds and surfaces
    background = Linen,
    onBackground = Graphite,
    
    surface = White,
    onSurface = Graphite,
    surfaceVariant = Color(0xFFF8F5F3),       // Slightly warmer than white
    onSurfaceVariant = Stone,
    
    // Borders and outlines - clean and minimal
    outline = LightGray,
    outlineVariant = Color(0xFFF0F0F0),       // Very subtle borders
    
    // Error states
    error = Error,
    onError = White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C)
)

@Composable
fun MyrecipeAppTheme(
    darkTheme: Boolean = false,
    // DISABLED dynamic color to ensure consistent theming across all devices
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
        content = content
    )
}
