#!/usr/bin/env python3
"""
OSM boundary relations -> provinces.json for VietLens AI.

Two decisions carry this file.

**Simplification happens per OSM *way*, not per province polygon.** Neighbouring
provinces share the very same way objects along their common border, so
simplifying each way once and reusing the result guarantees both sides land on an
identical line. Simplifying each province's ring independently would drift the two
copies apart and open hairline gaps along every internal border.

**Province polygons are clipped against a land mask.** OSM tags coastal ADM1
boundaries out to territorial waters, so raw rings run in straight chords across
the sea — the Gulf of Tonkin sits "inside" Hưng Yên, open water off Vũng Tàu sits
"inside" TP.HCM. Unclipped, Vietnam renders as a slab instead of the S-shape the
whole feature is about. Every province is clipped by the same mask, so the shared
coastline stays consistent between neighbours exactly as the shared ways do.
"""
import json, math, os
from collections import defaultdict

from shapely.geometry import shape, Polygon, MultiPolygon, box
from shapely.ops import unary_union

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, "osm_raw.json")
NE_LAND = os.path.join(HERE, "ne_land.geojson")
OUT = os.path.join(HERE, "provinces.json")
ARCHIPELAGOS = os.path.join(HERE, "archipelagos.json")

# ── Tuning ───────────────────────────────────────────────────
EPS_DEG = 0.0055         # Douglas-Peucker tolerance, degrees (~600 m)
COAST_EPS = 0.0035       # coastline kept finer; it is the silhouette people recognise
MIN_PART_DEG2 = 0.0006   # drop fragments under ~7 km²
MAX_RINGS = 14           # bound the islet count per province
COORD_DP = 4             # ~11 m — far finer than anything drawn on a phone
VN_BBOX = (101.0, 7.0, 116.5, 24.5)

# Rings east of this go in the map's inset boxes rather than the main outline.
# The split is unambiguous: the easternmost mainland/coastal ring reaches 109.47°E
# and the westernmost archipelago ring starts at 111.45°E.
OFFSHORE_LON = 110.5

# Douglas-Peucker tolerance for archipelago islands, ~110 m. They are drawn as
# fixed-size symbols, so all this has to preserve is a bound around the real island.
ARCHIPELAGO_EPS = 0.001

# The 2025 reorganisation, 63 units -> 34. Every entry below was confirmed by
# probing the pre-merger provincial capital against the generated polygons
# (see verify.py) rather than typed from memory. Provinces that kept their
# territory unchanged are absent.
MERGED_FROM = {
    "An Giang": ["An Giang", "Kiên Giang"],
    "Bắc Ninh": ["Bắc Ninh", "Bắc Giang"],
    "Cà Mau": ["Cà Mau", "Bạc Liêu"],
    "Cần Thơ": ["Cần Thơ", "Sóc Trăng", "Hậu Giang"],
    "Gia Lai": ["Gia Lai", "Bình Định"],
    "Hưng Yên": ["Hưng Yên", "Thái Bình"],
    "Hải Phòng": ["Hải Phòng", "Hải Dương"],
    "Hồ Chí Minh": ["Hồ Chí Minh", "Bà Rịa – Vũng Tàu", "Bình Dương"],
    "Khánh Hòa": ["Khánh Hòa", "Ninh Thuận"],
    "Lào Cai": ["Lào Cai", "Yên Bái"],
    "Lâm Đồng": ["Lâm Đồng", "Đắk Nông", "Bình Thuận"],
    "Ninh Bình": ["Ninh Bình", "Hà Nam", "Nam Định"],
    "Phú Thọ": ["Phú Thọ", "Vĩnh Phúc", "Hòa Bình"],
    "Quảng Ngãi": ["Quảng Ngãi", "Kon Tum"],
    "Quảng Trị": ["Quảng Trị", "Quảng Bình"],
    "Thái Nguyên": ["Thái Nguyên", "Bắc Kạn"],
    "Tuyên Quang": ["Tuyên Quang", "Hà Giang"],
    "Tây Ninh": ["Tây Ninh", "Long An"],
    "Vĩnh Long": ["Vĩnh Long", "Bến Tre", "Trà Vinh"],
    "Đà Nẵng": ["Đà Nẵng", "Quảng Nam"],
    "Đắk Lắk": ["Đắk Lắk", "Phú Yên"],
    "Đồng Nai": ["Đồng Nai", "Bình Phước"],
    "Đồng Tháp": ["Đồng Tháp", "Tiền Giang"],
}


