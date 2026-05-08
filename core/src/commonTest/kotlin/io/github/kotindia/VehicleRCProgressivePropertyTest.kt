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
 * Property-based tests for [VehicleRC] progressive validation API.
 */
class VehicleRCProgressivePropertyTest {
    private val rcAllowedList: List<Char> = ('A'..'Z').toList() + ('a'..'z').toList() + ('0'..'9').toList()

    private val arbPartialAlpha: Arb<String> =
        Arb.list(Arb.element(rcAllowedList), 1..9).map { it.joinToString("") }

    private val arbArbitraryShort: Arb<String> =
        Arb.list(Arb.element(rcAllowedList + listOf(' ', '-', '@', '!')), 0..30).map { it.joinToString("") }

    private val arbArbitraryLong: Arb<String> =
        Arb.list(Arb.element(rcAllowedList + listOf(' ', '-', '@', '!')), 0..50).map { it.joinToString("") }

    @Test
    fun pt1_partialAllowedChars_alwaysTyping() {
        runBlocking {
            checkAll(1000, arbPartialAlpha) { input ->
                val result = VehicleRC.validateProgressive(input)
                assertTrue(result is ProgressiveResult.Typing, "Expected Typing for len=${input.length} input=$input but got $result")
            }
        }
    }

    @Test
    fun pt2_sanitize_idempotent() {
        runBlocking {
            checkAll(1000, arbArbitraryShort) { input ->
                val once = VehicleRC.sanitize(input)
                assertEquals(once, VehicleRC.sanitize(once), "sanitize not idempotent: input=$input")
            }
        }
    }

    @Test
    fun pt3_sanitize_boundsAndCharset() {
        runBlocking {
            checkAll(1000, arbArbitraryLong) { input ->
                val result = VehicleRC.sanitize(input)
                assertTrue(result.length <= VehicleRC.maxLength)
                for (c in result) assertTrue(c in VehicleRC.allowedChars, "disallowed char '$c' in: $result")
            }
        }
    }
}
