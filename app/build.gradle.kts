import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.plugin)
    // Consumer side of the baseline profile pipeline: takes the .txt produced
    // by the :baselineprofile module and packages it into the AAB so the
    // ART optimiser AOT-compiles startup-critical classes on install.
    alias(libs.plugins.android.baselineprofile)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Load local.properties
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.kartik.mealtime"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kartik.mealtime"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add vector drawable support for lower API levels
        vectorDrawables {
            useSupportLibrary = true
        }

        // API keys are no longer shipped in the app. They live server-side in the
        // Cloudflare Worker proxy (server/cloudflare-worker), which injects them and
        // forwards requests. The app only needs the proxy's URL. Overridable via
        // local.properties (proxy.base.url) for dev/staging; defaults to production.
        val proxyBaseUrl = localProperties.getProperty("proxy.base.url")
            ?: "https://mealtime-proxy.kartik-mealtime.workers.dev"
        buildConfigField("String", "PROXY_BASE_URL", "\"$proxyBaseUrl\"")

        // AdMob production ad-unit IDs — injected from local.properties so they never
        // live in source control. Left blank in debug (debug uses Google's baked-in
        // test IDs) and blank in release until configured. AdConfig.adsEnabled disables
        // ads in release when these are blank, so test IDs can never ship as production.
        val admobBannerId = localProperties.getProperty("admob.banner.id") ?: ""
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
        val admobInterstitialId = localProperties.getProperty("admob.interstitial.id") ?: ""
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")

        // AdMob App ID for the manifest. Default to Google's test App ID (valid format,
        // safe for debug); the release build below swaps in the production App ID when
        // admob.app.id is set in local.properties. A valid App ID must always be present
        // or the Ads SDK's auto-init ContentProvider crashes the app at launch.
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
    }

    // Only register a release signing config if local.properties has all 4 entries.
    // Avoids a configuration-time failure when contributors clone without a keystore.
    val keystorePath = localProperties.getProperty("keystore.path")
    val keystoreStorePassword = localProperties.getProperty("keystore.store.password")
    val keystoreKeyAlias = localProperties.getProperty("keystore.key.alias")
    val keystoreKeyPassword = localProperties.getProperty("keystore.key.password")
    val hasReleaseSigning = !keystorePath.isNullOrBlank() &&
            !keystoreStorePassword.isNullOrBlank() &&
            !keystoreKeyAlias.isNullOrBlank() &&
            !keystoreKeyPassword.isNullOrBlank() &&
            rootProject.file(keystorePath).exists()

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile     = rootProject.file(keystorePath!!)
                storePassword = keystoreStorePassword
                keyAlias      = keystoreKeyAlias
                keyPassword   = keystoreKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // Swap in the production AdMob App ID when configured; otherwise keep the
            // test App ID so the Ads SDK can still initialize (ad requests stay gated
            // by AdConfig.adsEnabled, which is false in release without prod unit IDs).
            val prodAdmobAppId = localProperties.getProperty("admob.app.id")
            if (!prodAdmobAppId.isNullOrBlank()) {
                manifestPlaceholders["admobAppId"] = prodAdmobAppId
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "NONE"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig for API keys
    }

    testOptions {
        unitTests {
            // Robolectric needs Android resources merged into the unit-test classpath.
            isIncludeAndroidResources = true
            // Return sane defaults (0/false/empty) instead of throwing on unmocked
            // android.jar calls — keeps pure-JVM tests from hitting "not mocked" errors.
            isReturnDefaultValues = true
        }
    }

    lint {
        // Fail the build on any lint error so CI catches regressions; without this,
        // `lintDebug` reports errors but exits 0 and the CI job passes regardless.
        abortOnError = true
        // Treat warnings as errors so newly-introduced ones can't accumulate silently.
        warningsAsErrors = true
        // Grandfather the existing 76 lint errors into a baseline so the new
        // quality gate only fails on regressions. Refresh with `./gradlew :app:updateLintBaseline`
        // after a deliberate cleanup pass.
        baseline = file("lint-baseline.xml")
        // Still emit reports uploaded by CI for debuggability.
        htmlReport = true
        xmlReport = true
        // Don't gate the build on lint checking test sources — keeps fixtures lenient.
        checkTestSources = false
    }

    // Expose the exported Room schemas as androidTest assets so MigrationTestHelper
    // can replay each schema version against an in-memory database. Schemas live in
    // app/schemas/ (configured via `ksp.arg("room.schemaLocation", ...)` above).
    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // OkHttp — used directly by GeminiAiService for streaming. Version-aligned via the
    // catalog with logging-interceptor (libs.logging.interceptor, below) and the OkHttp
    // 5.x that Retrofit 3.x pulls transitively, so only one OkHttp is on the classpath.
    implementation(libs.okhttp)

    // Google Play Billing — premium subscription (monthly + annual base plans).
    // Wrapped by data/billing/BillingManager; entitlement flows through
    // data/billing/BillingEntitlementRepository -> EntitlementRepository seam.
    implementation(libs.billing.ktx)

    // Google AdMob — banner + interstitial ads (Phase A monetization).
    // Production ad unit IDs go in data/ads/AdConfig.kt; test IDs are baked in.
    implementation(libs.play.services.ads)

    // Google UMP (User Messaging Platform) — GDPR/EEA consent flow. Required to
    // serve ads to EU/UK users; drives data/ads/ConsentManager.kt.
    implementation(libs.user.messaging.platform)

    // Android 12+ SplashScreen API (with back-compat shim for older versions).
    implementation(libs.androidx.core.splashscreen)

    // Kotlinx Serialization (required for type-safe Navigation 2.8+)
    implementation(libs.kotlinx.serialization.json)

    // Material 3 Extended Icons
    implementation(libs.androidx.compose.material.icons.extended)

    // Google Fonts (Playfair Display for screen titles)
    implementation(libs.androidx.compose.ui.text.google.fonts)

    // DataStore Preferences (theme persistence)
    implementation(libs.androidx.datastore.preferences)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // HorizontalPager for Carousel
    implementation(libs.androidx.compose.foundation)



    // compose viewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Network Calls
    implementation(libs.retrofit)

    // json to kotlin object mapping
    implementation(libs.converter.gson)

    // HTTP logging interceptor for debugging
    implementation(libs.logging.interceptor)

    // for image loading
    implementation(libs.coil.compose)

    // Room (local database for favorites + shopping list persistence)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)   // coroutines + Flow support
    ksp(libs.androidx.room.compiler)

    // Gson for TypeConverters
    implementation(libs.gson)


    // Baseline Profile
    implementation(libs.androidx.profileinstaller)
    // Pulls the generated profile from the :baselineprofile producer module
    // and bundles it into app/src/<variant>/generated/baselineProfiles/.
    baselineProfile(project(":baselineprofile"))

    // Core & Compose BOM
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)

    // Baseline Profile
    androidTestImplementation(libs.androidx.benchmark.macro.junit4)
    androidTestImplementation(libs.androidx.uiautomator)

    // Debugging
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Firebase — BOM keeps all Firebase library versions in sync automatically
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    // Credential Manager — modern Google Sign-In flow (replaces deprecated GoogleSignInClient).
    // Returns a GoogleIdToken via androidx.credentials → fed into FirebaseAuth.signInWithCredential.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

// Hilt Navigation Compose
    implementation(libs.androidx.hilt.navigation.compose)


}
