# Sovereignty map pipeline

Generates `app/src/main/assets/sovereignty_map.json` — the locator map on the
sovereignty statement screen: Việt Nam, the coasts that enclose the Biển Đông, and
Hoàng Sa and Trường Sa drawn where they actually are.

## Regenerating

```bash
pip install shapely pillow

python3 build_region.py      # downloads Natural Earth on first run
# then LOOK at check_region.png — it must read as this sea, with a whole Việt Nam
```

The script writes the asset in place. Delete `ne_50m_admin_0_countries.geojson` and
`check_region.png` afterwards; both are working files, not part of the project.

## Three sources, because no one of them has all three layers

| Layer | Source | Why not the others |
| --- | --- | --- |
| Việt Nam | `data/src/main/assets/provinces.json`, dissolved | It is already the country this app draws. Taking it from Natural Earth instead would put a second, slightly different Việt Nam in the same build. |
| Neighbouring coasts | Natural Earth 1:50m countries (public domain) | Context only, and nothing else in the project covers land outside Việt Nam. |
| The archipelagos | `tools/provinces/archipelagos.json` | Both are absent or unusable in the boundary relations — see that pipeline's README. |

**Natural Earth's `Paracel Islands` and `Spratly Islands` features are dropped**, not
drawn as neighbours. On this screen they are Vietnamese, and they are drawn from the
app's own data a layer further up.

## The frame is load-bearing

`FRAME = (97.0, 3.0, 131.0, 24.2)`, and the card sizes itself to that rectangle's
projected aspect rather than the rectangle being fitted into a card of some other
shape. Two things it must keep:

- **The whole country.** Lũng Cú is at 23.39°N, so a round 23.0 clips the top off
  Việt Nam — which is not something this screen of all screens can ship.
- **A shore on every side of the sea.** The archipelagos read as being *somewhere*
  only if the water around them is bounded. Crop to Việt Nam and the two clusters
  float in an empty rectangle.

## Everything here is a symbol, not a survey

Islands are drawn as dots of a fixed size and thinned to at most 14 per archipelago.
Hoàng Sa's islets are under a kilometre across inside a 200 km box: at this scale a
faithful dot is a fraction of a pixel, so the choice is a symbol or nothing. Simplify
tolerances are set to about a pixel at the size the card is drawn — finer detail is
invisible and only costs file size.
