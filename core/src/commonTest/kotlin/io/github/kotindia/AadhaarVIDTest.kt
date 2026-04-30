// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [AadhaarVID] validator, formatter, and masker.
 *
 * Valid test cases use [AadhaarReferenceVectors.KNOWN_VALID_VIDS] — externally-sourced
 * 16-digit VID vectors cross-validated against mastermunj/format-utils JS Verhoeff impl (MIT).
 * No real AadhaarVID numbers are used — computed from known 15-digit prefixes per §3.4 guardrail.
 */
class AadhaarVIDTest {
    // ---------------------------------------------------------------------------
    // validate() — Known-valid cases (≥5, using VID reference vectors)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_knownValid_vid1_returnsValid() {
        assertEquals(ValidationResult.Valid, AadhaarVID.validate(AadhaarReferenceVectors.KNOWN_VALID_VIDS[0]))
    }

    @Test
    fun validate_knownValid_vid2_returnsValid() {
        assertEquals(ValidationResult.Valid, AadhaarVID.validate(AadhaarReferenceVectors.KNOWN_VALID_VIDS[1]))
    }

    @Test
    fun validate_knownValid_vid3_returnsValid() {
        assertEquals(ValidationResult.Valid, AadhaarVID.validate(AadhaarReferenceVectors.KNOWN_VALID_VIDS[2]))
    }

    @Test
    fun validate_knownValid_vid4_returnsValid() {
        assertEquals(ValidationResult.Valid, AadhaarVID.validate(AadhaarReferenceVectors.KNOWN_VALID_VIDS[3]))
    }

    @Test
    fun validate_knownValid_vid5_returnsValid() {
        assertEquals(ValidationResult.Valid, AadhaarVID.validate(AadhaarReferenceVectors.KNOWN_VALID_VIDS[4]))
    }

    @Test
    fun validate_knownValid_vidSpacedInput_stripsSpacesAndReturnsValid() {
        val raw = AadhaarReferenceVectors.KNOWN_VALID_VIDS[0] // "2000000000000006" (16 digits)
        val spaced = "${raw.substring(0, 4)} ${raw.substring(4, 8)} ${raw.substring(8, 12)} ${raw.substring(12)}"
        assertEquals(ValidationResult.Valid, AadhaarVID.validate(spaced))
    }

    @Test
    fun validate_knownValid_vidWhitespacePadded_returnsValid() {
        val raw = AadhaarReferenceVectors.KNOWN_VALID_VIDS[0]
        assertEquals(ValidationResult.Valid, AadhaarVID.validate("  $raw  "))
    }

    // ---------------------------------------------------------------------------
    // validate() — EMPTY cases (≥2)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_emptyString_returnsEmpty() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), AadhaarVID.validate(""))
    }

    @Test
    fun validate_blankSpaces_returnsEmpty() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), AadhaarVID.validate("   "))
    }

    // ---------------------------------------------------------------------------
    // validate() — WRONG_LENGTH cases (≥2)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_fifteenDigits_returnsWrongLength() {
        // 15 digits (missing check digit) — too short
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), AadhaarVID.validate("200000000000000"))
    }

    @Test
    fun validate_seventeenDigits_returnsWrongLength() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.WRONG_LENGTH),
            AadhaarVID.validate("20000000000000067"),
        )
    }

    @Test
    fun validate_twelvedigits_wrongLengthForVID_returnsWrongLength() {
        // A valid Aadhaar (12 digits) is wrong length for VID (16 digits)
        assertEquals(
            ValidationResult.Invalid(InvalidReason.WRONG_LENGTH),
            AadhaarVID.validate(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[0]),
        )
    }

    // ---------------------------------------------------------------------------
    // validate() — INVALID_FORMAT cases (≥2)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_nonDigitAtStart_returnsInvalidFormat() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            AadhaarVID.validate("A000000000000006"),
        )
    }

    @Test
    fun validate_specialCharInMiddle_returnsInvalidFormat() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            AadhaarVID.validate("2000000!00000006"),
        )
    }

    // ---------------------------------------------------------------------------
    // validate() — INVALID_PREFIX cases (≥2)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_firstDigitZero_returnsInvalidPrefix() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_PREFIX),
            AadhaarVID.validate("0000000000000006"),
        )
    }

    @Test
    fun validate_firstDigitOne_returnsInvalidPrefix() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_PREFIX),
            AadhaarVID.validate("1000000000000006"),
        )
    }

    // ---------------------------------------------------------------------------
    // validate() — INVALID_CHECKSUM cases (≥2)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_invalidChecksum_lastDigitCorrupted_returnsInvalidChecksum() {
        // "2000000000000006" is valid; last digit +1 = "2000000000000007" → invalid checksum
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            AadhaarVID.validate("2000000000000007"),
        )
    }

    @Test
    fun validate_invalidChecksum_vid2Corrupted_returnsInvalidChecksum() {
        // "3000000000000001" is valid; last digit +1 → invalid
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            AadhaarVID.validate("3000000000000002"),
        )
    }

    @Test
    fun validate_invalidChecksum_middleDigitCorrupted_returnsInvalidChecksum() {
        val valid = AadhaarReferenceVectors.KNOWN_VALID_VIDS[0]
        val corrupted = valid.substring(0, 7) + ((valid[7].digitToInt() + 1) % 10) + valid.substring(8)
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM), AadhaarVID.validate(corrupted))
    }

    // ---------------------------------------------------------------------------
    // format() cases (≥4)
    // ---------------------------------------------------------------------------

    @Test
    fun format_rawSixteenDigit_returnsFourGroupSpacedForm() {
        // "2000000000000006" → "2000 0000 0000 0006"
        assertEquals("2000 0000 0000 0006", AadhaarVID.format("2000000000000006"))
    }

    @Test
    fun format_alreadySpacedInput_idempotent() {
        val formatted = AadhaarVID.format("2000000000000006")
        assertEquals(formatted, AadhaarVID.format(formatted))
    }

    @Test
    fun format_idempotency_formatOfFormatEqualsFormat() {
        val raw = AadhaarReferenceVectors.KNOWN_VALID_VIDS[2]
        val once = AadhaarVID.format(raw)
        val twice = AadhaarVID.format(once)
        assertEquals(once, twice)
    }

    @Test
    fun format_invalidInput_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { AadhaarVID.format("invalid") }
    }

    @Test
    fun format_invalidChecksum_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { AadhaarVID.format("2000000000000007") }
    }

    @Test
    fun format_invalidPrefix_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { AadhaarVID.format("1000000000000006") }
    }

    // ---------------------------------------------------------------------------
    // mask() cases (≥5)
    // ---------------------------------------------------------------------------

    @Test
    fun mask_defaultParams_rawSixteenDigit_returnsLastFourVisible() {
        // Default (visibleStart=0, visibleEnd=4) on 16-digit → 12 X's + last 4
        assertEquals("XXXXXXXXXXXX0006", AadhaarVID.mask("2000000000000006"))
    }

    @Test
    fun mask_fullMask_zeroVisibleEnd_returnsAllXs() {
        assertEquals("XXXXXXXXXXXXXXXX", AadhaarVID.mask("2000000000000006", visibleStart = 0, visibleEnd = 0))
    }

    @Test
    fun mask_customMaskChar_usesAsterisk() {
        assertEquals("************0006", AadhaarVID.mask("2000000000000006", maskChar = '*'))
    }

    @Test
    fun mask_emptyString_returnsEmpty() {
        assertEquals("", AadhaarVID.mask(""))
    }

    @Test
    fun mask_overlapRule_visibleSumExceedsLength_returnsUnmasked() {
        // visibleStart + visibleEnd = 17 >= 16 → unmasked
        val raw = "2000000000000006"
        assertEquals(raw, AadhaarVID.mask(raw, visibleStart = 9, visibleEnd = 8))
    }

    @Test
    fun mask_neverThrowsOnArbitraryInput() {
        AadhaarVID.mask("not-a-vid")
        AadhaarVID.mask("")
        AadhaarVID.mask("!@#\$%^&*()")
    }

    // ---------------------------------------------------------------------------
    // isValid() cases (≥2)
    // ---------------------------------------------------------------------------

    @Test
    fun isValid_validVid_returnsTrue() {
        assertTrue(AadhaarVID.isValid(AadhaarReferenceVectors.KNOWN_VALID_VIDS[0]))
    }

    @Test
    fun isValid_invalidVid_returnsFalse() {
        assertFalse(AadhaarVID.isValid("2000000000000007"))
    }

    @Test
    fun isValid_emptyString_returnsFalse() {
        assertFalse(AadhaarVID.isValid(""))
    }
}
