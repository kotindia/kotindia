# Changelog

All notable changes to KotIndia will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `ValidationResult` sealed interface (`Valid` data object, `Invalid` data class with `reason` field) — public API contract for all validators
- `InvalidReason` enum (`EMPTY`, `WRONG_LENGTH`, `INVALID_FORMAT`, `INVALID_CHECKSUM`, `INVALID_PREFIX`, `INVALID_CATEGORY`)
- `internal/` subpackage scaffolded for upcoming algorithm helpers (`Verhoeff`, `Luhn`, `GstinChecksum`)
