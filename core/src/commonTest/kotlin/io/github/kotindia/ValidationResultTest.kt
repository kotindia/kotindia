// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class ValidationResultTest {
    @Test
    fun validIsInstanceOfValidationResult() {
        assertIs<ValidationResult>(ValidationResult.Valid)
    }

    @Test
    fun invalidIsInstanceOfValidationResult() {
        assertIs<ValidationResult>(ValidationResult.Invalid(InvalidReason.EMPTY))
    }

    @Test
    fun invalidReasonIsCorrect() {
        val result = ValidationResult.Invalid(InvalidReason.EMPTY)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun validDataObjectIdentity() {
        // data object: same reference, equals true
        val a = ValidationResult.Valid
        val b = ValidationResult.Valid
        assertEquals(a, b)
    }

    @Test
    fun invalidDataClassEquality() {
        val a = ValidationResult.Invalid(InvalidReason.EMPTY)
        val b = ValidationResult.Invalid(InvalidReason.EMPTY)
        assertEquals(a, b)
    }

    @Test
    fun invalidInequalityDifferentReasons() {
        val a = ValidationResult.Invalid(InvalidReason.EMPTY)
        val b = ValidationResult.Invalid(InvalidReason.WRONG_LENGTH)
        assertNotEquals(a, b)
    }

    @Test
    fun exhaustiveWhenCompiles() {
        // Verifies that a when expression over ValidationResult compiles
        // without an else branch — sealed contract is exhaustive.
        val result: ValidationResult = ValidationResult.Valid
        val label =
            when (result) {
                is ValidationResult.Valid -> "valid"
                is ValidationResult.Invalid -> "invalid"
            }
        assertEquals("valid", label)
    }

    @Test
    fun exhaustiveWhenOnInvalidBranch() {
        val result: ValidationResult = ValidationResult.Invalid(InvalidReason.INVALID_FORMAT)
        val label =
            when (result) {
                is ValidationResult.Valid -> "valid"
                is ValidationResult.Invalid -> "invalid:${result.reason}"
            }
        assertEquals("invalid:${InvalidReason.INVALID_FORMAT}", label)
    }
}
