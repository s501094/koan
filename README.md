# Kōan

A private Android browser on GeckoView, rebuilding Zen Browser's ideas natively.

**Nothing in this app tracks you.** No analytics, no crash reporting, no telemetry, no ads, no
accounts, no sync, no phone-home of any kind. The app makes exactly zero network requests of its
own — every packet it sends is one you asked for by opening a page.

## The privacy position

Most browsers that call themselves private mean "we block other people's trackers." Kōan does
that too, but the claim here is narrower and stronger: **the browser itself is not a party to
your browsing.** There is nothing in it that reports, counts, measures, or remembers on anyone
else's behalf.

### What the app does not do

- No analytics SDK. Not Firebase, Crashlytics, Sentry, Adjust, Amplitude, Mixpanel, AppsFlyer,
  Facebook — none of them, not even the "anonymous" kind.
- No crash reporting. Gecko's crash handler is explicitly `null`.
- No telemetry, no experiments, no A/B tests, no feature flags fetched from a server.
- No account, no login, no sync, no cloud, no backup.
- No update check. The app never contacts a server to ask about itself.
- No unique identifier is generated, stored, or transmitted. There is no install ID to correlate.
- Zero network calls in the app's own code — no `HttpURLConnection`, `OkHttp`, `Retrofit`, or raw
  sockets anywhere in `app/` or `core/`.

### What it switches off in the engine

A stock Gecko isn't a tracker, but it is chatty: it fetches Safe Browsing lists from Google,
polls Mozilla's Remote Settings and Normandy, probes a captive-portal endpoint, looks up your
region, and carries a telemetry stack even when nothing is uploading.
[`PrivacyHardening.kt`](core/engine/src/main/java/com/tyell/koan/engine/PrivacyHardening.kt)
turns off ~60 prefs covering all of it, and blanks the endpoints as well, so a pref flipped back
on by a future engine bump still has nowhere to send anything.

Also on by default: strict tracking protection, Global Privacy Control, referer trimmed across
origins, prefetch and the network predictor disabled, `<a ping>` and `navigator.sendBeacon` off,
and WebRTC restricted to the default route so ICE can't hand out your local interface addresses.

Five Android permissions, down from ten before the audit. `QUERY_ALL_PACKAGES` — which lets an
app enumerate everything you have installed, a first-class fingerprinting signal — was pulled out
of a transitive dependency and removed.

### Cookie isolation is the actual architecture

Spaces aren't folders with a colour. Each one is a separate cookie jar, using Gecko's `contextId`
partition key — the same mechanism Firefox containers use. Two Spaces can hold two logins to the
same site, and a tracker that identifies you in one cannot join that up with the other. Sign into
work Google in one Space and personal Google in another and they are, as far as the sites are
concerned, two different browsers.

This is a more useful shape of defence than anti-fingerprinting toggles, because it doesn't try
to make you look like nobody. It makes you look like several unrelated people.

### What Kōan cannot protect you from

Being straight about the boundary, because a privacy tool that overstates its reach is worse than
one that doesn't exist:

- **Your ISP** sees every domain you connect to. DNS-over-HTTPS is deliberately left *off* rather
  than on — routing every lookup through one resolver just moves that visibility to a company you
  chose instead of the one you're already paying.
- **Your mobile carrier** sees the same, plus your location, at the network level.
- **Websites you visit** see your requests, because that is what visiting means. Tracking
  protection blocks known third-party trackers; it does not and cannot stop a site from logging
  what you do on that site.
- **Your device and OS.** Android itself, and whatever else is installed, are outside this app's
  reach.

Kōan's claim stops at its own edge: nothing *inside* the app is watching. Everything past that
edge is the internet, and no browser changes it.

### Honesty about verification

The audit in [`PRIVACY.md`](PRIVACY.md) is **static** — it shows what code and endpoints exist in
the APK and how they're configured, not what actually goes out on the wire. It has not been
confirmed with packet capture yet. If you want to check rather than trust: install it, run
PCAPdroid or `tcpdump`, leave it idle, and confirm the only traffic is to sites you opened. That
measurement is on the TODO list and this document is a substitute for it, not a replacement.

