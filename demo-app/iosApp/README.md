# demo-app/iosApp — iOS Xcode Project

## Status: Xcode project skeleton deferred to Slice 13b

The Xcode `.xcodeproj` file (`project.pbxproj`) is not committed in this slice.
Authoring `project.pbxproj` by hand is unreliable and brittle across Xcode versions.
The Swift source files (`iOSApp.swift`, `ContentView.swift`) ARE committed here so the
Swift code is version-controlled even though the Xcode project file is not.

The Kotlin/KMP side is complete: the shared framework builds via Gradle.

---

## Build the shared framework (Gradle)

```bash
# From repo root
./gradlew :demo-app:shared:linkDebugFrameworkIosSimulatorArm64 --no-daemon
```

Output framework location:

```
demo-app/shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework
```

This path is used in CI (`demo-app.yml` — `build-ios-framework` job) to verify the
framework compiles correctly on `macos-latest`.

---

## Set up the Xcode project locally (Slice 13b or manual)

Follow these steps in Xcode to create the project that wraps the KMP framework:

### 1. Create the Xcode project

1. Open Xcode → File → New → Project → iOS → App
2. Product Name: `iosApp`
3. Bundle Identifier: `io.github.kotindia.demo`
4. Interface: SwiftUI
5. Language: Swift
6. Save to: `demo-app/iosApp/` (replace the generated `iosApp/` subdirectory)

### 2. Add the KMP framework

1. In Xcode: select the `iosApp` target → Build Settings → Framework Search Paths
2. Add: `$(SRCROOT)/../../shared/build/bin/iosSimulatorArm64/debugFramework`
   (SRCROOT-relative — required for reproducible builds across machines; Marcus arch condition)
3. In Build Phases → Link Binary With Libraries → add `shared.framework`
4. In Build Phases → + → New Run Script Phase → add:

```bash
cd "$SRCROOT/../.." && ./gradlew :demo-app:shared:embedAndSignAppleFrameworkForXcode
```

5. In Build Phases → Embed Frameworks → add `shared.framework` with "Embed & Sign"

### 3. Replace generated Swift source files

Copy the committed Swift files into the Xcode project:

- `demo-app/iosApp/iosApp/iOSApp.swift` → replaces the generated `iOSApp.swift`
- `demo-app/iosApp/iosApp/ContentView.swift` → replaces the generated `ContentView.swift`

### 4. Build and run

Select scheme `iosApp` → simulator `iPhone 16 Pro` (arm64) → Run.

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
