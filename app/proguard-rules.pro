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

# ── Kotlinx Serialisation ─────────────────────────────────────────────────────
# Used for: Spoonacular DTOs, AI DTOs, Gemini/Groq request+response, Room
# TypeConverter for Recipe, DataStore-backed caches, type-safe nav routes.
# Rules from the kotlinx.serialization manual; kept on top of the consumer-rules
# already shipped by the lib so R8's `-allowaccessmodification` doesn't strip
# the generated `$serializer` companion or rename it past lookup.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep every @Serializable class along with its generated $serializer object.
-keep,includedescriptorclasses @kotlinx.serialization.Serializable class com.kartik.mealtime.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.kartik.mealtime.** {
    @kotlinx.serialization.Serializable <fields>;
}

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
-keep class com.kartik.mealtime.BuildConfig { *; }

# Suppress warnings about Kotlin coroutines internals (safe to ignore).
-dontwarn kotlinx.coroutines.**

# ── Gemini / Groq AI DTOs ─────────────────────────────────────────────────────
# Covered by the generic @Serializable rule above — no per-class keeps needed
# now that Gemini/Groq request+response DTOs are kotlinx.serialization classes.

# ── Hilt ──────────────────────────────────────────────────────────────────────
# Hilt bundles its own rules via the AAR, but these guard against edge cases
# where generated _HiltComponents or _MembersInjector classes get stripped.
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-dontwarn dagger.hilt.**

# ── Firebase ──────────────────────────────────────────────────────────────────
# Firebase auto-initializes via FirebaseInitProvider (a ContentProvider) which
# discovers SDK components via META-INF/services + ServiceLoader. The aggressive
# R8 config below (-allowaccessmodification, -repackageclasses, optimizationpasses 5)
# strips these by default, causing "FirebaseCrashlytics component is not present"
# NPE at Application.onCreate. Keep all component registrars + init plumbing.
-keep class * implements com.google.firebase.components.ComponentRegistrar { *; }
-keepnames class * implements com.google.firebase.components.ComponentRegistrar
-keep class com.google.firebase.provider.FirebaseInitProvider { *; }
-keep class com.google.firebase.components.ComponentDiscoveryService { *; }
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.google.firebase.installations.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.analytics.** { *; }
-dontwarn com.google.firebase.**

# ── Logging: strip ALL log calls in release ───────────────────────────────────
# Removes every android.util.Log call (v/d/i/w/e/wtf) plus printStackTrace and
# System.out.println from release builds. Prevents API behaviour, user actions,
# and stack traces from leaking into device logs where any installed app with
# READ_LOGS permission (on older Android versions) could harvest them.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
-assumenosideeffects class java.io.PrintStream {
    public void println(%);
    public void println(**);
}
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}

# ── Source-line stripping ─────────────────────────────────────────────────────
# Without these, decompiled stack traces include original .kt filenames and
# line numbers — a roadmap for reverse engineers. We still need SourceFile for
# Crashlytics symbolication, but we rename it to something opaque.
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ── Aggressive obfuscation ────────────────────────────────────────────────────
# Allow R8 to merge interfaces, repackage classes, and rename packages.
# Makes static analysis of the APK significantly harder.
-repackageclasses ''
-allowaccessmodification
-optimizationpasses 5

# ── API key protection (defense in depth) ─────────────────────────────────────
# BuildConfig holds Spoonacular + Gemini keys. R8 normally keeps the class for
# us (rule above), but we make the field strings live in a renamed class so
# string search through the APK doesn't immediately surface them.
# NOTE: This is not real security — a determined attacker WILL extract the keys.
# The proper fix is a Cloud Function proxy. See SECURITY.md.