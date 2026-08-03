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
│   ├── MainViewModel.kt              App-level state: theme, splash gate, startup sweep
│   ├── core/
│   │   ├── mvi/MviViewModel.kt       THE MVI CONTRACT — read this first
│   │   ├── designsystem/
│   │   │   ├── theme/                Color, Type, Dimens, Motion, Insets, Theme
│   │   │   └── component/            Reusable composables (AppSnackbar, PermissionSheet…)
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
├── androidHostTest/                  ComposeStabilityReportTest (needs java.io.File)
└── androidDeviceTest/                Gesture/recomposition tests (need a real device)
```

**Rule:** anything platform-specific is an `expect` in `commonMain` with an `actual` in
both `androidMain` and `iosMain`. Never `if (Platform.isAndroid)` in shared code.

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

**`XContract.kt` is mandatory, even when the effect set is empty.** Contract files are the
first thing anyone reads to understand a screen; burying `LensState` at line 200 of a
ViewModel hides it. An empty effect set is still declared:

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

Build gates: `tasks.withType<Test>` depends on `compileAndroidMain` so the Compose reports
exist before the stability test reads them.

---

## 10. Where does a new file go?

| I am adding… | It goes in | Notes |
|---|---|---|
| A screen | `feature/<name>/` — 3 files (Contract, ViewModel, Screen) | Register the VM in `presentationModule` |
| A business rule | `:domain/usecase/XUseCase.kt` | Register in `useCaseModule`. Never in a ViewModel |
| A data model | `:domain/model/` | Immutable `data class`. Add its package to `compose-stability.conf` if new |
| A repository | Interface in `:domain/repository/`, impl in `:data/` | ViewModels depend on the interface |
| A reusable composable | `core/designsystem/component/` | Only if used by ≥2 features |
| A colour / dimension / duration | `core/designsystem/theme/` | Never a magic number in a screen |
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

### Open — correctness

| # | Deviation | Location | Effect |
|---|---|---|---|
| 1 | `ChatScreen` never collects `viewModel.effects` | `feature/chat/ChatScreen.kt` | `ScrollToBottom` ×2, `RequestMicPermission`, `ShowMessage` all dropped. The mic button cannot work. `Channel(BUFFERED)` fills at 64 and `send` then suspends forever inside `viewModelScope` — a coroutine leak per emission. |
| 2 | `JournalScreen` never collects `viewModel.effects` | `feature/journal/JournalScreen.kt` | `JournalEffect.ShowMessage` dropped; same channel-fill leak. |
| 3 | `ChatViewModel.onMicPermissionGranted()` is public and unreferenced | `feature/chat/ChatViewModel.kt:99` | Dead escape hatch. Should be `ChatIntent.MicPermissionGranted`. |

### Open — MVI discipline

| # | Deviation | Location | Should be |
|---|---|---|---|
| 4 | `SovereigntyViewModel` is a plain `ViewModel` exposing `StateFlow<RegionMap?>` | `feature/sovereignty/` | `MviViewModel` + `SovereigntyContract.kt` |
| 5 | `MainViewModel` is a plain `ViewModel` exposing two StateFlows | `MainViewModel.kt` | Acceptable *only* as the app-level host VM; document the exemption or convert |
| 6 | `newCapturePath()` public on two VMs, called from the composable | `LensViewModel:74`, `DiscoveryViewModel:153` | The Route should get `CaptureStore` from Koin, or the VM should raise the path in an Effect |
| 7 | Contract types declared inline in the ViewModel file | `collection`, `journal`, `settings`, `translate` | Extract to `XContract.kt` (5 features already do this) |
| 8 | `effects.collectLatest { }` — cancels handling of the previous effect | `LensScreen:248`, `SettingsScreen:145`, `DiscoveryScreen:204` | `collect` |
| 9 | Effect collection is `LaunchedEffect(viewModel) { … }`, not lifecycle-aware | all 5 screens that collect | A shared `CollectEffects` helper using `repeatOnLifecycle(STARTED)` |
| 10 | No shared effect-collection helper — the same block is hand-rolled 5× and forgotten 2× | `core/mvi/` | Add `CollectEffects.kt`; this is the direct cause of #1 and #2 |

### Open — hygiene

| # | Deviation | Location |
|---|---|---|
| 11 | Screen files far over the 200-line rule: `LensScreen` 2170, `DiscoveryScreen` 1970, `PassportScreen` 1061, `JournalScreen` 806, `SettingsScreen` 788, `TranslationScreen` 715 | `feature/*/` |
| 12 | Unused `import kotlinx.coroutines.launch` | `Journal`, `Settings`, `Chat`, `Explore`, `Translation` ViewModels |

### Fixed
_(move rows here with the commit that fixed them)_

---

## 12. What is already right — keep it

Do not "improve" these; they are deliberate and documented in the code:

- `Channel`-based effects with the exactly-once guarantee (`LensViewModelCrashTest` asserts it).
- `launchSafely` on every screen action, with `CancellationException` rethrown.
- Route/Screen split — done consistently across all 10 features.
- Derived state as computed `val`s on the state class (`isBusy`, `visibleDays`, `hasMap`),
  never duplicated fields.
- Ids in state, not objects (`selectedItemId`, `selectedPlaceId`) — so a background refresh
  re-renders the open sheet instead of leaving a stale copy.
- Structural job ownership: the stage ticker is a *child* of the analysis job, not a field.
- Cancel-and-replace for superseding requests (`analysisJob?.cancel()`, `detailsJob?.cancel()`).
- `AppResult` / `AppError` all the way up; no exceptions across layer boundaries.
