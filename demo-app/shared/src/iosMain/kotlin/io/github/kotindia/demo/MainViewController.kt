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
 *
 * `enforceStrictPlistSanityCheck = false` disables CMP's runtime check for
 * UILaunchScreen / UIApplicationSceneManifest etc. in Info.plist. We use Xcode 26's
 * generated Info.plist (GENERATE_INFOPLIST_FILE=YES) which uses the new
 * `INFOPLIST_KEY_UILaunchScreen_Generation = YES` flag instead of an explicit dict —
 * the runtime check pre-dates this Xcode flag and false-positives. Standard JetBrains
 * escape hatch documented in the CMP source itself (PlistSanityCheck.ios.kt).
 */
public fun MainViewController(): UIViewController = ComposeUIViewController(
    configure = { enforceStrictPlistSanityCheck = false },
) { App() }
