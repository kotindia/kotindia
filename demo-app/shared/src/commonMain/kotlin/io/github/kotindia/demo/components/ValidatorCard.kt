// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.demo.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.kotindia.ValidationResult

/**
 * Spec for a single validator card in the demo.
 *
 * All fields internal — no public API surface (demo is not a library).
 *
 * @property title Short name shown as card heading (e.g. "Mobile").
 * @property formatHint Describes expected input format, shown below the title.
 * @property sampleInputs Clickable sample chips; each fills the input field.
 * @property validate Delegate to the core validator's validate() function.
 * @property format Delegate to format() — may throw on invalid input; ValidatorCard catches.
 * @property mask Delegate to mask() with default params, or null if no mask method exists.
 * @property checksumExplanation Optional detailed text shown when result is INVALID_CHECKSUM.
 */
internal data class ValidatorCardSpec(
    val title: String,
    val formatHint: String,
    val sampleInputs: List<String>,
    val validate: (String) -> ValidationResult,
    val format: (String) -> String,
    val mask: ((String) -> String)? = null,
    val checksumExplanation: String? = null,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ValidatorCard(spec: ValidatorCardSpec, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    var showMask by remember { mutableStateOf(false) }

    val result = remember(input) { spec.validate(input) }
    val isValid = result is ValidationResult.Valid

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- Title + format hint ---
            Text(spec.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                spec.formatHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- Sample chips ---
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                spec.sampleInputs.forEach { sample ->
                    AssistChip(
                        onClick = { input = sample },
                        label = { Text(sample, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // --- Input field ---
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Enter ${spec.title}") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    autoCorrect = false,
                ),
            )

            Spacer(modifier = Modifier.height(10.dp))

            // --- Result chip ---
            ResultChip(result)

            // --- Checksum explanation (Algorithm tab use) ---
            if (result is ValidationResult.Invalid &&
                result.reason == io.github.kotindia.InvalidReason.INVALID_CHECKSUM &&
                spec.checksumExplanation != null
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = spec.checksumExplanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // --- Format preview (shown when valid) ---
            if (isValid && input.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val formatted = remember(input) {
                    runCatching { spec.format(input) }.getOrNull()
                }
                if (formatted != null) {
                    Text(
                        text = "Format: $formatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // --- Mask preview + toggle (PII only) ---
            if (spec.mask != null && isValid && input.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val maskedValue = remember(input) {
                        runCatching { spec.mask.invoke(input) }.getOrNull() ?: ""
                    }
                    Text(
                        text = if (showMask) "Mask: $maskedValue" else "Mask: hidden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = showMask,
                        onCheckedChange = { showMask = it },
                    )
                }
            }
        }
    }
}
