import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}
val supabaseUrl: String = (localProperties.getProperty("SUPABASE_URL")
    ?: System.getenv("SUPABASE_URL")
    ?: "https://api.kodexti.com")
val supabaseKey: String = (localProperties.getProperty("SUPABASE_KEY")
    ?: System.getenv("SUPABASE_KEY")
    ?: "sb_publishable_zstYiRQi8ysTq5WXUL2SNP_c1EKvmOE")
val googleServerClientId: String = (localProperties.getProperty("GOOGLE_SERVER_CLIENT_ID")
    ?: System.getenv("GOOGLE_SERVER_CLIENT_ID")
    ?: "932068399848-ekb75dp64iot6ap496okojhgp4tkmt9k.apps.googleusercontent.com")

android {
    namespace = "com.example.campusgo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.campusgo"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "0.6.2-beta"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Kotlinx Serialization
  implementation(libs.kotlinx.serialization.json)

  // Supabase
  val supabaseBom = platform(libs.supabase.bom)
  implementation(supabaseBom)
  implementation(libs.supabase.postgrest)
  implementation(libs.supabase.auth)
  implementation(libs.supabase.realtime)
  implementation(libs.supabase.storage)

  // Ktor Client
  implementation(libs.ktor.client.okhttp)
  implementation(libs.ktor.client.core)

  // Coil
  implementation(libs.coil.compose)

  // Koin Dependency Injection
  implementation(libs.koin.android)
  implementation(libs.koin.androidx.compose)

  // Media3 ExoPlayer
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)

  // Firebase Cloud Messaging
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.messaging)

  // Google Sign-In via Credential Manager
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
}

