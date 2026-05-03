// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * The result of a KotIndia validator call.
 *
 * Every validator in `io.github.kotindia` returns a [ValidationResult].
 * Use [Valid] to check success and [Invalid] to retrieve the failure reason.
 *
 * Callers should use a sealed `when` expression to branch on the result:
 *
 * ```kotlin
 * val result = Mobile.validate("9876543210")
 * when (result) {
 *     is ValidationResult.Valid -> println("Valid input")
 *     is ValidationResult.Invalid -> println("Invalid: ${result.reason}")
 * }
 * ```
 *
 * Or use the convenience `isValid` shorthand (available on each validator object):
 *
 * ```kotlin
 * val ok: Boolean = Mobile.validate("9876543210") is ValidationResult.Valid
 * ```
 *
 * Validators in this library **never throw**. All error paths are represented as
 * [Invalid] with an appropriate [InvalidReason].
 *
 * @sample io.github.kotindia.samples.validationResultSample
 */
public sealed interface ValidationResult {
    /**
     * The input passed all format and checksum checks.
     *
     * This is a `data object`, so it can be compared with `==` and used in `when` branches
     * without an additional type-cast.
     */
    public data object Valid : ValidationResult

    /**
     * The input failed validation.
     *
     * @param reason Why the input is invalid. See [InvalidReason] for all possible values.
     *
     * @throws Nothing validators in this library never throw.
     */
    public data class Invalid(
        public val reason: InvalidReason,
    ) : ValidationResult
}
