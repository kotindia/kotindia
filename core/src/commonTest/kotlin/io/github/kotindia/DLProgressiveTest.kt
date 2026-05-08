// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Progressive validation unit tests for [DL].
 *
 * DL maxLength = 16 (input cap — one beyond the largest valid DL length).
 * DL valid normalized length range: 14 (1-digit RTO) or 15 (2-digit RTO).
 * DL formatPartial: uppercase as-is (no space grouping).
 *
 * State machine: partial = length < MIN_LENGTH (14), complete/over-cap = length >= MIN_LENGTH.
 * A 14-char or 15-char valid DL returns Valid directly (Step 6 delegates to validate()).
 * A 16-char all-alphanumeric input hits Step 6, validate() returns WRONG_LENGTH → Invalid.
 */
class DLProgressiveTest {
    // T1 — empty/whitespace returns Empty
    @Test
    fun t1_emptyInputReturnsEmpty() {
        assertEquals(ProgressiveResult.Empty, DL.validateProgressive(""))
        assertEquals(ProgressiveResult.Empty, DL.validateProgressive("   "))
        assertEquals(ProgressiveResult.Empty, DL.validateProgressive("\t"))
    }

    // T2 — partial input (< MIN_LENGTH = 14) returns Typing with uppercase visualText
    @Test
    fun t2_partialInputReturnsTypingWithVisualText() {
        val r1 = DL.validateProgressive("m")
        assertIs<ProgressiveResult.Typing>(r1)
        assertEquals("M", r1.visualText)

        val r8 = DL.validateProgressive("mh122011")
        assertIs<ProgressiveResult.Typing>(r8)
        assertEquals("MH122011", r8.visualText)

        // 13 chars is still partial (< MIN_LENGTH = 14)
        val r13 = DL.validateProgressive("MH12201100123")
        assertIs<ProgressiveResult.Typing>(r13)
        assertEquals("MH12201100123", r13.visualText)
    }

    // T3 — complete valid input returns Valid (both valid lengths: 14 and 15)
    // DL.validateProgressive at length >= MIN_LENGTH(14) delegates to validate().
    // A structurally valid 14-char DL → ProgressiveResult.Valid.
    // A structurally valid 15-char DL → ProgressiveResult.Valid.
    @Test
    fun t3_completeValidDlReturnsValid() {
        // 14-char DL: state(2) + RTO(1 digit) + year+serial(11) = "MH" + "1" + "20110012345"
        val result14 = DL.validateProgressive("MH120110012345")
        assertEquals(ProgressiveResult.Valid, result14)

        // 15-char DL: state(2) + RTO(2 digits) + year+serial(11) = "MH" + "12" + "20110012345"
        val result15 = DL.validateProgressive("MH1220110012345")
        assertEquals(ProgressiveResult.Valid, result15)
    }

    // T4 — complete input with wrong format returns Invalid
    @Test
    fun t4_invalidFormatAtFullLengthReturnsInvalid() {
        // Invalid state code "ZZ" — 15-char input returns Typing (< maxLength=16),
        // so use a 16-char input to hit Step 6
        val result = DL.validateProgressive("ZZ0120230012345X")
        // This is 16 chars — hits Step 6 → delegates to validate
        // validate will return WRONG_LENGTH or INVALID_FORMAT or INVALID_PREFIX
        assertIs<ProgressiveResult.Invalid>(result)
    }

