plugins {
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id ("org.jetbrains.kotlin.plugin.serialization")version "1.9.23"
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.dacs31"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dacs31"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Không cần cấu hình thêm
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
    }
}

dependencies {
    implementation ("com.google.dagger:hilt-android:2.48")
//    kapt ("com.google.dagger:hilt-android-compiler:2.48")
//    alias(libs.plugins.kotlin.kapt)
    kapt ("com.google.dagger:hilt-android-compiler:2.48")
    implementation ("androidx.hilt:hilt-navigation-compose:1.1.0")
    // Coil để hiển thị hình ảnh QR
    implementation ("io.coil-kt:coil-compose:2.4.0")

    // Ktor để gọi API
    implementation ("io.ktor:ktor-client-core:2.3.7")
    implementation ("io.ktor:ktor-client-cio:2.3.7")
    implementation ("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation ("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // Kotlinx Serialization
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    //coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.1")

    implementation("androidx.compose.runtime:runtime-livedata:1.7.4")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")

    // Mapbox Maps SDK
    implementation("com.mapbox.maps:android:11.4.0")

    // Thêm Mapbox Turf để sử dụng TurfMeasurement
    implementation("com.mapbox.mapboxsdk:mapbox-sdk-turf:6.8.0")

    // OkHttp để gọi API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Jetpack Compose
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)

    // Firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database.ktx)

    // Google Sign-In
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Testing
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.animation.core.lint)
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}