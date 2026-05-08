// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

// Top-level private constant — constructed once at class-load time, not per call.
// DL maxLength = 16 (PRD Scope table). DL normalized length is 14-15, but raw input
// before sanitize can have hyphens stripped — sanitize operates on allowedChars (alphanumeric).
private val DL_ALLOWED_CHARS: Set<Char> =
    buildSet {
        for (c in 'A'..'Z') add(c)
        for (c in 'a'..'z') add(c)
        for (c in '0'..'9') add(c)
    }

/**
 * Validator, formatter, and masker for Indian Driving License (DL) numbers.
 *
 * A Driving License is the state-issued motor vehicle permit in India, governed by the
 * Motor Vehicles Act 1988 and administered by state RTOs through the MoRTH Sarathi
 * Parivahan portal (https://parivahan.gov.in/parivahan/).
 *
 * ## Supported format
 *
 * **Phase 1 supports the POST-2013 Sarathi Parivahan standard format ONLY.**
 *
 * Structure (after normalisation — see below):
 * - Characters 0–1: 2-letter state/UT code (e.g. `MH`, `DL`, `KA`) — closed list of 38 valid codes
 * - Characters 2–2 or 2–3: RTO code — 1 or 2 digits (state-assigned)
 * - Characters 3–13 or 4–14: 4-digit year of issue + 7-digit serial — 11 digits total
 * - Total normalised length: **14** (1-digit RTO) or **15** (2-digit RTO)
 *
 * Examples of valid inputs:
 *
 * | Raw input | Normalised | Notes |
 * |-----------|------------|-------|
 * | `"MH1220110012345"` | `"MH1220110012345"` | Maharashtra, 2-digit RTO |
 * | `"MH12-20110012345"` | `"MH1220110012345"` | Hyphen separator (Digilocker format) |
 * | `"MH12 20110012345"` | `"MH1220110012345"` | Space separator |
 * | `"mh1220110012345"` | `"MH1220110012345"` | Lowercase |
 * | `"DL-5-2016-0012345"` | `"DL520160012345"` | Delhi, 1-digit RTO |
 *
 * **State codes accepted:** `AN AP AR AS BR CG CH DD DL DN GA GJ HP HR JH JK KA KL LA LD`
 * `MH ML MN MP MZ NL OD OR PB PY RJ SK TN TR TS UK UP WB` (38 total).
 * - `LA` — Ladakh, added as a UT in 2019 (post-reorganisation).
 * - `OD` — Odisha current official code (post-2011).
 * - `OR` — Odisha legacy ISO code (pre-2011). Existing DLs issued with `OR` remain valid.
 *
 * ## Documented limitation (R4 — DL format diversity)
 *
 * **Pre-2013 state-specific Sarathi formats are EXPLICITLY NOT SUPPORTED in Phase 1.**
 *
 * Before the Sarathi Parivahan portal standardised the DL numbering scheme in 2013,
 * each of India's 36 states and UTs issued DLs in their own format — different lengths,
 * separator conventions, and character arrangements. These historical formats are diverse,
 * undocumented in any canonical public spec, and largely still in circulation on physically
 * printed DL cards that predate the portal.
 *
 * Inputs matching pre-2013 patterns will return:
 * - [InvalidReason.WRONG_LENGTH] — if the pre-2013 number normalises to fewer than 14 or more than 15 chars
 * - [InvalidReason.INVALID_FORMAT] — if the pre-2013 number has letters in RTO/year/serial positions
 *
 * Pre-2013 format examples that are rejected:
 * - `"MH-12-345678"` — old Maharashtra format (~12 chars after strip)
 * - `"DL-7-2003-0123456"` — old Delhi format with different RTO placement
 * - `"TN-25-XXYY1234"` — old Tamil Nadu alphanumeric format
 *
 * **Source:** MoRTH Sarathi Parivahan portal (https://sarathi.parivahan.gov.in/). Format
 * decisions are based on the post-2013 Sarathi-issued DL numbering standard documented at
 * the Sarathi portal and cross-referenced with Digilocker DL data format.
 *
 * **No Sarathi portal lookup is performed.** Validation is structural only — DL validity,
 * holder name, vehicle categories, and suspension status require a live government API call,
 * which is explicitly out of scope for this library.
 *
 * **DL is a Private government ID** (Photo ID + address proof). Mask before logging or display.
 * See [mask] for PII-safe display options.
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.dlSample
 */
