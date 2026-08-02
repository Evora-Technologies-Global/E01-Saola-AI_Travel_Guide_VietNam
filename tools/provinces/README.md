# Province boundary pipeline

Generates `data/src/main/assets/provinces.json` — the 34 post-2025-merger provinces
the travel passport map draws.

## Regenerating

```bash
pip install shapely pillow

# 1. OSM admin_level=4 relations inside Vietnam (~22 MB, takes a few minutes)
printf '%s' '[out:json][timeout:600];relation["boundary"="administrative"]["admin_level"="4"](area:3600049915);out geom;' > q.txt
curl -s --max-time 900 -X POST "https://overpass-api.de/api/interpreter" --data-urlencode data@q.txt -o osm_raw.json

# 2. Natural Earth land polygons, used to clip territorial waters off the outlines
curl -sL "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_10m_land.geojson" -o ne_land.geojson

# 3. Build, then LOOK at the result and check it against real coordinates
python3 build_provinces.py
python3 render_check.py      # writes check_map.png — open it, it must look like Vietnam
python3 verify.py            # 61 coordinate probes + overlap/coverage check

cp provinces.json ../../data/src/main/assets/provinces.json
```

`./gradlew :data:test` then re-checks the shipped file from Kotlin.

## Do not substitute the source

GADM and Natural Earth's admin levels both still carry the **pre-2025 63 provinces**.
The output must contain exactly 34 units. `verify.py` and `ProvinceGeometryTest`
both fail loudly if it does not.

## The archipelagos need a second source

`archipelagos.json` holds Hoàng Sa and Trường Sa as island geometry, gathered from
OSM's `place=island` / `place=islet` / `natural=reef` features rather than from the
boundary relations, because the relations do not carry them usably:

- OSM's Khánh Hòa relation has Trường Sa only as three administrative sea areas up
  to 1.3° across — kept in the output for containment, filtered out of the drawing.
- OSM's Đà Nẵng relation has no Hoàng Sa at all; the disputed archipelago sits
  outside every national admin hierarchy. Attaching it to Đà Nẵng is a claim the
  boundary data does not make, which is why it is a separate, labelled step.

To refresh it, query the two bounding boxes (Paracel `15.5,110.9,17.3,112.6`,
Spratly `7.8,111.2,12.0,115.2`) one simple statement at a time — Overpass rejects
unions and `->` assignments with a 406 — and rebuild the file in the same shape.

## Two decisions that are load-bearing

- **Simplify per OSM way, not per province.** Adjacent provinces share way objects
  along their common border; simplifying each way once keeps both sides identical.
  Per-province simplification opens hairline gaps along every internal border.
- **Clip to the coastline.** OSM extends coastal boundaries into territorial
  waters, which renders Vietnam as a slab instead of the S-shape.
- **Split mainland from offshore at 110.5°E.** `polys` is what the map is fitted to
  and `offshore` is what the inset boxes draw. The split is unambiguous: the
  easternmost coastal ring reaches 109.47°E, the westernmost island starts at
  111.45°E. `bbox` covers the mainland alone; `bboxAll` covers everything, and is
  what containment tests against — using the mainland box there would reject a photo
  taken on Trường Sa before it was ever ray-cast.
