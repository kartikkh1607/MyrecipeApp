package com.example.myrecipeapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myrecipeapp.ui.theme.MyrecipeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d("MainActivity", "Starting MyrecipeApp creation")
            
            // Log device information
            Log.d("MainActivity", "Android Version: ${android.os.Build.VERSION.RELEASE}")
            Log.d("MainActivity", "API Level: ${android.os.Build.VERSION.SDK_INT}")
            Log.d("MainActivity", "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            
            // Enable edge-to-edge for modern UI
            enableEdgeToEdge()
            Log.d("MainActivity", "Edge-to-edge enabled")

            setContent {
                Log.d("MainActivity", "Setting content with MyrecipeAppTheme")
                val recipeViewModel: MainViewModel = viewModel()
                val isSystemInDarkTheme = isSystemInDarkTheme()
                
                // For now, using system theme. Can be enhanced later with user preference
                val darkTheme = isSystemInDarkTheme
                
                MyrecipeAppTheme(darkTheme = darkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Log.d("MainActivity", "Creating MainScreen")
                        MainScreen()
                    }
                }
            }
            
            Log.d("MainActivity", "MainActivity created successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error creating MainActivity: ${e.message}", e)
            throw e // Re-throw to let the system handle it
        }
    }
}

