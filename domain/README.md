# `:domain` — the pure-Kotlin core

> Models, use cases, repository **interfaces**, and the two types every boundary speaks in:
> `AppResult` and `AppError`. Nothing else.
>
> Structure map: [`../LLM.md`](../LLM.md) §2, §10 · How a screen consumes this:
> [`../docs/android-mvi-best-practices.md`](../docs/android-mvi-best-practices.md) ·
> Sibling modules: [`../data/README.md`](../data/README.md) ·
> [`../shared/README.md`](../shared/README.md)

---

## 1. What this module is

The one module in the project that depends on **nothing in this project**, and on almost
nothing outside it: coroutines for `Flow`, and kotlinx-datetime for `Instant`/`LocalDate`.
No Compose, no Android, no Ktor, no Room, no Koin.

That is not tidiness for its own sake. Three concrete things fall out of it:

- **Business rules are testable on any target with no fixture.** `:domain`'s suite needs no
  device, no database, no HTTP mock — 9 tests, identical count on the JVM and on the iOS
  simulator.
- **Nothing here can be coupled to the UI**, because there is no UI type in scope to couple
  to. A rule that starts wanting a `Color` or a `stringResource` is a rule that belongs in
  `:shared`.
- **Both platforms get the same rules by construction.** Everything is in `commonMain`;
  there is deliberately **no `androidMain` and no `iosMain` here**. A rule that needs a
  platform API belongs in `:data`, behind an interface declared here.

```
:shared  ──api──▶  :domain  ◀──api──  :data
```

`:shared` re-exports it to Swift (`api(projects.domain)` plus `export(projects.domain)` on
the framework), so every type in this module is part of the iOS surface. Renaming one is an
API break on two platforms at once.

---

## 2. Layout

```
domain/src/commonMain/kotlin/com/evora/technologies/saola/domain/
├── model/          17 files — the vocabulary the whole app speaks
│   ├── AppSettings.kt        theme, narration, location opt-in; GeminiModel + CONFIGURED
│   ├── Discovery.kt          a recognised photo and its sections
│   ├── DiscoveryCategory.kt  the six categories the recogniser and the board share
│   ├── DiscoveryNote.kt      the traveller's own note and kept photos
│   ├── DiscoveryReport.kt    "this is wrong" feedback on a recognition
│   ├── ChatMessage.kt        one conversation turn
│   ├── Journal.kt            days, day summaries, journal stats
│   ├── Catalog.kt            the 61 culture-collection entries, resolved to one language
│   ├── Province.kt           a province outline and its rings
│   ├── TravelPassport.kt     the per-province roll-up the map draws
│   ├── NearbyPlace.kt        an Explore result, its prominence and both its names
│   ├── TranslationResult.kt  OCR + translation, with per-block boxes
│   ├── TextBox.kt            one recognised block and where it sits on the photo
│   ├── TranslateLanguage.kt  the eight languages, their codes and BCP-47 tags
│   ├── CaptureImage.kt       a capture on its way to the recogniser
│   ├── GeoPoint.kt           lat/lon
│   └── DateNames.kt          hand-assembled month/day names — see §6
├── repository/     15 files — 14 interfaces + the `fun interface DemoDataSeeder`,
│                   the ports :data implements
├── usecase/        10 files — one business rule per class
└── util/
    ├── AppResult.kt          Success / Failure, plus map / flatMap / onSuccess / onFailure
    ├── AppError.kt           every failure the app can surface, in domain terms
    ├── MapsUrls.kt           the directions hand-off URL
    └── UserAgent.kt          how the app names itself to Overpass and Wikimedia
```

Tests live in `domain/src/commonTest/` and run on every target.

---

## 3. The rules

### 3.1 Dependencies

**No Compose, no Android, no Apple, no Ktor, no Room, no Koin in this module.** The
`build.gradle.kts` allows exactly `kotlinx-coroutines-core` and `kotlinx-datetime`, and the
datetime dependency is `api` rather than `implementation` on purpose: domain models carry
`Instant` and `LocalDate` in their public signatures, so every consumer needs the same types.

