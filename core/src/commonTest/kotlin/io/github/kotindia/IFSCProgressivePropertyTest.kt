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
 * Property-based tests for [IFSC] progressive validation API.
 */
class IFSCProgressivePropertyTest {
    private val ifscAllowedList: List<Char> =
        ('A'..'Z').toList() + ('a'..'z').toList() + ('0'..'9').toList()

    // Arb for partial inputs: alphanumeric strings of length 1..(maxLength-1) = 1..10
    private val arbPartialAlpha: Arb<String> =
        Arb
            .list(Arb.element(ifscAllowedList), 1..10)
            .map { it.joinToString("") }

    private val arbArbitraryShort: Arb<String> =
        Arb
            .list(
                Arb.element(ifscAllowedList + listOf(' ', '-', '_', '@', '!', '#', '/', '\\')),
                0..30,
            ).map { it.joinToString("") }

    private val arbArbitraryLong: Arb<String> =
        Arb
            .list(
                Arb.element(ifscAllowedList + listOf(' ', '-', '_', '@', '!', '#', '/', '\\')),
                0..50,
            ).map { it.joinToString("") }

    // PT1 — Typing invariant
    @Test
    fun pt1_partialAllowedChars_alwaysTyping() {
        runBlocking {
            checkAll(1000, arbPartialAlpha) { input ->
                val result = IFSC.validateProgressive(input)
                assertTrue(
                    result is ProgressiveResult.Typing,
                    "Expected Typing for len=${input.length} input=$input but got $result",
                )
            }
        }
    }

    // PT2 — sanitize idempotency
    @Test
    fun pt2_sanitize_idempotent() {
        runBlocking {
            checkAll(1000, arbArbitraryShort) { input ->
                val once = IFSC.sanitize(input)
                val twice = IFSC.sanitize(once)
                assertEquals(once, twice, "sanitize not idempotent: input=$input once=$once twice=$twice")
            }
        }
    }

    // PT3 — sanitize bounds and charset
    @Test
    fun pt3_sanitize_boundsAndCharset() {
        runBlocking {
            checkAll(1000, arbArbitraryLong) { input ->
                val result = IFSC.sanitize(input)
                assertTrue(result.length <= IFSC.maxLength, "sanitize output too long: ${result.length} > ${IFSC.maxLength}")
                for (c in result) {
                    assertTrue(c in IFSC.allowedChars, "sanitize output contains disallowed char '$c' in: $result")
                }
            }
        }
    }
}
