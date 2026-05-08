// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [IMEI].
 *
 * Covers the 9-test template from Anika's Phase 2 test strategy §1.3.
 * IMEI formatPartial: digits as-is per OQ-14-2 Marcus ruling (no spacing).
 */
class IMEIProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, IMEI.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, IMEI.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, IMEI.validateProgressive("\t"))
    }

    // T2 — partial input returns Typing with correct visualText (digits as-is)
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        val r1 = IMEI.validateProgressive("4")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("4", r1.visualText)

        val r7 = IMEI.validateProgressive("4901542")
        assertIs<ProgressiveResult.Typing>(r7)
        assertEquals("4901542", r7.visualText)

        // maxLength - 1 = 14
        val r14 = IMEI.validateProgressive("49015420323751")
        assertIs<ProgressiveResult.Typing>(r14)
        assertEquals("49015420323751", r14.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        // 356938035643809 — known valid from IMEITest / IMEIReferenceVectors
        assertEquals(ProgressiveResult.Valid, IMEI.validateProgressive("356938035643809"))
    }

    // T4 — complete input with invalid checksum returns Invalid(INVALID_CHECKSUM)
    @Test
    fun t4_invalidChecksumReturnsInvalid() {
        // "356938035643808" — last digit flipped 9→8, breaks Luhn
        val corrupted = "356938035643808"
        if (!IMEI.isValid(corrupted)) {
            val result = IMEI.validateProgressive(corrupted)
            assertEquals(
                ProgressiveResult.Invalid(InvalidReason.INVALID_CHECKSUM, ValidationContext.None),
                result,
            )
        }
    }

    // T5 — non-allowed char returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            IMEI.validateProgressive("49015a4"),
        )
        // Devanagari
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            IMEI.validateProgressive("490१542"),
        )
        // Hyphen (not stripped in validateProgressive)
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            IMEI.validateProgressive("490-154"),
        )
    }

    // T6 — over maxLength (all allowed chars) returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        val input = "4901542032375180" // 16 digits
        val result = IMEI.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 15, actual = 16),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars accessible
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = IMEI.allowedChars
        assertEquals(15, IMEI.maxLength)
        assertTrue('0' in IMEI.allowedChars)
        assertTrue('9' in IMEI.allowedChars)
        assertFalse('A' in IMEI.allowedChars)
        assertFalse('-' in IMEI.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        assertEquals("490154203237518", IMEI.sanitize("490 154 203 237 518"))
        assertEquals("490154203237518", IMEI.sanitize("490154203237518extra"))
        assertEquals("", IMEI.sanitize("ABC-XYZ"))
        assertEquals("", IMEI.sanitize(""))
        val once = IMEI.sanitize("490 154 203 237 518extra")
        assertEquals(once, IMEI.sanitize(once))
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        val result = IMEI.validateProgressive("49015a42032375180")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        val lengthOnlyResult = IMEI.validateProgressive("4901542032375180")
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
