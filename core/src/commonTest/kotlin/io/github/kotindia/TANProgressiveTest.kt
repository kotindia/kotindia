// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [TAN].
 *
 * TAN maxLength = 10 per PRD Scope table.
 * TAN formatPartial: uppercase as-is.
 * TAN format: `[A-Z]{4}[0-9]{5}[A-Z]`
 */
class TANProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, TAN.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, TAN.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, TAN.validateProgressive("\t"))
    }

    // T2 — partial input returns Typing with uppercase visualText
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        val r1 = TAN.validateProgressive("m")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("M", r1.visualText)

        // 4 chars
        val r4 = TAN.validateProgressive("mumd")
        assertIs<ProgressiveResult.Typing>(r4)
        assertEquals("MUMD", r4.visualText)

        // maxLength - 1 = 9 chars
        val r9 = TAN.validateProgressive("mumd12345")
        assertIs<ProgressiveResult.Typing>(r9)
        assertEquals("MUMD12345", r9.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        // Format: [A-Z]{4}[0-9]{5}[A-Z]
        assertEquals(ProgressiveResult.Valid, TAN.validateProgressive("MUMD12345A"))
        assertEquals(ProgressiveResult.Valid, TAN.validateProgressive("mumd12345a"))
    }

    // T4 — complete input with wrong format returns Invalid(INVALID_FORMAT)
    @Test
    fun t4_invalidFormatAtFullLengthReturnsInvalid() {
        // 10 chars but starts with digit — fails [A-Z]{4}[0-9]{5}[A-Z] regex
        val result = TAN.validateProgressive("1234567890")
        assertIs<ProgressiveResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    // T5 — non-allowed char returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            TAN.validateProgressive("MUM 123"),
        )
        // Devanagari
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            TAN.validateProgressive("MUM१23"),
        )
        // Hyphen
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            TAN.validateProgressive("MUM-123"),
        )
    }

    // T6 — over maxLength returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        val input = "MUMD12345AB" // 11 chars
        val result = TAN.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 10, actual = 11),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars accessible
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = TAN.allowedChars
        assertEquals(10, TAN.maxLength)
        assertTrue('A' in TAN.allowedChars)
        assertTrue('a' in TAN.allowedChars)
        assertTrue('0' in TAN.allowedChars)
        assertTrue('9' in TAN.allowedChars)
        assertFalse(' ' in TAN.allowedChars)
        assertFalse('-' in TAN.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        assertEquals("MUMD12345A", TAN.sanitize("MUMD 12345A"))
        assertEquals("MUMD12345A", TAN.sanitize("MUMD12345AEXTRA"))
        assertEquals("", TAN.sanitize(" - !"))
        assertEquals("", TAN.sanitize(""))
        val once = TAN.sanitize("MUMD12345AEXTRA")
        assertEquals(once, TAN.sanitize(once))
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT (bad-char fires first)
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        val result = TAN.validateProgressive("MUMD 12345AB")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        val lengthOnlyResult = TAN.validateProgressive("MUMD12345AB")
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
