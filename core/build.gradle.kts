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

import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import java.net.URI

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.binary.compat.validator)
    // NOTE (AC2 — demo-app ABI exclusion): binary-compat-validator is applied ONLY in this
    // subproject (:core). The demo-app subprojects (shared, androidApp, desktopApp) are separate
    // Gradle projects — they are structurally invisible to apiValidation. No ignoredProjects
    // configuration is needed. Confirmed: demo-app/shared/build.gradle.kts (comment, line 8).
    alias(libs.plugins.vanniktech.publish)
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
                // Exclude documentation-only samples package — pure @sample snippets,
                // not part of the library logic, never exercised by unit tests.
                packages("io.github.kotindia.samples")
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

// ---------------------------------------------------------------------------
// Dokka 2.x HTML generation configuration (Slice 11)
// Plugin applied in root build.gradle.kts (alias(libs.plugins.dokka)).
// Output: core/build/dokka/html/ — NOT committed (covered by build/ gitignore).
// Source links point to the main branch on GitHub; update to a tag in Slice 12.
// reportUndocumented = true enforces KDoc completeness as a build gate (AC4).
// ---------------------------------------------------------------------------

dokka {
    moduleName.set("kotindia-core")
    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(file("src"))
            remoteUrl.set(URI("https://github.com/kotindia/kotindia/tree/main/core/src"))
            remoteLineSuffix.set("#L")
        }
        reportUndocumented.set(true)
        // Exclude documentation-only samples package from the public API docs.
        // It is internal by convention (all functions are `internal`) but Dokka
        // would still traverse it. Suppressing keeps the generated index clean.
        perPackageOption {
            matchingRegex.set("io\\.github\\.kotindia\\.samples.*")
            suppress.set(true)
        }
    }
}

// ---------------------------------------------------------------------------
// Maven Central publishing — Vanniktech 0.33.0 (Sonatype Central Portal)
// Credentials are NEVER hardcoded here. They come from:
//   - Local dev: ~/.gradle/gradle.properties (ORG_GRADLE_PROJECT_* vars)
//   - CI (release.yml): GitHub Actions secrets injected as env vars
// Actual publish is gated by Sandeep tagging v0.1.0. See docs/build/publishing.md.
// Reference: https://vanniktech.github.io/gradle-maven-publish-plugin/central/
// ---------------------------------------------------------------------------

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    // Sign publications only when GPG credentials are present.
    // - Local dev publishToMavenLocal works without GPG.
    // - CI's AC10 exclusion guard (Demo App workflow) runs publishToMavenLocal
    //   without GPG to enumerate published artifacts, NOT to publish for real.
    // - release.yml injects ORG_GRADLE_PROJECT_signingInMemoryKey from secrets;
    //   that path enables signing and uploads signed artifacts to Sonatype.
    val hasSigningKey =
        providers.gradleProperty("signingInMemoryKey").orNull?.isNotBlank() == true
    if (hasSigningKey) {
        signAllPublications()
    }

    coordinates("io.github.kotindia", "core", "0.1.0")

    pom {
        name.set("KotIndia Core")
        description.set(
            "Canonical Indian developer toolkit for Kotlin Multiplatform — " +
                "validators, formatters, and masking utilities for PAN, Aadhaar, " +
                "GSTIN, IFSC, Mobile, and more.",
        )
        url.set("https://github.com/kotindia/kotindia")

        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("kotindia")
                name.set("KotIndia")
                url.set("https://github.com/kotindia")
            }
        }

        scm {
            connection.set("scm:git:https://github.com/kotindia/kotindia.git")
            developerConnection.set("scm:git:ssh://git@github.com/kotindia/kotindia.git")
            url.set("https://github.com/kotindia/kotindia/tree/main")
        }
    }

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = true,
        ),
    )
}
