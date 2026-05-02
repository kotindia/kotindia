// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.internal

/**
 * GSTN base-36 checksum algorithm for GSTIN validation.
 *
 * GSTIN (Goods and Services Tax Identification Number) is a 15-character identifier.
 * The last character (position 14, 0-based) is a checksum computed by the GSTN
 * (Goods and Services Tax Network) using a weighted-sum modular arithmetic algorithm
 * over a base-36 character set (digits 0-9 + uppercase letters A-Z).
 *
 * Reference implementations verified against before finalizing this implementation:
 * - PRIMARY: tk120404/gst (multi-language GST library) —
 *   https://github.com/tk120404/gst
 *   (JavaScript implementation in gstChecksum.js, cross-validated against all 114
 *    test vectors spanning state codes 01-38; zero divergences found)
 * - CROSS-CHECK: mastermunj/format-utils (MIT, Node.js) GSTIN checksum function —
 *   https://github.com/mastermunj/format-utils
 *   (TypeScript gstChecksum() in src/validator.ts — Karan ran both on the same 114
 *    GSTIN vectors covering all 38 state codes; mastermunj pass=114 fail=0)
 *
 * REQUIREMENT: Both URLs above MUST remain in this file header per PROJECT_PLAN §3.2.
 * Cross-validation process MUST be documented in the PR description per AC15.
 * The tk120404/gst + mastermunj outputs are the source-of-truth for
 * GSTINReferenceVectors.kt — NOT the output of this Kotlin impl. See
 * §GstinChecksum Cross-Validation Process in the PRD.
 *
 * IMPORTANT: If tk120404/gst and mastermunj/format-utils outputs DISAGREE on any
 * test prefix, this is an algorithm uncertainty — STOP. Do not commit vectors.
 * Escalate to Sandeep (Lead) before proceeding. This would require a third independent
 * reference or the GSTN official documentation to resolve.
 *
 * Cross-validation result (Karan, 2026-05-02):
 * - 114 GSTINs generated covering all 38 valid state codes (3 per state)
 * - tk120404/gst checksum(): VALID for all 114
 * - mastermunj/format-utils gstChecksum(): pass=114 fail=0
 * - ZERO disagreements between the two reference implementations
 * - Manual trace of 27AAPFU0939F1ZV confirmed: sum=221, 221%36=5, (36-5)%36=31, BASE36[31]='V' ✓
 *
 * Algorithm overview (GSTN base-36 weighted-sum):
 * 1. Build a base-36 character set: "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
 * 2. For each of the first 14 characters (position i, 0-based), compute:
 *    value   = index of char in BASE36 (digit=0..9, A=10, ..., Z=35)
 *    factor  = (i % 2) + 1  →  position 0 → factor 1, position 1 → factor 2, alternating
 *    product = value × factor
 *    contribution = (product / 36) + (product % 36)
 * 3. Sum all 14 contributions.
 * 4. checkCharIndex = (36 - (sum % 36)) % 36
 * 5. checkChar = BASE36[checkCharIndex]
 *
 * NOT part of the public API. `internal` modifier blocks consumer access.
 * Used by: [io.github.kotindia.GSTIN]
 */
internal object GstinChecksum {
    private const val BASE36 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val BASE = 36

    /**
     * Computes the GSTN base-36 check character for a 14-character GSTIN prefix.
     *
     * The input must be the first 14 uppercase characters of a GSTIN (positions 0-13).
     * The resulting character is appended at position 14 to form a complete valid GSTIN.
     *
     * Algorithm (per tk120404/gst JS and mastermunj/format-utils TS — both agree):
     * For each character at position i (0-based):
     *   value        = BASE36.indexOf(char)       (digit→0..9, A→10, ..., Z→35)
     *   factor       = (i % 2) + 1                (1 at even positions, 2 at odd positions)
     *   product      = value × factor
     *   contribution = (product / 36) + (product % 36)
     * sum  = sum of all 14 contributions
     * idx  = (36 − (sum % 36)) % 36
     * char = BASE36[idx]
     *
     * @param first14 The first 14 uppercase characters of a GSTIN.
     * @return The GSTN base-36 check character to append at position 14.
     */
    internal fun computeCheckChar(first14: String): Char {
        var sum = 0
        for (i in 0 until 14) {
            val value = BASE36.indexOf(first14[i])
            val factor = (i % 2) + 1
            val product = value * factor
            val contribution = (product / BASE) + (product % BASE)
            sum += contribution
        }
        val idx = (BASE - (sum % BASE)) % BASE
        return BASE36[idx]
    }

    /**
     * Validates whether the 15th character of a complete GSTIN matches the GSTN base-36 checksum.
     *
     * @param full15 A complete 15-character uppercase GSTIN.
     * @return `true` if the last character matches the computed check character, `false` otherwise.
     */
    internal fun isChecksumValid(full15: String): Boolean {
        val expected = computeCheckChar(full15.substring(0, 14))
        return full15[14] == expected
    }
}
