# LLM.md — Project Code Structure

> **Read this file BEFORE writing any code. Update it AFTER any structural change.**
> This is the map of the codebase: where things live, what each layer may depend on, and
> where a new file belongs. It is not a tutorial — for *how* to write an MVI screen, read
> [`docs/android-mvi-best-practices.md`](docs/android-mvi-best-practices.md).

**Reference implementation:** `VietLensAI` — Kotlin Multiplatform + Compose Multiplatform,
Android + iOS from one presentation layer. The structure below is the standard for this
project and for any mobile app project this `.claude/` config is copied into.

---

## 1. Rules of use

| When | Do |
|---|---|
| Before writing code | Read §2–§9 of this file, then read the relevant section of `docs/android-mvi-best-practices.md`. |
| Before adding a file | Check §10 "Where does a new file go" — do not invent a new package. |
| After adding/moving/deleting a package, module, layer or architectural rule | Update this file in the same change. |
| After fixing something in §11 | Move the row from "Open" to "Fixed" with the commit reference. |
| Never | Copy a pattern from a file listed in §11 as a known deviation. |

---

## 2. Module graph

```
:app        Android host. Thin: Application class, one Activity, theme, manifest.
            Knows nothing about screens.
              ↓
:shared     THE WHOLE PRESENTATION LAYER. Every screen, every ViewModel, the
            navigation graph, the design system. Compose Multiplatform.
            Produces `VietLensShared.framework` (static) for iOS.
              ↓
:domain     Models, use cases, repository *interfaces*, AppResult/AppError.
            Pure Kotlin. NO Compose, NO Android, NO Ktor, NO Room.
              ↑
:data       Repository *implementations*, Room, DataStore, Ktor, platform storage.
            Implements :domain interfaces. :shared depends on it only to install
            its Koin modules.

iosApp      Xcode project. Links the framework, calls `MainViewController`.
```

**Dependency direction is one-way and enforced by Gradle.** `:shared` declares
`api(projects.domain)` (re-exported to Swift) and `implementation(projects.data)`.
`:domain` depends on nothing in this project.

**Why `:domain` and `:data` are Compose-free:** it keeps the layering honest, and it is
also why `shared/compose-stability.conf` exists — see §8.

---

## 3. Package layout inside `:shared`

Root package: `com.duylt.trave.vietlensai`

```
src/
├── commonMain/kotlin/com/duylt/trave/vietlensai/
│   ├── MainViewModel.kt              App-level state: theme, splash gate, startup sweep.
│   │                                 The ONE plain ViewModel — see the exemption below
│   ├── core/
│   │   ├── mvi/MviViewModel.kt       THE MVI CONTRACT — read this first
│   │   ├── mvi/CollectEffects.kt     The one effect collector every XRoute uses
│   │   ├── designsystem/
│   │   │   ├── theme/                Color, Type (+ StampType, VietLensShapes), Dimens
│   │   │   │                         (Spacing, PageSpacing), Motion, Insets, Theme
│   │   │   └── component/            PageHeader, OverlayHeader, Components, AppSnackbar,
│   │   │                             AppAsyncImage, PermissionSheet, SovereigntyBanner,
│   │   │                             SystemBars
│   │   └── util/                     Log, Formatters, Permissions, DateTimeFormat,
│   │                                 ErrorMessages, VolumeShutterBus, DetectTimeout
│   ├── feature/<name>/               ONE PACKAGE PER SCREEN — see §5
│   ├── navigation/
│   │   ├── Destinations.kt           Routes object + TopLevelDestination enum
│   │   └── VietLensApp.kt            NavHost, scaffold, bottom bar
│   ├── di/SharedModules.kt           useCaseModule + presentationModule + appModules()
│   ├── platform/                     expect: Platform, PlatformActions
│   └── voice/                        expect: SpeechRecognizer, TextToSpeech
│
├── commonMain/composeResources/
│   ├── values/strings.xml            Default (en)
│   ├── values-vi/strings.xml         Vietnamese
│   └── files/                        Raw assets read via Res.readBytes
│
├── androidMain/kotlin/…              actual: CameraX, Android permissions, TTS/STT,
│                                     PlaceMap (Maps SDK), PlatformUiModule
├── iosMain/kotlin/…                  actual: AVFoundation, MapKit, AVSpeech,
│                                     MainViewController (the framework entry point)
├── commonTest/                       ViewModel tests + testing/Fakes.kt
├── androidHostTest/                  ComposeStabilityReportTest + DesignTokenTest
│                                     (both need java.io.File)
└── androidDeviceTest/                Gesture/recomposition tests (need a real device)
```

