# LLM.md — Project Code Structure

> **Read this file BEFORE writing any code. Update it AFTER any structural change.**
> This is the map of the codebase: where things live, what each layer may depend on, and
> where a new file belongs. It is not a tutorial — for *how* to write an MVI screen, read
> [`docs/android-mvi-best-practices.md`](docs/android-mvi-best-practices.md); for how the
> app arranges itself on a window big enough for two panes, and the rule that keeps that
> from becoming a second app, read
> [`docs/large-screen-layout.md`](docs/large-screen-layout.md).

**Reference implementation:** `Saola` — Kotlin Multiplatform + Compose Multiplatform,
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
            TWO ARRANGEMENT LAYERS OVER ONE VIEWMODEL LAYER: `mobile/` and
            `tablet/` each hold screens and a shell; everything that decides
            what the app knows or does sits below both. See §3.
            Produces `SaolaShared.framework` (static) for iOS.
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

**The two arrangement layers are not a fourth module, and the count is the argument.** Ten
features, ten Contracts, ten ViewModels — and **eighteen** arrangement files: ten under
`mobile/feature/`, eight under `tablet/feature/`. So a defect in what a screen *knows* is
fixed once for both form factors, and only a defect in where something *sits* is fixed twice.
The split earns its keep exactly while that ratio holds: the moment a file under `tablet/`
starts deciding something, the project has twenty ViewModels wearing ten names, and the
second one is the one nobody re-reads. §3 states the rule; `docs/large-screen-layout.md` is
the working guide, including which screens deliberately have no tablet arrangement.

**Why `:domain` and `:data` are Compose-free:** it keeps the layering honest, and it is
also why `shared/compose-stability.conf` exists — see §8.

---

## 3. Package layout inside `:shared`

Root package: `com.evora.technologies.saola`

