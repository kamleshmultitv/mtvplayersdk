# Keep SDK public API
-keep public class com.app.videosdk.** {
    public *;
}

# Keep models
-keep class com.app.videosdk.model.** {
    <init>(...);
}
-keepclassmembers class com.app.videosdk.model.** {
    <fields>;
}

# Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Cast
-keep class com.google.android.gms.cast.** { *; }
-dontwarn com.google.android.gms.cast.**

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Compose safety
-dontwarn androidx.compose.**

-keep class com.app.videosdk.** { *; }

-keep interface com.app.videosdk.listener.PipListener { *; }


