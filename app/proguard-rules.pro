# ==========================================
# Proguard / R8 Rules for CampusGO
# ==========================================

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses,Signature
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class **$$serializer {
    public static final ** INSTANCE;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}

# Modelos de Dominio y DTOs de CampusGO
-keep class com.example.campusgo.domain.model.** { *; }
-keep class com.example.campusgo.data.** { *; }

# Supabase Kt
-keep class io.github.jan.supabase.** { *; }

# Ktor Client
-keep class io.ktor.** { *; }

# Koin Dependency Injection
-keep class org.koin.** { *; }

# AndroidX Credentials / Google Sign-In
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# ExoPlayer Media3
-keep class androidx.media3.** { *; }

# Coil Image Loader
-keep class coil.** { *; }

# Room Database
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Dao

