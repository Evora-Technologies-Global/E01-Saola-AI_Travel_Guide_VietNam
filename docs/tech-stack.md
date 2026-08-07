# Saola — tech stack and technical detail

> What the app is built out of, why each choice was made, and what it costs to change it.
> Kept in English, the same register as [`LLM.md`](../LLM.md), because it quotes code
> identifiers verbatim. [`README.md`](../README.md) (Vietnamese) and
> [`README-en.md`](../README-en.md) are the documents for a reader who has not opened the
> project yet; this one assumes they have.

| Question | Read |
|---|---|
| Where does a file go? What may depend on what? | [`../LLM.md`](../LLM.md) |
| How do I write an MVI screen? | [`android-mvi-best-practices.md`](android-mvi-best-practices.md) |
| How does the app arrange itself on a large window? | [`large-screen-layout.md`](large-screen-layout.md) |
| What is in one module, and what are its rules? | [`../domain/README.md`](../domain/README.md) · [`../data/README.md`](../data/README.md) · [`../shared/README.md`](../shared/README.md) |
| What is it built out of, and why? | this file |

---

## 1. Architecture

Four Gradle modules, dependencies pointing inwards:

```
:app        Android host. Thin: Application class, one Activity, theme, manifest.
              ↓
:shared     THE WHOLE PRESENTATION LAYER — every screen, every ViewModel, the
            navigation graph, the design system. Compose Multiplatform.
            Two arrangement layers over one ViewModel layer: mobile/ and tablet/.
            Produces SaolaShared.framework (static) for iOS.
              ↓
:domain     Models, use cases, repository *interfaces*, AppResult/AppError.
            Pure Kotlin. No Compose, no Android, no Ktor, no Room.
              ↑
:data       Repository *implementations*, Room, DataStore, Ktor, platform storage.

iosApp      Xcode project. Links the framework, calls MainViewController.
```

`:shared` declares `api(projects.domain)` (re-exported to Swift) and
`implementation(projects.data)`. `:domain` depends on nothing in this project.

**Two arrangements, one brain.** Ten features, ten Contracts, ten ViewModels — and eighteen
arrangement files: ten under `mobile/feature/`, eight under `tablet/feature/`. A defect in
what a screen *knows* is fixed once for both form factors; only a defect in where something
*sits* is fixed twice. No business logic and no `XViewModel` may live under either branch.

Every screen ViewModel extends `MviViewModel<S, I, E>`; `onIntent` is its only public
method; State/Intent/Effect live in `XContract.kt`; every coroutine goes through
`launchSafely`. The full rule set is in
[`android-mvi-best-practices.md`](android-mvi-best-practices.md), the file map in
[`LLM.md`](../LLM.md).

---

## 2. Tech stack, with versions

| Layer | Choice | Version |
|---|---|---|
| Language | Kotlin Multiplatform | 2.3.21 |
| Build | AGP · KSP · Gradle JDK | 9.2.1 · 2.3.10 · 17+ |
| UI | Compose Multiplatform | 1.12.0-beta03 |
| UI (Android artifacts) | Compose UI · Material Icons · Activity Compose | 1.12.0-beta02 · 1.7.8 · 1.13.0 |
| Navigation | Navigation Compose (JetBrains) | 2.9.2 |
| Lifecycle / ViewModel | Lifecycle (JetBrains) | 2.11.0 |
| DI | Koin (BOM) | 4.2.2 |
| Database | Room (multiplatform) + SQLite bundled | 2.8.4 · 2.7.0 |
| Preferences | DataStore Preferences Core | 1.2.1 |
| Networking | Ktor Client (OkHttp / Darwin) | 3.5.1 |
| Serialization | kotlinx.serialization JSON | 1.11.0 |
| Concurrency | kotlinx.coroutines | 1.11.0 |
| Date / time | kotlinx-datetime · kotlinx-io | 0.8.0 · 0.9.0 |
| Images | Coil 3 (+ Ktor 3 fetcher) | 3.4.0 |
| Camera (Android) | CameraX · ExifInterface | 1.6.1 · 1.4.2 |
| Camera (iOS) | AVFoundation | platform |
| OCR (Android) | ML Kit text recognition — Latin, Chinese, Japanese, Korean | 16.0.1 |
| OCR (iOS) | Vision `VNRecognizeTextRequest` | platform |
| Maps (Android) | Maps Compose (Maps SDK for Android) | 8.3.1 |
| Maps (iOS) | MapKit | platform |
| Location | Play Services Location · CoreLocation | 21.4.0 · platform |
| Narration | Android `TextToSpeech` · iOS `AVSpeechSynthesizer` | platform |
| Logging | Kermit (logcat / os_log) | 2.1.0 |
| IO | Okio | 3.16.0 |
| AI | Google Gemini 3 via the Google AI Studio REST API | see below |
| Testing | `kotlin.test` · JUnit4 · Turbine · Ktor MockEngine · `koin-test` | 4.13.2 · 1.2.1 |

**Platform floor:** Android `minSdk` 26, `targetSdk` 36, `compileSdk` 37. iOS deployment
target 16.0. App version 1.0.0 (`versionCode` 100), set once in the root `gradle.properties`
because `:app` stamps the APK and `:shared` compiles it into the Settings footer.

**Model chain** (`AppSettings.kt`): `gemini-3.5-flash` → `gemini-3.1-flash-lite` →
`gemini-3-pro-preview`. Only 3.x ids are offered — Google retires older generations for keys
created after a cutoff, so a `gemini-2.x` id would `404` for any new user.

**There is no mocking library.** `libs.versions.toml` still declares `mockk` and
`mockk-android`, but no module depends on either and nothing imports `io.mockk` — doubles are
hand-written fakes (`commonTest/testing/Fakes.kt`) plus Ktor's `MockEngine`, because MockK is
JVM-only and `commonTest` compiles for Kotlin/Native. Those two catalog entries are dead and
should be deleted rather than reached for.

