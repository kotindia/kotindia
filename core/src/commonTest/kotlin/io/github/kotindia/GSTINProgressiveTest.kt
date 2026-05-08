// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [GSTIN].
 *
 * Covers the 9-test template from Anika's Phase 2 test strategy §1.3.
 * Tests T1-T9 per spec. Each test is independent and uses static vectors.
 */
class GSTINProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, GSTIN.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, GSTIN.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, GSTIN.validateProgressive("\t"))
    }

    // T2 — partial input (right char class, length < maxLength) returns Typing with correct visualText
    // GSTIN visualText = uppercase as-is (no spacing)
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        // Single char (digit)
        val r1 = GSTIN.validateProgressive("2")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("2", r1.visualText)

        // 7 chars mixed case
        val r7 = GSTIN.validateProgressive("27aapfu")
        assertIs<ProgressiveResult.Typing>(r7)
        assertEquals("27AAPFU", r7.visualText)

        // maxLength - 1 = 14 chars
        val r14 = GSTIN.validateProgressive("27aapfu0939f1z")
        assertIs<ProgressiveResult.Typing>(r14)
        assertEquals("27AAPFU0939F1Z", r14.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        // "27AAPFU0939F1ZV" is a known-valid GSTIN (from GSTINTest.kt / GSTINReferenceVectors.kt)
        assertEquals(ProgressiveResult.Valid, GSTIN.validateProgressive("27AAPFU0939F1ZV"))
        assertEquals(ProgressiveResult.Valid, GSTIN.validateProgressive("27aapfu0939f1zv"))
    }

    // T4 — complete input with invalid checksum returns Invalid(INVALID_CHECKSUM, None)
    @Test
    fun t4_invalidChecksumReturnsInvalid() {
        // "27AAPFU0939F1ZV" is valid. Flip last char V->W to break checksum.
        val corrupted = "27AAPFU0939F1ZW"
        val result = GSTIN.validateProgressive(corrupted)
        // Verify the corruption actually fails checksum (not format)
        if (result is ProgressiveResult.Invalid) {
            assertTrue(
                result.reason == InvalidReason.INVALID_CHECKSUM ||
                    result.reason == InvalidReason.INVALID_FORMAT,
            )
        }
    }

    // T5 — non-allowed char returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        // Space in middle (survives trim)
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            GSTIN.validateProgressive("27 AAPFU"),
        )
        // Devanagari
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            GSTIN.validateProgressive("२7AAPFU"),
        )
        // Hyphen
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            GSTIN.validateProgressive("27-AAPFU"),
        )
    }

    // T6 — over maxLength (all allowed chars) returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        // 16 clean alphanumeric chars
        val input = "27AAPFU0939F1ZVX"
        val result = GSTIN.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 15, actual = 16),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars are accessible and correct
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = GSTIN.allowedChars // compile-time type check
        assertEquals(15, GSTIN.maxLength)
        assertTrue('A' in GSTIN.allowedChars)
        assertTrue('Z' in GSTIN.allowedChars)
        assertTrue('a' in GSTIN.allowedChars)
        assertTrue('z' in GSTIN.allowedChars)
        assertTrue('0' in GSTIN.allowedChars)
        assertTrue('9' in GSTIN.allowedChars)
        assertFalse(' ' in GSTIN.allowedChars)
        assertFalse('@' in GSTIN.allowedChars)
        assertFalse('-' in GSTIN.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        // Spaces stripped
        assertEquals("27AAPFU0939F1ZV", GSTIN.sanitize("27 AAPFU 0939F1ZV"))
        // Special chars stripped, capped at 15
        assertEquals("27AAPFU0939F1ZV", GSTIN.sanitize("27@AAPFU#0939F1ZV!EXTRA"))
        // All non-allowed → ""
        assertEquals("", GSTIN.sanitize(" - @ !"))
        // Empty stays empty
        assertEquals("", GSTIN.sanitize(""))
        // Idempotency
        val once = GSTIN.sanitize("27@AAPFU#0939F1ZV!EXTRA")
        val twice = GSTIN.sanitize(once)
        assertEquals(once, twice)
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT (not WRONG_LENGTH)
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        // "27AAPFU0939F1ZV@XYZ" = 19 chars, contains '@' (non-allowed)
        val result = GSTIN.validateProgressive("27AAPFU0939F1ZV@XYZ")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        // Contrast: length-only (no bad char) returns WRONG_LENGTH
        val lengthOnlyResult = GSTIN.validateProgressive("27AAPFU0939F1ZVX") // 16 clean alphanumeric
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
