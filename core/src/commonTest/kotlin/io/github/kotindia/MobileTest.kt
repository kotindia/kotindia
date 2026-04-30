// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// MobileTest — unit tests for Mobile validator (Slice 3)
// Template: all per-validator unit test files follow this structure.
// Minimum: ≥30 @Test methods (AC7).
// Property tests NOT required for Mobile (AC8 — no checksum).
// ---------------------------------------------------------------------------

class MobileTest {
    // -----------------------------------------------------------------------
    // Valid cases (≥10)
    // All 8 normalisation forms from AC2, plus prefix boundaries.
    // -----------------------------------------------------------------------

    @Test
    fun validRaw10Digits() {
        assertIs<ValidationResult.Valid>(Mobile.validate("9876543210"))
    }

    @Test
    fun validSpaceSeparated() {
        assertIs<ValidationResult.Valid>(Mobile.validate("98765 43210"))
    }

    @Test
    fun validE164NoSpace() {
        assertIs<ValidationResult.Valid>(Mobile.validate("+919876543210"))
    }

    @Test
    fun validE164WithSpaceAfterCode() {
        assertIs<ValidationResult.Valid>(Mobile.validate("+91 9876543210"))
    }

    @Test
    fun validE164WithSpaces() {
        assertIs<ValidationResult.Valid>(Mobile.validate("+91 98765 43210"))
    }

    @Test
    fun validE164WithHyphens() {
        assertIs<ValidationResult.Valid>(Mobile.validate("+91-98765-43210"))
    }

    @Test
    fun validLeadingZero() {
        // "09876543210" → strip leading 0 → "9876543210" → valid
        assertIs<ValidationResult.Valid>(Mobile.validate("09876543210"))
    }

    @Test
    fun validWhitespacePadding() {
        assertIs<ValidationResult.Valid>(Mobile.validate(" 9876543210 "))
    }

    @Test
    fun validBoundaryStartsWith6() {
        assertIs<ValidationResult.Valid>(Mobile.validate("6000000000"))
    }

    @Test
    fun validBoundaryStartsWith9() {
        assertIs<ValidationResult.Valid>(Mobile.validate("9999999999"))
    }

    @Test
    fun validStartsWith7() {
        assertIs<ValidationResult.Valid>(Mobile.validate("7000000000"))
    }

    @Test
    fun validStartsWith8() {
        assertIs<ValidationResult.Valid>(Mobile.validate("8000000000"))
    }

    // -----------------------------------------------------------------------
    // EMPTY cases (≥3)
    // -----------------------------------------------------------------------

