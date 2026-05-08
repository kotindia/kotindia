// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [Pincode].
 *
 * Covers the 9-test template from Anika's Phase 2 test strategy §1.3.
 * Tests T1-T9 per spec. Each test is independent and uses static vectors.
 *
 * Pincode formatPartial: digits as-is per OQ-14-2 Marcus ruling (no spacing).
 */
class PincodeProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, Pincode.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, Pincode.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, Pincode.validateProgressive("\t"))
    }

    // T2 — partial input (right char class, length < maxLength) returns Typing with correct visualText
    // Pincode formatPartial: digits as-is
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        val r1 = Pincode.validateProgressive("5")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("5", r1.visualText)

        val r3 = Pincode.validateProgressive("560")
        assertIs<ProgressiveResult.Typing>(r3)
        assertEquals("560", r3.visualText)

        // maxLength - 1 = 5 digits
        val r5 = Pincode.validateProgressive("56000")
        assertIs<ProgressiveResult.Typing>(r5)
        assertEquals("56000", r5.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        assertEquals(ProgressiveResult.Valid, Pincode.validateProgressive("560001"))
        assertEquals(ProgressiveResult.Valid, Pincode.validateProgressive("110001"))
    }

    // T4 — complete input with wrong format (invalid prefix: starts with 0) returns Invalid
    @Test
    fun t4_invalidPrefixAtFullLengthReturnsInvalid() {
        val result = Pincode.validateProgressive("000001")
        assertIs<ProgressiveResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    // T5 — non-allowed char (letter, Devanagari) returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            Pincode.validateProgressive("56a001"),
        )
        // Devanagari digit
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            Pincode.validateProgressive("५60001"),
        )
        // Space in middle
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            Pincode.validateProgressive("560 001"),
        )
    }

    // T6 — over maxLength (all allowed chars) returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        val input = "5600011"
        val result = Pincode.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 6, actual = 7),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars are accessible and correct
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = Pincode.allowedChars
        assertEquals(6, Pincode.maxLength)
        assertTrue('0' in Pincode.allowedChars)
        assertTrue('9' in Pincode.allowedChars)
        assertFalse('A' in Pincode.allowedChars)
        assertFalse(' ' in Pincode.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        assertEquals("560001", Pincode.sanitize("560 001"))
        assertEquals("560001", Pincode.sanitize("560001extra99"))
        assertEquals("", Pincode.sanitize("ABC"))
        assertEquals("", Pincode.sanitize(""))
        val once = Pincode.sanitize("560001extra99")
        assertEquals(once, Pincode.sanitize(once))
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT (not WRONG_LENGTH)
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        val result = Pincode.validateProgressive("560a0011")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        val lengthOnlyResult = Pincode.validateProgressive("5600011")
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
