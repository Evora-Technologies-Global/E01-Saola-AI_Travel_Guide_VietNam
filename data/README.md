# `:data` — the implementation layer

> Every repository implementation, Room, DataStore, Ktor, the Gemini and OpenStreetMap
> clients, on-device OCR, and everything platform-specific that is not UI.
>
> Structure map: [`../LLM.md`](../LLM.md) §2, §6, §10 · What it implements:
> [`../domain/README.md`](../domain/README.md) · What consumes it:
> [`../shared/README.md`](../shared/README.md) · Why each choice:
> [`../docs/tech-stack.md`](../docs/tech-stack.md) §3, §4, §5, §7

---

## 1. What this module is

`:data` implements the interfaces `:domain` declares, and it is the **only** module allowed
to know how any of it actually happens — that there is a Room database, that recognition is
an HTTPS call to Google AI Studio, that a place list comes from Overpass, that a photograph
is a JPEG in `files/captures`.

```
:shared  ──implementation──▶  :data  ──api──▶  :domain
```

Two consequences worth stating, because both have been tested by a change:

- **`:shared` depends on `:data` for one reason only: to install its Koin modules.** No
  screen, ViewModel or use case names a `…Impl`, a DTO, a DAO or an entity. `appModules()`
  calls `dataModules(isDebug)` and nothing else crosses.
- **`:data` is where `expect`/`actual` lives for anything that is not UI.** The camera and the
  narration engine are `:shared`'s, because they are attached to a screen; the file store, the
  locale, the IO dispatcher, the HTTP engine and OCR are this module's.

---

## 2. Layout

```
data/src/
├── commonMain/kotlin/…/data/
│   ├── di/                 The whole graph below the use cases — see §4
│   │   ├── DataModules.kt      dataModules(isDebug) — the one entry point
│   │   ├── CoreModule.kt       Json, dispatchers, shared primitives
│   │   ├── NetworkModule.kt    HttpClient; installs Ktor Logging only when isDebug
│   │   ├── DatabaseModule.kt   SaolaDatabase + the six DAOs
│   │   ├── RepositoryModule.kt 11 single<XRepository> bindings, ApiKeyProvider, and the
│   │   │                       Gemini / Overpass / Wikipedia clients under them
│   │   ├── SeedModule.kt       BundledDemoDataSeeder or a no-op, by isDebug
│   │   └── PlatformModule.kt   expect val platformDataModule
│   ├── repository/         11 …Impl classes + StorageGuards
│   ├── local/
│   │   ├── db/             SaolaDatabase, Converters, dao/Daos.kt, entity/Entities.kt,
│   │   │                   entity/StoredJson.kt
│   │   ├── datastore/      SettingsDataStore + its factory
│   │   ├── asset/          BundledAssets (expect) + the province and catalog sources
│   │   └── file/           ImagePolicy — EXIF correction, 1024 px, JPEG
│   ├── remote/
│   │   ├── gemini/         GeminiClient, RemoteDataSource, Prompts, Schemas, Guardrails,
│   │   │                   ApiKeyProvider, dto/
│   │   ├── openmap/        OverpassClient, WikipediaClient, dto/
│   │   └── HttpClientEngine.kt (expect)
│   ├── mapper/             entity ⇄ domain, dto ⇄ domain. Three files by subject
│   ├── geo/                ProvinceGeometry — point-in-polygon + nearest-within-25 km
│   ├── catalog/            CatalogMatcher — discovery ⇄ catalogue entry, §6
│   ├── platform/           DeviceLanguage, IoDispatcher (expect), LocationPolicy
│   ├── seed/               BundledDemoDataSeeder, DemoContent, DemoContentMapper
│   └── util/Log.kt         Kermit, one tag
├── androidMain/
│   ├── assets/             provinces.json (189 KB) · catalog.json — canonical copies
│   └── kotlin/…            CameraX-free actuals: AndroidCaptureStore, MlKitTextRecognizer,
│                           FusedLocationRepository, OkHttp engine, AndroidBundledAssets
├── iosMain/kotlin/…        IosCaptureStore, VisionTextRecognizer, CoreLocationRepository,
│                           Darwin engine, IosBundledAssets, IosPaths
├── commonTest/             9 suites — wire formats, mappers, guardrails, hostile input
└── androidHostTest/        3 suites that need java.io.File (geometry, matcher, memory)
```

