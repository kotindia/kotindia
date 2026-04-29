// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * The reason a validator returned [ValidationResult.Invalid].
 *
 * Each value identifies the specific validation failure that was detected.
 * Callers switching on [InvalidReason] should use an `else` branch to remain
 * forward-compatible with new values added in future releases.
 *
 * ```kotlin
 * val result = PAN.validate("ABCDE1234F")
 * if (result is ValidationResult.Invalid) {
 *     when (result.reason) {
 *         InvalidReason.EMPTY          -> showError("Input is empty")
 *         InvalidReason.WRONG_LENGTH   -> showError("Wrong number of characters")
 *         InvalidReason.INVALID_FORMAT -> showError("Invalid character pattern")
 *         else                         -> showError("Validation failed: ${result.reason}")
 *     }
 * }
 * ```
 *
 * @sample io.github.kotindia.samples.invalidReasonSample
 */
public enum class InvalidReason {
    /** Input is blank or empty — validator received `""` or a whitespace-only string. */
    EMPTY,

    /** Input has the wrong character count for this document type (e.g., PAN must be exactly 10 chars). */
    WRONG_LENGTH,

    /** Input does not match the required character pattern (regex) for this document type. */
    INVALID_FORMAT,

    /** Input fails the document-specific checksum algorithm (Verhoeff for Aadhaar, Luhn for IMEI, GSTN for GSTIN). */
    INVALID_CHECKSUM,

    /** Input has an unrecognised prefix (e.g., pincode starting with 0, mobile not starting with 6–9). */
    INVALID_PREFIX,

    /** Input encodes an invalid category code (e.g., PAN 4th character not in {P, C, H, A, B, G, J, L, F, T}). */
    INVALID_CATEGORY,
}
