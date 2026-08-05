# Stranded flags, and the eight ViewModels nobody was testing

**Created:** 04.08.2026 · **Branch:** `duylt_dev` · **Status:** all three phases complete

Found by writing the missing ViewModel suites during a four-device verification pass —
Android phone (API 37), Android tablet (API 35), iPhone 17 Pro and iPad Pro. Every device
launched clean and every existing test was green; the defects below are invisible from the
outside because they need a repository to *throw* rather than to fail.

---

## What the verification pass established

| Target | Result |
|---|---|
| Android phone, API 37, 411 × 891 dp | Launches clean, phone shell, no FATAL. Cold start 1.5 s |
| Android tablet, API 35, 1280 × 800 dp | Launches clean, rail + two panes, no FATAL. `:shared:connectedAndroidDeviceTest` **12 / 12** |
| iPhone 17 Pro simulator | Builds and launches clean, phone shell |
| iPad Pro 13-inch simulator, 1024 pt portrait | Builds and launches clean, **rail + two panes** |
| iPad Pro 11-inch simulator, 834 pt portrait | Launches clean, **phone shell** — see open question Q1 |
| Unit tests before this plan | 277 green — 161 JVM, 116 iOS simulator |

No crash was reproducible on any of the four devices. The one `F/DEBUG` in logcat belongs to
`com.android.bluetooth`, not to this app.

---

## The defect

**A flag raised before a suspend call, and lowered only on the paths somebody thought of.**

Every one of these ViewModels raises a "busy" flag, calls a repository, and lowers the flag in
each `AppResult` arm. `launchSafely`'s `onError` is the fifth path — the unwrapped throw, the
one the layer below promises never to produce — and at four sites it writes the error without
lowering the flag.

That is not a cosmetic spinner. Three of the four flags **also guard re-entry**, so the screen
refuses the one action that would clear it. The traveller's only way out is to kill the app.

This is not a new class of bug in this codebase. `LLM.md` §11 row #24 is the same defect,
found in the chat on 04.08.2026 and fixed the same day; `LensViewModel:179` and
`JournalViewModel:85` both carry comments explaining the rule in their own words. Four screens
never got the message.

| # | Site | Flag stranded | What the traveller sees |
|---|---|---|---|
| **B1** | `ExploreViewModel.search` | `isLoading` / `isRefreshing` | Whole-screen spinner for ever. `search` returns early while either is up, so **refresh and retry are dead for the life of the process** |
| **B2** | `ExploreViewModel.selectPlace` | `isLoadingDetails` | The place sheet spins for ever. Recoverable by closing the sheet |
| **B3** | `DiscoveryViewModel.SaveNote` | `isSavingNote` | **The worst of the four.** `launchSafely` takes the *default* `onError` — logging alone — so nothing is reported at all, and `SaveNote` returns early while the flag is up. The traveller's own writing sits in a composer whose save button no longer does anything, with no message anywhere on screen |
| **B4** | `TranslationViewModel.translate` | `isLoading` | An error card under a spinner that is still turning. Recoverable — `Retry` cancels and restarts — so this is the mildest |

### Why the tests never caught it

There were none. Four of twelve ViewModels had a suite; the eight below had nothing, and
`DesignTokenTest` / `ComposeStabilityReportTest` do not execute a ViewModel at all.

---

## Phases

| Phase | What | Status |
|---|---|---|
| [01](phase-01-viewmodel-test-coverage.md) | Write the missing ViewModel suites — the diagnostic that found B1–B4 | ✅ Complete |
| [02](phase-02-lower-the-flag-on-the-throw-path.md) | Fix B1–B4, re-run every leg on both platforms | ✅ Complete |
| [03](phase-03-clean-dead-code-and-solid.md) | Dead code, SOLID and Clean-Architecture cleanup found on the way through | ✅ Complete for what was safe; two findings recorded rather than fixed |

## Where it ended

| | Before | After |
|---|---|---|
| Unit tests | 277 | **385** |
| `:shared` JVM / iOS simulator | 41 / 30 | **95 / 84** |
| ViewModels with a suite | 4 of 12 | **12 of 12** |
| Instrumented tests, API 35 tablet | 12 / 12 | **12 / 12** |
| Stranded busy flags | 4 | **0** |

Re-verified at runtime on all four targets after the fixes and again after the refactor.

---

## Open questions

**Q1 — is 840 dp the right threshold?** `WindowClass.ExpandedMinWidth` is 840 dp, and an iPad
Pro 11-inch in portrait is 834 pt. It misses by **six points**, so the most common iPad Pro
draws the phone shell in portrait while the 13-inch draws the tablet one. A Pixel Tablet in
portrait (800 dp) is in the same position. The threshold is documented and deliberate — the
wireframe was drawn at 1194 × 834 — so this is a product decision rather than a defect, and it
is listed here rather than fixed. Lowering it to 820 dp would take in every 11-inch iPad and
the Pixel Tablet; leaving it means "tablet layout" means "landscape, or a 13-inch".

**Q2 — `SovereigntyViewModel` can only be half-tested.** It calls `Res.readBytes` directly, so
there is no seam to fake and no bundle on a test runtime. Its suite therefore pins the failure
path — a map that will not load must still let the statement render — and the success path has
no test on either platform. Recorded as `LLM.md` §11 row #27: the fix is a `SovereigntyMapSource`
port in `:domain` with the implementation in `:data`, matching `ProvinceAssetSource` and
`CatalogAssetSource`.
