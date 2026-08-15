# Third-party notices

Kōan's own source is MIT (see LICENSE). A built APK also contains the following, under their
own licences.

## Mozilla Public License 2.0

- **GeckoView** (`org.mozilla.geckoview:geckoview-omni`) — the browser engine.
- **Mozilla Android Components** (`org.mozilla.components:*`) — engine bindings, browser state,
  session storage, icons, feature-session, feature-tabs, support-ktx.

MPL-2.0 is file-level copyleft: it covers Mozilla's files, not the code that links against them.
Source for the exact versions used is at <https://hg.mozilla.org/mozilla-central> and
<https://github.com/mozilla-mobile/firefox-android>; the versions are pinned in
`gradle/libs.versions.toml`. Full text: <https://www.mozilla.org/MPL/2.0/>.

**If you distribute a built APK**, you are passing on MPL-covered binaries and must keep this
notice with it.

## Apache License 2.0

AndroidX (Compose, Lifecycle, Room, Activity, Core), the Kotlin standard library, and Google's
Material Components. Full text: <https://www.apache.org/licenses/LICENSE-2.0>.

## Zen Browser

Kōan reimplements ideas from [Zen Browser](https://zen-browser.app) (MPL-2.0) — Spaces,
Essentials, Glance, Boosts, the gradient theme system. No Zen source is copied: Zen is
privileged chrome JS/CSS with no equivalent layer on Android, so everything here is written from
scratch in Kotlin. Concepts aren't copyrightable, so this is attribution rather than obligation.

Zen's SVG icon set has **not** been ported. If it ever is, those files are MPL-2.0 and need an
attribution entry here in the same commit.
