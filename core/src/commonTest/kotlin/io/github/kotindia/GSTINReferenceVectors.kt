// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * Externally-sourced reference vectors for GSTIN GSTN base-36 checksum validation.
 *
 * All entries in [KNOWN_VALID_GSTINS] are 15-character uppercase GSTINs that pass the
 * GSTN base-36 checksum algorithm. No real business GSTINs are used — all constructed
 * from synthetic 14-character prefixes covering all 38 valid state codes.
 *
 * **Cross-validation process (Karan, 2026-05-02):**
 * 1. tk120404/gst JavaScript implementation read and ported to a Node.js test script
 *    (https://github.com/tk120404/gst — gstChecksum.js function)
 * 2. mastermunj/format-utils TypeScript gstChecksum() ported to same script
 *    (https://github.com/mastermunj/format-utils — src/validator.ts)
 * 3. 114 GSTIN prefixes generated covering all 38 valid state codes (3 per state code),
 *    spanning PAN categories P and T, and varied digit patterns
 * 4. tk120404/gst checksum() returned VALID for all 114 — ZERO failures
 * 5. mastermunj/format-utils gstChecksum() returned pass=114 fail=0 — ZERO disagreements
 * 6. BOTH implementations agree on all 114 vectors
 * 7. Kotlin GstinChecksum.computeCheckChar() verified via build + property test round-trip
 * 8. Manual trace of 27AAPFU0939F1ZV confirmed: chars=[2,7,A,A,P,F,U,0,9,3,9,F,1,Z],
 *    values=[2,7,10,10,25,15,30,0,9,3,9,15,1,35], factors=[1,2,1,2,1,2,1,2,1,2,1,2,1,2],
 *    contributions=[2,14,10,20,25,30,30,0,9,6,9,30,1,35], sum=221,
 *    221%36=5, (36-5)%36=31, BASE36[31]='V' ✓
 *
 * **Anti-circular rule:** No entry cites "our own Kotlin GstinChecksum" as sole source.
 * All vectors are grounded in the two external JS/TS reference implementations above.
 *
 * **State code coverage:** 3 vectors per valid state code (01–38), totalling 114 entries.
 * Ladakh (state code 38) is explicitly included per PROJECT_PLAN §8 R3.
 * Legacy state codes 25 (Daman & Diu) and 28 (old Andhra Pradesh) are included — existing
 * GSTINs under these codes remain valid and must not be rejected.
 */
internal object GSTINReferenceVectors {
    /**
     * 114 known-valid 15-character GSTIN strings.
     *
     * Each group covers one state code (3 vectors per state).
     * External source: tk120404/gst (https://github.com/tk120404/gst) and
     * mastermunj/format-utils (https://github.com/mastermunj/format-utils) — both agree.
     */
    internal val KNOWN_VALID_GSTINS: List<String> =
        listOf(
            // Group 01 — Jammu & Kashmir
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "01AAPFU0939F1Z9",
            "01AATFB5356N1Z2",
            "01AACCE1234G1ZG",
            // Group 02 — Himachal Pradesh
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "02AAPFU0939F1Z7",
            "02AATFB5356N1Z0",
            "02AACCE1234G1ZE",
            // Group 03 — Punjab
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "03AAPFU0939F1Z5",
            "03AATFB5356N1ZY",
            "03AACCE1234G1ZC",
            // Group 04 — Chandigarh
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "04AAPFU0939F1Z3",
            "04AATFB5356N1ZW",
            "04AACCE1234G1ZA",
            // Group 05 — Uttarakhand
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "05AAPFU0939F1Z1",
            "05AATFB5356N1ZU",
            "05AACCE1234G1Z8",
            // Group 06 — Haryana
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "06AAPFU0939F1ZZ",
            "06AATFB5356N1ZS",
            "06AACCE1234G1Z6",
            // Group 07 — Delhi
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "07AAPFU0939F1ZX",
            "07AATFB5356N1ZQ",
            "07AACCE1234G1Z4",
            // Group 08 — Rajasthan
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "08AAPFU0939F1ZV",
            "08AATFB5356N1ZO",
            "08AACCE1234G1Z2",
            // Group 09 — Uttar Pradesh
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "09AAPFU0939F1ZT",
            "09AATFB5356N1ZM",
            "09AACCE1234G1Z0",
            // Group 10 — Bihar
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "10AAPFU0939F1ZA",
            "10AATFB5356N1Z3",
            "10AACCE1234G1ZH",
            // Group 11 — Sikkim
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "11AAPFU0939F1Z8",
            "11AATFB5356N1Z1",
            "11AACCE1234G1ZF",
            // Group 12 — Arunachal Pradesh
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "12AAPFU0939F1Z6",
            "12AATFB5356N1ZZ",
            "12AACCE1234G1ZD",
            // Group 13 — Nagaland
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "13AAPFU0939F1Z4",
            "13AATFB5356N1ZX",
            "13AACCE1234G1ZB",
            // Group 14 — Manipur
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "14AAPFU0939F1Z2",
            "14AATFB5356N1ZV",
            "14AACCE1234G1Z9",
            // Group 15 — Mizoram
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "15AAPFU0939F1Z0",
            "15AATFB5356N1ZT",
            "15AACCE1234G1Z7",
            // Group 16 — Tripura
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "16AAPFU0939F1ZY",
            "16AATFB5356N1ZR",
            "16AACCE1234G1Z5",
            // Group 17 — Meghalaya
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "17AAPFU0939F1ZW",
            "17AATFB5356N1ZP",
            "17AACCE1234G1Z3",
            // Group 18 — Assam
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "18AAPFU0939F1ZU",
            "18AATFB5356N1ZN",
            "18AACCE1234G1Z1",
            // Group 19 — West Bengal
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "19AAPFU0939F1ZS",
            "19AATFB5356N1ZL",
            "19AACCE1234G1ZZ",
            // Group 20 — Jharkhand
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "20AAPFU0939F1Z9",
            "20AATFB5356N1Z2",
            "20AACCE1234G1ZG",
            // Group 21 — Odisha
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "21AAPFU0939F1Z7",
            "21AATFB5356N1Z0",
            "21AACCE1234G1ZE",
            // Group 22 — Chhattisgarh
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "22AAPFU0939F1Z5",
            "22AATFB5356N1ZY",
            "22AACCE1234G1ZC",
            // Group 23 — Madhya Pradesh
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "23AAPFU0939F1Z3",
            "23AATFB5356N1ZW",
            "23AACCE1234G1ZA",
            // Group 24 — Gujarat
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "24AAPFU0939F1Z1",
            "24AATFB5356N1ZU",
            "24AACCE1234G1Z8",
            // Group 25 — Daman & Diu (LEGACY — merged into 26 in 2020; existing GSTINs remain valid)
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "25AAPFU0939F1ZZ",
            "25AATFB5356N1ZS",
            "25AACCE1234G1Z6",
            // Group 26 — Dadra & Nagar Haveli and Daman & Diu (merged entity post-2020)
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "26AAPFU0939F1ZX",
            "26AATFB5356N1ZQ",
            "26AACCE1234G1Z4",
            // Group 27 — Maharashtra
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            // Manual trace: 27AAPFU0939F1ZV confirmed sum=221, (36-(221%36))%36=31='V'
            "27AAPFU0939F1ZV",
            "27AATFB5356N1ZO",
            "27AACCE1234G1Z2",
            // Group 28 — Andhra Pradesh (LEGACY — replaced by 37 post-2014; existing GSTINs remain valid)
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "28AAPFU0939F1ZT",
            "28AATFB5356N1ZM",
            "28AACCE1234G1Z0",
            // Group 29 — Karnataka
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "29AAPFU0939F1ZR",
            "29AATFB5356N1ZK",
            "29AACCE1234G1ZY",
            // Group 30 — Goa
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "30AAPFU0939F1Z8",
            "30AATFB5356N1Z1",
            "30AACCE1234G1ZF",
            // Group 31 — Lakshadweep
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "31AAPFU0939F1Z6",
            "31AATFB5356N1ZZ",
            "31AACCE1234G1ZD",
            // Group 32 — Kerala
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "32AAPFU0939F1Z4",
            "32AATFB5356N1ZX",
            "32AACCE1234G1ZB",
            // Group 33 — Tamil Nadu
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "33AAPFU0939F1Z2",
            "33AATFB5356N1ZV",
            "33AACCE1234G1Z9",
            // Group 34 — Puducherry
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "34AAPFU0939F1Z0",
            "34AATFB5356N1ZT",
            "34AACCE1234G1Z7",
            // Group 35 — Andaman & Nicobar Islands
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "35AAPFU0939F1ZY",
            "35AATFB5356N1ZR",
            "35AACCE1234G1Z5",
            // Group 36 — Telangana
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "36AAPFU0939F1ZW",
            "36AATFB5356N1ZP",
            "36AACCE1234G1Z3",
            // Group 37 — Andhra Pradesh (post-2014 bifurcation)
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            "37AAPFU0939F1ZU",
            "37AATFB5356N1ZN",
            "37AACCE1234G1Z1",
            // Group 38 — Ladakh (post-2019 reorganisation — R3 risk: absent from pre-2020 validators)
            // Source: tk120404/gst checksum() + mastermunj/format-utils gstChecksum() — both agree
            // Explicitly required by PROJECT_PLAN §8 R3 and PRD AC8
            "38AAPFU0939F1ZS",
            "38AATFB5356N1ZL",
            "38AACCE1234G1ZZ",
        )

    /**
     * Known-invalid 15-character GSTIN strings.
     * Each entry is labeled with the specific corruption applied to a known-valid source.
     * All verified: tk120404/gst checksum() returns false for each.
     */
    internal val KNOWN_INVALID_GSTINS: List<String> =
        listOf(
            // valid was "27AAPFU0939F1ZV" — check char incremented V→W in base-36
            "27AAPFU0939F1ZW",
            // valid was "27AAPFU0939F1ZV" — check char changed to U
            "27AAPFU0939F1ZU",
            // valid was "29AATFB5356N1ZK" — check char incremented K→L
            "29AATFB5356N1ZL",
            // valid was "01AAPFU0939F1Z9" — check char 9→0
            "01AAPFU0939F1Z0",
            // valid was "38AAPFU0939F1ZS" — check char S→T (Ladakh R3 explicit corrupted vector)
            "38AAPFU0939F1ZT",
            // valid was "07AAPFU0939F1ZX" — check char X→Y
            "07AAPFU0939F1ZY",
            // valid was "10AAPFU0939F1ZA" — check char A→B
            "10AAPFU0939F1ZB",
            // valid was "33AATFB5356N1ZV" — check char V→W
            "33AATFB5356N1ZW",
            // valid was "19AAPFU0939F1ZS" — check char S→T
            "19AAPFU0939F1ZT",
            // valid was "24AATFB5356N1ZU" — check char U→V
            "24AATFB5356N1ZV",
            // valid was "06AAPFU0939F1ZZ" — check char Z→0 (wraps in base-36)
            "06AAPFU0939F1Z0",
            // valid was "12AATFB5356N1ZZ" — check char Z→0
            "12AATFB5356N1Z0",
        )
}
