#!/usr/bin/env python3
"""Gate before any Kotlin gets written: do real coordinates land in the right province?"""
import json, os
from shapely.geometry import shape, Point, Polygon
from shapely.ops import unary_union

HERE = os.path.dirname(os.path.abspath(__file__))
d = json.load(open(os.path.join(HERE, "provinces.json"), encoding="utf-8"))
provs = d["provinces"]

geoms = {}
for p in provs:
    parts = [Polygon(r) for r in p["polys"] if len(r) >= 3]
    parts = [g if g.is_valid else g.buffer(0) for g in parts]
    geoms[p["name"]] = unary_union(parts)

# (lat, lon, expected province after the 2025 merger)
CASES = [
    (21.0287, 105.8524, "Hà Nội", "Hà Nội"),
    (20.8449, 106.6881, "Hải Phòng", "Hải Phòng"),
    (20.9373, 106.3145, "Hải Phòng", "Hải Dương -> Hải Phòng"),
    (20.9517, 107.0784, "Quảng Ninh", "Hạ Long"),
    (19.8067, 105.7852, "Thanh Hóa", "Thanh Hóa"),
    (18.6796, 105.6813, "Nghệ An", "Vinh"),
    (18.3559, 105.8877, "Hà Tĩnh", "Hà Tĩnh"),
    (17.4689, 106.6223, "Quảng Trị", "Đồng Hới (Quảng Bình -> Quảng Trị)"),
    (16.8163, 107.1003, "Quảng Trị", "Đông Hà"),
    (16.4637, 107.5909, "Huế", "Huế"),
    (16.0544, 108.2022, "Đà Nẵng", "Đà Nẵng"),
    (15.5736, 108.4740, "Đà Nẵng", "Tam Kỳ (Quảng Nam -> Đà Nẵng)"),
    (15.1214, 108.8044, "Quảng Ngãi", "Quảng Ngãi"),
    (13.7830, 109.2196, "Gia Lai", "Quy Nhơn (Bình Định -> Gia Lai)"),
    (13.9833, 108.0000, "Gia Lai", "Pleiku"),
    (13.0882, 109.0929, "Đắk Lắk", "Tuy Hòa (Phú Yên -> Đắk Lắk)"),
    (12.6667, 108.0500, "Đắk Lắk", "Buôn Ma Thuột"),
    (12.2388, 109.1967, "Khánh Hòa", "Nha Trang"),
    (11.5645, 108.9887, "Khánh Hòa", "Phan Rang (Ninh Thuận -> Khánh Hòa)"),
    (11.9404, 108.4583, "Lâm Đồng", "Đà Lạt"),
    (10.9280, 108.1020, "Lâm Đồng", "Phan Thiết (Bình Thuận -> Lâm Đồng)"),
    (10.9574, 106.8426, "Đồng Nai", "Biên Hòa"),
    (10.7769, 106.7009, "Hồ Chí Minh", "TP.HCM"),
    (10.3460, 107.0843, "Hồ Chí Minh", "Vũng Tàu (BR-VT -> TP.HCM)"),
    (10.9804, 106.6519, "Hồ Chí Minh", "Thủ Dầu Một (Bình Dương -> TP.HCM)"),
    (11.3100, 106.0983, "Tây Ninh", "Tây Ninh"),
    (10.5350, 106.4130, "Tây Ninh", "Tân An (Long An -> Tây Ninh)"),
    (10.3600, 106.3600, "Đồng Tháp", "Mỹ Tho (Tiền Giang -> Đồng Tháp)"),
    (10.4600, 105.6300, "Đồng Tháp", "Cao Lãnh"),
    (10.2500, 105.9700, "Vĩnh Long", "Vĩnh Long"),
    (9.9347, 106.3453, "Vĩnh Long", "Trà Vinh -> Vĩnh Long"),
    (10.2415, 106.3759, "Vĩnh Long", "Bến Tre -> Vĩnh Long"),
    (10.0452, 105.7469, "Cần Thơ", "Cần Thơ"),
    (9.6025, 105.9739, "Cần Thơ", "Sóc Trăng -> Cần Thơ"),
    (9.7840, 105.4700, "Cần Thơ", "Vị Thanh (Hậu Giang -> Cần Thơ)"),
    (10.0125, 105.0808, "An Giang", "Rạch Giá (Kiên Giang -> An Giang)"),
    (10.3860, 105.4350, "An Giang", "Long Xuyên"),
    (10.2270, 103.9640, "An Giang", "Phú Quốc"),
    (9.1769, 105.1524, "Cà Mau", "Cà Mau"),
    (9.2940, 105.7215, "Cà Mau", "Bạc Liêu -> Cà Mau"),
    (22.4856, 103.9707, "Lào Cai", "Lào Cai"),
    (21.7050, 104.8700, "Lào Cai", "Yên Bái -> Lào Cai"),
    (21.3833, 103.0167, "Điện Biên", "Điện Biên Phủ"),
    (21.3270, 103.9141, "Sơn La", "Sơn La"),
    (22.3960, 103.4580, "Lai Châu", "Lai Châu"),
    (22.8233, 104.9836, "Tuyên Quang", "Hà Giang -> Tuyên Quang"),
    (21.8230, 105.2140, "Tuyên Quang", "Tuyên Quang"),
    (22.6660, 106.2570, "Cao Bằng", "Cao Bằng"),
    (22.1470, 105.8340, "Thái Nguyên", "Bắc Kạn -> Thái Nguyên"),
    (21.5942, 105.8482, "Thái Nguyên", "Thái Nguyên"),
    (21.8530, 106.7610, "Lạng Sơn", "Lạng Sơn"),
    (21.3227, 105.4024, "Phú Thọ", "Việt Trì"),
    (20.8133, 105.3383, "Phú Thọ", "Hòa Bình -> Phú Thọ"),
    (21.3089, 105.6047, "Phú Thọ", "Vĩnh Yên (Vĩnh Phúc -> Phú Thọ)"),
    (21.1861, 106.0763, "Bắc Ninh", "Bắc Ninh"),
    (21.2730, 106.1946, "Bắc Ninh", "Bắc Giang -> Bắc Ninh"),
    (20.6464, 106.0511, "Hưng Yên", "Hưng Yên"),
    (20.4463, 106.3366, "Hưng Yên", "Thái Bình -> Hưng Yên"),
    (20.2506, 105.9744, "Ninh Bình", "Ninh Bình"),
    (20.4388, 106.1621, "Ninh Bình", "Nam Định -> Ninh Bình"),
    (20.5410, 105.9130, "Ninh Bình", "Phủ Lý (Hà Nam -> Ninh Bình)"),
]