**Rule:** anything platform-specific is an `expect` in `commonMain` with an `actual` in
both `androidMain` and `iosMain`. Never `if (Platform.isAndroid)` in shared code.

**The `MainViewModel` exemption, stated so it is not filed as an oversight.** `MviViewModel`
is for **screens** — anything with a route and a back-stack entry. `MainViewModel` is the
**window host**: no route, owns theme and the splash gate for the whole window, read by both
the Android Activity and `MainViewController` on iOS before any screen exists. An intent
channel with no sender and an effect channel with no collector would be ceremony. It is the
only plain `ViewModel` in the project and the only one allowed to be; every one of the ten
feature ViewModels extends `MviViewModel`.

---

## 4. The MVI core — `core/mvi/MviViewModel.kt`

Every screen ViewModel extends it. Four things it gives you, and nothing else:

```kotlin
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(initialState: S) : ViewModel() {
    val state: StateFlow<S>                              // the only way UI reads
    val effects: Flow<E>                                 // Channel(BUFFERED).receiveAsFlow()
    protected val currentState: S
    abstract fun onIntent(intent: I)                     // the only way UI writes
    protected fun setState(reducer: S.() -> S)
    protected fun sendEffect(effect: E)
    protected fun launchSafely(onError: (AppError) -> Unit = {}, block: suspend CoroutineScope.() -> Unit): Job
}
```

Three non-negotiables encoded here:

1. **Effects go through a `Channel`, not a `SharedFlow`.** A replaying hot flow either
   drops a navigation event (no subscriber) or fires it twice (config change). The channel
   buffers while the screen is backgrounded and delivers exactly once when it returns.
2. **`launchSafely` is the floor, not a licence.** The layer below already returns
   `AppResult` and folds failures into `AppError`. This catches the gap — an unwrapped Room
   read, a corrupt DataStore file. `CancellationException` is rethrown deliberately.
3. **`onIntent` is the single write entry point.** No other public method on a ViewModel.

---

## 5. Anatomy of a feature package

Every `feature/<name>/` has exactly these files:

```
feature/lens/
├── LensContract.kt     State + Intent + Effect. NOTHING ELSE. No logic, no VM.
├── LensViewModel.kt    Extends MviViewModel<LensState, LensIntent, LensEffect>
└── LensScreen.kt       LensRoute (stateful) + LensScreen (stateless) + private children
```

Optional, only when it earns its own file:
```
├── LensComponents.kt   Composables >150 lines or shared between two screens in the feature
├── CameraController.kt Platform-facing helper the screen owns (not the VM)
└── CameraOptions.kt    Feature-local enums/value types that are NOT domain concepts
```

**`XContract.kt` is mandatory, even when the effect set is empty** — all ten features have
one. Contract files are the first thing anyone reads to understand a screen; burying
`LensState` at line 200 of a ViewModel hides it. An empty effect set is still declared:

```kotlin
/** Nothing to emit — every action on this screen is navigation the route already owns. */
sealed interface CollectionEffect : UiEffect
```

**`Route` vs `Screen` — the split is mandatory:**

