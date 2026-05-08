// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Property-based tests for [Aadhaar] progressive validation API.
 *
 * Per Anika's Phase 2 test strategy §1.5: 3 properties per validator.
 * PT1: Typing invariant — partial valid-char inputs always return Typing.
 * PT2: sanitize idempotency.
 * PT3: sanitize bounds and charset.
 *
 * Uses `io.kotest.common.runBlocking` (bundled with kotest-property) to bridge
 * suspend `checkAll` into `@Test` — no external coroutines dep.
 * All checkAll calls use iterations = 1000 per Phase 2 strategy §1.5.
 *
 * Arb construction follows existing project convention: Arb.list(Arb.element(...)).map(joinToString).
 */
class AadhaarProgressivePropertyTest {
    // Arb for partial inputs: digit strings of length 1..(maxLength-1) = 1..11
    private val arbPartialDigits: Arb<String> =
        Arb
            .list(Arb.element(('0'..'9').toList()), 1..11)
            .map { it.joinToString("") }

    // Arb for arbitrary strings: mix of ASCII chars including non-allowed ones, length 0..30
    // Used for sanitize idempotency and bounds tests.
    private val arbArbitraryShort: Arb<String> =
        Arb
            .list(
                Arb.element(
                    ('0'..'9').toList() + ('a'..'z').toList() + ('A'..'Z').toList() +
                        listOf(' ', '-', '_', '@', '!', '#', '/', '\\'),
                ),
                0..30,
            ).map { it.joinToString("") }

    // Arb for longer arbitrary strings, length 0..50
    private val arbArbitraryLong: Arb<String> =
        Arb
            .list(
                Arb.element(
                    ('0'..'9').toList() + ('a'..'z').toList() + ('A'..'Z').toList() +
                        listOf(' ', '-', '_', '@', '!', '#', '/', '\\'),
                ),
                0..50,
            ).map { it.joinToString("") }

    // PT1 — Typing invariant: partial allowed-char inputs always return ProgressiveResult.Typing
    @Test
    fun pt1_partialAllowedChars_alwaysTyping() {
        runBlocking {
            checkAll(1000, arbPartialDigits) { input ->
                val result = Aadhaar.validateProgressive(input)
                assertTrue(
                    result is ProgressiveResult.Typing,
                    "Expected Typing for len=${input.length} input=$input but got $result",
                )
            }
        }
    }

    // PT2 — sanitize idempotency: sanitize(sanitize(x)) == sanitize(x) for all strings
    @Test
    fun pt2_sanitize_idempotent() {
        runBlocking {
            checkAll(1000, arbArbitraryShort) { input ->
                val once = Aadhaar.sanitize(input)
                val twice = Aadhaar.sanitize(once)
                assertEquals(
                    once,
                    twice,
                    "sanitize not idempotent: input=$input once=$once twice=$twice",
                )
            }
        }
    }

    // PT3 — sanitize bounds and charset: output length <= maxLength, all chars in allowedChars
    @Test
    fun pt3_sanitize_boundsAndCharset() {
        runBlocking {
            checkAll(1000, arbArbitraryLong) { input ->
                val result = Aadhaar.sanitize(input)
                assertTrue(
                    result.length <= Aadhaar.maxLength,
                    "sanitize output too long: ${result.length} > ${Aadhaar.maxLength}",
                )
                for (c in result) {
                    assertTrue(
                        c in Aadhaar.allowedChars,
                        "sanitize output contains disallowed char '$c' in: $result",
                    )
                }
            }
        }
    }
}
