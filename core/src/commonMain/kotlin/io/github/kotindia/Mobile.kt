// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * Validator, formatter, and masker for Indian mobile numbers (TRAI numbering plan).
 *
 * Accepts 10-digit numbers starting with 6, 7, 8, or 9.
 * Common input forms are normalised before validation: raw digits, E.164 (+91 prefix),
 * leading zero (common Indian dialing convention), spaces, and hyphens are all accepted.
 *
 * This object is stateless and thread-safe.
 *
 * ## Example
 *
 * ```kotlin
 * Mobile.validate("9876543210")               // ValidationResult.Valid
 * Mobile.validate("+91 98765 43210")          // ValidationResult.Valid
 * Mobile.validate("1234567890")               // ValidationResult.Invalid(INVALID_PREFIX)
 * Mobile.format("9876543210", true)           // "+91 98765 43210"
 * Mobile.mask("9876543210")                   // "XXXXXX3210"
 * ```
 *
 * @sample io.github.kotindia.samples.mobileSample
 */
public object Mobile {
    // Normalisation constants — kept private; not part of the public contract.
    private const val COUNTRY_CODE_WITH_PLUS = "+91"
    private const val EXPECTED_LENGTH = 10
    private const val VALID_FIRST_DIGITS = "6789"

    /**
     * Validates an Indian mobile number.
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip `+91` prefix (if present)
     * 3. Strip a single leading `0` (if present after step 2)
     * 4. Strip all remaining spaces and hyphens
     *
     * After normalisation, checks are applied in this order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if digit count is not 10
     * - [InvalidReason.INVALID_FORMAT] if any non-digit character remains
     * - [InvalidReason.INVALID_PREFIX] if the first digit is not 6, 7, 8, or 9
     *
     * **Edge cases:**
     * - `"+910876543210"` → strip `+91` → `"0876543210"` → strip leading `0` → `"876543210"`
     *   (9 digits) → [InvalidReason.WRONG_LENGTH]
     * - `"00876543210"` → no `+91` prefix → strip one leading `0` → `"0876543210"` (10 digits,
     *   starts with `0`) → [InvalidReason.INVALID_PREFIX]
     * - `"0876543210"` → strip leading `0` → `"876543210"` (9 digits) → [InvalidReason.WRONG_LENGTH]
     *
     * @param value Raw mobile number string in any common Indian format.
     * @return [ValidationResult.Valid] if the number is a valid 10-digit Indian mobile,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.mobileValidateSample
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
        if (normalized[0] !in VALID_FIRST_DIGITS) {
            return ValidationResult.Invalid(InvalidReason.INVALID_PREFIX)
        }
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw mobile number string in any common Indian format.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.mobileIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a validated mobile number for human-readable display.
     *
     * Input is normalised the same way as [validate] before formatting.
     * Output formats:
     * - `withCountryCode = false` (default): `"98765 43210"` (space after 5th digit)
     * - `withCountryCode = true`: `"+91 98765 43210"`
     *
     * @param value Raw mobile number string in any common Indian format.
     * @param withCountryCode If `true`, prepends `+91 ` to the formatted output. Default: `false`.
     * @return Formatted display string.
     * @throws IllegalArgumentException if [value] is not a valid Indian mobile number.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.mobileFormatSample
     */
    public fun format(
        value: String,
        withCountryCode: Boolean = false,
    ): String {
        if (!isValid(value)) {
            val display = value.take(50)
            throw IllegalArgumentException("Mobile.format requires a valid mobile number; got: $display")
        }
        val digits = normalize(value)
        val formatted = "${digits.substring(0, 5)} ${digits.substring(5)}"
        return if (withCountryCode) "+91 $formatted" else formatted
    }

    /**
     * Masks a mobile number for PII-safe display.
     *
     * Masking operates on the **raw input string** (not normalised). This is intentional —
     * the masker is a pure display utility that works character-by-character on whatever string
     * it receives. Callers who want to mask a normalised number should call [format] first:
     * `Mobile.mask(Mobile.format(value))`.
     *
     * Default behaviour: last 4 characters visible, remainder replaced by [maskChar]:
     * `"9876543210"` → `"XXXXXX3210"`.
     *
     * **Edge cases (all safe — never throws):**
     * - Empty input → returns `""`.
     * - `visibleStart + visibleEnd >= value.length` → entire string returned unmasked (overlap rule).
     * - Invalid or non-normalised input → masking applied character-by-character on the raw string
     *   (spaces, `+`, hyphens count as characters). No normalisation is performed.
     *
     * Examples with non-normalised input:
     * - `mask("+91 98765 43210")` (15 chars, default params) → `"XXXXXXXXXXX3210"`
     * - `mask("09876543210")` (11 chars) → `"XXXXXXX3210"`
     *
     * @param value Raw string to mask. No normalisation is applied.
     * @param visibleStart Number of leading characters to show unmasked. Default: `0`.
     * @param visibleEnd Number of trailing characters to show unmasked. Default: `4`.
     * @param maskChar Character to replace masked positions. Default: `'X'`.
     * @return Masked string. Never throws — display-safe by contract.
     * @sample io.github.kotindia.samples.mobileMaskSample
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
    // NOT part of the public API. May be promoted to internal/ if future
    // validators share the same stripping logic.
    // ---------------------------------------------------------------------------

    /**
     * Normalises a raw mobile number string:
     * 1. Trim leading/trailing whitespace
     * 2. Strip `+91` prefix (if present)
     * 3. Strip a single leading `0` (if present after step 2)
     * 4. Strip all spaces and hyphens
     */
    private fun normalize(value: String): String {
        var s = value.trim()
        if (s.startsWith(COUNTRY_CODE_WITH_PLUS)) {
            s = s.removePrefix(COUNTRY_CODE_WITH_PLUS)
        }
        if (s.startsWith("0")) {
            s = s.removePrefix("0")
        }
        s = s.replace(" ", "").replace("-", "")
        return s
    }
}
