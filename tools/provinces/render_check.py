#!/usr/bin/env python3
"""Render provinces.json so a human can actually look at it before it ships."""
import json, math, os, colorsys
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
d = json.load(open(os.path.join(HERE, "provinces.json"), encoding="utf-8"))
provs = d["provinces"]

W, H = 900, 1500
PAD = 20
LAT0 = 16.2
K = math.cos(math.radians(LAT0))

xs = [p[0] for pr in provs for ring in pr["polys"] for p in ring]
ys = [p[1] for pr in provs for ring in pr["polys"] for p in ring]
minx, maxx, miny, maxy = min(xs) * K, max(xs) * K, min(ys), max(ys)
sx = (W - 2 * PAD) / (maxx - minx)
sy = (H - 2 * PAD) / (maxy - miny)
s = min(sx, sy)


def proj(p):
    return (PAD + (p[0] * K - minx) * s, H - PAD - (p[1] - miny) * s)


img = Image.new("RGB", (W, H), (12, 20, 32))
dr = ImageDraw.Draw(img)
for i, pr in enumerate(provs):
    h = (i * 0.618) % 1.0
    r, g, b = colorsys.hsv_to_rgb(h, 0.55, 0.85)
    col = (int(r * 255), int(g * 255), int(b * 255))
    for ring in pr["polys"]:
        pts = [proj(p) for p in ring]
        if len(pts) >= 3:
            dr.polygon(pts, fill=col, outline=(10, 14, 20))

out = os.path.join(HERE, "check_map.png")
img.save(out)
print("wrote", out)

# Sanity numbers
print(f"lon {min(xs):.2f}..{max(xs):.2f}   lat {min(ys):.2f}..{max(ys):.2f}")
print(f"provinces={len(provs)} rings={sum(len(p['polys']) for p in provs)}")

# Phú Quốc lives near 103.96 E, 10.22 N — which province claims it?
target = (103.96, 10.22)


def inside(pt, ring):
    x, y = pt
    c = False
    n = len(ring)
    for i in range(n):
        x1, y1 = ring[i]
        x2, y2 = ring[(i + 1) % n]
        if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
            c = not c
    return c


hits = [pr["name"] for pr in provs for ring in pr["polys"] if inside(target, ring)]
print("Phú Quốc (103.96,10.22) falls inside:", hits or "NOTHING — island missing")

for probe, label in [((105.85, 21.03), "Hà Nội centre"),
                     ((106.70, 10.78), "TP.HCM centre"),
                     ((108.22, 16.06), "Đà Nẵng centre"),
                     ((105.00, 9.18), "Cà Mau tip")]:
    hits = [pr["name"] for pr in provs for ring in pr["polys"] if inside(probe, ring)]
    print(f"{label:<16} -> {hits or 'NOTHING'}")
