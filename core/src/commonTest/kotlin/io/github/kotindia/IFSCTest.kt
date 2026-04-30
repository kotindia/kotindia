// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// IFSCTest — unit tests for IFSC validator (Slice 5)
// Template: follows per-validator structure established by Slice 3 (MobileTest).
// Minimum: ≥27 @Test methods (AC7). No property tests — IFSC has no checksum.
// Reference: PRD dec_20260501_015103_cd188c
// ---------------------------------------------------------------------------

class IFSCTest {
    // -----------------------------------------------------------------------
    // Valid cases (≥10)
    // -----------------------------------------------------------------------

    @Test
    fun validCanonicalUppercase() {
        assertIs<ValidationResult.Valid>(IFSC.validate("HDFC0000001"))
    }

    @Test
    fun validAllLowercase() {
        // Lowercase normalised to uppercase before validation
        assertIs<ValidationResult.Valid>(IFSC.validate("hdfc0000001"))
    }

    @Test
    fun validMixedCase() {
        assertIs<ValidationResult.Valid>(IFSC.validate("HdFc0000001"))
    }

    @Test
    fun validInternalSpaces() {
        // Copy-paste with spaces: "HDFC 0000 001" → strip whitespace → "HDFC0000001"
        assertIs<ValidationResult.Valid>(IFSC.validate("HDFC 0000 001"))
    }

    @Test
    fun validWhitespacePadded() {
        // Leading/trailing whitespace trimmed before validation
        assertIs<ValidationResult.Valid>(IFSC.validate(" HDFC0000001 "))
    }

    @Test
    fun validLowercaseWithTrailingSpace() {
        assertIs<ValidationResult.Valid>(IFSC.validate("hdfc0000001 "))
    }

    @Test
    fun validSbi() {
        assertIs<ValidationResult.Valid>(IFSC.validate("SBIN0005943"))
    }

    @Test
    fun validIcici() {
        assertIs<ValidationResult.Valid>(IFSC.validate("ICIC0001234"))
    }

    @Test
    fun validAxis() {
        assertIs<ValidationResult.Valid>(IFSC.validate("AXIS0000001"))
    }

    @Test
    fun validPnb() {
        assertIs<ValidationResult.Valid>(IFSC.validate("PUNB0000001"))
    }

    // -----------------------------------------------------------------------
    // EMPTY cases (≥3)
    // -----------------------------------------------------------------------

    @Test
    fun invalidEmpty() {
        val result = IFSC.validate("")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun invalidBlankSpaces() {
        val result = IFSC.validate("   ")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    @Test
    fun invalidTabNewline() {
        val result = IFSC.validate("\t\n")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.EMPTY, result.reason)
    }

    // -----------------------------------------------------------------------
    // WRONG_LENGTH cases (≥3)
    // -----------------------------------------------------------------------

    @Test
    fun invalidTooShort7Chars() {
        val result = IFSC.validate("HDFC000")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun invalidTooLong12Chars() {
        val result = IFSC.validate("HDFC00000012")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    @Test
    fun invalidTooShort5Chars() {
        val result = IFSC.validate("HDFC0")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    /**
     * Hyphens are NOT stripped during normalisation.
     * "HDFC-0000001" → trim (no-op) → strip whitespace (no-op) → uppercase (no-op)
     * → "HDFC-0000001" (12 chars) → WRONG_LENGTH.
     * Per AC2 engineering note: hyphen not a documented IFSC input convention.
     */
    @Test
    fun invalidHyphenInserted_wrongLength() {
        val result = IFSC.validate("HDFC-0000001")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.WRONG_LENGTH, result.reason)
    }

    // -----------------------------------------------------------------------
    // INVALID_FORMAT cases (≥5)
    // -----------------------------------------------------------------------

    @Test
    fun invalidFormatDigitInBankCode() {
        // "HD3C0000001" — digit at position 2 in bank code (chars 0–3 must be [A-Z])
        val result = IFSC.validate("HD3C0000001")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatFifthCharNotZero() {
        // "HDFC1000001" — 5th char is '1', not '0' (RBI reserved position)
        val result = IFSC.validate("HDFC1000001")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatSpecialCharInBranch() {
        // "HDFC0!00001" — '!' in branch section (chars 5–10 must be [A-Z0-9])
        val result = IFSC.validate("HDFC0!00001")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    /**
     * Anika §5 Unicode pattern: Devanagari character in bank code position.
     * "अHDC0000001" — 'अ' is multi-byte; after normalization stays as-is (not ASCII).
     * Length check: 'अ' is one Kotlin Char but the regex [A-Z] won't match it → INVALID_FORMAT.
     * Note: length == 11 chars so WRONG_LENGTH is not triggered first.
     */
    @Test
    fun invalidFormatUnicodeDevanagari() {
        val result = IFSC.validate("अHDC0000001")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    @Test
    fun invalidFormatAtSignInBranch() {
        // "HDFC00000@1" — '@' at position 9 in branch section
        val result = IFSC.validate("HDFC00000@1")
        assertIs<ValidationResult.Invalid>(result)
        assertEquals(InvalidReason.INVALID_FORMAT, result.reason)
    }

    // -----------------------------------------------------------------------
    // format() cases (≥5 distinct tests plus idempotency + throws)
    // -----------------------------------------------------------------------

    @Test
    fun formatLowercaseNormalized() {
        assertEquals("HDFC0000001", IFSC.format("hdfc0000001"))
    }

    @Test
    fun formatMixedCaseNormalized() {
        assertEquals("HDFC0000001", IFSC.format("HdFc0000001"))
    }

    @Test
    fun formatSpacesStripped() {
        assertEquals("HDFC0000001", IFSC.format("HDFC 0000 001"))
    }

    @Test
    fun formatAlreadyCanonical() {
        assertEquals("HDFC0000001", IFSC.format("HDFC0000001"))
    }

    @Test
    fun formatIdempotent() {
        // format(format(x)) == format(x) for any valid x
        val once = IFSC.format("hdfc0000001")
        assertEquals(once, IFSC.format(once))
    }

    @Test
    fun formatWhitespacePaddedInput() {
        assertEquals("HDFC0000001", IFSC.format(" HDFC0000001 "))
    }

    @Test
    fun formatInvalidInputThrows() {
        assertFailsWith<IllegalArgumentException> {
            IFSC.format("invalid")
        }
    }

    @Test
    fun formatFifthCharViolationThrows() {
        // 5th char != '0' → invalid → throws
        assertFailsWith<IllegalArgumentException> {
            IFSC.format("HDFC1000001")
        }
    }

    @Test
    fun formatWrongLengthThrows() {
        assertFailsWith<IllegalArgumentException> {
            IFSC.format("HDFC000")
        }
    }

    // -----------------------------------------------------------------------
    // isValid() cases (≥2)
    // -----------------------------------------------------------------------

    @Test
    fun isValidReturnsTrueForValid() {
        assertTrue(IFSC.isValid("HDFC0000001"))
    }

    @Test
    fun isValidReturnsFalseForInvalid() {
        assertFalse(IFSC.isValid("HDFC1000001"))
    }
}
