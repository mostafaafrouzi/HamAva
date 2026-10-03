# HamAva ProGuard Configuration
# Copyright (c) 2026 Mostafa Afrouzi
# Licensed under the Apache License 2.0

# Keep Gemini API message classes
-keep class com.afrouzi.hamava.core.gemini.** { *; }
-keep class com.afrouzi.hamava.data.model.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Keep Gson models and annotations
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Hilt & ViewModel
-keep class * extends androidx.lifecycle.ViewModel
