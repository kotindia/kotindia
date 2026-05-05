// Copyright (c) The KotIndia Authors. Licensed under the Apache License, Version 2.0.
// SPDX-License-Identifier: Apache-2.0

package io.github.kotindia.demo

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * iOS entry point for the KotIndia demo app.
 *
 * Called from Swift as: MainViewControllerKt.MainViewController()
 *
 * This is the SECOND public symbol in demo-app:shared (alongside App()).
 * Required public for Swift interop via the generated ObjC header.
 * All other symbols in commonMain are internal — see AC3 iOS ObjC symbol guard.
 */
public fun MainViewController(): UIViewController = ComposeUIViewController { App() }