```
src/
├── commonMain/kotlin/com/evora/technologies/saola/
│   ├── MainViewModel.kt              App-level state: theme, splash gate, and the two
│   │                                 startup jobs — demo seed, then the orphan sweep.
│   │                                 The ONE plain ViewModel — see the exemption below
│   ├── core/
│   │   ├── mvi/MviViewModel.kt       THE MVI CONTRACT — read this first
│   │   ├── mvi/CollectEffects.kt     The one effect collector every XRoute uses
│   │   ├── designsystem/
│   │   │   ├── theme/                Color (+ GuidePalette), Type (+ StampType,
│   │   │   │                         SaolaShapes), Dimens (Spacing, PageSpacing,
│   │   │   │                         PaneWidth), Motion, Insets, Theme
│   │   │   └── component/            One composable per file, as under `feature/*/component/`:
│   │   │                             PageHeader, OverlayHeader, AppSnackbar, AppAsyncImage,
│   │   │                             PermissionSheet, SovereigntyBanner, SystemBars,
│   │   │                             DashedInk, LoadingState, ErrorState, EmptyState,
│   │   │                             AccentChip, ShimmerBox, Kicker, BackChip, FillGauge,
│   │   │                             SectionHeader, SurfaceLuminance
│   │   ├── window/WindowClass.kt     COMPACT / EXPANDED — measures the window, picks a branch
│   │   │                             plus rememberCanStackVertically: column or row inside mobile/
│   │   └── util/                     Log, Formatters, Permissions, DateTimeFormat,
│   │                                 ErrorMessages, VolumeShutterBus, DetectTimeout
│   ├── feature/<name>/               ONE PACKAGE PER SCREEN, shared half — see §5
│   │   └── component/                the composables both branches draw (camera 17,
│   │                                 discovery 20, passport 11, journal 9, settings 8,
│   │                                 explore 6, chat 6, sovereignty 5, collection 4)
│   ├── mobile/                       PRESENTATION BRANCH — what a phone draws
│   │   ├── navigation/
│   │   │   ├── BottomDestinations.kt TopLevelDestination enum — the four tabs
│   │   │   └── SaolaApp.kt        NavHost, scaffold, bottom bar
│   │   └── feature/<name>/XScreen.kt Route + Screen + private children
│   ├── tablet/                       PRESENTATION BRANCH — what a large window draws
│   │   ├── navigation/
│   │   │   ├── RailDestinations.kt   RailDestination enum + railDestination(), which is
│   │   │   │                         also what decides whether a rail is drawn at all
│   │   │   ├── SaolaTabletApp.kt  the shell: Row { NavigationRail · NavHost }
│   │   │   ├── TabletNavGraph.kt     the NavHost — one composable() per route
│   │   │   └── TwoPaneScaffold.kt    Row { fixed pane · flexible pane }, the only
│   │   │                             reader of PaneWidth's three content widths
│   │   └── feature/<name>/           XTabletScreen.kt — camera, discovery, journal,
│   │                                 explore, settings, sovereignty; XPane.kt where a
│   │                                 screen is a pane of another (passport, collection)
│   ├── navigation/
│   │   ├── Routes.kt                 Routes object + TOP_LEVEL + urlEncoded(). Both branches
│   │   ├── TopLevelNavigation.kt     isTopLevel / navigateToTopLevel / restartAtLens
│   │   └── SaolaRoot.kt           THE FORK — the one composable both entry points call
│   ├── di/SharedModules.kt           useCaseModule + presentationModule + appModules()
│   ├── platform/                     expect: Platform, PlatformActions
│   └── voice/                        expect: SpeechRecognizer, TextToSpeech
│
├── commonMain/composeResources/
│   ├── values/strings.xml            Default (en) — the fallback for every unshipped locale
│   ├── values-vi|ja|ko|zh|fr|es|th/  The other seven, same keys, same placeholders
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

**The two presentation branches, and the line between them.** `mobile/` and `tablet/` are
two *arrangements* of one app, not two apps. Everything that decides what the app knows or
does — Contract, ViewModel, use case, repository, design system, `Routes` — stays shared,
and a branch package holds nothing but layout: it receives `state`, emits `onIntent`, and
places composables. The rule that keeps this honest is negative and worth stating as one:

> **No business logic in `mobile/` or `tablet/`, and no `XViewModel` under either.**
> A branch that owns a decision is a branch that will answer it differently from the other
> one, and the second answer is the one nobody re-reads.

The split is a package convention, not a Gradle boundary, so nothing mechanically stops a
branch from calling into the other. If the tablet branch ever grows past layout, promote
both to `:shared:mobile` / `:shared:tablet` modules — the argument for not doing that yet
is in `plans/260804-1016-large-screen-branch/architecture-options.md` QĐ-1.

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

A feature spans **two** directories: what both presentation branches share, and what each
branch arranges.

```
feature/camera/                     shared — one copy, both branches call it
├── LensContract.kt     State + Intent + Effect. NOTHING ELSE. No logic, no VM.
└── LensViewModel.kt    Extends MviViewModel<LensState, LensIntent, LensEffect>

mobile/feature/camera/              the phone's arrangement
└── LensScreen.kt       LensRoute (stateful) + LensScreen (stateless) + private children

tablet/feature/camera/              the large window's arrangement, when one exists
└── LensTabletScreen.kt LensTabletRoute + LensTabletScreen + private children
```

Optional, only when it earns its own file:
```
feature/camera/
├── component/          One composable per file, `internal`, when BOTH branches draw it
├── CameraController.kt Platform-facing helper the screen owns (not the VM)
└── CameraOptions.kt    Feature-local enums/value types that are NOT domain concepts
```

`component/` is a directory rather than one `XComponents.kt` because the 200-line file rule
bites first: the camera's seventeen shared pieces are 1 500 lines together, the discovery's
twenty another 2 000. One composable per file, named after it, so a `Grep` for
`ShutterButton` lands on the file that draws it. Two small siblings share a file only when
they are one decision seen twice — `NoteCards.kt` holds the blank and the written note because
`NoteBlock` animates directly between them and they must keep the same corner and padding;
`CollectionTile.kt` holds the filled tile, the hatched one and the badge because they are one
square with two faces; and `core/designsystem/component/DashedInk.kt` holds the rule and the
dashed border because both are the notebook's stitched ink and a change to one that misses the
other is visible on the same page. That last one moved **out** of `feature/discovery/component/`
on 04.08.2026, when the passport's province panel turned out to hold a byte-identical private
copy of `DashedRule`: two features draw it, which is the line §10 draws around the design system.
`SovereigntyPanel.kt` and `SettingsRow.kt` are the same judgement stretched to its limit —
the statement's map panel and its note are one washed panel holding two different things, and
the settings card's value, switch and destructive rows are one row with three different ends.
Both pass the test the rule is really made of: the siblings are drawn stacked on one screenful,
so a change to one that misses the other is visible without scrolling.

**A list is shared as a `LazyListScope` extension, not as a composable.** `journalDays(…)` and
`collectionBoard(…)` emit the same items into whichever list is asking, because the two
branches put them in two different scrolling containers — the phone's runs the width of the
page, the tablet's is one pane — and a composable would have to nest a second scroller inside
the first. What they hold in one place is the order and its conditions: the end-of-day panel
appears for today only and only until the story exists, and a branch that got that wrong would
ask twice for something already done on one form factor.

**A composable used by both branches lives in `feature/<name>/`, never in one of them.**
That is the whole mechanism behind the constraint *"the tablet uses the same components as
mobile"*: if the shared piece sits inside `mobile/feature/x/XScreen.kt` as a `private fun`,
the tablet cannot call it and will copy it instead — and a copy diverges on the first fix
that only one side gets. When a branch needs something a screen currently keeps private,
lift it into `feature/<name>/` in the same change, do not duplicate it.

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

**`private` unless an instrumented test drives it, and then `internal` + `@VisibleForTesting`.**
Three screens carry the widening today: `TranslationScreen` for the pinch and drag test, and
`DiscoveryTabletScreen` and `JournalTabletScreen` for the two-pane scroll tests. It is a
widening rather than a hole because `androidDeviceTest` compiles *inside* `:shared`, so
`internal` reaches it and nothing outside the module gains anything. The alternative was worse
than the rule it would have protected: each of those tests asserts something about where a
`remember` sits in *that* composable — a scroll position surviving a full-window page, a
translation staying on the words it covers — and a probe assembled from a `Column` and a
`TwoPaneScaffold` would have proved only that the probe was written correctly. Do not widen a
screen for a preview or for convenience; a `@Preview` takes `state` and calls the private
function from the same file.

**One exception, and the argument for it.** `feature/camera/LensHost.kt` is a Route that sits
*above* the branch line: `LensRoute` and `LensTabletRoute` are both one call to it plus a
layout lambda. It exists because the lens's Route is not glue — it owns the capture
coroutine, whose `finally` releases the shutter when the traveller leaves mid-photo, and the
lifecycle observer that makes the volume keys mean "shutter". That is behaviour, and §3
forbids a branch from owning behaviour; two copies of it would mean the next fix to that
`finally` lands on one form factor and not the other. Read the rule as: **the arrangement is
per branch, the ViewModel wiring is shared whenever it is more than a call**. A screen whose
Route is genuinely five lines does not need one.

`feature/explore/ExploreHost.kt` is the second, added 04.08.2026 with the tablet map, and it
is what turns that exception into a rule. Explore's Route owns three things that are not
layout: the permission bridge (the answer exists only in the composable — an Activity result
launcher on Android, a `CLLocationManager` on iOS — so the screen tells the ViewModel rather
than the other way round), the effect collection that resolves a failure's text off the
effect's own payload, and `remember(viewModel) { viewModel::onIntent }`, which §12 argues for
at length and which buys nothing if only one branch has it. Its `content` lambda takes seven
parameters, and that is the honest count of what an arrangement of this screen needs; a
shorter list would mean a branch re-deriving one of them, which is the divergence the host
exists to prevent.

`feature/settings/SettingsHost.kt` is the third, and it is the smallest — three effect arms
and a rule about how they are shown. Every one of them is launched into a `rememberCoroutine
Scope` rather than awaited in the collector, because `showSnackbar` suspends for the length of
the notice and a collector that waited would hold the next effect behind the current one:
saving a key and clearing history land seconds apart, and queued they would tell the traveller
about the first act while they are looking at the result of the second. That is a paragraph of
reasoning attached to three lines of code, and the whole argument for a host is that a second
copy of those three lines would not carry it. Its `content` lambda takes three: `state`,
`onIntent`, and the `SnackbarHostState` the arrangement has to place — the phone hands it to a
`Scaffold`, the large window aligns it to the bottom of a `Box`, and neither of those is the
host's business.

**A pane is not a Route, and that is the rule that keeps `onIntent` honest on a large window.**
Where the tablet shows one feature inside another — the guide beside the discovery, the
passport and the collection beside the journal's day column — the *host* Route resolves every
ViewModel and the pane below takes `state` and `onIntent` like any other stateless piece.
`JournalTabletRoute` therefore takes three: `viewModel`, `passportViewModel`,
`collectionViewModel`, all from the same back-stack entry, which is also what makes switching
the pane back and forth free rather than re-decoding thirty-four province covers. The
alternative — a `PassportPane` that reaches for `koinViewModel()` itself — is a Route with no
destination, and there would be one per pane before anyone noticed. Named `XPane.kt` rather
than `XTabletScreen.kt` so the file name says which of the two it is.

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

**`isDebug` decides two bindings, not one.** `networkModule(isDebug)` installs Ktor's `Logging`
plugin or does not, and `seedModule(isDebug)` binds either `BundledDemoDataSeeder` or a no-op
`DemoDataSeeder`. Both are bindings rather than an `if` inside the implementation, so a release
build has no path to the code at all — and in the seeder's case the guarantee does not rest on
the flag either: `:app` packages the demo assets into the **debug variant only**
(`stageSeedAssets`), so `release` and `fastRelease` have nothing to seed *from*. The count of
modules is the same for both build types, and `AppGraphTest` pins that: a module that vanished
on one build type would resolve on the other and fail on a device.

Rules:
- ViewModels are `viewModel { }`, never `single { }` — one per `ViewModelStoreOwner`
  (the navigation back-stack entry), cleared with it.
- A ViewModel that reads route arguments takes `savedStateHandle: SavedStateHandle` and is
  registered as `viewModel { params -> XViewModel(savedStateHandle = params.get(), …) }`.
- **A ViewModel a screen composes without a route of its own also takes the argument outright.**
  `ChatViewModel` is the only one: on a large window the guide is a column inside the
  discovery's back-stack entry rather than a destination, so it is resolved as
  `koinViewModel(key = discoveryId) { parametersOf(discoveryId) }` and the binding reads
  `explicitDiscoveryId = params.getOrNull<String>()`. Both work in one `ParametersHolder`
  because Koin's `AndroidParametersHolder` special-cases only `SavedStateHandle` and looks
  everything else up by type. The `SavedStateHandle` path stays as the phone's, and the
  explicit one is not redundant: the discovery route happens to spell its argument the same
  way, so the fallback would answer correctly today and silently stop the day either route is
  renamed. Held to it by `ChatViewModelTest`.
- Both entry points (`SaolaApplication` on Android, `startSaola` on iOS) pass exactly
  `appModules(isDebug)`, so neither platform can get a graph the other does not have.
- Adding a ViewModel means adding **one** `viewModel { }` block here. Adding a use case
  means **one** `factory { }` line.

---

## 7. Navigation — `navigation/Routes.kt` + one shell per branch

Split across the branch line, and the split is the point:

| File | Owns | Why there |
|---|---|---|
| `navigation/Routes.kt` | every route string, every `ARG_*`, `TOP_LEVEL`, `urlEncoded()` | **shared.** One route table, or a deep link works on one form factor and not the other |
| `navigation/TopLevelNavigation.kt` | `isTopLevel()`, `navigateToTopLevel(route)`, `restartAtLens()` | **shared.** "Never stack a duplicate tab" is the app's behaviour, not a bar's; copied into the second shell it becomes a second answer |
| `navigation/SaolaRoot.kt` | the branch fork, and the `NavHostController` both shells share | **shared.** The one composable `MainActivity` and `MainViewController` call |
| `mobile/navigation/BottomDestinations.kt` | `TopLevelDestination` — the four tabs, their icons and labels | a bottom bar is one branch's answer; the tablet puts the same four places on a rail |
| `mobile/navigation/SaolaApp.kt` | the `NavHost`, the scaffold, the bar | the shell *is* the arrangement |
| `tablet/navigation/RailDestinations.kt` | `RailDestination` — the same four, plus `railDestination()`, which is also the rail's *visibility* test | the rail's own list, for the same reason the bar has one — and its own reading of which routes count as a place |
| `tablet/navigation/SaolaTabletApp.kt` | the large-window shell: rail, and the sovereignty seal below it | same, for the other arrangement |
| `tablet/navigation/TabletNavGraph.kt` | the tablet `NavHost`, and `NavBackStackEntry.discoveryId()` | its own file because phases 04–08 each rewrite one `composable` block in it, in parallel |
| `tablet/navigation/TwoPaneScaffold.kt` | `Row { fixed pane · flexible pane }` | the shape three tablet screens share, and the only reader of `PaneWidth`'s content widths |

- String routes with explicit builder functions, not type-safe serializable routes: the
  graph is small and a plain `String` keeps deep-linking trivial.
- Argument names are `const val ARG_*` in `Routes` and are read by
  `savedStateHandle[Routes.ARG_X]` in the ViewModel — never re-typed as string literals.
- **A file path argument goes in a query parameter, not a path segment.** A path is full of
  slashes; escaped slashes stop the route matching. See `Routes.TRANSLATION` and the
  hand-rolled `urlEncoded()` (Android's `Uri.encode` is not multiplatform).
- `TopLevelDestination` owns the bottom bar and `RailDestination` the rail, but **neither
  owns the membership**: that is `Routes.TOP_LEVEL`, and both enums must cover it in the same
  order. Screens reached from *within* a tab (passport, collection) are deliberately not in
  it. The sovereignty seal at the foot of the rail is deliberately not a `RailDestination`
  either — it opens a statement rather than switching place, and as an entry it would light
  up as the selected tab.
- **Navigation is an Effect, never state.** The ViewModel raises `XEffect.OpenY(id)`; the
  Route turns it into `navController.navigate(...)` via the lambda it was given. The rule is
  about a **ViewModel** carrying a navigation flag, and it does not reach a screen choosing
  which composable fills one of its own panes: `JournalTabletScreen` swaps passport for
  collection with a saved enum, exactly as `DiscoveryTabletScreen` swaps its five pages. Doing
  that with a nested `NavHost` would hand the pane a second `NavHostController`, and a nested
  controller registers its back callback **after** the root's — so it would take the system
  back gesture off the shell. An explicit `BackHandler`, switched off the moment the pane is
  home, is the same behaviour without the theft.
- **The rail decides its own visibility, and it is not `isTopLevel()`.** The phone hides its
  bar on anything outside `Routes.TOP_LEVEL`, which is right there: the passport and the
  collection are pushed on top of the journal. On a large window those two *are* the journal —
  three routes, one arrangement, differing only in which pane starts open — so the tablet shell
  asks `railDestination()`, which maps both to `RailDestination.JOURNAL`. Judged by
  `isTopLevel()` the rail vanished the moment a traveller deep-linked into the passport, or
  turned a phone-sized window holding the collection into a large one, and what was left was a
  top-level screen with no way off it but the system back gesture. Reproduced on a Pixel Tablet
  on 04.08.2026 and fixed the same day. `Routes.TOP_LEVEL` is **not** the place to fix it:
  membership there is an app fact, and adding the two would put them on the phone's bottom bar.
- **Several routes may reach one arrangement, and on the tablet five do.** `Routes.CHAT` opens
  `DiscoveryTabletRoute`, not a chat screen: the guide is a column beside the story there, so
  "the chat about d1" and "the discovery d1" are the same picture. `Routes.PASSPORT` and
  `Routes.COLLECTION` open `JournalTabletRoute` with `initialPane` set, for the same reason —
  on a window this size all three are the day column with something beside it. **None of those
  routes is removed**: a deep link has to land on both form factors, a traveller who was on one
  of them when the window changed size arrives through it, and the two shells' graphs have to
  compare structurally equal or `setGraph` clears the back stack on every resize. The discovery
  pair read their id through `NavBackStackEntry.discoveryId()`, which is why the helper exists
  rather than each block spelling out the same `savedStateHandle` read.
- **`DiscoveryEffect.OpenChat` means different things to the two shells, and that is the
  arrangement's business.** The phone navigates; the tablet feeds the question into the guide
  already on screen. The ViewModel says *ask this* and neither branch decides anything about
  the discovery — which is the line §3 draws.
- **A new route is registered in every branch shell that exists, identically.** Nothing
  enforces this — it is a `composable(Routes.X)` block per shell — so adding one to
  `SaolaApp` and not to `TabletNavGraph` shows up as a blank screen on one device and
  nowhere in a build log. The second failure is worse and quieter: `NavController.setGraph`
  compares the incoming graph with the one it holds **structurally**, and only two graphs it
  judges equal take the update-in-place path that swaps each destination's composable and
  leaves `backQueue` alone. One route the other shell does not declare, or a `navArgument`
  default written differently, fails that comparison and the controller answers by clearing
  the back stack — so the traveller is dropped back on the lens every time the window
  changes size. That is the whole reason both graphs list the same nine routes and the same
  three `Routes.TRANSLATION` defaults. Measured on a Pixel Tablet, 04.08.2026: passport open,
  1280 × 800 dp → 337 × 731 dp → back, still on the passport, and back once more returns to
  the journal.
- **One `NavHostController`, created above the fork.** `SaolaRoot` remembers it and hands
  the same instance to whichever shell the window size selects, which is why resizing —
  rotating an iPad, unfolding a fold, leaving split-screen — changes the arrangement and
  nothing else: the traveller stays on the screen they were reading, with their back stack.
  Neither shell may default the parameter; a default lets a caller create a second
  controller by omission, and the symptom is the app jumping to the lens on every rotation.
- **The fork reads the window, not the device.** `core/window/WindowClass.kt` calls it
  EXPANDED at ≥ 840dp wide **and** ≥ 600dp tall, measured by `BoxWithConstraints`. Both
  conditions, because a phone in landscape is ~891 × 411 — wider than the threshold and far
  too short for a 392dp master column beside a detail pane. There is no
  `material3-window-size-class` here: that library is Android-only, and measuring is also
  the only answer that stays true in split-screen.
- **A second question lives in the same file and does not choose a branch.**
  `rememberCanStackVertically(maxHeight)` is `≥ 500dp` and answers *"can a page stack its
  parts down the screen"* — both of its answers are `mobile/`. It exists because a phone
  turned sideways stays COMPACT and the lens cannot draw a column in 384dp: 214dp of chrome
  leaves the viewfinder a 55dp slot. Only `mobile/feature/camera/LensScreen.kt` reads it;
  the other nine phone screens either scroll or fit, checked one by one on a Galaxy A16 at
  832 × 384dp on 05.08.2026. **The two thresholds are deliberately not wired together** —
  600 is what two panes need and 500 is what one column needs, and coupling them would let
  a change to the tablet's gate silently re-lay-out the phone. `WindowClassTest` walks both
  boundaries, including the six points by which an iPad in portrait misses the width gate.

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
│                                     builders (`discovery(id = …)`, `nearbyPlace(id = …)`,
│                                     `translationResult(id = …)`)
├── MainViewModelTest.kt              The window host — splash gate + the startup sweep
└── feature/<name>/XViewModelTest.kt  One per ViewModel. **All twelve have one** since
                                      04.08.2026 — see §11 row #25 for what the eight
                                      missing ones were hiding

androidDeviceTest/
├── testing/DeviceFixtures.kt               `discovery(id = …)` — source sets cannot see
│                                           `commonTest`, so this is the second copy and
│                                           the last one
├── performance/RecompositionTest.kt        skipping, counted on a device
├── feature/translate/…GestureTest.kt       pinch and drag, real multi-touch
├── tablet/TwoPaneScroll.kt                 the two two-pane tests' shared machinery
└── tablet/feature/<name>/…Test.kt          one arrangement claim per file

:app/src/test/
└── AppGraphTest.kt                         the Koin graph, verified without starting it
```

**`:app` has exactly one suite and it is about assembly, not behaviour.** The module is two
files of glue, so what is worth testing is the one thing only it does: compose
`appModules(BuildConfig.DEBUG)` and hand the graph a `Context`. A definition whose constructor
cannot be satisfied does not fail there — every ViewModel is a `factory`, so a missing binding
under the passport stays invisible until somebody navigates to the passport on a device.
`AppGraphTest` resolves every constructor by reflection instead, on the JVM, constructing
nothing. Three things about it are worth knowing before editing it:

- **`verifyAll(modules)` does not do what its name suggests.** In Koin 4.2.2 it is
  `forEach { it.verify(…) }`, so each module resolves against its own definitions alone and
  every legitimate cross-module edge reads as missing. Verify **one** module that `includes`
  the rest; `Verification` flattens included modules first, which is also what `startKoin` sees.
- **Two `extraTypes` lists, and only one of them is a contract.** `injectedTypes` is what the
  host really supplies (`Context`, `SavedStateHandle`); `reflectionBlindSpots` is where the
  checker is wrong — a `single { }` lambda that builds its own argument (`HttpClientEngine`),
  and a generic key the raw constructor parameter cannot match (`DataStore<Preferences>`).
  Adding to the first is a finding. Adding to the second is a workaround, and mixing them is
  how a graph check quietly stops checking.
- **The suite proves it is not vacuous.** One case asserts that a graph with the data layer
  removed *fails*, because the three green ones would look identical if `verify` had stopped
  looking — which is exactly what happened while they were written against `verifyAll`.

**`commonTest` compiles for Kotlin/Native, and two things silently stop it.** Both were live
until 04.08.2026 and both compiled perfectly on the JVM, so `:shared:testAndroidHostTest` was
green throughout — see §11 row #14. First, a JVM-only class: `SecurityException` has no
`kotlin.` twin, and a test needing a platform-flavoured throwable declares its own
`RuntimeException` subclass instead. Second, and less obvious, **a backticked test name may
not contain `,` `.` `;` `:` or brackets** — Kotlin/Native rejects the identifier outright with
*"Name contains illegal characters"* while the JVM accepts all of them. Run
`:shared:allTests`, not `:shared:testAndroidHostTest`, or half the platforms this presentation
layer ships to are never compiled against. Currently **110 on the JVM, 99 on the iOS
simulator** — the difference is the two source-reading gates below, which need `java.io.File`.
Project total **413**, with `:data` at 110 / 76, `:domain` at 10 / 10 and `:app` at 4.

**The device leg runs on API 37 since 05.08.2026** — see §11 row #18. `ui-test-junit4` drags in
`espresso-core:3.5.0` and `androidx.test:runner:1.5.0` even at Compose 1.12, and both are named
explicitly in `libs.versions.toml` purely so Gradle resolves 3.7.0 / 1.7.0 instead. Delete those
two lines and every instrumented test dies in `Espresso.onIdle` before its body runs.

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
- **A suite that cannot use virtual time says so.** `SovereigntyViewModelTest` is the only one:
  its subject hands the parse to `Dispatchers.Default`, a real pool the test scheduler does not
  drive, so `advanceTimeBy` returns before the read has finished and the assertion reads a
  state that is still loading. It uses `runBlocking` with a real `withTimeout` instead. Copy
  this only for a ViewModel that genuinely leaves the test dispatcher — everywhere else the
  virtual clock is what makes the suite fast and deterministic.

**Every flag raised before a suspend call is lowered in `launchSafely`'s `onError` too, and
there is a test that proves the retry works.** Not "the flag comes down" — the suite calls the
repository a second time and counts, because a flag nobody reads would pass the weaker
assertion. Four screens failed this in 04.08.2026's audit; see §11 row #25.

Two source-reading gates live in `androidHostTest`, because both need `java.io.File`:

- `ComposeStabilityReportTest` — asserts against the Compose compiler's own reports (§8).
- `DesignTokenTest` — asserts against the `commonMain` **sources**, as text. A hardcoded
  radius and `MaterialTheme.shapes.large` compile to identical bytecode, so the file is the
  only place the difference still exists. See §13.
  Four of its rules scan feature packages, and since the branch split that means **three
  roots** — `feature/`, `mobile/feature/`, `tablet/feature/`, listed in `BRANCH_ROOTS`. Two
  checks exist purely to stop those rules from passing on nothing: `featureFiles()` fails
  outright on an empty union, and `every presentation branch is actually scanned` fails if a
  root in `POPULATED_BRANCHES` contributes zero files and prints the per-branch count. All
  three roots are in `POPULATED_BRANCHES` since the tablet lens landed on 04.08.2026; adding
  a fourth branch means updating both lists. It prints its own reach on every run — **135
  files: `feature: 116`, `mobile/feature: 10`, `tablet/feature: 9`** as of the tablet settings
  and sovereignty. Read the number; it should move only when files are genuinely added.
  `HEADER_OWNERS` counts a **pane** as a screen: `PassportPane.kt` and `CollectionPane.kt` are
  not destinations, but each opens with a title band over a page, and a hand-rolled header is
  more visible there than on a phone because the traveller sees it beside a compliant one.
  Both of its exclusions carry across the branch line: `SovereigntyTabletScreen.kt` is absent
  for the same reason `SovereigntyScreen.kt` is — the statement is a document with a close
  affordance, not a page with a heading — and `LensTabletScreen.kt` for the same reason
  `LensScreen.kt` is.

Build gates: `tasks.withType<Test>` depends on `compileAndroidMain` so the Compose reports
exist before the stability test reads them, and sets `saola.commonMainDir` so the token
test can find the sources.

---

## 10. Where does a new file go?

| I am adding… | It goes in | Notes |
|---|---|---|
| A screen | `feature/<name>/` — Contract + ViewModel; `mobile/feature/<name>/XScreen.kt` for the phone layout | Register the VM in `presentationModule`. §5 |
| The tablet layout of an existing screen | `tablet/feature/<name>/XTabletScreen.kt` | Layout only. No new Contract, no new ViewModel — reuse the shared ones. Point its route at it in `TabletNavGraph.kt`, and add the branch to `POPULATED_BRANCHES` if it is the first. Full six-step recipe: `docs/large-screen-layout.md` §7 |
| The tablet layout of a screen that is a *pane of another* | `tablet/feature/<name>/XPane.kt` | Stateless: `state` + `onIntent`, no ViewModel. The host Route resolves it. §5 |
| A pane width | `theme/Dimens.kt` → `PaneWidth` | A measured position, not a gap. Pass it to `TwoPaneScaffold`; never type it at a call site. §13.2 |
| A composable two branches both draw | `feature/<name>/component/OneName.kt` | Lift it out of the screen rather than copying it into the other branch, `internal`, one per file. §5 |
| A business rule | `:domain/usecase/XUseCase.kt` | Register in `useCaseModule`. Never in a ViewModel |
| A data model | `:domain/model/` | Immutable `data class`. Add its package to `compose-stability.conf` if new |
| A repository | Interface in `:domain/repository/`, impl in `:data/` | ViewModels depend on the interface |
| Something only a **development build** may do | A port in `:domain/repository/`, two impls in `:data/`, chosen by `xModule(isDebug)` | Never `if (BuildConfig.DEBUG)` inside the implementation — `:data` and `:shared` are single-variant and cannot see it. `seedModule` is the worked example; if it also needs *data*, package that from `:app`'s debug variant so a release APK has nothing to act on |
| A reusable composable | `core/designsystem/component/` | Only if used by ≥2 features |
| A page header | Nothing — call `PageHeader` or `OverlayHeader` | §13. A screen never writes its own; `DesignTokenTest` fails the build |
| A colour / dimension / duration | `core/designsystem/theme/` | Never a magic number in a screen. §13 |
| A text style | `theme/Type.kt` — a scale, or `StampType` | Never a `.copy()` or a `fontWeight` at a call site |
| A string | `composeResources/values/strings.xml` **and** all seven other `values-*/` | All eight, always. `resource_language` in each file names its own locale, and `uiLanguage()` reads it back |
| A platform capability | `expect` in `commonMain`, `actual` in both platform source sets | + a Koin binding in `platformUiModule` if it needs `Context` |
| A pure helper | `core/util/` | Must be testable without Compose |
| A screen-local helper composable | Bottom of the same `XScreen.kt`, `private` | Split to `XComponents.kt` past ~150 lines |

**Naming:** kebab-case for non-Kotlin files, PascalCase for Kotlin. Feature packages are
lowercase single words (`camera`, `chat`, `passport`). The screen is `XScreen.kt` even when
the package is named differently (`mobile/feature/camera/LensScreen.kt`). A feature package
keeps the **same name in all three roots** — `feature/camera/`, `mobile/feature/camera/`,
`tablet/feature/camera/` — so the three halves of one screen are found by one search.

**File size:** target under 200 lines per file (project rule). See §11 row #11 — six screen files
currently break this badly and are the standing refactor backlog.

---

## 11. Known deviations from the standard

These exist in the reference codebase today. **Do not copy them.** Fix them when you touch
the file.

### Open — hygiene

| # | Deviation | Location |
|---|---|---|
| 11 | One screen file far over the 200-line rule: `TranslationScreen` 683. **The branch split raises the cost of leaving this.** It keeps its reusable pieces as `private fun`s, and a `private fun` is invisible to `tablet/` — so a tablet branch either gets the piece lifted into `feature/<name>/component/` first, or copies it. The lift is the first step of any tablet screen, not a cleanup to be scheduled. **Five files have come off this list**: `LensScreen` and `DiscoveryScreen` on 04.08.2026 (rows 11a, 11b), then `PassportScreen` and `JournalScreen` the same day (row 11c), then `SettingsScreen` (row 11d). Translation is the one screen with no tablet arrangement — it is a photograph with the Vietnamese on it replaced in place, the same picture at any window size — so nothing has forced its lift yet. It is phase 10's, when the phone's screens are read in landscape. | `mobile/feature/translate/TranslationScreen.kt` |
| 16 | **The conversation with the guide still ignores dark mode**, and deliberately so for now. `plans/260803-1118-ui-standardisation/plan.md` phase 3.6 called for deleting `HeaderBackground` / `InkMuted` and taking the header's colours from the scheme. The header was only ever half the story: the page, the composer, the guide's bubbles and the thinking card are fixed to the lens palette too, and the phone pins the system bar icons to dark to match — protected by §12. Converting the header alone would put a scheme-dark bar on a cream page. **Half of the fix landed on 04.08.2026:** the seven tints are now `GuidePalette` in `Color.kt`, beside the lens and flag ones, because the tablet's guide column draws the same conversation and two branches cannot each own a copy of its colours. What is left is the decision itself — convert the whole conversation to the scheme, or state in `Color.kt` that it is fixed on purpose the way the lens palette does. Note that on a large window the cream column now sits beside a scheme-coloured story pane, which is the first place the two systems meet on one screen. | `core/designsystem/theme/Color.kt` (`GuidePalette`) |
| 17 | `TranslationScreen` is allowlisted in `DesignTokenTest` for `.sp` alongside the two map canvases. Its use is legitimate — `autoSize` steps a translated block's type down to fit the Vietnamese line it covers — but the allowlist is per *file*, so a genuine hardcoded size added anywhere else in those 700 lines would pass. Narrow it to the composable if the file is ever split. | `mobile/feature/translate/TranslationScreen.kt:409-419` |
| 19 | **`DesignTokenTest` scans `feature/` for four of its six rules** — gap, corner, weight and type size — while only the inset rule walks all of `commonMain`. A literal radius or a call-site `fontWeight` added under `core/designsystem/component/` is therefore invisible to the gate, which is the one place a bad value would spread furthest. The narrow scope was deliberate (a design-system component owns its own internal geometry, per `Dimens.kt`), but *internal padding* and *a sixth corner radius* are not the same exemption. Verified clean by hand on 03.08.2026 — zero corner literals, zero call-site weights, zero `.sp` outside `Type.kt` — so this is a latent hole, not a live one. Fix by scanning `designsystem/` for corner and weight while leaving the gap rule at `feature/`. | `androidHostTest/…/DesignTokenTest.kt:354` |
| 22 | **Seven placeholder languages still say Vietnamese.** `AppSettings.DEFAULT.language` and the `language` default on the Journal, Chat, Translation, Discovery, Passport and Explore contracts are all `AppLanguage.VIETNAMESE` — the value a screen holds for the few milliseconds before the settings flow delivers the device's answer. That was coherent while Vietnamese was the app-wide default; since narration follows the phone, English is the fallback everywhere else (`languageForTag`, `uiLanguage()`, the `values/` string table), so a Japanese phone can draw one frame of `12 thg 3, 2026` before flipping. Not new in kind — anyone who had picked English in the old picker saw the same flicker — only the affected population changed. No clean fix available where it sits: `deviceLanguage()` is `internal` to `:data` and a contract has no composition to call `uiLanguage()` from, so this needs either a `:domain`-level device-language port or an initial value threaded from DI. | `domain/…/AppSettings.kt:33`, `feature/*/XContract.kt` |
| 27 | **`SovereigntyViewModel` calls `Res.readBytes` directly**, which is a presentation-layer ViewModel reaching for a concrete resource API instead of depending on a port. It is the only ViewModel in the project with no injected collaborator, and therefore the only one whose subject cannot be faked: `SovereigntyViewModelTest` can assert that a map which fails to load still renders the statement, and nothing else — the success path has no test on any platform. Everything else in the app that reads an asset already goes through `:data` (`ProvinceAssetSource`, `CatalogAssetSource`). Fix by adding a `SovereigntyMapSource` port to `:domain` with the `Res.readBytes` implementation in `:data`, bound in `dataModule`. | `feature/sovereignty/SovereigntyViewModel.kt:31` |
| 21 | `docs/bug-report-effect-collection.md` does not exist, but `plans/260802-2103-mvi-refactor/plan.md` refers to it four times as the home for the deferred UI work. Either write it or drop the references. **Renumbered from 15 on 03.08.2026:** a *different* deviation in the Fixed table below already held that number, and seven places cite `§11 row #15` meaning that one — including live comments in `JournalContract.kt`, `JournalViewModelTest.kt` and `SettingsViewModelTest.kt`. A number cited from source code is not free to reuse. | `plans/260802-2103-mvi-refactor/plan.md` |

### Fixed

| # | Deviation | Fixed by |
|---|---|---|
| 29 | **The passport opened with an empty province panel already peeking**, its drag handle standing 80 dp up the screen with the sovereignty banner hidden behind it, on a map where nothing had been selected. A hidden sheet is parked immediately below the *scaffold*, not below the window, and `PassportScreen`'s scaffold changes height once on its own: the screen is pushed from the journal, where the shell's tab bar is on screen, and `SaolaApp` hands its 80 dp back a frame or two later when the bar finishes sliding away. The sheet does not re-settle onto the `Hidden` anchor that moves with it — it stays where the bottom edge *used to* be. **Only visible with animations off**, which is why it survived months of being looked at on a phone: with them on, the bar's height comes back long after the sheet has settled and it lands correctly by luck of timing. Every AVD ships with all three animation scales at `0`, and so does a real device in battery saver or with the developer setting off — this was never emulator-only. | **Fixed 05.08.2026:** the hide is keyed on the scaffold's own height as well as on the selection, so every height the scaffold takes puts the sheet back on the real bottom edge. `PassportPane` is deliberately **not** given the same line: the large-window shell stands its navigation on a rail, so that scaffold is one height for the life of the screen — verified at 1067 × 667 dp with the scales at 0, banner fully visible and no handle. Measured rather than eyeballed both ways: with the scales at 0 the sheet's top edge sat at `y = 2840` of a 3120 px window (280 px = the tab bar's 80 dp) and `uiautomator dump` reported a `Nút kéo` node; after the fix there is no such node and the banner reports its full `[56,2812][1384,3064]`. Selecting a province still peeks at exactly `SheetPeekHeight`, tapping across the map keeps the dragged height, and back still dismisses — all re-checked with the scales at 0 *and* at 1. |
| 18 | **`androidDeviceTest` could not run on API 37.** Every instrumented test failed in `Espresso.onIdle` with `NoSuchMethodException: android.hardware.input.InputManager.getInstance` — a reflection call Espresso makes that the platform removed — before any test body executed. The row was rescoped on 04.08.2026 after being wrong in the expensive direction: it had said "Android 15 or newer", so for two plans nobody ran the device leg at all, when in fact API 35 and 36 were green the whole time. | **Fixed 05.08.2026.** It was never a Compose problem and upgrading `ui-test-junit4` would not have helped — at Compose **1.12** that artifact still declares `espresso-core:3.5.0` and `androidx.test:runner:1.5.0`, both 2022 artifacts, and nothing else in the graph was high enough to win the conflict. Naming them explicitly in `libs.versions.toml` and adding them to `:shared`'s device suite and `:app`'s `androidTest` is what lets Gradle resolve **espresso 3.7.0 / runner 1.7.0**. Proved both ways on the same Pixel_7_Pro API 37 AVD in the same hour: with 3.5.0 the run dies with that exact `NoSuchMethodException`, with 3.7.0 it is **12 / 12 green**. Also still 12 / 12 on a Galaxy A16 at API 36, so nothing was traded away. |
| 26 | **A note that fails to save said nothing at all** — and, on the path nobody had looked at, said nothing *and closed the composer*. `DiscoveryViewModel` discarded the `AppResult` of all four of its writes, so an ordinary handled failure fell straight through to the success branch: `saveNote` cleared `noteEditor` one statement later, which threw the traveller's own writing away in the one place it existed; `deleteDiscovery` sent `NavigateBack` regardless, dropping them into a journal that still listed the discovery; `deleteNote` and `toggleFavorite` were silent. The row as written named only the note and only the throw path, because the throw path was the only one the suite could reach. | **Fixed 05.08.2026:** `DiscoveryEffect.ShowMessage` exists and every write raises it through one private `report(AppError)` — eight call sites otherwise, which is how four silent failures accumulated on one screen in the first place. State keeps no `error` field, per row #15: the routes resolve the text off the effect's payload with `userMessage()`. The composer closes on `onSuccess` and nowhere else, and `NavigateBack` likewise. Both arrangements gained an `AppSnackbarHost` — the phone's against the bottom edge under `imePadding()` so a failed save lands over the composer it is about, the tablet's centred on the window at `PaneWidth.sheet` because a notice pinned inside one pane reads as being about that pane alone. Covered by six new cases in `DiscoveryViewModelTest`, each driving *both* failure paths — the fakes gained `failOn…` hooks beside their `throwOn…` ones, because no `throwOn…` can reach the branch that caused this. |
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
| 23 | **`:app`'s instrumented suite did not compile.** `RecognitionEndToEndTest` called `settingsRepository.setLanguage(AppLanguage.ENGLISH)` at the head of three cases, and that method was deliberately deleted in `ef5c6a9` when narration started following the phone's own language — `SettingsRepository:35` carries a comment saying there is no `setLanguage` and why. The test file was unchanged since `09401ed`, so this was drift left by the i18n change. Distinct from row #18, which is about the API those tests can run on; this one was a compile failure. | **Fixed 03.08.2026:** the three calls are gone, along with the now-unused `settingsRepository` field and two imports. Nothing else had to change — every assertion in the file was already written to match its subject across both the English and the Vietnamese name ("temple of literature" *or* "văn miếu"; "noodle soup" *or* "phở"), because a title-only assertion would have punished the more precise answer. That is now stated in the class KDoc as a rule rather than left as a property the next edit could quietly break. Verified by `:app:compileDebugAndroidTestKotlin --rerun-tasks`; running the suite needs a device on API ≤ 36, per row #18 as it now reads. |
| 11a | **`LensScreen` was 2 183 lines, the largest file in the project.** | **Fixed 04.08.2026** by the tablet lens (plan phase 04): **388 lines.** Fourteen composables came out into `feature/camera/component/`, one per file — shutter, mode chips, tool row, viewfinder, focus reticle, zoom dial, translate bar, language sheet, the two overlays, the hint bubble, the gallery button, the two permission prompts and the two snackbars. What stayed in `mobile/` is what is genuinely the phone's: the column, the shutter row, and `RecentCaptureStack`, whose KDoc now says why the pile does not travel to a window with vertical space to spare. The Route's non-layout half went to `feature/camera/LensHost.kt` — see the exception in §5. Cost one slot in `UNSTABLE_CLASS_CEILING` (20 → 21): `ZoomDriver` was `private` and so invisible to the Compose report, and is now counted; the note in `ComposeStabilityReportTest` says why no `compose-stability.conf` entry could honestly fix it. Verified by installing on a Pixel Tablet and on a Pixel 7 and taking a photograph on each: permission prompt, capture, analysing overlay, the Gemini round trip and the failure snackbar all behave as before on the phone, and the tablet scopes its overlays to the frame so the panel stays readable. |
| 11b | **`DiscoveryScreen` was 1 986 lines, the largest file left after the lens.** | **Fixed 04.08.2026** by the tablet discovery (plan phase 05): **627 lines.** Twenty-one composables came out into `feature/discovery/component/` and six into `feature/chat/component/`, one per file, `internal`. What stayed in `mobile/` is what is genuinely the phone's: the one-column order, `PhotoHeader` with the paper lip over the foot of a full-bleed photograph, and `AskPill` — the button that exists only because on a phone the guide is a screen away. The extraction cost `DiscoveryTabletRoute` two allowlist entries in `ComposeStabilityReportTest`: it is the only Route in the app with two ViewModels, because it is the only screen that shows two features at once. No `UNSTABLE_CLASS_CEILING` change — nothing that moved was a class. |
| 13 | **Two Routes still rendered their page themselves** — `SovereigntyRoute` and `SettingsRoute` — so neither could be previewed. Not fixed with the MVI conversion because extracting a composable changes its recomposition scope, which is feature work rather than a structural rename. This row said "the only feature" until 04.08.2026 and was wrong: journal, passport and collection were in the same state and nobody had checked, which is what a claim with no test behind it is worth. | **Fixed 04.08.2026** by the tablet settings and sovereignty (plan phase 08). Both now split, and the split is what the large window needed rather than a tidy-up: `SovereigntyRoute` and `SovereigntyTabletRoute` each resolve the ViewModel and hand `state.map` to a stateless page, and `SettingsRoute` and `SettingsTabletRoute` are each one call to `SettingsHost` plus an arrangement. The row's own warning about recomposition scope was taken at its word — the statement was re-checked by eye on a phone before anything else in the phase was written, and again at the end. **All ten features now have the Route/Screen split**, which is the claim §12 used to make with two exceptions attached. |
| 11d | **`SettingsScreen` was 755 lines** — the last of the six over-long screens with a large-window arrangement waiting on it. | **Fixed 04.08.2026** by the tablet settings (plan phase 08): **178 lines.** Eight composables came out into `feature/settings/component/` — the key card, the model picker, the card container, the three rows, the theme picker and the two dialogs — plus the footer, which is the one place the version number is rendered. `ThemeRow` is the shape worth copying: the row *and* the dialog it opens are one component holding one `rememberSaveable`, because "which picker is open" is a fact about the control rather than about either arrangement, and split across the branches the flag would have been declared twice. What stayed in `mobile/` is the phone's one-column order and nothing else. |
| 11c | **`PassportScreen` was 1 059 lines and `JournalScreen` 840** — the two largest files left after the discovery, and between them the whole of the journal tab. | **Fixed 04.08.2026** by the tablet journal (plan phase 06): **258 and 248.** `CollectionScreen` came with them, 497 → 147, because on a large window the collection is the pane next to the days and every one of its tiles would otherwise have been copied. Twenty-four composables came out into `feature/journal/component/` (9), `feature/passport/component/` (11) and `feature/collection/component/` (4), and `DashedInk.kt` moved up to `core/designsystem/component/` when the passport turned out to hold a byte-identical private copy of `DashedRule`. All three Routes gained the `Route`/`Screen` split they were missing — see row 13. Two of the lifts are `LazyListScope` extensions rather than composables, for the reason §5 now gives. Verified on a Pixel Tablet at 1280 × 800 dp and on a Galaxy A16 at 411 × 890 dp: both panes, the pane switch, the province panel peeking inside a pane, and the phone's three screens unchanged. |
| 24 | **A chat request that threw left the thinking card up for good.** `ChatViewModel.send` raised `isSending` and lowered it in both `AppResult` arms, but its `launchSafely(onError = …)` wrote only `error`. So a handled failure recovered and an *unwrapped* one — a corrupt Room row, a mapper meeting a column an older version wrote — left the card on screen with nothing to take it down, and the composer disabled behind it because `canSend` reads the same flag. The traveller's only way out was to leave the conversation. Pre-dated this plan; the file was unchanged since the MVI refactor. | **Fixed 04.08.2026:** `onError` now lowers `isSending` too. Found by writing `ChatViewModelTest`, which the chat had never had — the "stuck states" row of the MVI doc's §7 table is exactly this case, and it was the one category the suite did not cover for this feature. |
| 25 | **Four screens stranded a busy flag on the unwrapped-throw path, and three of those flags also guarded re-entry** — so the screen refused the one action that would have cleared it. `ExploreViewModel.search` left `isLoading` / `isRefreshing` up and `search` returns early while either is, which retired **refresh and retry for the life of the process**; `ExploreViewModel.selectPlace` left the place sheet spinning; `TranslationViewModel.translate` drew an error card under a spinner still turning; and `DiscoveryViewModel.SaveNote` was the worst — it took `launchSafely`'s *default* `onError`, so the failure was logged and nothing else, leaving the traveller's own writing in a composer whose save button had permanently stopped working, with no message anywhere on screen. Identical in kind to row #24 in the chat, and to the case `LensViewModel:179` and `JournalViewModel:85` each already carry a comment about. **Nobody had looked because eight of the twelve ViewModels had no test at all** — camera, chat, journal and settings were the only four covered, and none of the four defects is reachable without a repository that *throws* rather than returning `AppResult.Failure`. | **Fixed 04.08.2026** by `plans/260804-2320-stranded-flags-and-coverage/`. Each `onError` now lowers its own flag, and Explore's details handler is guarded on the place id so a late throw cannot clear a sheet the traveller has since reopened elsewhere. The composer is deliberately **not** closed on failure — that would discard the words in the only place they exist. Found by writing the eight missing suites: `ExploreViewModelTest`, `DiscoveryViewModelTest`, `TranslationViewModelTest`, `PassportViewModelTest`, `CollectionViewModelTest`, `SovereigntyViewModelTest`, `MainViewModelTest` and the `ChatViewModelTest` that already existed. `:shared` went from **41 JVM / 30 iOS to 95 / 84**, and the project total from 277 to 385. Every one of the four fixes is pinned by a test that calls the repository a *second* time and counts, because lowering a flag nothing reads would pass a weaker assertion. Two intent-fuzz suites came with them, on Explore and Discovery, per the §9 rule. Re-verified on all four targets and `:shared:connectedAndroidDeviceTest` still 12 / 12. |
| 28 | **`core/designsystem/component/Components.kt` was a 446-line grab-bag of nine unrelated composables** — `LoadingState`, `ErrorState`, `EmptyState`, `AccentChip`, `ShimmerBox`, `Kicker`, `BackChip`, `FillGauge`, `SectionHeader` — in the one directory whose stated convention is one composable per file. §5 argues that convention for `feature/*/component/` and five refactors had already applied it there; the design system itself was the last place still contradicting it, which is the worst place for it, because this is where a reader goes to find out what a shared component looks like. | **Fixed 04.08.2026:** nine files, named after what they draw, plus `SurfaceLuminance.kt` for `isLightSurface` — which was private to the grab-bag while `AppAsyncImage` also needed it. **Not one call site changed**: the split is inside a single package, so every import already resolved. Each private constant travelled with the only composable that read it. |
| 14 | **`commonTest` did not compile for Kotlin/Native, so `:shared:allTests` had never been green.** Only the Android host leg ran; `compileTestKotlinIosSimulatorArm64` failed, and with it every ViewModel test on the platform half of this presentation layer ships to. | **Fixed 04.08.2026** by the tablet plan's test phase — and it was **two** defects, the second hidden behind the first. (a) `LensViewModelCrashTest:187` raised a `SecurityException`, a JVM class with no `kotlin.` twin; it now declares a local `PermissionRevoked : RuntimeException`, which says which failure is being simulated more plainly than the platform type did — `launchSafely` catches by `Throwable` either way. (b) With that gone the compiler reached `JournalViewModelTest:120`, whose backticked name read *"…is reported, not swallowed"*: **Kotlin/Native rejects `,` in an identifier** — *"Name contains illegal characters"* — while the JVM accepts it. Renamed to "…rather than swallowed". Now **41 tests on the JVM and 30 on the iOS simulator, both green**; §9 states the two rules so the next test does not reintroduce either. |
| 20 | **Five strings rendered their own escape characters** — `100%% MATCH` on the discovery badge, `N%% collected` / `N%% explored`, and `Turn today\'s one find into a story` on the journal card. `strings.xml` was written in Android `aapt` conventions, where `\'` is unescaped at build time and `String.format` collapses `%%` to `%`; Compose Multiplatform's resource reader substitutes positional arguments but does neither. The Vietnamese file already had it right — `Khớp %1$d%` with one percent — which is what confirmed the English was the anomaly rather than the renderer. | **Fixed 03.08.2026:** write `%` and `'` directly. `\n` is **not** part of this — it *is* unescaped by Compose Multiplatform, proven on device by `sovereignty_seal` rendering "CHỦ / QUYỀN / VN" on three lines, so `discovery_share_body:119` and `sovereignty_seal:348` were deliberately left alone. All four visible cases re-checked on device. |

