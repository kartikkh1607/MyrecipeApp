package com.kartik.mealtime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kartik.mealtime.data.ads.AdConfig
import com.kartik.mealtime.data.ads.ConsentManager
import com.kartik.mealtime.domain.model.ThemeMode
import com.kartik.mealtime.ui.components.BrandSplash
import com.kartik.mealtime.ui.screens.AuthScreen
import com.kartik.mealtime.ui.screens.MainScreen
import com.kartik.mealtime.ui.screens.VerifyEmailScreen
import com.kartik.mealtime.ui.theme.MyrecipeAppTheme
import com.kartik.mealtime.ui.viewmodel.AuthViewModel
import com.kartik.mealtime.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var consentManager: ConsentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() MUST be called before super.onCreate so the
        // SplashScreen API can intercept the activity lifecycle. The system
        // splash (cream icon on forest-green) shows here during cold-launch.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Gather GDPR/EEA ad consent (UMP), then initialize AdMob. Runs only when ads
        // are actually in play; ConsentManager handles the non-EEA fast path internally.
        if (AdConfig.adsEnabled) consentManager.gatherConsent(this)
        setContent {
            val recipeViewModel: MainViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val themeMode by recipeViewModel.themeMode.collectAsStateWithLifecycle()
            val authDestination by authViewModel.authDestination.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            // Brand splash extends the visual moment past the system splash —
            // showing "MealTime" title + tagline before fading into the app.
            // Once finished, [showBrandSplash] flips false and [authDestination]
            // decides: sign-in gate (mandatory), email-verification hold, or MainScreen.
            var showBrandSplash by remember { mutableStateOf(true) }

            MyrecipeAppTheme(
                darkTheme = darkTheme,
                dynamicColor = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        showBrandSplash -> BrandSplash(onFinished = { showBrandSplash = false })
                        // Sign-in is mandatory and email/password accounts must verify
                        // their email before reaching the app (and the Worker proxy).
                        authDestination == AuthViewModel.AuthDestination.SignIn ->
                            AuthScreen(viewModel = authViewModel)
                        authDestination == AuthViewModel.AuthDestination.VerifyEmail ->
                            VerifyEmailScreen(viewModel = authViewModel)
                        else -> MainScreen(viewModel = recipeViewModel)
                    }
                }
            }
        }
    }
}
