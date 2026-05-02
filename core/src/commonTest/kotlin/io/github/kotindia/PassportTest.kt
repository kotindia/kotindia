// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [Passport].
 *
 * Test vectors are synthetic — constructed to match the MEA format `[A-Z]\d{7}`.
 * No real Indian passport numbers are used (§3.4 guardrail).
 *
 * Reference: Ministry of External Affairs (MEA) passport number format, R8 (Sandeep, 2026-04-29).
 */
class PassportTest {
    // -------------------------------------------------------------------------
    // Valid cases
    // -------------------------------------------------------------------------

    @Test
    fun validRawUppercase() {
        assertEquals(ValidationResult.Valid, Passport.validate("M1234567"))
    }

    @Test
    fun validAllLowercase() {
        assertEquals(ValidationResult.Valid, Passport.validate("m1234567"))
    }

    @Test
    fun validInternalSpace() {
        assertEquals(ValidationResult.Valid, Passport.validate("M 1234567"))
    }

    @Test
    fun validWhitespacePadded() {
        assertEquals(ValidationResult.Valid, Passport.validate(" M1234567 "))
    }

    @Test
    fun validLowercaseTrailingSpace() {
        assertEquals(ValidationResult.Valid, Passport.validate("m1234567 "))
    }

    @Test
    fun validBoundaryMinimumDigits() {
        assertEquals(ValidationResult.Valid, Passport.validate("A0000000"))
    }

    @Test
    fun validBoundaryMaximumDigits() {
        assertEquals(ValidationResult.Valid, Passport.validate("Z9999999"))
    }

    @Test
    fun validMidRangeLetter() {
        assertEquals(ValidationResult.Valid, Passport.validate("B5555555"))
    }

    // -------------------------------------------------------------------------
    // EMPTY cases
    // -------------------------------------------------------------------------

    @Test
    fun invalidEmptyString() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), Passport.validate(""))
    }

    @Test
    fun invalidWhitespaceOnly() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), Passport.validate("   "))
    }

    @Test
    fun invalidTabNewline() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), Passport.validate("\t\n"))
    }

    // -------------------------------------------------------------------------
    // WRONG_LENGTH cases
    // -------------------------------------------------------------------------

    @Test
    fun invalidTooShort7Chars() {
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), Passport.validate("M123456"))
    }

    @Test
    fun invalidTooLong9Chars() {
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), Passport.validate("M12345678"))
    }

    @Test
    fun invalidTooShort5Chars() {
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), Passport.validate("M1234"))
    }

    @Test
    fun invalidHyphenGivesWrongLength() {
        // "M-1234567" → normalize → "M-1234567" (9 chars) → WRONG_LENGTH (hyphens not stripped)
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), Passport.validate("M-1234567"))
    }

    // -------------------------------------------------------------------------
    // INVALID_FORMAT cases
    // -------------------------------------------------------------------------

    @Test
    fun invalidDigitAtPositionZero() {
        // First char must be [A-Z]; '1' is a digit
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), Passport.validate("11234567"))
    }

    @Test
    fun invalidLettersInDigitSection() {
        // Positions 1–7 must be digits; all letters → INVALID_FORMAT
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), Passport.validate("MABCDEFG"))
    }

    @Test
    fun invalidLetterAtLastPosition() {
        // Last char must be digit; 'A' is a letter
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), Passport.validate("M123456A"))
    }

    @Test
    fun invalidSpecialCharInDigitSection() {
        // Hyphen in digit section after normalization → 8 chars but format fails
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), Passport.validate("M123-567"))
    }

    @Test
    fun invalidUnicodeDevanagariAtPositionZero() {
        // Devanagari character at position 0 — Anika §5 Unicode pattern
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), Passport.validate("अ1234567"))
    }

    // -------------------------------------------------------------------------
    // format() cases
    // -------------------------------------------------------------------------

    @Test
    fun formatLowercaseNormalized() {
        assertEquals("M1234567", Passport.format("m1234567"))
    }

    @Test
    fun formatIdempotentAlreadyCanonical() {
        assertEquals("M1234567", Passport.format("M1234567"))
    }

    @Test
    fun formatIdempotencyProperty() {
        val once = Passport.format("m1234567")
        val twice = Passport.format(once)
        assertEquals(once, twice)
    }

    @Test
    fun formatWhitespaceStripped() {
        assertEquals("M1234567", Passport.format("M 1234567"))
    }

    @Test
    fun formatInvalidThrowsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            Passport.format("invalid")
        }
    }

    @Test
    fun formatInvalidFormatThrows() {
        assertFailsWith<IllegalArgumentException> {
            Passport.format("11234567")
        }
    }

    @Test
    fun formatWrongLengthThrows() {
        assertFailsWith<IllegalArgumentException> {
            Passport.format("M123456")
        }
    }

    // -------------------------------------------------------------------------
    // mask() cases
    // -------------------------------------------------------------------------

    @Test
    fun maskDefaultLastFourVisible() {
        assertEquals("XXXX4567", Passport.mask("M1234567"))
    }

    @Test
    fun maskFirstOneVisible() {
        assertEquals("MXXXXXXX", Passport.mask("M1234567", visibleStart = 1, visibleEnd = 0))
    }

    @Test
    fun maskFullMask() {
        assertEquals("XXXXXXXX", Passport.mask("M1234567", visibleStart = 0, visibleEnd = 0))
    }

    @Test
    fun maskCustomMaskChar() {
        assertEquals("****4567", Passport.mask("M1234567", maskChar = '*'))
    }

    @Test
    fun maskEmptyInputNoThrow() {
        assertEquals("", Passport.mask(""))
    }

    @Test
    fun maskOverlapReturnsUnmasked() {
        // visibleStart(5) + visibleEnd(4) = 9 >= 8 → return unmasked
        assertEquals("M1234567", Passport.mask("M1234567", visibleStart = 5, visibleEnd = 4))
    }

    @Test
    fun maskRawLowercaseCharByChar() {
        // Raw lowercase "m1234567" — no normalization; default maskChar='X'; last 4 visible = "4567", 4 masked with 'X'
        // maskChar applied as-is regardless of input case — visible chars preserve original case
        assertEquals("XXXX4567", Passport.mask("m1234567"))
    }

    @Test
    fun maskIdempotent() {
        val masked = Passport.mask("M1234567", 0, 4, 'X')
        val maskedAgain = Passport.mask(masked, 0, 4, 'X')
        assertEquals(masked, maskedAgain)
    }

    // -------------------------------------------------------------------------
    // isValid() cases
    // -------------------------------------------------------------------------

    @Test
    fun isValidReturnsTrueForValid() {
        assertTrue(Passport.isValid("M1234567"))
    }

    @Test
    fun isValidReturnsFalseForInvalidFormat() {
        assertFalse(Passport.isValid("11234567"))
    }
}
