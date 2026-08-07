# `:shared` — the whole presentation layer

> Every screen, every ViewModel, the navigation graph, the design system — written once
> against Compose Multiplatform and drawn on Android and iOS. Two *arrangements*
> (`mobile/`, `tablet/`) over **one** ViewModel layer.
>
> Structure map: [`../LLM.md`](../LLM.md) §3–§13 · How to write a screen:
> [`../docs/android-mvi-best-practices.md`](../docs/android-mvi-best-practices.md) ·
> Large windows: [`../docs/large-screen-layout.md`](../docs/large-screen-layout.md) ·
> Layers below: [`../domain/README.md`](../domain/README.md) ·
> [`../data/README.md`](../data/README.md)

---

## 1. What this module is

`:app` is a thin Android host — an Application class, one Activity, a theme and a manifest —
and `iosApp` is an Xcode project that links the framework this module produces. **Neither
knows anything about a screen.** All of it is here.

```
:app  ──▶  :shared  ──api──▶  :domain
             │  └──implementation──▶  :data   (to install its Koin modules, nothing else)
             └──▶  SaolaShared.framework (static)  ──▶  iosApp
```

`api(projects.domain)` plus `export(projects.domain)` on the framework: every domain type is
part of the Swift surface. `implementation(projects.data)` is deliberately not `api` —
nothing above this module may name a `…Impl`, a DAO or a DTO.

The framework is **static**, not dynamic. A dynamic framework has to be embedded and signed
into the app bundle and every Compose symbol resolved through `dyld` at launch; static links
it straight into the executable.

---

## 2. Layout

```
shared/src/commonMain/kotlin/com/evora/technologies/saola/
├── MainViewModel.kt          the window host — theme, splash gate, the two startup jobs.
│                             The ONE plain ViewModel; see §3.2
├── core/
│   ├── mvi/                  MviViewModel.kt (read this first) + CollectEffects.kt
│   ├── designsystem/
│   │   ├── theme/            Color, Type, Dimens, Motion, Insets, Theme
│   │   └── component/        19 files, one composable each — PageHeader, OverlayHeader,
│   │                         AppSnackbar, AppAsyncImage, FillGauge, DashedInk, …
│   ├── window/WindowClass.kt COMPACT / EXPANDED — measures the window, picks a branch
│   └── util/                 Log, Formatters, Permissions, ErrorMessages, DetectTimeout, …
├── feature/<name>/           SHARED HALF — 10 packages, 123 files. Contract + ViewModel
│   └── component/            + everything both branches draw
├── mobile/                   ARRANGEMENT BRANCH — the phone. 14 files
│   ├── navigation/           BottomDestinations.kt, SaolaApp.kt
│   └── feature/<name>/       XScreen.kt — 12 files across 11 packages
├── tablet/                   ARRANGEMENT BRANCH — the large window. 13 files
│   ├── navigation/           RailDestinations.kt, SaolaTabletApp.kt, TabletNavGraph.kt,
│   │                         TwoPaneScaffold.kt
│   └── feature/<name>/       XTabletScreen.kt, or XPane.kt where a screen is a pane —
│                             9 files across 8 packages
├── navigation/               Routes.kt, TopLevelNavigation.kt, SaolaRoot.kt — SHARED
├── di/SharedModules.kt       useCaseModule + presentationModule + appModules()
├── platform/                 expect: IS_APPLE_PLATFORM + the five PlatformActions
└── voice/                    TextToSpeech — an interface, not an expect; see §3.6
```

Plus `commonMain/composeResources/` (eight `values-*/strings.xml`, same keys), `androidMain/`
and `iosMain/` actuals, and four test source sets — see §8.

---

## 3. The rules

### 3.1 MVI, and the four things `MviViewModel` gives you

```kotlin
abstract class MviViewModel<S : UiState, I : UiIntent, E : UiEffect>(initialState: S) : ViewModel() {
    val state: StateFlow<S>
    val effects: Flow<E>
    protected val currentState: S
    abstract fun onIntent(intent: I)
    protected fun setState(reducer: S.() -> S)
    protected fun sendEffect(effect: E)
    protected fun launchSafely(onError: (AppError) -> Unit = {}, block: suspend CoroutineScope.() -> Unit): Job
}
```

Non-negotiable, and each has already cost a real bug:

1. **Every screen ViewModel extends `MviViewModel`.** No plain `ViewModel` — one exemption,
   §3.2.
