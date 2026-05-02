// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * Validator, formatter, and masker for ESIC (Employee State Insurance Corporation) numbers.
 *
 * An ESIC number is a 17-digit identifier assigned to insured employees under
 * the Employees' State Insurance Act, 1948. It is a private health-linked identifier
 * and must be masked in any user-facing display per ESIC PII policy.
 *
 * There is no publicly documented checksum algorithm for ESIC numbers.
 * Validation is format-only (length + digit-only check).
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.esicSample
 */
@Suppress("ClassName")
public object ESIC {
    private const val EXPECTED_LENGTH = 17

    /**
     * Validates an ESIC number.
     *
     * Input normalization before validation:
     * 1. Trim leading/trailing whitespace
     * 2. Strip internal whitespace
     *
     * After normalization, checks are applied in this order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if digit count is not 17
     * - [InvalidReason.INVALID_FORMAT] if any non-digit character remains
     *
     * @param value Raw ESIC number string.
     * @return [ValidationResult.Valid] if the input normalizes to exactly 17 digits,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.esicValidateSample
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
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw ESIC number string.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.esicIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats an ESIC number to its canonical 17-digit form.
     *
     * Input is normalized (trim + strip internal whitespace) before formatting.
     * The canonical form is plain digits with no separators.
     *
     * @param value Raw ESIC number string.
     * @return Canonical 17-digit string.
     * @throws IllegalArgumentException if input is not a valid ESIC number.
     * @sample io.github.kotindia.samples.esicFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "ESIC.format requires a valid ESIC number; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    /**
     * Masks an ESIC number for PII-safe display.
     *
     * Default: last 4 digits visible, remainder replaced by [maskChar] (`"XXXXXXXXXXXXX4567"`).
     * Operates character-by-character on the raw input — does not normalize.
     *
     * **Edge cases (all safe — never throws):**
     * - Empty input → returns `""`.
     * - `visibleStart + visibleEnd >= value.length` → entire string returned unmasked (overlap rule).
     * - Non-normalized input → masking applied character-by-character on the raw string.
     *
     * @param value Raw ESIC number string. No normalization is applied.
     * @param visibleStart Number of leading characters to show unmasked. Default: `0`.
     * @param visibleEnd Number of trailing characters to show unmasked. Default: `4`.
     * @param maskChar Character to replace masked positions. Default: `'X'`.
     * @return Masked string. Never throws — display-safe by contract.
     * @sample io.github.kotindia.samples.esicMaskSample
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
    // Private normalization helper
    // NOT part of the public API.
    // ---------------------------------------------------------------------------

    /**
     * Normalizes a raw ESIC number string:
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace
     */
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "")
}