print(f"=== {len(CASES)} coordinate probes ===")
fails = []
for lat, lon, expect, label in CASES:
    pt = Point(lon, lat)
    hits = [n for n, g in geoms.items() if g.contains(pt)]
    ok = hits == [expect]
    if not ok:
        fails.append((label, expect, hits))
    print(f"  {'ok ' if ok else 'FAIL'} {label:<40} -> {hits or 'NOTHING'}")

print(f"\npassed {len(CASES)-len(fails)}/{len(CASES)}")
for label, expect, hits in fails:
    print(f"  FAIL {label}: expected {expect}, got {hits}")

print("\n=== overlaps between provinces (should be ~0) ===")
names = list(geoms)
worst = []
for i in range(len(names)):
    for j in range(i + 1, len(names)):
        a, b = geoms[names[i]], geoms[names[j]]
        if not a.intersects(b):
            continue
        ov = a.intersection(b).area
        if ov > 1e-6:
            worst.append((ov, names[i], names[j]))
worst.sort(reverse=True)
if worst:
    for ov, n1, n2 in worst[:10]:
        print(f"  {ov:.6f} deg²  {n1} / {n2}")
else:
    print("  none above 1e-6 deg²")

print("\n=== coverage ===")
allg = unary_union(list(geoms.values()))
tot = sum(g.area for g in geoms.values())
print(f"  sum of parts {tot:.4f} deg²   union {allg.area:.4f} deg²   "
      f"double-counted {tot-allg.area:.4f}")
print(f"  union pieces: {len(allg.geoms) if hasattr(allg,'geoms') else 1}")
