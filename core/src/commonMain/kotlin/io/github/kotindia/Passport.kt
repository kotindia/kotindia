// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * Validator, formatter, and masker for Indian Passport numbers.
 *
 * Indian Passport numbers are issued by the Ministry of External Affairs (MEA)
 * and serve as the primary international travel document and a widely accepted
 * proof of identity and address for Indian citizens.
 *
 * Format: `[A-Z]\d{7}` — 1 uppercase letter followed by 7 digits (8 characters total).
 * - Character 1: uppercase letter (passport series prefix, assigned by MEA)
 * - Characters 2–8: 7-digit serial number (padded with leading zeros as needed)
 *
 * **No checksum:** The Ministry of External Affairs has not published a public
 * checksum algorithm for Indian passport numbers. Validation is structural
 * (format + length) only.
 *
 * **Passport is Private government ID PII** (§3.5). Mask before logging or displaying in UI.
 * See [mask] for PII-safe display options.
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw uppercase: `"M1234567"`
 * - Lowercase: `"m1234567"` — normalised to uppercase
 * - With spaces: `"M 1234567"` — internal whitespace stripped
 * - Whitespace padded: `" M1234567 "` — leading/trailing whitespace trimmed
 *
 * **Reference:** Ministry of External Affairs (MEA) passport number format.
 * Format confirmed per R8 (Sandeep, 2026-04-29): digit-only serial `[A-Z]\d{7}`.
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.passportSample
 */
public object Passport {
    private const val EXPECTED_LENGTH = 8
    private val FORMAT_REGEX = Regex("^[A-Z]\\d{7}$")

    /**
     * Validates an Indian Passport number.
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers copy-paste with spaces)
     * 3. Uppercase all characters
     *
     * After normalisation, checks are applied in order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if length is not 8
     * - [InvalidReason.INVALID_FORMAT] if the string does not match `^[A-Z]\d{7}$`
     *
     * **No checksum:** MEA has not published a public checksum algorithm for
     * Indian passport numbers. Validation is structural only.
     *
     * **Note:** Hyphens are not stripped. `"M-1234567"` normalises to 9 characters
     * and returns [InvalidReason.WRONG_LENGTH].
     *
     * @param value Raw Passport number string in any common input form.
     * @return [ValidationResult.Valid] if the Passport number passes format validation,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.passportValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)

        if (normalized.isBlank()) return ValidationResult.Invalid(InvalidReason.EMPTY)
        if (normalized.length != EXPECTED_LENGTH) return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        if (!FORMAT_REGEX.matches(normalized)) return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw Passport number string.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.passportIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a Passport number to canonical form: 8-character uppercase, no separators.
     *
     * Applies the same normalisation as [validate] before formatting.
     * The MEA publishes no display separator convention — output is the raw uppercase
     * string: e.g. `"M1234567"`.
     *
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw Passport number string.
     * @return Canonical 8-character uppercase Passport number string.
     * @throws IllegalArgumentException if [value] is not a valid Passport number.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.passportFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "Passport.format requires a valid Passport number; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    /**
     * Masks a Passport number for PII-safe display.
     *
     * **Passport is Private government ID PII** — always mask before displaying in UI or logging.
     *
     * Masking operates on the **raw input string** (not normalised). This is intentional —
     * the masker is a display utility that works character-by-character on whatever string
     * it receives. Callers who want to mask a normalised Passport number should call [format]
     * first: `Passport.mask(Passport.format(value))`.
     *
     * **Default `(visibleStart=0, visibleEnd=4, maskChar='X')`:**
     * Shows last 4 characters, masks the rest.
     * `"M1234567"` → `"XXXX4567"` (last 4: `"4567"`)
     *
     * **Edge cases (all safe — never throws):**
     * - Empty input → returns `""`.
     * - `visibleStart + visibleEnd >= value.length` → entire string returned unmasked (overlap rule).
     * - Non-normalised input (lowercase, spaces) → masking applied character-by-character on the
     *   raw string. No normalisation is performed.
     *
     * @param value Raw Passport number string to mask. No normalisation is applied.
     * @param visibleStart Number of leading characters to show unmasked. Default: `0`.
     * @param visibleEnd Number of trailing characters to show unmasked. Default: `4`.
     * @param maskChar Character to replace masked positions. Default: `'X'`.
     * @return Masked string. Never throws — display-safe by contract.
     * @sample io.github.kotindia.samples.passportMaskSample
     */
    public fun mask(
        value: String,
        visibleStart: Int = 0,
        visibleEnd: Int = 4,
        maskChar: Char = 'X',
    ): String {
        // Maskers never throw — display-safe by contract.
        // Operate on the raw value as-is (no isValid check, no normalize call).
        if (value.isEmpty()) return ""

        val len = value.length

        // Overlap: if visible portions cover everything, return unmasked.
        if (visibleStart + visibleEnd >= len) return value

        val masked = StringBuilder()
        masked.append(value.substring(0, visibleStart))
        val maskCount = len - visibleStart - visibleEnd
        repeat(maskCount) { masked.append(maskChar) }
        masked.append(value.substring(len - visibleEnd))
        return masked.toString()
    }

    // ---------------------------------------------------------------------------
    // Private normalisation helper
    // NOT part of the public API.
    // Canonical uppercase-normalisation pattern (established by IFSC Slice 5,
    // referenced by PAN Slice 6, Passport Slice 10f).
    // ---------------------------------------------------------------------------

    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "").uppercase()
}
