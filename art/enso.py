import math

def enso_path(cx, cy, r, w_max, start_deg, sweep_deg, steps=180, wobble=0.0):
    """Filled outline of a single tapered brush stroke swept along an arc.

    A stroked circle gives a mechanical ring. A real ensō is one brush pass:
    thin where the brush lands, swelling mid-stroke, thinning as it lifts. So we
    walk the outer edge forward and the inner edge back, with the gap between
    them driven by a taper profile.
    """
    def thickness(t):
        # The brush lands with pressure, holds, then lifts to nothing.
        # Piecewise so the tail can actually reach a point — a single sine
        # leaves both ends blunt and the whole thing reads as a printed ring.
        if t < 0.10:                      # landing: fast pressure ramp
            p = 0.42 + 0.58 * (t / 0.10) ** 0.55
        elif t < 0.52:                    # the held body of the stroke
            p = 1.0
        else:                             # lift: long decay to a fine tail
            u = (t - 0.52) / 0.48
            p = (1.0 - u) ** 1.55
        return max(w_max * p, 0.35)

    def radius(t):
        # two harmonics — one slow breath, one faster tremor — so the arc
        # never settles onto a true circle
        return (r
                + wobble * math.sin(t * math.pi * 2.3 + 0.7)
                + wobble * 0.42 * math.sin(t * math.pi * 5.1 + 2.1))

    outer, inner = [], []
    for i in range(steps + 1):
        t = i / steps
        a = math.radians(start_deg + sweep_deg * t)
        rr, hw = radius(t), thickness(t) / 2.0
        outer.append((cx + (rr + hw) * math.cos(a), cy + (rr + hw) * math.sin(a)))
        inner.append((cx + (rr - hw) * math.cos(a), cy + (rr - hw) * math.sin(a)))

    pts = outer + inner[::-1]
    d = "M%.2f,%.2f" % pts[0]
    for p in pts[1:]:
        d += "L%.2f,%.2f" % p
    return d + "Z"


CANVAS = 1024
# macOS-style plate inset isn't relevant here; Android masks its own shape,
# so we work in the full square and keep art inside the 66/108 safe circle.
enso_1024 = enso_path(cx=512, cy=512, r=300, w_max=86,
                      start_deg=128, sweep_deg=298, wobble=9)

svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
  <defs>
    <linearGradient id="ground" x1="0" y1="0" x2="1024" y2="1024" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#20202C"/>
      <stop offset="0.55" stop-color="#15151C"/>
      <stop offset="1" stop-color="#0C0C11"/>
    </linearGradient>
    <linearGradient id="ink" x1="240" y1="200" x2="800" y2="850" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#F3D9A8"/>
      <stop offset="0.5" stop-color="#E6B57A"/>
      <stop offset="1" stop-color="#C98B52"/>
    </linearGradient>
    <filter id="soften" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="2.2"/>
    </filter>
    <filter id="glow" x="-60%" y="-60%" width="220%" height="220%">
      <feGaussianBlur stdDeviation="34"/>
    </filter>
  </defs>

  <rect width="1024" height="1024" fill="url(#ground)"/>

  <!-- the three theme dots, blurred into a wash so they read as light, not shapes -->
  <g filter="url(#glow)" opacity="0.58">
    <circle cx="512" cy="424" r="100" fill="#7C5CFF"/>
    <circle cx="424" cy="576" r="94"  fill="#4FD1FF"/>
    <circle cx="600" cy="576" r="94"  fill="#FF6FB1"/>
  </g>

  <!-- the same three dots, crisp and small, resolving at the centre -->
  <circle cx="512" cy="455" r="32" fill="#7C5CFF" opacity="0.96"/>
  <circle cx="463" cy="543" r="28" fill="#4FD1FF" opacity="0.90"/>
  <circle cx="561" cy="543" r="28" fill="#FF6FB1" opacity="0.90"/>

  <!-- one brush pass -->
  <path d="{enso_1024}" fill="url(#ink)" filter="url(#soften)"/>
