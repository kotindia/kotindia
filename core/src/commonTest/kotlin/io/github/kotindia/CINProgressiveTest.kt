// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [CIN].
 *
 * CIN formatPartial: uppercase as-is (no spacing).
 */
class CINProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, CIN.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, CIN.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, CIN.validateProgressive("\t"))
    }

    // T2 — partial input returns Typing with uppercase visualText
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        val r1 = CIN.validateProgressive("u")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("U", r1.visualText)

        val r10 = CIN.validateProgressive("u72200ka20")
        assertIs<ProgressiveResult.Typing>(r10)
        assertEquals("U72200KA20", r10.visualText)

        // maxLength - 1 = 20 chars
        val r20 = CIN.validateProgressive("u72200ka2013ptc09738")
        assertIs<ProgressiveResult.Typing>(r20)
        assertEquals("U72200KA2013PTC09738", r20.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        assertEquals(ProgressiveResult.Valid, CIN.validateProgressive("L17110MH1973PLC019786"))
        assertEquals(ProgressiveResult.Valid, CIN.validateProgressive("l17110mh1973plc019786"))
    }

    // T4 — complete input with wrong format returns Invalid(INVALID_FORMAT or INVALID_PREFIX)
    @Test
    fun t4_invalidFormatAtFullLengthReturnsInvalid() {
        // Starts with 'X' instead of L or U — INVALID_PREFIX
        val result = CIN.validateProgressive("X17110MH1973PLC019786")
        assertIs<ProgressiveResult.Invalid>(result)
        assertTrue(result.reason == InvalidReason.INVALID_PREFIX || result.reason == InvalidReason.INVALID_FORMAT)
    }

    // T5 — non-allowed char returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            CIN.validateProgressive("L172 00KA"),
        )
        // Devanagari
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            CIN.validateProgressive("L172१0KA"),
        )
        // Hyphen
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            CIN.validateProgressive("L-72200KA"),
        )
    }

    // T6 — over maxLength returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        val input = "L17110MH1973PLC019786X"
        val result = CIN.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 21, actual = 22),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars accessible
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = CIN.allowedChars
        assertEquals(21, CIN.maxLength)
        assertTrue('A' in CIN.allowedChars)
        assertTrue('a' in CIN.allowedChars)
        assertTrue('0' in CIN.allowedChars)
        assertFalse(' ' in CIN.allowedChars)
        assertFalse('-' in CIN.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        assertEquals("L17110MH1973PLC019786", CIN.sanitize("L17110MH1973PLC019786"))
        assertEquals("L17110MH1973PLC019786", CIN.sanitize("L17110MH1973PLC019786EXTRA"))
        assertEquals("", CIN.sanitize(" - @ !"))
        assertEquals("", CIN.sanitize(""))
        val once = CIN.sanitize("L17110MH1973PLC019786EXTRA")
        assertEquals(once, CIN.sanitize(once))
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        val result = CIN.validateProgressive("L17110MH1973PLC019786@XYZ")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        val lengthOnlyResult = CIN.validateProgressive("L17110MH1973PLC019786X")
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