**Espresso and the test runner are pinned up on purpose** (3.7.0 / 1.7.0). Neither is a
dependency this project asks for; both arrive under `ui-test-junit4`, which still declares
2022 artifacts. Espresso 3.5.0 calls `InputManager.getInstance()` by reflection inside
`Espresso.onIdle`, and the platform removed that method, so on API 37 every instrumented
test died with `NoSuchMethodException` before its body ran.

---

## 3. Where the data lives

**Room is the single source of truth** for everything the traveller has seen. Six tables:

| Table | Holds | Notes |
|---|---|---|
| `discoveries` | one row per recognised photo | indices on `createdAt`, `isFavorite`, `provinceId` |
| `chat_messages` | one row per conversation turn | cascade-deletes with its discovery |
| `discovery_notes` | the traveller's own note + kept photos | keyed by `discoveryId`, at most one per discovery |
| `discovery_reports` | "this recognition is wrong" | keyed by `discoveryId`, cascade-deletes with it; a re-filed objection overwrites, so `createdAt` is the latest one |
| `translations` | OCR + translation results | blocks stored as JSON with their bounding boxes |
| `trip_summaries` | one AI-written diary entry per day | keyed by ISO date, so regeneration overwrites |

List-shaped fields (sections, fun facts, tags, nearby, suggested questions) are stored as
JSON strings through `Converters`. Normalising them would buy nothing: they are only ever
read and written with their parent row and never queried across rows. Decoding is lenient —
a schema tweak degrades an old row to an empty list rather than crashing the journal.

**Photographs are files, not blobs.** Captures are written as JPEG, EXIF-corrected and
downscaled to a 1024 px long edge (`ImagePolicy`), into `files/captures` on Android and
`Library/Application Support/captures` on iOS.

**The database stores a file *name*, never a path.** `imageName` and `photoNamesJson` are
bare names, resolved late through `CaptureStore.resolve`. This is not tidiness: on iOS the
app's container is re-homed under a fresh UUID on every reinstall, update or restore, so a
path stored yesterday names a directory that no longer exists. It cost this app every
photograph once — not by failing to load them, but by deleting them: `listCaptures`
reported the new directory, the database still held the old one, and the orphan sweep found
no overlap and swept the lot.

**Settings live in DataStore Preferences** (`settings.preferences_pb`), not Room: theme,
narration switch, location opt-in — three keys. The API key and the model tier were two more
until 06.08.2026, when both became build decisions and their keys were dropped rather than left
readable: a stored value that nothing writes can only override a build decision with whatever an
older install happened to be left on.

**Nothing leaves the device except the image being recognised.** There is no account, no
backend and no analytics. The only outbound calls are Gemini (the photo and the prompt),
Overpass/Wikipedia/Commons (a coordinate), and the map tiles.

**Schema version 1, with `fallbackToDestructiveMigration(dropAllTables = true)` and no
migrations.** Acceptable only because nothing is published: any version Room has no
migration for — in either direction — drops every table and recreates them empty, silently.
A device carrying demo data has to be re-seeded.

**The number stays at 1 while the app is unpublished, and that is the rule rather than an
accident.** It had reached 3 — 2 for the `imagePath` → `imageName` rename, 3 for
`discovery_reports` — and every one of those bumps was destructive anyway: each existed only
to *trigger* the fallback, never to carry data across. Counting up while destroying everything
at each step describes a migration history that does not exist. So: change the schema freely,
leave the version alone.

The one thing that rule cannot do, worth knowing before trusting it: Room hashes the schema
into the file and compares it on open, so a **same-version** change is an integrity failure it
throws on rather than a fallback it runs. A developer holding an older `saola.db` uninstalls or
clears app data; on a debug build the demo trip re-seeds on the next launch. `data/schemas/`
holds the single exported `1.json` and is **git-ignored**, so it is a local build artefact, not
a record a reviewer can read in a diff.

**Publishing ends this.** The rows stop being disposable, the number starts moving, the first
real `Migration` is written, and `fallbackToDestructiveMigration` comes out of
`applySharedConfiguration`.

**Bundled assets**, read-only, shipped in the APK/app bundle:

| Asset | Size | What |
|---|---|---|
| `provinces.json` | 189 KB | the 34 provinces as polygons, generated from OSM (ODbL) |
| `catalog.json` | 95 KB | the 61 culture-collection entries in eight languages |

---

## 4. How a capture becomes a story

```
CameraX / AVFoundation capture
   → CaptureStore (EXIF-corrected, downscaled to 1024 px, JPEG)
   → GeminiClient (schema-constrained request, model fallback chain)
   → DiscoveryPayload → DiscoveryEntity → Room
   → Flow<Discovery> → MVI ViewModel → Compose
```

Two decisions carry most of the weight:

**Structured output.** Every AI call is bound server-side to a JSON Schema
(`GeminiSchemas.kt`), so parsing is a plain deserialisation — no regex hunting for JSON
inside prose, and a declared field is guaranteed present.

**Model fallback.** Gemini Flash routinely answers `503 high demand` at peak times. Failing
a capture because of that would be awful when someone is standing in front of a temple, so
a request walks down the chain of models and only gives up when every one is busy.
Non-retryable failures (bad key, malformed request) stop the chain immediately rather than
burning quota.

---

## 5. Travel passport

Vietnam drawn as its 34 provinces, each filling in with the traveller's own photograph,
clipped to that province's real outline. It adds **no dependency** — no Maps SDK, no API
key, no billing, no network — and about 200 KB to the release APK.

