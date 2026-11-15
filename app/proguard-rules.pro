# ====================================================================
# CSE Judge — Production ProGuard / R8 Optimization & Obfuscation Rules
# ====================================================================

# 1. General Optimization & Obfuscation
-repackageclasses 'com.csejudge.app.internal'
-allowaccessmodification

# 2. Kotlinx Serialization
# Preserve serializer methods and companion objects while allowing class name obfuscation
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class * {
    *** Companion;
}

# Preserve data models used in serialization
-keep,allowobfuscation class com.csejudge.app.data.model.** { *; }

# 3. AndroidX Room Database
# Room DAOs, Entities, and Database implementation
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# 4. Android Architecture & ViewModel
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# 5. Security & Cryptography
# Ensure cryptographic algorithms are not inlined or stripped
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# 6. Remove unnecessary logging in production builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
