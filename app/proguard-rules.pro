# Joud&Jawad Tube ProGuard / R8 Rules

# Keep Room entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Media3 reflection / service / decoder entry points (tightened)
-keep class androidx.media3.exoplayer.video.VideoDecoderGLSurfaceView { <init>(...); }
-keep class androidx.media3.ui.PlayerView { <init>(...); }
-keep class * extends androidx.media3.session.MediaSessionService
-keepclassmembers class * implements androidx.media3.exoplayer.Renderer {
    <init>(...);
}
-keepclassmembers class * implements androidx.media3.extractor.Extractor {
    <init>(...);
}
-keepclassmembers class * implements androidx.media3.datasource.DataSource$Factory {
    <init>(...);
}
-keep class androidx.media3.extractor.text.** { *; }
-keep class androidx.media3.exoplayer.text.** { *; }
-dontwarn androidx.media3.**

# Keep Coil reflection / decoder entry points (tightened)
-keep class coil.decode.VideoFrameDecoder$Factory { <init>(...); }
-keepclassmembers class * implements coil.decode.Decoder$Factory {
    <init>(...);
}
-keepclassmembers class * implements coil.fetch.Fetcher$Factory {
    <init>(...);
}

# Keep line numbers for crash reporting
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
