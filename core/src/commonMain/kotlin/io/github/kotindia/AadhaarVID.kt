// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import io.github.kotindia.internal.Verhoeff

/**
 * Validator, formatter, and masker for Aadhaar Virtual IDs (VID) issued by UIDAI.
 *
 * AadhaarVID is a 16-digit revocable identifier introduced by UIDAI in 2018, allowing
 * residents to share a privacy-safe proxy for their Aadhaar number without revealing the
 * underlying 12-digit Aadhaar. Used in eKYC flows where the full Aadhaar is not required.
 *
 * **Format:** 16 digits, first digit must be 2–9 (UIDAI never issues VIDs starting with 0 or 1).
 *
 * **Checksum:** Same Verhoeff algorithm as [Aadhaar]. Implementation in
 * [io.github.kotindia.internal.Verhoeff].
 *
 * **AadhaarVID is Private government-level PII** — same legal protection as Aadhaar
 * (Aadhaar Act 2016, IT Act 2000, DPDP Act 2023). Always mask before logging or displaying.
 * See [mask] for PII-safe display options.
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw: `"2345678901234561"`
 * - Spaced: `"2345 6789 0123 4561"` — spaces stripped before validation
 * - Whitespace-padded: `" 2345678901234561 "` — trimmed + stripped
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.aadhaarVIDSample
 */
public object AadhaarVID {
    private const val EXPECTED_LENGTH = 16
    private val DIGIT_REGEX = Regex("^\\d{16}$")
    private val INVALID_PREFIX_DIGITS = setOf('0', '1')

    /**
     * Validates an Aadhaar Virtual ID.
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers spaced `"2345 6789 0123 4561"` form)
     *
     * After normalisation, checks are applied in this order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if length is not 16
     * - [InvalidReason.INVALID_FORMAT] if any non-digit character remains
     * - [InvalidReason.INVALID_PREFIX] if the first digit is 0 or 1
     * - [InvalidReason.INVALID_CHECKSUM] if the Verhoeff checksum fails
     *
     * @param value Raw VID string in any common input form.
     * @return [ValidationResult.Valid] if the VID passes all checks,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     *   Never throws.
     * @sample io.github.kotindia.samples.aadhaarVIDValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)
        if (normalized.isBlank()) return ValidationResult.Invalid(InvalidReason.EMPTY)
        if (normalized.length != EXPECTED_LENGTH) return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        if (!DIGIT_REGEX.matches(normalized)) return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        if (normalized[0] in INVALID_PREFIX_DIGITS) return ValidationResult.Invalid(InvalidReason.INVALID_PREFIX)
        if (!Verhoeff.isValid(normalized)) return ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM)
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw VID string in any common input form.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.aadhaarVIDIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a valid AadhaarVID to the canonical spaced form.
     *
     * Output: `"XXXX XXXX XXXX XXXX"` — four groups of 4 digits separated by spaces.
     * Example: `"2345678901234561"` → `"2345 6789 0123 4561"`.
     *
     * Input is normalised (whitespace stripped) before formatting.
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw VID string in any common input form.
     * @return Canonical 4-group-spaced VID string.
     * @throws IllegalArgumentException if [value] is not a valid AadhaarVID.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.aadhaarVIDFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "AadhaarVID.format requires a valid AadhaarVID; got: ${value.take(50)}",
            )
        }
        val n = normalize(value)
        return "${n.substring(0, 4)} ${n.substring(4, 8)} ${n.substring(8, 12)} ${n.substring(12)}"
    }

    /**
     * Masks an AadhaarVID for PII-safe display.
     *
     * **AadhaarVID is Private government PII** — same legal protection as Aadhaar.
     * Always mask before displaying in UI or writing to logs.
     *
     * Masking operates on the **raw input string** (not normalised). Per OQ-10 contract:
     * the masker never calls [isValid] or [normalize] internally.
     *
     * **Default `(visibleStart=0, visibleEnd=4, maskChar='X')`:**
     * Shows last 4 characters of the raw input, masks the rest.
     * `"2345678901234561"` → `"XXXXXXXXXXXX4561"` (12 X's + last 4 digits)
     *
     * **Note on UIDAI-style spaced mask:**
     * The UIDAI display form is NOT built-in. To achieve `"XXXX XXXX XXXX 4561"`:
     * ```kotlin
     * val raw = "2345678901234561"
     * val uidaiMask = "XXXX XXXX XXXX " + raw.takeLast(4)  // -> "XXXX XXXX XXXX 4561"
     * ```
     * This is a known limitation of the standard masking signature (consistent with OQ-10 contract).
     *
     * **Edge cases (all safe — never throws):**
     * - Empty input → returns `""`.
     * - `visibleStart + visibleEnd >= value.length` → entire string returned unmasked (overlap rule).
     * - Non-normalised input → masking applied character-by-character on the raw string.
     *
     * @param value Raw VID string to mask. No normalisation is applied.
     * @param visibleStart Number of leading characters to show unmasked. Default: `0`.
     * @param visibleEnd Number of trailing characters to show unmasked. Default: `4`.
     * @param maskChar Character to replace masked positions. Default: `'X'`.
     * @return Masked string. Never throws — display-safe by contract.
     * @sample io.github.kotindia.samples.aadhaarVIDMaskSample
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
    // VID is all-digits — no uppercase transform needed.
    // ---------------------------------------------------------------------------
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "")
}