---

## 12. What is already right — keep it

Do not "improve" these; they are deliberate and documented in the code:

- `Channel`-based effects with the exactly-once guarantee (`LensViewModelCrashTest` asserts it).
- `launchSafely` on every screen action, with `CancellationException` rethrown.
- One effect collector, `core/mvi/CollectEffects.kt`: `collect`, `repeatOnLifecycle(STARTED)`,
  handler held through `rememberUpdatedState`. Never hand-roll the block again — doing so is
  how two screens ended up with no collector at all.
- Route/Screen split — all ten features, since §11 row #13 closed on 04.08.2026. No screen
  renders its page from its Route any more, and none should start: a Route that draws is a
  Route the other branch cannot arrange and nobody can preview.
- Derived state as computed `val`s on the state class (`isBusy`, `visibleDays`, `hasMap`),
  never duplicated fields.
- Ids in state, not objects (`selectedItemId`, `selectedPlaceId`) — so a background refresh
  re-renders the open sheet instead of leaving a stale copy.
- Structural job ownership: the stage ticker is a *child* of the analysis job, not a field.
- Cancel-and-replace for superseding requests (`analysisJob?.cancel()`, `detailsJob?.cancel()`).
- `AppResult` / `AppError` all the way up; no exceptions across layer boundaries.
- **A capture is stored by name, never by path.** `discoveries.imageName`,
  `translations.imageName` and `discovery_notes.photoNamesJson` hold a bare
  `capture_<millis>.jpg`; `CaptureStore.nameOf` strips a path down to one on the way in and
  `CaptureStore.resolve` rebuilds an openable path on the way out, in the entity → domain
  mappers. Resolve late and never store the answer: on iOS the app's container is re-homed
  under a fresh UUID on reinstall, update or restore, so a path written yesterday names a
  directory that is gone. This is not theoretical and it did not merely break image
  loading — `CaptureMaintenanceImpl` compares disk against database, found the two sides
  had no name in common, and deleted every photograph in the app. The sweep now works in
  names on both sides and refuses to delete anything when it recognises none of the files;
  `RepositoryFailureTest` holds it to both. Android is not exposed (`context.filesDir` is
  fixed by package name) but implements the same contract, because one port with two
  meanings is how this comes back.
