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
// PincodeTest — unit tests for Pincode validator (Slice 4)
// Template: follows per-validator structure established by Slice 3 (MobileTest).
// Minimum: ≥25 @Test methods (AC7). 3 additional per Anika warnings.
// Property tests NOT required — Pincode has no checksum algorithm.
// ---------------------------------------------------------------------------

class PincodeTest {
    // -----------------------------------------------------------------------
    // Valid cases (≥10)
    // -----------------------------------------------------------------------

    @Test
    fun validBengaluru() {
        assertIs<ValidationResult.Valid>(Pincode.validate("560001"))
    }

    @Test
    fun validNewDelhi() {
        assertIs<ValidationResult.Valid>(Pincode.validate("110001"))
    }

    @Test
    fun validMumbai() {
        assertIs<ValidationResult.Valid>(Pincode.validate("400001"))
    }

    @Test
    fun validKolkata() {
        assertIs<ValidationResult.Valid>(Pincode.validate("700001"))
    }

    @Test
    fun validChennai() {
        assertIs<ValidationResult.Valid>(Pincode.validate("600001"))
    }

    @Test
    fun validIndiPostDisplayForm() {
        // "560 001" — India Post canonical display form with space at index 3
        assertIs<ValidationResult.Valid>(Pincode.validate("560 001"))
    }

    @Test
    fun validWhitespacePadded() {
        // Leading/trailing whitespace stripped before validation
        assertIs<ValidationResult.Valid>(Pincode.validate(" 560001 "))
    }

    @Test
    fun validFirstDigitBoundaryOne() {
        // First digit 1 — lowest valid zone prefix
        assertIs<ValidationResult.Valid>(Pincode.validate("100000"))
    }

    @Test
    fun validFirstDigitBoundaryNine() {
        // First digit 9 — APS zone, highest valid zone prefix
        assertIs<ValidationResult.Valid>(Pincode.validate("900000"))
    }

    @Test
    fun validUpperBound() {
        assertIs<ValidationResult.Valid>(Pincode.validate("999999"))
    }

    @Test
    fun validOneLowerPad() {
        // Another typical value with first-digit 1
        assertIs<ValidationResult.Valid>(Pincode.validate("100001"))
    }

    // -----------------------------------------------------------------------
    // EMPTY cases (≥3)
    // -----------------------------------------------------------------------

    @Test
    fun invalidEmpty() {
        val result = Pincode.validate("")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun invalidBlankSpaces() {
        val result = Pincode.validate("   ")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun invalidTabNewline() {
        val result = Pincode.validate("\t\n")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    // -----------------------------------------------------------------------
    // WRONG_LENGTH cases (≥4, including Anika warning #2)
    // -----------------------------------------------------------------------

    @Test
    fun invalidTooShort4Digits() {
        val result = Pincode.validate("5600")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun invalidTooLong7Digits() {
        val result = Pincode.validate("5600015")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun invalidTooShort5Digits() {
        val result = Pincode.validate("56001")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    /**
     * Anika warning #2 (fb_20260501_013314_f73996): hyphens are NOT stripped during
     * normalisation. "560-001" normalises to "560-001" (7 chars) → WRONG_LENGTH.
     */
    @Test
    fun invalidHyphenInserted_wrongLength() {
        val result = Pincode.validate("560-001")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    // -----------------------------------------------------------------------
    // INVALID_FORMAT cases (≥4, including Anika warning #1 Unicode)
    // -----------------------------------------------------------------------

    @Test
    fun invalidFormatAllLetters() {
        val result = Pincode.validate("abcdef")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatEmbeddedLetter() {
        val result = Pincode.validate("56000A")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    /**
     * Anika warning #1 (fb_20260501_013310_f6fa13): Unicode/non-ASCII character.
     * test-strategy.md §5 mandates at least one Unicode input test per validator.
     * "अ60001" — Devanagari 'अ' + 5 digits, correct length after stripping spaces,
     * but non-digit char → INVALID_FORMAT.
     */
    @Test
    fun invalidFormatUnicodeDevanagari() {
        val result = Pincode.validate("अ60001")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    /**
     * Devanagari digits ('०'..'९', U+0966..U+096F) pass Kotlin's isDigit() (Unicode Nd category).
     * Library MUST reject them — only ASCII '0'..'9' are valid Pincode digits.
     * Without the ASCII range guard, this 6-char all-Devanagari input would silently validate as Valid.
     */
    @Test
    fun invalidFormatAllDevanagariDigits() {
        val result = Pincode.validate("५६००१०")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatSpecialChar() {
        val result = Pincode.validate("5600!1")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    // -----------------------------------------------------------------------
    // INVALID_PREFIX cases (≥3)
    // -----------------------------------------------------------------------

    @Test
    fun invalidPrefixLeadingZero() {
        // India Post pincodes never start with 0 — no valid postal zone maps to 0.
        val result = Pincode.validate("050001")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    @Test
    fun invalidPrefixAllZeros() {
        val result = Pincode.validate("000000")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    @Test
    fun invalidPrefixZeroOtherwiseNumeric() {
        val result = Pincode.validate("000001")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    // -----------------------------------------------------------------------
    // format() cases (≥5)
    // -----------------------------------------------------------------------

    @Test
    fun formatRawDigits() {
        assertEquals("560 001", Pincode.format("560001"))
    }

    @Test
    fun formatIdempotent() {
        // format(format(x)) == format(x)
        val once = Pincode.format("560001")
        assertEquals(once, Pincode.format(once))
    }

    @Test
    fun formatFromWhitespacePadded() {
        assertEquals("560 001", Pincode.format(" 560001 "))
    }

    @Test
    fun formatInvalidThrows() {
        assertFailsWith<IllegalArgumentException> {
            Pincode.format("invalid")
        }
    }

    @Test
    fun formatInvalidPrefixThrows() {
        assertFailsWith<IllegalArgumentException> {
            Pincode.format("050001")
        }
    }

    // -----------------------------------------------------------------------
    // isValid() cases (≥2)
    // -----------------------------------------------------------------------

    @Test
    fun isValidReturnsTrueForValid() {
        assertTrue(Pincode.isValid("560001"))
    }

    @Test
    fun isValidReturnsFalseForInvalid() {
        assertFalse(Pincode.isValid("050001"))
    }

    // -----------------------------------------------------------------------
    // Anika warning #3 (fb_20260501_013317_564b15): double-space behavior
    // Decision: strip ALL internal whitespace → "560  001" → "560001" → Valid.
    // Documented in Pincode.kt KDoc. normalize() uses Regex("\\s") which covers
    // any whitespace character and quantity.
    // -----------------------------------------------------------------------

    @Test
    fun doubleSpaceStrippedToValid() {
        // "560  001" → trim (no-op) → strip all internal whitespace → "560001" → Valid
        assertIs<ValidationResult.Valid>(Pincode.validate("560  001"))
    }
}
