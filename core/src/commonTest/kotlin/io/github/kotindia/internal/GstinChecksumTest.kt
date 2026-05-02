// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.internal

import io.github.kotindia.GSTINReferenceVectors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [GstinChecksum].
 *
 * All expected check characters are sourced from tk120404/gst (https://github.com/tk120404/gst)
 * and cross-validated with mastermunj/format-utils (https://github.com/mastermunj/format-utils).
 * Zero disagreements found between both reference implementations across all 114 test vectors.
 *
 * Anti-circular: expected values are NOT derived from our own Kotlin GstinChecksum output.
 * They are taken directly from the external JS/TS reference implementations.
 */
internal class GstinChecksumTest {
    // --- computeCheckChar: known vectors from tk120404/gst cross-validated with mastermunj ---

    @Test
    fun computeCheckChar_maharashtra_aapfu() {
        // 27AAPFU0939F1ZV — manual trace confirmed: sum=221, (36-(221%36))%36=31='V'
        // Source: tk120404/gst JS + mastermunj TS both return VALID for 27AAPFU0939F1ZV
        assertEquals('V', GstinChecksum.computeCheckChar("27AAPFU0939F1Z"))
    }

    @Test
    fun computeCheckChar_karnataka_aatfb() {
        // 29AATFB5356N1ZK — source: tk120404/gst + mastermunj both agree
        assertEquals('K', GstinChecksum.computeCheckChar("29AATFB5356N1Z"))
    }

    @Test
    fun computeCheckChar_jammukashmir_aapfu() {
        // 01AAPFU0939F1Z9 — source: tk120404/gst + mastermunj both agree
        assertEquals('9', GstinChecksum.computeCheckChar("01AAPFU0939F1Z"))
    }

    @Test
    fun computeCheckChar_ladakh_aapfu() {
        // 38AAPFU0939F1ZS — R3 explicit test (Ladakh, state code 38, post-2019)
        // Source: tk120404/gst + mastermunj both agree
        assertEquals('S', GstinChecksum.computeCheckChar("38AAPFU0939F1Z"))
    }

    @Test
    fun computeCheckChar_ladakh_aatfb() {
        // 38AATFB5356N1ZL — R3 second Ladakh vector
        // Source: tk120404/gst + mastermunj both agree
        assertEquals('L', GstinChecksum.computeCheckChar("38AATFB5356N1Z"))
    }

    @Test
    fun computeCheckChar_ladakh_aacce() {
        // 38AACCE1234G1ZZ — R3 third Ladakh vector
        // Source: tk120404/gst + mastermunj both agree
        assertEquals('Z', GstinChecksum.computeCheckChar("38AACCE1234G1Z"))
    }

    @Test
    fun computeCheckChar_rajasthan_aapfu() {
        // 08AAPFU0939F1ZV — source: tk120404/gst + mastermunj both agree
        assertEquals('V', GstinChecksum.computeCheckChar("08AAPFU0939F1Z"))
    }

    @Test
    fun computeCheckChar_delhi_aapfu() {
        // 07AAPFU0939F1ZX — source: tk120404/gst + mastermunj both agree
        assertEquals('X', GstinChecksum.computeCheckChar("07AAPFU0939F1Z"))
    }

    @Test
    fun computeCheckChar_tamilnadu_aatfb() {
        // 33AATFB5356N1ZV — source: tk120404/gst + mastermunj both agree
        assertEquals('V', GstinChecksum.computeCheckChar("33AATFB5356N1Z"))
    }

    @Test
    fun computeCheckChar_kerala_aapfu() {
        // 32AAPFU0939F1Z4 — source: tk120404/gst + mastermunj both agree
        assertEquals('4', GstinChecksum.computeCheckChar("32AAPFU0939F1Z"))
    }

    @Test
    fun computeCheckChar_gujarat_aapfu() {
        // 24AAPFU0939F1Z1 — source: tk120404/gst + mastermunj both agree
        assertEquals('1', GstinChecksum.computeCheckChar("24AAPFU0939F1Z"))
    }

    @Test
    fun computeCheckChar_legacyDamanDiu_aapfu() {
        // 25AAPFU0939F1ZZ — legacy state code 25, source: tk120404/gst + mastermunj both agree
        assertEquals('Z', GstinChecksum.computeCheckChar("25AAPFU0939F1Z"))
    }

    @Test
    fun computeCheckChar_legacyAndhraPradesh_aapfu() {
        // 28AAPFU0939F1ZT — legacy state code 28, source: tk120404/gst + mastermunj both agree
        assertEquals('T', GstinChecksum.computeCheckChar("28AAPFU0939F1Z"))
    }

    // --- isChecksumValid: known-valid vectors ---

    @Test
    fun isChecksumValid_maharashtra_aapfu_returnsTrue() {
        // Source: tk120404/gst + mastermunj both validate this as VALID
        assertTrue(GstinChecksum.isChecksumValid("27AAPFU0939F1ZV"))
    }

    @Test
    fun isChecksumValid_karnataka_aatfb_returnsTrue() {
        // Source: tk120404/gst + mastermunj both agree
        assertTrue(GstinChecksum.isChecksumValid("29AATFB5356N1ZK"))
    }

    @Test
    fun isChecksumValid_ladakh_aapfu_returnsTrue() {
        // R3 explicit: Ladakh state code 38
        assertTrue(GstinChecksum.isChecksumValid("38AAPFU0939F1ZS"))
    }

    @Test
    fun isChecksumValid_allKnownValidVectors() {
        // All 114 vectors must pass — any failure = algorithm regression
        for (gstin in GSTINReferenceVectors.KNOWN_VALID_GSTINS) {
            assertTrue(GstinChecksum.isChecksumValid(gstin), "Expected valid: $gstin")
        }
    }

    // --- isChecksumValid: known-invalid vectors ---

    @Test
    fun isChecksumValid_corruptedCheckChar_maharashtra_returnsFalse() {
        // valid was 27AAPFU0939F1ZV, corrupted V→W
        assertFalse(GstinChecksum.isChecksumValid("27AAPFU0939F1ZW"))
    }

    @Test
    fun isChecksumValid_corruptedCheckChar_maharashtra_u_returnsFalse() {
        // valid was 27AAPFU0939F1ZV, corrupted V→U
        assertFalse(GstinChecksum.isChecksumValid("27AAPFU0939F1ZU"))
    }

    @Test
    fun isChecksumValid_allKnownInvalidVectors() {
        for (gstin in GSTINReferenceVectors.KNOWN_INVALID_GSTINS) {
            assertFalse(GstinChecksum.isChecksumValid(gstin), "Expected invalid: $gstin")
        }
    }
}