Two things are in the binary and can't be removed without building Gecko from source: Mozilla's
`libmegazord.so` has hard-coded Mozilla endpoints compiled in, and Glean/Nimbus classes survive
into `classes.dex`. None of it is initialised — no `Glean.initialize`, no Nimbus, no experiment
delegate, no Firefox Account. But "we never call it" is weaker than "it's also switched off", so
`PrivacyHardening` disables and blanks each one by name too. Both hold at once, on purpose.

**Safe Browsing is off**, and that's a real trade, not a free win. Firefox downloads hash lists
rather than sending Google every URL, so it's far better than it sounds — but it's still a
periodic connection to Google that browsing doesn't require. You lose phishing and malware
warnings. Re-enable the four `browser.safebrowsing.*` prefs in `about:config` if you'd rather have
them; `aboutConfigEnabled` is on precisely so you can.

## Features

**Spaces** — separate cookie jars with their own theme, Essentials, and tabs. Swipe the toolbar to
move between them.

**Essentials** — up to 12 pinned sites per Space, stored as addresses rather than tabs, so they
always return to the pinned URL instead of wandering.

**Glance** — long-press a link to peek at it in a floating card without leaving the page.

**Boosts** — per-site CSS and a touch element picker ("Zap") to permanently hide anything on a
page. The bundled extension has no `fetch`, no `XMLHttpRequest`, no `sendBeacon`, no `WebSocket`;
it talks to the app over a native port and nowhere else, and it's loaded from bundled assets, not
fetched or updatable.

**Themes** — 41 gradient presets plus a colour wheel, ported from Zen's CSS gradient system to
Kotlin. Each Space carries its own.

## Why GeckoView

Zen is a Firefox fork whose entire product is ~37k lines of privileged chrome JS/CSS. Android
Gecko has no chrome layer, so none of it ports — Kōan reimplements the concepts in Kotlin on top
of Mozilla's Android Components (153.0.4), which rides the same Gecko revision (153.0.3) as the
desktop build this was derived from. Gecko also matters for the privacy story: it's the only
mobile engine that isn't Chromium, and `contextId` partitioning is what Spaces are built on.

## Build

Needs the Android SDK (platform 36, build-tools 36.x) and Android Studio's bundled JBR 21.
`gradle.properties` already points at the JBR — the system JDK 26 will not work.

```
./gradlew :app:assembleDebug          # installs alongside release as .debug
./gradlew :app:assembleRelease        # signed
./gradlew test
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
adb install -r app/build/outputs/apk/release/app-release.apk
```

arm64-v8a only. Add `armeabi-v7a` to `abiFilters` in `app/build.gradle.kts` for an older device.

## Size

~208MB, of which ~152MB is `libxul.so` — Gecko itself, already stripped upstream. Not a build
problem; that's what the engine weighs.

## Status

Working: browsing, tabs, session restore across kills, URL-or-search, intent handling, tracking
protection, gradient theming, Spaces + Essentials, Glance, Boosts, popups, `<select>` prompts.

Not there yet: private tabs, folders, downloads, find-in-page, extension support, and the rest of
the prompt channel (alerts, file upload, HTTP auth). A closed tab currently comes back after an
app restart — a real bug, tracked in `TODO_FIX.md`.

## Icon

`art/enso.py` generates it. Edit the script, not the XML — the vector drawables are output.

## Contributing

PRs welcome, nothing formal — see `CONTRIBUTING.md`. The one hard rule: a change that adds a
network call, an analytics hook, or a third-party SDK doesn't get merged.

## Licence

MIT — see `LICENSE`. Do what you like with it.

A built APK also contains GeckoView and Mozilla Android Components, which are MPL-2.0; if you
distribute one, `THIRD_PARTY_NOTICES.md` goes with it.
