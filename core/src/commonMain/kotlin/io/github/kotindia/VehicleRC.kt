// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

// Top-level private constant — constructed once at class-load time, not per call.
private val VEHICLE_RC_ALLOWED_CHARS: Set<Char> =
    buildSet {
        for (c in 'A'..'Z') add(c)
        for (c in 'a'..'z') add(c)
        for (c in '0'..'9') add(c)
    }

/**
 * Validator and formatter for Indian vehicle registration numbers (RC — Registration Certificate).
 *
 * Vehicle registration numbers are issued by state RTOs under MoRTH (Ministry of Road Transport
 * and Highways). Every vehicle registered in India receives an RC number that appears on its
 * licence plate, insurance policy, fitness certificate, toll records, and e-challan.
 *
 * **Format:** `[A-Z]{2}\d{1,2}[A-Z]{1,3}\d{4}` — total 8–11 characters after normalization:
 * - Characters 1–2: state/UT code (e.g. `MH`, `KA`, `DL`) — validated against a closed set
 *   of 38 MoRTH-assigned codes (see [VALID_STATE_CODES])
 * - Characters 3–4 (or 3): district code (1–2 digits, assigned by the district RTO)
 * - Next 1–3 characters: series code (uppercase letters)
 * - Last 4 characters: unique registration number (4 digits)
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw uppercase:   `"MH12AB1234"` — canonical form
 * - Lowercase:       `"mh12ab1234"` — normalised to uppercase
 * - With spaces:     `"MH 12 AB 1234"` — internal whitespace stripped
 * - With hyphens:    `"MH-12-AB-1234"` — hyphens stripped (common in printed documents)
 * - Mixed:           `"mh-12-ab-1234"` — normalised
 * - Padded:          `" MH12AB1234 "` — leading/trailing whitespace trimmed
 *
 * **No `mask()` method:** Vehicle registration numbers appear on physical licence plates that
 * are publicly visible on roads and in public databases. Per KotIndia PII policy (§3.5,
 * Public/semi-public category), no masking is provided or needed.
 *
 * **State codes — 38 entries:** All active MoRTH-issued state/UT vehicle prefixes, plus two
 * legacy codes still present on physical plates for vehicles registered before code changes:
 * - `OD` — Odisha (current, post-2011)
 * - `OR` — Odisha legacy (pre-2011 plates still legally on roads; aligned with DL Slice 10d,
 *   Marcus arch review fb_20260503_020005_f0a329)
 * - `DD` — Daman & Diu legacy (merged into DN in 2020; older physical plates still active)
 * - `UT` excluded — not a canonically MoRTH-issued vehicle prefix; active Uttarakhand code is `UK`
 *
 * Source: MoRTH vehicle registration code list, cross-referenced with public Vahan4 data
 * (https://vahan.parivahan.gov.in/) and DL Slice 10d state code alignment.
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.vehicleRCSample
 */
public object VehicleRC {
    private val RC_REGEX = Regex("^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$")

    /**
     * Closed set of 38 MoRTH-assigned vehicle registration state/UT prefix codes.
     *
     * Includes:
     * - 36 currently active state and UT codes (post JK+LA split and DD+DN merge)
     * - `OR` — Odisha legacy prefix (pre-2011; replaced by OD, but legacy plates still on roads)
     * - `DD` — Daman & Diu legacy prefix (merged into DN in 2020; older plates still active)
     *
     * Excludes:
     * - `UT` — not a canonically MoRTH-issued vehicle prefix; active Uttarakhand code is `UK`
     * - `BH` — Bharat Series (special category, not a geographic state code)
     *
     * Source: MoRTH vehicle prefix list, cross-referenced with Vahan4 public data
     * (https://vahan.parivahan.gov.in/) and aligned with DL Slice 10d per Marcus arch review.
     */
    private val VALID_STATE_CODES: Set<String> =
        setOf(
            "AN", // Andaman & Nicobar Islands (UT)
            "AP", // Andhra Pradesh
            "AR", // Arunachal Pradesh
            "AS", // Assam
            "BR", // Bihar
            "CG", // Chhattisgarh
            "CH", // Chandigarh (UT)
            "DD", // Daman & Diu (legacy; merged into DN 2020, older plates still active)
            "DL", // Delhi (UT)
            "DN", // Dadra & Nagar Haveli and Daman & Diu (merged UT, post-2020)
            "GA", // Goa
            "GJ", // Gujarat
            "HP", // Himachal Pradesh
            "HR", // Haryana
            "JH", // Jharkhand
            "JK", // Jammu & Kashmir (UT, post-2019)
            "KA", // Karnataka
            "KL", // Kerala
            "LA", // Ladakh (UT, post-2019)
            "LD", // Lakshadweep (UT)
            "MH", // Maharashtra
            "ML", // Meghalaya
            "MN", // Manipur
            "MP", // Madhya Pradesh
            "MZ", // Mizoram
            "NL", // Nagaland
            "OD", // Odisha (current post-2011)
            "OR", // Odisha legacy (pre-2011; aligned with DL Slice 10d per Marcus fb_20260503_020005_f0a329)
            "PB", // Punjab
            "PY", // Puducherry (UT)
            "RJ", // Rajasthan
            "SK", // Sikkim
            "TN", // Tamil Nadu
            "TR", // Tripura
            "TS", // Telangana
            "UK", // Uttarakhand (active MoRTH-issued code; UT prefix is non-canonical)
            "UP", // Uttar Pradesh
            "WB", // West Bengal
        )

