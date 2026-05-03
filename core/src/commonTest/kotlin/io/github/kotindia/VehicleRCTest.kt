// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VehicleRCTest {
    // -------------------------------------------------------------------------
    // Valid cases (14 tests)
    // -------------------------------------------------------------------------

    @Test
    fun `valid - MH12AB1234 Maharashtra 2-digit district 2-letter series`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("MH12AB1234"))
    }

    @Test
    fun `valid - KA01A1234 Karnataka 1-digit district 1-letter series 9 chars`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("KA01A1234"))
    }

    @Test
    fun `valid - DL4CAB1234 Delhi 1-digit district 2-letter series 10 chars`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("DL4CAB1234"))
    }

    @Test
    fun `valid - TN09ABC1234 Tamil Nadu 2-digit district 3-letter series 11 chars`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("TN09ABC1234"))
    }

    @Test
    fun `valid - UP32AB1234 Uttar Pradesh`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("UP32AB1234"))
    }

    @Test
    fun `valid - KL07BA1234 Kerala`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("KL07BA1234"))
    }

    @Test
    fun `valid - GJ01AA1234 Gujarat`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("GJ01AA1234"))
    }

    @Test
    fun `valid - RJ14AB1234 Rajasthan`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("RJ14AB1234"))
    }

    @Test
    fun `valid - PB10A1234 Punjab 1-letter series 9 chars`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("PB10A1234"))
    }

    @Test
    fun `valid - WB02A1234 West Bengal 9 chars`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("WB02A1234"))
    }

    @Test
    fun `valid - spaces stripped MH 12 AB 1234`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("MH 12 AB 1234"))
    }

    @Test
    fun `valid - hyphens stripped MH-12-AB-1234`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("MH-12-AB-1234"))
    }

    @Test
    fun `valid - lowercase mh12ab1234`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("mh12ab1234"))
    }

    @Test
    fun `valid - whitespace padded with spaces`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate(" MH12AB1234 "))
    }

    // -------------------------------------------------------------------------
    // EMPTY cases (3 tests)
    // -------------------------------------------------------------------------

    @Test
    fun `empty - empty string`() {
        val result = VehicleRC.validate("")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun `empty - blank spaces only`() {
        val result = VehicleRC.validate("   ")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun `empty - tab and newline`() {
        val result = VehicleRC.validate("\t\n")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    // -------------------------------------------------------------------------
    // WRONG_LENGTH cases (3 tests)
    // -------------------------------------------------------------------------

    @Test
    fun `wrong length - 7 chars after normalize MH1A123`() {
        val result = VehicleRC.validate("MH1A123")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun `wrong length - 12 chars after normalize TN09ABCD1234`() {
        val result = VehicleRC.validate("TN09ABCD1234")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun `wrong length - 7 chars MH12A12`() {
        val result = VehicleRC.validate("MH12A12")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    // -------------------------------------------------------------------------
    // INVALID_FORMAT cases (5 tests)
    // -------------------------------------------------------------------------

    @Test
    fun `invalid format - digits in state code position 12AB001234`() {
        val result = VehicleRC.validate("12AB001234")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun `invalid format - special char exclamation MH1!AB1234`() {
        val result = VehicleRC.validate("MH1!AB1234")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun `invalid format - Devanagari Unicode`() {
        val result = VehicleRC.validate("अBCD1234AB")
        assertIs<ValidationResult.Invalid>(result)
        // Unicode chars push length > 11 after stripping whitespace/hyphens — WRONG_LENGTH or INVALID_FORMAT
        // Either is acceptable; enforce it is not Valid
        assertTrue(result.reason == InvalidReason.WRONG_LENGTH || result.reason == InvalidReason.INVALID_FORMAT)
    }

    @Test
    fun `invalid format - letter in final 4-digit section MH12AB123X`() {
        val result = VehicleRC.validate("MH12AB123X")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun `invalid format - 4-letter series exceeds max MH12ABCD1234`() {
        val result = VehicleRC.validate("MH12ABCD1234")
        assertIs<ValidationResult.Invalid>(result)
        // 12 chars → WRONG_LENGTH (before format check fires)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    // -------------------------------------------------------------------------
    // INVALID_PREFIX cases (3 tests)
    // -------------------------------------------------------------------------

    @Test
    fun `invalid prefix - ZZ not a state code`() {
        val result = VehicleRC.validate("ZZ12AB1234")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    @Test
    fun `invalid prefix - XX not a state code`() {
        val result = VehicleRC.validate("XX12AB1234")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    @Test
    fun `invalid prefix - AA not a state code`() {
        val result = VehicleRC.validate("AA12AB1234")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_PREFIX, result.reason)
    }

    // -------------------------------------------------------------------------
    // format() cases (6 tests)
    // -------------------------------------------------------------------------

    @Test
    fun `format - lowercase to canonical`() {
        assertEquals("MH12AB1234", VehicleRC.format("mh12ab1234"))
    }

    @Test
    fun `format - spaces stripped to canonical`() {
        assertEquals("MH12AB1234", VehicleRC.format("MH 12 AB 1234"))
    }

    @Test
    fun `format - hyphens stripped to canonical`() {
        assertEquals("MH12AB1234", VehicleRC.format("MH-12-AB-1234"))
    }

    @Test
    fun `format - already canonical is idempotent`() {
        assertEquals("MH12AB1234", VehicleRC.format("MH12AB1234"))
    }

    @Test
    fun `format - double application is idempotent`() {
        assertEquals(VehicleRC.format("mh12ab1234"), VehicleRC.format(VehicleRC.format("mh12ab1234")))
    }

    @Test
    fun `format - throws IllegalArgumentException for invalid state code`() {
        assertFailsWith<IllegalArgumentException> {
            VehicleRC.format("ZZ12AB1234")
        }
    }

    // -------------------------------------------------------------------------
    // isValid() cases (2 tests)
    // -------------------------------------------------------------------------

    @Test
    fun `isValid - valid plate returns true`() {
        assertTrue(VehicleRC.isValid("MH12AB1234"))
    }

    @Test
    fun `isValid - invalid state code returns false`() {
        assertFalse(VehicleRC.isValid("ZZ12AB1234"))
    }

    // -------------------------------------------------------------------------
    // Additional edge & boundary cases (3 tests)
    // -------------------------------------------------------------------------

    @Test
    fun `valid - OR Odisha legacy state code`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("OR01A1234"))
    }

    @Test
    fun `valid - LA Ladakh post-2019 UT`() {
        assertEquals(ValidationResult.Valid, VehicleRC.validate("LA01A1234"))
    }

    @Test
    fun `format - throws for wrong length input`() {
        assertFailsWith<IllegalArgumentException> {
            VehicleRC.format("MH12AB123")
        }
    }
}
