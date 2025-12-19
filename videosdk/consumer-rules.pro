# =====================================================
# Video SDK – Consumer ProGuard Rules
# Applied automatically to all consuming apps
# =====================================================

# ---------- PUBLIC SDK API ----------
-keep public class com.app.videosdk.** {
    public *;
}

# ---------- SDK MODELS ----------
-keep class com.app.videosdk.model.** {
    <init>(...);
}
-keepclassmembers class com.app.videosdk.model.** {
    <fields>;
}

# ---------- MEDIA3 / EXOPLAYER ----------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ---------- GOOGLE CAST ----------
-keep class com.google.android.gms.cast.** { *; }
-dontwarn com.google.android.gms.cast.**

# ---------- GSON ----------
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ---------- COMPOSE ----------
-dontwarn androidx.compose.**

# ---------- ANNOTATIONS ----------
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
