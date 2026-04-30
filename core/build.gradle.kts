/*
 * Copyright 2026 The KotIndia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.binary.compat.validator)
}

// ---------------------------------------------------------------------------
// Kotlin Multiplatform configuration
// ---------------------------------------------------------------------------

kotlin {
    // Strict explicit API mode — compile error on any missing visibility/return type.
    // Required for library publishing: every public symbol must be intentional.
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    explicitApi()

    // Java toolchain — JDK 17 (LTS, AGP 8.7 minimum requirement).
    // Foojay resolver (settings.gradle.kts) auto-provisions JDK 17 on CI.
    jvmToolchain(17)

    // ---------------------------------------------------------------------------
    // KMP targets — Phase 1 (Slice 1): JVM + iOS only.
    // Android target deferred to Slice 2.
    // JS / WASM / Linux deferred to v0.2.
    // ---------------------------------------------------------------------------
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // HARD RULE: zero runtime dependencies.
            // If you need to add an implementation dep here, STOP and ask first.
            // Any such dep will be caught by the enforceZeroDeps task below.
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            // kotest-property for algorithmic property tests (Verhoeff, Luhn, GSTIN).
            // NEVER add kotest-runner-junit5 — JVM-only, breaks K/N silently.
            implementation(libs.kotest.property)
        }
    }
}

// ---------------------------------------------------------------------------
// Zero-dependency enforcement (Marcus D6)
// Dynamically scans ALL *Implementation configurations — covers new KMP targets
// added in future slices without manual task updates.
// ---------------------------------------------------------------------------

tasks.register("enforceZeroDeps") {
    group = "verification"
    description = "Fail build if core module declares any external runtime dependencies."
    doLast {
        val banned =
            configurations
                .filter { cfg ->
                    cfg.name.endsWith("Implementation") &&
                        !cfg.name.contains("Test", ignoreCase = true)
                }.flatMap { cfg ->
                    cfg.dependencies.filter { dep ->
                        dep.group != null &&
                            !dep.group!!.startsWith("org.jetbrains.kotlin") &&
                            !dep.group!!.startsWith("org.jetbrains.kotlinx")
                    }
                }
        if (banned.isNotEmpty()) {
            error(
                "Zero-dep violation in core module: ${banned.map { "${it.group}:${it.name}" }}",
            )
        }
    }
}

tasks.named("check") { dependsOn("enforceZeroDeps") }

// ---------------------------------------------------------------------------
// Kover coverage configuration
// Threshold: 0 for Slice 1 (no source yet). Flip to 100 when first validator lands.
// ---------------------------------------------------------------------------

kover {
    reports {
        filters {
            excludes {
                // Exclude generated / build-config classes if any appear
                classes("*.BuildConfig")
            }
        }
        verify {
            rule("Line coverage threshold") {
                // 100% line coverage required from Slice 3 onward (per OQ-6 resolution)
                // Kover 0.9.x: bound{} uses minValue only (metric/aggregation removed from DSL).
                // Default metric = LINE, default aggregation = COVERED_PERCENTAGE.
                minBound(100)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ktlint configuration
// ---------------------------------------------------------------------------

ktlint {
    version.set("1.5.0")
    android.set(false)
    outputToConsole.set(true)
    enableExperimentalRules.set(false)
    filter {
        exclude("**/generated/**")
    }
}

// ---------------------------------------------------------------------------
// detekt configuration
// ---------------------------------------------------------------------------

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}
