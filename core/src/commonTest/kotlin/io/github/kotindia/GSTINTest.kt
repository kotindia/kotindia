// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [GSTIN] — validate, isValid, format.
 *
 * Valid-case tests use [GSTINReferenceVectors.KNOWN_VALID_GSTINS] entries sourced from:
 * - tk120404/gst JS implementation (https://github.com/tk120404/gst)
 * - mastermunj/format-utils TS implementation (https://github.com/mastermunj/format-utils)
 * Both implementations agreed on all 114 test vectors (zero disagreements).
 *
 * State code coverage: one @Test per valid code 01–38, including Ladakh (38) per R3.
 */
internal class GSTINTest {
    // ---------------------------------------------------------------------------
    // validate — Known valid (≥10 from reference vectors)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_knownValid_maharashtra_aapfu_returnsValid() {
        // 27AAPFU0939F1ZV — externally verified by tk120404/gst + mastermunj
        assertEquals(ValidationResult.Valid, GSTIN.validate(GSTINReferenceVectors.KNOWN_VALID_GSTINS[78]))
    }

    @Test
    fun validate_knownValid_maharashtra_aatfb_returnsValid() {
        assertEquals(ValidationResult.Valid, GSTIN.validate(GSTINReferenceVectors.KNOWN_VALID_GSTINS[79]))
    }

    @Test
    fun validate_knownValid_maharashtra_aacce_returnsValid() {
        assertEquals(ValidationResult.Valid, GSTIN.validate(GSTINReferenceVectors.KNOWN_VALID_GSTINS[80]))
    }

    @Test
    fun validate_knownValid_karnataka_aapfu_returnsValid() {
        // 29AAPFU0939F1ZR
        assertEquals(ValidationResult.Valid, GSTIN.validate(GSTINReferenceVectors.KNOWN_VALID_GSTINS[84]))
    }

    @Test
    fun validate_knownValid_karnataka_aatfb_returnsValid() {
        // 29AATFB5356N1ZK
        assertEquals(ValidationResult.Valid, GSTIN.validate(GSTINReferenceVectors.KNOWN_VALID_GSTINS[85]))
    }

    @Test
    fun validate_knownValid_delhi_aapfu_returnsValid() {
        // 07AAPFU0939F1ZX
        assertEquals(ValidationResult.Valid, GSTIN.validate(GSTINReferenceVectors.KNOWN_VALID_GSTINS[18]))
    }

    @Test
    fun validate_knownValid_kerala_aapfu_returnsValid() {
        // 32AAPFU0939F1Z4
        assertEquals(ValidationResult.Valid, GSTIN.validate(GSTINReferenceVectors.KNOWN_VALID_GSTINS[93]))
    }

    @Test
    fun validate_knownValid_tamilnadu_aapfu_returnsValid() {
        // 33AAPFU0939F1Z2
        assertEquals(ValidationResult.Valid, GSTIN.validate(GSTINReferenceVectors.KNOWN_VALID_GSTINS[96]))
    }

    @Test
    fun validate_knownValid_gujarat_aapfu_returnsValid() {
        // 24AAPFU0939F1Z1
        assertEquals(ValidationResult.Valid, GSTIN.validate(GSTINReferenceVectors.KNOWN_VALID_GSTINS[69]))
    }

    @Test
    fun validate_knownValid_rajasthan_aapfu_returnsValid() {
        // 08AAPFU0939F1ZV
        assertEquals(ValidationResult.Valid, GSTIN.validate(GSTINReferenceVectors.KNOWN_VALID_GSTINS[21]))
    }

    @Test
    fun validate_knownValid_lowercase_normalised_returnsValid() {
        // lowercase → normalised to uppercase before validation
        assertEquals(ValidationResult.Valid, GSTIN.validate("27aapfu0939f1zv"))
    }

    @Test
    fun validate_knownValid_withLeadingTrailingWhitespace_returnsValid() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("  27AAPFU0939F1ZV  "))
    }

    @Test
    fun validate_knownValid_withInternalSpace_returnsValid() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("27 AAPFU0939F1ZV"))
    }

    // ---------------------------------------------------------------------------
    // validate — State code coverage (≥38, one per state code 01–38)
    // All vectors externally sourced from tk120404/gst + mastermunj — both agree
    // ---------------------------------------------------------------------------

    @Test
    fun validGstin_state01_JammuKashmir() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("01AAPFU0939F1Z9"))
    }

    @Test
    fun validGstin_state02_HimachalPradesh() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("02AAPFU0939F1Z7"))
    }

    @Test
    fun validGstin_state03_Punjab() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("03AAPFU0939F1Z5"))
    }

    @Test
    fun validGstin_state04_Chandigarh() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("04AAPFU0939F1Z3"))
    }

    @Test
    fun validGstin_state05_Uttarakhand() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("05AAPFU0939F1Z1"))
    }

    @Test
    fun validGstin_state06_Haryana() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("06AAPFU0939F1ZZ"))
    }

    @Test
    fun validGstin_state07_Delhi() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("07AAPFU0939F1ZX"))
    }

    @Test
    fun validGstin_state08_Rajasthan() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("08AAPFU0939F1ZV"))
    }

    @Test
    fun validGstin_state09_UttarPradesh() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("09AAPFU0939F1ZT"))
    }

    @Test
    fun validGstin_state10_Bihar() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("10AAPFU0939F1ZA"))
    }

    @Test
    fun validGstin_state11_Sikkim() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("11AAPFU0939F1Z8"))
    }

    @Test
    fun validGstin_state12_ArunachalPradesh() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("12AAPFU0939F1Z6"))
    }

    @Test
    fun validGstin_state13_Nagaland() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("13AAPFU0939F1Z4"))
    }

    @Test
    fun validGstin_state14_Manipur() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("14AAPFU0939F1Z2"))
    }

    @Test
    fun validGstin_state15_Mizoram() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("15AAPFU0939F1Z0"))
    }

    @Test
    fun validGstin_state16_Tripura() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("16AAPFU0939F1ZY"))
    }

    @Test
    fun validGstin_state17_Meghalaya() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("17AAPFU0939F1ZW"))
    }

    @Test
    fun validGstin_state18_Assam() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("18AAPFU0939F1ZU"))
    }

    @Test
    fun validGstin_state19_WestBengal() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("19AAPFU0939F1ZS"))
    }

    @Test
    fun validGstin_state20_Jharkhand() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("20AAPFU0939F1Z9"))
    }

    @Test
    fun validGstin_state21_Odisha() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("21AAPFU0939F1Z7"))
    }

    @Test
    fun validGstin_state22_Chhattisgarh() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("22AAPFU0939F1Z5"))
    }

    @Test
    fun validGstin_state23_MadhyaPradesh() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("23AAPFU0939F1Z3"))
    }

    @Test
    fun validGstin_state24_Gujarat() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("24AAPFU0939F1Z1"))
    }

    @Test
    fun validGstin_state25_DamanDiu_legacy() {
        // LEGACY state code 25 — merged into 26 in 2020; existing GSTINs remain valid
        assertEquals(ValidationResult.Valid, GSTIN.validate("25AAPFU0939F1ZZ"))
    }

    @Test
    fun validGstin_state26_DadraNagarHaveli() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("26AAPFU0939F1ZX"))
    }

    @Test
    fun validGstin_state27_Maharashtra() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("27AAPFU0939F1ZV"))
    }

    @Test
    fun validGstin_state28_AndhraPradesh_legacy() {
        // LEGACY state code 28 — replaced by 37 post-2014; existing GSTINs remain valid
        assertEquals(ValidationResult.Valid, GSTIN.validate("28AAPFU0939F1ZT"))
    }

    @Test
    fun validGstin_state29_Karnataka() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("29AAPFU0939F1ZR"))
    }

    @Test
    fun validGstin_state30_Goa() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("30AAPFU0939F1Z8"))
    }

    @Test
    fun validGstin_state31_Lakshadweep() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("31AAPFU0939F1Z6"))
    }

    @Test
    fun validGstin_state32_Kerala() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("32AAPFU0939F1Z4"))
    }

    @Test
    fun validGstin_state33_TamilNadu() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("33AAPFU0939F1Z2"))
    }

    @Test
    fun validGstin_state34_Puducherry() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("34AAPFU0939F1Z0"))
    }

    @Test
    fun validGstin_state35_AndamanNicobar() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("35AAPFU0939F1ZY"))
    }

    @Test
    fun validGstin_state36_Telangana() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("36AAPFU0939F1ZW"))
    }

    @Test
    fun validGstin_state37_AndhraPradesh() {
        assertEquals(ValidationResult.Valid, GSTIN.validate("37AAPFU0939F1ZU"))
    }

    @Test
    fun validGstin_state38_Ladakh() {
        // R3 EXPLICIT — Ladakh post-2019 reorganisation; absent from pre-2020 validators
        // Source: tk120404/gst + mastermunj both agree on 38AAPFU0939F1ZS
        assertEquals(ValidationResult.Valid, GSTIN.validate("38AAPFU0939F1ZS"))
    }

    // ---------------------------------------------------------------------------
    // validate — EMPTY (≥3)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_emptyString_returnsInvalidEmpty() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), GSTIN.validate(""))
    }

    @Test
    fun validate_blankSpaces_returnsInvalidEmpty() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), GSTIN.validate("   "))
    }

    @Test
    fun validate_tabNewline_returnsInvalidEmpty() {
        assertEquals(ValidationResult.Invalid(InvalidReason.EMPTY), GSTIN.validate("\t\n"))
    }

    // ---------------------------------------------------------------------------
    // validate — WRONG_LENGTH (≥3)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_14chars_returnsInvalidWrongLength() {
        // One char short
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), GSTIN.validate("27AAPFU0939F1Z"))
    }

    @Test
    fun validate_16chars_returnsInvalidWrongLength() {
        // One char too long
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), GSTIN.validate("27AAPFU0939F1ZVX"))
    }

    @Test
    fun validate_5chars_returnsInvalidWrongLength() {
        assertEquals(ValidationResult.Invalid(InvalidReason.WRONG_LENGTH), GSTIN.validate("27AAP"))
    }

    // ---------------------------------------------------------------------------
    // validate — INVALID_FORMAT (≥3)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_specialCharAtEnd_returnsInvalidFormat() {
        // Exclamation mark at position 14 — not a valid base-36 char
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), GSTIN.validate("27AAPFU0939F1Z!"))
    }

    @Test
    fun validate_devanagariDigit_returnsInvalidFormat() {
        // Devanagari '२' (U+0968) at position 1 — not ASCII digit
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), GSTIN.validate("2२AAPFU0939F1ZV"))
    }

    @Test
    fun validate_position13NotZ_returnsInvalidFormat() {
        // Position 13 must be literal 'Z' per GSTN spec; 'A' here fails regex
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), GSTIN.validate("27AAPFU0939F1AV"))
    }

    // ---------------------------------------------------------------------------
    // validate — INVALID_PREFIX (≥5)
    // ---------------------------------------------------------------------------

    @Test
    fun validate_stateCode00_returnsInvalidPrefix() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_PREFIX), GSTIN.validate("00AAPFU0939F1Z6"))
    }

    @Test
    fun validate_stateCode39_returnsInvalidPrefix() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_PREFIX), GSTIN.validate("39AAPFU0939F1Z6"))
    }

    @Test
    fun validate_stateCode40_returnsInvalidPrefix() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_PREFIX), GSTIN.validate("40AAPFU0939F1Z6"))
    }

    @Test
    fun validate_stateCode99_returnsInvalidPrefix() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_PREFIX), GSTIN.validate("99AAPFU0939F1Z6"))
    }

    @Test
    fun validate_stateCodeAB_nonNumericTwoChars_returnsInvalidFormat() {
        // "AB" in state code position — fails regex (first 2 must be digits) → INVALID_FORMAT
        // INVALID_FORMAT is caught before INVALID_PREFIX (per validation order in AC3)
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_FORMAT), GSTIN.validate("ABAAPFU0939F1ZV"))
    }

    // ---------------------------------------------------------------------------
    // validate — INVALID_CATEGORY (≥3): PAN 4th char (position 5 of GSTIN) must be
    // one of P C H A B G J L F T
    // ---------------------------------------------------------------------------

    @Test
    fun validate_pan4thCharZ_returnsInvalidCategory() {
        // Positions 2-11 of GSTIN = "AAAZF0939F" — 4th char of PAN = 'Z' (invalid category)
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_CATEGORY), GSTIN.validate("27AAAZF0939F1ZV"))
    }

    @Test
    fun validate_pan4thCharK_returnsInvalidCategory() {
        // Positions 2-11 = "AAAKF0939F" — 4th char of PAN = 'K' (invalid category)
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_CATEGORY), GSTIN.validate("27AAAKF0939F1ZV"))
    }

    @Test
    fun validate_pan4thCharS_returnsInvalidCategory() {
        // Positions 2-11 = "AAASF0939F" — 4th char of PAN = 'S' (invalid category)
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_CATEGORY), GSTIN.validate("27AAASF0939F1ZV"))
    }

    // ---------------------------------------------------------------------------
    // validate — INVALID_CHECKSUM (≥5): take known-valid, corrupt last char in base-36
    // ---------------------------------------------------------------------------

    @Test
    fun validate_corruptedCheckChar_maharashtra_vToW_returnsInvalidChecksum() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM), GSTIN.validate("27AAPFU0939F1ZW"))
    }

    @Test
    fun validate_corruptedCheckChar_maharashtra_vToU_returnsInvalidChecksum() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM), GSTIN.validate("27AAPFU0939F1ZU"))
    }

    @Test
    fun validate_corruptedCheckChar_karnataka_kToL_returnsInvalidChecksum() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM), GSTIN.validate("29AATFB5356N1ZL"))
    }

    @Test
    fun validate_corruptedCheckChar_jammukashmir_9to0_returnsInvalidChecksum() {
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM), GSTIN.validate("01AAPFU0939F1Z0"))
    }

    @Test
    fun validate_corruptedCheckChar_ladakh_sToT_returnsInvalidChecksum() {
        // Ladakh R3 explicit corrupted vector
        assertEquals(ValidationResult.Invalid(InvalidReason.INVALID_CHECKSUM), GSTIN.validate("38AAPFU0939F1ZT"))
    }

    @Test
    fun validate_allKnownInvalidVectors_returnsInvalidChecksum() {
        for (gstin in GSTINReferenceVectors.KNOWN_INVALID_GSTINS) {
            val result = GSTIN.validate(gstin)
            assertTrue(
                result is ValidationResult.Invalid &&
                    result.reason == InvalidReason.INVALID_CHECKSUM,
                "Expected INVALID_CHECKSUM for $gstin but got $result",
            )
        }
    }

    // ---------------------------------------------------------------------------
    // format() (≥4)
    // ---------------------------------------------------------------------------

    @Test
    fun format_lowercase_returnsUppercase() {
        assertEquals("27AAPFU0939F1ZV", GSTIN.format("27aapfu0939f1zv"))
    }

    @Test
    fun format_whitespacePadded_returnsClean() {
        assertEquals("27AAPFU0939F1ZV", GSTIN.format("  27AAPFU0939F1ZV  "))
    }

    @Test
    fun format_idempotent() {
        val once = GSTIN.format("27AAPFU0939F1ZV")
        val twice = GSTIN.format(once)
        assertEquals(once, twice)
    }

    @Test
    fun format_invalidInput_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            GSTIN.format("invalid")
        }
    }

    @Test
    fun format_invalidPrefix_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            GSTIN.format("00AAPFU0939F1ZV")
        }
    }

    @Test
    fun format_invalidChecksum_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            GSTIN.format("27AAPFU0939F1ZW")
        }
    }

    // ---------------------------------------------------------------------------
    // isValid (≥2)
    // ---------------------------------------------------------------------------

    @Test
    fun isValid_knownValidGstin_returnsTrue() {
        assertTrue(GSTIN.isValid("27AAPFU0939F1ZV"))
    }

    @Test
    fun isValid_ladakh_returnsTrue() {
        // R3 explicit isValid test for state code 38
        assertTrue(GSTIN.isValid("38AAPFU0939F1ZS"))
    }

    @Test
    fun isValid_emptyString_returnsFalse() {
        assertFalse(GSTIN.isValid(""))
    }

    @Test
    fun isValid_corruptedCheckChar_returnsFalse() {
        assertFalse(GSTIN.isValid("27AAPFU0939F1ZW"))
    }
}
