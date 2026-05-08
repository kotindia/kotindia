// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [VehicleRC].
 *
 * VehicleRC maxLength = 10 per PRD Scope table.
 * VehicleRC formatPartial: uppercase as-is.
 * Note: VehicleRC.validate() accepts normalized length 8-11. maxLength=10.
 * Valid RCs of length 8-10 will trigger Steps 5 or 6 correctly.
 */
class VehicleRCProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, VehicleRC.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, VehicleRC.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, VehicleRC.validateProgressive("\t"))
    }

    // T2 — partial input returns Typing with uppercase visualText
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        val r1 = VehicleRC.validateProgressive("m")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("M", r1.visualText)

        val r5 = VehicleRC.validateProgressive("mh12a")
        assertIs<ProgressiveResult.Typing>(r5)
        assertEquals("MH12A", r5.visualText)

        // maxLength - 1 = 9 chars
        val r9 = VehicleRC.validateProgressive("mh01ab123")
        assertIs<ProgressiveResult.Typing>(r9)
        assertEquals("MH01AB123", r9.visualText)
    }

    // T3 — complete valid input (10-char RC) returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        // "MH01AB1234" = 10 chars, valid RC
        assertEquals(ProgressiveResult.Valid, VehicleRC.validateProgressive("MH01AB1234"))
        assertEquals(ProgressiveResult.Valid, VehicleRC.validateProgressive("mh01ab1234"))
    }

    // T4 — complete input with wrong format returns Invalid
    @Test
    fun t4_invalidFormatAtFullLengthReturnsInvalid() {
        // Invalid state code "ZZ" — returns INVALID_PREFIX
        val result = VehicleRC.validateProgressive("ZZ01AB1234")
        assertIs<ProgressiveResult.Invalid>(result)
        assertTrue(result.reason == InvalidReason.INVALID_PREFIX || result.reason == InvalidReason.INVALID_FORMAT)
    }

    // T5 — non-allowed char returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            VehicleRC.validateProgressive("MH 01AB"),
        )
        // Hyphen
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            VehicleRC.validateProgressive("MH-01AB"),
        )
        // Devanagari
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            VehicleRC.validateProgressive("MH०1AB"),
        )
    }

    // T6 — over maxLength returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        val input = "MH01AB12345" // 11 alphanumeric chars
        val result = VehicleRC.validateProgressive(input)
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
        val allowedCharsTyped: Set<Char> = VehicleRC.allowedChars
        assertEquals(10, VehicleRC.maxLength)
        assertTrue('A' in VehicleRC.allowedChars)
        assertTrue('a' in VehicleRC.allowedChars)
        assertTrue('0' in VehicleRC.allowedChars)
        assertFalse(' ' in VehicleRC.allowedChars)
        assertFalse('-' in VehicleRC.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        // Hyphens stripped
        assertEquals("MH01AB1234", VehicleRC.sanitize("MH-01-AB-1234"))
        // Spaces stripped, capped at 10
        assertEquals("MH01AB1234", VehicleRC.sanitize("MH 01 AB 1234 XY"))
        assertEquals("", VehicleRC.sanitize(" - !"))
        assertEquals("", VehicleRC.sanitize(""))
        val once = VehicleRC.sanitize("MH-01-AB-1234-EXTRA")
        assertEquals(once, VehicleRC.sanitize(once))
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        val result = VehicleRC.validateProgressive("MH 01AB12345")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        val lengthOnlyResult = VehicleRC.validateProgressive("MH01AB12345")
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
