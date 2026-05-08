// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [Mobile].
 *
 * Covers the 9-test template from Anika's Phase 2 test strategy §1.3.
 * Tests T1-T9 per spec. Each test is independent and uses static vectors.
 *
 * Mobile formatPartial: chunked(5).joinToString(" ") per OQ-14-2 Marcus ruling.
 * No +91 prefix in visualText.
 */
class MobileProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, Mobile.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, Mobile.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, Mobile.validateProgressive("\t"))
    }

    // T2 — partial input (right char class, length < maxLength) returns Typing with correct visualText
    // Mobile formatPartial: chunked(5) → "98765 4321" style (no +91)
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        // 5 digits — no space yet (single chunk)
        val r5 = Mobile.validateProgressive("98765")
        assertIs<ProgressiveResult.Typing>(r5)
        assertEquals("98765", r5.visualText)

        // 6 digits — "98765 4" (two chunks: "98765" and "4")
        val r6 = Mobile.validateProgressive("987654")
        assertIs<ProgressiveResult.Typing>(r6)
        assertEquals("98765 4", r6.visualText)

        // maxLength - 1 = 9 digits — "98765 4321"
        val r9 = Mobile.validateProgressive("987654321")
        assertIs<ProgressiveResult.Typing>(r9)
        assertEquals("98765 4321", r9.visualText)
    }

    // T3 — complete valid input returns Valid
    @Test
    fun t3_completeValidInputReturnsValid() {
        assertEquals(ProgressiveResult.Valid, Mobile.validateProgressive("9876543210"))
        assertEquals(ProgressiveResult.Valid, Mobile.validateProgressive("6001234567"))
    }

    // T4 — complete input with wrong format (invalid prefix) returns Invalid(INVALID_PREFIX)
    // Mobile has no checksum — test prefix failure at full length
    @Test
    fun t4_invalidPrefixAtFullLengthReturnsInvalid() {
        // Starts with 1 — invalid Mobile prefix
        val result = Mobile.validateProgressive("1234567890")
        assertIs<ProgressiveResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    // T5 — non-allowed char (letter, Devanagari) returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        // Letter in middle
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            Mobile.validateProgressive("9876a3210"),
        )
        // Devanagari digit
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            Mobile.validateProgressive("9876२3210"),
        )
        // Space in middle (survives trim, treated as non-allowed char)
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            Mobile.validateProgressive("98765 4321"),
        )
    }

    // T6 — over maxLength (all allowed chars) returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        // 11 clean digits
        val input = "98765432100"
        val result = Mobile.validateProgressive(input)
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
        val allowedCharsTyped: Set<Char> = Mobile.allowedChars // compile-time type check
        assertEquals(10, Mobile.maxLength)
        assertTrue('0' in Mobile.allowedChars)
        assertTrue('9' in Mobile.allowedChars)
        assertFalse('A' in Mobile.allowedChars)
        assertFalse(' ' in Mobile.allowedChars)
        assertFalse('+' in Mobile.allowedChars)
        assertFalse('-' in Mobile.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        // Letters stripped, digit count capped at 10
        assertEquals("9876543210", Mobile.sanitize("9876a543210"))
        // sanitize does NOT strip +91 prefix — it strips non-digits then caps at 10.
        // "+919876543210" → strip "+" → "919876543210" (12 digits) → take(10) → "9198765432"
        assertEquals("9198765432", Mobile.sanitize("+919876543210"))
        // All non-allowed → ""
        assertEquals("", Mobile.sanitize("abcXYZ"))
        // Empty stays empty
        assertEquals("", Mobile.sanitize(""))
        // Idempotency
        val once = Mobile.sanitize("9876543210 extra 999")
        val twice = Mobile.sanitize(once)
        assertEquals(once, twice)
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT (not WRONG_LENGTH)
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        // "9876a54321000" = 13 chars, contains 'a' (non-allowed)
        val result = Mobile.validateProgressive("9876a54321000")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        // Contrast: length-only (no bad char) returns WRONG_LENGTH
        val lengthOnlyResult = Mobile.validateProgressive("98765432100") // 11 clean digits
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
