// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.internal

/**
 * Verhoeff checksum algorithm using the dihedral group D5.
 *
 * Implements the three lookup tables (d, p, inv) and the two core functions:
 * - [computeCheckDigit]: given an n-digit numeric string, returns the Verhoeff check digit
 * - [isValid]: given an (n+1)-digit string with check digit appended, returns true iff valid
 *
 * Reference implementations verified against before finalizing these tables:
 * - PRIMARY: Wikipedia Verhoeff algorithm — https://en.wikipedia.org/wiki/Verhoeff_algorithm
 *   (D5 multiplication table, permutation table, inverse table — published worked examples verified)
 * - CROSS-CHECK: mastermunj/format-utils (MIT, Node.js) Verhoeff implementation —
 *   https://github.com/mastermunj/format-utils/blob/master/src/verhoeff.ts
 *   (Karan ran both on the same 110-vector set; all outputs matched before committing these tables)
 *
 * REQUIREMENT: Both URLs above MUST remain in this file header. Cross-validation script
 * (or instructions to reproduce) MUST be in the PR description per AC14. The mastermunj
 * output is the source-of-truth for AadhaarReferenceVectors.kt Group A vectors — NOT
 * the output of this Kotlin impl. See §Verhoeff Cross-Validation Process in the PRD.
 *
 * Karan's cross-validation process (2026-04-29):
 * 1. Transcribed d, p, inv tables manually from Wikipedia D5 spec
 * 2. Ported mastermunj/format-utils Verhoeff JS function to a scratch Node.js script
 * 3. Generated 110 valid 12-digit Aadhaar numbers from 11-digit prefixes; all passed
 *    JS isValid() in the same script confirming JS-side correctness
 * 4. All 110 check digits verified by both JS impl and these Kotlin tables before any
 *    test vectors were committed — see AadhaarReferenceVectors.kt source comments
 * 5. Property tests (round-trip + adjacent-transposition) further confirm table correctness
 *
 * NOT part of the public API. `internal` modifier blocks consumer access.
 * Used by: [io.github.kotindia.Aadhaar], [io.github.kotindia.AadhaarVID]
 */
internal object Verhoeff {
    // -----------------------------------------------------------------------------------
    // D5 dihedral group multiplication table (d)
    // Source: Wikipedia Verhoeff algorithm — "Multiplication table" section
    // https://en.wikipedia.org/wiki/Verhoeff_algorithm
    // Row i, column j → d[i][j] = product of i and j in D5
    // -----------------------------------------------------------------------------------
    private val d: Array<IntArray> =
        arrayOf(
            // Row 0 (identity element)
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
            // Row 1
            intArrayOf(1, 2, 3, 4, 0, 6, 7, 8, 9, 5),
            // Row 2
            intArrayOf(2, 3, 4, 0, 1, 7, 8, 9, 5, 6),
            // Row 3
            intArrayOf(3, 4, 0, 1, 2, 8, 9, 5, 6, 7),
            // Row 4
            intArrayOf(4, 0, 1, 2, 3, 9, 5, 6, 7, 8),
            // Row 5
            intArrayOf(5, 9, 8, 7, 6, 0, 4, 3, 2, 1),
            // Row 6
            intArrayOf(6, 5, 9, 8, 7, 1, 0, 4, 3, 2),
            // Row 7
            intArrayOf(7, 6, 5, 9, 8, 2, 1, 0, 4, 3),
            // Row 8
            intArrayOf(8, 7, 6, 5, 9, 3, 2, 1, 0, 4),
            // Row 9
            intArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0),
        )

    // -----------------------------------------------------------------------------------
    // Permutation table (p)
    // Source: Wikipedia Verhoeff algorithm — "Permutation table" section
    // https://en.wikipedia.org/wiki/Verhoeff_algorithm
    // p[pos % 8][digit] → permuted digit value at position pos (counting from right, 0-based)
    // -----------------------------------------------------------------------------------
    private val p: Array<IntArray> =
        arrayOf(
            // Position 0 (identity)
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
            // Position 1
            intArrayOf(1, 5, 7, 6, 2, 8, 3, 0, 9, 4),
            // Position 2
            intArrayOf(5, 8, 0, 3, 7, 9, 6, 1, 4, 2),
            // Position 3
            intArrayOf(8, 9, 1, 6, 0, 4, 3, 5, 2, 7),
            // Position 4
            intArrayOf(9, 4, 5, 3, 1, 2, 6, 8, 7, 0),
            // Position 5
            intArrayOf(4, 2, 8, 6, 5, 7, 3, 9, 0, 1),
            // Position 6
            intArrayOf(2, 7, 9, 3, 8, 0, 6, 4, 1, 5),
            // Position 7
            intArrayOf(7, 0, 4, 6, 9, 1, 3, 2, 5, 8),
        )

    // -----------------------------------------------------------------------------------
    // Inverse table (inv)
    // Source: Wikipedia Verhoeff algorithm — "Inverse table" section
    // https://en.wikipedia.org/wiki/Verhoeff_algorithm
    // inv[i] → the inverse of i in D5 (i.e., d[i][inv[i]] == 0)
    // -----------------------------------------------------------------------------------
    private val inv: IntArray = intArrayOf(0, 4, 3, 2, 1, 5, 6, 7, 8, 9)

    /**
     * Computes the Verhoeff check digit for the given numeric prefix.
     *
     * @param digits A non-empty numeric string (the prefix WITHOUT the check digit).
     * @return The Verhoeff check digit character ('0'..'9') to append to [digits].
     */
    internal fun computeCheckDigit(digits: String): Char {
        var c = 0
        // Process from right-to-left; check digit position is 0, so prefix starts at 1
        for (i in digits.indices.reversed()) {
            val digit = digits[i].digitToInt()
            val pos = digits.length - i // position counting from right, starting at 1
            c = d[c][p[pos % 8][digit]]
        }
        return inv[c].digitToChar()
    }

    /**
     * Validates whether [digitsWithChecksum] passes the Verhoeff checksum.
     *
     * @param digitsWithChecksum A numeric string whose last character is the Verhoeff check digit.
     * @return `true` if the checksum is valid, `false` otherwise.
     */
    internal fun isValid(digitsWithChecksum: String): Boolean {
        var c = 0
        // Process from right-to-left; position 0 = rightmost = check digit position
        for (i in digitsWithChecksum.indices.reversed()) {
            val digit = digitsWithChecksum[i].digitToInt()
            val pos = digitsWithChecksum.length - 1 - i
            c = d[c][p[pos % 8][digit]]
        }
        return c == 0
    }
}
