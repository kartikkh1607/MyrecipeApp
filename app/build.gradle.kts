plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
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
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"${project.findProperty("spoonacular.api.key")}\"")
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
    val nav_version = "2.7.7"

    // Material 3 Extended Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:$nav_version")

    // HorizontalPager for Carousel
    implementation("androidx.compose.foundation:foundation:1.7.5")

    // Accompanist for advanced UI components
    implementation("com.google.accompanist:accompanist-pager:0.32.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.32.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    // Lottie for advanced animations
    implementation("com.airbnb.android:lottie-compose:6.1.0")

    // compose viewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // Network Calls
    implementation("com.squareup.retrofit2:retrofit:3.0.0")

    // json to kotlin object mapping
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Gson for TypeConverters
    implementation("com.google.code.gson:gson:2.11.0")

    // Date picker
    implementation("io.github.vanpra.compose-material-dialogs:datetime:0.9.0")

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

