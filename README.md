# AI Travel Companion 🇻🇳

> An AI-powered travel assistant that transforms every trip into an interactive and personalized cultural journey.

## 📖 Overview

AI Travel Companion is a mobile application that uses **Google Gemini** to help travelers explore Vietnam through real-time visual understanding and natural conversations.

Instead of searching for information manually, users simply point their camera at a landmark, local dish, museum artifact, or historical building. The AI instantly recognizes the object, explains its history, shares interesting stories, translates nearby text, and answers follow-up questions through a conversational interface.

The goal is to provide every traveler with an intelligent local guide available anytime and anywhere.

---

# ❗ Problem

Travelers often struggle to understand the cultural and historical significance behind the places they visit.

Current solutions require users to:

- Search Google manually.
- Read long Wikipedia articles.
- Install multiple applications.
- Hire tour guides.
- Translate signs separately.

This creates a fragmented travel experience.

---

# 💡 Solution

AI Travel Companion combines:

- Computer Vision
- Google Gemini
- Voice narration (answers only — see below)
- Context Awareness

into one seamless experience.

Simply point your camera and ask.

The AI will:

- Identify landmarks
- Explain historical context
- Tell cultural stories
- Translate menus and signs
- Recommend nearby attractions
- Continue conversations naturally

---

# ✨ Features

## 🏛 Landmark Recognition

Recognize famous Vietnamese landmarks.

Examples:

- Temple of Literature
- One Pillar Pagoda
- Ho Chi Minh Mausoleum
- Notre-Dame Cathedral
- Hoi An Ancient Town

AI provides:

- History
- Architecture
- Cultural significance
- Fun facts

---

## 🍜 Local Food Guide

Point the camera at local dishes.

AI explains:

- Dish name
- Ingredients
- Origin
- Traditional way to eat
- Regional differences
- Estimated price
- Similar dishes

---

## 🏺 Museum Companion

Identify museum artifacts.

AI introduces:

- Historical background
- Time period
- Purpose
- Related events

Acts like an intelligent audio guide.

---

## 🏯 Architecture Explorer

Recognize Vietnamese architecture.

Explain:

- Architectural style
- Materials
- Historical influences
- Cultural meaning

---

## 🌍 Real-Time Translation

Translate:

- Restaurant menus
- Street signs
- Museum descriptions
- Public notices

Supports international visitors.

---

## 🎙 Voice Conversation

> **Half built.** The answers are spoken, the questions are typed.
>
> Narration reads guide replies aloud in the app's language, and translations in whichever
> of the eight target languages was picked. The Settings switch auto-speaks chat replies
> only — the Listen buttons on a translation and on a discovery are manual taps it does not
> reach.
>
> Dictation was written against Android's `SpeechRecognizer` and iOS's Speech framework and
> finished on both, but no button was ever added to reach it, so nothing could call it. It
> was deleted on 02.08.2026 along with the microphone and speech-recognition permissions it
> had the app *declare* — declared but never requested, since the code that would have asked
> was itself unreachable.

Users can ask naturally:

> Why was this building constructed?

> Who built it?

> Is there another place like this nearby?

The AI remembers previous context without requiring users to repeat information.

---

## 📍 Smart Recommendation

