// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.internal

/**
 * Luhn checksum algorithm (mod-10).
 *
 * The Luhn algorithm (also known as the "modulus 10" or "mod 10" algorithm) was
 * developed by IBM scientist Hans Peter Luhn. It is used to validate IMEI numbers,
 * credit card numbers, and other identification numbers.
 *
 * Reference implementations verified against before finalizing this implementation:
 * - PRIMARY: Wikipedia Luhn algorithm — https://en.wikipedia.org/wiki/Luhn_algorithm
 *   (mod-10 algorithm specification + worked example for "79927398713" → valid)
 * - CROSS-CHECK: npm `luhn` package (MIT, https://www.npmjs.com/package/luhn)
 *   (Karan ran npm luhn against all 110 14-digit IMEI prefixes; luhn.validate() used as
 *    external ground-truth verifier for IMEIReferenceVectors.kt — NOT the output of this
 *    Kotlin impl. Zero divergences found.)
 *
 * REQUIREMENT: Both URLs above MUST remain in this file header. Cross-validation
 * process MUST be documented in the PR description per AC15. The npm `luhn` validate()
 * output is the source-of-truth verifier for IMEIReferenceVectors.kt — NOT this Kotlin
 * impl. See §Luhn Cross-Validation Process in the PRD.
 *
 * Karan's cross-validation process (2026-05-02):
 * 1. Read Wikipedia Luhn algorithm specification — transcribed algorithm into Kotlin
 * 2. Verified Wikipedia worked example: computeCheckDigit("7992739871") == 3,
 *    isChecksumValid("79927398713") == true, isChecksumValid("79927398710") == false
 * 3. Installed npm `luhn` (MIT) locally; used luhn.validate() as independent verifier
 * 4. Generated 110 14-digit IMEI prefixes (10 groups of 10 + 10 varied extra)
 * 5. computeCheckDigit() run on each prefix → 15-digit IMEI; ALL passed luhn.validate()
 * 6. Zero divergences between Kotlin computeCheckDigit() and npm luhn.validate()
 * 7. 12 invalid vectors (last digit +1 mod 10) also verified: luhn.validate() = false for all
 *
 * NOT part of the public API. `internal` modifier blocks consumer access.
 * Used by: [io.github.kotindia.IMEI]
 */
internal object Luhn {
    /**
     * Computes the Luhn check digit for a numeric string WITHOUT the check digit.
     *
     * Algorithm (from Wikipedia https://en.wikipedia.org/wiki/Luhn_algorithm):
     * 1. Append a placeholder '0' to form the full number with check digit at position 0.
     * 2. Double every second digit from the right (positions 1, 3, 5… counting from right, 0-based).
     * 3. If doubling produces a value > 9, subtract 9.
     * 4. Sum all digits.
     * 5. Check digit = (10 - (sum % 10)) % 10.
     *
     * @param digits A non-empty numeric string WITHOUT the check digit (e.g. 14 digits for IMEI).
     * @return The Luhn check digit (0–9) to append to [digits].
     */
    internal fun computeCheckDigit(digits: String): Int {
        // Build the number with a placeholder check digit ('0') at the rightmost position.
        val full = digits + "0"
        val len = full.length
        var sum = 0
        for (i in full.indices) {
            var d = full[i].digitToInt()
            // Position from right: 0 = rightmost (placeholder check digit position).
            val posFromRight = len - 1 - i
            // Double every second digit from right, starting at position 1.
            if (posFromRight % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return (10 - (sum % 10)) % 10
    }

    /**
     * Validates whether [digitsWithChecksum] passes the Luhn checksum.
     *
     * Standard Luhn validation: the total sum (with doubling on even positions from right,
     * 0-based, starting from position 1) must be divisible by 10.
     *
     * @param digitsWithChecksum A numeric string whose last character is the Luhn check digit.
     * @return `true` if the checksum is valid, `false` otherwise.
     */
    internal fun isChecksumValid(digitsWithChecksum: String): Boolean {
        val len = digitsWithChecksum.length
        var sum = 0
        for (i in digitsWithChecksum.indices) {
            var d = digitsWithChecksum[i].digitToInt()
            // Position from right: 0 = rightmost (the check digit position).
            val posFromRight = len - 1 - i
            // Double every second digit from right, starting at position 1.
            if (posFromRight % 2 == 1) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
        }
        return sum % 10 == 0
    }
}
