// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [Passport].
 *
 * Passport formatPartial: uppercase as-is.
 */
class PassportProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, Passport.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, Passport.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, Passport.validateProgressive("\t"))
    }

    // T2 — partial input returns Typing with uppercase visualText
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        val r1 = Passport.validateProgressive("a")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("A", r1.visualText)

        // 4 chars
        val r4 = Passport.validateProgressive("a123")
        assertIs<ProgressiveResult.Typing>(r4)
        assertEquals("A123", r4.visualText)

        // maxLength - 1 = 7 chars
        val r7 = Passport.validateProgressive("a123456")
        assertIs<ProgressiveResult.Typing>(r7)
        assertEquals("A123456", r7.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        assertEquals(ProgressiveResult.Valid, Passport.validateProgressive("A1234567"))
        assertEquals(ProgressiveResult.Valid, Passport.validateProgressive("a1234567"))
    }

    // T4 — complete input with wrong format returns Invalid(INVALID_FORMAT)
    @Test
    fun t4_invalidFormatAtFullLengthReturnsInvalid() {
        // 8 chars but starts with digit — fails FORMAT_REGEX [A-Z]\d{7}
        val result = Passport.validateProgressive("12345678")
        assertIs<ProgressiveResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    // T5 — non-allowed char returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            Passport.validateProgressive("A 12345"),
        )
        // Devanagari
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            Passport.validateProgressive("A१2345"),
        )
        // Hyphen
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            Passport.validateProgressive("A-12345"),
        )
    }

    // T6 — over maxLength returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        val input = "A12345678"
        val result = Passport.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 8, actual = 9),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars accessible
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = Passport.allowedChars
        assertEquals(8, Passport.maxLength)
        assertTrue('A' in Passport.allowedChars)
        assertTrue('a' in Passport.allowedChars)
        assertTrue('0' in Passport.allowedChars)
        assertTrue('9' in Passport.allowedChars)
        assertFalse(' ' in Passport.allowedChars)
        assertFalse('-' in Passport.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        assertEquals("A1234567", Passport.sanitize("A 1234567"))
        assertEquals("A1234567", Passport.sanitize("A1234567EXTRA"))
        assertEquals("", Passport.sanitize(" - !"))
        assertEquals("", Passport.sanitize(""))
        val once = Passport.sanitize("A1234567EXTRA")
        assertEquals(once, Passport.sanitize(once))
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        val result = Passport.validateProgressive("A 12345678")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        val lengthOnlyResult = Passport.validateProgressive("A12345678")
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
