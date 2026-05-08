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
 * Property-based tests for [CIN] progressive validation API.
 */
class CINProgressivePropertyTest {
    private val cinAllowedList: List<Char> = ('A'..'Z').toList() + ('a'..'z').toList() + ('0'..'9').toList()

    private val arbPartialAlpha: Arb<String> =
        Arb.list(Arb.element(cinAllowedList), 1..20).map { it.joinToString("") }

    private val arbArbitraryShort: Arb<String> =
        Arb.list(Arb.element(cinAllowedList + listOf(' ', '-', '@', '!')), 0..30).map { it.joinToString("") }

    private val arbArbitraryLong: Arb<String> =
        Arb.list(Arb.element(cinAllowedList + listOf(' ', '-', '@', '!')), 0..50).map { it.joinToString("") }

    @Test
    fun pt1_partialAllowedChars_alwaysTyping() {
        runBlocking {
            checkAll(1000, arbPartialAlpha) { input ->
                val result = CIN.validateProgressive(input)
                assertTrue(result is ProgressiveResult.Typing, "Expected Typing for len=${input.length} input=$input but got $result")
            }
        }
    }

    @Test
    fun pt2_sanitize_idempotent() {
        runBlocking {
            checkAll(1000, arbArbitraryShort) { input ->
                val once = CIN.sanitize(input)
                assertEquals(once, CIN.sanitize(once), "sanitize not idempotent: input=$input")
            }
        }
    }

    @Test
    fun pt3_sanitize_boundsAndCharset() {
        runBlocking {
            checkAll(1000, arbArbitraryLong) { input ->
                val result = CIN.sanitize(input)
                assertTrue(result.length <= CIN.maxLength)
                for (c in result) assertTrue(c in CIN.allowedChars, "disallowed char '$c' in: $result")
            }
        }
    }
}