`data/schemas/` holds Room's exported schema JSON and is git-ignored — see §3.6.
`data/consumer-rules.pro` is **inert** — a
leftover from when this was a `com.android.library`; see
[`../docs/tech-stack.md`](../docs/tech-stack.md) §11.

---

## 3. The rules

### 3.1 Nothing in this module is visible above it

A repository implementation is bound as `single<XRepository> { XRepositoryImpl(...) }` and is
reached only through the `:domain` interface. **An entity, a DAO, a DTO or a Ktor type may
never appear in a signature `:shared` can see.** Mapping happens here, in `mapper/`, on the
way in and on the way out.

The `…Impl` classes themselves are public only because Koin needs to construct them; nothing
outside this module imports one, and `AppGraphTest` in `:app` is what would notice if one
started leaking through a binding.

### 3.2 There is no `DEBUG` constant here, and there cannot be

`:data` and `:shared` are built by `com.android.kotlin.multiplatform.library`, which produces
a **single** Android variant — `:app`'s debug and release builds link the very same class
files. So nothing in this module can ask which build type it ended up in.

It used to try. A generated `DEBUG` constant keyed off `-Psaola.debug` defaulted to `true`,
which meant a plain `:app:assembleRelease` shipped with Ktor's `Logging` plugin installed.

> **The flag arrives from the entry point instead** — `BuildConfig.DEBUG` on Android, Swift's
> `#if DEBUG` on iOS — and is threaded down as `dataModules(isDebug)`.

Two modules consume it, and both express it as a **binding** rather than an `if` inside an
implementation: `networkModule(isDebug)` installs the `Logging` plugin or does not, and
`seedModule(isDebug)` binds `BundledDemoDataSeeder` or a no-op. A binding means a release
build has no reference to the code at all, so R8 removes Ktor's whole logging path rather than
leaving it dormant.

**The module count is the same for both build types**, and `AppGraphTest` pins that: a module
that vanished on one would resolve on the other and fail on a device.

### 3.3 `DataBuildConfig` is generated, holds one field, and is `internal`

`BuildConfig` is Android-only and this module is not. The `generateBuildConfig` Gradle task
writes a `DataBuildConfig` object into `commonMain` carrying exactly one value — the Gemini
key read from the git-ignored `local.properties`. It is the same for every build type.

Do not add a second field to it without asking whether the value genuinely differs per
*build* rather than per *build type*; if it is the latter, §3.2 applies.

### 3.4 Repositories return `AppResult` and never throw for an expected condition

Offline, throttled, unreadable photo, no GPS fix — all `AppResult.Failure(AppError.…)`. The
contract is `:domain`'s and the argument is in
[`../domain/README.md`](../domain/README.md) §3.3.

`StorageGuards.kt` is where the local-storage half of that lives: a Room or DataStore call
that throws is folded into `AppError.Storage`, so a corrupt preferences file surfaces as a
message rather than as a crash on the splash screen. `RepositoryFailureTest` drives it.

### 3.5 Platform differences take one of two forms, and never a runtime branch

**Never `if (Platform.isAndroid)`.** A branch compiles on both platforms and so is proved on
neither. There are exactly two legitimate shapes:

| Shape | Use when | Examples |
|---|---|---|
| `expect`/`actual` declaration | the *thing itself* has no common form | `httpClientEngine()`, `platformIoDispatcher()`, `deviceLanguage()`, `SaolaDatabaseConstructor`, `platformDataModule` |
| An interface here, one implementation per source set, **bound by `platformDataModule`** | there is a common contract and only the mechanism differs | `CaptureStore` → `AndroidCaptureStore` / `IosCaptureStore`; `TextRecognizer` → ML Kit / Vision; `LocationRepository` → FusedLocation / CoreLocation; `BundledAssets` |

The second is the default, and it is what keeps the count of `expect` declarations at five.
Prefer it: an interface can be faked in `commonTest`, and an `actual` cannot.

`deviceLanguage()` is the one to read first if you are adding an `expect`: it is the platform
locale API on each side, and it is one of the three places that answer "which language"
([`../docs/tech-stack.md`](../docs/tech-stack.md) §6).

### 3.6 Room: schema 1, destructive fallback, and what that costs you

`fallbackToDestructiveMigration(dropAllTables = true)`, no migrations. Acceptable **only**
because nothing is published: any version Room has no migration for — in either direction —
drops every table and recreates them empty, silently.

- **Six tables**, six DAOs: `discoveries`, `chat_messages`, `discovery_notes`,
  `discovery_reports`, `translations`, `trip_summaries`. Details and indices in
  [`../docs/tech-stack.md`](../docs/tech-stack.md) §3.
