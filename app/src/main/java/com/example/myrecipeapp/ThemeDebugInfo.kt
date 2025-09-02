package com.example.myrecipeapp

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

@Composable
fun ThemeDebugInfo() {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    LaunchedEffect(Unit) {
        Log.d("ThemeDebug", "=== THEME DEBUG INFO ===")
        Log.d("ThemeDebug", "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        Log.d("ThemeDebug", "Android API: ${android.os.Build.VERSION.SDK_INT}")
        Log.d("ThemeDebug", "Primary: #${Integer.toHexString(colorScheme.primary.toArgb()).uppercase()}")
        Log.d("ThemeDebug", "Secondary: #${Integer.toHexString(colorScheme.secondary.toArgb()).uppercase()}")
        Log.d("ThemeDebug", "Background: #${Integer.toHexString(colorScheme.background.toArgb()).uppercase()}")
        Log.d("ThemeDebug", "Surface: #${Integer.toHexString(colorScheme.surface.toArgb()).uppercase()}")
        Log.d("ThemeDebug", "On Primary: #${Integer.toHexString(colorScheme.onPrimary.toArgb()).uppercase()}")
        Log.d("ThemeDebug", "On Background: #${Integer.toHexString(colorScheme.onBackground.toArgb()).uppercase()}")
        Log.d("ThemeDebug", "========================")
    }
}
