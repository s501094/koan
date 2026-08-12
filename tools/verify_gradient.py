"""Render Zen's real CSS gradient next to the Compose layer model.

Left column  = the exact CSS string getGradient() emits, drawn by the browser.
Right column = my Layer list, expressed as SVG with the same geometry Compose
               will compute (CSS-angle -> two points, farthest-corner radius,
               fade to same-colour-alpha-0).

If a pair differs, the port is wrong.
"""
import math

W = H = 260

# ---- ported maths (mirrors the Kotlin) -------------------------------------
PICKER = 380.0
CFP_C = (PICKER + 60) / 2
CFP_R = (PICKER + 60 - 30) / 2
HARM_C = HARM_R = PICKER / 2


def hue2rgb(p, q, t):
    if t < 0: t += 1
    if t > 1: t -= 1
    if t < 1 / 6: return p + (q - p) * 6 * t
    if t < 1 / 2: return q
    if t < 2 / 3: return p + (q - p) * (2 / 3 - t) * 6
    return p


def hsl2rgb(h, s, l):
    if s == 0:
        r = g = b = l
    else:
        q = l * (1 + s) if l < 0.5 else l + s - l * s
        p = 2 * l - q
        r, g, b = hue2rgb(p, q, h + 1/3), hue2rgb(p, q, h), hue2rgb(p, q, h - 1/3)
    return [math.floor(v * 255 + 0.5) for v in (r, g, b)]


def blend(a, b, pct):
    p = pct / 100
    return [math.floor(a[i] * p + b[i] * (1 - p) + 0.5) for i in range(3)]


def color_from_pos(x, y, lightness, type_="explicit-lightness"):
    x += 29; y += 29
    dist = math.hypot(x - CFP_C, y - CFP_C)
    ang = math.degrees(math.atan2(y - CFP_C, x - CFP_C)) % 360
    nd = 1 - min(dist / CFP_R, 1)
    sat, light = nd * 100, lightness
    if type_ != "explicit-lightness":
        sat = 90 + (1 - nd) * 10
        light = round((1 - nd) * 100)
    if type_ == "explicit-black-white":
        sat, light = 0, round((1 - nd) * 100)
    return hsl2rgb(ang / 360, sat / 100, light / 100)


def apply_harmony(x, y, angles):
    pts = [(x, y)]
    base = math.degrees(math.atan2(y - HARM_C, x - HARM_C)) % 360
    d = min(math.hypot(x - HARM_C, y - HARM_C), HARM_R)
    for off in angles:
        rad = math.radians((base + off) % 360)
        pts.append((HARM_C + d * math.cos(rad), HARM_C + d * math.sin(rad)))
    return pts


def toolbar_base(dark):
    return [23, 23, 26] if dark else [240, 240, 244]


def resolve(pts, lightness, opacity, dark, type_="explicit-lightness"):
    return [blend(color_from_pos(x, y, lightness, type_), toolbar_base(dark), opacity * 100)
            for x, y in pts]


def rgbs(c):
    return "rgb(%d, %d, %d)" % tuple(c)


def rgba0(c):
    return "rgba(%d, %d, %d, 0)" % tuple(c)


# ---- left: the CSS getGradient() actually emits -----------------------------
def zen_css(cols):
    rot = -45
    if len(cols) == 1:
        return rgbs(cols[0])
    if len(cols) == 2:
        layers = [
            f"linear-gradient({rot}deg, {rgbs(cols[1])} 0%, {rgba0(cols[1])} 100%)",
            f"linear-gradient({rot+180}deg, {rgbs(cols[0])} 0%, {rgba0(cols[0])} 100%)",
        ][::-1]
        return ", ".join(layers)
    c1, c2, c3 = cols[2], cols[0], cols[1]
    return ", ".join([
        f"linear-gradient(-5deg, {rgbs(c1)} 10%, {rgba0(c1)} 80%)",
        f"radial-gradient(circle at 95% 0%, {rgbs(c3)} 0%, {rgba0(c3)} 75%)",
        f"radial-gradient(circle at 0% 0%, {rgbs(c2)} 10%, {rgba0(c2)} 70%)",
    ])


# ---- right: my Layer list, with Compose's geometry, drawn as SVG ------------
def linear_points(angle_deg):
    r = math.radians(angle_deg)
    dx, dy = math.sin(r), -math.cos(r)
    length = abs(W * dx) + abs(H * dy)
    cx, cy, half = W / 2, H / 2, length / 2
    return (cx - dx * half, cy - dy * half, cx + dx * half, cy + dy * half)


def farthest_corner(cx, cy):
    return max(math.hypot(cx, cy), math.hypot(W - cx, cy),
               math.hypot(cx, H - cy), math.hypot(W - cx, H - cy))


