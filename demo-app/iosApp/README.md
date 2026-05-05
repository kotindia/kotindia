# demo-app/iosApp — iOS Xcode Project

## Status: Ready to run (Slice 13b complete)

The Xcode project is committed. Open `demo-app/iosApp/iosApp.xcodeproj` in Xcode,
select the `iosApp` scheme + iPhone 16e simulator, and press Run. The KMP shared
framework is rebuilt automatically before each Xcode build via the embedded Gradle
run script.

---

## Open and run (Slice 13b — project committed)

Prerequisites: macOS with Xcode 16+, KMP shared framework built at least once.

1. Build the shared framework (first time only, or after KMP source changes):

```bash
# From repo root
./gradlew :demo-app:shared:linkDebugFrameworkIosSimulatorArm64 --no-daemon
```

2. Open `demo-app/iosApp/iosApp.xcodeproj` in Xcode.
3. Select scheme `iosApp` and simulator `iPhone 16e`.
4. Press Run (Cmd+R). The Gradle run script rebuilds the framework automatically on each build.

Framework location (used by the Xcode project via `$(SRCROOT)/../shared/...`):

```
demo-app/shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework
```

This path is also verified in CI (`demo-app.yml` — `build-ios-framework` job) on `macos-latest`.

---

## Swift source (committed — Slice 13a)

### iOSApp.swift

Entry point. Uses SwiftUI App lifecycle.

```swift
import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

### ContentView.swift

Wraps the KMP Compose `UIViewController` in a `UIViewControllerRepresentable`.

```swift
import SwiftUI
import shared

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

`MainViewControllerKt.MainViewController()` corresponds to the `MainViewController.kt` function
in `demo-app/shared/src/iosMain/` (Dev's responsibility — Slice 13a F12).

---

## ObjC symbol leakage guard

KMP iOS framework exports all non-`internal` Kotlin symbols as ObjC API by default.
Only `MainViewController()` in `iosMain/MainViewController.kt` is `public` (required for
Swift invocation). Every other shared declaration MUST be `internal` to prevent unintended
framework header pollution.

Optional CI verification (recommended for Slice 13b):

```bash
grep -r "kotindia" \
  demo-app/shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework/Headers/ \
  | grep -v "MainViewController" && exit 1 || true
```

---

## References

- Marcus arch ruling (OQ-1 RESOLVED): Direct `.framework` embedding — NO CocoaPods.
- PRD: `docs/prd/phase-post-1-slice-13a-demo-app.md` § AC6, § Engineering Notes — iOS integration
- Brain decision: `dec_20260505_234407_846b55` (Karan, 2026-05-05)
