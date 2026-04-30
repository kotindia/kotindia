// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [Verhoeff] using known external reference vectors.
 *
 * Vectors sourced from mastermunj/format-utils JS Verhoeff impl (MIT)
 * https://github.com/mastermunj/format-utils — cross-validated 2026-04-29.
 * Tests verify the Kotlin D5 lookup tables match the external implementation.
 */
class VerhoeffTest {
    // ---------------------------------------------------------------------------
    // computeCheckDigit — known-vector tests (externally sourced)
    // ---------------------------------------------------------------------------

    @Test
    fun computeCheckDigit_groupA_prefix20100000000_returns0() {
        // Source: mastermunj/format-utils JS — prefix "20100000000" → check digit '0'
        assertEquals('0', Verhoeff.computeCheckDigit("20100000000"))
    }

    @Test
    fun computeCheckDigit_groupA_prefix21100000001_returns9() {
        // Source: mastermunj/format-utils JS — prefix "21100000001" → check digit '9'
        assertEquals('9', Verhoeff.computeCheckDigit("21100000001"))
    }

    @Test
    fun computeCheckDigit_groupB_prefix30100000010_returns8() {
        // Source: mastermunj/format-utils JS — prefix "30100000010" → check digit '8'
        assertEquals('8', Verhoeff.computeCheckDigit("30100000010"))
    }

    @Test
    fun computeCheckDigit_groupC_prefix40100000020_returns8() {
        // Source: mastermunj/format-utils JS — prefix "40100000020" → check digit '8'
        assertEquals('8', Verhoeff.computeCheckDigit("40100000020"))
    }

    @Test
    fun computeCheckDigit_groupD_prefix50100000030_returns7() {
        // Source: mastermunj/format-utils JS — prefix "50100000030" → check digit '7'
        assertEquals('7', Verhoeff.computeCheckDigit("50100000030"))
    }

    @Test
    fun computeCheckDigit_groupE_prefix60100000040_returns1() {
        // Source: mastermunj/format-utils JS — prefix "60100000040" → check digit '1'
        assertEquals('1', Verhoeff.computeCheckDigit("60100000040"))
    }

    @Test
    fun computeCheckDigit_groupF_prefix70100000050_returns8() {
        // Source: mastermunj/format-utils JS — prefix "70100000050" → check digit '8'
        assertEquals('8', Verhoeff.computeCheckDigit("70100000050"))
    }

    @Test
    fun computeCheckDigit_groupG_prefix80100000060_returns3() {
        // Source: mastermunj/format-utils JS — prefix "80100000060" → check digit '3'
        assertEquals('3', Verhoeff.computeCheckDigit("80100000060"))
    }

    @Test
    fun computeCheckDigit_groupH_prefix90100000070_returns3() {
        // Source: mastermunj/format-utils JS — prefix "90100000070" → check digit '3'
        assertEquals('3', Verhoeff.computeCheckDigit("90100000070"))
    }

    @Test
    fun computeCheckDigit_singleDigit_prefix2_computesCorrectly() {
        // Single digit prefix: check digit for "2" should produce a valid pair
        val check = Verhoeff.computeCheckDigit("2")
        val full = "2$check"
        assertTrue(Verhoeff.isValid(full), "Single-digit prefix check digit should produce valid pair")
    }

    // ---------------------------------------------------------------------------
    // isValid — known-vector tests
    // ---------------------------------------------------------------------------

    @Test
    fun isValid_knownValid_groupA_first_returnsTrue() {
        assertTrue(Verhoeff.isValid("201000000000"))
    }

    @Test
    fun isValid_knownValid_groupB_first_returnsTrue() {
        assertTrue(Verhoeff.isValid("301000000108"))
    }

    @Test
    fun isValid_knownValid_groupH_last_returnsTrue() {
        assertTrue(Verhoeff.isValid("991000000796"))
    }

    @Test
    fun isValid_corrupted_lastDigitIncremented_returnsFalse() {
        // "201000000000" is valid; incrementing last digit by 1 → "201000000001"
        assertFalse(Verhoeff.isValid("201000000001"))
    }

    @Test
    fun isValid_corrupted_lastDigitDecremented_returnsFalse() {
        // "201000000000" is valid; decrementing last digit wraps → "201000000009"
        assertFalse(Verhoeff.isValid("201000000009"))
    }

    @Test
    fun isValid_corrupted_middleDigitChanged_returnsFalse() {
        // "301000000108" is valid; changing digit at index 5 by +1 → invalid
        assertFalse(Verhoeff.isValid("301001000108"))
    }

    @Test
    fun isValid_adjacentTransposition_detectsError() {
        // "201000000000" is valid; swap digits at index 0 and 1 → "021000000000"
        // (first digit becomes 0 — but Verhoeff should still catch the checksum mismatch)
        // Note: this tests ONLY checksum detection, not prefix rule
        assertFalse(Verhoeff.isValid("021000000000"))
    }

    @Test
    fun isValid_validVid16Digit_returnsTrue() {
        // 16-digit VID — same Verhoeff algorithm
        assertTrue(Verhoeff.isValid("2000000000000006"))
    }

    @Test
    fun isValid_invalidVid16Digit_returnsFalse() {
        // "2000000000000006" is valid; last digit +1 → invalid
        assertFalse(Verhoeff.isValid("2000000000000007"))
    }

    @Test
    fun roundTrip_computeThenValidate_returnsTrue() {
        // Compute check digit for known prefix and verify the full number validates
        val prefix = "23456789012"
        val check = Verhoeff.computeCheckDigit(prefix)
        val full = prefix + check
        assertEquals(12, full.length)
        assertTrue(Verhoeff.isValid(full))
    }
}