**There is no check-in button.** Every recognised capture already carries the coordinates it
was taken at, so pointing the camera at a temple *is* the check-in.
`DiscoveryRepositoryImpl` resolves the province at write time and stores it in a
denormalised `provinceId` column, which turns the map's per-province roll-up into one
grouped SQL query instead of point-in-polygon over 11,041 vertices per row on every read —
`Province.contains` tests `mainlandRings` and then `offshoreRings`, so a fix that matches
nothing walks the whole corpus behind its bounding-box early-out.

### The data

`data/src/*/assets/provinces.json` (189 KB) is generated by
`tools/provinces/build_provinces.py` from OpenStreetMap boundary relations (ODbL), clipped
against a Natural Earth coastline (public domain). Two things in that script are not obvious
and both are load-bearing:

**Simplification happens per OSM *way*, not per province.** Neighbouring provinces share the
very same way objects along their common border. Simplifying each way once and reusing the
result means both sides land on an identical line; simplifying each province's ring
independently would drift the two copies apart and open hairline gaps along every internal
border. 74% of ways are shared, so this is most of the country.

**Polygons are clipped to the coastline.** OSM tags coastal ADM1 boundaries out to
territorial waters — unclipped, the Gulf of Tonkin sits *inside* Hưng Yên and open water off
Vũng Tàu sits *inside* TP.HCM, and the country renders as a slab rather than the S-shape the
whole feature is about.

> Do not regenerate this from GADM or Natural Earth admin levels. Both still carry the
> **pre-2025 63 provinces**; the file must contain exactly 34.

### Resolving a coordinate

A strict polygon test alone would fail precisely where people take photographs: because the
outlines are clipped to the coast, a Hạ Long Bay cruise, a beach at Mỹ Khê, or a fix that
drifted offshore all land in open water and match nothing. So `locateProvince` falls back to
the nearest province within 25 km. Measured against real tourist coordinates, the furthest
one needed 3.4 km.

`ProvinceGeometryTest` runs against the shipped asset and asserts ~63 real coordinates,
including every pre-2025 provincial capital resolving to the province that absorbed it — so
a regenerated asset built from a pre-merger source fails the build instead of shipping.

### Rendering

A plain Compose `Canvas`. Paths are built once in unmagnified canvas space and pan and zoom
are applied as a transform, so a gesture never rebuilds the mainland's **9,151** vertices —
`Province.toShape` walks `mainlandRings` only, and the insets build their 1,813 separately.
Photos are filled
with `clipPath(ring) { drawImage(…) }`, **fitted per ring rather than per province**: Khánh
Hòa's bounding box reaches Trường Sa and is almost entirely open sea, so a single copy
stretched across it left the mainland showing a narrow strip of the photo while the islets
showed an unrelated band from its opposite edge.

At country zoom a province is 40–80 px wide, so a photo inside one reads as a patch of
colour rather than a picture. That is the intended mosaic effect; pinch-zoom and the detail
sheet are what make an individual photo legible.

### Hoàng Sa and Trường Sa

Both archipelagos are drawn in labelled inset boxes down the right-hand side, the way every
Vietnamese map draws them. Placed where they actually are, they push the country's extent
3.4° further east and flatten the mainland's aspect from 0.475 to 0.818 — pinning the part a
traveller can actually collect into little over half the screen to make room for open sea.
The insets do not pan or zoom with the mainland; they are a separate reference frame, which
is also the convention. Tapping either box selects the province that administers it.

Getting them into the data took two different routes, and neither is obvious:

- **Trường Sa** arrives inside OSM's Khánh Hòa boundary relation — but only as three
  administrative sea areas up to 1.3° across. Drawn as islands those are three enormous
  meaningless circles, so the map filters them out by size. They stay in the data because
  they are what makes a photo taken anywhere in the archipelago resolve to Khánh Hòa,
  including on islands OSM has not mapped.
- **Hoàng Sa** is in no boundary relation at all. OSM keeps the disputed archipelago out of
  every national admin hierarchy, so the first cut of this map drew Trường Sa and silently
  omitted Hoàng Sa. Vietnam administers it as huyện đảo Hoàng Sa, a district of Đà Nẵng.

Both are therefore drawn from OSM's own island/islet/reef features — real surveyed geometry,
nothing hand-drawn — curated into `tools/provinces/archipelagos.json` in one clearly
labelled step, because attaching Hoàng Sa to Đà Nẵng is a claim the boundary data does not
itself make. `ProvinceGeometryTest` asserts both archipelagos are present and resolve to
their province, so regenerating the asset cannot quietly drop one.

### Known gaps

- **Small provinces are hard to tap.** Hit testing is strict point-in-polygon with no
  tolerance, while several provinces are ~22 dp wide at country zoom. A screen reader is not
  affected — `ProvinceSemanticsOverlay` gives each province a node the size of its bounding
  box, so a double-tap reaches ones a fingertip cannot.
- **`ProvinceRepositoryImpl` has no test.** `ProvinceGeometryTest` covers the geometry below
  it and `PassportViewModelTest` the state above it; the repository between them is unproven.

Two entries left this list on 06.08.2026, and both were about what a stranger to the code
sees rather than about the geometry. **Accessibility**: the `Canvas` was a single unlabelled
node and TalkBack skipped the feature entirely, so `ProvinceSemanticsOverlay` now lays a
labelled, actionable node over each province — 34 of them plus both archipelagos, counted by
`uiautomator dump` on a Galaxy A16 against 0 before. **Attribution**: ODbL §4.3 asks for the
credit on the produced work rather than in a repository, so it is now a line on the map
itself, with a `Settings → About → Licences` screen behind it.

---

## 6. Culture collection

