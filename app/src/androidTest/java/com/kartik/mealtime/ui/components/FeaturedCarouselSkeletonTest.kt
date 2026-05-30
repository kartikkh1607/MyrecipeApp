package com.kartik.mealtime.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test for [FeaturedCarouselSkeleton] — verifies the loading placeholder
 * mounts and renders without throwing. Catches stability regressions (e.g. an
 * unstable Brush lambda) and outright composition errors that the type system
 * won't see but a real device will.
 */
@RunWith(AndroidJUnit4::class)
class FeaturedCarouselSkeletonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                FeaturedCarouselSkeleton()
            }
        }

        // The skeleton renders an undisclosed number of shimmer rectangles. We don't
        // assert on their specifics — this is a smoke test for "did composition mount".
        // fetchSemanticsNode() throws if setContent failed, which is the regression we
        // care about (e.g. an unstable Brush lambda crashing during recomposition).
        composeRule.onRoot().fetchSemanticsNode()
    }
}
