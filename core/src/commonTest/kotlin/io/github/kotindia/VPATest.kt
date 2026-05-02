// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VPATest {
    // -------------------------------------------------------------------------
    // Valid cases (≥12)
    // -------------------------------------------------------------------------

    @Test
    fun valid_rawLowercaseCanonical() {
        assertEquals(ValidationResult.Valid, VPA.validate("user@ybl"))
    }

    @Test
    fun valid_mixedCaseNormalized() {
        assertEquals(ValidationResult.Valid, VPA.validate("User@YBL"))
    }

    @Test
    fun valid_allUppercaseNormalized() {
        assertEquals(ValidationResult.Valid, VPA.validate("USER@YBL"))
    }

    @Test
    fun valid_whitespacePadded() {
        assertEquals(ValidationResult.Valid, VPA.validate(" user@ybl "))
    }

    @Test
    fun valid_dotInUsername() {
        assertEquals(ValidationResult.Valid, VPA.validate("name.surname@oksbi"))
    }

    @Test
    fun valid_underscoreAndDigitsInUsername() {
        assertEquals(ValidationResult.Valid, VPA.validate("user_123@ybl"))
    }

    @Test
    fun valid_dashInUsername() {
        assertEquals(ValidationResult.Valid, VPA.validate("user-name@axl"))
    }

    @Test
    fun valid_allDigitUsername() {
        assertEquals(ValidationResult.Valid, VPA.validate("9876543210@paytm"))
    }

    @Test
    fun valid_longPspHandle() {
        assertEquals(ValidationResult.Valid, VPA.validate("user@okhdfcbank"))
    }

    @Test
    fun valid_minimumThreeCharUsername() {
        assertEquals(ValidationResult.Valid, VPA.validate("abc@ibl"))
    }

    @Test
    fun valid_okicicPspHandle() {
        assertEquals(ValidationResult.Valid, VPA.validate("user@okicici"))
    }

    @Test
    fun valid_digitAndDotInUsername() {
        assertEquals(ValidationResult.Valid, VPA.validate("name.surname2@fbl"))
    }

    // -------------------------------------------------------------------------
    // EMPTY cases (≥3)
    // -------------------------------------------------------------------------

    @Test
    fun invalid_emptyString() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.EMPTY),
            VPA.validate(""),
        )
    }

    @Test
    fun invalid_whitespaceOnly() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.EMPTY),
            VPA.validate("   "),
        )
    }

    @Test
    fun invalid_tabAndNewline() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.EMPTY),
            VPA.validate("\t\n"),
        )
    }

    // -------------------------------------------------------------------------
    // WRONG_LENGTH cases (≥2)
    // -------------------------------------------------------------------------

    @Test
    fun invalid_fiftyOneCharVpa() {
        // Build: 41-char username + "@" + 9-char psp = 51 total
        val username = "a".repeat(41)
        val psp = "b".repeat(9)
        val vpa = "$username@$psp" // 41+1+9 = 51
        assertEquals(
            ValidationResult.Invalid(InvalidReason.WRONG_LENGTH),
            VPA.validate(vpa),
        )
    }

    @Test
    fun invalid_fiftyFiveCharVpa() {
        // Build: 45-char username + "@" + 9-char psp = 55 total
        val username = "a".repeat(45)
        val psp = "b".repeat(9)
        val vpa = "$username@$psp" // 45+1+9 = 55
        assertEquals(
            ValidationResult.Invalid(InvalidReason.WRONG_LENGTH),
            VPA.validate(vpa),
        )
    }

    // -------------------------------------------------------------------------
    // INVALID_FORMAT cases (≥8)
    // -------------------------------------------------------------------------

    @Test
    fun invalid_noAtSign() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            VPA.validate("user"),
        )
    }

    @Test
    fun invalid_multipleAtSigns() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            VPA.validate("a@b@c"),
        )
    }

    @Test
    fun invalid_emptyUsername() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            VPA.validate("@ybl"),
        )
    }

    @Test
    fun invalid_usernameTooShort() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            VPA.validate("ab@ybl"),
        )
    }

    @Test
    fun invalid_emptyPsp() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            VPA.validate("user@"),
        )
    }

    @Test
    fun invalid_specialCharInUsername() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            VPA.validate("user!@ybl"),
        )
    }

    @Test
    fun invalid_internalSpaceInUsername() {
        // Critical: space is NOT stripped — space in VPA = INVALID_FORMAT
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            VPA.validate("user @ybl"),
        )
    }

    @Test
    fun invalid_specialCharInPsp() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            VPA.validate("user@psp!"),
        )
    }

    @Test
    fun invalid_devanagariUnicodeInUsername() {
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            VPA.validate("उपयोगकर्ता@ybl"),
        )
    }

    // -------------------------------------------------------------------------
    // format() cases (≥4)
    // -------------------------------------------------------------------------

    @Test
    fun format_mixedCaseToLowercase() {
        assertEquals("user@ybl", VPA.format("User@YBL"))
    }

    @Test
    fun format_alreadyCanonical_idempotent() {
        assertEquals("user@ybl", VPA.format("user@ybl"))
    }

    @Test
    fun format_idempotencyProperty() {
        val input = "User@YBL"
        assertEquals(VPA.format(input), VPA.format(VPA.format(input)))
    }

    @Test
    fun format_paddedWhitespaceTrimmed() {
        assertEquals("user@ybl", VPA.format(" user@ybl "))
    }

    @Test
    fun format_allUppercaseToLowercase() {
        assertEquals("user@ybl", VPA.format("USER@YBL"))
    }

    @Test
    fun format_throwsOnInvalidInput() {
        assertFailsWith<IllegalArgumentException> {
            VPA.format("invalid")
        }
    }

    @Test
    fun format_throwsOnEmptyUsername() {
        assertFailsWith<IllegalArgumentException> {
            VPA.format("@ybl")
        }
    }

    @Test
    fun format_throwsOnEmpty() {
        assertFailsWith<IllegalArgumentException> {
            VPA.format("")
        }
    }

    // -------------------------------------------------------------------------
    // isValid() cases (≥2)
    // -------------------------------------------------------------------------

    @Test
    fun isValid_returnsTrue_forValidVpa() {
        assertTrue(VPA.isValid("user@ybl"))
    }

    @Test
    fun isValid_returnsFalse_forEmptyUsername() {
        assertFalse(VPA.isValid("@ybl"))
    }

    @Test
    fun isValid_returnsFalse_forEmpty() {
        assertFalse(VPA.isValid(""))
    }

    // -------------------------------------------------------------------------
    // Edge / boundary
    // -------------------------------------------------------------------------

    @Test
    fun valid_exactlyFiftyChars() {
        // 40-char username + "@" + 9-char psp = 50 — must be Valid
        val username = "a".repeat(40)
        val psp = "b".repeat(9)
        val vpa = "$username@$psp" // 40+1+9 = 50
        assertEquals(ValidationResult.Valid, VPA.validate(vpa))
    }

    @Test
    fun invalid_spaceBetweenUsernameAndAt() {
        // Ensures internal space is INVALID_FORMAT even adjacent to @
        assertEquals(
            ValidationResult.Invalid(InvalidReason.INVALID_FORMAT),
            VPA.validate("username @psp"),
        )
    }
}
