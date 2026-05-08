// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [PAN].
 *
 * Covers the 9-test template from Anika's Phase 2 test strategy §1.3.
 * Tests T1-T9 per spec. Each test is independent and uses static vectors.
 */
class PANProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, PAN.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, PAN.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, PAN.validateProgressive("\t"))
    }

    // T2 — partial input (right char class, length < maxLength) returns Typing with correct visualText
    // PAN visualText = uppercase as-is (no spacing)
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        // Single char (lowercase — normalised to uppercase in visualText)
        val r1 = PAN.validateProgressive("a")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("A", r1.visualText)

        // 5 chars mixed case
        val r5 = PAN.validateProgressive("abcPe")
        assertIs<ProgressiveResult.Typing>(r5)
        assertEquals("ABCPE", r5.visualText)

        // maxLength - 1 = 9 chars
        val r9 = PAN.validateProgressive("abcpe1234")
        assertIs<ProgressiveResult.Typing>(r9)
        assertEquals("ABCPE1234", r9.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        // "ABCPE1234F" is a valid PAN (known-valid from PANTest.kt)
        assertEquals(ProgressiveResult.Valid, PAN.validateProgressive("ABCPE1234F"))
        // lowercase version also valid
        assertEquals(ProgressiveResult.Valid, PAN.validateProgressive("abcpe1234f"))
    }

    // T4 — complete input with wrong format (non-checksum validator) returns Invalid(INVALID_FORMAT)
    // PAN has no checksum — test post-maxLength structural failure instead
    @Test
    fun t4_invalidFormatAtFullLengthReturnsInvalid() {
        // 10 chars but wrong pattern: starts with digit — fails PAN regex
        val result = PAN.validateProgressive("1BCPE1234F")
        assertIs<ProgressiveResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    // T5 — non-allowed char (space in middle, Devanagari) returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        // Space in middle (survives trim, becomes non-allowed char)
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            PAN.validateProgressive("ABC E1234"),
        )
        // Devanagari char
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            PAN.validateProgressive("ABC१E"),
        )
        // Special char @
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            PAN.validateProgressive("ABC@E"),
        )
    }

    // T6 — over maxLength (all allowed chars) returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        // 11 clean alphanumeric chars
        val input = "ABCPE1234FA"
        val result = PAN.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 10, actual = 11),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars are accessible and correct
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = PAN.allowedChars // compile-time type check
        assertEquals(10, PAN.maxLength)
        assertTrue('A' in PAN.allowedChars)
        assertTrue('Z' in PAN.allowedChars)
        assertTrue('a' in PAN.allowedChars)
        assertTrue('z' in PAN.allowedChars)
        assertTrue('0' in PAN.allowedChars)
        assertTrue('9' in PAN.allowedChars)
        assertFalse(' ' in PAN.allowedChars)
        assertFalse('@' in PAN.allowedChars)
        assertFalse('-' in PAN.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        // Spaces stripped (space not in allowedChars)
        assertEquals("ABCPE1234F", PAN.sanitize("ABCPE 1234 F"))
        // Special chars stripped, capped at 10
        assertEquals("ABCPE1234F", PAN.sanitize("ABCPE@1234#FXYZ"))
        // All non-allowed → ""
        assertEquals("", PAN.sanitize(" - @ ! "))
        // Empty stays empty
        assertEquals("", PAN.sanitize(""))
        // Idempotency
        val once = PAN.sanitize("ABCPE@1234#FXYZ extra")
        val twice = PAN.sanitize(once)
        assertEquals(once, twice)
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT (not WRONG_LENGTH)
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        // "ABCPE1234F@XYZ" = 14 chars, contains '@' (non-allowed)
        val result = PAN.validateProgressive("ABCPE1234F@XYZ")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        // Contrast: length-only (no bad char) returns WRONG_LENGTH
        val lengthOnlyResult = PAN.validateProgressive("ABCPE1234FA") // 11 clean alphanumeric
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
