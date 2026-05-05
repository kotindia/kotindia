// demo-app:desktopApp — JVM Desktop Application entry point
//
// GUARDRAILS (AC2 — merge blocker if violated):
// - NO alias(libs.plugins.vanniktech.publish) — demo-app is never published to Maven Central
// - NO alias(libs.plugins.kover) — excluded from coverage scope
// - NO alias(libs.plugins.dokka) — not a library
// - NO explicitApi() — demo-app is not a library

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":demo-app:shared"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "io.github.kotindia.demo.desktop.MainKt"

        nativeDistributions {
            // Packaging scaffolded for future use — not validated in v0.1.0 (out of scope per PRD).
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotIndia Demo"
            packageVersion = "0.1.0"
        }
    }
}
