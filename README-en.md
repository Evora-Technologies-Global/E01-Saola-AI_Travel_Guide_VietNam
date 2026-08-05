# VietLens AI 🇻🇳

> Point the camera at a temple roof, a bowl of noodles or a street sign — the app recognises
> it, tells the story behind it, and keeps it in your journey.

🇻🇳 [Bản tiếng Việt](README.md) · 🛠 [Technical detail](TechStack-temp.md)

---

## 1. What this app is

VietLens AI is a travel app that uses **Google Gemini** to turn the phone camera into a local
guide.

Instead of searching Google, reading Wikipedia and opening a separate translation app, the
traveller just holds the phone up. The AI recognises the landmark, dish or artefact in frame,
explains its history and cultural meaning, translates menus and signs, and answers follow-up
questions — all on one screen.

What sets it apart from an ordinary travel app: **everything you photograph is kept**. Your
own pictures fill in a map of Vietnam's 34 provinces, unlock a board of 61 things worth
finding, and each day is written up by the AI as a page of a journal.

The app runs on **Android and iOS** from one shared codebase, speaks **eight languages**, and
keeps all of the traveller's data on their own device — no account, no server.

---

## 2. Features

### Core — recognise and converse

| Feature | What it does |
|---|---|
| **Five lens modes** | Auto · Heritage · Food · Museum · Translate |
| **Structured recognition** | Returns a name, local name, summary, sections, tags and a confidence — not a wall of prose |
| **Contextual chat** | Ask follow-up questions grounded on the place you just photographed; history is kept |
| **Narration** | Reads answers and translations aloud in the device's language |
| **Menu & sign translation** | On-device OCR plus translation, overlaid in place over each line of text |

### The distinctive part — what brings people back

**🗺 Travel passport.** Vietnam drawn as its **34 post-2025 provinces**, each one filling in
with the photograph you took there, clipped to that province's real outline. **There is no
check-in button** — a photo already carries its coordinates, so photographing a pagoda *is*
the check-in. **Hoàng Sa and Trường Sa** are included in inset boxes, the way every Vietnamese
map draws them.

**🎴 Culture collection.** 61 things worth finding in Vietnam — phở, bánh chưng, the upturned
eaves of a communal house, a basket boat, a stele on a stone tortoise, a gong. Each tile is a
hatched square until you photograph the thing, and then it fills with **your own picture**. The
board is full from the first launch, and it counts photographs you took weeks ago.

**🧭 Explore.** A live map of what is worth walking to within **5 km**. The data comes from
**OpenStreetMap + Wikipedia + Wikimedia Commons** — no API key, no billing. Ranked by
Wikipedia readership over the last 60 days and by how carefully somebody mapped the place.
**There are no star ratings**, because no open source carries them — better to be missing a
number than to invent one.

**📖 Travel journal.** Grouped by day, with an AI-written summary of each day and ideas for
tomorrow.

**🌏 Eight languages.** Vietnamese · English · Japanese · Korean · Chinese · French · Spanish ·
Thai. The interface, the AI's answers and the narration voice all **follow the device
language** — a Japanese phone gets Japanese screens, Japanese answers and a Japanese voice.

**📱 Two layouts.** Phones use a bottom tab bar; tablets and wide windows use a vertical
navigation rail and a two-pane layout — read the article on the left, ask the guide on the
right.

---

## 3. Screenshots

