# Phase 02 — Lower the flag on the throw path

**Priority:** high · **Status:** in progress · **Depends on:** phase 01

Four ViewModels raise a busy flag before a repository call and lower it in each `AppResult`
arm but not in `launchSafely`'s `onError`. Three of the four flags also guard re-entry, so the
screen refuses the action that would clear it.

## Context

- [`plan.md`](plan.md) — the table of B1–B4 and what each costs
- [`phase-01-viewmodel-test-coverage.md`](phase-01-viewmodel-test-coverage.md) — the suites that
  found them; each failing test is the acceptance criterion for one fix
- `docs/android-mvi-best-practices.md` §7, the "stuck states" row — this defect by name
- `LLM.md` §11 row #24 — the same defect in the chat, fixed 04.08.2026

## The shape to copy

Already in the codebase three times over. `LensViewModel:179` states it plainly:

```kotlin
// `isAnalysing = false` as well as the error: an unexpected throw skips every
// `when` branch below, so this is the only thing that lifts the scrim. Leaving it
// up would trade a crash for a dead screen, which is not obviously the better one.
analysisJob = launchSafely(
    onError = { setState { copy(isAnalysing = false, error = it) } },
) {
```

`ChatViewModel:97` and `JournalViewModel:89` are the same. The fix is to make the four
outliers match — not to invent a mechanism.

## Files to modify

| File | Change |
|---|---|
| `feature/explore/ExploreViewModel.kt` | `search` and `selectPlace` — lower the flags in `onError` |
| `feature/discovery/DiscoveryViewModel.kt` | `SaveNote` — lower `isSavingNote` **and** stop failing silently |
| `feature/translate/TranslationViewModel.kt` | `translate` — lower `isLoading` in `onError` |

Nothing is added and no contract changes. B3 is the only one that needs a decision beyond the
one-line lower: the composer has no error field, so the failure has to go somewhere.

## Implementation steps

1. **B1** — `ExploreViewModel.search`: `onError = { setState { copy(isLoading = false, isRefreshing = false, error = it) } }`.
2. **B2** — `ExploreViewModel.selectPlace`: `onError = { setState { copy(isLoadingDetails = false, error = it) } }`.
3. **B4** — `TranslationViewModel.translate`: `onError = { setState { copy(isLoading = false, error = it) } }`.
4. **B3** — `DiscoveryViewModel.SaveNote`: lower `isSavingNote` in `onError`, and keep
   `noteEditor` open so the traveller's words survive. A note that failed to save must not be
   silently discarded, and the composer staying open with a live save button is the recovery.
5. Re-run `:shared:allTests` — both the JVM and the iOS simulator legs, per `LLM.md` §9.
6. Re-run `:shared:connectedAndroidDeviceTest` on the API 35 tablet.
7. Re-install and launch on all four targets to confirm nothing regressed at runtime.

## Todo

- [x] B1 — Explore search lowers both spinners
- [x] B2 — Explore details lowers its spinner
- [x] B4 — Translation lowers its spinner
- [x] B3 — Discovery note save lowers its flag and keeps the words
- [x] `:shared:allTests` green on JVM and iOS simulator
- [x] `:domain:allTests` and `:data:allTests` still green
- [x] Device suite still 12 / 12 on the tablet
- [x] All four targets re-verified at runtime
- [x] `LLM.md` §11 updated — rows moved to Fixed

## Success criteria

- The five failing tests from phase 01 pass, and no existing test regresses.
- Every `launchSafely` in the project that is preceded by a raised flag lowers it in `onError`.
- A retry after a throw reaches the repository — asserted, not assumed. This is the half that
  matters: lowering a flag nothing reads would be cosmetic, and the tests call the repository
  a second time and count.

## Risk assessment

Low. Each change is inside an existing lambda, adds no branch and no new state field. The
risk of *not* doing it is a screen the traveller can only escape by killing the app.

One thing to be careful of: `error` on `ExploreState` is rendered as a whole-screen card when
the map is empty, so lowering `isLoading` without setting `error` would show an empty map with
no explanation. Both are written together.
