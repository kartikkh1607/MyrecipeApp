import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp") version "2.3.7"
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.plugin)
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
    namespace = "com.example.myrecipeapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kartik.mealtime"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add vector drawable support for lower API levels
        vectorDrawables {
            useSupportLibrary = true
        }

        // This line reads the property from local.properties and creates a field in BuildConfig
        val apiKey = localProperties.getProperty("spoonacular.api.key") ?: ""
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"$apiKey\"")
        val geminiApiKey = localProperties.getProperty("gemini.api.key") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig for API keys
    }
}

dependencies {
    // Gemini AI - OkHttp for streaming
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    val nav_version = "2.8.5"

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
    val room_version = "2.6.1"
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)   // coroutines + Flow support
    ksp(libs.androidx.room.compiler)

    // Gson for TypeConverters
    implementation(libs.gson)


    // Baseline Profile
    implementation(libs.androidx.profileinstaller)

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

// Hilt Navigation Compose
    implementation(libs.androidx.hilt.navigation.compose.v120)


}
