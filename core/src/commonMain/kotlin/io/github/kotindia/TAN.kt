// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

// Top-level private constant — constructed once at class-load time, not per call.
private val TAN_ALLOWED_CHARS: Set<Char> =
    buildSet {
        for (c in 'A'..'Z') add(c)
        for (c in 'a'..'z') add(c)
        for (c in '0'..'9') add(c)
    }

/**
 * Validator and formatter for Indian Tax Deduction Account Numbers (TAN).
 *
 * TAN is a 10-character alphanumeric code issued by the Indian Income Tax Department
 * to entities required to deduct or collect tax at source (TDS/TCS). It is mandatory
 * on all TDS/TCS returns, certificates, and challans. TANs are issued and administered
 * under Section 203A of the Income Tax Act, 1961.
 *
 * Format: `[A-Z]{4}[0-9]{5}[A-Z]` — 10 characters total
 * - Characters 1–4: jurisdiction code (uppercase letters, typically city/region abbreviation)
 * - Characters 5–9: five-digit sequential number assigned by the Income Tax Department
 * - Character 10: single uppercase letter (structural check character; no public algorithm exists)
 *
 * **No checksum:** The Income Tax Department does not publish a checksum algorithm for TAN.
 * Validation is structural (format) only.
 *
 * **No `mask()`:** TAN is a public business identifier — entities publish their TAN on invoices
 * and tax certificates. No masking is required or provided per KotIndia PII policy (§3.5).
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw uppercase: `"MUMD12345A"`
 * - Lowercase: `"mumd12345a"` — normalised to uppercase
 * - Mixed case: `"MumD12345a"` — normalised to uppercase
 * - With spaces: `"MUMD 12345 A"` — internal whitespace stripped
 * - Whitespace padded: `" MUMD12345A "` — leading/trailing whitespace trimmed
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.tanSample
 */
@Suppress("ClassName") // TAN is a universal acronym — preserved for consumer readability
public object TAN {
    private const val EXPECTED_LENGTH = 10
    private val TAN_REGEX = Regex("^[A-Z]{4}[0-9]{5}[A-Z]$")

    /**
     * Validates an Indian Tax Deduction Account Number (TAN).
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers copy-paste with spaces)
     * 3. Uppercase all characters
     *
     * After normalisation, checks are applied in order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if length is not 10
     * - [InvalidReason.INVALID_FORMAT] if the string does not match `^[A-Z]{4}[0-9]{5}[A-Z]$`
     *
     * The regex encodes all structural constraints simultaneously:
     * - Characters 1–4 must be `[A-Z]`
     * - Characters 5–9 must be `[0-9]`
     * - Character 10 must be `[A-Z]`
     *
     * **Note:** Hyphens and other special characters are not stripped. `"MUMD-2345A"`
     * normalises to a 10-character string but fails regex and returns [InvalidReason.INVALID_FORMAT].
     *
     * @param value Raw TAN string in any common input form.
     * @return [ValidationResult.Valid] if the code is a valid TAN,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.tanValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)

        if (normalized.isBlank()) {
            return ValidationResult.Invalid(InvalidReason.EMPTY)
        }
        if (normalized.length != EXPECTED_LENGTH) {
            return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        }
        if (!TAN_REGEX.matches(normalized)) {
            return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        }
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw TAN string.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.tanIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a TAN to canonical form: 10-character uppercase, no separators.
     *
     * Applies the same normalisation as [validate] before formatting.
     * TAN has no published display separator convention — output is the raw
     * uppercase string: e.g. `"MUMD12345A"`.
     *
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw TAN string.
     * @return Canonical 10-character uppercase TAN string.
     * @throws IllegalArgumentException if [value] is not a valid TAN.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.tanFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "TAN.format requires a valid TAN; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    // ---------------------------------------------------------------------------
    // Progressive validation API (Slice 14 / Phase 2)
    // ---------------------------------------------------------------------------

    /**
     * Maximum accepted length for a sanitized TAN input.
     *
     * TAN is always 10 characters. Any sanitized input longer than this is over-length.
     */
    public val maxLength: Int = 10

    /**
     * Set of characters accepted by [sanitize] and [validateProgressive] for TAN.
     *
     * TAN accepts ASCII uppercase letters (`'A'..'Z'`), lowercase letters (`'a'..'z'`),
     * and decimal digits (`'0'..'9'`). Lowercase letters are normalised to uppercase by
     * [validateProgressive]. Any other character is stripped by [sanitize].
     *
     * **Note:** lowercase letters ARE included in this set so callers can pass raw user input
     * directly to `if (char in TAN.allowedChars)` UI filters without pre-uppercasing. The
     * validator normalises internally via [validate] and [validateProgressive].
     *
     * Exposed as `Set<Char>` for uniform caller-side `if (char in TAN.allowedChars)` checks.
     * The constant is top-level to avoid per-call allocation.
     */
    public val allowedChars: Set<Char> = TAN_ALLOWED_CHARS

    /**
     * Strips all characters not in [allowedChars] from [rawInput] and truncates to [maxLength].
     *
     * This is a pure, idempotent function — `sanitize(sanitize(x)) == sanitize(x)` for all inputs.
     *
     * @param rawInput Any string, including pasted values with spaces or symbols.
     * @return A string containing only alphanumeric chars, at most [maxLength] characters long.
     * @sample io.github.kotindia.samples.tanSanitizeSample
     */
    public fun sanitize(rawInput: String): String = rawInput.filter { it in TAN_ALLOWED_CHARS }.take(maxLength)

    /**
     * Validates a TAN for incremental, as-you-type input.
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
     * @param value TAN input to evaluate, possibly partial. Callers should run [sanitize]
     *   first; internal spaces will return [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT].
     * @return A [ProgressiveResult] indicating the current state of the input.
     * @sample io.github.kotindia.samples.tanValidateProgressiveSample
     */
    public fun validateProgressive(value: String): ProgressiveResult {
        // Step 1: trim whitespace
        val normalized = value.trim()

        // Step 2: empty check
        if (normalized.isEmpty()) return ProgressiveResult.Empty

        // Step 3: bad-char check — fires BEFORE length check (AC4 priority)
        if (normalized.any { it !in TAN_ALLOWED_CHARS }) {
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
            return ProgressiveResult.Typing(visualText = normalized.uppercase())
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
     * Normalises a raw TAN string:
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers copy-paste with spaces)
     * 3. Uppercase all characters (TAN is case-insensitive in real-world usage;
     *    canonical form is uppercase per Income Tax Department)
     */
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "").uppercase()
}
