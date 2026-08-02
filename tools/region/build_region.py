"""Builds app/src/main/assets/sovereignty_map.json — the locator map on the
sovereignty statement screen.

Three layers, from three sources, because no single one carries all of them:

- Việt Nam, dissolved from the app's own provinces.json, so the country on this
  screen is the same geometry the passport map draws.
- The neighbouring coastlines, from Natural Earth 1:50m country polygons. They are
  context only — the frame exists so the two archipelagos can be seen where they
  actually are, in a sea with shores on every side.
- The islands of Hoàng Sa and Trường Sa, from archipelagos.json, thinned to the
  handful of symbols that fit at this scale.

Natural Earth files Hoàng Sa and Trường Sa as separate "Paracel Islands" and
"Spratly Islands" entities. They are dropped from the neighbour layer outright: on
this screen they are Vietnamese, and the app says so from its own data.

    pip install shapely pillow
    python3 build_region.py        # writes the asset and check_region.png — look at it
"""

import json
import os
import subprocess

from shapely.geometry import Polygon, box, shape
from shapely.ops import unary_union

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))

PROVINCES = os.path.join(REPO, "data", "src", "main", "assets", "provinces.json")
ARCHIPELAGOS = os.path.join(REPO, "tools", "provinces", "archipelagos.json")
OUT = os.path.join(REPO, "app", "src", "main", "assets", "sovereignty_map.json")
CHECK_PNG = os.path.join(HERE, "check_region.png")

NE_URL = (
    "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/"
    "geojson/ne_50m_admin_0_countries.geojson"
)
NE_CACHE = os.path.join(HERE, "ne_50m_admin_0_countries.geojson")

# The frame. Wide enough that both archipelagos sit well inside it with a shore on
# every side — Hải Nam and the south China coast above, the Philippines to the east,
# Borneo below, the peninsula to the west — and no wider, or Việt Nam shrinks to a
# thread down the left edge.
#
# The top is 24.2°N rather than a round 23°: Lũng Cú, the northernmost point of the
# country, is at 23.39°, and a frame that clips the top off Việt Nam is not a frame
# this screen can use.
FRAME = (97.0, 3.0, 131.0, 24.2)

# One pixel is ~0.1° at the size this is drawn, so anything finer than this is
# invisible and only costs file size.
SIMPLIFY_NEIGHBOURS = 0.02
SIMPLIFY_VIETNAM = 0.012

# Roughly a pixel of area. Below it an island is a speck of noise on the sea.
MIN_AREA_NEIGHBOURS = 0.012
MIN_AREA_VIETNAM = 0.004

# Hoàng Sa's islets are ~800 m across in a 200 km box; drawn to scale they are
# nothing. They are symbols here, so what matters is how many and roughly where.
ISLAND_GRID_DEGREES = 0.3
MAX_ISLANDS_PER_ARCHIPELAGO = 14

NOT_NEIGHBOURS = {"Vietnam", "Paracel Islands", "Spratly Islands", "Scarborough Reef"}


def download_natural_earth():
    # curl rather than urllib: a python.org build on macOS ships without a CA
    # bundle, and this is the same one-liner the provinces pipeline already uses.
    if not os.path.exists(NE_CACHE):
        print("downloading Natural Earth 1:50m countries…")
        subprocess.run(["curl", "-sL", "--max-time", "180", "-o", NE_CACHE, NE_URL], check=True)
    with open(NE_CACHE) as f:
        return json.load(f)


def rings_of(geometry, min_area):
    """Exterior rings big enough to see, as flat [lon, lat, lon, lat, …] arrays."""
    if geometry.is_empty:
        return []
    parts = geometry.geoms if geometry.geom_type.startswith("Multi") else [geometry]
    out = []
    for part in parts:
        if part.geom_type != "Polygon" or part.area < min_area:
            continue
        flat = []
        for x, y in part.exterior.coords[:-1]:  # consumers close the ring themselves
            flat.append(round(x, 4))
            flat.append(round(y, 4))
        if len(flat) >= 6:
            out.append(flat)
    return out


def build_vietnam(frame):
    with open(PROVINCES) as f:
        data = json.load(f)
    polygons = []
    for province in data["provinces"]:
        # provinces.json rings are flat [lon, lat, lon, lat, …], the shape the map
        # renderer wants; shapely wants pairs.
        for ring in province["polys"]:
            points = [(ring[i], ring[i + 1]) for i in range(0, len(ring), 2)]
            if len(points) >= 3:
                polygons.append(Polygon(points).buffer(0))
    dissolved = unary_union(polygons)
    clipped = dissolved.intersection(frame).simplify(SIMPLIFY_VIETNAM, preserve_topology=True)
    return rings_of(clipped, MIN_AREA_VIETNAM)


