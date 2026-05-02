// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.internal

import io.github.kotindia.GSTINReferenceVectors
import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Property-based tests for [GstinChecksum].
 *
 * Uses kotest-property [checkAll] with 1000 iterations per property.
 * Uses `io.kotest.common.runBlocking` (bundled with kotest-property) to bridge
 * suspend `checkAll` into `@Test` functions — matches Luhn/Verhoeff property test pattern.
 * Uses `kotlin.test` assertions (NOT kotest shouldBe — that requires JVM-only runner).
 *
 * Properties tested:
 * 1. Round-trip: for any valid GSTIN prefix, computeCheckChar + isChecksumValid = true
 * 2. Check-char corruption: replacing the check char (position 14) with +1 mod-36 invalidates
 * 3. Single-char corruption: replacing any of the first 14 chars with +1 mod-36 invalidates
 *
 * All checkAll calls use iterations = 1000 per test-strategy §4.
 * Property tests are in a separate file from unit tests (per PROJECT_PLAN §3.2).
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
internal class GstinChecksumPropertyTest {
    private val base36 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    private val validStateCodes =
        listOf(
            "01",
            "02",
            "03",
            "04",
            "05",
            "06",
            "07",
            "08",
            "09",
            "10",
            "11",
            "12",
            "13",
            "14",
            "15",
            "16",
            "17",
            "18",
            "19",
            "20",
            "21",
            "22",
            "23",
            "24",
            "25",
            "26",
            "27",
            "28",
            "29",
            "30",
            "31",
            "32",
            "33",
            "34",
            "35",
            "36",
            "37",
            "38",
        )

    private val validCategoryChars = listOf('P', 'C', 'H', 'A', 'B', 'G', 'J', 'L', 'F', 'T')

    private val upperCaseLetters = ('A'..'Z').toList()
    private val digits = ('0'..'9').toList()
    private val entityChars: List<Char> = ('1'..'9').toList() + upperCaseLetters

    // Arb for 3 random uppercase letters
    private val arb3UpperCase: Arb<String> =
        Arb.list(Arb.element(upperCaseLetters), 3..3).map { it.joinToString("") }

    // Arb for 1 random uppercase letter
    private val arb1UpperCase: Arb<String> =
        Arb.list(Arb.element(upperCaseLetters), 1..1).map { it.joinToString("") }

    // Arb for 4 random decimal digits
    private val arb4Digits: Arb<String> =
        Arb.list(Arb.element(digits), 4..4).map { it.joinToString("") }

    /**
     * Property 1 — Round-trip across all valid state codes.
     *
     * For any valid GSTIN prefix (state + PAN-shaped 10 chars + entity + 'Z'),
     * computeCheckChar() produces a character c such that isChecksumValid(prefix14 + c) == true.
     *
     * Covers all 38 valid state codes (Arb.element cycles through the full list).
     */
    @Test
    fun property_roundTrip_computeAndValidate() {
        runBlocking {
            checkAll(
                iterations = 1000,
                Arb.element(validStateCodes),
                arb3UpperCase,
                Arb.element(validCategoryChars),
                arb1UpperCase,
                arb4Digits,
                arb1UpperCase,
                Arb.element(entityChars),
            ) { state, p13, cat, p5, digitStr, p10, entity ->
                // Construct PAN-shaped 10-char string: [A-Z]{3}[PCHABGJLFT][A-Z][0-9]{4}[A-Z]
                val pan = "$p13$cat$p5$digitStr$p10"
                val prefix14 = "${state}${pan}${entity}Z"
                val checkChar = GstinChecksum.computeCheckChar(prefix14)
                val full = prefix14 + checkChar
                assertTrue(
                    GstinChecksum.isChecksumValid(full),
                    "Round-trip failed for state=$state prefix14=$prefix14 checkChar=$checkChar",
                )
            }
        }
    }

    /**
     * Property 2 — Corrupting the check character (position 14) always invalidates.
     *
     * For any known-valid GSTIN, replacing the last character with the next base-36
     * character (+1 mod 36, cyclic) must cause isChecksumValid to return false.
     */
    @Test
    fun property_checkCharCorruption_alwaysInvalidates() {
        runBlocking {
            checkAll(
                iterations = 1000,
                Arb.element(GSTINReferenceVectors.KNOWN_VALID_GSTINS),
            ) { valid ->
                val checkChar = valid.last()
                val corruptedIdx = (base36.indexOf(checkChar) + 1) % 36
                val corruptedChar = base36[corruptedIdx]
                if (corruptedChar != checkChar) {
                    val corrupted = valid.dropLast(1) + corruptedChar
                    assertFalse(
                        GstinChecksum.isChecksumValid(corrupted),
                        "Corrupted check char should fail: $corrupted (original: $valid)",
                    )
                }
            }
        }
    }

    /**
     * Property 3 — Single-char corruption at any position (0..13) always invalidates.
     *
     * For any known-valid GSTIN, replacing any of the first 14 characters with the next
     * base-36 character (+1 mod 36) must cause isChecksumValid to return false.
     *
     * The GSTN base-36 weighted-sum algorithm detects single-character substitutions
     * reliably. Any algorithm-specific exception discovered here would be documented.
     */
    @Test
    fun property_singleCharCorruption_first14_alwaysInvalidates() {
        runBlocking {
            checkAll(
                iterations = 1000,
                Arb.element(GSTINReferenceVectors.KNOWN_VALID_GSTINS),
                Arb.int(0, 13),
            ) { valid, pos ->
                val original = valid[pos]
                val originalIdx = base36.indexOf(original)
                val corruptedIdx = (originalIdx + 1) % 36
                val corruptedChar = base36[corruptedIdx]
                if (corruptedChar != original) {
                    val corrupted = valid.substring(0, pos) + corruptedChar + valid.substring(pos + 1)
                    assertFalse(
                        GstinChecksum.isChecksumValid(corrupted),
                        "Single-char corruption at pos=$pos should fail: $corrupted (original: $valid)",
                    )
                }
            }
        }
    }
}
