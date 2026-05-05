// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.demo.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kotindia.Aadhaar
import io.github.kotindia.GSTIN
import io.github.kotindia.IMEI
import io.github.kotindia.demo.components.ValidatorCard
import io.github.kotindia.demo.components.ValidatorCardSpec

/**
 * AlgorithmTab — showcases the three validators that use a mathematical checksum.
 *
 * When the result is INVALID_CHECKSUM, ValidatorCard displays the checksumExplanation
 * text so the developer understands exactly what the algorithm detected.
 *
 * Sample inputs provided:
 *   - Aadhaar: one valid + one invalid (last digit corrupted)
 *   - IMEI: one valid + one invalid (last digit corrupted)
 *   - GSTIN: one valid + one invalid (checksum char changed)
 */
@Composable
internal fun AlgorithmTab(contentPadding: PaddingValues = PaddingValues(0.dp)) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Algorithm Showcase",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Enter a valid or deliberately corrupted value. " +
                "When the checksum fails, the card explains why.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // --- Aadhaar / Verhoeff ---
        ValidatorCard(
            spec = ValidatorCardSpec(
                title = "Aadhaar — Verhoeff",
                formatHint = "12 digits, prefix 2-9. Last digit is the Verhoeff D5 check digit.",
                // 234567890124 = valid; 234567890125 = last digit corrupted
                sampleInputs = listOf("234567890124", "234567890125"),
                validate = Aadhaar::validate,
                format = { Aadhaar.format(it) },
                mask = { Aadhaar.mask(it) },
                checksumExplanation = "Verhoeff check digit mismatch — " +
                    "the last digit does not match the computed Verhoeff D5 value " +
                    "over the first 11 digits. This catches transpositions " +
                    "(e.g. swapping adjacent digits) and single-digit errors.",
            ),
        )

        // --- IMEI / Luhn ---
        ValidatorCard(
            spec = ValidatorCardSpec(
                title = "IMEI — Luhn (mod-10)",
                formatHint = "15 digits. Last digit is the Luhn mod-10 check digit.",
                // 490154203237518 = valid; 490154203237519 = last digit corrupted
                sampleInputs = listOf("490154203237518", "490154203237519"),
                validate = IMEI::validate,
                format = { IMEI.format(it) },
                mask = { IMEI.mask(it) },
                checksumExplanation = "Luhn mod-10 check failed — " +
                    "the last digit does not match the computed Luhn value. " +
                    "Luhn doubles every second digit (right-to-left), " +
                    "subtracts 9 from values >9, sums all digits, " +
                    "and the total must be divisible by 10.",
            ),
        )

        // --- GSTIN / GSTN base-36 ---
        ValidatorCard(
            spec = ValidatorCardSpec(
                title = "GSTIN — GSTN base-36",
                formatHint = "15 chars. Last char is the GSTN weighted base-36 checksum.",
                // 27AAPFU0939F1ZV = valid; 27AAPFU0939F1ZX = checksum char corrupted
                sampleInputs = listOf("27AAPFU0939F1ZV", "27AAPFU0939F1ZX"),
                validate = GSTIN::validate,
                format = { GSTIN.format(it) },
                mask = null,
                checksumExplanation = "GSTN base-36 checksum mismatch — " +
                    "the character at position 15 does not match the computed checksum. " +
                    "The GSTN algorithm maps each of the first 14 chars to a base-36 value, " +
                    "applies a weighted sum, and takes the result mod 36 to derive " +
                    "the expected check character.",
            ),
        )
    }
}