    /**
     * Validates an Indian vehicle registration number (RC).
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers space-separated plate forms)
     * 3. Strip all hyphens (covers hyphen-separated plate forms common in printed documents)
     * 4. Uppercase all characters
     *
     * After normalisation, checks are applied in order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if length is not in 8..11
     * - [InvalidReason.INVALID_FORMAT] if string does not match `^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$`
     * - [InvalidReason.INVALID_PREFIX] if the first two characters are not in the closed set of
     *   38 MoRTH-assigned state/UT codes (see [VALID_STATE_CODES])
     *
     * Never throws. Returns [ValidationResult.Invalid] for all invalid inputs.
     *
     * @param value Raw vehicle registration string in any common input form.
     * @return [ValidationResult.Valid] if the registration is valid,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.vehicleRCValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)

        if (normalized.isBlank()) {
            return ValidationResult.Invalid(InvalidReason.EMPTY)
        }
        if (normalized.length !in 8..11) {
            return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        }
        if (!RC_REGEX.matches(normalized)) {
            return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        }
        if (normalized.substring(0, 2) !in VALID_STATE_CODES) {
            return ValidationResult.Invalid(InvalidReason.INVALID_PREFIX)
        }
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw vehicle registration string.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.vehicleRCIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a vehicle registration to canonical form: uppercase, no separators.
     *
     * Applies the same normalisation as [validate] (trim + strip whitespace + strip hyphens +
     * uppercase) before formatting. No single display separator standard exists for Indian
     * vehicle plates — the canonical output is uppercase with no separators.
     *
     * Examples:
     * - `"mh12ab1234"` → `"MH12AB1234"`
     * - `"MH 12 AB 1234"` → `"MH12AB1234"`
     * - `"MH-12-AB-1234"` → `"MH12AB1234"`
     * - `"MH12AB1234"` → `"MH12AB1234"` (idempotent)
     *
     * This method is idempotent: `format(format(x)) == format(x)` for any valid `x`.
     *
     * @param value Raw vehicle registration string.
     * @return Canonical uppercase vehicle registration string (8–11 characters, no separators).
     * @throws IllegalArgumentException if [value] is not a valid vehicle registration.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.vehicleRCFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "VehicleRC.format requires a valid vehicle registration; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    // ---------------------------------------------------------------------------
    // Progressive validation API (Slice 14 / Phase 2)
    // ---------------------------------------------------------------------------

    /**
     * Maximum accepted length for a sanitized VehicleRC input.
     *
     * Vehicle RC normalized length is 8-11 characters. Per PRD Scope table, maxLength = 10.
     * Sanitized inputs of length 8-10 that pass structural validation are valid;
     * length > 10 is over-length for progressive purposes.
     */
    public val maxLength: Int = 10

    /**
     * Set of characters accepted by [sanitize] and [validateProgressive] for VehicleRC.
     *
     * VehicleRC accepts ASCII uppercase letters (`'A'..'Z'`), lowercase letters (`'a'..'z'`),
     * and decimal digits (`'0'..'9'`). Hyphens and spaces (common in printed plate forms)
     * are NOT in allowedChars — callers should not pass hyphenated raw RC to validateProgressive.
     *
     * **Note:** lowercase letters ARE included in this set so callers can pass raw user input
     * directly to `if (char in VehicleRC.allowedChars)` UI filters without pre-uppercasing. The
     * validator normalises internally via [validate] and [validateProgressive].
     *
     * Exposed as `Set<Char>` for uniform caller-side `if (char in VehicleRC.allowedChars)` checks.
     * The constant is top-level to avoid per-call allocation.
     */
    public val allowedChars: Set<Char> = VEHICLE_RC_ALLOWED_CHARS

    /**
     * Strips all characters not in [allowedChars] from [rawInput] and truncates to [maxLength].
     *
     * This is a pure, idempotent function — `sanitize(sanitize(x)) == sanitize(x)` for all inputs.
     *
     * **Note:** hyphens are NOT in [allowedChars] and are stripped by sanitize. For example,
     * `sanitize("MH-12-AB-1234")` → `"MH12AB1234"`.
     *
     * @param rawInput Any string, including pasted values with hyphens, spaces, or symbols.
     * @return A string containing only alphanumeric chars, at most [maxLength] characters long.
     * @sample io.github.kotindia.samples.vehicleRCSanitizeSample
     */
    public fun sanitize(rawInput: String): String = rawInput.filter { it in VEHICLE_RC_ALLOWED_CHARS }.take(maxLength)

    /**
     * Validates a VehicleRC for incremental, as-you-type input.
     *
     * Callers should run [sanitize] first; internal spaces and hyphens will return
     * [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT].
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
     * @param value VehicleRC input to evaluate, possibly partial. Callers should run [sanitize]
     *   first; internal spaces and hyphens will return [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT].
     * @return A [ProgressiveResult] indicating the current state of the input.
     * @sample io.github.kotindia.samples.vehicleRCValidateProgressiveSample
     */
    public fun validateProgressive(value: String): ProgressiveResult {
        // Step 1: trim whitespace
        val normalized = value.trim()

        // Step 2: empty check
        if (normalized.isEmpty()) return ProgressiveResult.Empty

        // Step 3: bad-char check — fires BEFORE length check (AC4 priority)
        if (normalized.any { it !in VEHICLE_RC_ALLOWED_CHARS }) {
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
    // Differs from IFSC normalize: strips hyphens in addition to whitespace.
    // Hyphens appear in common printed plate forms ("MH-12-AB-1234").
    // ---------------------------------------------------------------------------

    private fun normalize(value: String): String = value.trim().replace(Regex("[\\s\\-]"), "").uppercase()
}
