// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [ESIC].
 *
 * ESIC maxLength = 17 per PRD Scope table.
 * ESIC formatPartial: digits as-is (no spacing per OQ-14-2).
 */
class ESICProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, ESIC.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, ESIC.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, ESIC.validateProgressive("\t"))
    }

    // T2 — partial input returns Typing with digits as-is visualText
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        val r1 = ESIC.validateProgressive("1")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("1", r1.visualText)

        // 8 chars
        val r8 = ESIC.validateProgressive("12345678")
        assertIs<ProgressiveResult.Typing>(r8)
        assertEquals("12345678", r8.visualText)

        // maxLength - 1 = 16 chars
        val r16 = ESIC.validateProgressive("1234567890123456")
        assertIs<ProgressiveResult.Typing>(r16)
        assertEquals("1234567890123456", r16.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        // 17 digits — ESIC has no checksum, any 17-digit string is valid
        assertEquals(ProgressiveResult.Valid, ESIC.validateProgressive("12345678901234567"))
        assertEquals(ProgressiveResult.Valid, ESIC.validateProgressive("00000000000000000"))
    }

    // T4 — complete 17-char input with non-digit characters returns Invalid(INVALID_FORMAT)
    // Note: ESIC validate() normalises by stripping internal whitespace. Using a 17-char
    // input with a non-digit char that passes through trim but hits the format check.
    // Since validateProgressive fires bad-char check FIRST, any non-digit at any length
    // returns INVALID_FORMAT.
    @Test
    fun t4_nonDigitAtFullLengthReturnsInvalidFormat() {
        // 17 chars but includes a letter — bad-char fires first
        val result = ESIC.validateProgressive("1234567890123456A")
        assertIs<ProgressiveResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    // T5 — non-allowed char returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            ESIC.validateProgressive("123 456"),
        )
        // Devanagari digit
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            ESIC.validateProgressive("123४56"),
        )
        // Hyphen
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            ESIC.validateProgressive("123-456"),
        )
    }

    // T6 — over maxLength returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        val input = "123456789012345678" // 18 digits
        val result = ESIC.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 17, actual = 18),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars accessible
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = ESIC.allowedChars
        assertEquals(17, ESIC.maxLength)
        assertTrue('0' in ESIC.allowedChars)
        assertTrue('9' in ESIC.allowedChars)
        assertFalse('A' in ESIC.allowedChars)
        assertFalse(' ' in ESIC.allowedChars)
        assertFalse('-' in ESIC.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        assertEquals("12345678901234567", ESIC.sanitize("1234567890123456789"))
        assertEquals("12345678901234567", ESIC.sanitize("12345-67890-12345-67"))
        assertEquals("", ESIC.sanitize(" - !"))
        assertEquals("", ESIC.sanitize(""))
        val once = ESIC.sanitize("1234567890123456789")
        assertEquals(once, ESIC.sanitize(once))
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT (bad-char fires first)
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        val result = ESIC.validateProgressive("1234567890123456 8")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        val lengthOnlyResult = ESIC.validateProgressive("123456789012345678")
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
