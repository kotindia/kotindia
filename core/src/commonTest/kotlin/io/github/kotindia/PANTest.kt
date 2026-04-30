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
// PANTest — unit tests for PAN validator (Slice 6)
// Template: per-validator unit test structure (established by Slice 3 Mobile).
// Minimum: ≥35 @Test methods (AC7).
// Property tests NOT required for PAN (no checksum algorithm).
// ---------------------------------------------------------------------------

class PANTest {
    // -----------------------------------------------------------------------
    // Valid cases (≥10)
    // Tests all 10 valid category codes and all 6 normalization input forms.
    // -----------------------------------------------------------------------

    @Test
    fun validRawUppercaseCanonical() {
        // Person category (P) — canonical uppercase form
        assertIs<ValidationResult.Valid>(PAN.validate("ABCPE1234F"))
    }

    @Test
    fun validAllLowercase() {
        // Lowercase normalised to uppercase before validation
        assertIs<ValidationResult.Valid>(PAN.validate("abcpe1234f"))
    }

    @Test
    fun validMixedCase() {
        assertIs<ValidationResult.Valid>(PAN.validate("AbCpE1234f"))
    }

    @Test
    fun validInternalSpaces() {
        // Internal spaces stripped during normalisation
        assertIs<ValidationResult.Valid>(PAN.validate("ABCPE 1234 F"))
    }

    @Test
    fun validWhitespacePadded() {
        // Leading/trailing whitespace trimmed during normalisation
        assertIs<ValidationResult.Valid>(PAN.validate(" ABCPE1234F "))
    }

    @Test
    fun validLowercaseTrailingSpace() {
        assertIs<ValidationResult.Valid>(PAN.validate("abcpe1234f "))
    }

    @Test
    fun validCategoryCompany() {
        // 4th char C = Company
        assertIs<ValidationResult.Valid>(PAN.validate("ABCCE1234F"))
    }

    @Test
    fun validCategoryHuf() {
        // 4th char H = Hindu Undivided Family
        assertIs<ValidationResult.Valid>(PAN.validate("ABCHE1234F"))
    }

    @Test
    fun validCategoryAop() {
        // 4th char A = Association of Persons
        assertIs<ValidationResult.Valid>(PAN.validate("ABCAE1234F"))
    }

    @Test
    fun validCategoryBoi() {
        // 4th char B = Body of Individuals
        assertIs<ValidationResult.Valid>(PAN.validate("ABCBE1234F"))
    }

    @Test
    fun validCategoryGovernment() {
        // 4th char G = Government
        assertIs<ValidationResult.Valid>(PAN.validate("ABCGE1234F"))
    }

    @Test
    fun validCategoryArtificialJuridicalPerson() {
        // 4th char J = Artificial Juridical Person
        assertIs<ValidationResult.Valid>(PAN.validate("ABCJE1234F"))
    }

    @Test
    fun validCategoryLocalAuthority() {
        // 4th char L = Local Authority
        assertIs<ValidationResult.Valid>(PAN.validate("ABCLE1234F"))
    }

    @Test
    fun validCategoryFirm() {
        // 4th char F = Firm / LLP
        assertIs<ValidationResult.Valid>(PAN.validate("ABCFE1234F"))
    }

    @Test
    fun validCategoryTrust() {
        // 4th char T = Trust
        assertIs<ValidationResult.Valid>(PAN.validate("ABCTE1234F"))
    }

    // -----------------------------------------------------------------------
    // EMPTY cases (≥3)
    // -----------------------------------------------------------------------