2. **`onIntent` is the only public method.** No escape hatches.
3. **State/Intent/Effect live in `XContract.kt`**, never inline in the ViewModel file. All ten
   features have one, including where the effect set is empty — an empty `sealed interface`
   with a KDoc saying why is still declared.
4. **Every coroutine goes through `launchSafely`**, and `CancellationException` is rethrown.
5. **Effects go through a `Channel`, not a `SharedFlow`.** A replaying hot flow either drops a
   navigation event (no subscriber) or fires it twice (config change).
6. **Every declared Effect is collected by a screen**, with `collect` (never `collectLatest`),
   lifecycle-aware.
7. **Navigation is an Effect, never a flag in state.**
8. **Sub-jobs are structural children of their parent job**, never fields cancelled by hand.
9. **No Compose import and no platform import inside a ViewModel.**

The full argument for each is in
[`../docs/android-mvi-best-practices.md`](../docs/android-mvi-best-practices.md).

### 3.2 `MainViewModel` is the one plain `ViewModel`, and that is written down

`MviViewModel` is for **screens** — anything with a route and a back-stack entry.
`MainViewModel` is the **window host**: no route, owns the theme and the splash gate for the
whole window, read by the Android Activity and by `MainViewController` on iOS before any
screen exists. An intent channel with no sender and an effect channel with no collector would
be ceremony. It is the only one allowed to be, and all ten feature ViewModels extend
`MviViewModel`.

### 3.3 `mobile/` and `tablet/` are arrangements, not apps

> **No business logic in `mobile/` or `tablet/`, and no `XViewModel` under either.**

A branch package receives `state`, emits `onIntent`, and places composables. Everything that
decides what the app *knows or does* — Contract, ViewModel, use case, repository, design
system, `Routes` — stays shared.

The count is the argument: ten Contracts and ten ViewModels against **eighteen arrangements**
of them — ten `XScreen.kt` under `mobile/feature/`, eight `XTabletScreen.kt`/`XPane.kt` under
`tablet/feature/`. A defect in what a screen knows is fixed once; only a defect in where
something *sits* is fixed twice.

The *file* counts differ from that and `DesignTokenTest` prints them on every run —
`mobile/feature: 12`, `tablet/feature: 9` — because they also count the licences screen and
its card (no shared half at all, §7) and `RecentScanList.kt`, a composable only the tablet lens
draws. Read the printed number when checking the gate's reach, and eighteen when arguing about
the split. The moment a file under `tablet/` starts deciding something, the project has twenty
ViewModels wearing ten names, and the second one is the one nobody re-reads.

This is a **package convention, not a Gradle boundary** — nothing mechanically stops a branch
from calling into the other. If the tablet branch ever grows past layout, promote both to
`:shared:mobile` / `:shared:tablet` modules.

**A composable both branches draw lives in `feature/<name>/`, never inside one branch.** That
is the whole mechanism behind "the tablet uses the same components as mobile": a shared piece
left `private` inside `mobile/feature/x/XScreen.kt` cannot be called from `tablet/`, so it
gets copied — and a copy diverges on the first fix only one side receives. Lift it in the same
change; do not duplicate.

### 3.4 `Route` vs `Screen` — the split is mandatory

| | `XRoute` | `XScreen` |
|---|---|---|
| Visibility | `fun` (public) | `private fun` |
| Takes | navigation lambdas + `viewModel: XViewModel = koinViewModel()` | `state`, `onIntent`, nav lambdas |
| Does | collects state, collects effects, owns lifecycle observers and platform controllers | renders, and nothing else |
| Previewable | no | yes |

Only `XRoute` may touch the ViewModel. Everything below it is pure.

**`private` unless an instrumented test drives it**, and then `internal` +
`@VisibleForTesting`. Three screens carry that widening today. It is a widening rather than a
hole because `androidDeviceTest` compiles *inside* this module. **Do not widen a screen for a
preview** — a `@Preview` takes `state` and calls the private function from the same file.

**A `XHost.kt` sits above the branch line when a Route is more than glue.** Three exist —
`LensHost`, `ExploreHost`, `SettingsHost` — because each owns behaviour rather than layout: the
capture coroutine whose `finally` releases the shutter, the permission bridge whose answer
exists only in the composable, the two effect arms launched into a `rememberCoroutineScope` so
a slow snackbar cannot queue the next notice behind it. §3.3 forbids a branch from owning
behaviour, and two copies mean the next fix lands on one form factor only. **A Route that is
genuinely five lines does not need one.**