- **The version stays at 1 while the app is unpublished. Change the schema freely; leave the
  number alone.** It had reached 3 and every bump was destructive anyway — each existed only
  to *trigger* the fallback, never to carry data across, and counting up while destroying
  everything at each step describes a migration history that does not exist.
- **What that rule cannot do:** Room hashes the schema into the file and compares it on open,
  so a *same-version* change is an integrity failure it **throws** on rather than a fallback it
  runs. A device holding an older `saola.db` needs an uninstall or a clear-app-data, not a
  recovery path. On a debug build the demo trip re-seeds on the next launch, so the cost is one
  relaunch.
- `exportSchema = true` writes `data/schemas/…/1.json`, which is **git-ignored** — a local
  build artefact, not a record a reviewer reads in a diff. Delete the directory and rebuild if
  it ever disagrees with the entities.
- **List-shaped fields are JSON strings** through `Converters`, and decoding is **lenient** on
  purpose: a schema tweak degrades an old row to an empty list rather than crashing the
  journal.
- **The database stores a file *name*, never a path.** `imageName` and `photoNamesJson` are
  bare names resolved late through `CaptureStore.resolve`. On iOS the container is re-homed
  under a fresh UUID on every reinstall, so a stored path names a directory that no longer
  exists — this cost the app every photograph once, by *deleting* them: the orphan sweep found
  no overlap between the new directory and the old paths and swept the lot.
- The moment this app is published, **this rule inverts**: schema changes need real migrations
  and this paragraph has to be rewritten in the same commit.

### 3.7 Every Gemini call is bound to a schema, and walks the fallback chain

`GeminiSchemas.kt` binds each request to a JSON Schema server-side, so parsing is a plain
deserialisation — no regex hunting for JSON inside prose, and a declared field is guaranteed
present. A new AI-backed field means editing the schema and the DTO together; a DTO field the
schema does not declare will simply never arrive.

`GeminiClient` walks `GeminiModel.fallbackChain` when a model answers `503 high demand`, and
stops the chain immediately on a non-retryable failure (bad key, malformed request) rather
than burning quota. `GeminiGuardrails.kt` is what keeps a hostile or off-topic response from
reaching the journal; `HostileInputTest` and `GeminiGuardrailsTest` drive it.

### 3.8 The Explore cache is in memory, deliberately

Every other repository here is Room-backed because the traveller's own history has to survive
a cold launch. `PlaceRepositoryImpl` holds somebody else's data about wherever the phone
happened to be, and restoring it from disk would open the screen on last week's city. A Room
table would also have meant a schema migration — and §3.6's destructive fallback would have
dropped the whole journal to cache a list of cafés.

Window: 15 minutes and 300 metres. The details cache is keyed on the place **and the
language**; keyed on the id alone it served the Vietnamese article back after a switch to
English.

### 3.9 Assets have one canonical copy, in `androidMain/assets`

`provinces.json` and `catalog.json` live there because that is the one location Android
packages with no extra wiring. `copySharedAssetsToIos` (a `Sync` task at the bottom of
`build.gradle.kts`) mirrors the directory into `iosApp/SharedAssets`, which the Xcode project
references as a folder. One copy in version control means the 34 outlines can never drift
between platforms.

Two build-file details that look like boilerplate and are not:

- `androidResources { enable = true }` — off by default in the multiplatform library plugin.
  With it off, `provinces.json` is simply not packaged and province lookup fails at runtime.
- The directory is named `SharedAssets`, not `Resources`. A directory literally called
  `Resources` at the root of an iOS `.app` makes CFBundle fall back to the legacy bundle
  layout and the app fails to install with "Missing bundle ID".

> **Do not regenerate `provinces.json` from GADM or Natural Earth admin levels.** Both still
> carry the pre-2025 63 provinces; the file must contain exactly 34. The generator is
> `tools/provinces/build_provinces.py`, and `ProvinceGeometryTest` fails the build if a
> pre-merger source is used.

---

## 4. Dependency injection

```kotlin
fun dataModules(isDebug: Boolean): List<Module> = listOf(
    coreModule,
    networkModule(isDebug),
    databaseModule,
    repositoryModule,
    seedModule(isDebug),
    platformDataModule,
)
```

Six modules, one entry point, called only from `appModules(isDebug)` in `:shared`. No
annotation processor — Hilt and Dagger do not run for Kotlin/Native — so bindings are listed
by hand and a missing one is a grep away.

