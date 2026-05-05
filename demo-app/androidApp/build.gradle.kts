// demo-app:androidApp — Android Application entry point
//
// GUARDRAILS (AC2 — merge blocker if violated):
// - NO alias(libs.plugins.vanniktech.publish) — demo-app is never published to Maven Central
// - NO alias(libs.plugins.kover) — excluded from coverage scope
// - NO alias(libs.plugins.dokka) — not a library
// - NO explicitApi() — demo-app is not a library

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.kotindia.demo.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.kotindia.demo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(project(":demo-app:shared"))
    // activity-compose: provides ComponentActivity.setContent { } and enableEdgeToEdge()
    implementation(libs.androidx.activity.compose)
}
