// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import io.github.kotindia.internal.Verhoeff

// Top-level private constant — constructed once at class-load time, not per call.
// Prevents per-call Set<Char> allocation inside Aadhaar.allowedChars.
private val AADHAAR_ALLOWED_CHARS: Set<Char> = ('0'..'9').toSet()

/**
 * Validator, formatter, and masker for Aadhaar numbers issued by UIDAI.
 *
 * Aadhaar is the 12-digit unique identity number issued by the Unique Identification
 * Authority of India (UIDAI) to every Indian resident. It is required for:
 * - Opening bank accounts (RBI KYC mandate)
 * - Filing income tax returns
 * - SIM card activation (TRAI mandate)
 * - Government welfare benefit disbursement (DBT)
 * - eKYC onboarding at regulated financial institutions
 *
 * **Format:** 12 digits, first digit must be 2–9 (UIDAI never issues numbers starting with 0 or 1).
 *
 * **Checksum:** Verhoeff algorithm (dihedral group D5). Implementation in
 * [io.github.kotindia.internal.Verhoeff] — cross-referenced against Wikipedia D5 spec
 * and mastermunj/format-utils JS implementation before finalising lookup tables.
 *
 * **Aadhaar is Private government-level PII** protected under the Aadhaar Act 2016,
 * IT Act 2000, and India's DPDP Act 2023. Always mask before logging or displaying.
 * See [mask] for PII-safe display options.
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw: `"234567890121"`
 * - UIDAI spaced: `"2345 6789 0121"` — spaces stripped before validation
 * - Partial spacing: `"2345 67890121"` — stripped
 * - Whitespace-padded: `" 234567890121 "` — trimmed + stripped
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.aadhaarSample
 */
public object Aadhaar {
    private const val EXPECTED_LENGTH = 12
    private val DIGIT_REGEX = Regex("^\\d{12}$")
    private val INVALID_PREFIX_DIGITS = setOf('0', '1')

    /**
     * Validates an Aadhaar number.
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers UIDAI spaced `"1234 5678 9012"` form)
     *
     * After normalisation, checks are applied in this order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if length is not 12
     * - [InvalidReason.INVALID_FORMAT] if any non-digit character remains
     * - [InvalidReason.INVALID_PREFIX] if the first digit is 0 or 1
     * - [InvalidReason.INVALID_CHECKSUM] if the Verhoeff checksum fails
     *
     * @param value Raw Aadhaar string in any common input form.
     * @return [ValidationResult.Valid] if the Aadhaar passes all checks,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     *   Never throws.
     * @sample io.github.kotindia.samples.aadhaarValidateSample
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
     * @param value Raw Aadhaar string in any common input form.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.aadhaarIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a valid Aadhaar number to the UIDAI canonical spaced form.
     *
     * Output: `"XXXX XXXX XXXX"` — three groups of 4 digits separated by spaces.
     * Example: `"234567890121"` → `"2345 6789 0121"`.
     *
     * Input is normalised (whitespace stripped) before formatting.
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw Aadhaar string in any common input form.
     * @return Canonical UIDAI-spaced Aadhaar string.
     * @throws IllegalArgumentException if [value] is not a valid Aadhaar number.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.aadhaarFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "Aadhaar.format requires a valid Aadhaar number; got: ${value.take(50)}",
            )
        }
        val n = normalize(value)
        return "${n.substring(0, 4)} ${n.substring(4, 8)} ${n.substring(8)}"
    }

    /**
     * Masks an Aadhaar number for PII-safe display.
     *
     * **Aadhaar is strictly Private government PII** (Aadhaar Act 2016 + DPDP Act 2023).
     * Always mask before displaying in UI or writing to logs.
     *
     * Masking operates on the **raw input string** (not normalised). This is intentional —
     * the masker is a display utility working character-by-character on whatever it receives.
     * Callers who want to mask a normalised Aadhaar should call [format] first:
     * `Aadhaar.mask(Aadhaar.format(value))`.
     *
     * **Default `(visibleStart=0, visibleEnd=4, maskChar='X')`:**
     * Shows last 4 characters of the raw input, masks the rest.
     * `"234567890121"` → `"XXXXXXXX0121"` (8 X's + last 4 digits)
     *
     * **Note on UIDAI-style spaced mask (`"XXXX XXXX 0121"`):**
     * The UIDAI standard display form is NOT a built-in output of `mask()`. To achieve it:
     * ```kotlin
     * // Recommended — construct manually from raw digits
     * val raw = "234567890121"
     * val uidaiMask = "XXXX XXXX " + raw.takeLast(4)  // -> "XXXX XXXX 0121"
     *
     * // mask() on raw 12 digits gives "XXXXXXXX0121" (no spaces — char-by-char on raw)
     * // mask() on formatted 14-char string "2345 6789 0121" gives "XXXXXXXXXX0121"
     * // (masks spaces too — not the UIDAI form)
     * ```
     * This is a known limitation of the standard masking signature (consistent with OQ-10 contract).
     * No additional API surface is added; compose the UIDAI form manually as shown above.
     *
     * **Edge cases (all safe — never throws):**
     * - Empty input → returns `""`.
     * - `visibleStart + visibleEnd >= value.length` → entire string returned unmasked (overlap rule).
     * - Non-normalised input → masking applied character-by-character on the raw string.
     *
     * @param value Raw Aadhaar string to mask. No normalisation is applied.
     * @param visibleStart Number of leading characters to show unmasked. Default: `0`.
     * @param visibleEnd Number of trailing characters to show unmasked. Default: `4`.
     * @param maskChar Character to replace masked positions. Default: `'X'`.
     * @return Masked string. Never throws — display-safe by contract.
     * @sample io.github.kotindia.samples.aadhaarMaskSample
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
    // Progressive validation API (Slice 14 / Phase 2)
    // ---------------------------------------------------------------------------

    /**
     * Maximum accepted length for a sanitized Aadhaar input.
     *
     * Aadhaar is always 12 digits. Any sanitized input longer than this is over-length.
     */
    public val maxLength: Int = 12