# ── Douglas-Peucker (way level) ──────────────────────────────
def _perp2(p, a, b):
    k = math.cos(math.radians(a[1]))
    px, py = (p[0] - a[0]) * k, p[1] - a[1]
    bx, by = (b[0] - a[0]) * k, b[1] - a[1]
    den = bx * bx + by * by
    if den == 0:
        return px * px + py * py
    t = max(0.0, min(1.0, (px * bx + py * by) / den))
    dx, dy = px - t * bx, py - t * by
    return dx * dx + dy * dy


def simplify(pts, eps):
    if len(pts) < 3:
        return list(pts)
    eps2 = eps * eps
    keep = [False] * len(pts)
    keep[0] = keep[-1] = True
    stack = [(0, len(pts) - 1)]
    while stack:
        i, j = stack.pop()
        if j <= i + 1:
            continue
        worst, wi = -1.0, -1
        a, b = pts[i], pts[j]
        for k in range(i + 1, j):
            d = _perp2(pts[k], a, b)
            if d > worst:
                worst, wi = d, k
        if worst > eps2:
            keep[wi] = True
            stack.append((i, wi))
            stack.append((wi, j))
    return [p for p, k in zip(pts, keep) if k]


def key(p):
    return (round(p[0], 7), round(p[1], 7))


def stitch(segments):
    """Join way segments end-to-end into closed rings."""
    if not segments:
        return []
    ends = defaultdict(list)
    for i, seg in enumerate(segments):
        ends[key(seg[0])].append(i)
        ends[key(seg[-1])].append(i)
    used, rings = [False] * len(segments), []
    for start in range(len(segments)):
        if used[start]:
            continue
        used[start] = True
        ring = list(segments[start])
        while key(ring[0]) != key(ring[-1]):
            tail = key(ring[-1])
            nxt = next((i for i in ends.get(tail, []) if not used[i]), None)
            if nxt is None:
                break
            used[nxt] = True
            seg = segments[nxt]
            ring.extend(seg[1:] if key(seg[0]) == tail else list(reversed(seg))[1:])
        if len(ring) >= 4:
            rings.append(ring)
    return rings


def clean_name(raw):
    for prefix in ("Thành phố ", "Tỉnh "):
        if raw.startswith(prefix):
            return raw[len(prefix):]
    return raw


def clean_name_en(raw, fallback):
    """OSM gives "An Giang Province" / "Can Tho City"; the UI wants the bare name."""
    if not raw:
        return fallback
    for suffix in (" Province", " City", " province", " city"):
        if raw.endswith(suffix):
            return raw[: -len(suffix)]
    return raw


def load_archipelagos():
    """
    Hoàng Sa and Trường Sa, as real islands rather than as boundary artefacts.

    Neither arrives usably from the `admin_level=4` relations. OSM keeps Hoàng Sa out
    of Đà Nẵng's relation altogether — the archipelago is disputed, so it sits outside
    every national admin hierarchy — which left alone produces a lopsided map with one
    Vietnamese archipelago drawn and the other silently absent. Khánh Hòa's relation
    *does* include Trường Sa, but only as three coarse administrative blobs up to 1.3°
    across, which drawn as islands are three enormous meaningless circles.

    Vietnam administers Hoàng Sa as a district of Đà Nẵng and Trường Sa as a district
    of Khánh Hòa, and a map published in Vietnam shows both. So both are taken from
    OSM's own island/islet/reef features — real surveyed geometry, nothing drawn by
    hand — in one clearly labelled step, because attaching Hoàng Sa to Đà Nẵng is a
    claim the boundary data does not itself make.
    """
    if not os.path.exists(ARCHIPELAGOS):
        print("  !! archipelagos.json missing — Hoàng Sa and Trường Sa will be absent")
        return {}
    data = json.load(open(ARCHIPELAGOS, encoding="utf-8"))
    by_province = defaultdict(list)
    for group in data.get("archipelagos", []):
        rings = []
        for f in group.get("features", []):
            ring = f.get("ring") or []
            if len(ring) < 3:
                continue
            # Simplified hard. These islands span ~800 m inside a 200-km box, so the
            # map draws them as fixed-size symbols however many vertices they carry;
            # a faithful 158-point reef outline would be 40x the bytes for a shape
            # nobody can ever see. What is left still bounds the real island, which
            # is what a GPS fix is tested against.
            ring = simplify([(x, y) for x, y in ring], ARCHIPELAGO_EPS)
            if len(ring) < 3:
                continue
            flat = []
            for x, y in ring:
                flat.append(round(x, COORD_DP))
                flat.append(round(y, COORD_DP))
            rings.append(flat)
        by_province[group["province"]].extend(rings)
        print(f"{group['name']}: {len(rings)} islands/reefs -> {group['province']}")
    return dict(by_province)


