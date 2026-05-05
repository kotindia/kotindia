# Changelog

All notable changes to KotIndia will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Multiplatform demo app (`demo-app/`) — Compose Multiplatform showcase for Android, Desktop
  (JVM), and iOS demonstrating all 16 `core` validators across 4 tabs: PII validators (with
  mask preview), public-info validators, algorithmic showcase (Verhoeff, Luhn, GSTIN checksum
  explanation), and About. Consumes `:core` via project dependency (`project(":core")`). Excluded
  from Maven Central publishing, Kover coverage, Dokka API docs, binary-compat-validator, and
  `explicitApi()` gates per PROJECT_PLAN guardrails (AC2).
- New CI workflow `.github/workflows/demo-app.yml`: builds Android + JVM Desktop on
  `ubuntu-latest`, links iOS framework on `macos-latest`. Failures block PRs but do NOT block
  `release.yml` (tag-triggered). Includes AC10 exclusion verification steps (apiCheck, koverVerify,
  publishToMavenLocal artifact guard).
- iOS Xcode project skeleton deferred to Slice 13b. The shared framework builds via Gradle
  (`./gradlew :demo-app:shared:linkDebugFrameworkIosSimulatorArm64`). Swift source stubs
  (`iOSApp.swift`, `ContentView.swift`) committed to `demo-app/iosApp/iosApp/`. Setup steps
  documented in `demo-app/iosApp/README.md`. Direct `.framework` embedding per Marcus arch ruling
  (OQ-1 RESOLVED) — no CocoaPods.
- Slice 13b: iOS Xcode project skeleton committed (`demo-app/iosApp/iosApp.xcodeproj/`).
  Direct `.framework` embedding, no CocoaPods. Scheme `iosApp`, deployment target iOS 15.0,
  Swift 5.0. Targets iPhone 16e simulator (arm64). Framework Search Path resolves via
  `$(SRCROOT)/../shared/build/bin/iosSimulatorArm64/debugFramework`. Pre-compile Gradle run
  script rebuilds the KMP framework automatically on each Xcode build. Verified:
  `xcodebuild -list` lists target `iosApp` + scheme `iosApp`; `xcodebuild ... build` succeeds.
- `kotindia.useMavenCore` Gradle property in `demo-app/shared/build.gradle.kts`. Pass
  `-Pkotindia.useMavenCore=true` to swap the in-repo `project(":core")` dependency for the
  published `io.github.kotindia:core:0.1.0` artifact from Maven Central. Validates the
  production consumer experience end-to-end (POM resolution, KMP variant selection,
  sources/javadoc/.pom.asc availability, transitive dependency closure). Default unchanged
  (`project(":core")`) so day-to-day iteration loop stays fast. Verified: Maven mode resolves
  with only `org.jetbrains.kotlin:kotlin-stdlib` transitive — zero-dep guarantee held in the
  published artifact.

### Changed

- `demo-app/shared` no longer declares the `iosX64` KMP target. Compose Multiplatform 1.11.0-rc01
  does not publish `iosX64` artifacts on Maven Central (HTTP 404 on `material3-uikitx64`,
  `ui-uikit-uikitx64`, etc. for that version). Apple Silicon Macs use `iosSimulatorArm64`
  natively; Intel Macs are end-of-life for iOS development. The `:core` library still
  publishes `iosX64` (only Compose Multiplatform's UI artifacts are missing — `:core` has
  zero runtime deps so it is unaffected). Re-add to `demo-app/shared` when JetBrains restores
  iosX64 publications, or when CMP stable lands.
- Gradle daemon JVM heap raised from 1 GB default to `-Xmx4g -XX:MaxMetaspaceSize=1g` in
  `gradle.properties`. Required for Compose Multiplatform Android dex merging
  (`mergeExtDexDebug`) once the demo-app pulls in Compose runtime/foundation/material3/ui
  transitive bytecode — the standard CMP setting.
- `org.gradle.jvmargs` includes `-XX:-HeapDumpOnOutOfMemoryError` to suppress on-disk heap
  dumps when the JVM hits OOM. Heap dumps would otherwise drop ~1 GB `*.hprof` files at the
  repo root containing JVM memory snapshots — a credential-leak vector for the Gradle daemon
  if it had loaded Sonatype/GPG creds from `~/.gradle/gradle.properties`.

### Fixed