    /**
     * Set of characters accepted by [sanitize] and [validateProgressive] for Aadhaar.
     *
     * Aadhaar accepts only ASCII decimal digits (`'0'..'9'`). Any other character is stripped by
     * [sanitize] and triggers [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT]
     * inside [validateProgressive].
     *
     * Exposed as `Set<Char>` for uniform caller-side `if (char in Aadhaar.allowedChars)` checks.
     * The constant is top-level to avoid per-call allocation.
     */
    public val allowedChars: Set<Char> = AADHAAR_ALLOWED_CHARS

    /**
     * Strips all characters not in [allowedChars] from [rawInput] and truncates to [maxLength].
     *
     * This is a pure, idempotent function — `sanitize(sanitize(x)) == sanitize(x)` for all inputs.
     * Useful for restricting keyboard input or cleaning a pasted value before passing to
     * [validateProgressive].
     *
     * Examples:
     * - `sanitize("1234 5678 9012")` → `"123456789012"` (spaces stripped)
     * - `sanitize("1234abcd56789012345")` → `"123456789012"` (letters stripped, truncated at 12)
     * - `sanitize("abc")` → `""` (all non-allowed → empty)
     * - `sanitize("")` → `""`
     *
     * @param rawInput Any string, including pasted values with spaces, letters, or symbols.
     * @return A string containing only digits, at most [maxLength] characters long.
     * @sample io.github.kotindia.samples.aadhaarSanitizeSample
     */
    public fun sanitize(rawInput: String): String = rawInput.filter { it in AADHAAR_ALLOWED_CHARS }.take(maxLength)

    /**
     * Validates an Aadhaar number for incremental, as-you-type input.
     *
     * Returns one of four [ProgressiveResult] states. The critical invariant: partial inputs of
     * the correct character class (digits) NEVER return [ProgressiveResult.Invalid] — partial
     * inputs always return [ProgressiveResult.Typing].
     *
     * State machine evaluation order (first match wins):
     * 1. Trim whitespace. If result is empty → [ProgressiveResult.Empty]
     * 2. Any character not in [allowedChars] → [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT]
     * 3. Length > [maxLength] (all allowed chars) → [ProgressiveResult.Invalid] with [InvalidReason.WRONG_LENGTH]
     *    and [ValidationContext.LengthMismatch]
     * 4. Length in `1..(maxLength - 1)` → [ProgressiveResult.Typing] with formatted partial text
     * 5. Length == [maxLength] → delegates to [validate]; returns [ProgressiveResult.Valid] or
     *    [ProgressiveResult.Invalid] with appropriate [InvalidReason]
     *
     * @param value Raw Aadhaar input, possibly partial, possibly with spaces or other chars.
     * @return A [ProgressiveResult] indicating the current state of the input.
     * @sample io.github.kotindia.samples.aadhaarValidateProgressiveSample
     */
    public fun validateProgressive(value: String): ProgressiveResult {
        // Step 1: trim whitespace
        val normalized = value.trim()

        // Step 2: empty check
        if (normalized.isEmpty()) return ProgressiveResult.Empty

        // Step 3: bad-char check — fires BEFORE length check (AC4 priority)
        if (normalized.any { it !in AADHAAR_ALLOWED_CHARS }) {
            return ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None)
        }

        // Step 4: over-length check
        if (normalized.length > maxLength) {
            return ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = maxLength, actual = normalized.length),
            )
        }

        // Step 5: partial check
        if (normalized.length < maxLength) {
            return ProgressiveResult.Typing(visualText = formatPartial(normalized))
        }

        // Step 6: complete — delegate to full validate()
        return when (val result = validate(normalized)) {
            is ValidationResult.Valid -> ProgressiveResult.Valid
            is ValidationResult.Invalid -> ProgressiveResult.Invalid(result.reason, ValidationContext.None)
        }
    }

    // ---------------------------------------------------------------------------
    // Private normalisation helper
    // Aadhaar is all-digits — no uppercase transform needed.
    // ---------------------------------------------------------------------------
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "")

    // ---------------------------------------------------------------------------
    // Private progressive helpers
    // ---------------------------------------------------------------------------

    // formatPartial: operates on sanitized (digits-only) string, not raw input.
    // Aadhaar display standard: groups of 4 separated by spaces ("XXXX XXXX XXXX").
    private fun formatPartial(digits: String): String = digits.chunked(4).joinToString(" ")
}
