# TODO / broken

## TODO

- Boost JS runs in the content-script sandbox, so it can't touch page globals. Fine for DOM
  tweaks, not enough for scripts that need to patch a site's own JS.
- No Boost import/export, and no UI for the `js` field yet (CSS + Zap only in the sheet).
- Zap selectors are computed once; a site that changes its class names breaks the selector
  silently. No "this rule matched nothing" feedback.
- Privacy audit is static only — never run on a device. Verify with PCAPdroid or tcpdump.
- Glean/Nimbus classes and Mozilla endpoints remain compiled into libmegazord.so /
  libcrashtools.so. Uninitialised and pref-disabled, but not removable without building Gecko.

- A Glance left open when the app is killed comes back as an ordinary tab — `glanceTabId` is UI
  state and isn't persisted. Harmless, but it's a behaviour difference from desktop.
- No nested Glance: long-pressing inside the overlay does nothing, since only the selected tab's
  hitResult is observed. Same as desktop, but by accident rather than design.
- `zen.glance.open-essential-external-links` not implemented — external links from an Essential
  should open in a Glance rather than navigating the Essential away.
- Two live EngineViews while a Glance is open. Fine for one overlay; worth watching for memory.

- Popup tabs aren't marked as popups, so there's no "opened by localhost:8099" affordance and no
  way to tell one from a tab you opened yourself. `parentId` is set, just unused in the UI.
- No app-level popup quota. Deliberate: Gecko's own popup blocker already caps it — 20
  `window.open` calls in a loop produced exactly one tab on device, with and without user
  activation. Revisit only if a real site is seen getting more than one through.
- Essentials favicons only appear when a tab in the Space happens to be on that URL. Needs
  `BrowserIcons` proper (it's already constructed in `KoanComponents`, just unused).
- Spaces can't be reordered; `position` is write-only in practice.
- Space chips don't scroll to the active one when it changes off-screen.
- `:core:data` schema export is on but there are no migrations yet — v1 only.

- Theme picker: dots other than the primary aren't individually draggable yet — moving the
  primary re-derives the rest from the harmony. Desktop lets you drag each one free ("floating").
- Custom (literal hex) dots are modelled in `ColorDot.customRgb` and handled by the gradient
  composer, but there's no UI to enter one.
- The wheel is sampled at 56x56 and redrawn each frame during a drag. Fine on a phone, but if it
  ever stutters, cache it to an ImageBitmap keyed on lightness+type.

- No private tabs. Gecko supports it per-session (`privateMode` on the engine session) and the
  contextId machinery is already the right shape for it, but nothing exposes it — there's no way
  to open a tab that leaves no cookies, cache or session entry behind. Requested with a lock:
  getting back into private tabs should take the fingerprint reader, falling back to the device
  PIN/pattern. `androidx.biometric` + `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`.
- Long-press on a video's scrubber pauses the video instead of scrubbing. A press-and-hold on
  the seek bar should drag it. Suspect our own Glance long-press: the hit-result observer fires
  on any long press, and whatever it does to the touch stream leaves the page seeing a tap
  (play/pause toggle) rather than a drag. Check what Glance does on a press that lands on a
  non-link, and whether the video controls ever see the move events.
- Home page should be Google. Currently `MainActivity.HOME_URL` = `https://duckduckgo.com/`,
  used for new tabs and the first tab of a new Space. One constant. Decide separately whether
  `UrlInput.DEFAULT_SEARCH_TEMPLATE` (also DDG) moves with it — and note both choices sit against
  the privacy thesis in `PRIVACY.md`, since Google logs queries and DDG is the reason the app
  currently makes no tracked search request. Make it a setting rather than a swapped constant.
- No way to get to an empty tab. Closing the last tab, or wanting a clean blank one, isn't
  reachable from the UI.

- Stage 6: Folders + Live Folders (RSS, GitHub).
- Port the 151 Zen SVG icons to vector drawables (MPL-2.0, needs attribution file).
- Downloads, context menu and find-in-page features are on the classpath but not wired up.
- Only choice prompts (`<select>`) are handled. `alert`/`confirm`/`prompt`, `<input type=file>`,
  HTTP auth and date/time pickers come through the same `promptRequests` channel and are still
  dropped. Save-password and credit-card capture stay unimplemented on purpose.
- uBlock Origin install flow (`feature-addons`, direct XPI URL — AMO collections need a Mozilla-side collection).
- No armeabi-v7a in the APK. Add to `abiFilters` if an old device ever needs it.
- Long-press context menu currently falls through to Gecko's default, not ours.
- Only run on one device so far (Nothing A059P, Android 16, arm64). Spaces gestures, theme
  persistence and Essentials still unverified on hardware — only popups and the tab sheet are.

## Flagged / broken

- APK is ~208MB. `libxul.so` alone is 152MB and is already stripped upstream — the AGP
  "unable to strip" warning is a red herring. Nothing to fix, but don't re-investigate it.

- `browser-engine-gecko` drags in `service-nimbus` and `glean-native`. Never initialised and
  telemetry prefs are off, but the code is in the APK. Worth confirming it stays inert.
- `enableEdgeToEdge` + `imePadding` on the toolbar is untested against the keyboard on a real device.
- Landscape wastes most of the tab sheet on the Spaces row and Essentials grid — the tab list only
  gets one row before it has to scroll. Bounded now, but the layout wants a two-column split.
- Session restore races the intent handler on cold start with a VIEW intent — restore runs in a
  coroutine, the intent tab is added after. Looks right, not verified.
- `android:dataExtractionRules` is not set. `allowBackup="false"` stops Google cloud backup, but
  on Android 12+ device-to-device transfer is governed separately — so a phone-to-phone migration
  could carry the profile (cookie jars included) to a new device. Set the rules file and deny
  both `<cloud-backup>` and `<device-transfer>` explicitly.
- WorkManager is in the release APK, pulled by `org.mozilla.components:concept-storage` (via
  `feature-session` / `browser-state`). It adds an exported `SystemJobService` and a
  `DiagnosticsReceiver` — both permission-guarded, but the point of WorkManager is running code
  while the app is closed, which a browser making no network calls of its own shouldn't need.
  No storage delegate is wired, so nothing should be scheduling anything. Verify on device with
  `adb shell dumpsys jobscheduler | grep com.tyell.koan`, then consider excluding the module.

- Additions and navigations are still on the 30s autoSave throttle, so killing the app right
  after opening a tab still loses that tab. Left alone deliberately — losing a tab you opened
  costs you a tab, it never leaks one. Only removals were given the guarantee.
