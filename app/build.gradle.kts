import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

// Load local.properties
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.myrecipeapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myrecipeapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add vector drawable support for lower API levels
        vectorDrawables {
            useSupportLibrary = true
        }

        // Enable multidex for better compatibility
        multiDexEnabled = true

        // This line reads the property from local.properties and creates a field in BuildConfig
        val apiKey = localProperties.getProperty("spoonacular.api.key") ?: "null"
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"$apiKey\"")
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Performance optimizations
            ndk {
                debugSymbolLevel = "NONE"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig for API keys
    }
}

dependencies {
    val nav_version = "2.8.5"

    // Kotlinx Serialization (required for type-safe Navigation 2.8+)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Material 3 Extended Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:$nav_version")

    // HorizontalPager for Carousel
    implementation("androidx.compose.foundation:foundation:1.7.5")



    // compose viewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // Network Calls
    implementation("com.squareup.retrofit2:retrofit:3.0.0")

    // json to kotlin object mapping
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    
    // HTTP logging interceptor for debugging
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Room (local database for favorites + shopping list persistence)
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")   // coroutines + Flow support
    ksp("androidx.room:room-compiler:$room_version")

    // Gson for TypeConverters
    implementation("com.google.code.gson:gson:2.11.0")


    // Baseline Profile
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

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
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.robolectric:robolectric:4.10.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Baseline Profile
    androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:1.2.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")

    // Debugging
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
