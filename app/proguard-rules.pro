# Joud&Jawad Tube ProGuard / R8 Rules

# Keep Room entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Media3 reflection classes
-keep class androidx.media3.** { *; }

# Keep Coil classes
-keep class coil.** { *; }

# Keep JSON models & reflection objects
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep line numbers for crash reporting
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
