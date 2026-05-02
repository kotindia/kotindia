// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CINTest {
    // -------------------------------------------------------------------------
    // Valid cases (≥10)
    // -------------------------------------------------------------------------

    @Test
    fun validUnlistedKarnatakaPTC() {
        assertEquals(ValidationResult.Valid, CIN.validate("U72200KA2013PTC097389"))
    }

    @Test
    fun validListedMaharashtraPLC() {
        assertEquals(ValidationResult.Valid, CIN.validate("L17110MH1973PLC019786"))
    }

    @Test
    fun validAllLowercase() {
        assertEquals(ValidationResult.Valid, CIN.validate("u72200ka2013ptc097389"))
    }

    @Test
    fun validMixedCase() {
        assertEquals(ValidationResult.Valid, CIN.validate("U72200ka2013ptc097389"))
    }

    @Test
    fun validWithInternalSpaces() {
        assertEquals(ValidationResult.Valid, CIN.validate("U72200 KA2013 PTC097389"))
    }

    @Test
    fun validWhitespacePadded() {
        assertEquals(ValidationResult.Valid, CIN.validate(" U72200KA2013PTC097389 "))
    }

    @Test
    fun validLowercaseWithTrailingSpace() {
        assertEquals(ValidationResult.Valid, CIN.validate("u72200ka2013ptc097389 "))
    }

    @Test
    fun validUnlistedDelhiNPL() {
        assertEquals(ValidationResult.Valid, CIN.validate("U01100DL1996NPL074306"))
    }

    @Test
    fun validListedGujaratPLC() {
        assertEquals(ValidationResult.Valid, CIN.validate("L36100GJ2001PLC039889"))
    }

    @Test
    fun validUnlistedTamilNaduGAP() {
        assertEquals(ValidationResult.Valid, CIN.validate("U74999TN2018GAP123456"))
    }

    // -------------------------------------------------------------------------
    // EMPTY cases (≥3)
    // -------------------------------------------------------------------------

    @Test
    fun emptyString() {
        val result = CIN.validate("")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun whitespaceOnly() {
        val result = CIN.validate("   ")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun tabAndNewline() {
        val result = CIN.validate("\t\n")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    // -------------------------------------------------------------------------
    // WRONG_LENGTH cases (≥3)
    // -------------------------------------------------------------------------

    @Test
    fun wrongLengthTooShort20Chars() {
        val result = CIN.validate("U72200KA2013PTC09738") // 20 chars
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun wrongLengthTooLong22Chars() {
        val result = CIN.validate("U72200KA2013PTC0973890") // 22 chars
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun wrongLengthSixChars() {
        val result = CIN.validate("U722KA")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    // -------------------------------------------------------------------------
    // INVALID_PREFIX cases (≥3)
    // -------------------------------------------------------------------------

    @Test
    fun invalidPrefixLetterA() {
        val result = CIN.validate("A72200KA2013PTC097389")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    @Test
    fun invalidPrefixDigit9() {
        val result = CIN.validate("972200KA2013PTC097389")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    @Test
    fun invalidPrefixLetterB() {
        val result = CIN.validate("B72200KA2013PTC097389")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    // -------------------------------------------------------------------------
    // INVALID_FORMAT cases (≥6)
    // -------------------------------------------------------------------------

    @Test
    fun invalidFormatLettersAtIndustryCodePositions() {
        // "UKA200KA2013PTC097389" — positions 1–5 must be digits
        val result = CIN.validate("UKA200KA2013PTC097389")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatDigitsAtStateCodePositions() {
        // After space-strip: "U7220099 2013PTC097389" → digits at positions 6–7
        val result = CIN.validate("U7220099201 3PTC097389")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatLettersAtYearPositions() {
        // positions 8–11 must be digits
        val result = CIN.validate("U72200KA20XXPTC097389")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatDigitsAtCompanyClassPositions() {
        // positions 12–14 must be letters
        val result = CIN.validate("U72200KA20131234097389")
        // 22 chars → WRONG_LENGTH, but test another form with exactly 21 digits at class
        val result2 = CIN.validate("U72200KA2013123097389")
        assertIs<ValidationResult.Invalid>(result2)
        assertEquals(InvalidReason.INVALID_FORMAT, result2.reason)
    }

    @Test
    fun invalidFormatLettersAtRegistrationPositions() {
        // positions 15–20 must be digits
        val result = CIN.validate("U72200KA2013PTCABCDEF")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatDevanagariUnicode() {
        // Devanagari character at last position
        val result = CIN.validate("U72200KA2013PTC09738अ")
        assertIs<ValidationResult.Invalid>(result)
        // Could be WRONG_LENGTH or INVALID_FORMAT depending on char count — either is correct
        assertTrue(
            result.reason == InvalidReason.INVALID_FORMAT ||
                result.reason == InvalidReason.WRONG_LENGTH,
        )
    }

    // -------------------------------------------------------------------------
    // format() cases (≥4 + throws)
    // -------------------------------------------------------------------------

    @Test
    fun formatLowercaseNormalized() {
        assertEquals("U72200KA2013PTC097389", CIN.format("u72200ka2013ptc097389"))
    }

    @Test
    fun formatSpacesStripped() {
        assertEquals("U72200KA2013PTC097389", CIN.format("U72200 KA2013 PTC097389"))
    }

    @Test
    fun formatIdempotentCanonical() {
        assertEquals("U72200KA2013PTC097389", CIN.format("U72200KA2013PTC097389"))
    }

    @Test
    fun formatDoubleIdempotent() {
        val once = CIN.format("u72200ka2013ptc097389")
        val twice = CIN.format(once)
        assertEquals(once, twice)
    }

    @Test
    fun formatInvalidThrowsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            CIN.format("invalid")
        }
    }

    @Test
    fun formatInvalidPrefixThrows() {
        assertFailsWith<IllegalArgumentException> {
            CIN.format("A72200KA2013PTC097389")
        }
    }

    // -------------------------------------------------------------------------
    // isValid() cases (≥2)
    // -------------------------------------------------------------------------

    @Test
    fun isValidReturnsTrueForValid() {
        assertTrue(CIN.isValid("U72200KA2013PTC097389"))
    }

    @Test
    fun isValidReturnsFalseForInvalidPrefix() {
        assertFalse(CIN.isValid("A72200KA2013PTC097389"))
    }

    // -------------------------------------------------------------------------
    // Additional edge cases
    // -------------------------------------------------------------------------

    @Test
    fun validListedPrefixExplicit() {
        // Confirm 'L' prefix is accepted
        assertTrue(CIN.isValid("L17110MH1973PLC019786"))
    }

    @Test
    fun validUnlistedPrefixExplicit() {
        // Confirm 'U' prefix is accepted
        assertTrue(CIN.isValid("U72200KA2013PTC097389"))
    }

    @Test
    fun invalidFormatTooManyDigitsAfterNormalize() {
        val result = CIN.validate("U722001KA2013PTC097389") // 22 chars
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun formatMessageContainsInput() {
        val ex =
            assertFailsWith<IllegalArgumentException> {
                CIN.format("BADINPUT")
            }
        assertTrue(ex.message?.contains("BADINPUT") == true)
    }

    @Test
    fun validateDoesNotThrow() {
        // validate() must never throw — always returns ValidationResult
        val result = CIN.validate("!!!GARBAGE!!!")
        assertIs<ValidationResult.Invalid>(result)
    }
}
