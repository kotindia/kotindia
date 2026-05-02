# Changelog

All notable changes to KotIndia will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

### Changed

- Kover line coverage threshold raised from 0% to 100% (enforced via koverVerify in CI).
