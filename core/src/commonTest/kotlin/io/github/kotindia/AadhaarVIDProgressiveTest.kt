// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [AadhaarVID].
 *
 * Covers the 9-test template from Anika's Phase 2 test strategy §1.3.
 * Tests T1-T9 per spec. Each test is independent and uses static vectors.
 */
class AadhaarVIDProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, AadhaarVID.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, AadhaarVID.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, AadhaarVID.validateProgressive("\t"))
    }

    // T2 — partial input (right char class, length < maxLength) returns Typing with correct visualText
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        // Single digit
        val r1 = AadhaarVID.validateProgressive("2")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("2", r1.visualText)

        // 9 digits — chunked(4) produces "1234 5678 9"
        val r9 = AadhaarVID.validateProgressive("123456789")
        assertIs<ProgressiveResult.Typing>(r9)
        assertEquals("1234 5678 9", r9.visualText)

        // 13 digits — chunked(4) produces "1234 5678 9012 3"
        val r13 = AadhaarVID.validateProgressive("2010000000001")
        assertIs<ProgressiveResult.Typing>(r13)
        assertEquals("2010 0000 0000 1", r13.visualText)

        // maxLength - 1 = 15 digits
        val r15 = AadhaarVID.validateProgressive("201000000000123")
        assertIs<ProgressiveResult.Typing>(r15)
        assertEquals("2010 0000 0000 123", r15.visualText)
    }

    // T3 — complete valid input returns Valid
    // Hard-asserts against a known-Verhoeff-valid VID from AadhaarReferenceVectors.KNOWN_VALID_VIDS.
    // No conditional fallback — if this fails the vector table has regressed.
    @Test
    fun t3_completeValidInputReturnsValid() {
        // Sourced from AadhaarReferenceVectors.KNOWN_VALID_VIDS[0] = "2000000000000006"
        // Cross-validated against mastermunj/format-utils JS Verhoeff impl (MIT).
        val knownValid = AadhaarReferenceVectors.KNOWN_VALID_VIDS[0]
        assertEquals(ProgressiveResult.Valid, AadhaarVID.validateProgressive(knownValid))
    }

    // T4 — complete input with invalid checksum returns Invalid(INVALID_CHECKSUM, None)
    // Hard-asserts on a deterministic corruption of a known-valid VID — no conditional fallback.
    @Test
    fun t4_invalidChecksumReturnsInvalid() {
        // Take KNOWN_VALID_VIDS[0] = "2000000000000006" and flip last digit by +1 mod 10.
        // Verhoeff's property guarantees any single-digit change to the check digit invalidates the
        // checksum — this specific corruption "2000000000000007" is rejected by our Kotlin impl
        // and independently by the mastermunj/format-utils JS Verhoeff impl (cross-verified).
        val knownValid = AadhaarReferenceVectors.KNOWN_VALID_VIDS[0] // "2000000000000006"
        val corruptedLastDigit = ((knownValid.last().digitToInt() + 1) % 10).toString()
        val corrupted = knownValid.dropLast(1) + corruptedLastDigit // "2000000000000007"

        // Sanity: validate() must reject the corrupted VID. If this fails the Verhoeff impl regressed.
        require(AadhaarVID.validate(corrupted) is ValidationResult.Invalid) {
            "AadhaarReferenceVectors regression: corrupted VID '$corrupted' was unexpectedly valid."
        }

        // Hard-assert: validateProgressive returns Invalid(INVALID_CHECKSUM, None).
        val result = AadhaarVID.validateProgressive(corrupted)
        assertIs<ProgressiveResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_CHECKSUM, result.reason)
    }

    // T5 — non-allowed char (ASCII + Devanagari) returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        // ASCII non-digit
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            AadhaarVID.validateProgressive("1234a567890"),
        )
        // Devanagari digit ० (U+0966)
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            AadhaarVID.validateProgressive("२345678"),
        )
        // Space in middle (not leading/trailing — survives trim)
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            AadhaarVID.validateProgressive("1234 5678"),
        )
    }

    // T6 — over maxLength (all allowed chars) returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        // 17 clean digits
        val input = "20100000000001234"
        val result = AadhaarVID.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 16, actual = 17),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars are accessible and correct
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = AadhaarVID.allowedChars // compile-time type check
        assertEquals(16, AadhaarVID.maxLength)
        assertTrue('0' in AadhaarVID.allowedChars)
        assertTrue('9' in AadhaarVID.allowedChars)
        assertTrue('5' in AadhaarVID.allowedChars)
        assertFalse('A' in AadhaarVID.allowedChars)
        assertFalse('a' in AadhaarVID.allowedChars)
        assertFalse(' ' in AadhaarVID.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        // Spaces stripped
        assertEquals("2010000000001234", AadhaarVID.sanitize("2010 0000 0000 1234"))
        // Non-digit stripped, capped at 16
        assertEquals("1234567890123456", AadhaarVID.sanitize("1234abcd5678901234567"))
        // All non-allowed → ""
        assertEquals("", AadhaarVID.sanitize("abc"))
        // Empty stays empty
        assertEquals("", AadhaarVID.sanitize(""))
        // Idempotency
        val once = AadhaarVID.sanitize("2010 0000 0000 1234 extra 999")
        val twice = AadhaarVID.sanitize(once)
        assertEquals(once, twice)
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT (not WRONG_LENGTH)
    // Verifies AC4 evaluation order: priority-2 (non-allowed char) fires before priority-3 (length)
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        // "1234a56789012345678" = 19 chars, contains 'a' (non-allowed)
        val result = AadhaarVID.validateProgressive("1234a56789012345678")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        // Contrast: length-only (no bad char) returns WRONG_LENGTH
        val lengthOnlyResult = AadhaarVID.validateProgressive("20100000000012345") // 17 clean digits
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }
}
