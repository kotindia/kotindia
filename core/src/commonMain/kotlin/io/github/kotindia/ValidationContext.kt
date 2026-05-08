// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia

/**
 * Structured, type-safe context attached to every [ProgressiveResult.Invalid].
 *
 * Carries diagnostic parameters — never strings — so consumers can render
 * their own copy in any language with full parameter access.
 *
 * Slice 14 ships the minimal set: [None] and [LengthMismatch]. Slice 15 adds
 * `CharacterAt`, `ChecksumMismatch`, `PrefixMismatch`, and `CategoryMismatch`.
 */
public sealed interface ValidationContext {
    /**
     * No additional context. Default for [ProgressiveResult.Invalid] when no
     * structured parameters are meaningful (e.g., bad-character cases in slice 14).
     */
    public data object None : ValidationContext

    /**
     * The input length did not match the expected length.
     *
     * @param expected The validator's required length.
     * @param actual The length of the input after normalisation.
     */
    public data class LengthMismatch(
        public val expected: Int,
        public val actual: Int,
    ) : ValidationContext
}