A board of 61 things worth finding in Vietnam — dishes, roof details, a basket boat, a stele
on a stone tortoise — grouped by the categories the recogniser already uses. A tile is a
hatched square until the traveller photographs the thing, and then it fills with **their own
picture**.

It replaced a list of AI-generated "where to next" suggestions, which had three problems no
amount of prompt work would fix. It was empty until you had already captured something, so
the first launch showed a dead end. It had no Places API behind it, so every address, price
and distance was invented. And it could not be demonstrated from a desk, because the
suggestions were always about wherever you were sitting.

### It lives beside the passport, not in a tab

The collection first shipped *as* the Explore tab, and that was wrong. It and the passport
are the same move along different axes — slots that fill with your own photographs, a
progress count, tap for detail — one counting *where* you have been and the other *what* you
have found. Two tabs' worth of apparatus for one idea made them read as rival features
rather than as two views of one trip.

So the board is pushed from the journal, directly under the passport row, and the two are
drawn as siblings: one `ProgressRow` for both journal CTAs, one `FillGauge` for both
progress bars, and the same header form.

`FillGauge` is two boxes rather than a `LinearProgressIndicator`, and that is worth knowing
before anyone "simplifies" it: since M3 1.3 the indicator draws a gap and a stop dot at the
leading edge. Right for a determinate task, wrong here — nothing is in flight, the bar is a
fill level, and the stop dot reads as a target being missed.

**There is no progress table.** The board is `catalog.json × the journal`, computed on read.
Storing "unlocked" rows would be a second source of truth that could outlive a deleted
photograph, and deriving it means the collection is *retroactive* — install this build over
an older one and the tiles are already part-filled from photos taken weeks ago.

### Matching, and why diacritics are kept

A discovery is matched to a catalogue entry by comparing its title, local name and tags
against that entry's `aliases`, as whole words, **without folding Vietnamese tone marks**.
Folding is the obvious implementation and it is quietly wrong: `phở` and `phố` become the
same string, so a photograph of Phố cổ Hội An would collect a bowl of noodles. `đình`
collapses onto `định`, `cửa` onto `của`. Gemini writes Vietnamese with its diacritics and the
catalogue is written the same way, so keeping them costs nothing and removes a whole class of
wrong unlocks. `CatalogMatcherTest` pins that case, along with the whole-word rule that stops
`chè` matching `chèo`.

The summary and section bodies are deliberately **not** searched. They are paragraphs that
mention neighbouring dishes and regional variants, and matching against them would collect
half the board from one photograph.

### Eight languages, one source

Interface and narration both follow the **device** language: a Japanese phone gets Japanese
screens, Japanese answers and a Japanese voice. There is no in-app language setting — there
used to be one, defaulting to Vietnamese, which is how an English phone ended up reading
English labels while the guide talked to it in Vietnamese.

The eight are Vietnamese, English, Japanese, Korean, Chinese, French, Spanish and Thai — the
same eight `TranslateLanguage` offers, with the same codes and BCP-47 tags, asserted in
`GeminiModelTest`. Anything else falls to English, because that is the string table Compose
Resources resolves it to.

Three places answer "which language", each for code that cannot reach the others:

| Where | What it reads | Used by |
|---|---|---|
| `uiLanguage()` in `Formatters.kt` | the `resource_language` marker in the resolved `strings.xml` | anything drawn beside a `stringResource` |
| `deviceLanguage()` in `:data/platform/` | the platform locale API | repositories, prompts, the TTS voice |
| `CatalogAssetSource` | `deviceLanguage()`, once, while parsing | the culture collection's names and hints |

The catalogue resolves at parse time rather than at draw time so `CatalogItem` holds one name
and one hint instead of a `Map` of eight — a Map on that model would be unstable to Compose
and cost the collection board its recomposition skips.

Written dates are hand-assembled in `domain/model/DateNames.kt`: `DateTimeFormatter` is
JVM-only and `NSDateFormatter` Apple-only, and the journal heading has to name the same month
as the prompt that writes the day's story. Thai years are Common Era there, not Buddhist Era,
so they do not disagree with every other date the app shows.

### Known gaps

- **No new-unlock moment.** A tile flips to collected silently the next time the board is
  opened. The reveal is the most rewarding part of a collection and it happens off-screen.
- **The board is still a record, not a guide.** It shows what you have; it never says what to
  go and get. The recognition hints are the material for that — sixty-one sentences teaching
  how to look — but they sit behind a tap on a grey tile.
- **The catalogue is Kinh-majority.** Highland and Cham material is present but thin, and the
  Mekong delta is under-represented against the north.
- **`aliases` is a hand-maintained list.** A dish Gemini names in a way nobody anticipated
  stays locked with no signal that anything went wrong.
- **The favourites and translation-history reads are held open by tests alone.**
  `DiscoveryRepository.observeFavorites` and `TranslationRepository.observeTranslations` /
  `observeTranslation` / `delete` lost their last screen when the recommendation clean-out
  removed the use cases in front of them.

---

## 7. Explore

A live map of wherever the traveller is standing, with the places worth walking to within
**5 km** already pinned on it. Tap a marker and a sheet comes up with the photograph, the
encyclopaedia entry, how many people read about it, the distance, the opening hours and
whatever else was mapped. **Bắt đầu** hands the place to Google Maps with the destination
already filled in.

### Built on open data, and why that turned out to matter

The first version of this screen ran on the **Google Places API**, and it worked — real
ratings, real reviews, real photographs. It is gone because Places API (New) could not be
enabled on the project this app is built under, and a feature that a billing setting can
switch off is not a feature. Everything here now comes from sources that need **no API key
and no billing account**:

| Source | Question it answers |
|---|---|
| OpenStreetMap (Overpass) | what *is* a place, and where |
| Wikipedia | what is this place, and how many people read about it |
| Wikimedia Commons | what does it look like |