If a rule needs a platform API — a locale, a file, a GPS fix — declare an interface in
`repository/` and let `:data` implement it. `LocationRepository` and `TextRecognizer` are the
worked examples: neither is a "repository" in the storage sense, both are ports.

### 3.2 Models are immutable, and something depends on that

Every model is a `data class` with `val`s only, an enum, or a sealed interface of those. No
mutable collection is ever handed out, and no list on a model is edited in place — they are
replaced wholesale.

This is a **load-bearing** contract, not a style preference. `:domain` is compiled without
the Compose plugin, so the compiler cannot infer stability from its bytecode; instead
[`../shared/compose-stability.conf`](../shared/compose-stability.conf) declares
`com.evora.technologies.saola.domain.model.*` and `…domain.util.*` stable outright.
`ComposeStabilityReportTest` re-checks the resulting report on every build.

> **A `var`, or a `MutableList` field, on any type in `model/` or `util/` breaks that
> declaration.** The build fails if the report disagrees — and if it did not, the cost would
> be a screen that can never skip a recomposition.

Adding a **new package** under `domain/` means adding it to `compose-stability.conf` in the
same change; the two wildcards above only cover `model` and `util`.

### 3.3 Every boundary returns `AppResult`, and nothing throws for an expected condition

```kotlin
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
```

An unreachable network, a throttled model, an unreadable photo — all of those are outcomes
the UI has to render, so they travel as `Failure`, never as an exception. A repository that
throws for one of them has moved the decision to a `try`/`catch` somebody has to remember to
write.

`AppError` holds **no user-facing string.** It says `RateLimited(retryAfterSeconds)` or
`AllModelsBusy(triedModels)`; turning that into words is the presentation layer's job
(`core/util/ErrorMessages.kt`), which is what lets the same error read in Vietnamese on one
phone and Japanese on the next. A `detail: String?` on `Api`, `Malformed`, `Storage` and
`Unexpected` is for logs and bug reports, not for a snackbar.

Exceptions are still allowed for genuinely unexpected states — that is what
`AppError.Unexpected` and `MviViewModel.launchSafely` exist to catch.

### 3.4 A use case is one class, one rule, one `operator fun invoke`

```kotlin
class ObserveDiscoveryUseCase(private val repository: DiscoveryRepository) {
    operator fun invoke(id: String): Flow<Discovery?> = repository.observeDiscovery(id)
}
```

- **`suspend operator fun invoke(...): AppResult<T>`** for a one-shot action;
  **`operator fun invoke(...): Flow<T>`** for a subscription. Nothing else is public on it.
- **Constructor injection of repository interfaces only.** No Koin lookup inside a use case,
  no concrete `…Impl` type.
- **Registered in one line** in `useCaseModule` in
  [`../shared/src/commonMain/kotlin/com/evora/technologies/saola/di/SharedModules.kt`](../shared/src/commonMain/kotlin/com/evora/technologies/saola/di/SharedModules.kt)
  as `factory { XUseCase(get()) }`. Thirty of them today, and thirty `factory` lines — the
  two numbers must agree, and a use case with no binding fails at the first screen that asks
  for it rather than at build time.
- **A pass-through use case is still worth its file.** `ObserveDiscoveriesUseCase` adds
  nothing to the repository call it wraps, and it stays: it is the seam a fake slots into in
  a ViewModel test, and the day the rule stops being a pass-through nothing above it changes.

**A use case is where a decision that spans two repositories goes.** `RecognizeImageUseCase`
is the reason the rule is written down: it asks `LocationRepository` whether the permission
was granted before asking `DiscoveryRepository` to recognise, and it treats a failed fix as
non-fatal. Put that in the repository and storage has to know about permissions; put it in
the ViewModel and the next screen to call `recognize` forgets it. That is exactly how the
passport once shipped never filling in.