def to_polygons(geom):
    if geom.is_empty:
        return []
    if isinstance(geom, Polygon):
        return [geom]
    if isinstance(geom, MultiPolygon):
        return list(geom.geoms)
    return [g for g in getattr(geom, "geoms", []) if isinstance(g, Polygon)]


def main():
    # ── land mask ──
    print("building land mask...", flush=True)
    ne = json.load(open(NE_LAND, encoding="utf-8"))
    land = unary_union([shape(f["geometry"]) for f in ne["features"]])
    land = land.intersection(box(*VN_BBOX))
    land = land.simplify(COAST_EPS, preserve_topology=True)
    if not land.is_valid:
        land = land.buffer(0)
    print(f"  land parts: {len(to_polygons(land))}")

    # ── OSM relations ──
    data = json.load(open(RAW, encoding="utf-8"))
    rels = [e for e in data["elements"] if e.get("type") == "relation"]
    vn = [r for r in rels
          if r.get("tags", {}).get("ISO3166-2", "").startswith("VN-")
          or r.get("tags", {}).get("name", "").startswith(("Tỉnh ", "Thành phố "))]
    print(f"vietnamese ADM1: {len(vn)}")

    # ── way-level simplification (the topology fix) ──
    way_raw, owners = {}, defaultdict(set)
    for r in vn:
        for m in r.get("members", []):
            if m.get("type") == "way" and "geometry" in m:
                way_raw[m["ref"]] = [(g["lon"], g["lat"]) for g in m["geometry"]]
                owners[m["ref"]].add(r["id"])
    shared = sum(1 for w, o in owners.items() if len(o) > 1)
    way_simple = {w: simplify(p, EPS_DEG) for w, p in way_raw.items()}
    print(f"ways: {len(way_raw)}  shared: {shared} ({100*shared/len(way_raw):.0f}%)  "
          f"points {sum(map(len, way_raw.values()))} -> {sum(map(len, way_simple.values()))}")

    global ARCHIPELAGO_RINGS
    ARCHIPELAGO_RINGS = load_archipelagos()

    provinces = []
    stats = []
    for r in vn:
        t = r["tags"]
        name = clean_name(t.get("name", ""))
        segs = [way_simple[m["ref"]] for m in r.get("members", [])
                if m.get("type") == "way" and m.get("role") != "inner"
                and m["ref"] in way_simple and len(way_simple[m["ref"]]) >= 2]

        polys = []
        for ring in stitch(segs):
            p = Polygon(ring)
            if not p.is_valid:
                p = p.buffer(0)
            polys.extend(to_polygons(p))
        if not polys:
            print(f"  !! {name}: no polygons")
            continue

        whole = unary_union(polys)
        clipped = whole.intersection(land)
        parts = to_polygons(clipped)

        # Islets Natural Earth does not carry (Trường Sa) vanish on clip; fall back
        # to the unclipped ring rather than silently losing the territory.
        recovered = 0
        for src in to_polygons(whole):
            if src.area >= MIN_PART_DEG2 and not src.intersects(land):
                parts.append(src)
                recovered += 1

        parts = [p for p in parts if p.area >= MIN_PART_DEG2] or [max(parts, key=lambda g: g.area)]
        parts.sort(key=lambda g: g.area, reverse=True)
        dropped = max(0, len(parts) - MAX_RINGS)
        parts = parts[:MAX_RINGS]

        # Flat interleaved [lon, lat, lon, lat, …] rather than nested pairs: it is a
        # third smaller on disk and parses into a DoubleArray without boxing 9,000
        # two-element lists on the way.
        mainland, offshore = [], []
        for p in parts:
            flat = []
            for x, y in list(p.exterior.coords)[:-1]:
                flat.append(round(x, COORD_DP))
                flat.append(round(y, COORD_DP))
            (offshore if min(flat[0::2]) > OFFSHORE_LON else mainland).append(flat)

        # Two kinds of ring end up here and both are needed. The ones from the
        # boundary relation are administrative sea areas up to 1.3° across — useless
        # to draw, but they are what makes a photo taken anywhere in the archipelago
        # resolve to its province, including on islands OSM has not mapped. The
        # curated ones are the actual islands, which is what the inset draws. The map
        # tells them apart by size.
        offshore.extend(ARCHIPELAGO_RINGS.get(name, []))

        # Every province has a mainland; a province with only offshore rings would
        # be a bug in the split, not a real place.
        if not mainland:
            print(f"  !! {name}: no mainland rings after the offshore split")
            continue

        xs = [ring[i] for ring in mainland for i in range(0, len(ring), 2)]
        ys = [ring[i] for ring in mainland for i in range(1, len(ring), 2)]
        axs = [ring[i] for ring in mainland + offshore for i in range(0, len(ring), 2)]
        ays = [ring[i] for ring in mainland + offshore for i in range(1, len(ring), 2)]
        cx, cy = max(parts, key=lambda g: g.area).representative_point().coords[0]

        provinces.append({
            "id": t.get("ISO3166-2") or f"VN-r{r['id']}",
            "name": name,
            "nameEn": clean_name_en(t.get("name:en"), name),
            "type": "city" if t.get("name", "").startswith("Thành phố") else "province",
            "mergedFrom": MERGED_FROM.get(name, []),
            "center": [round(cx, COORD_DP), round(cy, COORD_DP)],
            # Mainland only. The map fits the country to the screen from this, and
            # including Trường Sa would squeeze the mainland into the left half of
            # the canvas to make room for open sea.
            "bbox": [round(min(xs), COORD_DP), round(min(ys), COORD_DP),
                     round(max(xs), COORD_DP), round(max(ys), COORD_DP)],
            # Everything, archipelagos included. Resolving a GPS fix rejects on the
            # bounding box before ray casting, so a photo taken on Trường Sa would
            # stamp nothing at all if that test used the mainland box.
            "bboxAll": [round(min(axs), COORD_DP), round(min(ays), COORD_DP),
                        round(max(axs), COORD_DP), round(max(ays), COORD_DP)],
            "polys": mainland,
            "offshore": offshore,
        })
        stats.append((name, len(mainland), sum(len(r) // 2 for r in mainland),
                      len(offshore), recovered, dropped))

    provinces.sort(key=lambda p: p["name"])
    out = {
        "version": 1,
        "source": "OpenStreetMap contributors (ODbL 1.0); coastline clip from Natural Earth (public domain)",
        "note": "polys = mainland outline; offshore = Hoàng Sa / Trường Sa, drawn in the map's inset boxes",
        "provinces": provinces,
    }
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, separators=(",", ":"))

    total = sum(len(r) // 2 for p in provinces for r in p["polys"])
    off = sum(len(p["offshore"]) for p in provinces)
    print(f"\nprovinces: {len(provinces)}  mainland points: {total}  offshore rings: {off}  file: {os.path.getsize(OUT)/1024:.0f} KB\n")
    for name, nr, npts, noff, rec, drop in sorted(stats):
        extra = (f"  +{noff} offshore" if noff else "") + (f"  -{drop} tiny" if drop else "")
        print(f"  {name:<22} rings={nr:<3} pts={npts:<5}{extra}")


if __name__ == "__main__":
    main()
