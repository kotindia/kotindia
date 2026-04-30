// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [Aadhaar] validator, formatter, and masker.
 *
 * Valid test cases use [AadhaarReferenceVectors.KNOWN_VALID_AADHAARS] — externally-sourced
 * vectors cross-validated against mastermunj/format-utils JS Verhoeff impl (MIT).
 * No real Aadhaar numbers are used — computed from known prefixes per §3.4 guardrail.
 */
class AadhaarTest {
    // ---------------------------------------------------------------------------
    // validate() — Known-valid cases (≥10, using reference vectors)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_knownValid_groupA_first_returnsValid() {
        assertEquals(ValidationResult.Valid, Aadhaar.validate(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[0]))
    }

    @Test
    fun validate_knownValid_groupA_second_returnsValid() {
        assertEquals(ValidationResult.Valid, Aadhaar.validate(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[1]))
    }

    @Test
    fun validate_knownValid_groupB_first_returnsValid() {
        assertEquals(ValidationResult.Valid, Aadhaar.validate(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[10]))
    }

    @Test
    fun validate_knownValid_groupC_first_returnsValid() {
        assertEquals(ValidationResult.Valid, Aadhaar.validate(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[20]))
    }

    @Test
    fun validate_knownValid_groupD_first_returnsValid() {
        assertEquals(ValidationResult.Valid, Aadhaar.validate(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[30]))
    }

    @Test
    fun validate_knownValid_groupE_first_returnsValid() {
        assertEquals(ValidationResult.Valid, Aadhaar.validate(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[40]))
    }

    @Test
    fun validate_knownValid_groupF_first_returnsValid() {
        assertEquals(ValidationResult.Valid, Aadhaar.validate(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[50]))
    }

    @Test
    fun validate_knownValid_groupG_first_returnsValid() {
        assertEquals(ValidationResult.Valid, Aadhaar.validate(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[60]))
    }

    @Test
    fun validate_knownValid_groupH_first_returnsValid() {
        assertEquals(ValidationResult.Valid, Aadhaar.validate(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[70]))
    }

    @Test
    fun validate_knownValid_lastVector_returnsValid() {
        assertEquals(ValidationResult.Valid, Aadhaar.validate(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS.last()))
    }

    @Test
    fun validate_knownValid_uidaiSpacedInput_stripsSpacesAndReturnsValid() {
        // Raw "201000000000" → UIDAI spaced form with spaces stripped → Valid
        val raw = AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[0]
        val spaced = "${raw.substring(0, 4)} ${raw.substring(4, 8)} ${raw.substring(8)}"
        assertEquals(ValidationResult.Valid, Aadhaar.validate(spaced))
    }

    @Test
    fun validate_knownValid_whitespacepadded_returnsValid() {
        val raw = AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[0]
        assertEquals(ValidationResult.Valid, Aadhaar.validate("  $raw  "))
    }

    // ---------------------------------------------------------------------------
    // validate() — EMPTY cases (≥3)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_emptyString_returnsEmpty() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), Aadhaar.validate(""))
    }

    @Test
    fun validate_blankSpaces_returnsEmpty() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), Aadhaar.validate("   "))
    }

    @Test
    fun validate_tabNewline_returnsEmpty() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), Aadhaar.validate("\t\n"))
    }

    // ---------------------------------------------------------------------------
    // validate() — WRONG_LENGTH cases (≥3)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_elevenDigits_returnsWrongLength() {
        // 11-digit valid prefix (no check digit) — too short
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), Aadhaar.validate("20100000000"))
    }

    @Test
    fun validate_thirteenDigits_returnsWrongLength() {
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), Aadhaar.validate("2010000000000"))
    }

    @Test
    fun validate_fiveDigits_returnsWrongLength() {
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), Aadhaar.validate("20100"))
    }

    // ---------------------------------------------------------------------------
    // validate() — INVALID_FORMAT cases (≥3)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_nonDigitAtStart_returnsInvalidFormat() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), Aadhaar.validate("A01000000000"))
    }

    @Test
    fun validate_specialCharInMiddle_returnsInvalidFormat() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), Aadhaar.validate("234!67890121"))
    }

    @Test
    fun validate_devanagariDigit_returnsInvalidFormat() {
        // Devanagari "२" (U+0968) — not an ASCII digit
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), Aadhaar.validate("२34567890121"))
    }

    // ---------------------------------------------------------------------------
    // validate() — INVALID_PREFIX cases (≥3)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_firstDigitZero_returnsInvalidPrefix() {
        // 12 digits, starts with 0 — UIDAI never issues Aadhaar starting with 0 or 1
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_PREFIX), Aadhaar.validate("034567890121"))
    }

    @Test
    fun validate_firstDigitOne_returnsInvalidPrefix() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_PREFIX), Aadhaar.validate("134567890121"))
    }

    @Test
    fun validate_firstDigitOne_differentSuffix_returnsInvalidPrefix() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_PREFIX), Aadhaar.validate("100000000000"))
    }

    // ---------------------------------------------------------------------------
    // validate() — INVALID_CHECKSUM cases (≥5, using KNOWN_INVALID_AADHAARS)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_invalidChecksum_case1_returnsInvalidChecksum() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            Aadhaar.validate(AadhaarReferenceVectors.KNOWN_INVALID_AADHAARS[0]),
        )
    }

    @Test
    fun validate_invalidChecksum_case2_returnsInvalidChecksum() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            Aadhaar.validate(AadhaarReferenceVectors.KNOWN_INVALID_AADHAARS[1]),
        )
    }

    @Test
    fun validate_invalidChecksum_case3_returnsInvalidChecksum() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            Aadhaar.validate(AadhaarReferenceVectors.KNOWN_INVALID_AADHAARS[2]),
        )
    }

    @Test
    fun validate_invalidChecksum_case4_returnsInvalidChecksum() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            Aadhaar.validate(AadhaarReferenceVectors.KNOWN_INVALID_AADHAARS[3]),
        )
    }

    @Test
    fun validate_invalidChecksum_case5_returnsInvalidChecksum() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            Aadhaar.validate(AadhaarReferenceVectors.KNOWN_INVALID_AADHAARS[4]),
        )
    }

    @Test
    fun validate_invalidChecksum_corruptedMiddleDigit_returnsInvalidChecksum() {
        // Take first known-valid, change a middle digit
        val valid = AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[0]
        val corrupted = valid.substring(0, 5) + ((valid[5].digitToInt() + 1) % 10) + valid.substring(6)
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM), Aadhaar.validate(corrupted))
    }

    // ---------------------------------------------------------------------------
    // format() cases (≥5)
    // ---------------------------------------------------------------------------

    @Test
    fun format_rawTwelveDigit_returnsUidaiSpacedForm() {
        assertEquals("2010 0000 0000", Aadhaar.format("201000000000"))
    }

    @Test
    fun format_alreadySpacedInput_idempotent() {
        val formatted = Aadhaar.format("201000000000")
        assertEquals(formatted, Aadhaar.format(formatted))
    }

    @Test
    fun format_idempotency_formatOfFormatEqualsFormat() {
        val raw = AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[5]
        val once = Aadhaar.format(raw)
        val twice = Aadhaar.format(once)
        assertEquals(once, twice)
    }

    @Test
    fun format_invalidInput_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { Aadhaar.format("invalid") }
    }

    @Test
    fun format_invalidChecksum_throwsIllegalArgumentException() {
        // Known-invalid checksum — format must throw
        assertFailsWith<IllegalArgumentException> {
            Aadhaar.format(AadhaarReferenceVectors.KNOWN_INVALID_AADHAARS[0])
        }
    }

    @Test
    fun format_invalidPrefix_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { Aadhaar.format("134567890121") }
    }

    @Test
    fun format_groupBVector_returnsCorrectSpacedForm() {
        val raw = AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[10] // "301000000108"
        assertEquals("3010 0000 0108", Aadhaar.format(raw))
    }

    // ---------------------------------------------------------------------------
    // mask() cases (≥6)
    // ---------------------------------------------------------------------------

    @Test
    fun mask_defaultParams_rawTwelveDigit_returnsLastFourVisible() {
        // Default (visibleStart=0, visibleEnd=4) on 12-digit raw → 8 X's + last 4
        assertEquals("XXXXXXXX0000", Aadhaar.mask("201000000000"))
    }

    @Test
    fun mask_fullMask_zeroVisibleEnd_returnsAllXs() {
        assertEquals("XXXXXXXXXXXX", Aadhaar.mask("201000000000", visibleStart = 0, visibleEnd = 0))
    }

    @Test
    fun mask_customMaskChar_usesCustomChar() {
        assertEquals("********0000", Aadhaar.mask("201000000000", maskChar = '*'))
    }

    @Test
    fun mask_emptyString_returnsEmpty() {
        assertEquals("", Aadhaar.mask(""))
    }

    @Test
    fun mask_overlapRule_visibleSumExceedsLength_returnsUnmasked() {
        // visibleStart + visibleEnd = 13 >= 12 → unmasked
        val raw = "201000000000"
        assertEquals(raw, Aadhaar.mask(raw, visibleStart = 7, visibleEnd = 6))
    }

    @Test
    fun mask_visibleStartFour_showsFirstFourAndLastFour() {
        assertEquals("2010XXXX0000", Aadhaar.mask("201000000000", visibleStart = 4, visibleEnd = 4))
    }

    @Test
    fun mask_rawSpacedInput_masksSpacesAsCharacters() {
        // mask() works char-by-char on raw — spaces are characters too
        val spaced = "2010 0000 0000" // 14 chars
        // Default (0, 4): last 4 visible = "0000", 10 X's
        assertEquals("XXXXXXXXXX0000", Aadhaar.mask(spaced))
    }

    @Test
    fun mask_neverThrowsOnArbitraryInput() {
        // mask() must never throw regardless of input content
        Aadhaar.mask("not-a-valid-aadhaar-at-all")
        Aadhaar.mask("!@#$%^&*()")
        Aadhaar.mask("   ")
    }

    // ---------------------------------------------------------------------------
    // isValid() cases (≥2)
    // ---------------------------------------------------------------------------

    @Test
    fun isValid_validAadhaar_returnsTrue() {
        assertTrue(Aadhaar.isValid(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[0]))
    }

    @Test
    fun isValid_invalidAadhaar_returnsFalse() {
        assertFalse(Aadhaar.isValid(AadhaarReferenceVectors.KNOWN_INVALID_AADHAARS[0]))
    }

    @Test
    fun isValid_emptyString_returnsFalse() {
        assertFalse(Aadhaar.isValid(""))
    }
}
