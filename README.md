# Kōan

An Android browser on GeckoView, rebuilding Zen Browser's ideas natively.

Zen is a Firefox fork whose entire product is ~37k lines of privileged chrome JS/CSS. Android
Gecko has no chrome layer, so none of it ports — Kōan reimplements the concepts in Kotlin on
top of Mozilla's Android Components (153.0.4), which rides the same Gecko revision (153.0.3)
as the desktop build this was derived from.

## Build

Needs the Android SDK (platform 36, build-tools 36.x) and Android Studio's bundled JBR 21.
`gradle.properties` already points at the JBR — the system JDK 26 will not work.

```
./gradlew :app:assembleDebug          # 235MB, installs alongside release as .debug
./gradlew :app:assembleRelease        # 208MB, signed
./gradlew :app:testDebugUnitTest
```

Release signing reads `~/.koan/signing.properties`:

```
koanStoreFile=/Users/you/.koan/release.keystore
koanStorePassword=...
koanKeyAlias=koan
koanKeyPassword=...
```

Without it the release build still succeeds, just unsigned.

## Install

```
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
```

arm64-v8a only. Add `armeabi-v7a` to `abiFilters` in `app/build.gradle.kts` for an older device.

## Size

~208MB, of which ~152MB is `libxul.so` — Gecko itself, already stripped upstream. Not a build
problem; that is what the engine weighs.

## Status

Working: browsing, tabs, session restore across kills, URL-or-search, intent handling,
tracking protection, adaptive icon.

Next: gradient theming, then Spaces + Essentials, then Glance, then Boosts, then folders.
See `SUMMARY.md` and `TODO_FIX.md`.

## Icon

`art/enso.py` generates it. Edit the script, not the XML — the vector drawables are output.
