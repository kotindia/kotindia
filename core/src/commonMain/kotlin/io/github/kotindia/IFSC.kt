// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

// Top-level private constant — constructed once at class-load time, not per call.
private val IFSC_ALLOWED_CHARS: Set<Char> =
    buildSet {
        for (c in 'A'..'Z') add(c)
        for (c in 'a'..'z') add(c)
        for (c in '0'..'9') add(c)
    }

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
    // Progressive validation API (Slice 14 / Phase 2)
    // ---------------------------------------------------------------------------

    /**
     * Maximum accepted length for a sanitized IFSC input.
     *
     * IFSC is always 11 characters. Any sanitized input longer than this is over-length.
     */
    public val maxLength: Int = 11

    /**
     * Set of characters accepted by [sanitize] and [validateProgressive] for IFSC.
     *
     * IFSC accepts ASCII uppercase letters (`'A'..'Z'`), lowercase letters (`'a'..'z'`),
     * and decimal digits (`'0'..'9'`). Lowercase letters are normalised to uppercase by
     * [validateProgressive]. Any other character is stripped by [sanitize].
     *
     * **Note:** lowercase letters ARE included in this set so callers can pass raw user input
     * directly to `if (char in IFSC.allowedChars)` UI filters without pre-uppercasing. The
     * validator normalises internally via [validate] and [validateProgressive].
     *
     * Exposed as `Set<Char>` for uniform caller-side checks. The constant is top-level to
     * avoid per-call allocation.
     */
    public val allowedChars: Set<Char> = IFSC_ALLOWED_CHARS

    /**
     * Strips all characters not in [allowedChars] from [rawInput] and truncates to [maxLength].
     *
     * This is a pure, idempotent function — `sanitize(sanitize(x)) == sanitize(x)` for all inputs.
     *
     * @param rawInput Any string, including pasted values with spaces or symbols.
     * @return A string containing only alphanumeric chars, at most [maxLength] characters long.
     * @sample io.github.kotindia.samples.ifscSanitizeSample
     */
    public fun sanitize(rawInput: String): String = rawInput.filter { it in IFSC_ALLOWED_CHARS }.take(maxLength)

    /**
     * Validates an IFSC for incremental, as-you-type input.
     *
     * Callers should run [sanitize] first; internal spaces will return [ProgressiveResult.Invalid]
     * with [InvalidReason.INVALID_FORMAT].
     *
     * Returns one of four [ProgressiveResult] states. The critical invariant: partial inputs of
     * the correct character class (alphanumeric) NEVER return [ProgressiveResult.Invalid] — partial
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
     * @param value IFSC input to evaluate, possibly partial. Callers should run [sanitize]
     *   first; internal spaces will return [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT].
     * @return A [ProgressiveResult] indicating the current state of the input.
     * @sample io.github.kotindia.samples.ifscValidateProgressiveSample
     */
    public fun validateProgressive(value: String): ProgressiveResult {
        // Step 1: trim whitespace
        val normalized = value.trim()

        // Step 2: empty check
        if (normalized.isEmpty()) return ProgressiveResult.Empty

        // Step 3: bad-char check — fires BEFORE length check (AC4 priority)
        if (normalized.any { it !in IFSC_ALLOWED_CHARS }) {
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

    // ---------------------------------------------------------------------------
    // Private progressive helpers
    // ---------------------------------------------------------------------------

    // formatPartial: operates on sanitized string, uppercases for display.
    // IFSC has no standardised spacing — uppercase as-is.
    private fun formatPartial(value: String): String = value.uppercase()
}
