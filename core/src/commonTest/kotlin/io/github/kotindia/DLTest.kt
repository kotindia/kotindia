// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DLTest {
    // -------------------------------------------------------------------------
    // Valid — variety of state codes, separator forms, 1-digit and 2-digit RTO
    // -------------------------------------------------------------------------

    @Test
    fun valid_maharashtra_twoDigitRto() {
        assertEquals(ValidationResult.Valid, DL.validate("MH1220110012345"))
    }

    @Test
    fun valid_karnataka_twoDigitRto() {
        assertEquals(ValidationResult.Valid, DL.validate("KA0520150098765"))
    }

    @Test
    fun valid_delhi_twoDigitRto() {
        assertEquals(ValidationResult.Valid, DL.validate("DL0520160012345"))
    }

    @Test
    fun valid_tamilNadu_twoDigitRto() {
        assertEquals(ValidationResult.Valid, DL.validate("TN3820190056789"))
    }

    @Test
    fun valid_uttarPradesh_twoDigitRto() {
        assertEquals(ValidationResult.Valid, DL.validate("UP8020120099999"))
    }

    @Test
    fun valid_kerala_twoDigitRto() {
        assertEquals(ValidationResult.Valid, DL.validate("KL0120180012345"))
    }

    @Test
    fun valid_gujarat_twoDigitRto() {
        assertEquals(ValidationResult.Valid, DL.validate("GJ0120170056789"))
    }

    @Test
    fun valid_rajasthan_twoDigitRto() {
        assertEquals(ValidationResult.Valid, DL.validate("RJ0920150012345"))
    }

    @Test
    fun valid_punjab_twoDigitRto() {
        assertEquals(ValidationResult.Valid, DL.validate("PB0820140056789"))
    }

    @Test
    fun valid_westBengal_twoDigitRto() {
        assertEquals(ValidationResult.Valid, DL.validate("WB0120200012345"))
    }

    @Test
    fun valid_ladakh_postReorganisation2019() {
        // LA is a post-2019 UT code — must be accepted
        assertEquals(ValidationResult.Valid, DL.validate("LA0120220012345"))
    }

    @Test
    fun valid_odishaLegacyOrCode() {
        // OR is the pre-2011 Odisha ISO code; existing DLs issued with OR remain valid
        assertEquals(ValidationResult.Valid, DL.validate("OR0120050012345"))
    }

    @Test
    fun valid_odishaCurrentOdCode() {
        assertEquals(ValidationResult.Valid, DL.validate("OD0120140012345"))
    }

    @Test
    fun valid_hyphenSeparatorNormalised() {
        // "MH12-20110012345" → strip hyphens → "MH1220110012345"
        assertEquals(ValidationResult.Valid, DL.validate("MH12-20110012345"))
    }

    @Test
    fun valid_lowercaseNormalised() {
        // lowercase → uppercase
        assertEquals(ValidationResult.Valid, DL.validate("mh1220110012345"))
    }

    @Test
    fun valid_oneDigitRto() {
        // 1-digit RTO → 14 chars total: state(2)+rto(1)+year+serial(11)
        assertEquals(ValidationResult.Valid, DL.validate("MH120110012345"))
    }

    @Test
    fun valid_spaceSeparatorNormalised() {
        // space stripped
        assertEquals(ValidationResult.Valid, DL.validate("MH12 20110012345"))
    }

    @Test
    fun valid_mixedSeparatorsNormalised() {
        // multiple separators
        assertEquals(ValidationResult.Valid, DL.validate("mh12-2011-0012345"))
    }

    @Test
    fun valid_leadingTrailingWhitespace() {
        // trim + strip
        assertEquals(ValidationResult.Valid, DL.validate(" MH12 2011 0012345 "))
    }

    @Test
    fun valid_assamState() {
        assertEquals(ValidationResult.Valid, DL.validate("AS0120180056789"))
    }

    // -------------------------------------------------------------------------
    // EMPTY
    // -------------------------------------------------------------------------

    @Test
    fun empty_emptyString() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), DL.validate(""))
    }

    @Test
    fun empty_spaces() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), DL.validate("   "))
    }

    @Test
    fun empty_tabNewline() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), DL.validate("\t\n"))
    }

    // -------------------------------------------------------------------------
    // WRONG_LENGTH
    // -------------------------------------------------------------------------

    @Test
    fun wrongLength_thirteenChars() {
        // 13 chars after normalize — too short (min is 14)
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), DL.validate("MH12011001234"))
    }

    @Test
    fun wrongLength_sixteenChars() {
        // 16 chars — too long (max is 15)
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), DL.validate("MH12201100123456"))
    }

    @Test
    fun wrongLength_twoCharsOnly() {
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), DL.validate("MH"))
    }

    // -------------------------------------------------------------------------
    // INVALID_FORMAT
    // -------------------------------------------------------------------------

    @Test
    fun invalidFormat_nonAlphanumericChar() {
        // @ in position 4 — not matching regex
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), DL.validate("MH12@0110012345"))
    }

    @Test
    fun invalidFormat_numericPrefix_stateNotAtStart() {
        // numeric chars at start — regex requires [A-Z]{2} first
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), DL.validate("1220110012345MH"))
    }

    @Test
    fun invalidFormat_letterInRtoPosition() {
        // 'A' in RTO digit position → regex rejects
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), DL.validate("MH1A20110012345"))
    }

    @Test
    fun invalidFormat_devanagariUnicode() {
        // Devanagari char in correct-length input — regex [A-Z] and [0-9] reject non-ASCII
        // 14 chars total (1 devanagari + 13 ASCII), passes WRONG_LENGTH check, fails regex
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), DL.validate("मH1220110012345"))
    }

    @Test
    fun invalidFormat_lettersInSerialPosition() {
        // 'AB' in year/serial area — regex rejects
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), DL.validate("MH1220110AB2345"))
    }

    @Test
    fun invalidFormat_allLetters() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), DL.validate("MHABCDEFGHIJKLM"))
    }

    // -------------------------------------------------------------------------
    // INVALID_PREFIX
    // -------------------------------------------------------------------------

    @Test
    fun invalidPrefix_ZZ() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_PREFIX), DL.validate("ZZ1220110012345"))
    }

    @Test
    fun invalidPrefix_XX() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_PREFIX), DL.validate("XX0520150098765"))
    }

    @Test
    fun invalidPrefix_AA() {
        // AA is structurally valid letters but not an Indian state code
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_PREFIX), DL.validate("AA0120200012345"))
    }

    // -------------------------------------------------------------------------
    // format()
    // -------------------------------------------------------------------------

    @Test
    fun format_lowercaseToUppercase() {
        assertEquals("MH1220110012345", DL.format("mh1220110012345"))
    }

    @Test
    fun format_hyphenStripped() {
        assertEquals("MH1220110012345", DL.format("MH12-20110012345"))
    }

    @Test
    fun format_spaceStripped() {
        assertEquals("MH1220110012345", DL.format("MH12 20110012345"))
    }

    @Test
    fun format_idempotent() {
        val once = DL.format("mh12-20110012345")
        val twice = DL.format(once)
        assertEquals(once, twice)
    }

    @Test
    fun format_invalidThrowsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            DL.format("invalid")
        }
    }

    @Test
    fun format_emptyThrowsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            DL.format("")
        }
    }

    @Test
    fun format_invalidPrefixThrowsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            DL.format("ZZ1220110012345")
        }
    }

    // -------------------------------------------------------------------------
    // mask()
    // -------------------------------------------------------------------------

    @Test
    fun mask_defaultLastFourVisible() {
        // Default: visibleStart=0, visibleEnd=4 → "XXXXXXXXXXX2345"
        assertEquals("XXXXXXXXXXX2345", DL.mask("MH1220110012345"))
    }

    @Test
    fun mask_customVisibleStart() {
        // visibleStart=2 → first 2 chars visible + last 4 visible
        assertEquals("MHXXXXXXXXX2345", DL.mask("MH1220110012345", visibleStart = 2))
    }

    @Test
    fun mask_customMaskChar() {
        assertEquals("***********2345", DL.mask("MH1220110012345", maskChar = '*'))
    }

    @Test
    fun mask_emptyStringNeverThrows() {
        assertEquals("", DL.mask(""))
    }

    @Test
    fun mask_overlapRuleReturnsUnmasked() {
        // visibleStart+visibleEnd >= len → return unmasked
        assertEquals("MH12", DL.mask("MH12", visibleStart = 0, visibleEnd = 8))
    }

    @Test
    fun mask_visibleEndZero_allMasked() {
        assertEquals("XXXXXXXXXXXXXXX", DL.mask("MH1220110012345", visibleEnd = 0))
    }

    @Test
    fun mask_rawNonNormalisedInput() {
        // mask operates char-by-char on raw input, no normalization
        // "MH12-20110012345" = 16 chars raw, last 4 = "2345"
        assertEquals("XXXXXXXXXXXX2345", DL.mask("MH12-20110012345"))
    }

    // -------------------------------------------------------------------------
    // isValid()
    // -------------------------------------------------------------------------

    @Test
    fun isValid_trueForValidDl() {
        assertTrue(DL.isValid("MH1220110012345"))
    }

    @Test
    fun isValid_falseForInvalidDl() {
        assertFalse(DL.isValid("ZZ1220110012345"))
    }

    @Test
    fun isValid_falseForEmpty() {
        assertFalse(DL.isValid(""))
    }
}