- iOS demo app crashed at launch with `IllegalStateException` from
  `androidx.compose.ui.uikit.PlistSanityCheck`. The runtime check pre-dates Xcode 26's
  `INFOPLIST_KEY_*_Generation` flags and false-positives when the generated `Info.plist`
  doesn't contain the expected `UILaunchScreen` / `UIApplicationSceneManifest` dicts.
  Resolved by passing `enforceStrictPlistSanityCheck = false` via
  `ComposeUIViewController(configure = { ... })` in `MainViewController.kt` — the
  JetBrains-documented escape hatch.
- iOS Xcode build failed with "User Script Sandboxing Enabled in Xcode Project" under
  Xcode 26's default `ENABLE_USER_SCRIPT_SANDBOXING=YES`. The pre-compile Run Script Phase
  shells out to Gradle, which needs to read repo files outside `SRCROOT`. Set
  `ENABLE_USER_SCRIPT_SANDBOXING=NO` in both Debug and Release build configurations of the
  iOS Xcode project — standard fix for KMP/CMP iOS projects with Gradle Run Script Phases.

### Security

- `.gitignore` now covers `*.hprof`. Heap dumps are full JVM memory snapshots and could leak
  secrets the Gradle daemon had loaded (Sonatype/GPG credentials from
  `~/.gradle/gradle.properties`). Combined with `-XX:-HeapDumpOnOutOfMemoryError` (above)
  this closes both the prevention (no auto-dump) and defense-in-depth (gitignore catches
  any manual dumps) paths.

## [0.1.0] - 2026-05-05

### Added

