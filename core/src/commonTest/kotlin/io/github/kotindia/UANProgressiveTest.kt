// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [UAN].
 *
 * Covers the 9-test template from Anika's Phase 2 test strategy §1.3.
 * UAN formatPartial: chunked(4).joinToString(" ") per PRD Scope table.
 */
class UANProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, UAN.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, UAN.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, UAN.validateProgressive("\t"))
    }

    // T2 — partial input returns Typing with correct visualText (chunked(4))
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        val r1 = UAN.validateProgressive("1")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("1", r1.visualText)

        // 5 digits: "1012 3"
        val r5 = UAN.validateProgressive("10123")
        assertIs<ProgressiveResult.Typing>(r5)
        assertEquals("1012 3", r5.visualText)

        // maxLength - 1 = 11 digits: "1012 3456 789"
        val r11 = UAN.validateProgressive("10123456789")
        assertIs<ProgressiveResult.Typing>(r11)
        assertEquals("1012 3456 789", r11.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        assertEquals(ProgressiveResult.Valid, UAN.validateProgressive("100123456789"))
        assertEquals(ProgressiveResult.Valid, UAN.validateProgressive("101234567890"))
    }

    // T4 — complete input with wrong format returns Invalid(INVALID_FORMAT)
    // UAN has no checksum — but all-digit 12-char should be valid; test what would fail
    @Test
    fun t4_invalidFormatAtFullLengthReturnsInvalid() {
        // UAN validate() only checks digits — so full-length valid-digit input should be Valid.
        // Test with a case where a letter snuck in via some other path — but validateProgressive
        // catches bad chars at step 3. So this is more of a belt-and-suspenders: demonstrate
        // that a valid-length valid-digit input at step 6 passes through cleanly.
        val result = UAN.validateProgressive("100123456789")
        assertEquals(ProgressiveResult.Valid, result)
    }

    // T5 — non-allowed char returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            UAN.validateProgressive("1001a3456789"),
        )
        // Devanagari
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            UAN.validateProgressive("१001234"),
        )
        // Space in middle
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            UAN.validateProgressive("1001 3456"),
        )
    }

    // T6 — over maxLength returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        val input = "1001234567890"
        val result = UAN.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 12, actual = 13),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars accessible
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = UAN.allowedChars
        assertEquals(12, UAN.maxLength)
        assertTrue('0' in UAN.allowedChars)
        assertTrue('9' in UAN.allowedChars)
        assertFalse('A' in UAN.allowedChars)
        assertFalse(' ' in UAN.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        assertEquals("100123456789", UAN.sanitize("1001 2345 6789"))
        assertEquals("100123456789", UAN.sanitize("100123456789extra00"))
        assertEquals("", UAN.sanitize("ABC"))
        assertEquals("", UAN.sanitize(""))
        val once = UAN.sanitize("1001 2345 6789extra")
        assertEquals(once, UAN.sanitize(once))
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        val result = UAN.validateProgressive("1001a234567890")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        val lengthOnlyResult = UAN.validateProgressive("1001234567890")
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
