# TODO / broken

## TODO

- Theme picker: dots other than the primary aren't individually draggable yet — moving the
  primary re-derives the rest from the harmony. Desktop lets you drag each one free ("floating").
- Theme is global, not per-Space. Moves into the Space row when `:core:data` lands.
- Custom (literal hex) dots are modelled in `ColorDot.customRgb` and handled by the gradient
  composer, but there's no UI to enter one.
- The wheel is sampled at 56x56 and redrawn each frame during a drag. Fine on a phone, but if it
  ever stutters, cache it to an ImageBitmap keyed on lightness+type.

- Stage 3: `:core:data` Room module — spaces, tabs, folders, boosts, theme presets.
- Stage 3: Space switcher + per-Space `contextId` isolation + Essentials grid.
- Stage 4: Glance modal.
- Stage 5: Boosts (bundled WebExtension, touch element picker, Zap).
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