- Two headers and no third: `PageHeader` and `OverlayHeader`. The temptation is a
  `titleStyle` parameter — that is how the five hand-rolled headers came back last time.
  **Each branch composes its own header rather than sharing a configured one.** The component
  is the shared thing; what goes in its slots is arrangement, and the discovery proves it: the
  phone's chat header carries a back chip because the story is a screen behind it, and the
  tablet's guide column has no back chip because the story is the column to its left. A shared
  `ChatHeader(…)` wrapping `PageHeader` would need a parameter per difference and would end up
  a third header with a mode — and it would put the two branch screens one level away from the
  `PageHeader(` call `DesignTokenTest.HEADER_OWNERS` looks for.
- **The system bars: status shown, navigation hidden, and the icon colour owned in one place.**
  `MainActivity.hideNavigationBar()` hides exactly one bar — the app's tabs and shutter row
  already sit where it would be, while the clock and the battery are worth more than the strip
  they cost. The status bar is transparent with no scrim of its own and every page runs *under*
  it, which is why a screen's background goes on before `screenInsetsPadding()` and never after:
  reversed, the page starts below the bar and leaves a band of window colour across the top.
  The icons follow the theme through `DefaultSystemBarIcons`, called once by `SaolaTheme` —
  **not** by either host, because a host repeating the `ThemePreference` `when` is how iOS and
  Android come to disagree about what dark mode means. `PinSystemBarIcons` is the override for
  the two screens that paint their own background regardless of the theme, and it sits *over*
  that base rather than replacing it: the cream chat and the lacquer-red statement, and nothing
  else. An immersive screen is deliberately not on that list — the lens and the translation take
  the theme's answer like every other page. §11 row #16.