**A pane is not a Route.** Where the tablet shows one feature inside another, the *host* Route
resolves every ViewModel and the pane takes `state` + `onIntent` like any other stateless
piece. Named `XPane.kt` so the file name says which of the two it is.

### 3.5 One route table, one shell per branch

`navigation/Routes.kt` is shared and holds every route string, every `ARG_*`, `TOP_LEVEL` and
`urlEncoded()` — one table, or a deep link works on one form factor and not the other.
`TopLevelNavigation.kt` is shared for the same reason: "never stack a duplicate tab" is the
app's behaviour, not a bar's.

- **A new route is registered in every branch shell that exists, identically.** Nothing
  enforces it, and the second failure mode is the quiet one: `NavController.setGraph` compares
  graphs **structurally**, and only two it judges equal take the update-in-place path. One
  route the other shell does not declare — or a `navArgument` default written differently —
  fails that comparison and the controller clears the back stack on every resize.
- Argument names are `const val ARG_*` in `Routes`, read via `savedStateHandle[Routes.ARG_X]`,
  never re-typed as literals.
- **A file path argument goes in a query parameter, not a path segment** — escaped slashes stop
  a route matching.
- **The rail decides its own visibility with `railDestination()`, not `isTopLevel()`.** On a
  large window the passport and the collection *are* the journal; judged by `isTopLevel()` the
  rail vanished the moment a traveller deep-linked into the passport.

### 3.6 Everything platform-specific is `expect`/`actual`, with one narrow exception

Twelve `expect` declarations: the five `PlatformActions` (photo picker, text sharer, mail
sharer, URL opener, text copier), the two permission states, the two system-bar helpers,
`rememberCameraController()`, `PlaceMap` and `platformUiModule`. Actuals in `androidMain`
(CameraX, Maps SDK, `TextToSpeech`) and `iosMain` (AVFoundation, MapKit,
`AVSpeechSynthesizer`).

`PlaceMap` is the only `expect` in the whole Explore feature — what gets searched for, how it
is ranked, what a marker means and what the sheet says are written once.

`TextToSpeech` is an **interface with a per-platform implementation bound by
`platformUiModule`**, not an `expect class`, because the Android engine needs a `Context`.
Prefer that shape for anything with a common contract: an interface can be faked in
`commonTest`, and an `actual` cannot.

**The exception is `IS_APPLE_PLATFORM`, and it is deliberately kept to strings.** One call
site uses it — the maps URL scheme in `feature/discovery/ReportMail.kt`, which Apple claims as
`maps://` and Android as `geo:`. Anything larger than a string belongs in an `expect`
declaration, where the compiler enforces that both platforms are handled. **A runtime branch
around behaviour compiles on both platforms and is proved on neither.**

### 3.7 Compose stability is a build gate, not a hope

`:domain` and `:data` are compiled without the Compose plugin, so every domain type reaching a
composable would be inferred **unstable** — the composable compares by reference, can never
skip, and since every `setState { copy(…) }` makes a new object, the whole subtree re-executes
on every emission.

[`compose-stability.conf`](compose-stability.conf) declares them stable instead of putting a
Compose dependency in `:domain`. Two gates keep it true: the compiler writes reports on every
build (`composeCompiler { reportsDestination }`), and `ComposeStabilityReportTest` asserts
against them — a class added to the conf file that does not hold up fails the build.

> Comments in that file use `//`. A `#` is parsed as part of a class pattern and breaks the
> build.

---

## 4. The UI standard

Enforced by `DesignTokenTest`, which reads the `commonMain` **sources as text** — a hardcoded
radius and `MaterialTheme.shapes.large` compile to identical bytecode, so the file is the only
place the difference still exists.

**Nothing is measured at the call site.**

| Token | Lives in | Rule |
|---|---|---|
| Typography | `theme/Type.kt` | All fifteen Material scales declared. **No `fontWeight`, no `fontSize`, no size-changing `.copy()` inside `feature/`** — a variant is a scale and it goes in `Type.kt` |
| Spacing | `theme/Dimens.kt` → `Spacing`, `PageSpacing` | `ScreenGutter + 4.dp` is a token being locally corrected, which is a token that has stopped meaning anything |
| Pane widths | `theme/Dimens.kt` → `PaneWidth` | A *position*, not a gap — deliberately off the spacing scale. Only `TwoPaneScaffold` reads the three content widths |
| Shape | `theme/Type.kt` → `MaterialTheme.shapes`, `Corner`, `Pill` | `Corner` holds the same five as **numbers**, for anything tracing a corner by hand; it must agree with the `clip` on the same node to the pixel |
| Header | `PageHeader` or `OverlayHeader` | Every screen's header is one of the two. `DesignTokenTest.HEADER_OWNERS` lists the **fifteen** files that must comply, and states every exclusion in its KDoc |

