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

# ── Coil (image loading) ──────────────────────────────────────────────────────
-dontwarn coil.**

# ── Android / Compose ─────────────────────────────────────────────────────────
# Keep BuildConfig so the API key and DEBUG flag are accessible at runtime.
-keep class com.example.myrecipeapp.BuildConfig { *; }

# Suppress warnings about Kotlin coroutines internals (safe to ignore).
-dontwarn kotlinx.coroutines.**

# ── Logging: strip debug logs in release ──────────────────────────────────────
# Removes all android.util.Log.d and Log.v calls from release builds,
# preventing API usage patterns from leaking into device logs.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}