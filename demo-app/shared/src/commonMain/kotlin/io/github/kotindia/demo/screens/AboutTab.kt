// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.demo.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AboutTab(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
    ) {
        Text("KotIndia", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Version 0.1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Kotlin Multiplatform Indian validator library",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(4.dp))

        // GitHub
        ListItem(
            headlineContent = { Text("GitHub") },
            supportingContent = { Text("github.com/kotindia/kotindia") },
            trailingContent = { Text("->", style = MaterialTheme.typography.labelSmall) },
        )
        HorizontalDivider()

        // Maven Central
        ListItem(
            headlineContent = { Text("Maven Central") },
            supportingContent = { Text("central.sonatype.com/artifact/io.github.kotindia/core") },
            trailingContent = { Text("->", style = MaterialTheme.typography.labelSmall) },
        )
        HorizontalDivider()

        // Docs
        ListItem(
            headlineContent = { Text("Docs") },
            supportingContent = { Text("kotindia.github.io/kotindia") },
            trailingContent = { Text("->", style = MaterialTheme.typography.labelSmall) },
        )
        HorizontalDivider()

        // License
        ListItem(
            headlineContent = { Text("License") },
            supportingContent = { Text("Apache License 2.0") },
        )
        HorizontalDivider()

        Spacer(modifier = Modifier.height(20.dp))

        // --- Dark / Light toggle ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (darkTheme) "Dark mode" else "Light mode",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = darkTheme,
                onCheckedChange = { onToggleTheme() },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "KotIndia provides format and checksum validation for Indian system codes. " +
                "No network calls. No third-party dependencies. Pure Kotlin Multiplatform.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