- `GuidePalette` in `Color.kt` — the conversation's seven fixed tints, read by the phone's chat
  screen and the tablet's guide column. Not seven `private val`s at the top of a screen again;
  that is how the two would end up two shades of cream apart. §11 row #16.
- `feature/sovereignty/component/SovereigntyInk.kt` — the same move one size down, and the
  reason it is not in `Color.kt` beside the palette above. `SeaWash` and the three alphas are
  *derived* from `Vermilion` and `PaperCream`, and only one feature draws them, so the design
  system would gain four names it could never be asked about. What matters is that they are in
  one file rather than in five component files and two branches: the statement's whole claim is
  that it looks identical on every device, and a second copy of a composited wash is a second
  shade of it.
- `SHUTTER_INSET`, `COMPOSER_CLEARANCE`, `SheetPeekHeight` and the stamp geometry are
  *measured positions*, not gaps. They are named `private val`s on purpose and must not be
  snapped onto the `Spacing` scale; the shutter one is a hit target nothing covers.
- **The Explore map's four warm-up pieces.** They look like ceremony and each one is a
  measured frame budget — see `docs/android-mvi-best-practices.md` §8 for the full
  argument. (a) `PlaceMap.android.kt` loads the Maps renderer on `Dispatchers.IO` behind a
  process-wide flag and gates `GoogleMap` on it; deleting the gate puts most of a second
  of Play-services work back on the main thread, once per process, on the frame the tab
  opens. (b) **Both** arrangements compose the map as soon as the *permission* is answered,
  not when the first fix lands, and draw their loading and failure states over it as opaque
  covers — that is what buys the engine its head start. (c) `Modifier.mapCover()` carries
  an empty `pointerInput`, without which a drag on the cover pans the map underneath it —
  and the rule is not the cover's alone: **anything a branch floats over the map carries the
  same guard**, which on a large window is the results column, a scrolling surface that
  would otherwise pan the map every time the traveller ran out of list. (d) Both platforms
  apply the **first** camera request without animating; composing early means the map opens
  on the Hanoi fallback, and animating that flies the traveller in from the capital every
  time they open the tab.