| | `XRoute` | `XScreen` |
|---|---|---|
| Visibility | `fun` (public) | `private fun` |
| Takes | navigation lambdas + `viewModel: XViewModel = koinViewModel()` | `state: XState`, `onIntent: (XIntent) -> Unit`, nav lambdas |
| Does | collects state, collects effects, owns lifecycle observers and platform controllers | renders — and nothing else |
| Testable in preview | no | yes |

Only `XRoute` may touch the ViewModel. `XScreen` and everything below it receive
`state` + `onIntent` and are pure.

---

## 6. Dependency injection — `di/SharedModules.kt`

One file, the whole graph above the repositories. No annotation processor (Hilt/Dagger do
not run for Kotlin/Native), so bindings are listed by hand — a missing one is a grep away.

```kotlin
val useCaseModule: Module      // factory { XUseCase(get()) } — one line each
val presentationModule: Module // viewModel { XViewModel(...) } + process-wide singletons
internal expect val platformUiModule: Module   // actual per platform

fun appModules(isDebug: Boolean): List<Module> =
    dataModules(isDebug) + useCaseModule + presentationModule + platformUiModule
```

Rules:
- ViewModels are `viewModel { }`, never `single { }` — one per `ViewModelStoreOwner`
  (the navigation back-stack entry), cleared with it.
- A ViewModel that reads route arguments takes `savedStateHandle: SavedStateHandle` and is
  registered as `viewModel { params -> XViewModel(savedStateHandle = params.get(), …) }`.
- Both entry points (`VietLensApplication` on Android, `startVietLens` on iOS) pass exactly
  `appModules(isDebug)`, so neither platform can get a graph the other does not have.
- Adding a ViewModel means adding **one** `viewModel { }` block here. Adding a use case
  means **one** `factory { }` line.

---

## 7. Navigation — `navigation/Destinations.kt` + `VietLensApp.kt`

- String routes with explicit builder functions, not type-safe serializable routes: the
  graph is small and a plain `String` keeps deep-linking trivial.
- Argument names are `const val ARG_*` in `Routes` and are read by
  `savedStateHandle[Routes.ARG_X]` in the ViewModel — never re-typed as string literals.
