// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * Validator and formatter for Indian Postal Index Numbers (PIN codes).
 *
 * A valid Indian PIN code is a 6-digit number where the first digit is 1–9 (never 0).
 * No checksum exists for Indian PIN codes — validation is format-only.
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw 6 digits: `"560001"`
 * - India Post display form (space after digit 3): `"560 001"`
 * - Whitespace-padded: `" 560001 "`
 * - Multiple internal spaces: `"560  001"` — all internal whitespace is stripped
 *
 * PIN codes are not PII (they are public address information) — no `mask()` method is
 * provided. See the KotIndia API design rationale for details.
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.pincodeSample
 */
public object Pincode {
    private const val EXPECTED_LENGTH = 6

    /**
     * Validates an Indian PIN code.
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip **all** internal whitespace (supports India Post display form `XXX XXX`
     *    and any multi-space variants)
     *
     * After normalisation, checks are applied in this order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if digit count is not 6
     * - [InvalidReason.INVALID_FORMAT] if any non-digit character remains
     * - [InvalidReason.INVALID_PREFIX] if the first digit is `'0'`
     *
     * **Note:** Hyphens are not stripped. `"560-001"` normalises to `"560-001"` (7 chars)
     * and returns [InvalidReason.WRONG_LENGTH].
     *
     * @param value Raw PIN code string.
     * @return [ValidationResult.Valid] if the PIN code is a valid 6-digit Indian postal code,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.pincodeValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)

        if (normalized.isBlank()) {
            return ValidationResult.Invalid(InvalidReason.EMPTY)
        }
        if (normalized.length != EXPECTED_LENGTH) {
            return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        }
        if (normalized.any { it !in '0'..'9' }) {
            return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        }
        if (normalized[0] == '0') {
            return ValidationResult.Invalid(InvalidReason.INVALID_PREFIX)
        }
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw PIN code string.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.pincodeIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a validated PIN code in India Post canonical display form: `XXX XXX`.
     *
     * Input is normalised the same way as [validate] before formatting.
     * Output always has a single space after the third digit: e.g. `"560 001"`.
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw PIN code string.
     * @return Formatted display string in `XXX XXX` form.
     * @throws IllegalArgumentException if [value] is not a valid Indian PIN code.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.pincodeFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "Pincode.format requires a valid pincode; got: ${value.take(50)}",
            )
        }
        val digits = normalize(value)
        return "${digits.substring(0, 3)} ${digits.substring(3)}"
    }

    // ---------------------------------------------------------------------------
    // Private normalisation helper
    // NOT part of the public API.
    // ---------------------------------------------------------------------------

    /**
     * Normalises a raw PIN code string:
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers both single-space India Post display
     *    form "XXX XXX" and multi-space variants like "XXX  XXX")
     */
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "")
}
