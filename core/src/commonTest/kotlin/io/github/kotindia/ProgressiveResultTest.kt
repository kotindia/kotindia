// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressiveResultTest {
    @Test
    fun emptyIsDataObject() {
        val a: ProgressiveResult = ProgressiveResult.Empty
        val b: ProgressiveResult = ProgressiveResult.Empty
        assertTrue(a === b)
        assertEquals(a, b)
    }

    @Test
    fun typingCarriesVisualText() {
        val r = ProgressiveResult.Typing(visualText = "123 456")
        assertEquals("123 456", r.visualText)
    }

    @Test
    fun validIsDataObject() {
        val a: ProgressiveResult = ProgressiveResult.Valid
        val b: ProgressiveResult = ProgressiveResult.Valid
        assertTrue(a === b)
        assertEquals(a, b)
    }

    @Test
    fun invalidCarriesReasonAndContext() {
        val r =
            ProgressiveResult.Invalid(
                reason = InvalidReason.WRONG_LENGTH,
                context = ValidationContext.LengthMismatch(expected = 12, actual = 13),
            )
        assertEquals(InvalidReason.WRONG_LENGTH, r.reason)
        assertEquals(ValidationContext.LengthMismatch(12, 13), r.context)
    }
}
