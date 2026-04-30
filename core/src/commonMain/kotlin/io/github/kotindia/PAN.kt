// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * Validator, formatter, and masker for Indian Permanent Account Numbers (PAN).
 *
 * PAN is a 10-character alphanumeric code issued by the Income Tax Department of India,
 * uniquely identifying taxpayers — individuals, companies, HUFs, trusts, and other entities.
 *
 * Format: `[A-Z]{5}[0-9]{4}[A-Z]`
 * - Characters 1–3: alphabetic (freeform, assigned by ITD)
 * - Character 4: entity category code — one of: `P C H A B G J L F T`
 * - Character 5: first letter of the holder's name (individuals) or entity name
 * - Characters 6–9: four-digit sequential number
 * - Character 10: check letter (alphabetic; ITD has not published the algorithm)
 *
 * **4th-character entity category codes (index 3, 0-based):**
 * - `P` — Person / Individual
 * - `C` — Company
 * - `H` — Hindu Undivided Family (HUF)
 * - `A` — Association of Persons (AOP)
 * - `B` — Body of Individuals (BOI)
 * - `G` — Government
 * - `J` — Artificial Juridical Person
 * - `L` — Local Authority
 * - `F` — Firm / Limited Liability Partnership
 * - `T` — Trust
 *
 * **PAN is financial PII** (Income Tax ID). Mask before logging or displaying in UI.
 * See [mask] for PII-safe display options.
 *
 * **Accepted input forms** (all normalised before validation):
 * - Raw uppercase: `"ABCPE1234F"`
 * - Lowercase: `"abcpe1234f"` — normalised to uppercase
 * - Mixed case: `"AbCpE1234f"` — normalised to uppercase
 * - With spaces: `"ABCPE 1234 F"` — internal whitespace stripped
 * - Whitespace padded: `" ABCPE1234F "` — leading/trailing whitespace trimmed
 *
 * **No checksum:** The Income Tax Department has not published a public checksum
 * algorithm for the 10th character. Validation is structural (format + category) only.
 *
 * This object is stateless and thread-safe.
 *
 * @sample io.github.kotindia.samples.panSample
 */
@Suppress("ClassName") // PAN is a universal acronym — preserved for consumer readability
public object PAN {
    private const val EXPECTED_LENGTH = 10
    private const val CATEGORY_INDEX = 3 // 0-based index of the entity category character
    private val PAN_REGEX = Regex("^[A-Z]{5}[0-9]{4}[A-Z]$")
    private val VALID_CATEGORY_CHARS = setOf('P', 'C', 'H', 'A', 'B', 'G', 'J', 'L', 'F', 'T')

    /**
     * Validates an Indian Permanent Account Number (PAN).
     *
     * Input normalisation applied before validation (in order):
     * 1. Trim leading/trailing whitespace
     * 2. Strip all internal whitespace (covers copy-paste with spaces)
     * 3. Uppercase all characters
     *
     * After normalisation, checks are applied in order — first failing check wins:
     * - [InvalidReason.EMPTY] if the result is blank
     * - [InvalidReason.WRONG_LENGTH] if length is not 10
     * - [InvalidReason.INVALID_FORMAT] if the string does not match `^[A-Z]{5}[0-9]{4}[A-Z]$`
     * - [InvalidReason.INVALID_CATEGORY] if the 4th character (index 3) is not one of:
     *   `P` (Person), `C` (Company), `H` (HUF), `A` (AOP), `B` (BOI),
     *   `G` (Government), `J` (Artificial Juridical Person), `L` (Local Authority),
     *   `F` (Firm), `T` (Trust)
     *
     * **Note:** Hyphens are not stripped. `"ABCPE-1234F"` normalises to 11 characters
     * and returns [InvalidReason.WRONG_LENGTH].
     *
     * @param value Raw PAN string in any common input form.
     * @return [ValidationResult.Valid] if the PAN passes format and category validation,
     *   [ValidationResult.Invalid] with an appropriate [InvalidReason] otherwise.
     * @sample io.github.kotindia.samples.panValidateSample
     */
    public fun validate(value: String): ValidationResult {
        val normalized = normalize(value)

        if (normalized.isBlank()) return ValidationResult.Invalid(InvalidReason.EMPTY)
        if (normalized.length != EXPECTED_LENGTH) return ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        if (!PAN_REGEX.matches(normalized)) return ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        if (normalized[CATEGORY_INDEX] !in VALID_CATEGORY_CHARS) {
            return ValidationResult.Invalid(InvalidReason.INVALID_CATEGORY)
        }
        return ValidationResult.Valid
    }

