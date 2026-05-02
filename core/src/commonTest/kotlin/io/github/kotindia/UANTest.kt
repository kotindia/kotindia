// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UANTest {
    // -------------------------------------------------------------------------
    // Valid cases (≥8)
    // -------------------------------------------------------------------------

    @Test
    fun validate_rawDigits_returnsValid() {
        assertEquals(ValidationResult.Valid, UAN.validate("101234567890"))
    }

    @Test
    fun validate_spaceSeparated_returnsValid() {
        // EPFO portal display convention: "1012 3456 7890"
        assertEquals(ValidationResult.Valid, UAN.validate("1012 3456 7890"))
    }

    @Test
    fun validate_whitespacePadded_returnsValid() {
        assertEquals(ValidationResult.Valid, UAN.validate(" 101234567890 "))
    }

    @Test
    fun validate_asymmetricSpace_returnsValid() {
        assertEquals(ValidationResult.Valid, UAN.validate("1012 34567890"))
    }

    @Test
    fun validate_allZeros_returnsValid() {
        // No first-digit rule — all zeros must be accepted
        assertEquals(ValidationResult.Valid, UAN.validate("000000000000"))
    }

    @Test
    fun validate_allNines_returnsValid() {
        // No upper-bound rule — all nines must be accepted
        assertEquals(ValidationResult.Valid, UAN.validate("999999999999"))
    }

    @Test
    fun validate_boundaryStartsWith1_returnsValid() {
        assertEquals(ValidationResult.Valid, UAN.validate("100000000000"))
    }

    @Test
    fun validate_boundaryStartsWith2_returnsValid() {
        assertEquals(ValidationResult.Valid, UAN.validate("200000000001"))
    }

    // -------------------------------------------------------------------------
    // EMPTY cases (≥3)
    // -------------------------------------------------------------------------

    @Test
    fun validate_emptyString_returnsEmpty() {
        val result = UAN.validate("")
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), result)
    }

    @Test
    fun validate_whitespaceOnly_returnsEmpty() {
        val result = UAN.validate("   ")
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), result)
    }

    @Test
    fun validate_tabAndNewline_returnsEmpty() {
        val result = UAN.validate("\t\n")
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), result)
    }

    // -------------------------------------------------------------------------
    // WRONG_LENGTH cases (≥3)
    // -------------------------------------------------------------------------

    @Test
    fun validate_11digits_returnsWrongLength() {
        val result = UAN.validate("10123456789")
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), result)
    }

    @Test
    fun validate_13digits_returnsWrongLength() {
        val result = UAN.validate("1012345678901")
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), result)
    }

    @Test
    fun validate_6digits_returnsWrongLength() {
        val result = UAN.validate("123456")
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), result)
    }

    // -------------------------------------------------------------------------
    // INVALID_FORMAT cases (≥3 + hyphen note)
    // -------------------------------------------------------------------------

    @Test
    fun validate_letterInPosition_returnsInvalidFormat() {
        // 12 chars after whitespace strip, but contains 'X'
        val result = UAN.validate("10123456789X")
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), result)
    }

    @Test
    fun validate_specialChar_returnsInvalidFormat() {
        // 12 chars but contains '!'
        val result = UAN.validate("10123456789!")
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), result)
    }

    @Test
    fun validate_devanagariUnicode_returnsInvalidFormat() {
        // Devanagari 'अ' at position 0 — unicode non-digit (per Anika §5 pattern)
        // "अ01234567890" is 11 chars + Devanagari → after whitespace strip same → WRONG_LENGTH or INVALID_FORMAT
        // The Devanagari char counts as 1 char, so "अ01234567890" = 12 chars → INVALID_FORMAT
        val result = UAN.validate("अ01234567890")
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), result)
    }

    @Test
    fun validate_allDevanagariDigits_returnsInvalidFormat() {
        // Devanagari digits ('०'..'९', U+0966..U+096F) pass Kotlin's isDigit() (Unicode Nd category).
        // Library MUST reject them — only ASCII '0'..'9' are valid UAN digits.
        // Without the ASCII range guard, this 12-char all-Devanagari input would silently validate as Valid.
        val result = UAN.validate("१२३४५६७८९०१२")
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), result)
    }

    @Test
    fun validate_mixedAlphaDigit_returnsInvalidFormat() {
        // 12 chars, mixed alpha at end
        val result = UAN.validate("1234567ABCDE")
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), result)
    }

    @Test
    fun validate_hyphenated14chars_returnsWrongLength() {
        // "1012-3456-7890" = 14 chars after whitespace strip → WRONG_LENGTH
        // Note: hyphens are NOT stripped — not a documented UAN input convention
        val result = UAN.validate("1012-3456-7890")
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), result)
    }

    @Test
    fun validate_hyphen12charsWithNonDigit_returnsInvalidFormat() {
        // "10123-67890X" = 12 chars after whitespace strip, contains hyphen and X → INVALID_FORMAT
        val result = UAN.validate("10123-67890X")
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), result)
    }

    // -------------------------------------------------------------------------
    // format() cases (≥4)
    // -------------------------------------------------------------------------

    @Test
    fun format_canonicalInput_returnsCanonical() {
        assertEquals("101234567890", UAN.format("101234567890"))
    }

    @Test
    fun format_spaceSeparated_stripsWhitespace() {
        assertEquals("101234567890", UAN.format("1012 3456 7890"))
    }

    @Test
    fun format_paddedInput_stripsWhitespace() {
        assertEquals("101234567890", UAN.format(" 101234567890 "))
    }

    @Test
    fun format_idempotent_sameResult() {
        val first = UAN.format("1012 3456 7890")
        val second = UAN.format(first)
        assertEquals(first, second)
    }

    @Test
    fun format_idempotencyProperty_holdsForSpacedInput() {
        // format(format("1012 3456 7890")) == format("1012 3456 7890")
        assertEquals(
            UAN.format("1012 3456 7890"),
            UAN.format(UAN.format("1012 3456 7890")),
        )
    }

    @Test
    fun format_invalidString_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            UAN.format("invalid")
        }
    }

    @Test
    fun format_wrongLength_throwsIllegalArgumentException() {
        // 11 digits → WRONG_LENGTH → throws
        assertFailsWith<IllegalArgumentException> {
            UAN.format("10123456789")
        }
    }

    @Test
    fun format_invalidFormat_throwsIllegalArgumentException() {
        // 12 chars with letter → INVALID_FORMAT → throws
        assertFailsWith<IllegalArgumentException> {
            UAN.format("10123456789X")
        }
    }

    // -------------------------------------------------------------------------
    // mask() cases (≥6)
    // -------------------------------------------------------------------------

    @Test
    fun mask_default_lastFourVisible() {
        // Default: visibleStart=0, visibleEnd=4
        assertEquals("XXXXXXXX7890", UAN.mask("101234567890"))
    }

    @Test
    fun mask_customVisibleStartAndEnd() {
        assertEquals("101XXXXXX890", UAN.mask("101234567890", visibleStart = 3, visibleEnd = 3))
    }

    @Test
    fun mask_fullMask_allMasked() {
        assertEquals("XXXXXXXXXXXX", UAN.mask("101234567890", visibleStart = 0, visibleEnd = 0))
    }

    @Test
    fun mask_customMaskChar() {
        assertEquals("********7890", UAN.mask("101234567890", maskChar = '*'))
    }

    @Test
    fun mask_emptyString_returnsEmpty() {
        // Never throws — display-safe by contract
        assertEquals("", UAN.mask(""))
    }

    @Test
    fun mask_overlap_returnsFullString() {
        // visibleStart=7, visibleEnd=6 → 7+6=13 >= 12 → full string returned
        assertEquals("101234567890", UAN.mask("101234567890", visibleStart = 7, visibleEnd = 6))
    }

    @Test
    fun mask_rawNonNormalized_charByChar() {
        // "1012 3456 7890" = 14 chars raw; last 4 chars = "7890"
        assertEquals("XXXXXXXXXX7890", UAN.mask("1012 3456 7890"))
    }

    @Test
    fun mask_idempotent_withMaskChar() {
        // mask(mask(x, 0, 4, 'X'), 0, 4, 'X') == mask(x, 0, 4, 'X')
        // Since 'X' is already the maskChar, applying mask again to already-masked gives same result
        val once = UAN.mask("101234567890", 0, 4, 'X')
        val twice = UAN.mask(once, 0, 4, 'X')
        assertEquals(once, twice)
    }

    @Test
    fun mask_alphaInput_charByChar() {
        // Non-digit raw input — masker works char-by-char regardless
        assertEquals("XXXXXXXXijkl", UAN.mask("abcdefghijkl"))
    }

    // -------------------------------------------------------------------------
    // isValid() cases (≥2)
    // -------------------------------------------------------------------------

    @Test
    fun isValid_validUAN_returnsTrue() {
        assertTrue(UAN.isValid("101234567890"))
    }

    @Test
    fun isValid_wrongLength_returnsFalse() {
        assertFalse(UAN.isValid("10123456789"))
    }
}
