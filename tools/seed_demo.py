#!/usr/bin/env python3
"""
Seed a demo device with a week's worth of discoveries, so the journal, the passport map
and the culture collection can be looked at without standing in front of a temple.

Photographs are real: each entry in tools/seed/demo-content.json names a Wikimedia Commons
file, fetched through Special:FilePath and re-encoded to the same 1024 px JPEG the app's
own ImagePolicy produces. Naming the file rather than following an article's lead image is
what makes the set reproducible — an article's lead image changes, and for several of these
it is a location map rather than a photograph.

The re-encoded copies are tracked in git under tools/seed/.work, so a normal run needs no
network at all: a photograph is only fetched when its file is missing.

provinceId is deliberately left NULL: that forces the app's backfill path to resolve every
row on open, which exercises the same geometry a real capture would use.

    python3 tools/seed_demo.py                  # the running Android device/emulator
    python3 tools/seed_demo.py --platform ios   # the booted iOS simulator

Android needs a *debug* build (run-as only works on a debuggable package).
"""
import argparse, json, os, sqlite3, subprocess, sys, time, uuid

HERE = os.path.dirname(os.path.abspath(__file__))
CONTENT = os.path.join(HERE, "seed", "demo-content.json")
WORK = os.path.join(HERE, "seed", ".work")
DB_LOCAL = os.path.join(WORK, "vietlens.db")

ANDROID_PKG = "com.duylt.trave.vietlensai.dev"
IOS_BUNDLE = "com.duylt.trave.vietlensai"
UA = "VietLensAI-seed/1.0 (hackathon demo; lothanhduy2003@gmail.com)"
MAX_EDGE_PX = 1024   # ImagePolicy.MAX_EDGE_PX
JPEG_QUALITY = 85    # ImagePolicy.JPEG_QUALITY


def sh(cmd, **kw):
    return subprocess.run(cmd, shell=True, capture_output=True, text=True, **kw)


def fetch_photo(commons_file, dest):
    """Download a Commons file and re-encode it the way a capture would be stored.

    curl rather than urllib: the system python3 on macOS ships without a usable CA
    bundle, so every https fetch fails certificate verification.
    """
    if os.path.exists(dest):
        return True
    from urllib.parse import quote
    url = f"https://commons.wikimedia.org/wiki/Special:FilePath/{quote(commons_file)}?width=1600"
    raw = dest + ".orig"
    r = subprocess.run(["curl", "-sL", "--fail", "-A", UA, "-o", raw, url])
    if r.returncode != 0 or not os.path.exists(raw) or os.path.getsize(raw) < 1024:
        print(f"  ! could not fetch {commons_file}")
        return False

    from PIL import Image
    with Image.open(raw) as img:
        img = img.convert("RGB")
        longest = max(img.size)
        if longest > MAX_EDGE_PX:
            ratio = MAX_EDGE_PX / longest
            img = img.resize((max(1, round(img.width * ratio)),
                              max(1, round(img.height * ratio))), Image.LANCZOS)
        img.save(dest, "JPEG", quality=JPEG_QUALITY)
    os.remove(raw)
    return True


# ── the two device backends ────────────────────────────────────────────────────

class Android:
    name = "android"

    def __init__(self, serial=None):
        self.adb = "adb" + (f" -s {serial}" if serial else "")

    def stop_app(self):
        sh(f"{self.adb} shell am force-stop {ANDROID_PKG}")

    def pull_db(self):
        for suffix in ("", "-wal", "-shm"):
            sh(f"{self.adb} shell run-as {ANDROID_PKG} cat databases/vietlens.db{suffix} "
               f"> '{DB_LOCAL}{suffix}'")

    def push_db(self):
        sh(f"{self.adb} shell run-as {ANDROID_PKG} rm -f databases/vietlens.db-wal "
           f"databases/vietlens.db-shm")
        sh(f"{self.adb} push '{DB_LOCAL}' /data/local/tmp/vietlens.db")
        sh(f"{self.adb} shell run-as {ANDROID_PKG} cp /data/local/tmp/vietlens.db "
           f"databases/vietlens.db")
        sh(f"{self.adb} shell rm -f /data/local/tmp/vietlens.db")

    def push_photos(self, pairs):
        sh(f"{self.adb} shell run-as {ANDROID_PKG} mkdir -p files/captures")
        for local, name in pairs:
            sh(f"{self.adb} push '{local}' /data/local/tmp/{name}")
            sh(f"{self.adb} shell run-as {ANDROID_PKG} cp /data/local/tmp/{name} "
               f"files/captures/{name}")
            sh(f"{self.adb} shell rm -f /data/local/tmp/{name}")
        return len(sh(f"{self.adb} shell run-as {ANDROID_PKG} ls files/captures")
                   .stdout.split())


class Ios:
    """The simulator keeps the container on the host filesystem, so this is plain file IO."""
    name = "ios"

    def __init__(self, udid="booted"):
        self.udid = udid
        container = sh(f"xcrun simctl get_app_container {udid} {IOS_BUNDLE} data").stdout.strip()
        if not container or not os.path.isdir(container):
            sys.exit(f"could not find the app container — is {IOS_BUNDLE} installed on {udid}?")
        # iosAppSupportDirectory(): Library/Application Support inside the data container.
        self.support = os.path.join(container, "Library", "Application Support")
        self.captures = os.path.join(self.support, "captures")

    def stop_app(self):
        sh(f"xcrun simctl terminate {self.udid} {IOS_BUNDLE}")

    def pull_db(self):
        import shutil
        for suffix in ("", "-wal", "-shm"):
            src = os.path.join(self.support, "vietlens.db" + suffix)
            if os.path.exists(src):
                shutil.copy(src, DB_LOCAL + suffix)
            elif os.path.exists(DB_LOCAL + suffix):
                os.remove(DB_LOCAL + suffix)

    def push_db(self):
        import shutil
        for suffix in ("-wal", "-shm"):
            stale = os.path.join(self.support, "vietlens.db" + suffix)
            if os.path.exists(stale):
                os.remove(stale)
        shutil.copy(DB_LOCAL, os.path.join(self.support, "vietlens.db"))

    def push_photos(self, pairs):
        import shutil
        os.makedirs(self.captures, exist_ok=True)
        for local, name in pairs:
            shutil.copy(local, os.path.join(self.captures, name))
        return len(os.listdir(self.captures))