- **A file path argument goes in a query parameter, not a path segment.** A path is full of
  slashes; escaped slashes stop the route matching. See `Routes.TRANSLATION` and the
  hand-rolled `urlEncoded()` (Android's `Uri.encode` is not multiplatform).
- `TopLevelDestination` enum owns the bottom bar. Screens reached from *within* a tab
  (passport, collection) are deliberately not in it.
- **Navigation is an Effect, never state.** The ViewModel raises `XEffect.OpenY(id)`; the
  Route turns it into `navController.navigate(...)` via the lambda it was given.

---

## 8. Compose stability — `shared/compose-stability.conf`

`:domain` and `:data` are compiled without the Compose plugin, so **every domain type
reaching a composable is inferred unstable** even though each is an immutable `data class`.
Unstable is a behaviour, not a warning: the composable compares by reference, can never
skip, and since every `setState { copy(…) }` makes a new object, the whole subtree
re-executes on every emission.

`compose-stability.conf` declares them stable instead of putting a Compose dependency in
`:domain`. Everything listed there **must** be genuinely immutable after construction.

Two gates keep this true:
- `composeCompiler { reportsDestination / metricsDestination }` — reports written every build.
- `ComposeStabilityReportTest` (androidHostTest) — asserts against those reports. A class
  added to the conf file that does not hold up fails the build.

Comments in that file use `//`. A `#` is parsed as part of a class pattern and breaks the build.

---

## 9. Testing layout

```
commonTest/
├── testing/Fakes.kt                  Fake repositories + `clearAsFrameworkWould()` +
│                                     builders (`discovery(id = …)`)
└── feature/<name>/XViewModelTest.kt  One per ViewModel
```

Conventions, from `LensViewModelCrashTest`:
- `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@BeforeTest`, `resetMain()` in `@AfterTest`.
  `viewModelScope` is hard-wired to `Dispatchers.Main.immediate`.
- Time is driven by the scheduler (`advanceTimeBy` / `runCurrent`), never by real delays.
- **Never `advanceUntilIdle`** when the VM has a `while (isActive)` loop — the scheduler
  never goes idle and the suite hangs instead of failing. Use a bounded `settle(horizon)`.
- Effects are asserted with Turbine: `vm.effects.test { assertTrue(awaitItem() is …) }`.
- **Intent fuzzing is the standard for any VM with more than two concurrent jobs**: feed
  every intent in N shuffled orders with a fixed seed and assert nothing throws. This is
  what found the leaked stage ticker.

Two source-reading gates live in `androidHostTest`, because both need `java.io.File`:

- `ComposeStabilityReportTest` — asserts against the Compose compiler's own reports (§8).
- `DesignTokenTest` — asserts against the `commonMain` **sources**, as text. A hardcoded
  radius and `MaterialTheme.shapes.large` compile to identical bytecode, so the file is the
  only place the difference still exists. See §13.

Build gates: `tasks.withType<Test>` depends on `compileAndroidMain` so the Compose reports
exist before the stability test reads them, and sets `vietlens.commonMainDir` so the token
test can find the sources.

---

## 10. Where does a new file go?

| I am adding… | It goes in | Notes |
|---|---|---|
| A screen | `feature/<name>/` — 3 files (Contract, ViewModel, Screen) | Register the VM in `presentationModule` |
| A business rule | `:domain/usecase/XUseCase.kt` | Register in `useCaseModule`. Never in a ViewModel |
| A data model | `:domain/model/` | Immutable `data class`. Add its package to `compose-stability.conf` if new |
| A repository | Interface in `:domain/repository/`, impl in `:data/` | ViewModels depend on the interface |
| A reusable composable | `core/designsystem/component/` | Only if used by ≥2 features |
| A page header | Nothing — call `PageHeader` or `OverlayHeader` | §13. A screen never writes its own; `DesignTokenTest` fails the build |
| A colour / dimension / duration | `core/designsystem/theme/` | Never a magic number in a screen. §13 |
| A text style | `theme/Type.kt` — a scale, or `StampType` | Never a `.copy()` or a `fontWeight` at a call site |
| A string | `composeResources/values/strings.xml` **and** `values-vi/` | Both, always |
| A platform capability | `expect` in `commonMain`, `actual` in both platform source sets | + a Koin binding in `platformUiModule` if it needs `Context` |
| A pure helper | `core/util/` | Must be testable without Compose |
| A screen-local helper composable | Bottom of the same `XScreen.kt`, `private` | Split to `XComponents.kt` past ~150 lines |

**Naming:** kebab-case for non-Kotlin files, PascalCase for Kotlin. Feature packages are
lowercase single words (`camera`, `chat`, `passport`). The screen is `XScreen.kt` even when
the package is named differently (`feature/camera/LensScreen.kt`).

**File size:** target under 200 lines per file (project rule). See §11 — six screen files
currently break this badly and are the standing refactor backlog.

---

## 11. Known deviations from the standard

These exist in the reference codebase today. **Do not copy them.** Fix them when you touch
the file.

### Open — hygiene

| # | Deviation | Location |
|---|---|---|
| 11 | Screen files far over the 200-line rule: `LensScreen` 2173, `DiscoveryScreen` 1974, `PassportScreen` 1061, `JournalScreen` 838, `SettingsScreen` 804, `TranslationScreen` 714 | `feature/*/` |
| 13 | `SovereigntyRoute` renders the page itself — there is no private stateless `SovereigntyScreen`. The only feature without the Route/Screen split, so the page cannot be previewed. Not fixed with the MVI conversion because extracting a composable changes its recomposition scope, which is feature work rather than a structural rename. | `feature/sovereignty/SovereigntyScreen.kt:79` |
| 14 | `commonTest` does not compile for Kotlin/Native: `LensViewModelCrashTest.kt:187` uses `SecurityException`, which is a JVM class. `:shared:allTests` therefore fails at `compileTestKotlinIosSimulatorArm64`; only the Android host run is green. Pre-dates this refactor — the file is unchanged since `09401ed`. | `commonTest/…/LensViewModelCrashTest.kt:187` |
| 16 | **The chat screen still ignores dark mode**, and deliberately so for now. `plans/260803-1118-ui-standardisation/plan.md` phase 3.6 called for deleting `HeaderBackground` / `InkMuted` and taking the header's colours from the scheme. The header was only ever half the story: `PageBackground`, `ComposerBackground`, `GuideCard` and the bubbles are fixed to the lens palette too, and the screen pins the system bar icons to dark to match — argued in `ChatScreen.kt:85-91` and protected by §12. Converting the header alone would put a scheme-dark bar on a cream page. The UI standardisation therefore took the header's *structure* (it is a `PageHeader` now) and left its palette, which is a colour-system decision and out of that plan's scope. Either convert the whole screen or write the fixed palette up in `Color.kt` beside the lens and sovereignty ones. | `feature/chat/ChatScreen.kt:92-98` |
| 17 | `TranslationScreen` is allowlisted in `DesignTokenTest` for `.sp` alongside the two map canvases. Its use is legitimate — `autoSize` steps a translated block's type down to fit the Vietnamese line it covers — but the allowlist is per *file*, so a genuine hardcoded size added anywhere else in those 700 lines would pass. Narrow it to the composable if the file is ever split. | `feature/translate/TranslationScreen.kt:405-415` |
| 18 | **`androidDeviceTest` cannot run on Android 15 or newer.** All nine instrumented tests fail in `Espresso.onIdle` with `NoSuchMethodException: android.hardware.input.InputManager.getInstance` — a reflection call Espresso makes that the platform removed. It is not a project defect and not a regression: `RecompositionTest.theChatStateIsComparedByValue` only compares two `ChatState` instances and fails identically. The only emulator on this machine is API 37, so the device leg of any verification is currently unrunnable. Fix by upgrading `androidx.compose.ui:ui-test-junit4` and its transitive `androidx.test:runner`/`espresso-core`, or by keeping an API 34 AVD for this suite. | `androidDeviceTest/` |
| 19 | **`DesignTokenTest` scans `feature/` for four of its six rules** — gap, corner, weight and type size — while only the inset rule walks all of `commonMain`. A literal radius or a call-site `fontWeight` added under `core/designsystem/component/` is therefore invisible to the gate, which is the one place a bad value would spread furthest. The narrow scope was deliberate (a design-system component owns its own internal geometry, per `Dimens.kt`), but *internal padding* and *a sixth corner radius* are not the same exemption. Verified clean by hand on 03.08.2026 — zero corner literals, zero call-site weights, zero `.sp` outside `Type.kt` — so this is a latent hole, not a live one. Fix by scanning `designsystem/` for corner and weight while leaving the gap rule at `feature/`. | `androidHostTest/…/DesignTokenTest.kt:290` |
| 21 | `docs/bug-report-effect-collection.md` does not exist, but `plans/260802-2103-mvi-refactor/plan.md` refers to it four times as the home for the deferred UI work. Either write it or drop the references. **Renumbered from 15 on 03.08.2026:** a *different* deviation in the Fixed table below already held that number, and seven places cite `§11 row #15` meaning that one — including live comments in `JournalContract.kt`, `JournalViewModelTest.kt` and `SettingsViewModelTest.kt`. A number cited from source code is not free to reuse. | `plans/260802-2103-mvi-refactor/plan.md` |

### Fixed

| # | Deviation | Fixed by |
|---|---|---|
| 1 | `ChatScreen` never collects `viewModel.effects` | Already fixed before the refactor: `ChatEffect` is now an empty sealed interface, `ScrollToBottom` / `ShowMessage` / `RequestMicPermission` were removed, and scrolling and the error banner are both driven from state. `ChatRoute` correctly has no collector. |
| 2 | `JournalScreen` never collects `viewModel.effects` | Already fixed before the refactor: `JournalRoute` collects `ShowMessage` and shows the error snackbar. The refactor converted the wrapper to `CollectEffects` and kept the effect — deleting it, as the refactor plan's step 3.3 assumed, would have removed a visible snackbar. |
| 3 | `ChatViewModel.onMicPermissionGranted()` public and unreferenced | Already fixed before the refactor: the method and the voice-input branch behind it are gone. |
| 4 | `SovereigntyViewModel` a plain `ViewModel` | MVI refactor — now `MviViewModel<SovereigntyState, SovereigntyIntent, SovereigntyEffect>` with `SovereigntyContract.kt`. Cost one slot in `UNSTABLE_CLASS_CEILING`; see the note in `ComposeStabilityReportTest`. |
| 5 | `MainViewModel` a plain `ViewModel` | MVI refactor — kept, and the exemption is now written down in §3 above, in `docs/android-mvi-best-practices.md` §3, and in the class's own KDoc. |
| 6 | `newCapturePath()` public on two VMs | MVI refactor — `LensEffect.TakePhoto` now carries `outputPath`, and `NoteCameraOverlay` takes `CaptureStore` from Koin. Both public methods deleted; Discovery also loses the five-level `newCapturePath` lambda thread. |
| 7 | Contract types inline in the ViewModel file | MVI refactor — `CollectionContract.kt`, `JournalContract.kt`, `SettingsContract.kt`, `TranslationContract.kt` added. All ten features now have one. |
| 8 | `effects.collectLatest { }` | MVI refactor — zero remaining. Settings needed its snackbar calls moved into `scope.launch` to keep replace-not-queue behaviour, because `showSnackbar` suspends for the length of the notice. |
| 9 | Effect collection not lifecycle-aware | MVI refactor — all six collectors go through `CollectEffects`, which uses `repeatOnLifecycle(STARTED)`. |
| 10 | No shared effect-collection helper | MVI refactor — `core/mvi/CollectEffects.kt`. It is the only place `effects.collect` appears. |
| 12 | Unused `import kotlinx.coroutines.launch` | MVI refactor — removed from the Journal, Settings, Explore and Translation ViewModels. Chat's had already gone with the voice branch. |
| 15 | **A failed day summary told the traveller nothing.** `JournalEffect.ShowMessage` was emitted and collected, then dropped: the route resolved the text as `state.error?.toUserMessage()` during composition and the handler did `errorMessage ?: return@launch`, but the effect is handled one main-queue turn after `sendEffect` — before the frame that would have produced the string. Found by running the app offline on a device; reproduced identically on a build of `dcdb958`, so it pre-dated the MVI refactor. **Fixed 03.08.2026:** `AppError.userMessage()` is a `suspend` twin of `toUserMessage()` that resolves outside composition, so the three routes that collect a message effect — Journal, Settings, Explore — now read it off the effect's own payload. `JournalState.error` and `SettingsState.error` are gone with it: neither screen draws a failure inline, so a failure belongs entirely to the effect. Journal's `launchSafely(onError = …)` now raises the effect too, so an unwrapped throw reports rather than just lowering the spinner. Covered by `JournalViewModelTest`. |
| 20 | **Five strings rendered their own escape characters** — `100%% MATCH` on the discovery badge, `N%% collected` / `N%% explored`, and `Turn today\'s one find into a story` on the journal card. `strings.xml` was written in Android `aapt` conventions, where `\'` is unescaped at build time and `String.format` collapses `%%` to `%`; Compose Multiplatform's resource reader substitutes positional arguments but does neither. The Vietnamese file already had it right — `Khớp %1$d%` with one percent — which is what confirmed the English was the anomaly rather than the renderer. | **Fixed 03.08.2026:** write `%` and `'` directly. `\n` is **not** part of this — it *is* unescaped by Compose Multiplatform, proven on device by `sovereignty_seal` rendering "CHỦ / QUYỀN / VN" on three lines, so `discovery_share_body:119` and `sovereignty_seal:348` were deliberately left alone. All four visible cases re-checked on device. |

---

## 12. What is already right — keep it

Do not "improve" these; they are deliberate and documented in the code:

- `Channel`-based effects with the exactly-once guarantee (`LensViewModelCrashTest` asserts it).
- `launchSafely` on every screen action, with `CancellationException` rethrown.
- One effect collector, `core/mvi/CollectEffects.kt`: `collect`, `repeatOnLifecycle(STARTED)`,
  handler held through `rememberUpdatedState`. Never hand-roll the block again — doing so is
  how two screens ended up with no collector at all.
- Route/Screen split — done in 9 of 10 features (sovereignty is §11 row #13).
- Derived state as computed `val`s on the state class (`isBusy`, `visibleDays`, `hasMap`),
  never duplicated fields.
- Ids in state, not objects (`selectedItemId`, `selectedPlaceId`) — so a background refresh
  re-renders the open sheet instead of leaving a stale copy.
- Structural job ownership: the stage ticker is a *child* of the analysis job, not a field.
- Cancel-and-replace for superseding requests (`analysisJob?.cancel()`, `detailsJob?.cancel()`).
- `AppResult` / `AppError` all the way up; no exceptions across layer boundaries.
- Two headers and no third: `PageHeader` and `OverlayHeader`. The temptation is a
  `titleStyle` parameter — that is how the five hand-rolled headers came back last time.
- `SHUTTER_INSET`, `COMPOSER_CLEARANCE`, `SheetPeekHeight` and the stamp geometry are
  *measured positions*, not gaps. They are named `private val`s on purpose and must not be
  snapped onto the `Spacing` scale; the shutter one is a hit target nothing covers.

---

## 13. The UI standard

> Full rationale and the pre-PR checklist: `docs/android-mvi-best-practices.md` §11.
> Enforced by `DesignTokenTest` (androidHostTest), which reads the sources as text.

**Every screen draws its text, its gaps and its corners from the same named set of values,
and every screen's header is one of two components.** Nothing is measured at the call site.

### 13.1 Typography — `theme/Type.kt`

All fifteen Material scales are declared, and that is the point: the six that used to be
left at the default did not carry `LineHeightStyle.Trim.None`, which is what keeps
Vietnamese stacked diacritics (ề, ộ, ữ) from being clipped. They were used 44 times.

Weight is baked in — display **Bold**, `headlineLarge` and `headlineMedium` **Bold**,
`headlineSmall` **SemiBold** where it meets the titles, title **SemiBold**, label
**SemiBold** then **Medium**, body **Normal**. Body sizes sit one step above Material (17 / 15 / 13)
because they carry the reading; the label ladder (14 / 12 / 11) does not, because labels
are chrome.

| Role | Scale |
|---|---|
| Page title, document screens | `headlineMedium` — set by `PageHeader` |
| Page subtitle | `bodyMedium` on `onSurfaceVariant` — set by `PageHeader` |
| Overlay title, over a photo or the camera | `titleLarge` — set by `OverlayHeader` |
| Eyebrow / kicker / section label | `StampType.kicker`, drawn through the `Kicker` composable |
| Card / row title | `titleMedium` |
| Reading body | `bodyLarge` (17 / 27) |
| Secondary body | `bodyMedium` (15 / 23) |
| Metadata / caption | `bodySmall` (13 / 20) |
| Button | `labelLarge` |

`StampType` holds the monospace chrome — `kicker`, `ordinal`, `caption`, `seal` — which
used to be re-derived at four separate call sites, one of them inside a component file.

**No `fontWeight`, no `fontSize`, no size-changing `.copy()` inside `feature/`.** A variant
is a scale and it lives in `Type.kt`.

### 13.2 Spacing — `theme/Dimens.kt`

```kotlin
object Spacing     { xxs 2 · xs 4 · sm 8 · md 12 · lg 16 · xl 24 · xxl 32 }
object PageSpacing { headerTop 12 · headerToContent 16 · sectionGap 24 · listBottom 32 · snackbarLift 96 }
val ScreenGutter = Spacing.lg
```

`PageSpacing` holds the gaps that have to agree *across* screens. `ScreenGutter` keeps its
name and its KDoc argument; **a screen may not adjust it** — `ScreenGutter + 4.dp` is a
token being locally corrected, which is a token that has stopped meaning anything.

A number that is a *position* rather than a gap is not on this scale. Name it, and say what
it was measured against.

### 13.3 Shape — `theme/Type.kt`

`MaterialTheme.shapes` was declared, passed to the theme, and referenced zero times; the
app drew 57 literals at twelve radii instead, seven of them values the theme did not have.

| Slot | Radius | Absorbed | For |
|---|---|---|---|
| `extraSmall` | 6 | 4, 6 | tags, tiny chips, the chat bubble's tail |
| `small` | 10 | 12 | inline controls, thumbnails |
| `medium` | 16 | 14, 16, 18 | cards, sheets-in-page |
| `large` | 24 | 20, 24 | big cards, the camera frame |
| `extraLarge` | 32 | 28 | the passport sheet, the chat composer |

Plus `Pill` (`RoundedCornerShape(percent = 50)`) and `CircleShape`. The app's only 3 dp
corner — the journal's 6 dp-tall progress bar — became `Pill`, which is what it always
meant.

`Corner` holds the same five as **numbers**, for everything that traces a corner by hand
and so cannot be handed a `Shape`: a `dashedBorder` stroking an outline, the passport
sheet's hairline, the chat bubble's two radii. Those must agree with the `clip` on the same
node to the pixel — an outline drawn at 20 inside a box clipped at 24 has its corner arcs
sliced off, which is what happened to two discovery cards during this refactor.

### 13.4 `PageHeader` — journal, settings, collection, passport, chat

```kotlin
PageHeader(title, kicker?, subtitle?, onBack?, trailing?, colors)
```

One box: `ScreenGutter` either side, `PageSpacing.headerTop` above,
`PageSpacing.headerToContent` below. Title always `headlineMedium`.

**It does not apply the top inset** — that stays on the screen's outermost container, which
is what `Insets.kt` argues: in landscape the cutout moves to one side and the whole page
has to move with it, not just its heading.

`colors` is the one override, and only because chat is fixed to the lacquer palette by
design (§11 row #16, §12). There is no `titleStyle` and there will not be one.

### 13.5 `OverlayHeader` — discovery, translation, explore

```kotlin
OverlayHeader(title?, subtitle?, style, busy, leading?, trailing?)
```

`OverlayHeaderStyle.Scrim` (white on a gradient — photos, camera), `Card` (a translucent
surface in scheme colours — the map, which a black gradient would bruise), or `Plain`
(nothing behind it — the discovery photo already draws its own three-stop gradient).

**It does apply `screenInsetsPadding()`**, because it floats over content and nothing else
is in a position to. That is what removed all five raw `statusBarsPadding()` calls in
`DiscoveryScreen` — the page's close and delete, the photo viewer's close, and the note
camera's close and flip — every one of which sat under the notch.

`OverlayIconButton` is now the app's one overlay affordance. Three components were deleted
into it: `GlassButton` (translation), `CloseChip` (sovereignty) and a private copy inside
`DiscoveryScreen`. `BackChip` survives for `PageHeader` only — a chip on a page and a disc
over a photograph are genuinely two things.

`LensScreen` and `SovereigntyScreen` render neither header, deliberately — the camera tool
row is a line of switches, and the sovereignty statement is a scrolling document whose own
column takes the inset. `DesignTokenTest.HEADER_OWNERS` lists the eight screens that must
comply and states both exclusions in its KDoc; the test also fails if a name in that list
stops matching a file, so renaming a screen cannot quietly drop it from the check.
