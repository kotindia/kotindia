// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

// Top-level private constant — constructed once at class-load time, not per call.
private val UAN_ALLOWED_CHARS: Set<Char> =
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
 * Validator, formatter, and masker for EPFO Universal Account Numbers (UAN).
 *
 * UAN is a 12-digit number issued by the Employees' Provident Fund Organisation (EPFO)
 * of India. It uniquely identifies each EPFO member across employers and persists
 * throughout their career.
 *
 * Format: 12 consecutive digits — no checksum algorithm is publicly documented by EPFO.
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw digits: `"101234567890"`
 * - With spaces: `"1012 3456 7890"` — internal whitespace stripped
 * - Whitespace padded: `" 101234567890 "` — leading/trailing whitespace trimmed
 *
 * **Note:** Hyphens are not stripped — not a documented UAN input convention.
 * `"1012-3456-7890"` normalises to `"1012-3456-7890"` (13 chars after whitespace strip)
 * → [InvalidReason.WRONG_LENGTH].
 *
 * **UAN is employment PII** — links to individual's EPF contributions and employment history.
 * Always mask before displaying in UI or logging. See [mask].
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.uanSample
 */
@Suppress("ClassName") // UAN is a universal acronym — preserved for consumer readability
public object UAN {
    private const val EXPECTED_LENGTH = 12

    /**
     * Validates an EPFO Universal Account Number (UAN).
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace
     *
     * After normalisation, checks are applied in order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if length is not 12
     * - [InvalidReason.INVALID_FORMAT] if any character is not a digit (`0–9`)
     *
     * **No checksum:** EPFO has not published a public checksum algorithm for UAN.
     * **No prefix rule:** EPFO has not published a first-digit constraint for UAN.
     * All 12-digit, all-digit values are accepted.
     *
     * @param value Raw UAN string in any common input form.
     * @return [ValidationResult.Valid] if the UAN passes format validation,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.uanValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)

        if (normalized.isBlank()) return ValidationResult.Invalid(InvalidReason.EMPTY)
        if (normalized.length != EXPECTED_LENGTH) return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        if (normalized.any { it !in UAN_ALLOWED_CHARS }) return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw UAN string.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.uanIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a UAN to canonical form: 12 consecutive digits, no separators.
     *
     * EPFO enforces no display separator convention. The canonical form is
     * the raw 12-digit string with all whitespace stripped.
     * Example: `UAN.format("1012 3456 7890")` → `"101234567890"`.
     *
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw UAN string.
     * @return Canonical 12-digit UAN string with no separators.
     * @throws IllegalArgumentException if [value] is not a valid UAN.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.uanFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "UAN.format requires a valid UAN; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    /**
     * Masks a UAN for PII-safe display.
     *
     * **UAN is employment PII** — always mask before displaying in UI or logging.
     *
     * Masking operates on the **raw input string** (not normalised). This is intentional —
     * the masker is a display utility that works character-by-character on whatever string
     * it receives. Callers who want to mask a normalised UAN should call [format] first:
     * `UAN.mask(UAN.format(value))`.
     *
     * **Default `(visibleStart=0, visibleEnd=4, maskChar='X')`:**
     * Shows last 4 characters, masks the rest.
     * `"101234567890"` → `"XXXXXXXX7890"` (last 4: `"7890"`)
     *
     * **Edge cases (all safe — never throws):**
     * - Empty input → returns `""`.
     * - `visibleStart + visibleEnd >= value.length` → entire string returned unmasked (overlap rule).
     * - Non-normalised input (spaces, hyphens) → masking applied character-by-character on the
     *   raw string. No normalisation is performed.
     *
     * @param value Raw UAN string to mask. No normalisation is applied.
     * @param visibleStart Number of leading characters to show unmasked. Default: `0`.
     * @param visibleEnd Number of trailing characters to show unmasked. Default: `4`.
     * @param maskChar Character to replace masked positions. Default: `'X'`.
     * @return Masked string. Never throws — display-safe by contract.
     * @sample io.github.kotindia.samples.uanMaskSample
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
     * Maximum accepted length for a sanitized UAN input.
     *
     * UAN is always 12 digits. Any sanitized input longer than this is over-length.
     */
    public val maxLength: Int = 12

    /**
     * Set of characters accepted by [sanitize] and [validateProgressive] for UAN.
     *
     * UAN accepts only ASCII decimal digits (`'0'..'9'`). Any other character is stripped by
     * [sanitize] and triggers [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT]
     * inside [validateProgressive].
     *
     * Exposed as `Set<Char>` for uniform caller-side `if (char in UAN.allowedChars)` checks.
     * The constant is top-level to avoid per-call allocation.
     */
    public val allowedChars: Set<Char> = UAN_ALLOWED_CHARS

    /**
     * Strips all characters not in [allowedChars] from [rawInput] and truncates to [maxLength].
     *
     * This is a pure, idempotent function — `sanitize(sanitize(x)) == sanitize(x)` for all inputs.
     *
     * @param rawInput Any string, including pasted values with spaces or symbols.
     * @return A string containing only digits, at most [maxLength] characters long.
     * @sample io.github.kotindia.samples.uanSanitizeSample
     */
    public fun sanitize(rawInput: String): String = rawInput.filter { it in UAN_ALLOWED_CHARS }.take(maxLength)

    /**
     * Validates a UAN for incremental, as-you-type input.
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
     * @param value UAN input to evaluate, possibly partial. Callers should run [sanitize]
     *   first; internal spaces will return [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT].
     * @return A [ProgressiveResult] indicating the current state of the input.
     * @sample io.github.kotindia.samples.uanValidateProgressiveSample
     */
    public fun validateProgressive(value: String): ProgressiveResult {
        // Step 1: trim whitespace
        val normalized = value.trim()

        // Step 2: empty check
        if (normalized.isEmpty()) return ProgressiveResult.Empty

        // Step 3: bad-char check — fires BEFORE length check (AC4 priority)
        if (normalized.any { it !in UAN_ALLOWED_CHARS }) {
            return ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None)
        }

        // Step 4: over-length check
        if (normalized.length > maxLength) {
            return ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = maxLength, actual = normalized.length),
            )
        }

        // Step 5: partial check — UAN: chunked(4).joinToString(" ") per PRD scope table
        if (normalized.length < maxLength) {
            return ProgressiveResult.Typing(visualText = normalized.chunked(4).joinToString(" "))
        }

        // Step 6: complete.
        // At this point normalized is exactly maxLength digits (Steps 2+3+4+5 guarantee it).
        // UAN has no checksum — any 12-digit string is valid. Always ProgressiveResult.Valid.
        return ProgressiveResult.Valid
    }

    // ---------------------------------------------------------------------------
    // Private normalisation helper
    // NOT part of the public API.
    // ---------------------------------------------------------------------------

    /**
     * Normalises a raw UAN string:
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace
     *
     * Hyphens are NOT stripped — not a documented UAN input convention.
     */
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "")
}
