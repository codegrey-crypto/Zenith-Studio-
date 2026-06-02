# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Moshi Kotlin models and generated JSON adapters
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keep class com.example.studio.model.** { *; }
-keep class * { @com.squareup.moshi.JsonClass <methods>; }
-keep class * { @com.squareup.moshi.Json <fields>; }

# Keep Room Database and Entity components
-keep class androidx.room.** { *; }
-keep class com.example.studio.database.** { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