    /**
     * Returns `true` if [validate] returns [ValidationResult.Valid].
     *
     * @param value Raw PAN string.
     * @return `true` if valid, `false` otherwise.
     * @sample io.github.kotindia.samples.panIsValidSample
     */
    public fun isValid(value: String): Boolean = validate(value) is ValidationResult.Valid

    /**
     * Formats a PAN to canonical form: 10-character uppercase, no separators.
     *
     * Applies the same normalisation as [validate] before formatting.
     * The Income Tax Department publishes no display separator convention — output
     * is the raw uppercase string: e.g. `"ABCPE1234F"`.
     *
     * This method is idempotent: `format(format(x)) == format(x)`.
     *
     * @param value Raw PAN string.
     * @return Canonical 10-character uppercase PAN string.
     * @throws IllegalArgumentException if [value] is not a valid PAN.
     *   The exception message includes the (truncated) input for diagnostics.
     * @sample io.github.kotindia.samples.panFormatSample
     */
    public fun format(value: String): String {
        if (!isValid(value)) {
            throw IllegalArgumentException(
                "PAN.format requires a valid PAN; got: ${value.take(50)}",
            )
        }
        return normalize(value)
    }

    /**
     * Masks a PAN for PII-safe display.
     *
     * **PAN is financial PII** — always mask before displaying in UI or logging.
     *
     * Masking operates on the **raw input string** (not normalised). This is intentional —
     * the masker is a display utility that works character-by-character on whatever string
     * it receives. Callers who want to mask a normalised PAN should call [format] first:
     * `PAN.mask(PAN.format(value))`.
     *
     * **Default `(visibleStart=0, visibleEnd=4, maskChar='X')`:**
     * Shows last 4 characters, masks the rest.
     * `"ABCPE1234F"` → `"XXXXXX234F"` (last 4: `"234F"`)
     *
     * **Middle-digit-exposure pattern `(visibleStart=5, visibleEnd=1)`:**
     * Shows first 5 characters and last 1, masks the 4-digit section (positions 5–8).
     * `"ABCPE1234F"` → `"ABCPEXXXXF"` (masks `"1234"`)
     *
     * **Note:** The pattern `"XXXXX1234X"` (all letters masked, digits exposed) from the
     * KotIndia design notes is not achievable with the standard `(visibleStart, visibleEnd)`
     * signature. Use `(5, 1)` for the closest practical alternative. This is a known
     * limitation of the standard masking signature documented here for clarity.
     *
     * **Edge cases (all safe — never throws):**
     * - Empty input → returns `""`.
     * - `visibleStart + visibleEnd >= value.length` → entire string returned unmasked (overlap rule).
     * - Non-normalised input (lowercase, spaces) → masking applied character-by-character on the
     *   raw string. No normalisation is performed.
     *
     * @param value Raw PAN string to mask. No normalisation is applied.
     * @param visibleStart Number of leading characters to show unmasked. Default: `0`.
     * @param visibleEnd Number of trailing characters to show unmasked. Default: `4`.
     * @param maskChar Character to replace masked positions. Default: `'X'`.
     * @return Masked string. Never throws — display-safe by contract.
     * @sample io.github.kotindia.samples.panMaskSample
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
    // Private normalisation helper
    // NOT part of the public API.
    // This is the reference uppercase-normalisation pattern for PAN, GSTIN, and TAN
    // (established by IFSC Slice 5).
    // ---------------------------------------------------------------------------

    private fun normalize(value: String): String = value.trim().replace(Regex("\\s"), "").uppercase()
}