def compose_svg(cols, uid):
    defs, rects = [], []

    def lin(angle, stop_pos, c, fade):
        i = f"{uid}l{len(defs)}"
        x1, y1, x2, y2 = linear_points(angle)
        defs.append(
            f'<linearGradient id="{i}" gradientUnits="userSpaceOnUse" '
            f'x1="{x1:.2f}" y1="{y1:.2f}" x2="{x2:.2f}" y2="{y2:.2f}">'
            f'<stop offset="{stop_pos}" stop-color="{rgbs(c)}"/>'
            f'<stop offset="{fade}" stop-color="{rgbs(c)}" stop-opacity="0"/></linearGradient>')
        rects.append(f'<rect width="{W}" height="{H}" fill="url(#{i})"/>')

    def rad(fx, fy, stop_pos, c, fade):
        i = f"{uid}r{len(defs)}"
        cx, cy = W * fx, H * fy
        r = farthest_corner(cx, cy)
        defs.append(
            f'<radialGradient id="{i}" gradientUnits="userSpaceOnUse" '
            f'cx="{cx:.2f}" cy="{cy:.2f}" r="{r:.2f}">'
            f'<stop offset="{stop_pos}" stop-color="{rgbs(c)}"/>'
            f'<stop offset="{fade}" stop-color="{rgbs(c)}" stop-opacity="0"/></radialGradient>')
        rects.append(f'<rect width="{W}" height="{H}" fill="url(#{i})"/>')

    if len(cols) == 1:
        rects.append(f'<rect width="{W}" height="{H}" fill="{rgbs(cols[0])}"/>')
    elif len(cols) == 2:
        # bottom-first
        lin(-45, 0, cols[1], 1)
        lin(135, 0, cols[0], 1)
    else:
        rad(0.0, 0.0, 0.10, cols[0], 0.70)
        rad(0.95, 0.0, 0.0, cols[1], 0.75)
        lin(-5, 0.10, cols[2], 0.80)

    return (f'<svg width="{W}" height="{H}" xmlns="http://www.w3.org/2000/svg">'
            f'<defs>{"".join(defs)}</defs>{"".join(rects)}</svg>')


CASES = [
    ("Coral Blend / analogous / 3 dots", 220.0, 187.0, 70, [50, 310], 0.5, False),
    ("Lagoon Blend / analogous / 3 dots", 147.0, 195.0, 60, [50, 310], 0.5, False),
    ("Amethyst Blend / dark / 3 dots", 265.0, 79.0, 40, [50, 310], 0.5, True),
    ("Dusk / complementary / 2 dots", 81.0, 84.0, 50, [180], 0.5, True),
    ("Ember / single dot", 237.0, 210.0, 30, [], 0.5, True),
    ("Coral Blend @ opacity 1.0", 220.0, 187.0, 70, [50, 310], 1.0, False),
]

rows = []
for i, (name, x, y, light, angles, opacity, dark) in enumerate(CASES):
    pts = apply_harmony(x, y, angles)
    cols = resolve(pts, light, opacity, dark)
    rows.append(f"""
    <div class="row">
      <div class="name">{name}<br><span class="sw">{
        " ".join('<i style="background:%s"></i>' % rgbs(c) for c in cols)}</span></div>
      <div class="pair">
        <figure><div class="box" style="background: {zen_css(cols)}"></div>
          <figcaption>Zen CSS (reference)</figcaption></figure>
        <figure>{compose_svg(cols, f"c{i}")}
          <figcaption>Compose layer model</figcaption></figure>
      </div>
    </div>""")

html = f"""<!doctype html><html><head><meta charset="utf-8"><style>
 body {{ background:#0d0d11; color:#ddd; font:13px -apple-system,sans-serif; margin:0; padding:20px; }}
 .row {{ display:flex; align-items:center; gap:20px; margin-bottom:14px; }}
 .name {{ width:210px; }}
 .sw i {{ display:inline-block; width:16px; height:16px; border-radius:3px; margin-top:5px; }}
 .pair {{ display:flex; gap:14px; }}
 figure {{ margin:0; }}
 .box {{ width:{W}px; height:{H}px; }}
 figcaption {{ font-size:11px; opacity:.5; padding-top:3px; }}
</style></head><body>{"".join(rows)}</body></html>"""

open("gradient_verify.html", "w").write(html)
print("cases:", len(CASES))
for name, x, y, light, angles, opacity, dark in CASES:
    cols = resolve(apply_harmony(x, y, angles), light, opacity, dark)
    print(f"  {name:38} " + " ".join("#%02X%02X%02X" % tuple(c) for c in cols))