All fifteen typography scales are declared rather than six left at the default because the
defaults do not carry `LineHeightStyle.Trim.None`, which is what keeps Vietnamese stacked
diacritics (ề, ộ, ữ) from being clipped. They were used 44 times.

`PageHeader` does **not** apply the top inset — that belongs to the screen's outermost
container, because in landscape the cutout moves to one side and the whole page must move with
it. `OverlayHeader` **does**, because it floats over content and nothing else is in a position
to.

Full tables in [`../LLM.md`](../LLM.md) §13.

---

## 5. Dependency injection

```kotlin
val useCaseModule: Module        // factory { XUseCase(get()) } — 30 lines
val presentationModule: Module   // viewModel { XViewModel(...) } — 11 + process-wide singletons
internal expect val platformUiModule: Module

fun appModules(isDebug: Boolean): List<Module> =
    dataModules(isDebug) + useCaseModule + presentationModule + platformUiModule
```

One file, the whole graph above the repositories. No annotation processor — Hilt and Dagger do
not run for Kotlin/Native — so a missing binding is a grep away.

- **ViewModels are `viewModel { }`, never `single { }`** — one per `ViewModelStoreOwner` (the
  back-stack entry), cleared with it.
- A ViewModel reading route arguments takes `savedStateHandle: SavedStateHandle`.
  `ChatViewModel` is the only one that *also* takes its id outright, because on a large window
  the guide is a column inside the discovery's entry rather than a destination.
- **Both entry points pass exactly `appModules(isDebug)`**, so neither platform can get a graph
  the other does not have.
- Adding a ViewModel is **one** `viewModel { }` block; adding a use case is **one** `factory { }`
  line.
- `isDebug` is passed in, never read from a constant here — this module is single-variant and
  cannot tell which of `:app`'s build types it landed in. See
  [`../data/README.md`](../data/README.md) §3.2.

---

## 6. Where a new file goes

| I am adding… | It goes in | And also |
|---|---|---|
| A screen | `feature/<name>/XContract.kt` + `XViewModel.kt`, and `mobile/feature/<name>/XScreen.kt` | One `viewModel { }` line; register the route in **every** shell |
| The tablet layout of that screen | `tablet/feature/<name>/XTabletScreen.kt` | Layout only — no new Contract, no new ViewModel. Six-step recipe: [`../docs/large-screen-layout.md`](../docs/large-screen-layout.md) §7 |
| The tablet layout of a screen that is a *pane of another* | `tablet/feature/<name>/XPane.kt` | Stateless. The host Route resolves it |
| A composable both branches draw | `feature/<name>/component/OneName.kt` | `internal`, one composable per file, named after it |
| A composable ≥2 features draw | `core/designsystem/component/` | That is the line the design system draws |
| A shared list | a `LazyListScope` extension, not a composable | The two branches put it in different scrolling containers |
| A pane width | `theme/Dimens.kt` → `PaneWidth` | Never typed at a call site |
| A string | all eight `values-*/strings.xml` | Same keys, same placeholders |
| Business logic | **not here** — `:domain/usecase/` | §3.3 |

---

## 7. Two branches, and which screens have one

Ten features have a shared Contract + ViewModel. Eight have a tablet arrangement; two
deliberately do not, and `Routes.TRANSLATION` and `Routes.LICENSES` are registered identically
in both shells and open the **same** composable under `mobile/feature/` — each is the same
picture at any window size (a full-bleed photograph with the Vietnamese replaced in place; a
scrolling document).

`mobile/feature/licenses/` has no shared half at all: it is the one screen with **no
ViewModel** — five fixed paragraphs and four links, nothing to decide. Read the rule as *a
screen gets a ViewModel when it has something to decide*, and the moment this one acquires a
decision it acquires a ViewModel in the same change. It keeps the Route/Screen split
regardless, because `LicensesRoute` touches something the page must not: `rememberUrlOpener()`.

