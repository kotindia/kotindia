// demo-app:shared — Compose Multiplatform KMP module
//
// GUARDRAILS (AC2 — merge blocker if violated):
// - NO alias(libs.plugins.vanniktech.publish) — demo-app is never published to Maven Central
// - NO alias(libs.plugins.kover) — demo-app excluded from coverage (Kover scoped to :core only)
// - NO alias(libs.plugins.dokka) — demo-app classes are not part of public API docs
// - NO alias(libs.plugins.binary.compat.validator) — binary-compat-validator applied only in :core;
//     demo-app subprojects are naturally invisible to apiValidation (no ignoredProjects needed)
// - NO explicitApi() — demo-app is not a library; all code is internal-by-convention
//
// enforceZeroDeps note: that task in :core scans :core's *Implementation configurations only.
// This module's Compose deps are in a separate Gradle project and do NOT appear in :core's
// configuration graph — no changes to enforceZeroDeps needed (AC2 §enforceZeroDeps scope).

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    // ktlint + detekt intentionally omitted — demo code is not library code
}

kotlin {
    // NO explicitApi() — demo-app is not a library
    jvmToolchain(17)

    androidTarget()
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()
    // iosX64 (Intel-Mac iOS simulator) intentionally omitted — Compose Multiplatform 1.11.0-rc01
    // does not publish iosX64 artifacts on Maven Central. Apple Silicon Macs use iosSimulatorArm64
    // natively; Intel Macs are end-of-life for iOS development. Re-add when JetBrains restores
    // iosX64 publications, OR drop entirely when CMP stable lands. Brain: dec_20260506_001754_221ed0

    // iOS framework configuration — direct .framework embedding (Marcus arch ruling, OQ-1 RESOLVED).
    // KGP generates shared.framework; Xcode project references it via SRCROOT-relative path:
    //   $(SRCROOT)/../../demo-app/shared/build/bin/iosSimulatorArm64/debugFramework
    // No Podfile, no CocoaPods, no pod install step.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Toggle: -Pkotindia.useMavenCore=true → consume the published artifact
            // from Maven Central instead of the in-repo :core source. Use this to
            // verify the production consumer experience (POM resolution, KMP variant
            // selection, sources/javadoc/.asc availability) end-to-end.
            //
            // Default (no flag): project(":core") — fast iteration, demo reacts to
            // unreleased :core changes, no publish round-trip.
            val useMavenCore =
                (providers.gradleProperty("kotindia.useMavenCore").orNull ?: "false")
                    .toBoolean()
            if (useMavenCore) {
                implementation("io.github.kotindia:core:0.1.0")
            } else {
                implementation(project(":core"))
            }
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }

        androidMain.dependencies {
            // androidx.activity:activity-compose resolved via compose BOM — no version needed here
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        // iosMain has no additional deps — CMP iOS target is automatic via the framework config above
    }
}

android {
    namespace = "io.github.kotindia.demo.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