- `val onIntent = remember(viewModel) { viewModel::onIntent }` in `ExploreHost`, and only
  there. A ViewModel is `unstable`, so the bound reference cannot be memoised and is a new
  object every recomposition — which denies every child below it the skip its `skippable`
  mark promises. Worth the line only where the subtree is expensive; on Explore it is a
  map. It sits in the host rather than in either branch's Route precisely because it is the
  kind of line that gets left out of the second copy. MVI doc §8.

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
object PaneWidth   { rail 104 · lensPanel 310 · guide 352 · journalList 392 · sheet 440 }
val ScreenGutter = Spacing.lg
```

`PaneWidth` is the large-window arrangement and is **not on the spacing scale** — see the
paragraph below. Each value was read off the tablet wireframe's 1218 px inner frame, and the
frame is what they are measured against: 1218 − 104 = 1114 for content, of which the guide
column is 31.6%. `TwoPaneScaffold` is the only file that reads the three content widths.

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
column takes the inset. Both exclusions carry to the large window, where the same two screens
have tablet arrangements and neither draws a header either.
`DesignTokenTest.HEADER_OWNERS` lists the fourteen files that must comply — the eight screens,
their tablet arrangements where one exists, and the two panes — and states every exclusion in
its KDoc; the test also fails if a name in that list stops matching a file, so renaming a
screen cannot quietly drop it from the check.
