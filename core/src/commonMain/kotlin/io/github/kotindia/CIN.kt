// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * Validator and formatter for Indian Company Identification Numbers (CIN).
 *
 * CIN is a 21-character Ministry of Corporate Affairs (MCA) code assigned to
 * every company registered in India under the Companies Act. It appears on all
 * corporate filings, invoices, letterheads, and is publicly searchable on the
 * MCA21 portal.
 *
 * **Structure:**
 * ```
 * [LU][0-9]{5}[A-Z]{2}[0-9]{4}[A-Z]{3}[0-9]{6}
 * ```
 * - Position 0:      Listed (`L`) or Unlisted (`U`)
 * - Positions 1–5:   5-digit NIC 2008 industry code
 * - Positions 6–7:   2-letter MCA state code (structural only — see note)
 * - Positions 8–11:  4-digit year of incorporation
 * - Positions 12–14: 3-letter company class (PTC, PLC, NPL, GAP, GAT, etc.)
 * - Positions 15–20: 6-digit MCA registration number
 *
 * **Note on state codes:** positions 6–7 accept any 2-letter `[A-Z]` combination.
 * MCA's authoritative state letter-code list is large and mutable; accepting any
 * 2-letter pair is correct for structural validation. State code semantics are
 * not enforced by this validator.
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw uppercase: `"U72200KA2013PTC097389"`
 * - Lowercase: `"u72200ka2013ptc097389"` — normalised to uppercase
 * - Mixed case: `"U72200ka2013ptc097389"` — normalised to uppercase
 * - With spaces: `"U72200 KA2013 PTC097389"` — internal whitespace stripped
 * - Whitespace padded: `" U72200KA2013PTC097389 "` — trimmed
 *
 * CIN is a public business identifier publicly searchable on the MCA21 portal —
 * no `mask()` method is provided. See the KotIndia API design rationale for details.
 *
 * This object is stateless and thread-safe. No checksum exists for CIN —
 * validation is structural only per MCA specification.
 *
 * @suppress ClassName CIN is a universal acronym — preserved for consumer readability.
 * @sample io.github.kotindia.samples.cinSample
 */
@Suppress("ClassName") // CIN is a universal acronym — preserved for consumer readability
public object CIN {
    private const val EXPECTED_LENGTH = 21

    // WARNING: Positions 6–7 (state code) accept any [A-Z]{2}. MCA does not publish
    // an authoritative machine-readable state-code list, and the list changes with
    // new UTs / state bifurcations. Open-list design is intentional — see PRD §3.5.
    private val FORMAT_REGEX = Regex("^[LU][0-9]{5}[A-Z]{2}[0-9]{4}[A-Z]{3}[0-9]{6}$")

    /**
     * Validates a Company Identification Number (CIN).
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers copy-paste with spaces)
     * 3. Uppercase all characters
     *
     * After normalisation, checks are applied in order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if length is not 21
     * - [InvalidReason.INVALID_PREFIX] if position 0 is not `L` or `U`
     * - [InvalidReason.INVALID_FORMAT] if the string does not match the CIN regex
     *
     * @param value Raw CIN string in any common input form.
     * @return [ValidationResult.Valid] if the code is a valid MCA CIN,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.cinValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)

        if (normalized.isBlank()) {
            return ValidationResult.Invalid(InvalidReason.EMPTY)
        }
        if (normalized.length != EXPECTED_LENGTH) {
            return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        }
        if (normalized[0] != 'L' && normalized[0] != 'U') {
            return ValidationResult.Invalid(InvalidReason.INVALID_PREFIX)
        }
        if (!FORMAT_REGEX.matches(normalized)) {
            return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        }
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw CIN string.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.cinIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a CIN to canonical form: 21-character uppercase, no separators.
     *
     * Applies the same normalisation as [validate] before formatting.
     * MCA publishes CINs without display separators — output is the raw
     * uppercase string: e.g. `"U72200KA2013PTC097389"`.
     *
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw CIN string.
     * @return Canonical 21-character uppercase CIN string.
     * @throws IllegalArgumentException if [value] is not a valid CIN.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.cinFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "CIN.format requires a valid CIN; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    // ---------------------------------------------------------------------------
    // Private normalisation helper
    // NOT part of the public API.
    // ---------------------------------------------------------------------------

    /**
     * Normalises a raw CIN string:
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers copy-paste with spaces)
     * 3. Uppercase all characters (CIN is case-insensitive in real-world usage;
     *    canonical form is uppercase per MCA)
     *
     * Follows the reference uppercase-normalisation pattern established by IFSC slice.
     */
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "").uppercase()
}
