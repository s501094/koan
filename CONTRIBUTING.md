# Contributing

PRs welcome. Nothing formal — open one and I'll review.

## Before you open one

- `./gradlew test` passes.
- New logic is unit-testable without a device where it can be. The pattern throughout is a pure
  object that decides (`Popups`, `Prompts`, `Glance`) plus a thin layer that acts on the engine.
  Tests exist for the first half; that split is the reason they can.
- Anything touching the engine or the UI has been run on a real device. This is a browser — the
  bugs that matter here don't show up in unit tests. Say which device in the PR.
- Commit messages are one line.

## Things worth knowing

`TODO_FIX.md` tracks what's outstanding and what's known-broken. Good places to start are in
there.

`PRIVACY.md` and `core/engine/.../PrivacyHardening.kt` are the point of the project. A change
that adds a network call, an analytics hook, or a third-party SDK will not be merged. If a
dependency you're adding phones home, say so in the PR — it's not automatically disqualifying,
but it has to be deliberate.

Build setup is in the README. The short version: Android SDK 36 and Android Studio's bundled
JBR 21, not the system JDK.
