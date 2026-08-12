"""Design 15 original themes by inverting Zen's colour-wheel maths.

Zen's presets are hand-placed coordinates. Rather than guess coordinates, pick
the hue / saturation / lightness you actually want and solve for the position
that produces it — then render the result to check it looks like anything.
"""
import math

PICKER = 380.0
CFP_C = (PICKER + 60) / 2      # 220
CFP_R = (PICKER + 60 - 30) / 2 # 205
DOT_HALF = 29.0
HARM_C = HARM_R = PICKER / 2

ANGLES = {
    "Floating": [],
    "Complementary": [180],
    "SingleAnalogous": [310],
    "SplitComplementary": [150, 210],
    "Analogous": [50, 310],
    "Triadic": [120, 240],
}


def position_for(hue_deg, saturation_pct):
    """Inverse of colorFromPosition for ExplicitLightness dots."""
    dist = CFP_R * (1 - saturation_pct / 100.0)
    a = math.radians(hue_deg)
    return (CFP_C + dist * math.cos(a) - DOT_HALF,
            CFP_C + dist * math.sin(a) - DOT_HALF)


def hue2rgb(p, q, t):
    if t < 0: t += 1
    if t > 1: t -= 1
    if t < 1/6: return p + (q - p) * 6 * t
    if t < 1/2: return q
    if t < 2/3: return p + (q - p) * (2/3 - t) * 6
    return p


def hsl2rgb(h, s, l):
    if s == 0:
        r = g = b = l
    else:
        q = l * (1 + s) if l < 0.5 else l + s - l * s
        p = 2 * l - q
        r, g, b = hue2rgb(p, q, h + 1/3), hue2rgb(p, q, h), hue2rgb(p, q, h - 1/3)
    return [math.floor(v * 255 + 0.5) for v in (r, g, b)]


def color_from_pos(x, y, lightness):
    x += DOT_HALF; y += DOT_HALF
    dist = math.hypot(x - CFP_C, y - CFP_C)
    ang = math.degrees(math.atan2(y - CFP_C, x - CFP_C)) % 360
    nd = 1 - min(dist / CFP_R, 1)
    return hsl2rgb(ang / 360, (nd * 100) / 100, lightness / 100)


def apply_harmony(x, y, harmony):
    pts = [(x, y)]
    base = math.degrees(math.atan2(y - HARM_C, x - HARM_C)) % 360
    d = min(math.hypot(x - HARM_C, y - HARM_C), HARM_R)
    for off in ANGLES[harmony]:
        rad = math.radians((base + off) % 360)
        pts.append((HARM_C + d * math.cos(rad), HARM_C + d * math.sin(rad)))
    return pts


def blend(a, b, pct):
    p = pct / 100
    return [math.floor(a[i] * p + b[i] * (1 - p) + 0.5) for i in range(3)]


def toolbar_base(dark):
    return [23, 23, 26] if dark else [240, 240, 244]


def luminance(c):
    a = []
    for v in c:
        v /= 255
        a.append(v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4)
    return a[0]*0.2126 + a[1]*0.7152 + a[2]*0.0722


def contrast(a, b):
    l1, l2 = luminance(a), luminance(b)
    return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)


def should_be_dark(primary, opacity, system_dark):
    bg = blend(toolbar_base(system_dark), primary, (1 - opacity) * 100)
    w = blend(bg, [255, 255, 255], 20)
    k = blend(bg, [0, 0, 0], 20)
    return contrast(bg, w) > contrast(bg, k)


# name, hue, saturation, lightness, harmony, mood
# Harmony choice is not cosmetic. Triadic (120/240) and SplitComplementary
# (150/210) throw companions most of the way round the wheel, which is fine for
# a deliberately polychrome theme and wrong for one named after a colour —
# "Ember" came out teal on the first pass. Colour-named themes get Floating or
# the tighter Analogous/SingleAnalogous; only the two abstract ones spread wide.
THEMES = [
    ("Aurora",     168, 72, 62, "Analogous"),
    ("Nocturne",   250, 58, 26, "SingleAnalogous"),
    ("Sakura",     338, 62, 82, "Analogous"),
    ("Ember",       16, 80, 38, "SingleAnalogous"),
    ("Moss",       128, 44, 30, "SingleAnalogous"),
    ("Vellum",      42, 40, 88, "SingleAnalogous"),
    ("Tidepool",   196, 70, 55, "Analogous"),
    ("Amber Dusk",  38, 74, 42, "SingleAnalogous"),
    ("Iris",       276, 58, 74, "Analogous"),
    ("Basalt",     220, 12, 26, "Floating"),
    ("Citrus",      66, 76, 70, "Analogous"),
    ("Wine",       332, 64, 30, "SingleAnalogous"),
    ("Glacier",    204, 46, 84, "SingleAnalogous"),
    ("Terracotta",  14, 58, 46, "SingleAnalogous"),
    ("Void",       262, 46, 12, "Triadic"),
]

rows, kotlin = [], []
for name, hue, sat, light, harmony in THEMES:
    x, y = position_for(hue, sat)
    pts = apply_harmony(x, y, harmony)
    primary = color_from_pos(x, y, light)
    dark = should_be_dark(primary, 0.5, light < 50)
    cols = [blend(color_from_pos(px, py, light), toolbar_base(dark), 50) for px, py in pts]

    def rgbs(c): return "rgb(%d,%d,%d)" % tuple(c)
    def rgba0(c): return "rgba(%d,%d,%d,0)" % tuple(c)

    if len(cols) == 1:
        bg = rgbs(cols[0])
    elif len(cols) == 2:
        bg = ", ".join([
            f"linear-gradient(135deg, {rgbs(cols[0])} 0%, {rgba0(cols[0])} 100%)",
            f"linear-gradient(-45deg, {rgbs(cols[1])} 0%, {rgba0(cols[1])} 100%)"])
    else:
        c1, c2, c3 = cols[2], cols[0], cols[1]
        bg = ", ".join([
            f"linear-gradient(-5deg, {rgbs(c1)} 10%, {rgba0(c1)} 80%)",
            f"radial-gradient(circle at 95% 0%, {rgbs(c3)} 0%, {rgba0(c3)} 75%)",
            f"radial-gradient(circle at 0% 0%, {rgbs(c2)} 10%, {rgba0(c2)} 70%)"])

    rows.append(f'''<figure><div class="sw" style="background:{bg}"></div>
      <figcaption>{name}<br><span>{harmony} · L{light} · {"dark" if dark else "light"}</span></figcaption></figure>''')
    kotlin.append(f'        Designed("{name}", {x:.1f}, {y:.1f}, {light}, Harmony.{harmony}),')
    print(f"# {name:11} primary=#%02X%02X%02X  {'dark' if dark else 'light'}" % tuple(primary))

open("themes_preview.html", "w").write(f"""<!doctype html><html><head><meta charset="utf-8"><style>
body{{background:#0b0b0f;color:#ccc;font:12px -apple-system,sans-serif;margin:0;padding:22px;
display:grid;grid-template-columns:repeat(5,1fr);gap:16px}}
figure{{margin:0}} .sw{{width:100%;height:150px;border-radius:12px}}
figcaption{{padding-top:6px;font-weight:600}} figcaption span{{font-weight:400;opacity:.45;font-size:11px}}
</style></head><body>{"".join(rows)}</body></html>""")

print("\n".join(kotlin))
