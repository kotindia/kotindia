// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvalidReasonTest {
    @Test
    fun entriesCountIsSix() {
        assertEquals(6, InvalidReason.entries.size)
    }

    @Test
    fun emptyPresent() {
        assertTrue(InvalidReason.entries.any { it == InvalidReason.EMPTY })
    }

    @Test
    fun wrongLengthPresent() {
        assertTrue(InvalidReason.entries.any { it == InvalidReason.WRONG_LENGTH })
    }

    @Test
    fun invalidFormatPresent() {
        assertTrue(InvalidReason.entries.any { it == InvalidReason.INVALID_FORMAT })
    }

    @Test
    fun invalidChecksumPresent() {
        assertTrue(InvalidReason.entries.any { it == InvalidReason.INVALID_CHECKSUM })
    }

    @Test
    fun invalidPrefixPresent() {
        assertTrue(InvalidReason.entries.any { it == InvalidReason.INVALID_PREFIX })
    }

    @Test
    fun invalidCategoryPresent() {
        assertTrue(InvalidReason.entries.any { it == InvalidReason.INVALID_CATEGORY })
    }

    @Test
    fun exhaustiveWhenCompiles() {
        // All 6 values are reachable — compile error if any is missing (no else branch).
        val results =
            InvalidReason.entries.map { reason ->
                when (reason) {
                    InvalidReason.EMPTY -> "empty"
                    InvalidReason.WRONG_LENGTH -> "wrong_length"
                    InvalidReason.INVALID_FORMAT -> "invalid_format"
                    InvalidReason.INVALID_CHECKSUM -> "invalid_checksum"
                    InvalidReason.INVALID_PREFIX -> "invalid_prefix"
                    InvalidReason.INVALID_CATEGORY -> "invalid_category"
                }
            }
        assertEquals(6, results.size)
    }
}
