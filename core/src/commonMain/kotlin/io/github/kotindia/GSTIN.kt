// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import io.github.kotindia.internal.GstinChecksum

/**
 * Validator and formatter for GSTIN (Goods and Services Tax Identification Number).
 *
 * GSTIN is the unique 15-character identifier assigned to every GST-registered business
 * in India by the GSTN (Goods and Services Tax Network). It is required for:
 * - Issuing GST-compliant tax invoices (B2B and B2C)
 * - Filing GSTR-1, GSTR-3B, and other GST returns (CBIC mandate)
 * - Input Tax Credit (ITC) reconciliation
 * - E-waybill generation (goods movement over ₹50,000)
 * - E-invoicing (mandatory for businesses with turnover >₹5 crore from 2023)
 * - Vendor onboarding / KYC by procuring entities
 *
 * **GSTIN structure (15 characters):**
 * - Positions 0-1:  2-digit GST state code (01–38, including legacy 25 and 28)
 * - Positions 2-11: Taxpayer's PAN (10 characters, [A-Z]{5}[0-9]{4}[A-Z] with category rules)
 * - Position 12:    Entity number (1 alphanumeric, typically '1'–'9' or 'A'–'Z')
 * - Position 13:    Literal 'Z' (fixed per GSTN specification)
 * - Position 14:    GSTN base-36 checksum character (computed via [io.github.kotindia.internal.GstinChecksum])
 *
 * **Checksum:** GSTN base-36 weighted-sum algorithm.
 * Reference: https://github.com/tk120404/gst and https://github.com/mastermunj/format-utils
 *
 * **Valid state codes (01–38):**
 * All 38 assigned GST state codes are accepted, including:
 * - Legacy code 25 (Daman & Diu — merged into 26 in 2020; existing GSTINs remain valid)
 * - Legacy code 28 (old Andhra Pradesh — replaced by 37 post-2014; existing GSTINs remain valid)
 * - Code 38 (Ladakh — created post-2019 reorganisation)
 *
 * **GSTIN is a public business identifier** — searchable on the GSTN taxpayer portal
 * (https://services.gstn.gov.in/). It is NOT personal PII. **No [mask] method is provided.**
 *
 * **Accepted input forms** (all normalised before validation):
 * - Canonical uppercase: `"27AAPFU0939F1ZV"`
 * - Lowercase:           `"27aapfu0939f1zv"` — normalised to uppercase
 * - With internal space: `"27 AAPFU0939F1ZV"` — whitespace stripped
 * - Whitespace-padded:   `" 27AAPFU0939F1ZV "` — trimmed + stripped
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.gstinSample
 */
@Suppress("ClassName") // GSTIN is a universal acronym — preserved for consumer readability
public object GSTIN {
    private const val EXPECTED_LENGTH = 15
    private val GSTIN_REGEX = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]$")

    private val VALID_STATE_CODES: Set<String> =
        setOf(
            "01",
            "02",
            "03",
            "04",
            "05",
            "06",
            "07",
            "08",
            "09",
            "10",
            "11",
            "12",
            "13",
            "14",
            "15",
            "16",
            "17",
            "18",
            "19",
            "20",
            "21",
            "22",
            "23",
            "24",
            "25", // LEGACY: Daman & Diu — merged into 26 in 2020; existing GSTINs remain valid
            "26",
            "27",
            "28", // LEGACY: old Andhra Pradesh code — replaced by 37 post-2014; existing GSTINs remain valid
            "29",
            "30",
            "31",
            "32",
            "33",
            "34",
            "35",
            "36",
            "37",
            "38", // Ladakh — created post-2019 reorganisation (R3 risk: absent from pre-2020 validators)
        )

    /**
     * Validates a GSTIN.
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers copy-paste with spaces)
     * 3. Uppercase all characters
     *
     * After normalisation, checks are applied in this order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if normalised length is not 15
     * - [InvalidReason.INVALID_FORMAT] if the string does not match the GSTIN regex
     *   (`^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]$`)
     * - [InvalidReason.INVALID_PREFIX] if the 2-digit state code is not in the valid set (01–38)
     * - [InvalidReason.INVALID_CATEGORY] if the 4th character of the embedded PAN (position 5
     *   of GSTIN, 0-based) is not a valid PAN entity category (one of: P C H A B G J L F T)
     * - [InvalidReason.INVALID_CHECKSUM] if the GSTN base-36 checksum fails
     *
     * **Never throws.** Returns [ValidationResult.Invalid] for all invalid inputs.
     *
     * @param value Raw GSTIN string in any common input form.
     * @return [ValidationResult.Valid] if the GSTIN passes all checks,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.gstinValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)
        if (normalized.isBlank()) return ValidationResult.Invalid(InvalidReason.EMPTY)
        if (normalized.length != EXPECTED_LENGTH) return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        if (!GSTIN_REGEX.matches(normalized)) return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        if (normalized.substring(0, 2) !in VALID_STATE_CODES) {
            return ValidationResult.Invalid(InvalidReason.INVALID_PREFIX)
        }
        // PAN-embedded check: positions 2-11 must form a valid PAN with a valid category char.
        // Reuses PAN.validate() to avoid duplicating the category set and regex (same module).
        // NOTE: Marcus arch flag — this coupling is intentional and documented in the PR.
        // If Marcus prefers decoupling, extract VALID_PAN_CATEGORY_CHARS to internal/PanValidatorUtils.kt.
        val panResult = PAN.validate(normalized.substring(2, 12))
        if (panResult is ValidationResult.Invalid) {
            return panResult // bubble up INVALID_CATEGORY or INVALID_FORMAT from embedded PAN
        }
        if (!GstinChecksum.isChecksumValid(normalized)) {
            return ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM)
        }
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw GSTIN string in any common input form.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.gstinIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a valid GSTIN to its canonical form.
     *
     * Output: 15-character uppercase string with **no separator**.
     * Example: `"27aapfu0939f1zv"` → `"27AAPFU0939F1ZV"`.
     *
     * Applies the same normalisation as [validate] (trim + strip whitespace + uppercase)
     * before formatting. Throws [IllegalArgumentException] if the input is not a valid GSTIN.
     *
     * This method is idempotent: `format(format(x)) == format(x)` for any valid input.
     *
     * @param value Raw GSTIN string in any common input form.
     * @return Canonical 15-character uppercase GSTIN string with no separator.
     * @throws IllegalArgumentException if [value] is not a valid GSTIN.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.gstinFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "GSTIN.format requires a valid GSTIN; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    // ---------------------------------------------------------------------------
    // Private normalisation helper
    // NOT part of the public API.
    // Matches PAN/IFSC normalisation pattern: trim + strip all whitespace + uppercase.
    // GSTIN is uppercase alphanumeric — no hyphen stripping needed.
    // ---------------------------------------------------------------------------
    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "").uppercase()
}