Taken on Pixel 7 Pro, Pixel Tablet, iPhone 17 and iPad Pro 11" simulators, with the 24-place
demo dataset from Sa Pa to Cần Thơ (see
[TechStack §10](TechStack-temp.md#10-demoing-it-and-the-seeded-data)); the pictures inside the
app are real photographs from Wikimedia Commons.

### Android — phone

| Lens | Journal | Travel passport | Culture collection |
|---|---|---|---|
| <img src="screenshots/android-phone/01-lens.png" width="180" alt="Lens"> | <img src="screenshots/android-phone/02-journal.png" width="180" alt="Journal"> | <img src="screenshots/android-phone/03-passport.png" width="180" alt="Passport"> | <img src="screenshots/android-phone/04-collection.png" width="180" alt="Collection"> |

| Discovery detail | Chat | Explore nearby |
|---|---|---|
| <img src="screenshots/android-phone/05-discovery.png" width="180" alt="Discovery"> | <img src="screenshots/android-phone/06-chat.png" width="180" alt="Chat"> | <img src="screenshots/android-phone/07-explore.png" width="180" alt="Explore"> |

| Place detail | Menu translation | Settings |
|---|---|---|
| <img src="screenshots/android-phone/08-explore-detail.png" width="180" alt="Place"> | <img src="screenshots/android-phone/09-translate.png" width="180" alt="Translate"> | <img src="screenshots/android-phone/10-settings.png" width="180" alt="Settings"> |

### Android — tablet

The wide layout: a vertical navigation rail on the left and two content panes side by side.

| Lens | Journal + passport | Collection |
|---|---|---|
| <img src="screenshots/android-tablet/01-lens.png" width="250" alt="Lens"> | <img src="screenshots/android-tablet/02-journal.png" width="250" alt="Journal"> | <img src="screenshots/android-tablet/03-collection.png" width="250" alt="Collection"> |

| Article + ask the guide | Explore | Settings |
|---|---|---|
| <img src="screenshots/android-tablet/04-discovery.png" width="250" alt="Discovery"> | <img src="screenshots/android-tablet/05-explore.png" width="250" alt="Explore"> | <img src="screenshots/android-tablet/06-settings.png" width="250" alt="Settings"> |

### iOS — iPhone

| Journal | Passport | Collection | Discovery detail |
|---|---|---|---|
| <img src="screenshots/ios-phone/01-journal.png" width="180" alt="Journal"> | <img src="screenshots/ios-phone/02-passport.png" width="180" alt="Passport"> | <img src="screenshots/ios-phone/03-collection.png" width="180" alt="Collection"> | <img src="screenshots/ios-phone/04-discovery.png" width="180" alt="Discovery"> |

| Chat | Explore (MapKit) | Settings |
|---|---|---|
| <img src="screenshots/ios-phone/05-chat.png" width="180" alt="Chat"> | <img src="screenshots/ios-phone/06-explore.png" width="180" alt="Explore"> | <img src="screenshots/ios-phone/07-settings.png" width="180" alt="Settings"> |

> The Lens screen is absent from the iOS set because the iOS Simulator has no camera — the
> viewfinder would be blank.

### iOS — iPad

| Journal + passport | Collection | Article + ask the guide |
|---|---|---|
| <img src="screenshots/ios-ipad/01-journal.png" width="250" alt="Journal"> | <img src="screenshots/ios-ipad/02-collection.png" width="250" alt="Collection"> | <img src="screenshots/ios-ipad/03-discovery.png" width="250" alt="Discovery"> |

| Explore | Settings |
|---|---|
| <img src="screenshots/ios-ipad/04-explore.png" width="250" alt="Explore"> | <img src="screenshots/ios-ipad/05-settings.png" width="250" alt="Settings"> |

> The iPad Explore shot shows twelve restaurants and no landmarks: Overpass (the OpenStreetMap
> query server) rate-limits per IP, and after a day of repeated searches the sights half of the
> query was refused. That is the app's real behaviour under throttling, not a layout fault.

---

## 4. Technology

One Kotlin Multiplatform codebase runs on both platforms; **every screen, every ViewModel and
the whole design system is written once** in Compose Multiplatform. Only the camera, OCR, maps,
location and narration are written per platform.

| Area | Technology | Version |
|---|---|---|
| Language | Kotlin Multiplatform | 2.3.21 |
| Build | AGP · KSP · JDK | 9.2.1 · 2.3.10 · 17+ |
| UI | Compose Multiplatform | 1.12.0-beta03 |
| Navigation | Navigation Compose (JetBrains) | 2.9.2 |
| Lifecycle / ViewModel | Lifecycle (JetBrains) | 2.11.0 |
| Architecture | Clean Architecture + MVI (`MviViewModel<S, I, E>`) | — |
| Dependency injection | Koin | 4.2.2 |
| Database | Room (multiplatform) + bundled SQLite | 2.8.4 · 2.7.0 |
| Preferences | DataStore Preferences | 1.2.1 |
| Networking | Ktor Client (OkHttp on Android, Darwin on iOS) | 3.5.1 |
| JSON | kotlinx.serialization | 1.11.0 |
| Concurrency | kotlinx.coroutines | 1.11.0 |
| Date / time | kotlinx-datetime | 0.8.0 |
| Images | Coil 3 | 3.4.0 |
| Camera | CameraX (Android) · AVFoundation (iOS) | 1.6.1 · platform |
| OCR | ML Kit — Latin, Chinese, Japanese, Korean (Android) · Vision (iOS) | 16.0.1 · platform |
| Maps | Maps Compose (Android) · MapKit (iOS) | 8.3.1 · platform |
| Location | Play Services Location · CoreLocation | 21.4.0 · platform |
| Narration | TextToSpeech · AVSpeechSynthesizer | platform |
| Logging | Kermit (logcat / os_log) | 2.1.0 |
| **AI** | **Google Gemini 3** via the Google AI Studio REST API | 3.5-flash → 3.1-flash-lite → 3-pro-preview |
| Testing | JUnit4 · MockK · Turbine · Ktor MockEngine · Koin test | 4.13.2 · 1.14.11 · 1.2.1 |

**Platform floor:** Android 8.0 (API 26) and above · iOS 16.0 and above.

**Four modules**, dependencies pointing one way, inwards:

```
:app  →  :shared  →  :domain  ←  :data
Android   the whole   pure       Room, Ktor,
 host    presentation Kotlin     DataStore
```

**Two technical decisions worth explaining to a non-engineer:**

- **Gemini returns JSON bound to a schema**, not prose. That is why the result screen always
  has every field it needs and never has to guess at what the AI wrote.
- **A model fallback chain.** Gemini Flash routinely returns `503` at peak times. Failing there
  would show an error to someone standing in front of a temple, so a request walks down to the
  next model and only gives up when the whole chain is busy.

The full detail — 34-province map geometry, the Overpass query, R8/AGP, APK signing — is in
[TechStack-temp.md](TechStack-temp.md).

---

## 5. Where the data lives

**Everything stays on the traveller's device. No account, no server, no analytics.**

| Data | Stored in | Notes |
|---|---|---|
| Discoveries, conversations, notes, translations, day summaries | **Room (SQLite)** — five tables | The single source of truth; screens read it through `Flow`, so it works offline |
| Photographs | **JPEG files** in the app's own directory | EXIF-corrected upright, downscaled to a 1024 px long edge |
| Preferences (API key, model, theme, narration, location) | **DataStore Preferences** | |
| The 34 provinces and 61 catalogue entries | **Assets bundled in the app** | Read-only, no network needed |
| Nearby-place data | **In memory** — 15 minutes / 300 m | Deliberately not on disk: this is data about where you *are*, and reopening the app onto last week's city would be wrong |

**The database stores a photo's *file name*, never a path.** On iOS the app's data directory is
renamed on every reinstall, update or restore, so a path stored yesterday points at a directory
that no longer exists. That bug once deleted **every photograph** a user had.

**What leaves the device:** the photo being recognised (to Gemini), the current coordinates (to
OpenStreetMap / Wikipedia), and map tiles. Nothing else.

**The Gemini API key** is read from `local.properties` at build time (never committed), or the
user pastes their own under **Settings → API key**, which takes precedence.

---

## 6. Who it is for

| Audience | Why it fits |
|---|---|
| **International visitors to Vietnam** | The two hardest barriers are the script and the cultural context — the app handles both in their own language |
| **Vietnamese travelling domestically** | The 34-province passport and the culture board turn a trip into something collectable |
| **Students** | On-the-spot lookup in museums and heritage sites, with content broken into sections rather than one block of text |
| **Museums and heritage sites** | Works as an intelligent audio guide with no hardware to buy |
| **Tour operators** | Supports the guide during the parts of a tour where guests explore on their own |

**Primary focus for the first phase:** international visitors and independent Vietnamese
travellers — the two groups whose main tool is a phone and who have no guide beside them.

---

## 7. Current status

| Item | Status |
|---|---|
| Camera capture / gallery import, five lens modes | ✅ |
| Landmark / food / artefact recognition with structured output | ✅ |
| Contextual chat, history persisted | ✅ |
| Narration of answers and translations | ✅ |
| OCR + translation of menus and signs, overlaid in place | ✅ |
| Journal grouped by day + AI day summary | ✅ |
| Travel passport across 34 provinces | ✅ |
| Culture collection of 61 entries | ✅ |
| Explore what is worth walking to within 5 km | ✅ |
| Eight-language UI and narration, following the device | ✅ |
| Offline-first reads (Room as the single source of truth) | ✅ |
| Tablet / wide-window layout | ✅ |
| iOS (Compose Multiplatform + MapKit) | ✅ |
| Voice dictation | ❌ removed — the code was finished but no button ever reached it |
| AI-generated destination suggestions | ❌ removed — addresses and prices were invented; replaced by **Explore** |

**Three limitations to know before a demo:**

1. **Accessibility is not done.** The passport map is a bare `Canvas`, so screen readers skip
   it entirely.
2. **Overpass is donated infrastructure with per-IP limits.** A burst of traffic gets refused
   until it recovers.
3. **Most places have no photograph.** OSM holds no images; only well-known places carry a
   Wikipedia article or a Commons photo.

---

## 8. Roadmap

**Near term — finish what exists**

- An unlock moment when a collection tile flips (today it happens silently)
- Turn the collection from a *record* into a *guide*: bring the 61 recognition hints out into
  the open instead of hiding them behind a tap
- Accessibility for the passport map and the explore map — required before any store listing
- A licences screen, to credit OpenStreetMap as ODbL requires
- Tests for the passport UI layer and both map actuals

**Medium term — extend the capability**

- **Gemini Live API** — real-time voice conversation
- **Offline mode** — download one province's data and work without a network
- **Voice dictation** (restore what was removed, this time with a button)
- Read `name:en` from OSM for the ~47% of places that already have an English name
- Marker clustering on the explore map

**Long term — product**

- **AR navigation** — directions and labels drawn over the live camera
- **Family / kids mode** — shorter content, a different narrative voice
- **Community contributions** — travellers propose entries for the culture collection
- Broaden the collection for highland, Cham and Mekong-delta material (currently Kinh-majority)
- Partner with museums and heritage sites for verified content

---

## 9. Getting started

```bash
# Android
./gradlew :app:installDebug

# iOS
open iosApp/iosApp.xcodeproj     # run the iosApp scheme

# Tests (no network, no API key needed)
./gradlew test
```

**The debug build seeds itself.** Open it once and it already holds 24 discoveries from Sa Pa
to Cần Thơ, a conversation and three journal write-ups — enough to look at the passport, the
collection and the journal without going out to photograph anything. The only condition is an
empty journal, so **Settings → clear everything** is the button that puts a demo build back
into a good state.

**`release` and `fastRelease` never seed, and cannot**: the 3.5 MB of demo photographs is
packaged into the `debug` variant only, so a shipping APK has nothing to seed from. Run a
release build to see the real empty state.

You need a Gemini key from [Google AI Studio](https://aistudio.google.com/apikey) in
`local.properties`. Full instructions, including the Android Maps SDK key and how to produce a
release build, are in [TechStack-temp.md §9–§11](TechStack-temp.md#9-getting-started).

---

## 10. AI Riser Vietnam 2026

**Category:** Cultural Tourism & Sports · **AI platform:** Google Gemini, Google AI Studio

> Making every traveller in Vietnam feel like they are exploring with a knowledgeable local
> beside them.

**Data attribution:** province boundaries and place data © OpenStreetMap contributors (ODbL) ·
coastline from Natural Earth (public domain) · article text and readership from Wikipedia
(CC BY-SA) · photographs from Wikimedia Commons under their individual licences.