# ── writing the rows ───────────────────────────────────────────────────────────

def write_rows(conn, content, photos_by_id, day_zero_ms):
    """Replace everything the demo owns. Chat and notes cascade off discoveries."""
    conn.execute("DELETE FROM discoveries")
    conn.execute("DELETE FROM trip_summaries")

    ids = {}
    for item in content["discoveries"]:
        row_id = str(uuid.uuid4())
        ids[item["id"]] = row_id
        created = day_zero_ms - item["day"] * 86_400_000 + item["hour"] * 3_600_000
        conn.execute(
            """INSERT INTO discoveries (id,title,localName,category,imageName,summary,
               sectionsJson,funFactsJson,tagsJson,nearbyJson,suggestedQuestionsJson,
               confidence,latitude,longitude,provinceId,placeHint,isFavorite,modelUsed,createdAt)
               VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (row_id, item["title"], item.get("localName"), item["category"],
             photos_by_id.get(item["id"]), item["summary"],
             json.dumps(item["sections"], ensure_ascii=False),
             json.dumps(item["funFacts"], ensure_ascii=False),
             json.dumps(item["tags"], ensure_ascii=False),
             json.dumps(item["nearby"], ensure_ascii=False),
             json.dumps(item["suggestedQuestions"], ensure_ascii=False),
             item["confidence"], item["lat"], item["lon"], None, item.get("placeHint"),
             1 if item.get("favorite") else 0, "gemini-3-flash-preview", created),
        )

    chat = content["chat"]
    parent = ids.get(chat["discoveryId"])
    if parent:
        base = day_zero_ms - 6 * 86_400_000 + 9 * 3_600_000 + 600_000
        for i, msg in enumerate(chat["messages"]):
            conn.execute(
                "INSERT INTO chat_messages (id,discoveryId,role,content,createdAt) VALUES (?,?,?,?,?)",
                (str(uuid.uuid4()), parent, msg["role"], msg["text"], base + i * 90_000),
            )

    for s in content["tripSummaries"]:
        stamp = day_zero_ms - s["day"] * 86_400_000
        date = time.strftime("%Y-%m-%d", time.localtime(stamp / 1000))
        conn.execute(
            """INSERT INTO trip_summaries (date,headline,narrative,highlightsJson,
               tomorrowIdeasJson,generatedAt) VALUES (?,?,?,?,?,?)""",
            (date, s["headline"], s["narrative"],
             json.dumps(s["highlights"], ensure_ascii=False),
             json.dumps(s["tomorrowIdeas"], ensure_ascii=False),
             stamp + 20 * 3_600_000),
        )
    return len(ids), len(chat["messages"]) if parent else 0, len(content["tripSummaries"])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--platform", choices=("android", "ios"), default="android")
    ap.add_argument("--device", help="adb serial, or simulator UDID (default: the only one)")
    args = ap.parse_args()

    os.makedirs(WORK, exist_ok=True)
    content = json.load(open(CONTENT, encoding="utf-8"))
    device = Android(args.device) if args.platform == "android" else Ios(args.device or "booted")

    print("stopping app…")
    device.stop_app()
    time.sleep(1)

    print(f"fetching {len(content['discoveries'])} photographs from Wikimedia Commons…")
    day_zero_ms = int(time.time() * 1000)
    photos, pairs = {}, []
    for item in content["discoveries"]:
        name = f"capture_{day_zero_ms - item['day'] * 86_400_000 + item['hour'] * 3_600_000}.jpg"
        local = os.path.join(WORK, f"{item['id']}.jpg")
        if fetch_photo(item["commons"], local):
            photos[item["id"]] = name
            pairs.append((local, name))
    print(f"  {len(pairs)}/{len(content['discoveries'])} photographs ready")

    print("pulling database…")
    device.pull_db()
    if not os.path.exists(DB_LOCAL) or not os.path.getsize(DB_LOCAL):
        sys.exit("database is empty — open the app once first so Room creates it")

    conn = sqlite3.connect(DB_LOCAL)
    conn.execute("PRAGMA journal_mode=DELETE")  # fold the WAL back in
    cols = [r[1] for r in conn.execute("PRAGMA table_info(discoveries)")]
    if "imageName" not in cols:
        sys.exit(f"unexpected schema — discoveries has {cols}")

    n_disc, n_chat, n_sum = write_rows(conn, content, photos, day_zero_ms)
    conn.commit()
    conn.execute("VACUUM")
    conn.close()
    print(f"inserted {n_disc} discoveries (provinceId NULL — backfill resolves them), "
          f"{n_chat} chat turns, {n_sum} day summaries")

    print("pushing photographs…")
    on_device = device.push_photos(pairs)
    print(f"  {on_device} files in the captures directory")

    device.push_db()
    print("database pushed — open the app")


if __name__ == "__main__":
    main()