> **Not built — replaced.** This was the original pitch: ask the model what to see next
> from your location and trip history. It shipped, and every address, price and distance
> in it turned out to be invented, so it was removed rather than patched. What answers the
> same question now is [Explore](#-explore), which asks OpenStreetMap and Wikipedia — two
> sources that can be checked — and the [culture collection](#-culture-collection), which
> tracks what you have found. The code was deleted on 02.08.2026.

Based on:

- Current location
- Previous destinations
- Personal interests

AI recommends:

- Nearby attractions
- Hidden local gems
- Restaurants
- Museums
- Walking routes

---

## 🧠 Personalized Travel Memory

AI remembers the travel journey.

For example:

Day 1

- Temple of Literature
- Hoan Kiem Lake
- Hanoi Old Quarter

The AI can summarize the day's experiences and recommend tomorrow's itinerary.

---

# 🚀 User Flow

```

Open Camera

↓

Point Camera

↓

Gemini Vision analyzes image

↓

Recognize object

↓

Generate explanation

↓

Voice response

↓

User asks follow-up questions

↓

Gemini continues conversation

↓

Recommend nearby attractions

```

---

# 🛠 Tech Stack

| Layer | Choice |
|---|---|
| UI | Jetpack Compose, Material 3, Navigation Compose, Coil |
| Architecture | Clean Architecture, MVI, Repository + UseCase, StateFlow |
| DI | Hilt (KSP) |
| Local storage | Room (single source of truth), DataStore Preferences |
| Networking | Ktor Client (OkHttp engine) + kotlinx.serialization |
| AI | Google Gemini 3 via the Google AI Studio REST API |
| Camera | CameraX (Preview + ImageCapture) |
| Narration | Android `TextToSpeech` / iOS `AVSpeechSynthesizer` |
| Location | Play Services fused location |
| Logging | Timber |
| Testing | JUnit4, MockK, Turbine, Ktor MockEngine, Hilt instrumented tests |

---

# 🏗 Architecture

Three Gradle modules, dependencies pointing inwards:

```
:app      Presentation — Compose UI, MVI ViewModels, navigation, camera, voice
   ↓
:data     Room, DataStore, Ktor Gemini client, mappers, repository impls, Hilt modules
   ↓
:domain   Pure Kotlin — models, repository interfaces, use cases. No Android, no framework.
```

`:domain` is a plain JVM module, so business rules stay testable on the JVM and
cannot accidentally couple to the UI. Every implementation in `:data` is
`internal`, so `:app` can only reach it through a `:domain` interface.

### How a capture becomes a story

```
CameraX capture
   → ImageStorage (EXIF-corrected, downscaled to 1024px, JPEG)
   → GeminiClient (schema-constrained request, model fallback chain)
   → DiscoveryPayload → DiscoveryEntity → Room
   → Flow<Discovery> → MVI ViewModel → Compose
```

Two decisions carry most of the weight:

**Structured output.** Every AI call is bound server-side to a JSON Schema
(`GeminiSchemas.kt`), so parsing is a plain deserialisation — there is no regex
hunting for JSON inside prose, and a declared field is guaranteed to be present.

**Model fallback.** Gemini Flash routinely answers `503 high demand` at peak
times. Failing a capture because of that would be awful when someone is standing
in front of a temple, so a request walks down a chain of models and only gives up
when every one is busy. Non-retryable failures (bad key, malformed request) stop
the chain immediately rather than burning quota.

---

# 🗺 Travel passport

Vietnam drawn as its 34 provinces, each one filling in with the traveller's own
photograph, clipped to that province's real outline. It adds **no dependency** —
no Maps SDK, no API key, no billing, no network — and about 200 KB to the release
APK.

**There is no check-in button.** Every recognised capture already carries the
coordinates it was taken at, so pointing the camera at a temple *is* the check-in.
`DiscoveryRepositoryImpl` resolves the province at write time and stores it in a
denormalised `provinceId` column, which turns the map's per-province roll-up into
one grouped SQL query instead of point-in-polygon over 9,151 vertices per row on
every read.

### The data

`data/src/main/assets/provinces.json` (189 KB) is generated by
`tools/provinces/build_provinces.py` from OpenStreetMap boundary relations (ODbL),
clipped against a Natural Earth coastline (public domain). Two things in that
script are not obvious and both are load-bearing:

**Simplification happens per OSM *way*, not per province.** Neighbouring provinces
share the very same way objects along their common border. Simplifying each way
once and reusing the result means both sides land on an identical line; simplifying
each province's ring independently would drift the two copies apart and open
hairline gaps along every internal border. 74% of ways are shared, so this is most
of the country.

**Polygons are clipped to the coastline.** OSM tags coastal ADM1 boundaries out to
territorial waters — unclipped, the Gulf of Tonkin sits *inside* Hưng Yên and open
water off Vũng Tàu sits *inside* TP.HCM, and the country renders as a slab rather
than the S-shape the whole feature is about.

> Do not regenerate this from GADM or Natural Earth admin levels. Both still carry
> the **pre-2025 63 provinces**; the file must contain exactly 34.

### Resolving a coordinate

A strict polygon test alone would fail precisely where people take photographs:
because the outlines are clipped to the coast, a Hạ Long Bay cruise, a beach at Mỹ
Khê, or a fix that drifted offshore all land in open water and match nothing. So
`locateProvince` falls back to the nearest province within 25 km. Measured against
real tourist coordinates, the furthest one needed 3.4 km.

`ProvinceGeometryTest` runs against the shipped asset and asserts ~63 real
coordinates, including every pre-2025 provincial capital resolving to the province
that absorbed it — so a regenerated asset built from a pre-merger source fails the
build instead of shipping.

### Rendering

A plain Compose `Canvas`. Paths are built once in unmagnified canvas space and pan
and zoom are applied as a transform, so a gesture never rebuilds 9,228 vertices.
Photos are filled with `clipPath(ring) { drawImage(…) }`, **fitted per ring rather
than per province**: Khánh Hòa's bounding box reaches Trường Sa and is almost
entirely open sea, so a single copy stretched across it left the mainland showing a
narrow strip of the photo while the islets showed an unrelated band from its
opposite edge.

At country zoom a province is 40–80 px wide, so a photo inside one reads as a patch
of colour rather than a picture. That is the intended mosaic effect; pinch-zoom and
the detail sheet are what make an individual photo legible.

### Hoàng Sa and Trường Sa

Both archipelagos are drawn in labelled inset boxes down the right-hand side, the
way every Vietnamese map draws them. Placed where they actually are, they push the
country's extent 3.4° further east and flatten the mainland's aspect from 0.475 to
0.818 — pinning the part a traveller can actually collect into little over half the
screen to make room for open sea. The insets do not pan or zoom with the mainland;
they are a separate reference frame, which is also the convention. Tapping either
box selects the province that administers it.

Getting them into the data took two different routes, and neither is obvious:

- **Trường Sa** arrives inside OSM's Khánh Hòa boundary relation — but only as three
  administrative sea areas up to 1.3° across. Drawn as islands those are three
  enormous meaningless circles, so the map filters them out by size. They stay in
  the data because they are what makes a photo taken anywhere in the archipelago
  resolve to Khánh Hòa, including on islands OSM has not mapped.
- **Hoàng Sa** is in no boundary relation at all. OSM keeps the disputed archipelago
  out of every national admin hierarchy, so the first cut of this map drew Trường Sa
  and silently omitted Hoàng Sa. Vietnam administers it as huyện đảo Hoàng Sa, a
  district of Đà Nẵng.

Both are therefore drawn from OSM's own island/islet/reef features — real surveyed
geometry, nothing hand-drawn — curated into `tools/provinces/archipelagos.json` in
one clearly labelled step, because attaching Hoàng Sa to Đà Nẵng is a claim the
boundary data does not itself make. `ProvinceGeometryTest` asserts both archipelagos
are present and resolve to their province, so regenerating the asset cannot quietly
drop one.

### Demoing it

Every capture taken in one room resolves to one province, so the map cannot be seen
filling in from a desk. `tools/seed_demo.py` pushes nine discoveries spread from Sa
Pa to Phú Quốc into the **debug** build, with `provinceId` left null so the app's own
backfill resolves them:

```bash
./gradlew :app:installDebug && python3 tools/seed_demo.py
```

Debug carries `applicationIdSuffix = ".dev"`, so it installs *beside* the signed
demo build rather than replacing it — seeding never touches the APK being demoed.

### Known gaps

- **No accessibility.** The map is a bare `Canvas` with no semantics, so TalkBack
  skips the feature entirely. Needs virtual semantics nodes or a parallel province
  list; not a hackathon-sized job, but a real blocker for a public release.
- **Small provinces are hard to tap.** Hit testing is strict point-in-polygon with
  no tolerance, while several provinces are ~22 dp wide at country zoom. Zoom works
  around it; a zoom-scaled snap radius in the tap path would fix it.
- **OSM attribution is not user-visible.** It is in this README and in the source,
  but ODbL wants it on the produced work — it belongs in a licences screen before
  any store listing.
- **The app layer is untested.** `ProvinceGeometryTest` covers the geometry; there
  is no test for `PassportViewModel`, `ProvinceRepositoryImpl`, or the screen.

---

# 🎴 Culture collection

A board of 61 things worth finding in Vietnam — dishes, roof details, a basket boat, a
stele on a stone tortoise — grouped by the same categories the recogniser already
uses. A tile is a hatched square until the traveller photographs the thing, and then
it fills with **their own picture**.

It replaced a list of AI-generated "where to next" suggestions, which had three
problems that no amount of prompt work would fix. It was empty until you had already
captured something, so the first launch showed a dead end. It had no Places API behind
it, so every address, price and distance was invented. And it could not be
demonstrated from a desk, because the suggestions were always about wherever you were
sitting. The board has none of those: it is full on first launch, every word in it was
written by hand, and tapping a tile works anywhere.

### It lives beside the passport, not in a tab

The collection first shipped *as* the Explore tab, and that was wrong. It and the
passport are the same move along different axes — slots that fill with your own
photographs, a progress count, tap for detail — one counting *where* you have been and
the other *what* you have found. Two tabs' worth of apparatus for one idea, with the
map buried in the journal and the board promoted to the bottom bar, made them read as
rival features rather than as two views of one trip.

So the board is pushed from the journal, directly under the passport row, and the two
are drawn as siblings rather than merely described as such: one `ProgressRow` for both
journal CTAs, one `FillGauge` for both progress bars, and the same header form — back
chip, headline, subtitle on one line, then the count with the percentage demoted to a
kicker on the right. Moving between the two should feel like turning a page.

`FillGauge` is two boxes rather than a `LinearProgressIndicator`, and that is worth
knowing before anyone "simplifies" it: since M3 1.3 the indicator draws a gap and a
stop dot at the leading edge. Right for a determinate task, wrong here — nothing is in
flight, the bar is a fill level, and the stop dot reads as a target being missed.

**The Explore tab is where to go next.** It is the thing neither of these can be —
something pointing forwards rather than a third account of where you have already been.
See [Explore](#-explore) below.

**There is no progress table.** The board is `catalog.json × the journal`, computed on
read. Storing "unlocked" rows would be a second source of truth that could outlive a
deleted photograph, and deriving it means the collection is *retroactive* — install
this build over an older one and the tiles are already part-filled from photos taken
weeks ago.

### Matching, and why diacritics are kept

A discovery is matched to a catalogue entry by comparing its title, local name and
tags against that entry's `aliases`, as whole words, **without folding Vietnamese tone
marks**. Folding is the obvious implementation and it is quietly wrong: `phở` and
`phố` become the same string, so a photograph of Phố cổ Hội An would collect a bowl of
noodles. `đình` collapses onto `định`, `cửa` onto `của`. Gemini writes Vietnamese with
its diacritics and the catalogue is written the same way, so keeping them costs
nothing and removes a whole class of wrong unlocks. `CatalogMatcherTest` pins that
case, along with the whole-word rule that stops `chè` matching `chèo`.

The summary and section bodies are deliberately **not** searched. They are paragraphs
that mention neighbouring dishes and regional variants, and matching against them
would collect half the board from one photograph.

### Two languages, and which one

Catalogue names and hints follow the **interface** language, not the in-app "story
language" that governs the AI's voice — a traveller is expected to pair an English
interface with Vietnamese narration. Since Compose Resources offers no way to read
back which locale it resolved, the string table answers for itself through a
`resource_language` marker; see `uiLanguage()` in `Formatters.kt`.

### Known gaps

- **No new-unlock moment.** A tile flips to collected silently the next time the board
  is opened. The reveal is the most rewarding part of a collection and it currently
  happens off-screen.
- **The board is still a record, not a guide.** It shows what you have; it never says
  what to go and get. The recognition hints are the material for that — sixty-one
  sentences teaching how to look — but they sit behind a tap on a grey tile instead of
  out in the open.
- **The catalogue is Kinh-majority.** Highland and Cham material is present but thin,
  and the Mekong delta is under-represented against the north.
- **`aliases` is a hand-maintained list.** A dish Gemini names in a way nobody
  anticipated stays locked with no signal that anything went wrong.
- **The favourites and translation-history reads are held open by tests alone.**
  `DiscoveryRepository.observeFavorites` and `TranslationRepository.observeTranslations`
  / `observeTranslation` / `delete` lost their last screen when the recommendation
  clean-out removed the use cases in front of them. The queries still work and are still
  tested; nothing calls them.

---

# 🧭 Explore

A live map of wherever the traveller is standing, with the places worth walking to
within **5 km** already pinned on it. Tap a marker and a sheet comes up with the
photograph, the encyclopaedia entry, how many people read about it, the distance, the
opening hours and whatever else was mapped. **Bắt đầu** hands the place to Google Maps
with the destination already filled in.

### Built on open data, and why that turned out to matter

The first version of this screen ran on the **Google Places API**, and it worked — real
ratings, real reviews, real photographs. It is gone because Places API (New) could not
be enabled on the project this app is built under, and a feature that a billing setting
can switch off is not a feature. Everything here now comes from sources that need **no
API key and no billing account**:

| Source | Question it answers |
|---|---|
| OpenStreetMap (Overpass) | what *is* a place, and where |
| Wikipedia | what is this place, and how many people read about it |
| Wikimedia Commons | what does it look like |

The one Google dependency left is the Maps SDK that draws the Android map, and iOS does
not even need that — it uses MapKit.

**There is no star rating, and there is not meant to be.** No open source carries
Google's ratings, so rather than invent a number the screen ranks by two things anybody
can check: Wikipedia readership over the last 60 days, and how carefully somebody mapped
the place. That is a real loss against the Places version and an honest one — inventing
a rating would have reintroduced the exact fault this feature was built to avoid, the
one that got the old AI recommendations deleted: *"every address, price and distance was
invented."*

### Each source is asked exactly one question

The division of labour is load-bearing, and both halves of it were learned by getting it
wrong first.

**Wikipedia does not choose the places.** An early version let it contribute places as
well as describe them, and the top of the list came back as "Hà Nội" and "Liên bang Đông
Dương" — geotagged articles that are not destinations. Wikipedia geotags cities,
hospitals, embassies and defunct political entities; a survey of 50 articles within 5 km
of Hoàn Kiếm found **54% were not places anyone visits**. OSM has real POI
classification, so OSM alone decides what appears.

**Wikipedia is only consulted where OSM points at it.** Looking articles up by name
seemed reasonable and is catastrophic: a zoo enclosure named "Báo lửa" matches the
article about the *leopard species*, a bell named "Chuông" matches the article about
bells, and "Thánh Paul" matches the apostle. Measured hit rate for name lookup was 32%,
almost all of it wrong. The `wikipedia` tag on an OSM element is an assertion by whoever
mapped it, so it cannot produce a false match.

### The Overpass query is the whole feature

Three things about it are not obvious and all three were bugs first.

**Places of worship must be asked for explicitly.** Vietnamese pagodas, đình, temples
and cathedrals overwhelmingly carry no `tourism` or `historic` tag at all. Verified
individually: Đền Ngọc Sơn is `place_of_worship` + `building=temple`, Nhà thờ Lớn is
`place_of_worship` alone, Văn Miếu is `landuse=religious` + `religion=confucian`. A
standard tourism filter finds **none of them**, while returning 48 traffic-island flower
beds — there are 222 named places of worship inside 5 km of Hoàn Kiếm that it never sees.

**Tag filters must precede the spatial filter.** Written `nw(around:…)["name"]["amenity"=…]`
the query times out; written `nw["amenity"=…]["name"](around:…)` it returns in seconds.
And a timed-out Overpass query answers **200 OK with an empty element list and a
`remark`** — so before that field was read, the screen confidently told a traveller
standing in central Hanoi that there was nothing around them.

**One query is too big.** The union spans ~250 sights and ~1,700 places to eat in
central Hanoi and does not finish. It is split into a sights query and a food query, run
concurrently against *different* mirrors.

`ENDPOINTS` lists only instances with worldwide coverage, and that qualifier is
load-bearing too: several widely-listed mirrors are regional extracts that do not say
so. `overpass.osm.ch` answers a Hanoi query with a cheerful `200 OK` and zero elements
because it holds Switzerland — it briefly shipped here as a fallback that silently
emptied the map.

### Ranking, and the curation layer under it

`NearbyPlace.prominence` is `log10(readers)` + mapping detail + bonuses for having a
description and a photograph + a bias towards sights, minus a light distance penalty
that only breaks ties. Restaurants have no readership at all, so mapping detail is their
only signal — a place whose mapper filled in opening hours, a website and a phone number
is far more likely to be real than a bare node.

Sights are also *capped against* food (`rankWithBalance`). Without it the screen tips
over completely in a residential district: measured in Nam Từ Liêm, an unfiltered
ranking returned forty takeaway grills and not one landmark, because OSM's food coverage
is dense and consistently tagged everywhere while its attraction coverage is thin
outside historic centres.

`isNotADestination` is a blocklist and is honest about being one. Roughly **half** of
what OSM files under "attraction" near Hoàn Kiếm is not somewhere anyone would go: 25
individual animal pens inside Thủ Lệ zoo tagged `tourism=attraction` (the screen was
recommending a golden monkey), 48 planted roundabouts, 15 identically-named ward war
memorials, and entries genuinely present in the data called `0 km`, `Abandoned Van`,
`Chuông` (a bell) and `Lư` (an incense burner). Chains are deduplicated by name — OSM
holds 35 branches of Highlands Coffee within 5 km, and the traveller wants the nearest.

### The map is the only `expect`

`PlaceMap` is the one platform-specific thing in the feature — the Maps SDK on Android,
MapKit on iOS, because there is no Google map on iOS to draw. Everything above it is
written once: what gets searched for, how it is ranked, what a marker means, what the
sheet says. The *places* are identical on both platforms — they arrive over plain HTTP
from OpenStreetMap and Wikipedia, which do not care what draws them — so only the
cartography underneath differs.

Markers carry the app's existing `CategoryColors` rather than a map palette of their
own — a temple is the same lacquer red here that it is in the journal and on the
culture board, so there is one colour scheme to learn rather than two.

Three details worth knowing before touching either actual:

- **`MapCamera.requestId`** is what makes it a request rather than a value. Without it,
  a second tap on a marker the traveller has since panned away from does nothing —
  the target is unchanged, so nothing recomposes.
- **The iOS interop view keeps `Cooperative` touch handling**, not `NonCooperative`.
  Cooperative costs the first ~150 ms of a pan, which MapKit replays; NonCooperative
  would hand MapKit every touch landing on it first, and the sheet's scrim lies
  directly on top of that view — losing tap-to-dismiss is the worse trade.
- **No companion objects inside `PlaceMapController`.** Kotlin/Native rejects
  "Fields are not supported for Companion of subclass of ObjC type", so its constants
  live at file scope.

### The cache is in memory, and that is deliberate

Every other repository here is Room-backed because the traveller's own history has to
survive a cold launch. This one holds somebody else's data about wherever the phone
happened to be, and restoring it from disk would open the screen on last week's city.
A Room table would also have meant a schema migration — and the project's
`fallbackToDestructiveMigration` would have dropped the whole journal to cache a list
of cafés.

Fifteen minutes and 300 metres is the window: walking the length of a street reuses the
search, crossing town re-runs it. The details cache is keyed on the place **and the
language** — keyed on the id alone it served the Vietnamese article back after a switch
to English, which is the one thing the switch was supposed to change.

### Known gaps

- **Most places have no photograph.** OSM holds no images, and only the well-known few
  carry a Wikipedia article or have been photographed onto Commons near enough to match.
  Measured in a residential district: none of forty. Those fall back to a category glyph
  in the place's own colour, which is why that had to look deliberate rather than broken.
- **Overpass is donated infrastructure and it throttles.** A burst of development
  traffic exhausts the per-IP allowance and every request until it recovers is refused;
  the mirror rotation exists for exactly this. A search takes seconds when it works and
  fails inside half a minute when it does not.
- **Neither map actual is tested.** `OpenMapClientTest` covers the wire formats, the
  query shape and the junk filter, but `PlaceMap.android.kt` and `PlaceMap.ios.kt` have
  never been exercised by a test.
- **Markers are not clustered.** Forty pins at neighbourhood zoom in a dense quarter
  still overlap; MapKit hides the losers by display priority and the Maps SDK stacks them.
- **The map is not accessible.** Both SDKs expose their own annotations to the
  platform's screen reader, but the place strip along the bottom is the only part of
  this screen with real semantics.
- **Names are Vietnamese-only for about half the data.** Only 47% of OSM attractions
  near Hoàn Kiếm carry `name:en`, and the app does not read it even where it exists.
- **No offline state.** The tiles, the photographs and the search all need a network.

---

# 🚀 Getting started

### Requirements

- Android Studio (AGP 9.2+), JDK 17+
- Android SDK Platform 37 installed
- A device or emulator on **API 26+**
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey)

### 1. Add your Gemini key

Put it in `local.properties`, which is git-ignored so the key never reaches
version control:

```properties
GEMINI_API_KEY=your_key_here
```

It is injected into `data/BuildConfig.GEMINI_API_KEY` at build time. Users can
also paste their own key at runtime under **Settings → Gemini API key**, which
takes precedence over the build-time one.

> Only Gemini **3.x** models are offered. Google retires older generations for
> keys created after a cutoff date, so a `gemini-2.x` id would `404` for any new
> user of this app.

### 1b. Add a Maps SDK key (Android only, and only for the Explore map)

One key, and it draws the Android map — nothing else. The places on that map come from
OpenStreetMap and Wikipedia and need no key at all, so this is the entire Google
footprint of the Explore tab.

```properties
MAPS_API_KEY=your_maps_sdk_key
```

Enable **Maps SDK for Android** on it, and restrict it to this app: package
`com.duylt.trave.vietlensai` (plus `.dev` for debug builds) and your signing SHA-1.
It is substituted into `com.google.android.geo.API_KEY` in the app manifest.

Without it the Explore tab still works — the markers, the sheet and the directions
button are all unaffected — but the map renders as blank tiles underneath them.

> **iOS needs no key whatsoever.** The map there is MapKit, which every iOS app links
> for free, and the place data is keyless on both platforms.

### 2. Build and run

```bash
./gradlew :app:installDebug     # build + install
./gradlew test                  # unit tests (no network, no key needed)
./gradlew :app:connectedDebugAndroidTest   # end-to-end, calls the real API
```

The instrumented suite drives the real pipeline — photo on disk → Gemini → Room —
and *skips* rather than fails when the API is unreachable or throttled, so an
offline machine never produces a misleading red build.

---

# 📦 Release builds

**Demo on the release build, not the debug one.** Debug disables R8, ships every
Compose tooling class and spreads the app over 22 dex files; release is one dex
with a baseline profile. Cold start measured on a Galaxy A16 (Android 16); size
and dex counts re-measured on 02.08.2026:

| | Debug | Release | |
|---|---|---|---|
| Cold start | ~2,580 ms | **~440 ms** | 5.9× faster |
| APK size | 75.4 MB | **49.9 MB** | −25.5 MB |
| dex files | 22 | **1** | |

Both figures are dominated by the four bundled ML Kit recognisers, which are
~45 MB of model data that R8 cannot touch — the code R8 *can* touch shrinks by
roughly the same 8× the earlier, pre-OCR measurement of 27.0 MB → 3.03 MB showed.

### One flag, two build types

`:shared` and `:data` are multiplatform modules built by
`com.android.kotlin.multiplatform.library`, which produces a **single** Android
variant — the debug APK and the release APK link the very same class files. So
nothing inside them can ask which build type it ended up in, and a generated
`DEBUG` constant there is a trap: it used to come from `-Pvietlens.debug`,
default `true`, which meant a plain `:app:assembleRelease` shipped with Ktor's
`Logging` plugin installed.

The flag is passed in at startup instead, by the one caller per platform that
knows the answer:

```kotlin
// VietLensApplication.kt (Android)
modules(appModules(BuildConfig.DEBUG))

// iOSApp.swift -> MainViewController.kt
MainViewControllerKt.startVietLens(debug: true)   // inside #if DEBUG
```

`appModules(isDebug)` threads it down to `networkModule(isDebug)`, the only
consumer. In a release build the `Logging` plugin is then never referenced, so
R8 drops Ktor's whole logging path out of the APK rather than leaving it dormant
— verified in `mapping.txt`, which contains no `io.ktor.client.plugins.logging`
class at all.

The app version works the same way, for the same reason: `vietlens.versionName`
and `vietlens.versionCode` live in the root `gradle.properties`, because `:app`
stamps the APK with them and `:shared` compiles the version into the Settings
footer. Written out separately they had already drifted — the APK said 1.0 while
the footer said 1.0.0.

### Signing

Signing material is read from the git-ignored `local.properties`; the keystore
itself is git-ignored too, so nothing sensitive reaches version control:

```properties
RELEASE_STORE_FILE=vietlens-release.jks
RELEASE_STORE_PASSWORD=…
RELEASE_KEY_ALIAS=vietlens
RELEASE_KEY_PASSWORD=…
```

Create one with:

```bash
keytool -genkeypair -v -keystore app/vietlens-release.jks \
        -alias vietlens -keyalg RSA -keysize 2048 -validity 10000
```

If the keystore or its credentials are missing the build still succeeds and
produces an *unsigned* release APK, rather than failing with a confusing
`validateSigningRelease` error on a fresh clone.

```bash
./gradlew :app:assembleRelease   # -> app/build/outputs/apk/release/VietLensAI_v1.0.0_build1_<date>.apk
./gradlew :app:installRelease    # build, sign and install
```

APKs are date-stamped so the file handed round during a demo says which build it
is. Debug carries a `-dev` version suffix, which shows up on the Settings screen.

#### `fastRelease`: the release build without the minute of waiting

```bash
./gradlew :app:assembleFastRelease   # -> app/build/outputs/apk/fastRelease/…-fastRelease.apk
./gradlew :app:installFastRelease
```

`fastRelease` is `release` with R8 turned off, and it exists because of where the
time goes. Profiling `assembleRelease` on a warm tree: `minifyReleaseWithR8`
takes 45–61s and every one of the other 129 tasks put together takes under 12s.
Shrinking is not part of that build, it *is* that build. `assembleFastRelease` on
the same tree finishes in ~12s.

Everything the running app can observe is unchanged: same `applicationId`, same
release keystore, `BuildConfig.DEBUG` still false — so Ktor's `Logging` plugin
stays off — and it installs straight over a release APK. Only the version name
differs, `1.0.0-fast`, visible in Android's app info.

What it cannot tell you is anything that only appears under obfuscation: a
missing keep rule, a serializer resolved by name, a class R8 renamed out from
under reflection. **Whatever ships to Play goes through `assembleRelease` and
gets smoke-tested there.** `fastRelease` is for the phone being passed around a
table.

The name says what the variant *is*, not what it is for: `demo` would have read
as "the build carrying the seeded demo data" next to `tools/seed_demo.py`, which
is an unrelated thing.

Lint is no longer part of either: `lintVital` ran on every non-debuggable
variant and was measured at 12.7s, which `assembleRelease` hid behind the longer
R8 task but `assembleFastRelease` could not. Since `abortOnError = false` already
meant lint could never fail a build, nothing was given up by taking it off the
path — run it deliberately with `./gradlew :app:lint`.

### A note on AGP 9 and minification

The project template generates `optimization { enable = false }` in the release
build type, and it is tempting to read that as the modern replacement for
`isMinifyEnabled`. It is not. `optimization.enable` is an experimental
"gradual R8" feature that fails configuration outright unless
`android.r8.gradual.support=true` is also set. Code shrinking in AGP 9.2.1 is
still driven by:

```kotlin
isMinifyEnabled = true
isShrinkResources = true
proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
```

Two further AGP 9 specifics: `getDefaultProguardFile("proguard-android.txt")` is
now a hard error (it implies `-dontoptimize`), and `android.enableR8.fullMode`
should not be set at all — full mode is the default and the flag is scheduled for
removal in AGP 10.

`app/proguard-rules.pro` is deliberately almost empty. Every dependency here
ships its own consumer rules, and a minified build was verified to produce no
`missing_rules.txt` at all. The only rules the app adds keep release stack traces
readable (`SourceFile`, `LineNumberTable`); see the comments in that file for why
the usual kotlinx-serialization / Room / Koin keeps are *not* needed and should
not be pasted back in.

Two other files look like they hold keep rules, and one of them does not:

* `app/src/main/keepRules/rules.keep` — AGP 9 combines everything under
  `src/main/keepRules` and passes it to R8, so this **is** live. It holds only
  the template's comments; rules go in `proguard-rules.pro`, next to the
  reasoning for them.
* `data/consumer-rules.pro` — **inert**. It is a leftover from when `:data` was a
  `com.android.library`. The multiplatform-library plugin publishes consumer
  rules only when `optimization { consumerKeepRules { publish = true } }` asks it
  to, and nothing does; the file explains how to wire it if a rule ever has to
  travel with the module.

Either way, confirm a rule landed by looking for its file as a section in
`app/build/outputs/mapping/release/configuration.txt` after a release build.

---

# 🎯 Target Users

- International tourists
- Domestic travelers
- Students
- Museums
- Cultural heritage sites
- Travel agencies

---

# 🌟 Why AI Travel Companion?

Unlike traditional travel apps that only provide static information, AI Travel Companion acts as an intelligent local guide capable of understanding images, maintaining natural conversations, and personalizing recommendations based on the user's journey.

The application makes cultural exploration more engaging, interactive, and accessible for everyone.

---

# ✅ What is built

| Feature | Status |
|---|---|
| Camera capture + gallery import, 5 lens modes | ✅ |
| Landmark / food / artifact recognition with structured output | ✅ |
| Rich result screen: sections, fun facts, tags, nearby, confidence | ✅ |
| Contextual chat grounded on the discovery, history persisted | ✅ |
| Narration of answers and translations (TextToSpeech) | ✅ |
| OCR + bilingual translation of menus and signs | ✅ |
| Travel journal grouped by day + AI day summary | ✅ |
| Travel passport: 34-province map that fills with your own photos | ✅ |
| Culture collection: 61 things to find, unlocked by your own photographs | ✅ |
| Explore: live map of what is worth walking to within 5 km | ✅ |
| Recommendations from location and trip history | Replaced by the collection — see below |
| Settings: API key, model, language, theme, TTS, location | ✅ |
| Vietnamese + English UI and AI narration | ✅ |
| Offline-first reads (Room single source of truth) | ✅ |
| Gemini Live API (real-time voice) | Roadmap |
| Google Maps SDK embed (Android) + MapKit (iOS) on the Explore map | ✅ |

---

# 🔮 Future Roadmap

- AR Navigation
- Offline AI Support
- Multi-language Voice Guide
- Gamification & Travel Passport
- Personalized Travel Diary
- Family & Kids Mode
- Accessibility Features
- Community Recommendations

---

# 🏆 AI Riser Vietnam 2026

Category:

- Cultural Tourism & Sports

Powered by:

- Google Gemini
- Google AI Studio

Mission:

> Making every traveler feel like they are exploring Vietnam with a knowledgeable local guide by their side.