The one Google dependency left is the Maps SDK that draws the Android map, and iOS does not
even need that — it uses MapKit.

**There is no star rating, and there is not meant to be.** No open source carries Google's
ratings, so rather than invent a number the screen ranks by two things anybody can check:
Wikipedia readership over the last 60 days, and how carefully somebody mapped the place. That
is a real loss against the Places version and an honest one — inventing a rating would have
reintroduced the exact fault this feature was built to avoid.

### Each source is asked exactly one question

**Wikipedia does not choose the places.** An early version let it contribute places as well
as describe them, and the top of the list came back as "Hà Nội" and "Liên bang Đông Dương" —
geotagged articles that are not destinations. A survey of 50 articles within 5 km of Hoàn
Kiếm found **54% were not places anyone visits**. OSM has real POI classification, so OSM
alone decides what appears.

**Wikipedia is only consulted where OSM points at it.** Looking articles up by name seemed
reasonable and is catastrophic: a zoo enclosure named "Báo lửa" matches the article about the
*leopard species*, a bell named "Chuông" matches the article about bells, and "Thánh Paul"
matches the apostle. Measured hit rate for name lookup was 32%, almost all of it wrong. The
`wikipedia` tag on an OSM element is an assertion by whoever mapped it, so it cannot produce
a false match.

### The Overpass query is the whole feature

Three things about it are not obvious and all three were bugs first.

**Places of worship must be asked for explicitly.** Vietnamese pagodas, đình, temples and
cathedrals overwhelmingly carry no `tourism` or `historic` tag at all. Verified individually:
Đền Ngọc Sơn is `place_of_worship` + `building=temple`, Nhà thờ Lớn is `place_of_worship`
alone, Văn Miếu is `landuse=religious` + `religion=confucian`. A standard tourism filter finds
**none of them**, while returning 48 traffic-island flower beds — there are 222 named places
of worship inside 5 km of Hoàn Kiếm that it never sees.

**Tag filters must precede the spatial filter.** Written `nw(around:…)["name"]["amenity"=…]`
the query times out; written `nw["amenity"=…]["name"](around:…)` it returns in seconds. And a
timed-out Overpass query answers **200 OK with an empty element list and a `remark`** — so
before that field was read, the screen confidently told a traveller standing in central Hanoi
that there was nothing around them.

**One query is too big.** The union spans ~250 sights and ~1,700 places to eat in central
Hanoi and does not finish. It is split into a sights query and a food query, run concurrently
against *different* mirrors.

`ENDPOINTS` lists only instances with worldwide coverage, and that qualifier is load-bearing
too: several widely-listed mirrors are regional extracts that do not say so.
`overpass.osm.ch` answers a Hanoi query with a cheerful `200 OK` and zero elements because it
holds Switzerland — it briefly shipped here as a fallback that silently emptied the map.

### Ranking, and the curation layer under it

`NearbyPlace.prominence` is `log10(readers)` + mapping detail + bonuses for having a
description and a photograph + a bias towards sights, minus a light distance penalty that
only breaks ties. Restaurants have no readership at all, so mapping detail is their only
signal — a place whose mapper filled in opening hours, a website and a phone number is far
more likely to be real than a bare node.

Sights are also *capped against* food (`rankWithBalance`). Without it the screen tips over
completely in a residential district: measured in Nam Từ Liêm, an unfiltered ranking returned
forty takeaway grills and not one landmark, because OSM's food coverage is dense and
consistently tagged everywhere while its attraction coverage is thin outside historic centres.

`isNotADestination` is a blocklist and is honest about being one. Roughly **half** of what OSM
files under "attraction" near Hoàn Kiếm is not somewhere anyone would go: 25 individual animal
pens inside Thủ Lệ zoo tagged `tourism=attraction` (the screen was recommending a golden
monkey), 48 planted roundabouts, 15 identically-named ward war memorials, and entries
genuinely present in the data called `0 km`, `Abandoned Van`, `Chuông` (a bell) and `Lư` (an
incense burner). Chains are deduplicated by name — OSM holds 35 branches of Highlands Coffee
within 5 km, and the traveller wants the nearest.

### The map is the only `expect`

`PlaceMap` is the one platform-specific thing in the feature — the Maps SDK on Android,
MapKit on iOS. Everything above it is written once: what gets searched for, how it is ranked,
what a marker means, what the sheet says. The *places* are identical on both platforms — they
arrive over plain HTTP from OpenStreetMap and Wikipedia, which do not care what draws them.

Markers carry the app's existing `CategoryColors` rather than a map palette of their own — a
temple is the same lacquer red here that it is in the journal and on the culture board.

Three details worth knowing before touching either actual:

- **`MapCamera.requestId`** is what makes it a request rather than a value. Without it, a
  second tap on a marker the traveller has since panned away from does nothing — the target
  is unchanged, so nothing recomposes.
- **The iOS interop view keeps `Cooperative` touch handling**, not `NonCooperative`.
  Cooperative costs the first ~150 ms of a pan, which MapKit replays; NonCooperative would
  hand MapKit every touch landing on it first, and the sheet's scrim lies directly on top of
  that view — losing tap-to-dismiss is the worse trade.
- **No companion objects inside `PlaceMapController`.** Kotlin/Native rejects "Fields are not
  supported for Companion of subclass of ObjC type", so its constants live at file scope.

### The cache is in memory, and that is deliberate

Every other repository here is Room-backed because the traveller's own history has to survive
a cold launch. This one holds somebody else's data about wherever the phone happened to be,
and restoring it from disk would open the screen on last week's city. A Room table would also
have meant a schema migration — and `fallbackToDestructiveMigration` would have dropped the
whole journal to cache a list of cafés.

