// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.demo.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.kotindia.InvalidReason
import io.github.kotindia.ValidationResult

@Composable
internal fun ResultChip(result: ValidationResult) {
    when (result) {
        is ValidationResult.Valid -> AssistChip(
            onClick = {},
            label = { Text("Valid", style = MaterialTheme.typography.labelMedium) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
        is ValidationResult.Invalid -> AssistChip(
            onClick = {},
            label = {
                Text(
                    text = "Invalid — ${result.reason.label}",
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                labelColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        )
    }
}

internal val InvalidReason.label: String
    get() = when (this) {
        InvalidReason.EMPTY -> "EMPTY"
        InvalidReason.WRONG_LENGTH -> "WRONG_LENGTH"
        InvalidReason.INVALID_FORMAT -> "INVALID_FORMAT"
        InvalidReason.INVALID_CHECKSUM -> "INVALID_CHECKSUM"
        InvalidReason.INVALID_PREFIX -> "INVALID_PREFIX"
        InvalidReason.INVALID_CATEGORY -> "INVALID_CATEGORY"
    }
