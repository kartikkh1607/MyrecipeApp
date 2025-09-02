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

// Modern Recipe App Dark Color Scheme - Sophisticated and warm
private val ModernRecipeDarkColorScheme = darkColorScheme(
    primary = CoralDark,
    onPrimary = Color.White,
    primaryContainer = PeachDark,
    onPrimaryContainer = Color.Black,
    
    secondary = MintDark,
    onSecondary = Color.White,
    secondaryContainer = SageDark,
    onSecondaryContainer = Color.White,
    
    tertiary = LavenderBlush,
    onTertiary = Color.Black,
    
    background = WarmDarkBg,
    onBackground = LightText,
    
    surface = SoftDarkSurface,
    onSurface = LightText,
    surfaceVariant = Color(0xFF404040),
    onSurfaceVariant = MutedText,
    
    outline = Color(0xFF707070),
    outlineVariant = Color(0xFF505050),
    
    error = Color(0xFFFF8A80),
    onError = Color.Black,
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color.White
)

// Modern Recipe App Light Color Scheme - Soft, appetizing, clean
private val ModernRecipeLightColorScheme = lightColorScheme(
    primary = SoftCoral,
    onPrimary = Color.White,
    primaryContainer = WarmPeach,
    onPrimaryContainer = CharcoalGray,
    
    secondary = FreshMint,
    onSecondary = Color.White,
    secondaryContainer = SageGreen,
    onSecondaryContainer = CharcoalGray,
    
    tertiary = LavenderBlush,
    onTertiary = CharcoalGray,
    
    background = CreamyWhite,
    onBackground = CharcoalGray,
    
    surface = PureWhite,
    onSurface = CharcoalGray,
    surfaceVariant = Color(0xFFF8F8F8),
    onSurfaceVariant = SoftGray,
    
    outline = Color(0xFFD0D0D0),
    outlineVariant = Color(0xFFE8E8E8),
    
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C)
)

@Composable
fun MyrecipeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
        // Use our modern recipe-themed color schemes by default
        darkTheme -> ModernRecipeDarkColorScheme
        else -> ModernRecipeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
