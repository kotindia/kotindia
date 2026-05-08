// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

// Top-level private constant — constructed once at class-load time, not per call.
private val PINCODE_ALLOWED_CHARS: Set<Char> =
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
    // Progressive validation API (Slice 14 / Phase 2)
    // ---------------------------------------------------------------------------

    /**
     * Maximum accepted length for a sanitized Pincode input.
     *
     * Indian PIN codes are always 6 digits. Any sanitized input longer than this is over-length.
     */
    public val maxLength: Int = 6

    /**
     * Set of characters accepted by [sanitize] and [validateProgressive] for Pincode.
     *
     * Pincode accepts only ASCII decimal digits (`'0'..'9'`). Any other character is stripped by
     * [sanitize] and triggers [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT]
     * inside [validateProgressive].
     *
     * Exposed as `Set<Char>` for uniform caller-side `if (char in Pincode.allowedChars)` checks.
     * The constant is top-level to avoid per-call allocation.
     */
    public val allowedChars: Set<Char> = PINCODE_ALLOWED_CHARS

    /**
     * Strips all characters not in [allowedChars] from [rawInput] and truncates to [maxLength].
     *
     * This is a pure, idempotent function — `sanitize(sanitize(x)) == sanitize(x)` for all inputs.
     *
     * @param rawInput Any string, including pasted values with spaces or symbols.
     * @return A string containing only digits, at most [maxLength] characters long.
     * @sample io.github.kotindia.samples.pincodeSanitizeSample
     */
    public fun sanitize(rawInput: String): String = rawInput.filter { it in PINCODE_ALLOWED_CHARS }.take(maxLength)

    /**
     * Validates a Pincode for incremental, as-you-type input.
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
     * @param value Pincode input to evaluate, possibly partial. Callers should run [sanitize]
     *   first; internal spaces will return [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT].
     * @return A [ProgressiveResult] indicating the current state of the input.
     * @sample io.github.kotindia.samples.pincodeValidateProgressiveSample
     */
    public fun validateProgressive(value: String): ProgressiveResult {
        // Step 1: trim whitespace
        val normalized = value.trim()

        // Step 2: empty check
        if (normalized.isEmpty()) return ProgressiveResult.Empty

        // Step 3: bad-char check — fires BEFORE length check (AC4 priority)
        if (normalized.any { it !in PINCODE_ALLOWED_CHARS }) {
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
