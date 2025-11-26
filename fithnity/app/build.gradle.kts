plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "tn.esprit.fithnity"
    compileSdk = 36

    defaultConfig {
        applicationId = "tn.esprit.fithnity"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose MapTiler API Key to the app
        buildConfigField("String", "MAPTILER_API_KEY", project.findProperty("MAPTILER_API_KEY") as String? ?: "\"\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Retrofit for REST API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp logging interceptor for debugging
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Jetpack Compose Lifecycle ViewModel integration
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    // Core Modern Compose (in case needed)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.1")
    // Material Icons Extended for modern icons
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.5")
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    // MapLibre for maps
    implementation("org.maplibre.gl:android-sdk:11.0.0")
    // Location services
    implementation("com.google.android.gms:play-services-location:21.0.1")
}

// Google Services plugin removed - using Twilio OTP instead