Which screens have a tablet arrangement and why: [`../docs/large-screen-layout.md`](../docs/large-screen-layout.md).

---

## 8. Testing

Four source sets, and the split is about what each one can physically reach:

| Source set | Holds | Why not elsewhere |
|---|---|---|
| `commonTest/` | one `XViewModelTest.kt` per ViewModel (**all ten have one**), `MainViewModelTest`, `testing/Fakes.kt` | compiles for Kotlin/Native too |
| `androidHostTest/` | `ComposeStabilityReportTest`, `DesignTokenTest` | both read files off disk with `java.io.File` |
| `androidDeviceTest/` | gesture, recomposition, hit-testing and two-pane scroll tests | need a real device to dispatch touches |
| (`:app/src/test/`) | `AppGraphTest` | the graph, resolved by reflection without starting Koin |

**123 on the JVM, 112 on the iOS simulator, plus 18 on a device.** The difference is the two
source-reading gates.

Conventions that matter:

- `Dispatchers.setMain(UnconfinedTestDispatcher())` in `@BeforeTest`, `resetMain()` in
  `@AfterTest` — `viewModelScope` is hard-wired to `Dispatchers.Main.immediate`.
- Time is driven by the scheduler (`advanceTimeBy` / `runCurrent`), never by real delays.
- **Never `advanceUntilIdle`** when the VM has a `while (isActive)` loop — the scheduler never
  goes idle and the suite hangs instead of failing. Use a bounded `settle(horizon)`.
- Effects are asserted with Turbine.
- **Intent fuzzing is the standard for any VM with more than two concurrent jobs**: every
  intent in N shuffled orders with a fixed seed, assert nothing throws. This is what found the
  leaked stage ticker.
- **A flag raised before a suspend call must be lowered in `launchSafely`'s `onError` too, and
  the test calls the repository a second time and counts.** Not "the flag comes down" — a flag
  nobody reads would pass the weaker assertion. Four screens failed this in one audit.

**Run `./gradlew :shared:allTests`, not `:shared:testAndroidHostTest`** — or half the platforms
this presentation layer ships to are never compiled against. Two things compile green on the
JVM and fail Kotlin/Native: a JVM-only type (`SecurityException` has no `kotlin.` twin), and a
backticked test name containing `,` `.` `;` `:` or brackets.

---

## 9. Build-file details that look like boilerplate and are not

- **`androidResources { enable = true }`** — off by default in
  `com.android.kotlin.multiplatform.library`. With it off, nothing assembles the Compose
  resources for the Android target and the app installs and then dies on the first string read
  with `MissingResourceException`.
- **`tasks.withType<Test> { dependsOn("compileAndroidMain") }`** — the Compose reports are a
  side effect of compiling, not a declared task output, so they cannot be wired as a task
  input. Without the explicit `dependsOn`, `test` on a clean tree finds no report and the
  stability gate fails for the wrong reason.
- **`ARCHS = arm64` in the Xcode project, and nothing else.**
  `syncComposeResourcesForIos` maps Xcode's `ARCHS` to Kotlin targets itself and recognises
  only `arm64`/`arm64e` for the simulator SDK; any other value is a hard `error(...)`. Adding
  an `iosX64()` target would not help — the plugin has no x86_64 branch to reach it by.
- **Compose resources need no copy task**; `provinces.json` does, and it is
  `:data:copySharedAssetsToIos` — a plain asset, not a Compose resource, so nothing in the
  Compose plugin knows about it.
- `iosX64` is deliberately absent: Xcode 26 ships no Intel iOS simulator, and
  `androidx.sqlite:sqlite-bundled` no longer publishes an `ios_x64` artifact.

---

## 10. Before reporting a change here as done

Run the checklist in
[`../docs/android-mvi-best-practices.md`](../docs/android-mvi-best-practices.md) §9, and check
that:

- The ViewModel extends `MviViewModel`, and `onIntent` is still its only public method.
- Every new Effect is collected by a screen, with `collect`, lifecycle-aware.
- Nothing new under `mobile/` or `tablet/` decides anything.
- A new route is in **every** shell, spelled identically.
- No literal size, weight or radius entered `feature/` — `DesignTokenTest` reads the sources.
- `./gradlew :shared:allTests` passes, not just the host-test task.
- [`../LLM.md`](../LLM.md) was updated in the same change if the package layout, a layer
  boundary, a navigation convention or a testing convention moved.
