// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [IFSC].
 *
 * Covers the 9-test template from Anika's Phase 2 test strategy §1.3.
 * Tests T1-T9 per spec. Each test is independent and uses static vectors.
 */
class IFSCProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, IFSC.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, IFSC.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, IFSC.validateProgressive("\t"))
    }

    // T2 — partial input (right char class, length < maxLength) returns Typing with correct visualText
    // IFSC visualText = uppercase as-is (no spacing)
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        // Single char (lowercase)
        val r1 = IFSC.validateProgressive("h")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("H", r1.visualText)

        // 4 chars
        val r4 = IFSC.validateProgressive("hdfc")
        assertIs<ProgressiveResult.Typing>(r4)
        assertEquals("HDFC", r4.visualText)

        // maxLength - 1 = 10 chars
        val r10 = IFSC.validateProgressive("hdfc000000")
        assertIs<ProgressiveResult.Typing>(r10)
        assertEquals("HDFC000000", r10.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        assertEquals(ProgressiveResult.Valid, IFSC.validateProgressive("HDFC0000001"))
        assertEquals(ProgressiveResult.Valid, IFSC.validateProgressive("hdfc0000001"))
    }

    // T4 — complete input with wrong format returns Invalid(INVALID_FORMAT)
    // IFSC has no checksum — 5th char must be '0'
    @Test
    fun t4_invalidFormatAtFullLengthReturnsInvalid() {
        // 5th char is '1' not '0' — fails IFSC regex
        val result = IFSC.validateProgressive("HDFC1000001")
        assertIs<ProgressiveResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    // T5 — non-allowed char returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        // Space in middle
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            IFSC.validateProgressive("HDFC 00"),
        )
        // Devanagari
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            IFSC.validateProgressive("HDFC२"),
        )
        // Hyphen
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            IFSC.validateProgressive("HDFC-0"),
        )
    }

    // T6 — over maxLength (all allowed chars) returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        // 12 clean alphanumeric chars
        val input = "HDFC0000001A"
        val result = IFSC.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 11, actual = 12),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars are accessible and correct
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = IFSC.allowedChars // compile-time type check
        assertEquals(11, IFSC.maxLength)
        assertTrue('A' in IFSC.allowedChars)
        assertTrue('Z' in IFSC.allowedChars)
        assertTrue('a' in IFSC.allowedChars)
        assertTrue('z' in IFSC.allowedChars)
        assertTrue('0' in IFSC.allowedChars)
        assertTrue('9' in IFSC.allowedChars)
        assertFalse(' ' in IFSC.allowedChars)
        assertFalse('-' in IFSC.allowedChars)
        assertFalse('@' in IFSC.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        // Spaces stripped
        assertEquals("HDFC0000001", IFSC.sanitize("HDFC 0000 001"))
        // Special chars stripped, capped at 11
        assertEquals("HDFC0000001", IFSC.sanitize("HDFC@0000#001!EXTRA"))
        // All non-allowed → ""
        assertEquals("", IFSC.sanitize(" - @ !"))
        // Empty stays empty
        assertEquals("", IFSC.sanitize(""))
        // Idempotency
        val once = IFSC.sanitize("HDFC@0000#001!EXTRA")
        val twice = IFSC.sanitize(once)
        assertEquals(once, twice)
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT (not WRONG_LENGTH)
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        // "HDFC0000001@XYZ" = 15 chars, contains '@' (non-allowed)
        val result = IFSC.validateProgressive("HDFC0000001@XYZ")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        // Contrast: length-only (no bad char) returns WRONG_LENGTH
        val lengthOnlyResult = IFSC.validateProgressive("HDFC0000001A") // 12 clean alphanumeric
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