</svg>
'''

open("icon.svg", "w").write(svg)

# --- Android adaptive icon -------------------------------------------------
# 108x108 viewport. Only the central 66dp circle is guaranteed visible on every
# launcher mask, so the whole stroke has to stay inside radius 33 of centre.
R, W, WOB = 24.0, 8.6, 0.7
enso_108 = enso_path(cx=54, cy=54, r=R, w_max=W,
                     start_deg=128, sweep_deg=298, steps=150, wobble=WOB)
print("outer reach:", round(R + W / 2 + WOB * 1.42, 2), "(must stay under 33)")

S = R / 300.0        # ring geometry scales with the stroke
DOT_BOOST = 1.45     # dots don't — at 48px they vanish if scaled linearly
DOTS = [  # (dx, dy, r, rgb) relative to centre, in 1024 space
    (0, -57, 32, "7C5CFF"),
    (-49, 31, 28, "4FD1FF"),
    (49, 31, 28, "FF6FB1"),
]

def circle_path(cx, cy, r):
    return (f"M{cx:.2f},{cy:.2f}m-{r:.2f},0"
            f"a{r:.2f},{r:.2f} 0,1 1,{2*r:.2f} 0"
            f"a{r:.2f},{r:.2f} 0,1 1,{-2*r:.2f} 0")

glow, crisp, mono_dots = [], [], []
for dx, dy, dr, rgb in DOTS:
    cx, cy = 54 + dx * S, 54 + dy * S
    r_crisp = dr * S * DOT_BOOST
    r_glow = r_crisp * 3.1
    glow.append(f'''        <path android:pathData="{circle_path(cx, cy, r_glow)}">
            <aapt:attr name="android:fillColor">
                <gradient android:type="radial" android:centerX="{cx:.2f}" android:centerY="{cy:.2f}" android:gradientRadius="{r_glow:.2f}">
                    <item android:offset="0.0" android:color="#B8{rgb}" />
                    <item android:offset="0.45" android:color="#5C{rgb}" />
                    <item android:offset="1.0" android:color="#00{rgb}" />
                </gradient>
            </aapt:attr>
        </path>''')
    crisp.append(f'        <path android:fillColor="#{rgb}" android:fillAlpha="0.95" '
                 f'android:pathData="{circle_path(cx, cy, r_crisp)}" />')
    mono_dots.append(f'    <path android:fillColor="#FFFFFFFF" android:fillAlpha="0.85" '
                     f'android:pathData="{circle_path(cx, cy, r_crisp)}" />')

AAPT = 'xmlns:aapt="http://schemas.android.com/aapt"'

open("ic_launcher_foreground.xml", "w").write(f'''<?xml version="1.0" encoding="utf-8"?>
<!--
  Ensō — one brush pass, drawn open.

  The three dots inside are the app's data model, not decoration: a theme in
  this browser is literally up to three colour dots placed on a wheel, which
  the gradient engine composes into the page background.

  VectorDrawable has no blur filter, so the soft wash around each dot is a
  radial gradient fading to transparent rather than a feGaussianBlur.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android" {AAPT}
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">

    <group>
{chr(10).join(glow)}

{chr(10).join(crisp)}

        <path android:pathData="{enso_108}">
            <aapt:attr name="android:fillColor">
                <gradient android:type="linear"
                    android:startX="25" android:startY="21"
                    android:endX="84" android:endY="90">
                    <item android:offset="0.0" android:color="#FFF3D9A8" />
                    <item android:offset="0.5" android:color="#FFE6B57A" />
                    <item android:offset="1.0" android:color="#FFC98B52" />
                </gradient>
            </aapt:attr>
        </path>
    </group>
</vector>
''')

open("ic_launcher_monochrome.xml", "w").write(f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#FFFFFFFF" android:pathData="{enso_108}" />
{chr(10).join(mono_dots)}
</vector>
''')

print("wrote ic_launcher_foreground.xml / ic_launcher_monochrome.xml")
