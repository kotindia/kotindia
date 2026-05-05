/*
 * Copyright 2026 The KotIndia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import SwiftUI
import shared

/// Root SwiftUI view — wraps the KMP Compose UIViewController.
///
/// The `shared` module is built by Kotlin Gradle Plugin and embedded as a
/// static framework. `MainViewControllerKt.MainViewController()` is the sole
/// public Kotlin symbol exposed to Swift (all other shared declarations are
/// `internal`). See demo-app/iosApp/README.md for Xcode setup instructions.
struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose handles IME insets itself
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
