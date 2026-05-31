package com.kartik.mealtime.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import org.junit.Rule
import org.junit.Test

/**
 * Produces `baseline-prof.txt` from a cold-start journey that the ART optimiser
 * uses to AOT-compile the classes hit during launch.
 *
 * **Run with:** `./gradlew :app:generateBaselineProfile` against a connected
 * rooted-debuggable device (Pixel emulator API 29+ image with Play Services
 * works; an API 33 AOSP image works without root). The generated file lands at
 * `app/src/<variant>/generated/baselineProfiles/baseline-prof.txt` and is
 * packaged into the AAB automatically by the `androidx.baselineprofile` plugin.
 *
 * **What this covers**
 *
 * Cold start through the SplashScreen API + BrandSplash composable + auth gate.
 * For most users the gate lands on [com.kartik.mealtime.ui.screens.AuthScreen]
 * because the test runner installs a fresh app with no signed-in Firebase user
 * — so this profile primarily warms the splash + auth + Compose foundation
 * paths, which IS the cold path for every new install.
 *
 * To also profile the post-auth screens, sign a test user in before running:
 * the journey then traverses HomeScreen, the carousel, and category grid, all
 * of which become part of the profile.
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE_NAME) {
        startActivityAndWait()
        // No explicit interactions here — startActivityAndWait already waits for
        // the first frame, which includes installSplashScreen + BrandSplash +
        // the AuthScreen/MainScreen branch. The macrobenchmark runner samples
        // the methods executed during this window and writes them to the profile.
    }

    private companion object {
        // Release applicationId. The "nonMinifiedRelease" variant produced for
        // profile generation matches this without the .debug suffix.
        const val PACKAGE_NAME = "com.kartik.mealtime"
    }
}
