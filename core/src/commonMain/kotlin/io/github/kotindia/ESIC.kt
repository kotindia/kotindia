// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

// Top-level private constant — constructed once at class-load time, not per call.
private val ESIC_ALLOWED_CHARS: Set<Char> =
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
        if (normalized.any { it !in ESIC_ALLOWED_CHARS }) {
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
    // Progressive validation API (Slice 14 / Phase 2)
    // ---------------------------------------------------------------------------

    /**
     * Maximum accepted length for a sanitized ESIC input.
     *
     * ESIC numbers are always 17 digits. Any sanitized input longer than this is over-length.
     */
    public val maxLength: Int = 17

    /**
     * Set of characters accepted by [sanitize] and [validateProgressive] for ESIC.
     *
     * ESIC accepts only ASCII decimal digits (`'0'..'9'`). Any other character is stripped by
     * [sanitize] and triggers [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT]
     * inside [validateProgressive].
     *
     * Exposed as `Set<Char>` for uniform caller-side `if (char in ESIC.allowedChars)` checks.
     * The constant is top-level to avoid per-call allocation.
     */
    public val allowedChars: Set<Char> = ESIC_ALLOWED_CHARS

    /**
     * Strips all characters not in [allowedChars] from [rawInput] and truncates to [maxLength].
     *
     * This is a pure, idempotent function — `sanitize(sanitize(x)) == sanitize(x)` for all inputs.
     *
     * @param rawInput Any string, including pasted values with spaces or symbols.
     * @return A string containing only digits, at most [maxLength] characters long.
     * @sample io.github.kotindia.samples.esicSanitizeSample
     */
    public fun sanitize(rawInput: String): String = rawInput.filter { it in ESIC_ALLOWED_CHARS }.take(maxLength)

    /**
     * Validates an ESIC number for incremental, as-you-type input.
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
     * @param value ESIC number input to evaluate, possibly partial. Callers should run [sanitize]
     *   first; internal spaces will return [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT].
     * @return A [ProgressiveResult] indicating the current state of the input.
     * @sample io.github.kotindia.samples.esicValidateProgressiveSample
     */
    public fun validateProgressive(value: String): ProgressiveResult {
        // Step 1: trim whitespace
        val normalized = value.trim()

        // Step 2: empty check
        if (normalized.isEmpty()) return ProgressiveResult.Empty

        // Step 3: bad-char check — fires BEFORE length check (AC4 priority)
        if (normalized.any { it !in ESIC_ALLOWED_CHARS }) {
            return ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None)
        }

        // Step 4: over-length check
        if (normalized.length > maxLength) {
            return ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = maxLength, actual = normalized.length),
            )
        }

        // Step 5: partial check — digits as-is per OQ-14-2
        if (normalized.length < maxLength) {
            return ProgressiveResult.Typing(visualText = normalized)
        }

        // Step 6: complete.
        // At this point normalized is exactly maxLength digits (Steps 2+3+4+5 guarantee it).
        // ESIC has no checksum — any 17-digit string is valid. Always ProgressiveResult.Valid.
        return ProgressiveResult.Valid
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
