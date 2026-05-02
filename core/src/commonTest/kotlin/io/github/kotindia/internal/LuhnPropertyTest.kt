// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.internal

import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Property-based tests for [Luhn] algorithm correctness.
 *
 * These tests verify properties that must hold for ALL inputs, not just known vectors:
 * 1. Round-trip: computing a check digit and then validating ALWAYS succeeds
 * 2. Adjacent transposition: swapping any two adjacent DIFFERENT digits of a valid
 *    15-digit number ALWAYS fails (Luhn detects ALL single adjacent transpositions of
 *    unequal digits — published Luhn property, per PRD AC11/Slice 7 lesson)
 * 3. Single-digit corruption: changing any one digit to a DIFFERENT value ALWAYS fails
 *    (Luhn detects ALL single-digit errors — published property)
 *
 * Per PRD AC11: adjacent-transposition test is MANDATORY — Anika blocks merge without it.
 * Per PROJECT_PLAN §3.2: property tests are in a separate file from unit tests.
 * All checkAll calls use iterations = 1000 per Anika test-strategy §4.
 *
 * Uses `io.kotest.common.runBlocking` (bundled with kotest-property) to bridge
 * suspend `checkAll` into `@Test` functions — no external coroutines dep required.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
class LuhnPropertyTest {
    // Generates exactly 14 random digit characters — an IMEI prefix (no check digit)
    private val arb14Digits: Arb<String> =
        Arb.list(Arb.element(('0'..'9').toList()), 14..14).map { chars -> chars.joinToString("") }

    // Generates exactly 15 random digit characters — a potential full IMEI
    private val arb15Digits: Arb<String> =
        Arb.list(Arb.element(('0'..'9').toList()), 15..15).map { chars -> chars.joinToString("") }

    /**
     * Round-trip property: for ANY 14-digit numeric prefix, computing the check digit
     * and appending it MUST produce a 15-digit string that [Luhn.isChecksumValid] accepts.
     *
     * This verifies self-consistency between [Luhn.computeCheckDigit] and [Luhn.isChecksumValid].
     * External vector tests in [LuhnTest] + [IMEIReferenceVectors] provide independent verification.
     */
    @Test
    fun property_roundTrip_computeCheckDigitThenValidate_alwaysTrue() {
        runBlocking {
            checkAll<String>(1000, arb14Digits) { prefix ->
                val checkDigit = Luhn.computeCheckDigit(prefix)
                val full = prefix + checkDigit.digitToChar()
                assertTrue(
                    Luhn.isChecksumValid(full),
                    "Round-trip failed: prefix=$prefix checkDigit=$checkDigit full=$full",
                )
            }
        }
    }

    /**
     * Adjacent transposition property: for ANY valid 15-digit Luhn number, swapping
     * any two ADJACENT digits that are DIFFERENT from each other (and are NOT the
     * `{0, 9}` pair) MUST make [Luhn.isChecksumValid] return false.
     *
     * Luhn detects most single adjacent transpositions of unequal digits. However,
     * there is one known exception: the `{0, 9}` pair. Swapping adjacent `0` and `9`
     * (in either order) is NOT detected by the Luhn algorithm — this is a documented
     * weakness (Wikipedia: "The Luhn algorithm will detect all single-digit errors, and
     * almost all transpositions of adjacent digits, with the notable exception of the
     * interchange of the two-digit sequence 09 to 90 (or vice versa)").
     *
     * The test filters out:
     * - Equal adjacent digit pairs (swapping equals is a no-op)
     * - The `{0, 9}` pair (documented Luhn exception)
     *
     * MANDATORY per PRD AC11 and Slice 7 Anika gate. The `{0,9}` exclusion is
     * documented here explicitly — it is a known algorithm limitation, not a test gap.
     */
    @Test
    fun property_adjacentTransposition_unequalDigits_alwaysFails() {
        runBlocking {
            checkAll<String>(1000, arb14Digits) { prefix ->
                val checkDigit = Luhn.computeCheckDigit(prefix)
                val full = prefix + checkDigit.digitToChar()
                // Try all adjacent pairs — only test pairs where:
                // 1. Digits differ (equal pairs are no-ops)
                // 2. Pair is NOT {0, 9} (documented Luhn exception: 09↔90 not detected)
                for (i in 0 until full.length - 1) {
                    val a = full[i]
                    val b = full[i + 1]
                    val isLuhnException = (a == '0' && b == '9') || (a == '9' && b == '0')
                    if (a != b && !isLuhnException) {
                        // Swap adjacent digits
                        val transposed = full.substring(0, i) + b + a + full.substring(i + 2)
                        assertFalse(
                            Luhn.isChecksumValid(transposed),
                            "Adjacent transposition NOT detected at pos=$i: original=$full transposed=$transposed",
                        )
                    }
                }
            }
        }
    }

    /**
     * Single-digit corruption property: for ANY valid 15-digit Luhn number, changing
     * any single digit to a DIFFERENT value MUST make [Luhn.isChecksumValid] return false.
     *
     * Luhn detects ALL single-digit errors — this is a fundamental property of the algorithm.
     * Uses +1 mod 10 as the corruption strategy (always changes to a different digit).
     */
    @Test
    fun property_singleDigitCorruption_anyPosition_alwaysFails() {
        runBlocking {
            checkAll<String>(1000, arb14Digits) { prefix ->
                val checkDigit = Luhn.computeCheckDigit(prefix)
                val full = prefix + checkDigit.digitToChar()
                for (pos in full.indices) {
                    val originalDigit = full[pos].digitToInt()
                    // +1 mod 10 always produces a different digit
                    val corruptedDigit = (originalDigit + 1) % 10
                    val corrupted =
                        full.substring(0, pos) + corruptedDigit.digitToChar() + full.substring(pos + 1)
                    assertFalse(
                        Luhn.isChecksumValid(corrupted),
                        "Single-digit corruption NOT detected at pos=$pos: original=$full corrupted=$corrupted",
                    )
                }
            }
        }
    }
}
