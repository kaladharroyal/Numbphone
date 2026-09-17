# ==============================================================================
# Minimal Phone — Production ProGuard & R8 Optimization Rules
# ==============================================================================

# 1. Android & Jetpack Compose Rules
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# 2. Room Persistence Keep Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# 3. Domain & Data Models
-keep class com.minimalphone.core.model.** { *; }
-keep class com.minimalphone.core.data.model.** { *; }
-keep class com.minimalphone.core.domain.UserSettings { *; }
-keep class com.minimalphone.core.domain.BackupExportResult { *; }
-keep class com.minimalphone.core.domain.BackupImportResult { *; }

# 4. Hilt & Dependency Injection
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep class dagger.hilt.** { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent
-keep class * implements dagger.hilt.internal.TestSingletonComponent

# 5. Jetpack DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences {
    public <methods>;
}

# 6. Android Services & Receivers
-keep class com.minimalphone.core.data.service.** { *; }
-keep class com.minimalphone.core.data.receiver.** { *; }

# 7. Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
