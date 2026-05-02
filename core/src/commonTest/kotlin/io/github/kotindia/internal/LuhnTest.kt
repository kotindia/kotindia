// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.internal

import io.github.kotindia.IMEIReferenceVectors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Direct unit tests for [Luhn] using externally-sourced known vectors.
 *
 * All expected values derived from:
 * - PRIMARY: Wikipedia Luhn algorithm — https://en.wikipedia.org/wiki/Luhn_algorithm
 *   (worked example: prefix "7992739871" → check digit 3 → "79927398713" valid)
 * - CROSS-CHECK: npm `luhn` package (MIT, https://www.npmjs.com/package/luhn)
 *   (luhn.validate() used as independent external verifier on all 110 generated IMEIs)
 *
 * Tests are deterministic — no property generation. Property-based tests are in [LuhnPropertyTest].
 */
class LuhnTest {
    // ---------------------------------------------------------------------------
    // computeCheckDigit — Wikipedia canonical worked example
    // ---------------------------------------------------------------------------

    @Test
    fun computeCheckDigit_wikipediaCanonicalExample_returns3() {
        // Wikipedia Luhn algorithm worked example:
        // prefix "7992739871" → check digit = 3 → full "79927398713"
        // https://en.wikipedia.org/wiki/Luhn_algorithm
        assertEquals(3, Luhn.computeCheckDigit("7992739871"))
    }

    @Test
    fun computeCheckDigit_allZeros14Digit_returnsExpected() {
        // Boundary: 14 all-zero prefix
        // "00000000000000" + computed check → must satisfy isChecksumValid
        val checkDigit = Luhn.computeCheckDigit("00000000000000")
        val full = "00000000000000" + checkDigit.digitToChar()
        assertTrue(Luhn.isChecksumValid(full), "All-zeros prefix: check=$checkDigit full=$full")
    }

    @Test
    fun computeCheckDigit_allNines14Digit_returnsExpected() {
        // Boundary: 14 all-nines prefix
        val checkDigit = Luhn.computeCheckDigit("99999999999999")
        val full = "99999999999999" + checkDigit.digitToChar()
        assertTrue(Luhn.isChecksumValid(full), "All-nines prefix: check=$checkDigit full=$full")
    }

    @Test
    fun computeCheckDigit_group_A_first_prefix() {
        // "001000000000000" (14 digits) → check digit 9 → "001000000000009"
        // Verified by npm luhn.validate("001000000000009") = true
        assertEquals(9, Luhn.computeCheckDigit("00100000000000"))
    }

    @Test
    fun computeCheckDigit_group_C_first_prefix() {
        // "201000000000000" (14 digits) → check digit 7 → "201000000000007"
        // Verified by npm luhn.validate("201000000000007") = true
        assertEquals(7, Luhn.computeCheckDigit("20100000000000"))
    }

    // ---------------------------------------------------------------------------
    // isChecksumValid — Wikipedia canonical examples
    // ---------------------------------------------------------------------------

    @Test
    fun isChecksumValid_wikipediaCanonical_returnsTrue() {
        // "79927398713" — Wikipedia Luhn algorithm worked example
        // https://en.wikipedia.org/wiki/Luhn_algorithm
        assertTrue(Luhn.isChecksumValid("79927398713"))
    }

    @Test
    fun isChecksumValid_wikipediaWrongCheckDigit0_returnsFalse() {
        // "79927398710" — wrong check digit (0 instead of 3)
        assertFalse(Luhn.isChecksumValid("79927398710"))
    }

    @Test
    fun isChecksumValid_wikipediaWrongCheckDigit1_returnsFalse() {
        // "79927398711" — wrong check digit (1 instead of 3)
        assertFalse(Luhn.isChecksumValid("79927398711"))
    }

    @Test
    fun isChecksumValid_wikipediaWrongCheckDigit2_returnsFalse() {
        // "79927398712" — wrong check digit (2 instead of 3)
        assertFalse(Luhn.isChecksumValid("79927398712"))
    }

    // ---------------------------------------------------------------------------
    // isChecksumValid — known-valid IMEI vectors (from IMEIReferenceVectors)
    // ---------------------------------------------------------------------------

    @Test
    fun isChecksumValid_knownValidImeis_first5_returnTrue() {
        // All verified by npm luhn.validate() during cross-validation (2026-05-02)
        assertTrue(Luhn.isChecksumValid(IMEIReferenceVectors.KNOWN_VALID_IMEIS[0])) // Group A
        assertTrue(Luhn.isChecksumValid(IMEIReferenceVectors.KNOWN_VALID_IMEIS[10])) // Group B
        assertTrue(Luhn.isChecksumValid(IMEIReferenceVectors.KNOWN_VALID_IMEIS[20])) // Group C
        assertTrue(Luhn.isChecksumValid(IMEIReferenceVectors.KNOWN_VALID_IMEIS[30])) // Group D
        assertTrue(Luhn.isChecksumValid(IMEIReferenceVectors.KNOWN_VALID_IMEIS[40])) // Group E
    }

    // ---------------------------------------------------------------------------
    // isChecksumValid — known-invalid IMEI vectors
    // ---------------------------------------------------------------------------

    @Test
    fun isChecksumValid_knownInvalidImeis_returnFalse() {
        // Both verified by npm luhn.validate() = false during cross-validation (2026-05-02)
        assertFalse(Luhn.isChecksumValid(IMEIReferenceVectors.KNOWN_INVALID_IMEIS[0]))
        assertFalse(Luhn.isChecksumValid(IMEIReferenceVectors.KNOWN_INVALID_IMEIS[1]))
    }

    // ---------------------------------------------------------------------------
    // Round-trip consistency: computeCheckDigit then isChecksumValid
    // ---------------------------------------------------------------------------

    @Test
    fun roundTrip_computeCheckDigitThenValidate_allGroupsPass() {
        // For each group's first prefix, verify round-trip consistency
        val prefixes =
            listOf(
                "00100000000000", // Group A
                "10100000000000", // Group B
                "20100000000000", // Group C
                "30100000000000", // Group D
                "40100000000000", // Group E
            )
        for (prefix in prefixes) {
            val checkDigit = Luhn.computeCheckDigit(prefix)
            val full = prefix + checkDigit.digitToChar()
            assertTrue(Luhn.isChecksumValid(full), "Round-trip failed for prefix=$prefix check=$checkDigit")
        }
    }
}
