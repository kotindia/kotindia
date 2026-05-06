# Contributing to KotIndia

Thanks for taking the time to contribute. This document covers how to set up
the project, propose changes, and what we look for in a pull request.

## Code of Conduct

By participating you agree to abide by the [Code of Conduct](CODE_OF_CONDUCT.md).
Be respectful, assume good faith, focus on the work.

---

## TL;DR

- Open an issue first for non-trivial changes — saves wasted work.
- Fork → branch → commit → push → open a pull request against `main`.
- All CI checks must pass green.
- All public APIs need KDoc with `@sample` and tests.
- `:core` has zero runtime dependencies. Don't break that.
- 100% line coverage on `:core` is enforced. Don't break that either.

---

## What we accept

| Type | Notes |
|---|---|
| **Bug fixes for existing validators** | Always welcome. Include a regression test. |
| **New validators for Indian identifiers** | Yes — open an issue first to confirm scope. Format + checksum (where applicable) + format + mask + tests + KDoc. |
| **Algorithm corrections** | Cite the authoritative source (UIDAI / GSTN / RBI / Income Tax Department). Cross-validate against at least two independent open-source implementations. |
| **Documentation improvements** | KDoc, README, docs site (Dokka HTML) — all welcome. |
| **CI / build improvements** | Welcome — keep them small and explain the why. |
| **Test coverage / property tests** | Always welcome. |
| **New KMP targets** (JS, WASM, Linux) | Open an issue first. We want to ship these but timing matters. |

## What we don't accept