def build_neighbours(frame):
    countries = download_natural_earth()
    out = []
    for feature in countries["features"]:
        props = feature["properties"]
        name = props.get("ADMIN") or props.get("NAME") or ""
        if name in NOT_NEIGHBOURS:
            continue
        geometry = shape(feature["geometry"])
        if not geometry.intersects(frame):
            continue
        clipped = geometry.intersection(frame).buffer(0)
        clipped = clipped.simplify(SIMPLIFY_NEIGHBOURS, preserve_topology=True)
        out.extend(rings_of(clipped, MIN_AREA_NEIGHBOURS))
    return out


def build_islands(name):
    with open(ARCHIPELAGOS) as f:
        data = json.load(f)
    group = next(g for g in data["archipelagos"] if g["name"] == name)

    # Biggest first, so thinning keeps the islands a map would actually name.
    features = []
    for feature in group["features"]:
        ring = feature["ring"]
        xs = [p[0] for p in ring]
        ys = [p[1] for p in ring]
        span = max(max(xs) - min(xs), max(ys) - min(ys))
        features.append((span, sum(xs) / len(xs), sum(ys) / len(ys)))
    features.sort(reverse=True)

    seen = set()
    points = []
    for _, cx, cy in features:
        cell = (round(cx / ISLAND_GRID_DEGREES), round(cy / ISLAND_GRID_DEGREES))
        if cell in seen:
            continue
        seen.add(cell)
        points.append(round(cx, 4))
        points.append(round(cy, 4))
        if len(points) // 2 == MAX_ISLANDS_PER_ARCHIPELAGO:
            break
    return points


def render_check(payload):
    """Writes a PNG of exactly what the app will draw. Open it; it must look right."""
    from PIL import Image, ImageDraw

    min_lon, min_lat, max_lon, max_lat = payload["frame"]

    # Same cosine correction the renderer applies, so this really is a preview of
    # the card rather than a stretched approximation of it.
    import math

    cos_latitude = math.cos(math.radians((min_lat + max_lat) / 2))
    height = 600
    width = round(height * (max_lon - min_lon) * cos_latitude / (max_lat - min_lat))
    image = Image.new("RGB", (width, height), (140, 22, 26))
    draw = ImageDraw.Draw(image)

    def project(lon, lat):
        x = (lon - min_lon) / (max_lon - min_lon) * width
        y = (max_lat - lat) / (max_lat - min_lat) * height
        return x, y

    def polygon(flat, fill):
        points = [project(flat[i], flat[i + 1]) for i in range(0, len(flat), 2)]
        if len(points) >= 3:
            draw.polygon(points, fill=fill)

    for ring in payload["neighbours"]:
        polygon(ring, (196, 150, 148))
    for ring in payload["vietnam"]:
        polygon(ring, (245, 226, 210))
    for key, colour in (("hoangSa", (230, 175, 60)), ("truongSa", (230, 175, 60))):
        points = payload[key]
        for i in range(0, len(points), 2):
            x, y = project(points[i], points[i + 1])
            draw.ellipse([x - 4, y - 4, x + 4, y + 4], fill=colour)

    image.save(CHECK_PNG)


def main():
    frame = box(*FRAME)
    payload = {
        "source": (
            "Việt Nam dissolved from provinces.json (OpenStreetMap, ODbL 1.0); "
            "neighbouring coastlines from Natural Earth 1:50m (public domain); "
            "Hoàng Sa and Trường Sa islands from archipelagos.json (OpenStreetMap, ODbL 1.0)"
        ),
        "note": (
            "Decorative locator map for the sovereignty statement screen. Rings are "
            "flat [lon, lat, …] and are not closed — consumers close them."
        ),
        "frame": list(FRAME),
        "vietnam": build_vietnam(frame),
        "neighbours": build_neighbours(frame),
        "hoangSa": build_islands("Hoàng Sa"),
        "truongSa": build_islands("Trường Sa"),
    }

    with open(OUT, "w") as f:
        json.dump(payload, f, ensure_ascii=False, separators=(",", ":"))

    vertices = sum(len(r) // 2 for r in payload["vietnam"] + payload["neighbours"])
    print(f"vietnam rings   {len(payload['vietnam'])}")
    print(f"neighbour rings {len(payload['neighbours'])}")
    print(f"islands         {len(payload['hoangSa']) // 2} + {len(payload['truongSa']) // 2}")
    print(f"vertices        {vertices}")
    print(f"size            {os.path.getsize(OUT) / 1024:.1f} KB -> {OUT}")

    render_check(payload)
    print(f"check           {CHECK_PNG}")


if __name__ == "__main__":
    main()
