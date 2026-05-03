// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * Validator and formatter for Indian Financial System Codes (IFSC).
 *
 * IFSC is an 11-character RBI-assigned code identifying a specific bank branch
 * for NEFT, RTGS, IMPS, and UPI transactions.
 *
 * Format: `[A-Z]{4}0[A-Z0-9]{6}`
 * - Characters 1–4: bank code (uppercase letters)
 * - Character 5: always `0` (reserved by RBI for future use)
 * - Characters 6–11: branch code (uppercase alphanumeric)
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw uppercase: `"HDFC0000001"`
 * - Lowercase: `"hdfc0000001"` — normalised to uppercase
 * - Mixed case: `"HdFc0000001"` — normalised to uppercase
 * - With spaces: `"HDFC 0000 001"` — internal whitespace stripped
 * - Whitespace padded: `" HDFC0000001 "` — leading/trailing whitespace trimmed
 *
 * IFSC is a public bank routing code published and distributed by RBI — no `mask()`
 * method is provided. See the KotIndia API design rationale for details.
 *
 * This object is stateless and thread-safe. No checksum exists for IFSC — validation
 * is structural (format) only per RBI specification.
 *
 * @sample io.github.kotindia.samples.ifscSample
 */
@Suppress("ClassName") // IFSC is a universal acronym — preserved for consumer readability
public object IFSC {
    private const val EXPECTED_LENGTH = 11
    private val IFSC_REGEX = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")

    /**
     * Validates an Indian Financial System Code (IFSC).
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers copy-paste with spaces)
     * 3. Uppercase all characters
     *
     * After normalisation, checks are applied in order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if length is not 11
     * - [InvalidReason.INVALID_FORMAT] if the string does not match `[A-Z]{4}0[A-Z0-9]{6}`
     *
     * The regex encodes all structural constraints simultaneously:
     * - Characters 1–4 must be `[A-Z]`
     * - Character 5 must be `'0'` (RBI reserved position)
     * - Characters 6–11 must be `[A-Z0-9]`
     *
     * **Note:** Hyphens are not stripped. `"HDFC-0000001"` normalises to a 12-character
     * string and returns [InvalidReason.WRONG_LENGTH].
     *
     * @param value Raw IFSC string in any common input form.
     * @return [ValidationResult.Valid] if the code is a valid RBI IFSC,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.ifscValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)

        if (normalized.isBlank()) {
            return ValidationResult.Invalid(InvalidReason.EMPTY)
        }
        if (normalized.length != EXPECTED_LENGTH) {
            return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        }
        if (!IFSC_REGEX.matches(normalized)) {
            return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        }
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw IFSC string.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.ifscIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats an IFSC to canonical form: 11-character uppercase, no separators.
     *
     * Applies the same normalisation as [validate] before formatting.
     * IFSC has no published display separator convention — output is the raw
     * uppercase string: e.g. `"HDFC0000001"`.
     *
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw IFSC string.
     * @return Canonical 11-character uppercase IFSC string.
     * @throws IllegalArgumentException if [value] is not a valid IFSC.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.ifscFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "IFSC.format requires a valid IFSC; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    // ---------------------------------------------------------------------------
    // Private normalisation helper
    // NOT part of the public API.
    // ---------------------------------------------------------------------------

    /**
     * Normalises a raw IFSC string:
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers copy-paste with spaces)
     * 3. Uppercase all characters (IFSC is case-insensitive in real-world usage;
     *    canonical form is uppercase per RBI)
     *
     * This is the reference uppercase-normalisation pattern for PAN, GSTIN, and TAN.
     */
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "").uppercase()
}
