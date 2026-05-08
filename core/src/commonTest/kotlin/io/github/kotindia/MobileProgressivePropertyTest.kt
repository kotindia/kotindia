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
 * Property-based tests for [Mobile] progressive validation API.
 */
class MobileProgressivePropertyTest {
    // Arb for partial inputs: digit strings of length 1..(maxLength-1) = 1..9
    private val arbPartialDigits: Arb<String> =
        Arb
            .list(Arb.element(('0'..'9').toList()), 1..9)
            .map { it.joinToString("") }

    private val arbArbitraryShort: Arb<String> =
        Arb
            .list(
                Arb.element(
                    ('0'..'9').toList() + ('a'..'z').toList() + ('A'..'Z').toList() +
                        listOf(' ', '-', '+', '_', '@'),
                ),
                0..30,
            ).map { it.joinToString("") }

    private val arbArbitraryLong: Arb<String> =
        Arb
            .list(
                Arb.element(
                    ('0'..'9').toList() + ('a'..'z').toList() + ('A'..'Z').toList() +
                        listOf(' ', '-', '+', '_', '@'),
                ),
                0..50,
            ).map { it.joinToString("") }

    // PT1 — Typing invariant
    @Test
    fun pt1_partialAllowedChars_alwaysTyping() {
        runBlocking {
            checkAll(1000, arbPartialDigits) { input ->
                val result = Mobile.validateProgressive(input)
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
                val once = Mobile.sanitize(input)
                val twice = Mobile.sanitize(once)
                assertEquals(once, twice, "sanitize not idempotent: input=$input once=$once twice=$twice")
            }
        }
    }

    // PT3 — sanitize bounds and charset
    @Test
    fun pt3_sanitize_boundsAndCharset() {
        runBlocking {
            checkAll(1000, arbArbitraryLong) { input ->
                val result = Mobile.sanitize(input)
                assertTrue(result.length <= Mobile.maxLength, "sanitize output too long: ${result.length} > ${Mobile.maxLength}")
                for (c in result) {
                    assertTrue(c in Mobile.allowedChars, "sanitize output contains disallowed char '$c' in: $result")
                }
            }
        }
    }
}
