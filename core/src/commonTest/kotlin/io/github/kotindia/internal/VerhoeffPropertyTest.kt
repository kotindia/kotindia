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
 * Property-based tests for [Verhoeff] algorithm correctness.
 *
 * These tests verify properties that must hold for ALL inputs, not just known vectors:
 * 1. Round-trip: computing a check digit and then validating ALWAYS succeeds
 * 2. Single-digit corruption: changing any digit always fails (Verhoeff catches ALL single-digit errors)
 * 3. Adjacent transposition: swapping any two adjacent DIFFERENT digits always fails
 *    (Verhoeff catches ALL adjacent transpositions — this is its key design property)
 *
 * Per PROJECT_PLAN §3.2: property tests are in a separate file from unit tests.
 * Per Anika test-strategy §4: all checkAll calls use iterations = 1000.
 * Per PROJECT_PLAN guardrail: adjacent-transposition detection is MANDATORY — round-trip alone
 * is insufficient because it only proves self-consistency, not external correctness.
 *
 * Uses `io.kotest.common.runBlocking` (bundled with kotest-property) to bridge
 * suspend `checkAll` into `@Test` functions — no external coroutines dep required.
 *
 * All test prefixes are 11 digits (Aadhaar prefix length) for consistency.
 * Round-trip property uses 3-digit prefixes for variety in check-digit space.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
class VerhoeffPropertyTest {
    // Generates exactly 11 random digit characters ('0'..'9') joined as a string
    private val arb11Digits: Arb<String> =
        Arb.list(Arb.element(('0'..'9').toList()), 11..11).map { chars -> chars.joinToString("") }

    // Generates exactly 3 random digit characters — used for round-trip variety
    private val arb3Digits: Arb<String> =
        Arb.list(Arb.element(('0'..'9').toList()), 3..3).map { chars -> chars.joinToString("") }

    /**
     * Round-trip property: for ANY 11-digit numeric prefix, computing the check digit
     * and appending it MUST produce a string that Verhoeff.isValid() accepts.
     */
    @Test
    fun property_roundTrip_computeCheckDigitThenValidate_alwaysTrue() {
        runBlocking {
            checkAll<String>(1000, arb11Digits) { prefix ->
                val check = Verhoeff.computeCheckDigit(prefix)
                val full = prefix + check
                assertTrue(
                    Verhoeff.isValid(full),
                    "Round-trip failed: prefix=$prefix check=$check full=$full",
                )
            }
        }
    }

    /**
     * Single-digit corruption property: for ANY valid 12-digit string, changing any
     * single digit to a DIFFERENT value MUST make Verhoeff.isValid() return false.
     *
     * This is a fundamental property of the Verhoeff algorithm — it detects ALL single-digit errors.
     */
    @Test
    fun property_singleDigitCorruption_anyPosition_alwaysFails() {
        runBlocking {
            checkAll<String>(1000, arb11Digits) { prefix ->
                val check = Verhoeff.computeCheckDigit(prefix)
                val full = prefix + check
                for (pos in full.indices) {
                    val originalDigit = full[pos].digitToInt()
                    val corruptedDigit = (originalDigit + 1) % 10
                    val corrupted = full.substring(0, pos) + corruptedDigit.toString() + full.substring(pos + 1)
                    assertFalse(
                        Verhoeff.isValid(corrupted),
                        "Single-digit corruption NOT detected at pos=$pos: original=$full corrupted=$corrupted",
                    )
                }
            }
        }
    }

    /**
     * Adjacent transposition property: for ANY valid 12-digit string, swapping any two
     * ADJACENT digits that are DIFFERENT from each other MUST make Verhoeff.isValid() return false.
     *
     * This is the Verhoeff algorithm's PRIMARY advantage over Luhn — it detects ALL adjacent
     * transpositions, the most common human digit-entry error.
     *
     * Per PROJECT_PLAN §3.2 guardrail: "Verhoeff property tests MUST explicitly verify
     * adjacent-transposition detection — round-trip alone is not sufficient."
     */
    @Test
    fun property_adjacentTransposition_differentDigits_alwaysFails() {
        runBlocking {
            checkAll<String>(1000, arb11Digits) { prefix ->
                val check = Verhoeff.computeCheckDigit(prefix)
                val full = prefix + check
                for (i in 0 until full.length - 1) {
                    val a = full[i]
                    val b = full[i + 1]
                    // Only test transpositions of DIFFERENT digits — swapping equal digits is a no-op
                    if (a != b) {
                        val transposed = full.substring(0, i) + b + a + full.substring(i + 2)
                        assertFalse(
                            Verhoeff.isValid(transposed),
                            "Adjacent transposition NOT detected at pos=$i: original=$full transposed=$transposed",
                        )
                    }
                }
            }
        }
    }
}