Fifteen minutes and 300 metres is the window: walking the length of a street reuses the
search, crossing town re-runs it. The details cache is keyed on the place **and the
language** — keyed on the id alone it served the Vietnamese article back after a switch to
English, which is the one thing the switch was supposed to change.

### Which name a place is shown under

OSM holds translations in `name:<code>` tags, and the search reads the traveller's own code
first, then `name:en`, then keeps the local `name`. On an English phone the list opens on
"Vietnam Military History Museum" and "Hanoi Museum" where it used to open on "Bảo tàng Lịch
sử Quân sự Việt Nam" and "Bảo tàng Hà Nội" — measured on a Galaxy A16 by pinning the app to
`en-US` and back to `vi-VN` against the same 40 results.

Three things about it are not obvious:

- **English is the fallback for six of the eight languages, not a preference.** Around Hoàn
  Kiếm, 47% of attractions carry `name:en` and effectively none carries `name:ja`, `name:ko`
  or `name:th`. A Japanese traveller given "Hoa Lo Prison" can at least read it aloud.
- **Vietnamese is excluded from that fallback.** The plain `name` tag in Vietnam *is* the
  Vietnamese name, so falling through would take a Vietnamese traveller from "Nhà tù Hỏa Lò"
  to "Hoa Lo Prison" — the one language where the fallback is a downgrade. The premise is not
  universal: the same Mỹ Đình search returned two sculptures whose `name` is Korean, mapped
  by whoever lives around them. Neither carries a `name:en`, so nothing changes for them
  either way — but a Vietnamese traveller looking at a place named in a third language will
  not be rescued into English by this rule, and that is accepted rather than unnoticed.
- **Two comparisons must keep reading the local name, and both are easy to get wrong.** The
  junk filter is written against what Vietnamese mappers type ("Vườn hoa …", "Lư", "0 km"),
  so a roundabout given a `name:en` would walk past a filter reading the translated name.
  And deduplication is by name, while the *displayed* name now differs per language — keyed
  on that, two branches of one café chain stop collapsing the moment somebody translates one
  of them. `NearbyPlace.mappedName` is the key for anything comparing places rather than
  showing them, and `PlaceNamingTest` drives the whole search to hold both rules.

The local name is not thrown away: `NearbyPlace.localName` carries it whenever it differs,
and the place sheet prints it under the title — the same shape `DiscoveryTitleBlock` gives a
recognition, and for the same reason. Somebody reading "Hoa Lo Prison" is standing in front
of a sign that says something else.

### Known gaps

- **Most places have no photograph.** OSM holds no images, and only the well-known few carry a
  Wikipedia article or have been photographed onto Commons near enough to match. Measured in a
  residential district: none of forty. Those fall back to a category glyph in the place's own
  colour, which is why that had to look deliberate rather than broken.
- **Overpass is donated infrastructure and it throttles.** A burst of development traffic
  exhausts the per-IP allowance and every request until it recovers is refused; the mirror
  rotation exists for exactly this. This is visible in `screenshots/ios-ipad/05-explore.webp`,
  taken after a day of repeated searches: the food query returned and the sights query did
  not, so the screen shows twelve restaurants and no landmarks.
- **Neither map actual is tested.** `OpenMapClientTest` covers the wire formats, the query
  shape and the junk filter, but `PlaceMap.android.kt` and `PlaceMap.ios.kt` have never been
  exercised by a test.
- **Markers are not clustered.** Forty pins at neighbourhood zoom in a dense quarter still
  overlap, and the two platforms fail differently: the Maps SDK stacks them, so the loser is
  covered and cannot be tapped, while MapKit *hides* the loser by display priority, so the
  place leaves the map with nothing to show it was ever there. The ranked list beside the map
  is the mitigation — it carries all forty however few pins are legible.
- **No offline state.** The tiles, the photographs and the search all need a network.

Two entries left this list on 06.08.2026. **Accessibility**: both map actuals now carry a
`contentDescription` naming what they are, each marker carries its place's name as the title
the platform speaks, and the ranked strip beside the map — which always holds every result —
is the path that needs no map at all. **Names**: the search reads `name:<the traveller's
language>` and falls back to `name:en`, so roughly half the attractions around Hoàn Kiếm now
arrive already translated — see *Which name a place is shown under* above.

---

## 8. Voice — what is and is not built

Narration is real: guide replies are read aloud in the device language, and translations in
whichever of the eight target languages was picked. The Settings switch auto-speaks chat
replies only — the Listen buttons on a translation and on a discovery are manual taps it does
not reach.

**Dictation is gone.** It was written against Android's `SpeechRecognizer` and iOS's Speech
framework and finished on both, but no button was ever added to reach it, so nothing could
call it. It was deleted on 02.08.2026 along with the microphone and speech-recognition
permissions it had the app *declare* — declared but never requested, since the code that
would have asked was itself unreachable.

**AI recommendations are gone too.** "Ask the model what to see next from your location and
trip history" shipped, and every address, price and distance in it turned out to be invented,
so it was removed rather than patched. What answers the same question now is Explore, which
asks OpenStreetMap and Wikipedia — two sources that can be checked — and the culture
collection, which tracks what you have found. The code was deleted on 02.08.2026.

---

## 9. Getting started

### Requirements

- Android Studio (AGP 9.2+), JDK 17+
- Android SDK Platform 37 installed
- A device or emulator on **API 26+**; Xcode 16+ for iOS
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey)

### 1. Add your Gemini key

Put it in `local.properties`, which is git-ignored so the key never reaches version control:

```properties
GEMINI_API_KEY=your_key_here
```

It is injected at build time, and that is the only way in: the runtime paste field under
Settings was removed on 06.08.2026 along with the model picker. A build without this property
starts, seeds and navigates normally — the lens screen says it has no key and recognition
returns `AppError.MissingApiKey`.