    @Test
    fun invalidEmpty() {
        val result = Mobile.validate("")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun invalidBlankSpaces() {
        val result = Mobile.validate("   ")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun invalidTabNewline() {
        val result = Mobile.validate("\t\n")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    // -----------------------------------------------------------------------
    // WRONG_LENGTH cases (≥4)
    // -----------------------------------------------------------------------

    @Test
    fun invalidTooShort3Digits() {
        val result = Mobile.validate("123")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun invalidTooLong11Digits() {
        val result = Mobile.validate("12345678901")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun invalidE164TooShortAfterStrip() {
        // "+91 1234" → strip +91 → " 1234" → strip space → "1234" → 4 digits → WRONG_LENGTH
        val result = Mobile.validate("+91 1234")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun invalidSevenDigits() {
        val result = Mobile.validate("9999999")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun invalidLeadingZeroResultsInNineDigits() {
        // "0876543210" → strip leading 0 → "876543210" → 9 digits → WRONG_LENGTH (NOT INVALID_PREFIX).
        // Document this edge: stripping the leading zero reduces length below 10.
        val result = Mobile.validate("0876543210")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    // -----------------------------------------------------------------------
    // INVALID_FORMAT cases (≥3)
    // -----------------------------------------------------------------------

    @Test
    fun invalidFormatAllLetters() {
        // 10 chars, all letters — passes length check but fails digit check
        val result = Mobile.validate("abcdefghij")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatEmbeddedLetters() {
        val result = Mobile.validate("9876ABC210")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatNonDigitAtEnd() {
        // "98765-4321X" → strip hyphen → "987654321X" → non-digit → INVALID_FORMAT
        val result = Mobile.validate("98765-4321X")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    // -----------------------------------------------------------------------
    // INVALID_PREFIX cases (≥4)
    // -----------------------------------------------------------------------

    @Test
    fun invalidPrefixStartsWith1() {
        val result = Mobile.validate("1234567890")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    @Test
    fun invalidPrefixStartsWith5() {
        val result = Mobile.validate("5876543210")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    @Test
    fun invalidPrefixStartsWith4() {
        val result = Mobile.validate("4000000000")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    @Test
    fun invalidPrefixDoubleLeadingZero() {
        // "00876543210" → no +91 prefix → strip one leading 0 → "0876543210"
        // 10 digits, starts with 0 → INVALID_PREFIX
        val result = Mobile.validate("00876543210")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    @Test
    fun invalidPrefixE164LeadingZeroResultsInWrongLength() {
        // "+910876543210" → strip +91 → "0876543210" → strip leading 0 → "876543210" → 9 digits → WRONG_LENGTH
        // Edge: after stripping +91 AND leading 0, we're left with 9 digits, not a prefix failure.
        val result = Mobile.validate("+910876543210")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    // -----------------------------------------------------------------------
    // format() cases (≥6)
    // -----------------------------------------------------------------------

    @Test
    fun formatWithoutCountryCode() {
        assertEquals("98765 43210", Mobile.format("9876543210"))
    }

    @Test
    fun formatWithCountryCode() {
        assertEquals("+91 98765 43210", Mobile.format("9876543210", withCountryCode = true))
    }

    @Test
    fun formatIdempotent() {
        // format(format(x)) should produce the same output
        val formatted = Mobile.format("9876543210")
        assertEquals(formatted, Mobile.format(formatted))
    }

    @Test
    fun formatFromE164Input() {
        assertEquals("98765 43210", Mobile.format("+91 98765 43210"))
    }

    @Test
    fun formatInvalidInputThrows() {
        assertFailsWith<IllegalArgumentException> {
            Mobile.format("invalid")
        }
    }

    @Test
    fun formatTooShortThrows() {
        assertFailsWith<IllegalArgumentException> {
            Mobile.format("123")
        }
    }

    // -----------------------------------------------------------------------
    // mask() cases (≥9)
    // -----------------------------------------------------------------------

    @Test
    fun maskDefaultParams() {
        // Default: visibleStart=0, visibleEnd=4 → last 4 visible
        assertEquals("XXXXXX3210", Mobile.mask("9876543210"))
    }

    @Test
    fun maskCustomVisibleStartAndEnd() {
        // visibleStart=2, visibleEnd=2 → "98" + "******" (6 masked) + "10" = "98******10"
        assertEquals("98******10", Mobile.mask("9876543210", visibleStart = 2, visibleEnd = 2, maskChar = '*'))
    }

    @Test
    fun maskFullMask() {
        // visibleStart=0, visibleEnd=0 → all masked
        assertEquals("XXXXXXXXXX", Mobile.mask("9876543210", visibleStart = 0, visibleEnd = 0))
    }

    @Test
    fun maskCustomMaskChar() {
        assertEquals("######3210", Mobile.mask("9876543210", maskChar = '#'))
    }

    @Test
    fun maskEmptyStringReturnsUnchanged() {
        assertEquals("", Mobile.mask(""))
    }

    @Test
    fun maskInvalidInputCharByChar() {
        // Marcus arch fix (dec_20260430_193150_101c4a): mask operates char-by-char on raw input.
        // "invalid" length=7, visibleStart=0, visibleEnd=4 → last 4 = "alid", middle 3 masked.
        // Old buggy behavior: returned "invalid" unchanged (was calling isValid() guard).
        assertEquals("XXXalid", Mobile.mask("invalid"))
    }

    @Test
    fun maskInvalidShortStringCharByChar() {
        // "12abc" length=5, visibleStart=0, visibleEnd=4 → last 4 = "2abc", middle 1 masked.
        // Old buggy behavior: returned "12abc" unchanged.
        assertEquals("X2abc", Mobile.mask("12abc"))
    }

    @Test
    fun maskOverlapReturnsFullString() {
        // visibleStart=5, visibleEnd=6 → 5+6=11 >= 10 → entire raw string returned unmasked.
        val result = Mobile.mask("9876543210", visibleStart = 5, visibleEnd = 6)
        assertEquals("9876543210", result)
    }

    @Test
    fun maskIdempotent() {
        // mask(mask(x)) == mask(x) for same params (AC4 idempotency requirement).
        // XXXXXX3210 length=10, visibleStart=0, visibleEnd=4 → last 4 = "3210", middle 6 masked → "XXXXXX3210" ✓
        val once = Mobile.mask("9876543210")
        val twice = Mobile.mask(once)
        assertEquals(once, twice)
    }

    // -----------------------------------------------------------------------
    // mask() — non-normalised input cases (locking Marcus arch fix semantic)
    // -----------------------------------------------------------------------

    @Test
    fun maskE164FormCharByChar() {
        // "+91 98765 43210" length=15, visibleStart=0, visibleEnd=4
        // last 4 = "3210", middle 11 masked → "XXXXXXXXXXX3210"
        assertEquals("XXXXXXXXXXX3210", Mobile.mask("+91 98765 43210"))
    }

    @Test
    fun maskLeadingZeroFormCharByChar() {
        // "09876543210" length=11, visibleStart=0, visibleEnd=4
        // last 4 = "3210", middle 7 masked → "XXXXXXX3210"
        assertEquals("XXXXXXX3210", Mobile.mask("09876543210"))
    }

    @Test
    fun maskWhitespacePaddedCharByChar() {
        // " 9876543210 " length=12, visibleStart=0, visibleEnd=4
        // last 4 = indices 8..11 = '2','1','0',' ' → "210 ", middle 8 masked → "XXXXXXXX210 "
        assertEquals("XXXXXXXX210 ", Mobile.mask(" 9876543210 "))
    }

    // -----------------------------------------------------------------------
    // isValid() cases (≥2)
    // -----------------------------------------------------------------------

    @Test
    fun isValidReturnsTrueForValid() {
        assertTrue(Mobile.isValid("9876543210"))
    }

    @Test
    fun isValidReturnsFalseForInvalid() {
        assertFalse(Mobile.isValid("1234567890"))
    }
}
