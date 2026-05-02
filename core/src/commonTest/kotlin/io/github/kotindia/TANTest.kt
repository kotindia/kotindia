// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TANTest {
    // -------------------------------------------------------------------------
    // Valid inputs — minimum 10 cases
    // -------------------------------------------------------------------------

    @Test
    fun validMumd12345a() {
        assertEquals(ValidationResult.Valid, TAN.validate("MUMD12345A"))
    }

    @Test
    fun validBlrc00001a() {
        assertEquals(ValidationResult.Valid, TAN.validate("BLRC00001A"))
    }

    @Test
    fun validDelp98765z() {
        assertEquals(ValidationResult.Valid, TAN.validate("DELP98765Z"))
    }

    @Test
    fun validKola00000a() {
        assertEquals(ValidationResult.Valid, TAN.validate("KOLA00000A"))
    }

    @Test
    fun validPune12345b() {
        assertEquals(ValidationResult.Valid, TAN.validate("PUNE12345B"))
    }

    @Test
    fun validLowercaseNormalisedToValid() {
        // lowercase: mumd12345a — should normalise to MUMD12345A
        assertEquals(ValidationResult.Valid, TAN.validate("mumd12345a"))
    }

    @Test
    fun validMixedCaseNormalisedToValid() {
        // mixed case: MumD12345a — should normalise to MUMD12345A
        assertEquals(ValidationResult.Valid, TAN.validate("MumD12345a"))
    }

    @Test
    fun validLeadingTrailingWhitespace() {
        assertEquals(ValidationResult.Valid, TAN.validate(" MUMD12345A "))
    }

    @Test
    fun validInternalWhitespace() {
        assertEquals(ValidationResult.Valid, TAN.validate("MUMD 12345 A"))
    }

    @Test
    fun validAbcd00001z() {
        assertEquals(ValidationResult.Valid, TAN.validate("ABCD00001Z"))
    }

    // -------------------------------------------------------------------------
    // EMPTY — minimum 3 cases
    // -------------------------------------------------------------------------

    @Test
    fun emptyString() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.EMPTY),
            TAN.validate(""),
        )
    }

    @Test
    fun whitespaceOnlyString() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.EMPTY),
            TAN.validate("   "),
        )
    }

    @Test
    fun tabAndNewline() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.EMPTY),
            TAN.validate("\t\n"),
        )
    }

    // -------------------------------------------------------------------------
    // WRONG_LENGTH — minimum 3 cases
    // -------------------------------------------------------------------------

    @Test
    fun wrongLength9Chars() {
        // MUMD1234A — 9 chars
        assertEquals(
            ValidationResult.Invalid(InvalidReason.WRONG_LENGTH),
            TAN.validate("MUMD1234A"),
        )
    }

    @Test
    fun wrongLength11Chars() {
        // MUMD123456A — 11 chars
        assertEquals(
            ValidationResult.Invalid(InvalidReason.WRONG_LENGTH),
            TAN.validate("MUMD123456A"),
        )
    }

    @Test
    fun wrongLength4Chars() {
        // MUMD — well under expected length
        assertEquals(
            ValidationResult.Invalid(InvalidReason.WRONG_LENGTH),
            TAN.validate("MUMD"),
        )
    }

    // -------------------------------------------------------------------------
    // INVALID_FORMAT — minimum 5 cases
    // -------------------------------------------------------------------------

    @Test
    fun invalidFormatDigitInFirstFourLetterBlock() {
        // MU1D12345A — digit at position 3 (first-4 must be letters)
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            TAN.validate("MU1D12345A"),
        )
    }

    @Test
    fun invalidFormatLetterInMiddleFiveDigitBlock() {
        // MUMD1A345A — letter at position 6 (positions 5-9 must be digits)
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            TAN.validate("MUMD1A345A"),
        )
    }

    @Test
    fun invalidFormatDigitAtFinalPosition() {
        // MUMD123451 — digit instead of letter at position 10 (must be [A-Z])
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            TAN.validate("MUMD123451"),
        )
    }

    @Test
    fun invalidFormatHyphenSpecialChar() {
        // MUMD-2345A — hyphen is not stripped; 10 chars but fails regex
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            TAN.validate("MUMD-2345A"),
        )
    }

    @Test
    fun invalidFormatDevanagariUnicodeInFirstPosition() {
        // अUMD12345A — Devanagari Unicode at first position
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            TAN.validate("अUMD12345A"),
        )
    }

    // -------------------------------------------------------------------------
    // format() — minimum 4 cases
    // -------------------------------------------------------------------------

    @Test
    fun formatLowercaseNormalisesToUppercase() {
        assertEquals("MUMD12345A", TAN.format("mumd12345a"))
    }

    @Test
    fun formatMixedCaseNormalisesToUppercase() {
        assertEquals("MUMD12345A", TAN.format("MumD12345a"))
    }

    @Test
    fun formatIsIdempotent() {
        val formatted = TAN.format("MUMD12345A")
        assertEquals(formatted, TAN.format(formatted))
    }

    @Test
    fun formatInvalidInputThrowsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            TAN.format("INVALID")
        }
    }

    // -------------------------------------------------------------------------
    // isValid() — minimum 2 cases
    // -------------------------------------------------------------------------

    @Test
    fun isValidReturnsTrueForValidTan() {
        assertTrue(TAN.isValid("MUMD12345A"))
    }

    @Test
    fun isValidReturnsFalseForInvalidTan() {
        assertFalse(TAN.isValid("MUMD12345"))
    }

    // -------------------------------------------------------------------------
    // Additional edge cases (beyond minimums)
    // -------------------------------------------------------------------------

    @Test
    fun formatExceptionMessageContainsTruncatedInput() {
        val ex =
            assertFailsWith<IllegalArgumentException> {
                TAN.format("bad")
            }
        assertTrue(ex.message?.contains("bad") == true)
    }

    @Test
    fun formatInternalWhitespaceProducesCanonicalOutput() {
        assertEquals("MUMD12345A", TAN.format("MUMD 12345 A"))
    }
}
