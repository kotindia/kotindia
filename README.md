# KotIndia

Canonical Indian developer toolkit for Kotlin Multiplatform — validators, formatters,
and maskers for PAN, Aadhaar, GSTIN, IFSC, UPI, and 12 more Indian system codes.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotindia/core.svg)](https://central.sonatype.com/artifact/io.github.kotindia/core) [![Build](https://github.com/kotindia/kotindia/actions/workflows/ci.yml/badge.svg)](https://github.com/kotindia/kotindia/actions/workflows/ci.yml) [![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE) [![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)

---

## What is KotIndia?

KotIndia fills a verified gap in the Kotlin and Android ecosystem: as of 2026, no comprehensive,
well-tested Kotlin Multiplatform library exists on Maven Central for validating and formatting
Indian system codes. KotIndia provides format validation, checksum verification (Verhoeff for
Aadhaar, Luhn for IMEI, GSTN base-36 for GSTIN), formatting, and PII masking for 16 Indian
identifiers — all in a single, zero-runtime-dependency, pure Kotlin Multiplatform library licensed
under Apache 2.0.

Version 0.1.0 ships the `core` module: 16 validators, formatters, and maskers for Android, JVM,
and iOS targets. Zero runtime dependencies — pure Kotlin stdlib only. 100% line coverage on `core`.
The `compose` UI component module and the `upi` deep-link module follow in Phase 2 and Phase 3
after `core` has adoption signal.

---

## Install

Add the dependency in your Gradle Kotlin DSL build file:

```kotlin
dependencies {
    implementation("io.github.kotindia:core:0.1.0")
}
```

Make sure `mavenCentral()` is in your `repositories` block
([Gradle docs](https://docs.gradle.org/current/userguide/declaring_repositories.html)):

```kotlin
repositories {
    mavenCentral()
}
```

---

## Validators — quick reference

| Validator | Format | Checksum | `mask()`? |
|-----------|--------|----------|-----------|
| `Mobile` | 10 digits, prefix 6/7/8/9 | None | YES |
| `Pincode` | 6 digits, first digit 1–9 | None | NO |
| `IFSC` | `[A-Z]{4}0[A-Z0-9]{6}` | None | NO |
| `PAN` | `[A-Z]{5}[0-9]{4}[A-Z]`, 4th-char category | None | YES |
| `Aadhaar` | 12 digits, first digit 2–9 | Verhoeff | YES |
| `AadhaarVID` | 16 digits, first digit 2–9 | Verhoeff | YES |
| `IMEI` | 15 digits | Luhn | YES |
| `GSTIN` | 15 chars: state + PAN + entity + Z + check | GSTN base-36 | NO |
| `UAN` | 12 digits | None | YES |
| `CIN` | 21 chars: L/U + industry + state + year + class + reg | Structural | NO |
| `VPA` | `username@psp`, max 50 chars | None | NO |
| `DL` | State(2) + RTO(1–2) + year(4) + serial(7) | None | YES |
| `VehicleRC` | `[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}` | None | NO |
| `Passport` | `[A-Z]\d{7}` | None | YES |
| `ESIC` | 17 digits | None | YES |
| `TAN` | `[A-Z]{4}[0-9]{5}[A-Z]` | None | NO |

---

## Code examples

### Aadhaar — validate, format, mask

```kotlin
import io.github.kotindia.*

Aadhaar.validate("234567890124")  // ValidationResult.Valid
Aadhaar.isValid("234567890124")   // true
Aadhaar.format("234567890124")    // "2345 6789 0124"
Aadhaar.mask("234567890124")      // "XXXXXXXX0124"
```

### PAN — validate, format, mask

```kotlin
import io.github.kotindia.*

PAN.validate("ABCPE1234F")        // ValidationResult.Valid
PAN.validate("ABCZE1234F")        // ValidationResult.Invalid(reason=INVALID_CATEGORY)
PAN.format("abcpe1234f")          // "ABCPE1234F"
PAN.mask("ABCPE1234F")            // "XXXXXX234F"
```

### GSTIN — validate, format

```kotlin
import io.github.kotindia.*

GSTIN.validate("27AAPFU0939F1ZV") // ValidationResult.Valid
GSTIN.format("27aapfu0939f1zv")   // "27AAPFU0939F1ZV"
```

### Mobile — validate, format with country code, mask

```kotlin
import io.github.kotindia.*

Mobile.validate("9876543210")                              // ValidationResult.Valid
Mobile.isValid("+91 98765 43210")                          // true
Mobile.format("9876543210", withCountryCode = true)        // "+91 98765 43210"
Mobile.mask("9876543210")                                  // "XXXXXX3210"
```

### IFSC — validate, format

```kotlin
import io.github.kotindia.*

IFSC.validate("HDFC0000001")      // ValidationResult.Valid
IFSC.format("hdfc0000001")        // "HDFC0000001"
```

### ValidationResult — pattern matching

```kotlin
import io.github.kotindia.*

when (val result = Aadhaar.validate(userInput)) {
    is ValidationResult.Valid -> println("Valid Aadhaar")
    is ValidationResult.Invalid -> when (result.reason) {
        InvalidReason.EMPTY -> println("Input is empty")
        InvalidReason.WRONG_LENGTH -> println("Must be 12 digits")
        InvalidReason.INVALID_PREFIX -> println("First digit must be 2–9")
        InvalidReason.INVALID_CHECKSUM -> println("Verhoeff checksum failed")
        else -> println("Invalid: ${result.reason}")
    }
}
```

---

## API documentation

Full API documentation (KDoc): https://kotindia.github.io/kotindia

---

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) for the code style
guide, branch conventions, and the PR review checklist. All PRs must pass CI (build + test + lint +
100% coverage) before review.

---

## License

Apache License 2.0. See [LICENSE](LICENSE).
