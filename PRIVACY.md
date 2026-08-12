# Privacy audit

Audit of the shipped release APK, 2026-08-12. Redo it after any dependency bump —
the interesting findings all came from transitive dependencies, not from our code.

## Our own code

- **Zero network calls.** No `HttpURLConnection`, `OkHttp`, `Retrofit`, sockets or
  `fetch` anywhere in `app/` or `core/`.
- **One outbound URL literal in the whole codebase**: `https://duckduckgo.com/` —
  the home page and the search template. That's it.
- The bundled Boosts extension has no `fetch`, no `XMLHttpRequest`, no
  `sendBeacon`, no `WebSocket`. It talks to the app over a native port and
  nowhere else. It is loaded from `resource://android/assets/` — bundled, not
  fetched, not updatable.
- No analytics SDK of any kind: no Firebase, Crashlytics, Sentry, Adjust,
  Amplitude, Leanplum, Mixpanel, AppsFlyer, Facebook.

## Permissions

Five, down from ten before the audit.

| Permission | Why |
|---|---|
| `INTERNET` | It's a browser |
| `ACCESS_NETWORK_STATE` | Offline detection |
| `WAKE_LOCK` | Media playback |
| `FOREGROUND_SERVICE` | Gecko's media service |
| `MODIFY_AUDIO_SETTINGS` | Audio focus |

Removed via `tools:node="remove"`:

- **`QUERY_ALL_PACKAGES`** — the worst one. Lets an app enumerate every app you
  have installed, which is a first-class fingerprinting signal. Declared by
  `support-ktx` / `support-utils` for the app-links feature, which we don't use.
- `RECEIVE_BOOT_COMPLETED`, `REORDER_TASKS`, `HIGH_SAMPLING_RATE_SENSORS`,
  `POST_NOTIFICATIONS` — nothing here exercises any of them.

## Dependencies removed

- **`androidx.test`** (and `.ext`, `.services`, `.espresso`). A *production*
  Mozilla artifact, `concept-toolbar`, depends on `androidx.test.ext:junit-ktx`.
  That put JUnit and three instrumentation activities
  (`InstrumentationActivityInvoker$BootstrapActivity` and friends) into the
  shipped release APK. Upstream packaging bug; excluded.

## Deliberately kept, with reasons

- **`com.google.android.gms:play-services-fido`.** Excluded on a first pass, then
  put back. GeckoView's own `org.mozilla.geckoview.WebAuthnTokenManager`
  references `gms.fido` directly, and Gecko reaches it for plain feature
  detection — sites call `isUserVerifyingPlatformAuthenticatorAvailable()` on
  page load. Removing it turns that into a `NoClassDefFoundError` on ordinary
  pages. It is an on-device FIDO client, not analytics, and makes no network
  calls of its own. To be rid of it: set `security.webauth.webauthn` to false in
  `about:config`, which stops Gecko entering the path at all.
- **OCSP / certificate revocation.** Contacts the CA of the site you're already
  visiting. Not a third party learning anything new, and disabling it weakens a
  real protection.
- **DNS-over-HTTPS left off, not on.** Turning it on would route every lookup
  through a single resolver that then sees your whole browsing history.

## Still in the binary, and what's done about it

Being honest about what an audit can and can't prove: these are compiled into
Mozilla's shared native libraries and cannot be removed without building Gecko
from source.

- `libmegazord.so` (22 MB) contains hard-coded
  `firefox.settings.services.mozilla.com`, `merino.services.mozilla.com`,
  `identity.mozilla.com` and `ads.mozilla.org`.
- `libcrashtools.so` contains `incoming.telemetry.mozilla.org`.
- Glean and Nimbus classes survive into `classes.dex` (~150 string references).

None of it is initialised: there is no `Glean.initialize`, no Nimbus API, no
`ExperimentDelegate` (explicitly `null`), no Firefox Account, no Sync, no crash
handler service (`crashHandler(null)`). But "we never call it" is a weaker
guarantee than "it is also switched off", so `PrivacyHardening` blanks or
disables each one by name as well — see the *endpoints found compiled into the
shipped native libraries* block.

## Gecko prefs set at startup

`core/engine/.../PrivacyHardening.kt` sets ~60 prefs on the default branch.
Categories: telemetry and its endpoint, Normandy/Shield studies, Remote Settings,
captive-portal and connectivity probes, region lookup, push, extension blocklist
and update pings, Safe Browsing, DNS/link prefetch and the predictor,
`<a ping>` and `navigator.sendBeacon`, WebRTC local-address leaks, crash report
submission, Firefox Accounts, and sponsored suggestions.

Set on the **default** branch deliberately, so `about:config` still shows them as
unmodified and every one can be overridden by hand. `aboutConfigEnabled` is on
for exactly that reason.

**Safe Browsing is off**, which is a real tradeoff, not a free win. Firefox's
implementation downloads hash lists from Google rather than sending it every URL,
so it's far better than it sounds — but it's still a periodic connection to
Google that browsing does not require. You lose phishing and malware warnings.
Re-enable with the four `browser.safebrowsing.*` prefs.

## What this audit does not prove

Static analysis only. It shows what code and endpoints are present and what is
configured, not what actually goes out on the wire. Nothing here has run on a
device yet.

To verify for real, install the APK and watch it: PCAPdroid, or a proxy with a
trusted CA, or `adb shell` + `tcpdump`. Open the browser, leave it idle for a few
minutes, and confirm the only traffic is to sites you visited. That's the
measurement this document is a substitute for, not a replacement of.
