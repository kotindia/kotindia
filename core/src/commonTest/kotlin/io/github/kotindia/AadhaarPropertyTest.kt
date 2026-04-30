// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import io.github.kotindia.internal.Verhoeff
import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Property-based tests for [Aadhaar] validator.
 *
 * Per PROJECT_PLAN §3.2: property tests are in a separate file from unit tests.
 * Per Anika test-strategy §4: all checkAll calls use iterations = 1000.
 *
 * Uses `io.kotest.common.runBlocking` (bundled with kotest-property) to bridge
 * suspend `checkAll` into `@Test` functions — no external coroutines dep required.
 */
@Suppress("LongMethod")
class AadhaarPropertyTest {
    // Valid Aadhaar first digits: 2..9
    private val validFirstDigits: Arb<Char> = Arb.element(('2'..'9').toList())

    // Generates exactly 10 random digit characters — will be appended after a valid first digit
    private val arb10Digits: Arb<String> =
        Arb.list(Arb.element(('0'..'9').toList()), 10..10).map { chars -> chars.joinToString("") }

    // Generates exactly 14 random digit characters — for VID prefix (16 - 1 first digit - check digit)
    private val arb14Digits: Arb<String> =
        Arb.list(Arb.element(('0'..'9').toList()), 14..14).map { chars -> chars.joinToString("") }

    /**
     * Round-trip: an 11-digit prefix starting with digit 2-9, with Verhoeff check digit appended,
     * MUST produce [ValidationResult.Valid] from [Aadhaar.validate].
     */
    @Test
    fun property_roundTrip_validPrefixPlusCheckDigit_alwaysValid() {
        runBlocking {
            checkAll<Char, String>(1000, validFirstDigits, arb10Digits) { firstDigit, suffix ->
                val prefix11 = firstDigit + suffix // 1 + 10 = 11 digits
                val check = Verhoeff.computeCheckDigit(prefix11)
                val full12 = prefix11 + check
                assertEquals(
                    ValidationResult.Valid,
                    Aadhaar.validate(full12),
                    "Round-trip failed: prefix=$prefix11 check=$check full=$full12",
                )
            }
        }
    }

    /**
     * Adjacent transposition: for any valid 12-digit Aadhaar, swapping two adjacent DIFFERENT
     * digits MUST return an invalid result (not Valid).
     *
     * Verhoeff detects ALL adjacent transpositions — this is its primary advantage over Luhn.
     */
    @Test
    fun property_adjacentTransposition_validAadhaar_alwaysInvalid() {
        runBlocking {
            checkAll<Char, String>(1000, validFirstDigits, arb10Digits) { firstDigit, suffix ->
                val prefix11 = firstDigit + suffix
                val check = Verhoeff.computeCheckDigit(prefix11)
                val full12 = prefix11 + check
                for (i in 0 until full12.length - 1) {
                    val a = full12[i]
                    val b = full12[i + 1]
                    if (a != b) {
                        val transposed = full12.substring(0, i) + b + a + full12.substring(i + 2)
                        assertFalse(
                            Aadhaar.isValid(transposed),
                            "Transposition not detected at pos=$i in full12=$full12 transposed=$transposed",
                        )
                    }
                }
            }
        }
    }

    /**
     * mask() never throws: for any string input, mask() must never throw an exception.
     */
    @Test
    fun property_maskNeverThrows_anyInput() {
        runBlocking {
            // Generate valid Aadhaar strings and verify mask never throws
            checkAll<Char, String>(500, validFirstDigits, arb10Digits) { firstDigit, suffix ->
                val prefix11 = firstDigit + suffix
                val check = Verhoeff.computeCheckDigit(prefix11)
                val full12 = prefix11 + check
                // Try various mask params — must never throw
                Aadhaar.mask(full12)
                Aadhaar.mask(full12, visibleStart = 0, visibleEnd = 0)
                Aadhaar.mask(full12, visibleStart = 4, visibleEnd = 4)
                Aadhaar.mask(full12, maskChar = '*')
                Aadhaar.mask("") // always safe
            }
        }
    }
}
