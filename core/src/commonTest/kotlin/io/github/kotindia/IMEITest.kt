// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [IMEI] — validate, isValid, format, mask.
 *
 * Valid-case tests use [IMEIReferenceVectors.KNOWN_VALID_IMEIS] entries sourced from:
 * - Wikipedia Luhn algorithm (https://en.wikipedia.org/wiki/Luhn_algorithm)
 * - npm `luhn` package (MIT, https://www.npmjs.com/package/luhn) — external verifier
 */
class IMEITest {
    // ---------------------------------------------------------------------------
    // validate — Valid cases (≥10) — from IMEIReferenceVectors
    // ---------------------------------------------------------------------------

    @Test
    fun validate_knownValid_groupA_first_returnsValid() {
        assertEquals(ValidationResult.Valid, IMEI.validate(IMEIReferenceVectors.KNOWN_VALID_IMEIS[0]))
    }

    @Test
    fun validate_knownValid_groupB_first_returnsValid() {
        assertEquals(ValidationResult.Valid, IMEI.validate(IMEIReferenceVectors.KNOWN_VALID_IMEIS[10]))
    }

    @Test
    fun validate_knownValid_groupC_first_returnsValid() {
        assertEquals(ValidationResult.Valid, IMEI.validate(IMEIReferenceVectors.KNOWN_VALID_IMEIS[20]))
    }

    @Test
    fun validate_knownValid_groupD_first_returnsValid() {
        assertEquals(ValidationResult.Valid, IMEI.validate(IMEIReferenceVectors.KNOWN_VALID_IMEIS[30]))
    }

    @Test
    fun validate_knownValid_groupE_first_returnsValid() {
        assertEquals(ValidationResult.Valid, IMEI.validate(IMEIReferenceVectors.KNOWN_VALID_IMEIS[40]))
    }

    @Test
    fun validate_knownValid_groupF_first_returnsValid() {
        assertEquals(ValidationResult.Valid, IMEI.validate(IMEIReferenceVectors.KNOWN_VALID_IMEIS[50]))
    }

    @Test
    fun validate_knownValid_groupG_first_returnsValid() {
        assertEquals(ValidationResult.Valid, IMEI.validate(IMEIReferenceVectors.KNOWN_VALID_IMEIS[60]))
    }

    @Test
    fun validate_knownValid_groupH_first_returnsValid() {
        assertEquals(ValidationResult.Valid, IMEI.validate(IMEIReferenceVectors.KNOWN_VALID_IMEIS[70]))
    }

    @Test
    fun validate_knownValid_groupI_first_returnsValid() {
        assertEquals(ValidationResult.Valid, IMEI.validate(IMEIReferenceVectors.KNOWN_VALID_IMEIS[80]))
    }

    @Test
    fun validate_knownValid_groupJ_first_returnsValid() {
        assertEquals(ValidationResult.Valid, IMEI.validate(IMEIReferenceVectors.KNOWN_VALID_IMEIS[90]))
    }

    @Test
    fun validate_withInternalSpaces_phoneDisplayForm_returnsValid() {
        // Phone display form — internal whitespace stripped before validation
        // "201000000000007" with spaces inserted
        assertEquals(ValidationResult.Valid, IMEI.validate("201 000 000 000 007"))
    }

    @Test
    fun validate_withLeadingTrailingWhitespace_returnsValid() {
        assertEquals(ValidationResult.Valid, IMEI.validate("  201000000000007  "))
    }

    // ---------------------------------------------------------------------------
    // validate — EMPTY (≥3)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_emptyString_returnsEmpty() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), IMEI.validate(""))
    }

    @Test
    fun validate_blankSpaces_returnsEmpty() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), IMEI.validate("   "))
    }

    @Test
    fun validate_tabNewline_returnsEmpty() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), IMEI.validate("\t\n"))
    }

    // ---------------------------------------------------------------------------
    // validate — WRONG_LENGTH (≥3)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_14Digits_returnsWrongLength() {
        // One digit short
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), IMEI.validate("20100000000000"))
    }

    @Test
    fun validate_16Digits_returnsWrongLength() {
        // One digit too many
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), IMEI.validate("2010000000000070"))
    }

    @Test
    fun validate_5Digits_returnsWrongLength() {
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), IMEI.validate("20100"))
    }

    // ---------------------------------------------------------------------------
    // validate — INVALID_FORMAT (≥3)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_nonDigitCharAtEnd_returnsInvalidFormat() {
        // 14 digits + 'A' — letter at last position
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), IMEI.validate("20100000000000A"))
    }

    @Test
    fun validate_devanagariDigit_returnsInvalidFormat() {
        // Devanagari digit '४' (U+0934) is not an ASCII digit
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), IMEI.validate("४01000000000007"))
    }

    @Test
    fun validate_hyphenatedInput_returnsWrongLength() {
        // Hyphens are NOT stripped — only whitespace is per PRD AC3.
        // "490-154-203-237518" is 18 chars after whitespace strip → WRONG_LENGTH fires
        // before INVALID_FORMAT (validation order: WRONG_LENGTH then INVALID_FORMAT).
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), IMEI.validate("490-154-203-237518"))
    }

    @Test
    fun validate_15CharWithHyphen_returnsInvalidFormat() {
        // 15-char input containing a hyphen — exactly right length but non-digit → INVALID_FORMAT
        // "49015-203237518" = 15 chars (5 digits + hyphen + 9 digits)
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), IMEI.validate("49015-203237518"))
    }

    // ---------------------------------------------------------------------------
    // validate — INVALID_CHECKSUM (≥5)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_invalidChecksum_groupA_returnsInvalidChecksum() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            IMEI.validate(IMEIReferenceVectors.KNOWN_INVALID_IMEIS[0]),
        )
    }

    @Test
    fun validate_invalidChecksum_groupB_returnsInvalidChecksum() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            IMEI.validate(IMEIReferenceVectors.KNOWN_INVALID_IMEIS[1]),
        )
    }

    @Test
    fun validate_invalidChecksum_groupC_returnsInvalidChecksum() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            IMEI.validate(IMEIReferenceVectors.KNOWN_INVALID_IMEIS[2]),
        )
    }

    @Test
    fun validate_invalidChecksum_groupD_returnsInvalidChecksum() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            IMEI.validate(IMEIReferenceVectors.KNOWN_INVALID_IMEIS[3]),
        )
    }

    @Test
    fun validate_invalidChecksum_groupE_returnsInvalidChecksum() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM),
            IMEI.validate(IMEIReferenceVectors.KNOWN_INVALID_IMEIS[4]),
        )
    }

    // ---------------------------------------------------------------------------
    // format() (≥5)
    // ---------------------------------------------------------------------------

    @Test
    fun format_cleanInput_returnsIdentical() {
        // format on already-clean 15-digit IMEI → same string (no separator)
        val imei = IMEIReferenceVectors.KNOWN_VALID_IMEIS[0]
        assertEquals(imei, IMEI.format(imei))
    }

    @Test
    fun format_spaceSeparatedPhoneForm_returnsClean15Digits() {
        // Phone display form → 15-digit clean output (no separator per AC4)
        assertEquals("201000000000007", IMEI.format("201 000 000 000 007"))
    }

    @Test
    fun format_leadingTrailingWhitespace_returnsClean() {
        assertEquals("201000000000007", IMEI.format("  201000000000007  "))
    }

    @Test
    fun format_idempotent_doubleFormatEqualsFormat() {
        val imei = IMEIReferenceVectors.KNOWN_VALID_IMEIS[20]
        assertEquals(IMEI.format(imei), IMEI.format(IMEI.format(imei)))
    }

    @Test
    fun format_invalidChecksum_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            IMEI.format(IMEIReferenceVectors.KNOWN_INVALID_IMEIS[0])
        }
    }

    @Test
    fun format_invalidFormat_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            IMEI.format("20100000000000A")
        }
    }

    @Test
    fun format_empty_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            IMEI.format("")
        }
    }

    // ---------------------------------------------------------------------------
    // mask() (≥6)
    // ---------------------------------------------------------------------------

    @Test
    fun mask_default_last4Visible_raw15Digit() {
        // Default (visibleStart=0, visibleEnd=4, maskChar='X') on raw 15-digit
        // "201000000000007" → "XXXXXXXXXXX0007" (11 X's + last 4)
        assertEquals("XXXXXXXXXXX0007", IMEI.mask("201000000000007"))
    }

    @Test
    fun mask_fullMask_visibleEnd0_allMasked() {
        // visibleEnd=0 → all 15 digits masked
        assertEquals("XXXXXXXXXXXXXXX", IMEI.mask("201000000000007", visibleStart = 0, visibleEnd = 0))
    }

    @Test
    fun mask_customMaskChar_asterisk() {
        assertEquals("***********0007", IMEI.mask("201000000000007", maskChar = '*'))
    }

    @Test
    fun mask_emptyInput_returnsEmpty() {
        // Never throws — display-safe by contract
        assertEquals("", IMEI.mask(""))
    }

    @Test
    fun mask_overlapRule_visibleStartPlusEndGteLength_returnsUnmasked() {
        // visibleStart=8 + visibleEnd=8 = 16 >= 15 → entire string unmasked (no throw)
        val imei = "201000000000007"
        assertEquals(imei, IMEI.mask(imei, visibleStart = 8, visibleEnd = 8))
    }

    @Test
    fun mask_rawSpaceSeparatedInput_masksCharByChar() {
        // "201 000 000 000 007" is 19 chars — mask operates on raw 19-char string char-by-char.
        // default (0, 4): last 4 chars of raw 19-char string are " 007" (space included).
        // Result: 15 X's + " 007"
        val raw = "201 000 000 000 007"
        val result = IMEI.mask(raw)
        assertEquals(raw.length, result.length)
        // Last 4 chars of raw string (including space)
        assertEquals(raw.takeLast(4), result.takeLast(4))
        // First 15 chars all masked
        assertTrue(result.take(raw.length - 4).all { it == 'X' })
    }

    // ---------------------------------------------------------------------------
    // isValid() (≥2)
    // ---------------------------------------------------------------------------

    @Test
    fun isValid_knownValidImei_returnsTrue() {
        assertTrue(IMEI.isValid(IMEIReferenceVectors.KNOWN_VALID_IMEIS[0]))
    }

    @Test
    fun isValid_knownInvalidImei_returnsFalse() {
        assertFalse(IMEI.isValid(IMEIReferenceVectors.KNOWN_INVALID_IMEIS[0]))
    }

    @Test
    fun isValid_emptyString_returnsFalse() {
        assertFalse(IMEI.isValid(""))
    }
}