    @Test
    fun invalidEmpty() {
        val result = PAN.validate("")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun invalidBlankSpaces() {
        val result = PAN.validate("   ")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun invalidTabNewline() {
        val result = PAN.validate("\t\n")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    // -----------------------------------------------------------------------
    // WRONG_LENGTH cases (≥3)
    // -----------------------------------------------------------------------

    @Test
    fun invalidTooShortFiveChars() {
        val result = PAN.validate("ABCPE")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun invalidTooShortEightChars() {
        val result = PAN.validate("ABCPE123")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun invalidTooLongElevenChars() {
        val result = PAN.validate("ABCPE1234FG")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun invalidHyphenCausesWrongLength() {
        // "ABCPE-1234F" → normalize → "ABCPE-1234F" (11 chars, hyphen NOT stripped)
        val result = PAN.validate("ABCPE-1234F")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    // -----------------------------------------------------------------------
    // INVALID_FORMAT cases (≥5)
    // -----------------------------------------------------------------------

    @Test
    fun invalidFormatDigitInFirstFivePositions() {
        // "1BCPE1234F" — digit at position 0 (must be [A-Z])
        val result = PAN.validate("1BCPE1234F")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatLetterInDigitSection() {
        // "ABCPEA234F" — letter 'A' at position 5 (must be [0-9])
        val result = PAN.validate("ABCPEA234F")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatDigitAtPositionNine() {
        // "ABCPE12341" — digit '1' at position 9 (must be [A-Z])
        val result = PAN.validate("ABCPE12341")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatSpecialCharInDigitSection() {
        // "ABCPE!234F" — special char '!' at position 5
        val result = PAN.validate("ABCPE!234F")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatDevanagariUnicode() {
        // "अBCPE1234F" — Devanagari Unicode at position 0 (per Anika §5 Unicode pattern)
        val result = PAN.validate("अBCPE1234F")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    // -----------------------------------------------------------------------
    // INVALID_CATEGORY cases (≥5)
    // These have valid regex format but invalid 4th-char category.
    // All must return INVALID_CATEGORY, NOT INVALID_FORMAT.
    // -----------------------------------------------------------------------

    @Test
    fun invalidCategoryFourthCharZ() {
        // Z is not a valid entity category code
        val result = PAN.validate("ABCZE1234F")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_CATEGORY, result.reason)
    }

    @Test
    fun invalidCategoryFourthCharK() {
        val result = PAN.validate("ABCKE1234F")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_CATEGORY, result.reason)
    }

    @Test
    fun invalidCategoryFourthCharS() {
        val result = PAN.validate("ABCSE1234F")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_CATEGORY, result.reason)
    }

    @Test
    fun invalidCategoryFourthCharM() {
        val result = PAN.validate("ABCME1234F")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_CATEGORY, result.reason)
    }

    @Test
    fun invalidCategoryFourthCharN() {
        val result = PAN.validate("ABCNE1234F")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_CATEGORY, result.reason)
    }

    // -----------------------------------------------------------------------
    // format() cases (≥4)
    // -----------------------------------------------------------------------

    @Test
    fun formatLowercaseNormalized() {
        assertEquals("ABCPE1234F", PAN.format("abcpe1234f"))
    }

    @Test
    fun formatMixedCaseNormalized() {
        assertEquals("ABCPE1234F", PAN.format("AbCpE1234f"))
    }

    @Test
    fun formatIdempotentAlreadyCanonical() {
        assertEquals("ABCPE1234F", PAN.format("ABCPE1234F"))
    }

    @Test
    fun formatIdempotentProperty() {
        // format(format(x)) == format(x) — idempotency property test
        val once = PAN.format("abcpe1234f")
        val twice = PAN.format(once)
        assertEquals(once, twice)
    }

    @Test
    fun formatInternalSpaces() {
        assertEquals("ABCPE1234F", PAN.format("ABCPE 1234 F"))
    }

    @Test
    fun formatWhitespacePadded() {
        assertEquals("ABCPE1234F", PAN.format(" ABCPE1234F "))
    }

    @Test
    fun formatInvalidInputThrows() {
        assertFailsWith<IllegalArgumentException> {
            PAN.format("invalid")
        }
    }

    @Test
    fun formatInvalidCategoryThrows() {
        // INVALID_CATEGORY is still invalid — format() throws on any invalid
        assertFailsWith<IllegalArgumentException> {
            PAN.format("ABCZE1234F")
        }
    }

    @Test
    fun formatWrongLengthThrows() {
        assertFailsWith<IllegalArgumentException> {
            PAN.format("ABCPE123")
        }
    }

    // -----------------------------------------------------------------------
    // mask() cases (≥6)
    // -----------------------------------------------------------------------

    @Test
    fun maskDefaultLastFourVisible() {
        // Default: visibleStart=0, visibleEnd=4 → last 4 visible
        assertEquals("XXXXXX234F", PAN.mask("ABCPE1234F"))
    }

    @Test
    fun maskFirstFivePlusLastOneVisible() {
        // visibleStart=5, visibleEnd=1 → first 5 + last 1 visible; masks digit section
        assertEquals("ABCPEXXXXF", PAN.mask("ABCPE1234F", visibleStart = 5, visibleEnd = 1))
    }

    @Test
    fun maskFullMask() {
        assertEquals("XXXXXXXXXX", PAN.mask("ABCPE1234F", visibleStart = 0, visibleEnd = 0))
    }

    @Test
    fun maskCustomMaskChar() {
        assertEquals("******234F", PAN.mask("ABCPE1234F", maskChar = '*'))
    }

    @Test
    fun maskEmptyStringReturnsEmpty() {
        assertEquals("", PAN.mask(""))
    }

    @Test
    fun maskOverlapReturnsUnmasked() {
        // 6+5=11 >= 10 → entire string returned unmasked (no throw)
        assertEquals("ABCPE1234F", PAN.mask("ABCPE1234F", visibleStart = 6, visibleEnd = 5))
    }

    @Test
    fun maskLowercaseRawCharByChar() {
        // Masker operates on raw input — lowercase visible tail is preserved char-by-char.
        // "abcpe1234f" length=10, default (0,4): last 4 = "234f", 6 masked with default 'X' → "XXXXXX234f"
        assertEquals("XXXXXX234f", PAN.mask("abcpe1234f"))
    }

    @Test
    fun maskIdempotent() {
        // mask(mask(x, 0, 4), 0, 4) == mask(x, 0, 4) when maskChar not in original visible tail
        val once = PAN.mask("ABCPE1234F")
        val twice = PAN.mask(once)
        assertEquals(once, twice)
    }

    @Test
    fun maskCustomMaskCharStar() {
        // visibleStart=5, visibleEnd=1, maskChar='*'
        assertEquals("ABCPE****F", PAN.mask("ABCPE1234F", visibleStart = 5, visibleEnd = 1, maskChar = '*'))
    }

    // -----------------------------------------------------------------------
    // isValid() cases (≥2)
    // -----------------------------------------------------------------------

    @Test
    fun isValidReturnsTrueForValid() {
        assertTrue(PAN.isValid("ABCPE1234F"))
    }

    @Test
    fun isValidReturnsFalseForInvalidCategory() {
        assertFalse(PAN.isValid("ABCZE1234F"))
    }

    @Test
    fun isValidReturnsFalseForEmpty() {
        assertFalse(PAN.isValid(""))
    }

    @Test
    fun isValidReturnsFalseForWrongLength() {
        assertFalse(PAN.isValid("ABCPE123"))
    }
}