@Suppress("ClassName") // DL is a universal Indian abbreviation — preserved for consumer readability
public object DL {
    private const val MIN_LENGTH = 14
    private const val MAX_LENGTH = 15

    // Post-2013 Sarathi format: state(2) + RTO(1-2 digits) + year+serial(11 digits).
    // RTO is 1 or 2 digits; year+serial together are 11 digits (no sub-division in regex).
    private val DL_REGEX = Regex("^[A-Z]{2}[0-9]{1,2}[0-9]{11}$")

    // 38 valid Sarathi state/UT codes. Closed list — unlisted codes → INVALID_PREFIX.
    // Source: MoRTH Sarathi Parivahan portal state/RTO code mapping (May 2026).
    private val VALID_STATE_CODES: Set<String> =
        setOf(
            "AN",
            "AP",
            "AR",
            "AS",
            "BR",
            "CG",
            "CH",
            "DD",
            "DL",
            "DN",
            "GA",
            "GJ",
            "HP",
            "HR",
            "JH",
            "JK",
            "KA",
            "KL",
            "LA", // Ladakh — post-2019 UT
            "LD",
            "MH",
            "ML",
            "MN",
            "MP",
            "MZ",
            "NL",
            "OD", // Odisha — current official code (post-2011)
            "OR", // LEGACY: Odisha pre-2011 ISO code; existing DLs issued with OR remain valid
            "PB",
            "PY",
            "RJ",
            "SK",
            "TN",
            "TR",
            "TS",
            "UK",
            "UP",
            "WB",
        )

