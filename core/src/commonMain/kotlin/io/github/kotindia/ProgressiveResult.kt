// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * The result of a `validateProgressive` call on a KotIndia validator.
 *
 * `validateProgressive` is designed for incremental, as-you-type validation in form fields.
 * It returns one of four states that map directly to common UI patterns:
 *
 * - [Empty] — input is blank; show no error.
 * - [Typing] — input is in progress (partial, correct character class); show hint text from [Typing.visualText].
 * - [Valid] — input is complete and passes all checks; show success indicator.
 * - [Invalid] — input has a structural problem (wrong character class, too long, or complete but fails validation);
 *   show an appropriate error message keyed on [Invalid.reason].
 *
 * The critical invariant: **partial inputs of the correct character class NEVER return [Invalid]**.
 * A 5-character PAN in progress returns [Typing], even though it does not yet match the PAN regex.
 * This prevents false error states while the user is still typing.
 *
 * Evaluation order is strict — first matching condition wins:
 * 1. Whitespace-only or empty → [Empty]
 * 2. Any character not in `allowedChars` → [Invalid] with [InvalidReason.INVALID_FORMAT]
 * 3. Length > `maxLength` (all allowed chars) → [Invalid] with [InvalidReason.WRONG_LENGTH] and [ValidationContext.LengthMismatch]
 * 4. Length in `1..(maxLength - 1)`, all allowed chars → [Typing]
 * 5. Length == `maxLength`, all allowed chars, passes full `validate()` → [Valid] or [Invalid] as appropriate
 *
 * @sample io.github.kotindia.samples.aadhaarValidateProgressiveSample
 */
public sealed interface ProgressiveResult {
    /**
     * The input is empty or whitespace-only.
     *
     * UI hint: do not show any validation indicator. The field is untouched or cleared.
     */
    public data object Empty : ProgressiveResult

    /**
     * The input is a valid partial entry — correct character class, length below [maxLength].
     *
     * UI hint: show [visualText] in the field. Do not show an error.
     *
     * @param visualText The partially-entered input formatted for display (e.g. `"1234 5678"` for Aadhaar).
     *   Formatting rules are validator-specific — digit validators use space-grouping where a published
     *   display standard exists; alphanumeric validators uppercase the input.
     */
    public data class Typing(
        public val visualText: String,
    ) : ProgressiveResult

    /**
     * The input is complete and passes all format and checksum checks.
     *
     * UI hint: show a success indicator. The value is ready for submission.
     */
    public data object Valid : ProgressiveResult

    /**
     * The input has a structural problem.
     *
     * This state is only returned when:
     * - The input contains a character not in `allowedChars` (at any length), or
     * - The input length exceeds `maxLength` (all chars valid), or
     * - The input length equals `maxLength` but fails `validate()`.
     *
     * **Never returned for partial inputs of the correct character class** — that would be [Typing].
     *
     * @param reason Why the input is invalid. See [InvalidReason] for possible values.
     * @param context Structured diagnostic context. In slice 14, [ValidationContext.None] for format errors
     *   and [ValidationContext.LengthMismatch] for length violations. Slice 15 expands this.
     */
    public data class Invalid(
        public val reason: InvalidReason,
        public val context: ValidationContext = ValidationContext.None,
    ) : ProgressiveResult
}
