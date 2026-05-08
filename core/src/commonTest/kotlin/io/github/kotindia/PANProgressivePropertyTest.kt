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
 * Property-based tests for [PAN] progressive validation API.
 *
 * Per Anika's Phase 2 test strategy §1.5: 3 properties per validator.
 * PT1: Typing invariant — partial valid-char inputs always return Typing.
 * PT2: sanitize idempotency.
 * PT3: sanitize bounds and charset.
 */
class PANProgressivePropertyTest {
    // PAN allowed chars: A-Z + a-z + 0-9
    private val panAllowedList: List<Char> =
        ('A'..'Z').toList() + ('a'..'z').toList() + ('0'..'9').toList()

    // Arb for partial inputs: alphanumeric strings of length 1..(maxLength-1) = 1..9
    private val arbPartialAlpha: Arb<String> =
        Arb
            .list(Arb.element(panAllowedList), 1..9)
            .map { it.joinToString("") }

    // Arb for arbitrary strings with mixed chars, length 0..30
    private val arbArbitraryShort: Arb<String> =
        Arb
            .list(
                Arb.element(
                    panAllowedList + listOf(' ', '-', '_', '@', '!', '#', '/', '\\'),
                ),
                0..30,
            ).map { it.joinToString("") }

    // Arb for longer arbitrary strings, length 0..50
    private val arbArbitraryLong: Arb<String> =
        Arb
            .list(
                Arb.element(
                    panAllowedList + listOf(' ', '-', '_', '@', '!', '#', '/', '\\'),
                ),
                0..50,
            ).map { it.joinToString("") }

    // PT1 — Typing invariant: partial allowed-char inputs always return ProgressiveResult.Typing
    @Test
    fun pt1_partialAllowedChars_alwaysTyping() {
        runBlocking {
            checkAll(1000, arbPartialAlpha) { input ->
                val result = PAN.validateProgressive(input)
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
                val once = PAN.sanitize(input)
                val twice = PAN.sanitize(once)
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
                val result = PAN.sanitize(input)
                assertTrue(
                    result.length <= PAN.maxLength,
                    "sanitize output too long: ${result.length} > ${PAN.maxLength}",
                )
                for (c in result) {
                    assertTrue(
                        c in PAN.allowedChars,
                        "sanitize output contains disallowed char '$c' in: $result",
                    )
                }
            }
        }
    }
}