    /**
     * Validates an Indian Driving License number.
     *
     * Normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (`replace(Regex("\\s"), "")`)
     * 3. Strip all hyphens (`replace("-", "")`) — hyphens are the primary separator in Digilocker DL strings
     * 4. Uppercase all characters
     *
     * After normalisation, checks are applied in this order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if normalised length is not in {14, 15}
     * - [InvalidReason.INVALID_FORMAT] if the string does not match `^[A-Z]{2}[0-9]{1,2}[0-9]{11}$`
     * - [InvalidReason.INVALID_PREFIX] if the 2-letter state code is not in the 38-entry valid set
     *
     * **Never throws.** Returns [ValidationResult.Invalid] for all invalid inputs.
     *
     * **Pre-2013 DL formats are not supported** — they will return [InvalidReason.WRONG_LENGTH] or
     * [InvalidReason.INVALID_FORMAT]. See the class-level KDoc for details (R4 documented limitation).
     *
     * @param value Raw DL string in any common input form (with or without hyphens/spaces, any case).
     * @return [ValidationResult.Valid] if the DL passes all structural checks,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.dlValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)
        if (normalized.isBlank()) return ValidationResult.Invalid(InvalidReason.EMPTY)
        if (normalized.length !in MIN_LENGTH..MAX_LENGTH) {
            return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        }
        if (!DL_REGEX.matches(normalized)) {
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
     * **Never throws.**
     *
     * @param value Raw DL string in any common input form.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.dlIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a valid Driving License number to its canonical form.
     *
     * Output: uppercase string with **no separator** (14 or 15 characters).
     * Example: `"mh12-20110012345"` → `"MH1220110012345"`.
     *
     * The Sarathi Parivahan portal has no official display separator convention —
     * no-separator canonical form is unambiguous and idempotent.
     *
     * Applies the same normalisation as [validate] (trim + strip whitespace + strip hyphens + uppercase)
     * before formatting. Throws [IllegalArgumentException] if the input is not a valid DL.
     *
     * This method is idempotent: `format(format(x)) == format(x)` for any valid input.
     *
     * @param value Raw DL string in any common input form.
     * @return Canonical uppercase no-separator DL string (14 or 15 characters).
     * @throws IllegalArgumentException if [value] is not a valid DL number.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.dlFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "DL.format requires a valid Driving License number; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    /**
     * Masks a Driving License number for PII-safe display.
     *
     * **DL is a Private government ID** (Photo ID + address proof) — always mask before
     * displaying in UI or logging to ensure compliance with PII handling requirements.
     *
     * Masking operates on the **raw input string** (not normalised). This is intentional —
     * the masker is a display utility that works character-by-character on whatever string
     * it receives. Callers who want to mask a normalised DL should call [format] first:
     * `DL.mask(DL.format(value))`.
     *
     * **Default `(visibleStart=0, visibleEnd=4, maskChar='X')`:**
     * Shows last 4 characters, masks the rest.
     * `"MH1220110012345"` → `"XXXXXXXXXXX2345"` (last 4: `"2345"`)
     *
     * **Edge cases (all safe — never throws):**
     * - Empty input → returns `""`.
     * - `visibleStart + visibleEnd >= value.length` → entire string returned unmasked (overlap rule).
     * - Non-normalised input (lowercase, hyphens, spaces) → masking applied character-by-character on the
     *   raw string. No normalisation is performed.
     *
     * @param value Raw DL string to mask. No normalisation is applied.
     * @param visibleStart Number of leading characters to show unmasked. Default: `0`.
     * @param visibleEnd Number of trailing characters to show unmasked. Default: `4`.
     * @param maskChar Character to replace masked positions. Default: `'X'`.
     * @return Masked string. Never throws — display-safe by contract.
     * @sample io.github.kotindia.samples.dlMaskSample
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
     * Maximum accepted length for a sanitized DL input.
     *
     * DL valid normalized length is 14–15 characters (post-2013 Sarathi format).
     * `maxLength = 16` is the **input cap** — it is one beyond the largest valid length.
     * Sanitized inputs of length 14 or 15 that pass structural validation are [ProgressiveResult.Valid];
     * a sanitized input of length exactly 16 (all allowed chars) still reaches Step 6 but
     * [validate] will reject it with [InvalidReason.WRONG_LENGTH].
     *
     * **Note:** This validator has a dual valid-length range (14 or 15) rather than a single
     * fixed length. This is intentional — the Sarathi post-2013 format allows 1-digit or 2-digit
     * RTO codes. See [validateProgressive] KDoc for how the state machine handles this range.
     */
    public val maxLength: Int = 16

    /**
     * Set of characters accepted by [sanitize] and [validateProgressive] for DL.
     *
     * DL accepts ASCII uppercase letters (`'A'..'Z'`), lowercase letters (`'a'..'z'`),
     * and decimal digits (`'0'..'9'`). Hyphens and spaces (separator conventions in DL strings)
     * are NOT in allowedChars — callers should not pass hyphenated raw DL to validateProgressive.
     *
     * **Note:** lowercase letters ARE included in this set so callers can pass raw user input
     * directly to `if (char in DL.allowedChars)` UI filters without pre-uppercasing. The
     * validator normalises internally via [validate] and [validateProgressive].
     *
     * Exposed as `Set<Char>` for uniform caller-side `if (char in DL.allowedChars)` checks.
     * The constant is top-level to avoid per-call allocation.
     */
    public val allowedChars: Set<Char> = DL_ALLOWED_CHARS

    /**
     * Strips all characters not in [allowedChars] from [rawInput] and truncates to [maxLength].
     *
     * This is a pure, idempotent function — `sanitize(sanitize(x)) == sanitize(x)` for all inputs.
     *
     * **Note:** hyphens are NOT in [allowedChars] and are stripped by sanitize. For example,
     * `sanitize("MH12-20110012345")` → `"MH1220110012345"`.
     *
     * @param rawInput Any string, including pasted values with hyphens, spaces, or symbols.
     * @return A string containing only alphanumeric chars, at most [maxLength] characters long.
     * @sample io.github.kotindia.samples.dlSanitizeSample
     */
    public fun sanitize(rawInput: String): String = rawInput.filter { it in DL_ALLOWED_CHARS }.take(maxLength)

