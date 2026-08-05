# Phase 01 — A suite for every ViewModel

**Priority:** high · **Status:** complete · **Depends on:** nothing

Four of twelve ViewModels had a test. The other eight had none, and none of the four defects
in phase 02 is reachable without one — they need a repository that *throws* rather than one
that returns `AppResult.Failure`, which is a state no device run produces on demand.

## Context

- [`plan.md`](plan.md) — what the four-device pass established before this phase started
- `LLM.md` §9 — the testing layout and the two rules that silently break Kotlin/Native
- `docs/android-mvi-best-practices.md` §7 — the five categories every suite covers

## What existed

| Had a suite | Had none |
|---|---|
| `LensViewModel`, `ChatViewModel`, `JournalViewModel`, `SettingsViewModel` | `DiscoveryViewModel`, `ExploreViewModel`, `PassportViewModel`, `TranslationViewModel`, `CollectionViewModel`, `SovereigntyViewModel`, `MainViewModel` |

`DesignTokenTest` and `ComposeStabilityReportTest` were the only other `:shared` tests, and
neither executes a ViewModel at all.

## Files created

| File | Subject |
|---|---|
| `commonTest/…/feature/explore/ExploreViewModelTest.kt` | Three concurrent waits, two of which guard re-entry |
| `commonTest/…/feature/discovery/DiscoveryViewModelTest.kt` | The note composer and the photo files it can leak |
| `commonTest/…/feature/translate/TranslationViewModelTest.kt` | A request started in `init`, its timeout and its cancellation |
| `commonTest/…/feature/passport/PassportViewModelTest.kt` | The province sheet, and a backfill nobody awaits |
| `commonTest/…/feature/collection/CollectionViewModelTest.kt` | An empty catalogue is not a loading one |
| `commonTest/…/feature/sovereignty/SovereigntyViewModelTest.kt` | A figure that will not load must not cost the statement |
| `commonTest/…/MainViewModelTest.kt` | The splash gate and the startup sweep |

## Files modified

`commonTest/…/testing/Fakes.kt` — added `FakePlaceRepository`, `FakeNoteRepository`,
`FakeTranslationRepository`, `FakeCaptureMaintenance`, the `nearbyPlace` and
`translationResult` builders, and `throwOnBackfill` on the province fake. Each carries the
`throwOn…` switch the file exists for.

One existing fake changed behaviour: `FakeDiscoveryRepository.observeByProvince` returned the
whole discovery list for any id, synchronously. It now reads a per-province map and emits
**nothing** for a province absent from it — because Room takes a tick to answer, and the frame
between tapping a province and its photographs arriving is the frame worth asserting. The old
fake made the state the sheet is actually in unobservable.

## Two things that decided the shape of the suites

1. **`commonTest` compiles for Kotlin/Native.** No JVM-only throwable, and no `,` `.` `;` `:`
   or brackets inside a backticked test name — `LLM.md` §9. Both rules were obeyed from the
   first line rather than discovered by a red iOS leg.
2. **`SovereigntyViewModelTest` cannot use `runTest`.** Its subject hands the parse to
   `Dispatchers.Default`, which the test scheduler does not drive, so `advanceTimeBy` returns
   before the read finishes. It uses `runBlocking` with a real `withTimeout` — the only suite
   in the project that does, and its KDoc says why so the next one does not copy it blindly.

## Todo

- [x] Seven new suites, one per untested ViewModel
- [x] Fakes for the four repositories that had none
- [x] Intent fuzzing on Explore and Discovery — the two with more than two concurrent jobs
- [x] Green on the JVM **and** the iOS simulator
- [x] `LLM.md` §9 updated

## Success criteria

- Every ViewModel in `:shared` has a suite. **Met** — twelve of twelve.
- The suites find something. **Met** — five failures on the first run: four product defects
  (phase 02) and one fake that answered too eagerly, described above.
- `:shared` count rises from 41 JVM / 30 iOS. **Met** — 95 / 84, project total 277 → 385.

## Risk assessment

None to the product: this phase adds test sources only. The one risk taken was the fake
behaviour change, which could in principle have masked something in another suite —
`observeByProvince` has exactly one caller, `PassportViewModel`, and one consumer of that
fake behaviour, this phase's own test.
