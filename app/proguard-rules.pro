# ── Retrofit + OkHttp ─────────────────────────────────────────────────────────
# Retrofit uses reflection to call interface methods & convert responses.
# Without these rules the release build will crash with NoSuchMethodError.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp platform detection
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Gson / JSON serialisation ─────────────────────────────────────────────────
# Gson uses field names (via reflection) to map JSON ↔ data classes.
# Without these rules all DTO fields will be stripped/renamed and
# the app will silently receive null/empty data for every API response.
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep all DTO classes (Spoonacular response models)
-keep class com.example.myrecipeapp.data.remote.dto.** { *; }

# ── Kotlinx Serialisation ─────────────────────────────────────────────────────
# Required for type-safe navigation routes serialised with @Serializable.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep all navigation route data classes
-keep @kotlinx.serialization.Serializable class com.example.myrecipeapp.ui.navigation.** { *; }

# ── Room (local database) ─────────────────────────────────────────────────────
# Room generates *_Impl classes at compile time via KSP. R8 must not strip them.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# ── Coil (image loading) ──────────────────────────────────────────────────────
-dontwarn coil.**

# ── Android / Compose ─────────────────────────────────────────────────────────
# Keep BuildConfig so the API key and DEBUG flag are accessible at runtime.
-keep class com.example.myrecipeapp.BuildConfig { *; }

# Suppress warnings about Kotlin coroutines internals (safe to ignore).
-dontwarn kotlinx.coroutines.**

# ── Gemini AI (request / response DTOs serialised with Gson) ─────────────────
# These data classes live directly in the .remote package and use @SerializedName.
# Without this rule R8 strips their fields and AI features return null responses.
-keep class com.example.myrecipeapp.data.remote.GenerateContentRequest { *; }
-keep class com.example.myrecipeapp.data.remote.GenerateContentResponse { *; }
-keep class com.example.myrecipeapp.data.remote.Content { *; }
-keep class com.example.myrecipeapp.data.remote.Part { *; }
-keep class com.example.myrecipeapp.data.remote.GenerationConfig { *; }
-keep class com.example.myrecipeapp.data.remote.Candidate { *; }
-keep class com.example.myrecipeapp.data.remote.ChatMessage { *; }

# ── Hilt ──────────────────────────────────────────────────────────────────────
# Hilt bundles its own rules via the AAR, but these guard against edge cases
# where generated _HiltComponents or _MembersInjector classes get stripped.
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-dontwarn dagger.hilt.**

# ── Logging: strip debug logs in release ──────────────────────────────────────
# Removes all android.util.Log.d and Log.v calls from release builds,
# preventing API usage patterns from leaking into device logs.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}