**Which model it calls is the same kind of decision**, and it is one line:
`GeminiModel.CONFIGURED` in `domain/src/commonMain/kotlin/…/model/AppSettings.kt`, currently
`FLASH_3_5`. The other entries stay reachable — `fallbackChain` walks them when the configured
one is overloaded.

### 2. Add a Maps SDK key (Android only, and only for the Explore map)

One key, and it draws the Android map — nothing else. The places on that map come from
OpenStreetMap and Wikipedia and need no key at all.

```properties
MAPS_API_KEY=your_maps_sdk_key
```

Enable **Maps SDK for Android** on it, and restrict it to this app: package
`com.evora.technologies.saola` (plus `.dev` for debug builds) and your signing SHA-1. It is
substituted into `com.google.android.geo.API_KEY` in the app manifest.

Without it the Explore tab still works — the markers, the sheet and the directions button are
all unaffected — but the map renders as blank tiles underneath them.

> **iOS needs no key whatsoever.** The map there is MapKit, which every iOS app links for
> free, and the place data is keyless on both platforms.

### 3. Build and run

```bash
./gradlew :app:installDebug                 # build + install (Android)
./gradlew test                              # unit tests (no network, no key needed)
./gradlew :app:connectedDebugAndroidTest    # end-to-end, calls the real API

open iosApp/iosApp.xcodeproj                # iOS: run the iosApp scheme
```

The instrumented suite drives the real pipeline — photo on disk → Gemini → Room — and *skips*
rather than fails when the API is unreachable or throttled, so an offline machine never
produces a misleading red build.

---

## 10. Demoing it, and the seeded data

Every capture taken in one room resolves to one province, so the passport map cannot be seen
filling in from a desk. A week of discoveries spread from Sa Pa to Cần Thơ, plus a
conversation and three AI day-summaries, is therefore written for you.

### The debug build seeds itself

`./gradlew :app:installDebug` and open the app. On first launch — and only when the journal is
genuinely empty — 24 discoveries, 6 chat turns and 3 day summaries are written, and the log
says so:

```
I Saola: Seeded 24 demo discoveries, 6 chat turns
```

**Release and fastRelease never seed, and cannot.** There are two independent gates:

| Gate | What it stops |
|---|---|
| `seedModule(isDebug)` binds a no-op `DemoDataSeeder` | the code from running |
| `:app`'s `stageSeedAssets` packages the demo into the **debug variant only** | the data from being there at all |

The second is what makes the guarantee cheap to trust: `unzip -l` on a release APK finds zero
`assets/seed/` entries, so even a mis-bound seeder would read null and write nothing. It also
keeps 3.5 MB of photographs out of the shipped APK.

**Emptiness is the whole condition**, with no "already seeded" flag beside it. That makes
**Settings → clear everything** the reset button on a debug build: wipe it, relaunch, and the
trip is back. The cost is that an empty journal cannot be held open on a debug build to look at
an empty state — do that on a release build.

### Where the content comes from

`tools/seed/demo-content.json` holds the text; `tools/seed/.work/*.jpg` holds the 24
photographs, **tracked in git** so the seed needs no network and the exact images in the
screenshots can be reviewed in a diff. `stageSeedAssets` copies both into `assets/seed/` for
the debug variant.

The photographs were fetched by their Wikimedia Commons file name through `Special:FilePath`
and re-encoded to the same 1024 px JPEG `ImagePolicy` produces. Naming the file rather than
following an article's lead image is what makes the set reproducible — for several of these the
lead image is a location map rather than a photograph.

Two details of the mapping are worth knowing, both pinned by `DemoContentMapperTest`:

- **Nothing is stamped in the future.** Entries carry an hour-of-day, and the app may be opened
  before that hour; today's are clamped back to now, a minute apart so their order survives.
- **`provinceId` is left NULL**, so the app's own backfill resolves every row against the
  shipped outlines on first open — the same geometry a real capture goes through.

### The script is still there, and is now the iOS route

`tools/seed_demo.py` does the same job from outside the app, and remains the only way to seed
an **iOS** simulator: the demo assets are packaged by `:app`'s debug variant, which iOS has no
equivalent of, so `IosBundledAssets.readBytes` finds nothing and an iOS debug build opens on a
real, empty journal. Adding the files to the Xcode target's Debug configuration is what would
turn it on there; the Kotlin side already handles it.

```bash
python3 tools/seed_demo.py                                    # a running Android device
python3 tools/seed_demo.py --platform ios --device <udid>     # a booted simulator
```

On Android the script needs a debug build, because `run-as` only works on a debuggable package;
on iOS it edits the simulator's container directly. Debug carries
`applicationIdSuffix = ".dev"`, so it installs *beside* the signed demo build rather than
replacing it — neither route touches the APK being demoed.

The screenshots in `screenshots/` were taken this way, on a Pixel 7 Pro emulator, a Pixel
Tablet emulator, an iPhone 17 simulator and an iPad Pro 11-inch simulator.

---

## 11. Release builds

**Demo on the release build, not the debug one.** Debug disables R8, ships every Compose
tooling class and spreads the app over 22 dex files; release is one dex with a baseline
profile. Cold start measured on a Galaxy A16 (Android 16); size and dex counts re-measured on
02.08.2026:

| | Debug | Release | |
|---|---|---|---|
| Cold start | ~2,580 ms | **~440 ms** | 5.9× faster |
| APK size | 75.4 MB | **49.9 MB** | −25.5 MB |
| dex files | 22 | **1** | |

Both figures are dominated by the four bundled ML Kit recognisers, which are ~45 MB of model
data that R8 cannot touch — the code R8 *can* touch shrinks by roughly 8×.

