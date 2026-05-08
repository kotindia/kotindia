// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import io.github.kotindia.internal.Luhn

// Top-level private constant — constructed once at class-load time, not per call.
private val IMEI_ALLOWED_CHARS: Set<Char> =
    setOf(
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
    )

/**
 * Validator, formatter, and masker for IMEI numbers assigned by the GSMA.
 *
 * IMEI (International Mobile Equipment Identity) is the unique 15-digit identifier
 * assigned to every mobile device globally by the GSMA (Global System for Mobile
 * Communications Association). IMEI is used for:
 * - Device blacklisting by carriers when stolen or lost (GSMA IMEI database / CEIR in India)
 * - SIM-lock and network unlock verification
 * - Device identity in telecom KYC flows (TRAI DoT mandate)
 * - Regulatory filings — all mobile device imports to India require IMEI registration with DoT
 * - Warranty and insurance claims (device identity verification)
 *
 * **Format:** 15 digits. Last digit is the Luhn mod-10 check digit.
 * First 8 digits = TAC (Type Allocation Code) — format-only validation, no GSMA database lookup.
 * Next 6 digits = serial number. No first-digit restriction (unlike Aadhaar or Mobile).
 *
 * **Checksum:** Luhn algorithm (ISO/IEC 7812 / mod-10). Implementation in
 * [io.github.kotindia.internal.Luhn] — cross-referenced against Wikipedia Luhn specification
 * and npm `luhn` package (MIT) before finalizing.
 *
 * **IMEI is Device PII** — it uniquely identifies a physical device (Telecom Cybersecurity
 * Rules, 2024). Always mask before logging or displaying. See [mask] for PII-safe display.
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw: `"490154203237518"`
 * - Phone display form: `"490 154 203 237 518"` — internal spaces stripped before validation
 * - Whitespace-padded: `" 490154203237518 "` — trimmed + stripped
 * - Hyphens are NOT stripped — `"490-154-203-237518"` → `INVALID_FORMAT`
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.imeiSample
 */
@Suppress("ClassName")
public object IMEI {
    private const val EXPECTED_LENGTH = 15
    private val DIGIT_REGEX = Regex("^\\d{15}$")

