// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.kotindia.demo.screens.AboutTab
import io.github.kotindia.demo.screens.AlgorithmTab
import io.github.kotindia.demo.screens.PiiTab
import io.github.kotindia.demo.screens.PublicTab

/**
 * Root Composable for the KotIndia demo app.
 *
 * This is the ONLY public symbol in demo-app:shared commonMain.
 * Called by:
 *  - Android: MainActivity.setContent { App() }
 *  - Desktop: Window { App() }
 *  - iOS: MainViewController() in iosMain (which calls ComposeUIViewController { App() })
 *
 * Material 3 theme, dark mode default, light/dark toggle in About tab.
 *
 * Tab 5 (Phase 2 — compose module showcase) and
 * Tab 6 (Phase 3 — UPI flow) slots are reserved as comments below.
 */
@Composable
public fun App() {
    var darkTheme by remember { mutableStateOf(true) } // dark-first per PRD
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        DemoNavHost(
            darkTheme = darkTheme,
            onToggleTheme = { darkTheme = !darkTheme },
        )
    }
}

// ---------------------------------------------------------------------------
// Internal nav host — keeps App() minimal
// ---------------------------------------------------------------------------

private val tabLabels = listOf(
    "PII",
    "Public",
    "Algorithm",
    "About",
    // Tab 5 slot: Phase 2 — compose module showcase (reserved)
    // Tab 6 slot: Phase 3 — UPI flow (reserved)
)

@Composable
private fun DemoNavHost(darkTheme: Boolean, onToggleTheme: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabLabels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {},  // no material-icons-extended dep — label-only nav
                        label = { Text(label) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> PiiTab(contentPadding = innerPadding)
            1 -> PublicTab(contentPadding = innerPadding)
            2 -> AlgorithmTab(contentPadding = innerPadding)
            3 -> AboutTab(
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                contentPadding = innerPadding,
            )
        }
    }
}