### One flag, two build types

`:shared` and `:data` are multiplatform modules built by
`com.android.kotlin.multiplatform.library`, which produces a **single** Android variant — the
debug APK and the release APK link the very same class files. So nothing inside them can ask
which build type it ended up in, and a generated `DEBUG` constant there is a trap: it used to
come from `-Psaola.debug`, default `true`, which meant a plain `:app:assembleRelease`
shipped with Ktor's `Logging` plugin installed.

The flag is passed in at startup instead, by the one caller per platform that knows the
answer:

```kotlin
// SaolaApplication.kt (Android)
modules(appModules(BuildConfig.DEBUG))

// iOSApp.swift -> MainViewController.kt
MainViewControllerKt.startSaola(debug: true)   // inside #if DEBUG
```

`appModules(isDebug)` threads it down to `networkModule(isDebug)`, the only consumer. In a
release build the `Logging` plugin is then never referenced, so R8 drops Ktor's whole logging
path out of the APK rather than leaving it dormant — verified in `mapping.txt`, which contains
no `io.ktor.client.plugins.logging` class at all.

The app version works the same way, for the same reason: `saola.versionName` and
`saola.versionCode` live in the root `gradle.properties`, because `:app` stamps the APK
with them and `:shared` compiles the version into the Settings footer. Written out separately
they had already drifted — the APK said 1.0 while the footer said 1.0.0.

### Signing

Signing material is read from the git-ignored `local.properties`; the keystore itself is
git-ignored too:

```properties
RELEASE_STORE_FILE=saola-release.jks
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=saola
RELEASE_KEY_PASSWORD=…
```

Create one with:

```bash
keytool -genkeypair -v -keystore app/saola-release.jks \
        -alias saola -keyalg RSA -keysize 2048 -validity 10000
```

If the keystore or its credentials are missing the build still succeeds and produces an
*unsigned* release APK, rather than failing with a confusing `validateSigningRelease` error on
a fresh clone.

```bash
./gradlew :app:assembleRelease   # -> app/build/outputs/apk/release/Saola_v1.0.0_build100_<date>.apk
./gradlew :app:installRelease    # build, sign and install
```

APKs are date-stamped so the file handed round during a demo says which build it is. Debug
carries a `-dev` version suffix, which shows up on the Settings screen.

### `fastRelease`: the release build without the minute of waiting

```bash
./gradlew :app:assembleFastRelease   # -> app/build/outputs/apk/fastRelease/…-fastRelease.apk
./gradlew :app:installFastRelease
```

`fastRelease` is `release` with R8 turned off, and it exists because of where the time goes.
Profiling `assembleRelease` on a warm tree: `minifyReleaseWithR8` takes 45–61s and every one
of the other 129 tasks put together takes under 12s. Shrinking is not part of that build, it
*is* that build. `assembleFastRelease` on the same tree finishes in ~12s.

Everything the running app can observe is unchanged: same `applicationId`, same release
keystore, `BuildConfig.DEBUG` still false — so Ktor's `Logging` plugin stays off — and it
installs straight over a release APK. Only the version name differs, `1.0.0-fast`.

What it cannot tell you is anything that only appears under obfuscation: a missing keep rule,
a serializer resolved by name, a class R8 renamed out from under reflection. **Whatever ships
to Play goes through `assembleRelease` and gets smoke-tested there.**

Lint is no longer part of either: `lintVital` ran on every non-debuggable variant and was
measured at 12.7s. Since `abortOnError = false` already meant lint could never fail a build,
nothing was given up by taking it off the path — run it deliberately with `./gradlew :app:lint`.

### A note on AGP 9 and minification

The project template generates `optimization { enable = false }` in the release build type,
and it is tempting to read that as the modern replacement for `isMinifyEnabled`. It is not.
`optimization.enable` is an experimental "gradual R8" feature that fails configuration outright
unless `android.r8.gradual.support=true` is also set. Code shrinking in AGP 9.2.1 is still
driven by:

```kotlin
isMinifyEnabled = true
isShrinkResources = true
proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
```

Two further AGP 9 specifics: `getDefaultProguardFile("proguard-android.txt")` is now a hard
error (it implies `-dontoptimize`), and `android.enableR8.fullMode` should not be set at all —
full mode is the default and the flag is scheduled for removal in AGP 10.

`app/proguard-rules.pro` is deliberately almost empty. Every dependency here ships its own
consumer rules, and a minified build was verified to produce no `missing_rules.txt` at all.
The only rules the app adds keep release stack traces readable (`SourceFile`,
`LineNumberTable`).

Two other files look like they hold keep rules, and one of them does not:

* `app/src/main/keepRules/rules.keep` — AGP 9 combines everything under `src/main/keepRules`
  and passes it to R8, so this **is** live. It holds only the template's comments.
* `data/consumer-rules.pro` — **inert**. A leftover from when `:data` was a
  `com.android.library`. The multiplatform-library plugin publishes consumer rules only when
  `optimization { consumerKeepRules { publish = true } }` asks it to, and nothing does.

Either way, confirm a rule landed by looking for its file as a section in
`app/build/outputs/mapping/release/configuration.txt` after a release build.

---

## 12. Attribution

- Province boundaries: © OpenStreetMap contributors, ODbL. Coastline: Natural Earth, public
  domain.
- Place data: © OpenStreetMap contributors (ODbL) via Overpass; article text and readership:
  Wikipedia (CC BY-SA); photographs: Wikimedia Commons under their individual licences.
- Demo screenshots use Wikimedia Commons photographs under their individual licences; see
  `tools/seed/demo-content.json` for the exact files.
- Maps: Google Maps SDK for Android; Apple MapKit on iOS.
- AI: Google Gemini via Google AI Studio.