**Grouped by feature, not one file per class.** `DiscoveryUseCases.kt` holds the six
discovery rules; `ChatUseCases.kt` holds three. They are a handful of lines each and the
200-line file limit is nowhere near.

### 3.5 A repository interface is a port, and it names an outcome, not a mechanism

`DiscoveryRepository.recognize(imagePath, mode, location)` — not `callGemini(...)`. The
interface may not mention Room, Ktor, Gemini, Overpass, a DTO, a Cursor or a `HttpResponse`;
if it does, `:data` has stopped being replaceable and the fake in `commonTest/testing/Fakes.kt`
has to imitate a wire format to compile.

Fourteen of them today, and four are ports rather than storage: `CaptureStore` (where photos
live), `LocationRepository` (a GPS fix), `TextRecognizer` (on-device OCR) and
`CaptureMaintenance` (the orphan sweep). `DemoDataSeeder` is a fifth, a `fun interface`, and
it exists for a rule stated in [`../LLM.md`](../LLM.md) §10: something only a
development build may do gets an interface here and two implementations in `:data`, chosen by
`seedModule(isDebug)` — never an `if (BuildConfig.DEBUG)` inside the implementation, because
`:data` and `:shared` are single-variant and cannot see it.

---

## 4. Where a new file goes

| I am adding… | It goes in | And also |
|---|---|---|
| A data model | `model/` | Immutable `data class`. Covered by the `model.*` wildcard in `compose-stability.conf` |
| A business rule | `usecase/XUseCases.kt` | One `factory { }` line in `useCaseModule` |
| A new outbound capability | `repository/XRepository.kt` | The implementation goes in `:data/repository/`, bound in `RepositoryModule.kt` |
| A new failure the UI must distinguish | `util/AppError.kt` | Give it a branch in `ErrorMessages.kt` in `:shared`, or it renders as the generic message |
| A constant two modules both send | `util/` | `MapsUrls.kt` and `UserAgent.kt` are here for that reason — `:data` and `:shared` can both see `:domain`, and neither can see the other |
| Anything needing a platform API | **not here** | An interface here, the actual in `:data` |
| A new package under `domain/` | anywhere | Add it to `compose-stability.conf` in the same change |

---

## 5. Testing

`domain/src/commonTest/` — 9 tests, and the count is the same on the JVM and on the iOS
simulator because there is nothing platform-flavoured to skip.

- `RecognizeImageUseCaseTest` — the location decision, including the non-fatal failed fix.
- `GeminiModelTest` — pins `GeminiModel.CONFIGURED` and asserts the eight
  `TranslateLanguage` entries carry the codes and BCP-47 tags the rest of the app assumes.

Two conventions carried from [`../LLM.md`](../LLM.md) §9 apply here too, and both are about
Kotlin/Native rather than about the JVM:

- **No JVM-only type in a test.** `SecurityException` has no `kotlin.` twin; declare a
  `RuntimeException` subclass instead.
- **A backticked test name may not contain `,` `.` `;` `:` or brackets.** Kotlin/Native
  rejects the identifier outright; the JVM accepts all of them, so this compiles green
  locally and fails the iOS leg.

Run `./gradlew :domain:allTests`, not just the host-test task.

---

## 6. Two things that look wrong and are not

**`DateNames.kt` hand-assembles month and day names in eight languages.** The obvious fix is
a formatter — and there is no multiplatform one: `DateTimeFormatter` is JVM-only and
`NSDateFormatter` is Apple-only. The journal heading has to name the same month as the Gemini
prompt that writes that day's story, so both read this table. Thai years are Common Era here,
not Buddhist Era, deliberately: a Buddhist year would disagree with every other date the app
shows.

**`GeminiModel.CONFIGURED` is a constant in `AppSettings.kt`, not a setting.** The model
picker was removed on 06.08.2026 and its DataStore key dropped rather than left readable — a
stored value that nothing writes can only override a build decision with whatever an older
install happened to be left on. `fallbackChain` still walks the other entries when the
configured model answers `503`. See [`../docs/tech-stack.md`](../docs/tech-stack.md) §2, §4.