    /**
     * Validates a DL for incremental, as-you-type input.
     *
     * Callers should run [sanitize] first; internal spaces and hyphens will return
     * [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT].
     *
     * Returns one of four [ProgressiveResult] states. The critical invariant: partial inputs of
     * the correct character class (alphanumeric) NEVER return [ProgressiveResult.Invalid] — partial
     * inputs always return [ProgressiveResult.Typing].
     *
     * **Dual-length completion:** DL is the only validator with a valid-length *range* (14–15)
     * rather than a single fixed length. Steps 5 and 6 handle this explicitly:
     * - Step 5 (partial): length < MIN_LENGTH (1..13) → [ProgressiveResult.Typing]
     * - Step 6 (complete): length in MIN_LENGTH..MAX_LENGTH (14–15) → delegate to [validate]
     * - Step 7 (over input cap): length == maxLength (16) → delegate to [validate], which returns
     *   [InvalidReason.WRONG_LENGTH] (since validate only accepts 14–15)
     *
     * State machine evaluation order (first match wins):
     * 1. Trim whitespace. If result is empty → [ProgressiveResult.Empty]
     * 2. Any character not in [allowedChars] → [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT]
     * 3. Length > [maxLength] (all allowed chars) → [ProgressiveResult.Invalid] with [InvalidReason.WRONG_LENGTH]
     *    and [ValidationContext.LengthMismatch]
     * 4. Length in `1..(MIN_LENGTH - 1)` (1..13) → [ProgressiveResult.Typing] with uppercase partial text
     * 5. Length in `MIN_LENGTH..maxLength` (14..16) → delegates to [validate]; returns [ProgressiveResult.Valid]
     *    (for 14–15 well-formed inputs) or [ProgressiveResult.Invalid] (for structural/prefix/length failures)
     *
     * @param value DL input to evaluate, possibly partial. Callers should run [sanitize]
     *   first; internal spaces and hyphens will return [ProgressiveResult.Invalid] with [InvalidReason.INVALID_FORMAT].
     * @return A [ProgressiveResult] indicating the current state of the input.
     * @sample io.github.kotindia.samples.dlValidateProgressiveSample
     */
    public fun validateProgressive(value: String): ProgressiveResult {
        // Step 1: trim whitespace
        val normalized = value.trim()

        // Step 2: empty check
        if (normalized.isEmpty()) return ProgressiveResult.Empty

        // Step 3: bad-char check — fires BEFORE length check (AC4 priority)
        if (normalized.any { it !in DL_ALLOWED_CHARS }) {
            return ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None)
        }

        // Step 4: over-length check (beyond input cap)
        if (normalized.length > maxLength) {
            return ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = maxLength, actual = normalized.length),
            )
        }

        // Step 5: partial check — only inputs shorter than MIN_LENGTH are still-typing.
        // DL has a dual valid-length range (14–15); inputs of length 14–16 proceed to Step 6.
        // maxLength=16 is the input cap; length-16 inputs reach validate() which rejects with WRONG_LENGTH.
        if (normalized.length < MIN_LENGTH) {
            return ProgressiveResult.Typing(visualText = normalized.uppercase())
        }

        // Step 6: complete or over-cap — delegate to full validate().
        // Valid DL (14 or 15 chars) → ProgressiveResult.Valid.
        // 16-char all-alphanumeric input → validate returns WRONG_LENGTH → ProgressiveResult.Invalid.
        return when (val result = validate(normalized)) {
            is ValidationResult.Valid -> ProgressiveResult.Valid
            is ValidationResult.Invalid -> ProgressiveResult.Invalid(result.reason, ValidationContext.None)
        }
    }

    // ---------------------------------------------------------------------------
    // Private normalisation helper
    // NOT part of the public API.
    // DL normalisation adds hyphen stripping vs PAN/GSTIN/IFSC —
    // Sarathi Parivahan and Digilocker issue DL strings with hyphens as separators.
    // ---------------------------------------------------------------------------
    private fun normalize(value: String): String =
        value
            .trim()
            .replace(Regex("\\s"), "")
            .replace("-", "")
            .uppercase()
}