| Type | Why |
|---|---|
| **External KYC API integrations** (Razorpay, Cashfree, Setu, etc.) | Out of scope. KotIndia does format + checksum validation only — never network calls to government databases. |
| **OCR / image extraction** of cards | Different problem domain (OpenCV / MLKit). |
| **Currency formatting** (₹1,23,456 lakh/crore) | Already solved by `Locale("en", "IN")` + `NumberFormat`. |
| **Adding runtime dependencies to `:core`** | Hard rule. The `enforceZeroDeps` Gradle task fails the build if violated. |
| **Backend services** | This is a library, not a server. |
| **Biometric / Aadhaar device integration** | Use [`ekoindia/android-uidai-rdservice-manager`](https://github.com/ekoindia/android-uidai-rdservice-manager). |

If your idea falls in the second column, open an issue and we'll explain the
reasoning. We're not opposed to discussing scope — but the scope is
deliberately narrow to keep maintenance honest.

---

## Project setup

**Prerequisites**

- JDK 17 (provisioned automatically by Gradle's Foojay resolver)
- Git
- macOS for iOS targets (Xcode 26+ for the demo app)

**Clone and build**

```bash
git clone https://github.com/kotindia/kotindia.git
cd kotindia
./gradlew :core:check
```

That single command runs compile + tests + ktlint + detekt + Kover (100%
coverage gate) + apiCheck (binary compatibility validator) + enforceZeroDeps.

**Run the demo app**

```bash
# Android
./gradlew :demo-app:androidApp:installDebug

# Desktop
./gradlew :demo-app:desktopApp:run

# iOS — open demo-app/iosApp/iosApp.xcodeproj in Xcode, Cmd+R
```

To consume the *published* `:core` artifact in the demo (validates the
production consumer path) instead of the in-repo source:

```bash
./gradlew :demo-app:androidApp:assembleDebug -Pkotindia.useMavenCore=true
```

---

## Workflow

1. **Open an issue first** — describe the problem or proposal. Skip this step
   only for typo / docs fixes.
2. **Fork** the repository to your account.
3. **Branch** off `main`:
   ```bash
   git checkout -b feat/v-driving-license
   ```
   Branch naming: `feat/`, `fix/`, `docs/`, `test/`, `chore/`, `refactor/`.
4. **Make your change** following the standards below.
5. **Run the gates locally** before pushing:
   ```bash
   ./gradlew :core:check
   ./gradlew :core:apiCheck
   ./gradlew :core:dokkaGeneratePublicationHtml  # if you touched public API
   ```
6. **Commit** using [Conventional Commits](https://www.conventionalcommits.org/):
   ```
   feat(core): add driving license validator with state-prefix check
   fix(v-aadhaar): handle leading-zero edge case in Verhoeff
   docs(readme): clarify Maven coordinates
   test(v-pan): add property tests for category code rules
   ```
7. **Push** to your fork and **open a PR** against `kotindia/kotindia:main`.
8. **CI runs automatically** — wait for green checks.
9. **Address review comments** by pushing more commits to the same branch
   (do not force-push during review unless asked — it dismisses approvals).
10. **Merge** — squash-only. The PR title becomes the commit message; PR body
    becomes the commit body. Your branch is auto-deleted on merge.

---

## Standards

### Code style

- Match the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- `ktlint` and `detekt` run in CI; they must be green.
- 4 spaces, no tabs. Trailing commas in multi-line lists.
- One public symbol per file when reasonable (validators are per-file: `Aadhaar.kt`, `PAN.kt`, etc.).

### Public API (`:core`)

- Every public symbol must be **explicitly** marked `public` (`explicitApi()`
  is set to STRICT — the compiler will error otherwise).
- Every public symbol needs **KDoc**: a one-line summary, `@param`, `@return`,
  and where applicable `@sample` and `@throws`.
- Use the existing `ValidationResult` sealed class and `InvalidReason` enum.
  Don't invent new result shapes.
- API changes (additions, signature changes, removals) must be reflected in
  the binary-compatibility-validator dump (`./gradlew :core:apiDump`). The
  PR template will remind you.

### Tests

- Every validator has unit tests in `core/src/commonTest/`.
- Algorithmic validators (Verhoeff, Luhn, GSTIN base-36) need property-based
  tests via `kotest-property`.
- Test vectors should be sourced from authoritative external implementations
  (see comments in existing validator tests). Don't reuse vectors generated
  by KotIndia's own implementation — that's circular.
- **Never** add JVM-only test dependencies. `kotest-runner-junit5` is banned
  because it breaks Kotlin/Native silently. Use `kotlin-test` and
  `kotest-property` only.
- 100% line coverage on `:core` is enforced via Kover. The build fails below
  threshold.

### Algorithmic validators

If you're contributing a new validator that includes a checksum:

1. Cite the **authoritative specification** — UIDAI for Verhoeff, GSTN for
   GSTIN checksum, RBI for IFSC structure, Income Tax Department for PAN.
2. **Cross-validate** against at least two independent open-source
   implementations in other languages (Node, Dart, Python). Document the
   sources in code comments.
3. Include known-valid test vectors **sourced externally** — published
   examples from the spec or government documents, never test data
   generated by your own implementation.

### Demo app

- The `demo-app/` modules are showcase code, not library code. They're
  excluded from Maven Central, Kover, Dokka, binary-compat-validator, and
  `explicitApi()`.
- They follow the same Kotlin style guide but lint warnings there don't
  block the build.
- New validators added to `:core` should also get a card in the demo — pick
  the appropriate tab (PII / Public / Algorithm).

### Commits

- One logical change per commit. Don't bundle unrelated changes.
- Use Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`,
  `refactor:`).
- The first line is a short summary (under 70 chars). Body explains the
  *why*, not the *what*.
- No "WIP" or "address review" commits in the final history — the squash
  merge collapses everything into one commit anyway.

---

## CI checks (all must pass)

| Workflow | What it runs |
|---|---|
| `CI` (ubuntu + macos) | `:core:check` + `:core:apiCheck` + Kover HTML report upload |
| `Demo App` (ubuntu) | `assembleDebug`, `desktopApp:assemble`, exclusion guards |
| `Demo App` (macos) | `linkDebugFrameworkIosSimulatorArm64` |
| `Docs` | Dokka HTML generation |

If any check is red, the PR can't merge. Look at the workflow logs in the
PR's "Checks" tab. Most failures are obvious (compile error, test failure,
ktlint complaint). If a flake — comment on the PR and a maintainer will
re-run.

---

## Releases

Releases are tag-triggered and only maintainers can push tags. Contributors
don't need to bump the version in their PR — that happens at release time.

If your contribution is in `[Unreleased]` in `CHANGELOG.md`, add an entry
under the appropriate section (`### Added`, `### Changed`, `### Fixed`,
`### Security`).

---

## Reporting bugs

Open an issue with:

- **Library version** (`io.github.kotindia:core:0.X.X`)
- **Platform** (Android, JVM, iOS device, iOS simulator)
- **Kotlin version** in your project
- **Minimal reproducer** — input that produces the wrong output, plus the
  expected output. For checksum validators, please include the source you're
  cross-checking against (a UIDAI document, a GSTN tool, etc.).
- **Stack trace** if there's an exception.

Validator bugs are taken seriously — incorrect checksum logic can ship to
production and cause real harm.

---

## Reporting security issues

**Do not open a public issue for security concerns.** Email the maintainer
directly (see `LICENSE` / commit history for contact) or use GitHub's
[private security advisory](https://github.com/kotindia/kotindia/security/advisories/new)
flow. Public disclosure should come after a fix is shipped.

---

## License

By contributing, you agree that your contributions will be licensed under the
[Apache License 2.0](LICENSE) — the same as the rest of the project.

---

## Questions?

- General questions: open a [GitHub Discussion](https://github.com/kotindia/kotindia/discussions) (when enabled) or an issue with the `question` label.
- Implementation questions: comment on the issue you're working on; we'll respond.
- Async chat: not yet — once the project has more contributors, we'll set up a Slack / Discord channel.

Thank you for contributing.
