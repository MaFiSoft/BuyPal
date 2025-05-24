# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the Android SDK tools proguard folder.
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room specific rules
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * {
    *;
}
-keep @androidx.room.Dao class * {
    *;
}

# Compose specific rules
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
