import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Keys come from the root .env file (never committed) or from CI environment variables.
fun env(key: String): String {
    val f = rootProject.file(".env")
    val p = Properties()
    if (f.exists()) f.inputStream().use { p.load(it) }
    return p.getProperty(key) ?: System.getenv(key) ?: ""
}

android {
    namespace = "com.ronix.learning"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ronix.learning"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SUPABASE_URL", "\"${env("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${env("SUPABASE_PUBLISHABLE_KEY")}\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { release { isMinifyEnabled = false } }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.vm)
    implementation(libs.coil)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.okhttp)
    implementation(libs.serialization.json)
    testImplementation("junit:junit:4.13.2")
}
