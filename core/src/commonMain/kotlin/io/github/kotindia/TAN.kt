// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * Validator and formatter for Indian Tax Deduction Account Numbers (TAN).
 *
 * TAN is a 10-character alphanumeric code issued by the Indian Income Tax Department
 * to entities required to deduct or collect tax at source (TDS/TCS). It is mandatory
 * on all TDS/TCS returns, certificates, and challans. TANs are issued and administered
 * under Section 203A of the Income Tax Act, 1961.
 *
 * Format: `[A-Z]{4}[0-9]{5}[A-Z]` — 10 characters total
 * - Characters 1–4: jurisdiction code (uppercase letters, typically city/region abbreviation)
 * - Characters 5–9: five-digit sequential number assigned by the Income Tax Department
 * - Character 10: single uppercase letter (structural check character; no public algorithm exists)
 *
 * **No checksum:** The Income Tax Department does not publish a checksum algorithm for TAN.
 * Validation is structural (format) only.
 *
 * **No `mask()`:** TAN is a public business identifier — entities publish their TAN on invoices
 * and tax certificates. No masking is required or provided per KotIndia PII policy (§3.5).
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw uppercase: `"MUMD12345A"`
 * - Lowercase: `"mumd12345a"` — normalised to uppercase
 * - Mixed case: `"MumD12345a"` — normalised to uppercase
 * - With spaces: `"MUMD 12345 A"` — internal whitespace stripped
 * - Whitespace padded: `" MUMD12345A "` — leading/trailing whitespace trimmed
 *
 * This object is stateless and thread-safe.
 *
 * @suppress ClassName TAN is a universal acronym — preserved for consumer readability.
 * @sample io.github.kotindia.samples.tanSample
 */
@Suppress("ClassName") // TAN is a universal acronym — preserved for consumer readability
public object TAN {
    private const val EXPECTED_LENGTH = 10
    private val TAN_REGEX = Regex("^[A-Z]{4}[0-9]{5}[A-Z]$")

    /**
     * Validates an Indian Tax Deduction Account Number (TAN).
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers copy-paste with spaces)
     * 3. Uppercase all characters
     *
     * After normalisation, checks are applied in order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if length is not 10
     * - [InvalidReason.INVALID_FORMAT] if the string does not match `^[A-Z]{4}[0-9]{5}[A-Z]$`
     *
     * The regex encodes all structural constraints simultaneously:
     * - Characters 1–4 must be `[A-Z]`
     * - Characters 5–9 must be `[0-9]`
     * - Character 10 must be `[A-Z]`
     *
     * **Note:** Hyphens and other special characters are not stripped. `"MUMD-2345A"`
     * normalises to a 10-character string but fails regex and returns [InvalidReason.INVALID_FORMAT].
     *
     * @param value Raw TAN string in any common input form.
     * @return [ValidationResult.Valid] if the code is a valid TAN,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.tanValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)

        if (normalized.isBlank()) {
            return ValidationResult.Invalid(InvalidReason.EMPTY)
        }
        if (normalized.length != EXPECTED_LENGTH) {
            return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        }
        if (!TAN_REGEX.matches(normalized)) {
            return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        }
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw TAN string.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.tanIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a TAN to canonical form: 10-character uppercase, no separators.
     *
     * Applies the same normalisation as [validate] before formatting.
     * TAN has no published display separator convention — output is the raw
     * uppercase string: e.g. `"MUMD12345A"`.
     *
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw TAN string.
     * @return Canonical 10-character uppercase TAN string.
     * @throws IllegalArgumentException if [value] is not a valid TAN.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.tanFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "TAN.format requires a valid TAN; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    // ---------------------------------------------------------------------------
    // Private normalisation helper
    // NOT part of the public API.
    // ---------------------------------------------------------------------------

    /**
     * Normalises a raw TAN string:
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers copy-paste with spaces)
     * 3. Uppercase all characters (TAN is case-insensitive in real-world usage;
     *    canonical form is uppercase per Income Tax Department)
     */
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "").uppercase()
}