- `ValidationResult` sealed interface (`Valid` data object, `Invalid` data class with `reason` field) — public API contract for all validators
- `InvalidReason` enum (`EMPTY`, `WRONG_LENGTH`, `INVALID_FORMAT`, `INVALID_CHECKSUM`, `INVALID_PREFIX`, `INVALID_CATEGORY`)
- `internal/` subpackage scaffolded for upcoming algorithm helpers (`Verhoeff`, `Luhn`, `GstinChecksum`)
- `Mobile` object validator with `validate(String): ValidationResult`, `isValid(String): Boolean`, `format(String, Boolean): String`, `mask(String, Int, Int, Char): String`. Validates Indian 10-digit mobile numbers (prefix 6/7/8/9). Accepts E.164 (+91) and leading-zero forms.
- `Pincode` object validator with `validate(String): ValidationResult`, `isValid(String): Boolean`, `format(String): String`. Validates Indian 6-digit Postal Index Numbers (first digit 1-9). No `mask()` method — pincodes are not PII (pincodes are public address info).
- `IFSC` object validator with `validate(String): ValidationResult`, `isValid(String): Boolean`, `format(String): String`. Validates RBI 11-character Indian Financial System Codes (`[A-Z]{4}0[A-Z0-9]{6}`). `format()` normalizes to canonical uppercase (no separators — IFSC has no display separator convention). No `mask()` method per PII-only policy (IFSC is a public bank routing code).
- `PAN` object validator with `validate(String): ValidationResult`, `isValid(String): Boolean`, `format(String): String`, `mask(String, Int, Int, Char): String`. Validates Indian Permanent Account Number (`[A-Z]{5}[0-9]{4}[A-Z]`, 4th-char category rules for P/C/H/A/B/G/J/L/F/T). First use of `InvalidReason.INVALID_CATEGORY`. `mask()` defaults to last-4-visible per standard PII masking policy (PAN is a Private government ID per §3.5). Middle-4-digits-visible pattern available via `PAN.mask(value, 5, 1)` — documented in KDoc.
- Verhoeff checksum helper (`internal object Verhoeff`) with D5 dihedral group lookup tables (`d`, `p`, `inv`). Transcribed from Wikipedia Verhoeff algorithm spec; cross-validated against mastermunj/format-utils JS implementation (MIT) using 110 generated prefixes — all outputs matched. Used by `Aadhaar` and `AadhaarVID`. Not part of public API.
- `Aadhaar` object validator — 12-digit UIDAI Aadhaar number with Verhoeff checksum. First digit must be 2-9 (`INVALID_PREFIX` otherwise). `validate()`, `isValid()`, `format()` (UIDAI spaced `"1234 5678 9012"`), `mask()` (last-4-visible by default; UIDAI exact spaced form documented in KDoc as caller-composition). Ships with 110 externally-sourced reference vectors (cross-validated against mastermunj/format-utils JS) and property tests for round-trip, single-digit corruption, and adjacent-transposition detection.
- `AadhaarVID` object validator — 16-digit UIDAI Virtual ID with same Verhoeff checksum and first-digit 2-9 rule. `format()` output: `"1234 5678 9012 3456"` (4 groups of 4). Same `mask()` contract as `Aadhaar`.
- Luhn checksum helper (`internal object Luhn`) — mod-10 algorithm for IMEI validation. Provides `computeCheckDigit` and `isChecksumValid`. Transcribed from Wikipedia Luhn algorithm spec (https://en.wikipedia.org/wiki/Luhn_algorithm); cross-validated against npm `luhn` package (MIT, https://www.npmjs.com/package/luhn) — luhn.validate() used as external verifier on 110 generated IMEIs, zero divergences. Not part of public API.
- `IMEI` object validator — 15-digit International Mobile Equipment Identity with Luhn mod-10 checksum. `validate()`, `isValid()`, `format()` (canonical 15-digit, no separator — ITU-T E.118 has no mandatory display format), `mask()` (last-4-visible by default). IMEI is Device PII per §3.5 — always mask before logging or display. Ships with 110 externally-sourced reference vectors (verified by npm luhn.validate()) and property tests for round-trip, adjacent-transposition detection, and single-digit corruption.
- GSTN base-36 checksum helper (`internal object GstinChecksum`) — weighted-sum modular arithmetic algorithm for GSTIN validation. Provides `computeCheckChar` and `isChecksumValid`. Transcribed from tk120404/gst JS implementation (https://github.com/tk120404/gst); cross-validated against mastermunj/format-utils TS (https://github.com/mastermunj/format-utils) — both implementations agree on all 114 generated test vectors covering state codes 01–38, zero divergences. Not part of public API.
- `GSTIN` object validator — 15-character Goods and Services Tax Identification Number. `validate()`, `isValid()`, `format()` (canonical 15-char uppercase, no separator). Validates: 2-digit state code (01–38, including legacy codes 25 and 28, and post-2019 Ladakh code 38), embedded PAN structure with 4th-character category rule (P/C/H/A/B/G/J/L/F/T), entity number, literal 'Z', and GSTN base-36 checksum character. NO `mask()` — GSTIN is a public business ID, not PII (PROJECT_PLAN §3.5). Ships with 114 externally-sourced reference vectors (tk120404/gst + mastermunj cross-validated) and property tests for round-trip, check-char corruption, and single-char corruption.
- `UAN` object validator with `validate(String): ValidationResult`, `isValid(String): Boolean`, `format(String): String`, `mask(String, Int, Int, Char): String`. Validates 12-digit EPFO Universal Account Number (format-only; no public checksum). Rejects non-ASCII digits including Devanagari ('०'..'९'). `mask()` defaults to last-4-visible per UAN Private ID categorization (§3.5).
- `Passport` object validator with `validate(String): ValidationResult`, `isValid(String): Boolean`, `format(String): String`, `mask(String, Int, Int, Char): String`. Validates Indian Passport number (`[A-Z]\d{7}`, 1 letter + 7 digits per MEA spec). No checksum — structural validation only. `mask()` defaults to last-4-visible per Passport PII categorization (Private government ID, §3.5).
- `ESIC` object validator with `validate(String): ValidationResult`, `isValid(String): Boolean`, `format(String): String`, `mask(String, Int, Int, Char): String`. Validates 17-digit Employee State Insurance Corporation insurance number (format-only; no public checksum). Uses ASCII-range digit check so Devanagari and other Unicode digits are correctly rejected. `mask()` defaults to last-4-visible per ESIC Private ID categorization (health PII, §3.5).
- `CIN` object validator with `validate(String): ValidationResult`, `isValid(String): Boolean`, `format(String): String`. Validates MCA 21-character Company Identification Number against structured pattern `[LU] + 5-digit industry code + 2-letter state code + 4-digit year + 3-letter company class + 6-digit registration`. Position 0 must be `L` (Listed) or `U` (Unlisted); non-L/U surfaces as `INVALID_PREFIX` before the regex check. Structural validation only — state-code semantics not enforced (MCA list is mutable). No `mask()` method per PII-only policy (CIN is a public business identifier searchable on the MCA portal).
- `VPA` object validator with `validate(String): ValidationResult`, `isValid(String): Boolean`, `format(String): String`. Validates UPI Virtual Payment Address structural format (`username@psp`, max 50 characters per NPCI spec). Normalization is trim + lowercase only — internal whitespace is NOT stripped, so spaces inside a VPA surface as `INVALID_FORMAT`. PSP allowlist not enforced (NPCI handle list is mutable). No `mask()` method per PII-only policy (VPA is a public payment handle, shared openly to receive money).
- `TAN` object validator with `validate(String): ValidationResult`, `isValid(String): Boolean`, `format(String): String`. Validates Income Tax Department Tax Deduction Account Number against `^[A-Z]{4}[0-9]{5}[A-Z]$` (10 characters, no checksum). No `INVALID_PREFIX` — TAN has no closed-position rule like PAN's 4th-character category. No `mask()` method per PII-only policy (TAN is a public business identifier per §3.5).
- `DL` validator (`object DL`) with `validate`, `isValid`, `format`, `mask`. Validates post-2013 Sarathi Driving License format: 2-letter state code (38 valid codes including OR legacy and LA post-2019) + 1-2 digit RTO code + 4-digit year + 7-digit serial. Pre-2013 state-specific formats explicitly NOT supported — inputs in pre-2013 format return `INVALID_FORMAT` or `WRONG_LENGTH` (R4 risk mitigation, documented in KDoc). `normalize()` strips hyphens in addition to whitespace (Digilocker DL strings use hyphens as separators). `mask()` defaults to last-4-visible per DL Private government ID categorization (PROJECT_PLAN §3.5). Includes `DlOutOfScopeTest.kt` documenting rejected pre-2013 formats and unsupported state-prefix patterns.
- `VehicleRC` object validator with `validate(String): ValidationResult`, `isValid(String): Boolean`, `format(String): String`. Validates Indian vehicle registration plate format `[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}` (8–11 chars). State code validated against the same closed 38-entry MoRTH-assigned set used by `DL` (includes OR legacy Odisha; excludes non-canonical UT — active Uttarakhand code is UK). Accepts space- and hyphen-separated forms; `format()` normalizes to uppercase canonical no separator. No `mask()` per PII-only policy (vehicle plates are publicly visible).

- README.md fully rewritten: badges (Maven Central, Build, License, KMP), one-paragraph pitch,
  install snippet (Gradle Kotlin DSL), 16-validator quick reference table with mask() column,
  6 code examples (Aadhaar, PAN, GSTIN, Mobile, IFSC, ValidationResult pattern matching),
  API docs link, contributing section, Apache 2.0 license.
- Dokka 2.2.0 HTML generation configured in `core/build.gradle.kts` (module name `kotindia-core`,
  source links to `main` branch on GitHub, `suppressInheritedMembers = true`,
  `reportUndocumented = true` KDoc completeness gate). Output: `core/build/dokka/html/`. Not
  committed to repo (covered by `build/` gitignore).
- `.github/workflows/docs.yml`: new CI workflow generates Dokka HTML on every push/PR to main
  and uploads as artifact `dokka-html` (7-day retention). gh-pages deploy gated to Slice 12.
- Maven Central publishing configuration: Vanniktech `mavenPublishing` block in `core/build.gradle.kts`
  with POM metadata (groupId `io.github.kotindia`, artifactId `core`, version `0.1.0`), GPG signing
  via in-memory keys, `automaticRelease = true` targeting Sonatype Central Portal. Upgraded
  `com.vanniktech.maven.publish` from 0.13.0 to 0.33.0 for Gradle 9 compatibility (0.13.0 uses
  removed `org.gradle.util.VersionNumber`). Tag-triggered `release.yml` workflow publishes to Maven
  Central then deploys Dokka HTML to gh-pages (gated on successful publish). `docs/build/publishing.md`
  8-step credential setup checklist for maintainer.

### Changed

- Kover line coverage threshold raised from 0% to 100% (enforced via koverVerify in CI).

### Fixed

- `Mobile` and `Pincode` validators now reject non-ASCII digits (including Devanagari `'०'..'९'`). Previously they used Kotlin's Unicode-aware `isDigit()` which accepts Unicode `Nd` digits — an all-Devanagari input of the correct length would silently validate as `Valid`. They now use an ASCII-range guard (`it !in '0'..'9'`) consistent with `ESIC` and `UAN`. Added explicit Devanagari-digit rejection tests to both validators.

[unreleased]: https://github.com/kotindia/kotindia/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/kotindia/kotindia/releases/tag/v0.1.0
