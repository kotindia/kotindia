// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * Validator and formatter for UPI Virtual Payment Addresses (VPA).
 *
 * A VPA (also called UPI ID) is a public payment handle used in NPCI UPI transactions.
 * Format: `username@psp` where `username` is an alphanumeric handle (dots, underscores,
 * dashes allowed) and `psp` is the PSP/bank handle (e.g. `ybl`, `oksbi`, `okhdfcbank`).
 *
 * **Accepted input forms** (all normalised before validation):
 * - Lowercase: `"user@ybl"` — canonical NPCI form
 * - Mixed case: `"User@YBL"` — normalised to `"user@ybl"`
 * - Uppercase: `"USER@YBL"` — normalised to `"user@ybl"`
 * - Whitespace padded: `" user@ybl "` — leading/trailing trimmed
 *
 * **Validation rules (post-normalization):**
 * - Maximum 50 characters total (NPCI spec)
 * - Username minimum 3 characters
 * - Exactly one `@` separating username and PSP
 * - Username: `[a-z0-9._-]{3,}`
 * - PSP: `[a-z0-9.-]+`
 *
 * **PSP allowlist:** Not enforced. NPCI adds PSPs regularly — a closed list would
 * produce false negatives on legitimate handles. Structural validation only.
 *
 * VPA is a public payment handle — shared openly to receive money. No `mask()`
 * method is provided. See the KotIndia API design rationale for details.
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.vpaSample
 */
@Suppress("ClassName") // VPA is a universal acronym — preserved for consumer readability
public object VPA {
    private const val MAX_LENGTH = 50
    private val FORMAT_REGEX = Regex("^[a-z0-9._-]{3,}@[a-z0-9.-]+$")

    /**
     * Validates a UPI Virtual Payment Address (VPA).
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Lowercase all characters (NPCI canonical form)
     *
     * **Note:** Internal whitespace is NOT stripped — VPAs never contain internal spaces,
     * and a space inside a VPA is a format violation.
     *
     * After normalisation, checks are applied in order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if total length exceeds 50
     * - [InvalidReason.INVALID_FORMAT] if the string does not match `username@psp` pattern
     *
     * **PSP allowlist not enforced.** `"user@anypsp"` is structurally valid. NPCI adds
     * PSPs regularly; a closed list would produce false negatives.
     *
     * @param value Raw VPA string in any common input form.
     * @return [ValidationResult.Valid] if the VPA is structurally valid,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.vpaValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)

        if (normalized.isBlank()) {
            return ValidationResult.Invalid(InvalidReason.EMPTY)
        }
        if (normalized.length > MAX_LENGTH) {
            return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        }
        if (!FORMAT_REGEX.matches(normalized)) {
            return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        }
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw VPA string.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.vpaIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a VPA to canonical form: lowercase, no separators.
     *
     * Applies the same normalisation as [validate] before formatting.
     * NPCI canonical form for VPAs is lowercase — e.g. `"user@ybl"`.
     *
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw VPA string.
     * @return Canonical lowercase VPA string.
     * @throws IllegalArgumentException if [value] is not a valid VPA.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.vpaFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "VPA.format requires a valid VPA; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    // ---------------------------------------------------------------------------
    // Private normalisation helper
    // NOT part of the public API.
    // ---------------------------------------------------------------------------

    /**
     * Normalises a raw VPA string:
     * 1. Trim leading/trailing whitespace
     * 2. Lowercase all characters (NPCI canonical form — VPAs are case-insensitive
     *    but NPCI resolves and stores them in lowercase)
     *
     * **Crucially different from IFSC:** internal whitespace is NOT stripped.
     * A space inside a VPA (e.g. `"user @ybl"`) is INVALID_FORMAT, not a
     * copy-paste variant. Stripping would silently mask a user input error.
     */
    private fun normalize(value: String): String = value.trim().lowercase()
}
