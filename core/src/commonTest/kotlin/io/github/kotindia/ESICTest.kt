// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ESICTest {
    // -------------------------------------------------------------------------
    // Valid cases (≥8)
    // -------------------------------------------------------------------------

    @Test
    fun valid_raw17Digits() {
        assertEquals(ValidationResult.Valid, ESIC.validate("12345678901234567"))
    }

    @Test
    fun valid_spaceSeparated() {
        assertEquals(ValidationResult.Valid, ESIC.validate("1234 5678 9012 3456 7"))
    }

    @Test
    fun valid_whitespacePadded() {
        assertEquals(ValidationResult.Valid, ESIC.validate(" 12345678901234567 "))
    }

    @Test
    fun valid_boundaryMinimumPlausible() {
        assertEquals(ValidationResult.Valid, ESIC.validate("10000000000000000"))
    }

    @Test
    fun valid_boundaryAllNines() {
        assertEquals(ValidationResult.Valid, ESIC.validate("99999999999999999"))
    }

    @Test
    fun valid_allZeros() {
        // No prefix rule — 17 zeros is valid format
        assertEquals(ValidationResult.Valid, ESIC.validate("00000000000000000"))
    }

    @Test
    fun valid_digitsDistributed() {
        assertEquals(ValidationResult.Valid, ESIC.validate("12345678901234560"))
    }

    @Test
    fun valid_repeatedDigit() {
        assertEquals(ValidationResult.Valid, ESIC.validate("11111111111111111"))
    }

    // -------------------------------------------------------------------------
    // EMPTY cases (≥3)
    // -------------------------------------------------------------------------

    @Test
    fun empty_emptyString() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), ESIC.validate(""))
    }

    @Test
    fun empty_whitespaceOnly() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), ESIC.validate("   "))
    }

    @Test
    fun empty_tabNewline() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), ESIC.validate("\t\n"))
    }

    // -------------------------------------------------------------------------
    // WRONG_LENGTH cases (≥3)
    // -------------------------------------------------------------------------

    @Test
    fun wrongLength_16Digits() {
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), ESIC.validate("1234567890123456"))
    }

    @Test
    fun wrongLength_18Digits() {
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), ESIC.validate("123456789012345678"))
    }

    @Test
    fun wrongLength_farTooShort() {
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), ESIC.validate("12345"))
    }

    // -------------------------------------------------------------------------
    // INVALID_FORMAT cases (≥3)
    // -------------------------------------------------------------------------

    @Test
    fun invalidFormat_alphaAtEnd() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), ESIC.validate("1234567890123456A"))
    }

    @Test
    fun invalidFormat_allLetters() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), ESIC.validate("ABCDEFGHIJKLMNOPQ"))
    }

    @Test
    fun invalidFormat_devanagariDigits() {
        // 17 chars including Devanagari decimal digits (०१ = ०१)
        // ASCII-only digit check: chars outside '0'..'9' range → INVALID_FORMAT
        // "1234०१89012345678" = 4 ASCII + 2 Devanagari + 11 ASCII = 17 chars
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            ESIC.validate("1234०१89012345678"),
        )
    }

    // -------------------------------------------------------------------------
    // format() cases (≥5, includes Marcus note 1: idempotency)
    // -------------------------------------------------------------------------

    @Test
    fun format_raw17DigitsReturnsCanonical() {
        assertEquals("12345678901234567", ESIC.format("12345678901234567"))
    }

    @Test
    fun format_spaceSeparatedNormalizesToCanonical() {
        assertEquals("12345678901234567", ESIC.format("1234 5678 9012 3456 7"))
    }

    @Test
    fun format_idempotent() {
        // Marcus note 1: format(format(x)) == format(x)
        val input = "12345678901234567"
        assertEquals(ESIC.format(input), ESIC.format(ESIC.format(input)))
    }

    @Test
    fun format_invalidInputThrows() {
        assertFailsWith<IllegalArgumentException> { ESIC.format("invalid") }
    }

    @Test
    fun format_16DigitsThrows() {
        assertFailsWith<IllegalArgumentException> { ESIC.format("1234567890123456") }
    }

    @Test
    fun format_exceptionMessageContainsTruncatedInput() {
        // Marcus note 2: exception uses value.take(50) truncation
        val ex = assertFailsWith<IllegalArgumentException> { ESIC.format("bad") }
        assertTrue(ex.message!!.contains("bad"), "message should contain the input: ${ex.message}")
    }

    // -------------------------------------------------------------------------
    // mask() cases (≥6)
    // -------------------------------------------------------------------------

    @Test
    fun mask_defaultLast4Visible() {
        assertEquals("XXXXXXXXXXXXX4567", ESIC.mask("12345678901234567"))
    }

    @Test
    fun mask_customVisibleStartAndEnd() {
        assertEquals("1234XXXXXXXXX4567", ESIC.mask("12345678901234567", visibleStart = 4, visibleEnd = 4))
    }

    @Test
    fun mask_fullMask() {
        assertEquals("XXXXXXXXXXXXXXXXX", ESIC.mask("12345678901234567", visibleStart = 0, visibleEnd = 0))
    }

    @Test
    fun mask_customMaskChar() {
        assertEquals("*************4567", ESIC.mask("12345678901234567", maskChar = '*'))
    }

    @Test
    fun mask_emptyInputReturnsEmpty() {
        assertEquals("", ESIC.mask(""))
    }

    @Test
    fun mask_idempotent() {
        val input = "12345678901234567"
        assertEquals(ESIC.mask(input), ESIC.mask(ESIC.mask(input)))
    }

    @Test
    fun mask_overlapReturnsFullString() {
        // visibleStart + visibleEnd >= 17 → full string unmasked
        assertEquals("12345678901234567", ESIC.mask("12345678901234567", visibleStart = 10, visibleEnd = 10))
    }

    @Test
    fun mask_rawNonNormalized() {
        // Raw non-normalized — char-by-char on spaces included (19 chars total)
        val raw = "1234 5678 9012 345"
        val result = ESIC.mask(raw)
        assertEquals(raw.length, result.length)
        assertTrue(result.endsWith(raw.takeLast(4)))
    }

    // -------------------------------------------------------------------------
    // isValid() cases (≥2)
    // -------------------------------------------------------------------------

    @Test
    fun isValid_validNumberReturnsTrue() {
        assertTrue(ESIC.isValid("12345678901234567"))
    }

    @Test
    fun isValid_invalidNumberReturnsFalse() {
        assertFalse(ESIC.isValid("1234"))
    }
}