    // T5 — non-allowed char returns Invalid(INVALID_FORMAT, None)
    @Test
    fun t5_nonAllowedCharReturnsInvalidFormat() {
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            DL.validateProgressive("MH 01"),
        )
        // Hyphen (not in allowedChars)
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            DL.validateProgressive("MH-01"),
        )
        // Devanagari
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            DL.validateProgressive("MH१2"),
        )
    }

    // T6 — over maxLength returns Invalid(WRONG_LENGTH, LengthMismatch)
    @Test
    fun t6_beyondMaxLengthReturnsInvalidWithLengthMismatch() {
        val input = "MH0120230012345XY" // 17 alphanumeric chars
        val result = DL.validateProgressive(input)
        assertEquals(
            ProgressiveResult.Invalid(
                InvalidReason.WRONG_LENGTH,
                ValidationContext.LengthMismatch(expected = 16, actual = 17),
            ),
            result,
        )
    }

    // T7 — maxLength and allowedChars accessible
    @Test
    fun t7_maxLengthAndAllowedCharsExposed() {
        @Suppress("UNUSED_VARIABLE")
        val allowedCharsTyped: Set<Char> = DL.allowedChars
        assertEquals(16, DL.maxLength)
        assertTrue('A' in DL.allowedChars)
        assertTrue('a' in DL.allowedChars)
        assertTrue('0' in DL.allowedChars)
        assertFalse(' ' in DL.allowedChars)
        assertFalse('-' in DL.allowedChars)
    }

    // T8 — sanitize strips non-allowed chars and caps at maxLength
    @Test
    fun t8_sanitizeStripsAndCaps() {
        // Hyphens stripped (not in allowedChars)
        assertEquals("MH1220110012345", DL.sanitize("MH12-20110012345"))
        // Spaces stripped, capped at 16
        assertEquals("MH0120230012345X", DL.sanitize("MH01 20230012345XY"))
        assertEquals("", DL.sanitize(" - !"))
        assertEquals("", DL.sanitize(""))
        val once = DL.sanitize("MH12-20110012345extra")
        assertEquals(once, DL.sanitize(once))
    }

    // T9 — mixed non-allowed char + over maxLength → INVALID_FORMAT
    @Test
    fun t9_mixedBadCharAndOverMaxLengthReturnsInvalidFormat() {
        val result = DL.validateProgressive("MH12 20110012345XY")
        assertEquals(
            ProgressiveResult.Invalid(InvalidReason.INVALID_FORMAT, ValidationContext.None),
            result,
        )
        val lengthOnlyResult = DL.validateProgressive("MH0120230012345XY")
        assertIs<ProgressiveResult.Invalid>(lengthOnlyResult)
        assertEquals(InvalidReason.WRONG_LENGTH, lengthOnlyResult.reason)
    }

    // T10 — complete valid DL at both valid lengths returns Valid (B1 regression guard)
    // Verifies that the dual valid-length range (14 or 15) is correctly handled:
    // Step 5 now uses MIN_LENGTH (14) as the partial threshold, so 14-char and 15-char
    // structurally-valid DLs both delegate to validate() and return ProgressiveResult.Valid.
    @Test
    fun t10_completeValidDLAtBothLengths_returnsValid() {
        // 14-char DL: MH + 1-digit RTO + 11-digit year+serial
        // "MH120110012345" = MH(2) + 1(1) + 20110012345(11) = 14 chars
        assertEquals(ProgressiveResult.Valid, DL.validateProgressive("MH120110012345"))
        // 15-char DL: MH + 2-digit RTO + 11-digit year+serial
        // "MH1220110012345" = MH(2) + 12(2) + 20110012345(11) = 15 chars
        assertEquals(ProgressiveResult.Valid, DL.validateProgressive("MH1220110012345"))
    }

    // T11 — 16-char all-alphanumeric input (at maxLength cap) returns WRONG_LENGTH
    // maxLength=16 is the input cap (one beyond the valid range). A 16-char sanitized input
    // reaches Step 6 → validate() → WRONG_LENGTH (valid length is 14–15, not 16).
    @Test
    fun t11_overMaxLengthDL_returnsWrongLength() {
        // 16 alphanumeric chars: valid state + valid structure but wrong length
        val result = DL.validateProgressive("MH1220110012345X")
        assertIs<ProgressiveResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    // T12 — 15-char structurally-valid DL with invalid state code returns INVALID_PREFIX
    // "ZZ" passes DL_REGEX (two uppercase letters) and length (15), but is not in VALID_STATE_CODES.
    // validate() returns INVALID_PREFIX. validateProgressive at length 15 (== MAX_LENGTH = 15)
    // delegates to validate() at Step 6 → returns Invalid(INVALID_PREFIX).
    @Test
    fun t12_invalidStateCodeAt15CharsReturnsInvalidPrefix() {
        // "ZZ1220110012345" = ZZ(2) + 12(2-digit RTO) + 20110012345(11 digits) = 15 chars
        // "ZZ" not in VALID_STATE_CODES → validate() returns INVALID_PREFIX
        val result = DL.validateProgressive("ZZ1220110012345")
        assertIs<ProgressiveResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }
}
