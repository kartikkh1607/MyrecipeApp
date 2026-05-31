plugins {
    // AGP 9.x bundles Kotlin support; the standalone kotlin.android plugin is
    // no longer required (and applying it now errors).
    alias(libs.plugins.android.test)
    alias(libs.plugins.android.baselineprofile)
}

android {
    namespace = "com.kartik.mealtime.baselineprofile"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // API 28 is the minimum the macrobenchmark runner supports for profile
        // generation; profiles produced here still install on the app's minSdk 26.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Empty product flavor that the baselineProfile plugin uses to match against
    // the app's build types.
    targetProjectPath = ":app"

    // Macrobenchmarks need a debuggable target so they can attach via UI Automator,
    // but the variant that runs is "nonMinifiedRelease" — minify off so jank/methods
    // are still resolvable, otherwise R8 mangling defeats the trace analysis.
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

kotlin {
    jvmToolchain(17)
}

baselineProfile {
    // Generate against this module; the consumer (:app) picks up the .txt
    // automatically via the baselineProfile() dependency wired in :app.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
