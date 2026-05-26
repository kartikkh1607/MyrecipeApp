package com.kartik.mealtime.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test for [BrandedSnackbarHost].
 *
 * Renders the host, pushes an indefinite snackbar (so it never auto-dismisses
 * mid-assertion), and verifies both the message and the prefixed action pill are
 * shown. Requires a connected device/emulator: run with
 * `./gradlew.bat :app:connectedDebugAndroidTest`.
 */
class BrandedSnackbarHostTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheSnackbarMessageAndActionLabel() {
        composeRule.setContent {
            val host = remember { SnackbarHostState() }
            LaunchedEffect(Unit) {
                host.showSnackbar(
                    message = "Added to favorites",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Indefinite
                )
            }
            MaterialTheme {
                BrandedSnackbarHost(hostState = host)
            }
        }

        composeRule.onNodeWithText("Added to favorites").assertIsDisplayed()
        composeRule.onNodeWithText("↩ Undo").assertIsDisplayed()
    }
}
