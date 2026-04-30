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

### Changed

- Kover line coverage threshold raised from 0% to 100% (enforced via koverVerify in CI).