    /**
     * Validates an IMEI number.
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers phone display form `"490 154 203 237 518"`)
     *
     * **Note on hyphens:** Hyphens are NOT stripped — only whitespace is. Input containing
     * hyphens (e.g. `"490-154-203-237518"`) will return [InvalidReason.INVALID_FORMAT].
     * Callers must strip hyphens manually if their input may contain them.
     *
     * After normalisation, checks are applied in this order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if normalised length is not 15
     * - [InvalidReason.INVALID_FORMAT] if any non-digit character remains
     * - [InvalidReason.INVALID_CHECKSUM] if the Luhn mod-10 checksum fails
     *
     * **No [InvalidReason.INVALID_PREFIX] check** — IMEI has no first-digit restriction.
     * TAC can start with any digit 0–9 per GSMA spec.
     *
     * @param value Raw IMEI string in any common input form.
     * @return [ValidationResult.Valid] if the IMEI passes all checks,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     *   Never throws.
     * @sample io.github.kotindia.samples.imeiValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)
        if (normalized.isBlank()) return ValidationResult.Invalid(InvalidReason.EMPTY)
        if (normalized.length != EXPECTED_LENGTH) return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        if (!DIGIT_REGEX.matches(normalized)) return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        if (!Luhn.isChecksumValid(normalized)) return ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM)
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw IMEI string in any common input form.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.imeiIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a valid IMEI number to its canonical form.
     *
     * Output: 15 consecutive digits with **no separator**.
     * Example: `"490 154 203 237 518"` → `"490154203237518"`.
     *
     * **No separator in output** — ITU-T E.118 does not mandate a canonical display separator.
     * Vendor display varies across manufacturers (groups of 2-3-4-6 on phone screens).
     * Callers who want grouped display (e.g. `"490-154-203-237518"`) must apply their own grouping.
     *
     * Input is normalised (whitespace stripped) before formatting.
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw IMEI string in any common input form.
     * @return Canonical 15-digit IMEI string with no separator.
     * @throws IllegalArgumentException if [value] is not a valid IMEI number.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.imeiFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "IMEI.format requires a valid IMEI number; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    /**
     * Masks an IMEI number for PII-safe display.
     *
     * **IMEI is Device PII** — it uniquely identifies a physical device (Telecom
     * Cybersecurity Rules, 2024). Always mask before displaying in UI or writing to logs.
     *
     * Masking operates on the **raw input string** (not normalised). This is intentional —
     * the masker is a display utility working character-by-character on whatever it receives.
     * Callers who want to mask a normalised 15-digit IMEI should call [format] first:
     * `IMEI.mask(IMEI.format(value))`.
     *
     * **Note on raw vs normalised masking:**
     * `IMEI.mask("490 154 203 237 518")` masks a 19-character string char-by-char — spaces
     * in the raw input are also masked. If caller wants to mask a clean 15-digit string,
     * call `IMEI.format(value)` first, then `IMEI.mask(...)`.
     *
     * **Default `(visibleStart=0, visibleEnd=4, maskChar='X')`:**
     * Shows last 4 characters of the raw input, masks the rest.
     * `"490154203237518"` → `"XXXXXXXXXXX7518"` (11 X's + last 4 digits)
     *
     * **Edge cases (all safe — never throws):**
     * - Empty input → returns `""`.
     * - `visibleStart + visibleEnd >= value.length` → entire string returned unmasked (overlap rule).
     * - Non-normalised input → masking applied character-by-character on the raw string.
     *
     * @param value Raw IMEI string to mask. No normalisation is applied.
     * @param visibleStart Number of leading characters to show unmasked. Default: `0`.
     * @param visibleEnd Number of trailing characters to show unmasked. Default: `4`.
     * @param maskChar Character to replace masked positions. Default: `'X'`.
     * @return Masked string. Never throws — display-safe by contract.
     * @sample io.github.kotindia.samples.imeiMaskSample
     */
    public fun mask(
        value: String,
        visibleStart: Int = 0,
        visibleEnd: Int = 4,
        maskChar: Char = 'X',
    ): String {
        // Maskers never throw — display-safe by contract.
        // Operate on the raw value as-is (no isValid check, no normalize call — per OQ-10).
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
     * Maximum accepted length for a sanitized IMEI input.
     *
     * IMEI is always 15 digits. Any sanitized input longer than this is over-length.
     */
    public val maxLength: Int = 15

    /**
     * Set of characters accepted by [sanitize] and [validateProgressive] for IMEI.
     *
     * IMEI accepts only ASCII decimal digits (`'0'..'9'`). Any other character is stripped by
     * [sanitize] and triggers [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT]
     * inside [validateProgressive].
     *
     * Exposed as `Set<Char>` for uniform caller-side `if (char in IMEI.allowedChars)` checks.
     * The constant is top-level to avoid per-call allocation.
     */
    public val allowedChars: Set<Char> = IMEI_ALLOWED_CHARS

    /**
     * Strips all characters not in [allowedChars] from [rawInput] and truncates to [maxLength].
     *
     * This is a pure, idempotent function — `sanitize(sanitize(x)) == sanitize(x)` for all inputs.
     *
     * @param rawInput Any string, including pasted values with spaces or symbols.
     * @return A string containing only digits, at most [maxLength] characters long.
     * @sample io.github.kotindia.samples.imeiSanitizeSample
     */
    public fun sanitize(rawInput: String): String = rawInput.filter { it in IMEI_ALLOWED_CHARS }.take(maxLength)

    /**
     * Validates an IMEI for incremental, as-you-type input.
     *
     * Callers should run [sanitize] first; internal spaces will return [ProgressiveResult.Invalid]
     * with [InvalidReason.INVALID_FORMAT].
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
     * @param value IMEI input to evaluate, possibly partial. Callers should run [sanitize]
     *   first; internal spaces will return [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT].
     * @return A [ProgressiveResult] indicating the current state of the input.
     * @sample io.github.kotindia.samples.imeiValidateProgressiveSample
     */
    public fun validateProgressive(value: String): ProgressiveResult {
        // Step 1: trim whitespace
        val normalized = value.trim()

        // Step 2: empty check
        if (normalized.isEmpty()) return ProgressiveResult.Empty

        // Step 3: bad-char check — fires BEFORE length check (AC4 priority)
        if (normalized.any { it !in IMEI_ALLOWED_CHARS }) {
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
            return ProgressiveResult.Typing(visualText = normalized) // digits as-is per OQ-14-2
        }

        // Step 6: complete — delegate to full validate()
        return when (val result = validate(normalized)) {
            is ValidationResult.Valid -> ProgressiveResult.Valid
            is ValidationResult.Invalid -> ProgressiveResult.Invalid(result.reason, ValidationContext.None)
        }
    }

    // ---------------------------------------------------------------------------
    // Private normalisation helper
    // IMEI is all-digits — no uppercase transform needed.
    // Only whitespace is stripped; hyphens are NOT stripped (they hit INVALID_FORMAT).
    // ---------------------------------------------------------------------------
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "")
}
