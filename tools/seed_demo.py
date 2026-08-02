#!/usr/bin/env python3
"""
Seed the dev build with discoveries so the passport map can be checked with photos in it.

provinceId is deliberately left NULL: that forces the app's backfill path to resolve
every row on open, which exercises the same geometry a real capture would use.
"""
import json, os, sqlite3, subprocess, sys, uuid, time
from PIL import Image, ImageDraw

PKG = "com.duylt.trave.vietlensai.dev"
HERE = os.path.dirname(os.path.abspath(__file__))
DB_LOCAL = os.path.join(HERE, "vietlens.db")

# (title, lat, lon, expected province, two colours for the fake photo)
SPOTS = [
    ("Văn Miếu – Quốc Tử Giám", 21.0287, 105.8524, "Hà Nội", (196, 92, 60), (250, 214, 137)),
    ("Sa Pa", 22.3364, 103.8438, "Lào Cai", (74, 124, 118), (206, 232, 199)),
    ("Vịnh Hạ Long", 20.9000, 107.1500, "Quảng Ninh", (42, 112, 148), (176, 226, 231)),
    ("Đại Nội Huế", 16.4637, 107.5909, "Huế", (150, 46, 46), (243, 200, 128)),
    ("Cầu Rồng", 16.0544, 108.2022, "Đà Nẵng", (198, 74, 52), (255, 206, 148)),
    ("Bãi biển Nha Trang", 12.2388, 109.1967, "Khánh Hòa", (28, 130, 168), (250, 235, 190)),
    ("Hồ Xuân Hương, Đà Lạt", 11.9404, 108.4583, "Lâm Đồng", (58, 106, 76), (222, 234, 176)),
    ("Chợ Bến Thành", 10.7769, 106.7009, "Hồ Chí Minh", (176, 84, 96), (252, 224, 176)),
    ("Phú Quốc", 10.2270, 103.9640, "An Giang", (24, 138, 152), (255, 240, 200)),
]


def sh(cmd, **kw):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True, **kw)


def make_photo(path, c1, c2, label):
    """A stand-in for a real capture: a gradient with some shapes, 1600x1200."""
    w, h = 1600, 1200
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)],
               fill=(int(c1[0] + (c2[0] - c1[0]) * t),
                     int(c1[1] + (c2[1] - c1[1]) * t),
                     int(c1[2] + (c2[2] - c1[2]) * t)))
    d.ellipse([w * 0.55, h * 0.10, w * 0.85, h * 0.40], fill=(255, 255, 255, 90))
    for i in range(6):
        x = w * (0.08 + i * 0.14)
        d.polygon([(x, h * 0.95), (x + w * 0.06, h * 0.55), (x + w * 0.12, h * 0.95)],
                  fill=(max(0, c1[0] - 40), max(0, c1[1] - 40), max(0, c1[2] - 40)))
    d.rectangle([0, h * 0.88, w, h], fill=(max(0, c2[0] - 60), max(0, c2[1] - 60), max(0, c2[2] - 60)))
    img.save(path, "JPEG", quality=88)


def main():
    print("stopping app…")
    sh(f"adb shell am force-stop {PKG}")
    time.sleep(1)

    # ── pull the database (checkpointing anything still in the WAL) ──
    for suffix in ("", "-wal", "-shm"):
        sh(f"adb shell run-as {PKG} cat databases/vietlens.db{suffix} > '{DB_LOCAL}{suffix}'")
    if not os.path.getsize(DB_LOCAL):
        sys.exit("database is empty — open the app once first")
    print(f"pulled db: {os.path.getsize(DB_LOCAL)} bytes")

    conn = sqlite3.connect(DB_LOCAL)
    conn.execute("PRAGMA journal_mode=DELETE")  # fold the WAL back in
    cols = [r[1] for r in conn.execute("PRAGMA table_info(discoveries)")]
    print("columns:", cols)
    assert "provinceId" in cols, "migration did not run — provinceId column missing"

    conn.execute("DELETE FROM discoveries")
    now = int(time.time() * 1000)
    photos = []
    for i, (title, lat, lon, expected, c1, c2) in enumerate(SPOTS):
        local_jpg = os.path.join(HERE, f"seed_{i}.jpg")
        make_photo(local_jpg, c1, c2, title)
        device_path = f"/data/data/{PKG}/files/captures/capture_seed_{i}.jpg"
        photos.append((local_jpg, device_path))

        conn.execute(
            """INSERT INTO discoveries (id,title,localName,category,imagePath,summary,
               sectionsJson,funFactsJson,tagsJson,nearbyJson,suggestedQuestionsJson,
               confidence,latitude,longitude,provinceId,placeHint,isFavorite,modelUsed,createdAt)
               VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (str(uuid.uuid4()), title, None, "LANDMARK", device_path,
             f"Ảnh mẫu tại {expected}.", "[]", "[]", "[]", "[]", "[]",
             0.9, lat, lon, None, expected, 0, "seed", now - i * 3_600_000),
        )
    conn.commit()
    conn.execute("VACUUM")
    conn.close()
    print(f"inserted {len(SPOTS)} discoveries with provinceId = NULL (backfill will resolve them)")

    # ── push photos ──
    sh(f"adb shell run-as {PKG} mkdir -p files/captures")
    for local_jpg, device_path in photos:
        tmp = f"/data/local/tmp/{os.path.basename(device_path)}"
        sh(f"adb push '{local_jpg}' {tmp}")
        sh(f"adb shell run-as {PKG} cp {tmp} files/captures/{os.path.basename(device_path)}")
        sh(f"adb shell rm -f {tmp}")
    listing = sh(f"adb shell run-as {PKG} ls -l files/captures").stdout
    print("photos on device:", len(listing.strip().splitlines()))

    # ── push the database back ──
    sh(f"adb shell run-as {PKG} rm -f databases/vietlens.db-wal databases/vietlens.db-shm")
    sh(f"adb push '{DB_LOCAL}' /data/local/tmp/vietlens.db")
    sh(f"adb shell run-as {PKG} cp /data/local/tmp/vietlens.db databases/vietlens.db")
    sh("adb shell rm -f /data/local/tmp/vietlens.db")
    print("database pushed")


if __name__ == "__main__":
    main()
