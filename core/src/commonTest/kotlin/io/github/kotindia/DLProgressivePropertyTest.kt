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
 * Property-based tests for [DL] progressive validation API.
 */
class DLProgressivePropertyTest {
    private val dlAllowedList: List<Char> = ('A'..'Z').toList() + ('a'..'z').toList() + ('0'..'9').toList()

    // Arb for strictly-partial inputs: length 1..(MIN_LENGTH-1) = 1..13.
    // DL has a dual valid-length range (14–15). Inputs < 14 chars are always Typing.
    // Inputs of length 14–15 may return Valid or Invalid depending on structure.
    private val arbStrictPartialAlpha: Arb<String> =
        Arb.list(Arb.element(dlAllowedList), 1..13).map { it.joinToString("") }

    private val arbArbitraryShort: Arb<String> =
        Arb.list(Arb.element(dlAllowedList + listOf(' ', '-', '@', '!')), 0..30).map { it.joinToString("") }

    private val arbArbitraryLong: Arb<String> =
        Arb.list(Arb.element(dlAllowedList + listOf(' ', '-', '@', '!')), 0..50).map { it.joinToString("") }

    // pt1 — all-allowed-chars inputs shorter than MIN_LENGTH (14) are always Typing.
    // DL is the only validator with a dual valid-length range (14–15) rather than a single
    // fixed maxLength, so the Typing invariant only holds for length < 14.
    @Test
    fun pt1_strictPartialAllowedChars_alwaysTyping() {
        runBlocking {
            checkAll(1000, arbStrictPartialAlpha) { input ->
                val result = DL.validateProgressive(input)
                assertTrue(result is ProgressiveResult.Typing, "Expected Typing for len=${input.length} input=$input but got $result")
            }
        }
    }

    @Test
    fun pt2_sanitize_idempotent() {
        runBlocking {
            checkAll(1000, arbArbitraryShort) { input ->
                val once = DL.sanitize(input)
                assertEquals(once, DL.sanitize(once), "sanitize not idempotent: input=$input")
            }
        }
    }

    @Test
    fun pt3_sanitize_boundsAndCharset() {
        runBlocking {
            checkAll(1000, arbArbitraryLong) { input ->
                val result = DL.sanitize(input)
                assertTrue(result.length <= DL.maxLength)
                for (c in result) assertTrue(c in DL.allowedChars, "disallowed char '$c' in: $result")
            }
        }
    }
}
