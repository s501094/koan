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

- Popups / `target=_blank` aren't handled — `WindowFeature` from `feature-session` isn't wired,
  so a page opening a new window currently does nothing. It also needs to inherit the Space's
  contextId when it is wired.
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

- Stage 6: Folders + Live Folders (RSS, GitHub).
- Port the 151 Zen SVG icons to vector drawables (MPL-2.0, needs attribution file).
- Downloads, prompts, context menu and find-in-page features are on the classpath but not wired up.
- uBlock Origin install flow (`feature-addons`, direct XPI URL — AMO collections need a Mozilla-side collection).
- No armeabi-v7a in the APK. Add to `abiFilters` if an old device ever needs it.
- Never run on a real device or emulator. The local AVD's system image is a stub (no `.img`
  files) and no phone was attached, so verification was static only: signature, manifest,
  ABI, icon resources, 6/6 unit tests.
- Long-press context menu currently falls through to Gecko's default, not ours.

## Flagged / broken

- APK is ~208MB. `libxul.so` alone is 152MB and is already stripped upstream — the AGP
  "unable to strip" warning is a red herring. Nothing to fix, but don't re-investigate it.

- `browser-engine-gecko` drags in `service-nimbus` and `glean-native`. Never initialised and
  telemetry prefs are off, but the code is in the APK. Worth confirming it stays inert.
- `enableEdgeToEdge` + `imePadding` on the toolbar is untested against the keyboard on a real device.
- Session restore races the intent handler on cold start with a VIEW intent — restore runs in a
  coroutine, the intent tab is added after. Looks right, not verified.
