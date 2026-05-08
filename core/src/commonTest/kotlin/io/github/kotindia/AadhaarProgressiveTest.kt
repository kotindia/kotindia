// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [Aadhaar].
 *
 * Covers the 9-test template from Anika's Phase 2 test strategy §1.3.
 * Tests T1-T9 per spec. Each test is independent and uses static vectors.
 */
class AadhaarProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, Aadhaar.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, Aadhaar.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, Aadhaar.validateProgressive("\t"))
    }

    // T2 — partial input (right char class, length < maxLength) returns Typing with correct visualText
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        // Single digit
        val r1 = Aadhaar.validateProgressive("2")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("2", r1.visualText)

        // 9 digits — chunked(4) produces "1234 5678 9"
        val r9 = Aadhaar.validateProgressive("123456789")
        assertIs<ProgressiveResult.Typing>(r9)
        assertEquals("1234 5678 9", r9.visualText)

        // maxLength - 1 = 11 digits
        val r11 = Aadhaar.validateProgressive("20100000000")
        assertIs<ProgressiveResult.Typing>(r11)
        assertEquals("2010 0000 000", r11.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        // AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[0] = "201000000000"
        val result = Aadhaar.validateProgressive(AadhaarReferenceVectors.KNOWN_VALID_AADHAARS[0])
        assertEquals(ProgressiveResult.Valid, result)
    }

    // T4 — complete input with invalid checksum returns Invalid(INVALID_CHECKSUM, None)
    @Test
    fun t4_invalidChecksumReturnsInvalid() {
        // "201000000000" — last digit flipped: 0→1 = "201000000001"
        val corrupted = "201000000001"
        val result = Aadhaar.validateProgressive(corrupted)
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_CHECKSUM, ValidationContext.None),
            result,
        )
    }

    // T5 — non-allowed char returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        val result = Aadhaar.validateProgressive("1234a567890")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        // Devanagari digit २ (U+0968) must reject — guards against Phase 1 isDigit() regression
        val devanagariResult = Aadhaar.validateProgressive("२34567")
        assertIs<ProgressiveResult.Invalid>(devanagariResult)
        assertEquals(InvalidReason.INVALID_FORMAT, devanagariResult.reason)
    }

    // T6 — over maxLength (all allowed chars) returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        // 13 clean digits
        val input = "2010000000001"
        val result = Aadhaar.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 12, actual = 13),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars are accessible and correct
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = Aadhaar.allowedChars // compile-time type check
        assertEquals(12, Aadhaar.maxLength)
        assertTrue('0' in Aadhaar.allowedChars)
        assertTrue('9' in Aadhaar.allowedChars)
        assertTrue('5' in Aadhaar.allowedChars)
        assertFalse('A' in Aadhaar.allowedChars)
        assertFalse('a' in Aadhaar.allowedChars)
        assertFalse(' ' in Aadhaar.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        // Spaces stripped
        assertEquals("201000000000", Aadhaar.sanitize("2010 0000 0000"))
        // Non-digit stripped, capped at 12
        assertEquals("123456789012", Aadhaar.sanitize("1234abcd56789012345"))
        // All non-allowed → ""
        assertEquals("", Aadhaar.sanitize("abc"))
        // Empty stays empty
        assertEquals("", Aadhaar.sanitize(""))
        // Idempotency: sanitize(sanitize(x)) == sanitize(x)
        val once = Aadhaar.sanitize("1234 5678 9012 extra text 999")
        val twice = Aadhaar.sanitize(once)
        assertEquals(once, twice)
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT (not WRONG_LENGTH)
    // Verifies AC4 evaluation order: priority-2 (non-allowed char) fires before priority-3 (length)
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        // "1234a567890123" = 14 chars, contains 'a' (non-allowed)
        val result = Aadhaar.validateProgressive("1234a567890123")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        // Contrast: length-only (no bad char) returns WRONG_LENGTH
        val lengthOnlyResult = Aadhaar.validateProgressive("2010000000001") // 13 clean digits
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
