# Keep Room entities
-keep class com.focuslock.app.data.db.** { *; }
-dontwarn org.jetbrains.annotations.**

# Room generates <Database>_Impl reflectively from the abstract class.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# BiometricPrompt + fragment host are resolved through the support library.
-keep class androidx.biometric.** { *; }

# lifecycle-runtime-compose 2.8.x resolves its own LocalLifecycleOwner through
# compose-ui 1.6.8's provider; the two CompositionLocals are only unified in
# Compose UI 1.7+. R8 strips that provider, so every collectAsStateWithLifecycle
# call throws "CompositionLocal LocalLifecycleOwner not present" in release.
# Drop this once the Compose BOM moves to 1.7+.
-keep class androidx.compose.ui.platform.AndroidCompositionLocals_androidKt** { *; }