- **Repositories are `single { }`**, ViewModels are `viewModel { }` (that half lives in
  `:shared`). A repository holds caches and open Flows; two instances would mean two caches.
- **Bind the interface, not the class**: `single<DiscoveryRepository> { DiscoveryRepositoryImpl(…) }`.
- **Android needs `androidContext(...)` at `startKoin`**, because `platformDataModule.android`
  resolves the application `Context` through it. iOS needs nothing.
- Adding a repository is **one interface** in `:domain`, **one class** here, **one line** in
  `RepositoryModule.kt`.

---

## 5. Where a new file goes

| I am adding… | It goes in | And also |
|---|---|---|
| A repository implementation | `repository/XRepositoryImpl.kt` | Interface in `:domain/repository/`, one `single<X>` line in `RepositoryModule.kt` |
| A Room table | `local/db/entity/Entities.kt` + a DAO in `dao/Daos.kt` | Register the entity **and** its DAO accessor on `SaolaDatabase`. **Do not bump the version** — §3.6. Uninstall on any device holding the old file |
| A converter for a list field | `local/db/Converters.kt` | Keep decoding lenient |
| A remote call | `remote/<service>/XClient.kt` + `dto/` | DTOs stay in this module; map in `mapper/` |
| A Gemini field | `remote/gemini/GeminiSchemas.kt` **and** the DTO | A DTO field the schema does not declare never arrives |
| A platform capability | `expect` in `commonMain/platform/`, `actual` in both | Bind it in `PlatformModule.<platform>.kt` |
| Something only a development build may do | An interface in `:domain`, two impls here | Chosen by `xModule(isDebug)`. Never `if (BuildConfig.DEBUG)` — §3.2 |
| A bundled asset | `androidMain/assets/` | `copySharedAssetsToIos` mirrors it; nothing else to wire |

---

## 6. Testing

**126 on the JVM, 92 on the iOS simulator.** The difference is `androidHostTest`, which reads
files off disk and so cannot be `commonTest`.

`commonTest/` — wire formats and rules, all with `Ktor MockEngine`, no network:

| Suite | Holds |
|---|---|
| `GeminiClientTest` | request shape, the fallback chain, non-retryable stops |
| `GeminiGuardrailsTest` | what may reach the journal |
| `HostileInputTest` | malformed, oversized and adversarial payloads |
| `OpenMapClientTest` | the Overpass query shape, the `remark` field, the junk filter |
| `PlaceNamingTest` | `name:<lang>` → `name:en` → local, and the two comparisons that must keep reading `mappedName` |
| `DiscoveryMapperTest` | entity ⇄ domain, both directions |
| `RepositoryFailureTest` | `StorageGuards` folding a throw into `AppError.Storage` |
| `DeviceLanguageTest`, `DemoContentMapperTest` | the locale read; the seed's clamped timestamps and NULL `provinceId` |

`androidHostTest/` — needs `java.io.File`:

- `ProvinceGeometryTest` runs against the **shipped asset** and asserts ~63 real coordinates,
  including every pre-2025 provincial capital resolving to the province that absorbed it.
- `CatalogMatcherTest` pins the diacritic rule — folding tone marks makes `phở` and `phố` the
  same string, so a photograph of Phố cổ Hội An would collect a bowl of noodles — and the
  whole-word rule that stops `chè` matching `chèo`.
- `MemoryBudgetTest`.

Run `./gradlew :data:allTests`, not `:data:testAndroidHostTest`. Two things compile green on
the JVM and fail the Kotlin/Native leg: a JVM-only type in a test (`SecurityException` has no
`kotlin.` twin), and a backticked test name containing `,` `.` `;` `:` or brackets.

**These counts are read off `build/test-results`, not remembered.** A count nobody re-measures
drifts into a number that looks authoritative and is not.

---

## 7. Known gaps

- **`ProvinceRepositoryImpl` has no test.** `ProvinceGeometryTest` covers the geometry below it
  and `PassportViewModelTest` the state above it; the repository between them is unproven.
- **Four reads are held open by tests alone.** `DiscoveryRepository.observeFavorites` and
  `TranslationRepository.observeTranslations` / `observeTranslation` / `delete` lost their last
  screen when the recommendation clean-out removed the use cases in front of them.
- **`consumer-rules.pro` is inert** and kept only so nobody re-adds it. See
  [`../docs/tech-stack.md`](../docs/tech-stack.md) §11.
