// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationContextTest {
    @Test
    fun noneIsDataObject() {
        val a: ValidationContext = ValidationContext.None
        val b: ValidationContext = ValidationContext.None
        assertTrue(a === b)
        assertEquals(a, b)
    }

    @Test
    fun lengthMismatchCarriesExpectedAndActual() {
        val ctx = ValidationContext.LengthMismatch(expected = 12, actual = 13)
        assertEquals(12, ctx.expected)
        assertEquals(13, ctx.actual)
    }

    @Test
    fun lengthMismatchEqualityByValue() {
        val a = ValidationContext.LengthMismatch(12, 13)
        val b = ValidationContext.LengthMismatch(12, 13)
        assertEquals(a, b)
    }
}
