# Large-window layout

> How `:shared` draws itself on a window big enough for two panes, and the one rule that keeps
> that from becoming a second app. Structure map: [`LLM.md`](../LLM.md) §3, §7, §13. How to
> write a screen: [`android-mvi-best-practices.md`](android-mvi-best-practices.md).

---

## 1. The rule

> **The large window is allowed to *arrange* differently. It is not allowed to *think*
> differently.**

Same data, same states, same components, same words — different places. Everything that
decides what the app knows or does is shared: Contract, ViewModel, use case, repository,
`Routes`, the design system. A file under `tablet/` receives `state`, emits `onIntent`, and
puts composables somewhere.

The reason is arithmetic. Ten features, ten ViewModels, eighteen arrangements of them — ten
phone screens and eight large-window ones. A defect in
what a screen *knows* is fixed once and both form factors get it; only a defect in where
something *sits* is fixed twice. Let one `tablet/` file own a decision and the project has
twenty ViewModels wearing ten names — and the second copy is the one nobody re-reads when the
bug report comes in.

Nothing mechanical stops a branch from breaking this: the split is a package convention, not a
Gradle boundary. What enforces it is that a lifted component is *cheaper* than a copied one,
plus this document and the pre-PR checklist in the MVI doc §9.

---

## 2. Which branch draws — `core/window/WindowClass.kt`

```kotlin
EXPANDED  ⟺  maxWidth ≥ 840.dp  &&  maxHeight ≥ 600.dp
```

Measured from `BoxWithConstraints` at the root of the composition, not asked of the device.

- **Both conditions, and the height is the one people forget.** A phone held sideways is
  ~891 × 411 dp — wider than the threshold and nowhere near tall enough for a 392 dp master
  column beside a detail pane.
- **No MEDIUM.** The wireframe was drawn at exactly one size, 1194 × 834. A third arrangement
  nobody designed is a third arrangement to keep in step.
- **Not `material3-window-size-class`.** That library is Android-only, and this file answers
  the same question on iOS. Measuring is also the only answer that stays true in split-screen,
  where the app owns a fraction of a display it could otherwise ask about and be misled by.

Measured 04.08.2026, and worth knowing before arguing about the threshold:

| Window | Class | Note |
|---|---|---|
| iPad Pro 11″ landscape · 1210 × 834 | EXPANDED | the wireframe's own size |
| iPad Pro 11″ portrait · 834 × 1210 | COMPACT | **six points** short of the width |
| Pixel Tablet landscape · 1280 × 800 | EXPANDED | |
| Pixel Tablet portrait · 800 × 1280 | COMPACT | |
| Phone landscape · 853 × 533 | COMPACT | wide enough, too short — the height clause working |
| Tablet split-screen · 437 × 500 | COMPACT | |
| Phone portrait · 384 × 832 | COMPACT | |

An iPad in portrait therefore draws the phone's bottom bar. That is the threshold behaving as
specified, not a bug — but it is the single most surprising consequence of it, so anyone
changing 840 should know they are changing that.

### 2.1 The second question — `rememberCanStackVertically`

```kotlin
canStack  ⟺  maxHeight ≥ 500.dp
```

**This does not choose a branch.** Both of its answers are `mobile/`; it chooses an
arrangement *inside* the phone branch, and it is the answer to a question the table above
raises and does not settle: a phone held sideways is COMPACT, so what does `mobile/` draw in
832 × 384 dp?

Nine of the ten phone screens draw exactly what they always did. They were checked one at a
time on a Galaxy A16 at that size on 05.08.2026, and each survives for a reason worth writing
down rather than re-deriving:

| Screen | Why it survives sideways |
|---|---|
| Journal, Collection, Settings, Sovereignty, Discovery | scroll — a `LazyColumn` or `verticalScroll` does not care how tall the window is |
| Explore | full-bleed map with floating chrome; the map fills whatever it is given |
| Passport | `VietnamMapCanvas` fits to `min(width/worldWidth, height/worldHeight)`, so a short window draws Vietnam smaller and complete |
| Translation | the photograph scales; the overlay is positioned on the image, not on the window |
| Chat | composer takes `imePadding()` under `adjustResize`, which is what the keyboard needs and landscape needs more |

The lens is the tenth and it fails, badly enough that it is the whole reason this threshold
exists. Its column needs **214 dp of chrome** before the picture gets a pixel — a 44 dp tool
row inside `Spacing.sm` (60), a chip row inside `Spacing.md` (68), a 78 dp shutter with
`Spacing.sm` under it (86) — which in 384 dp leaves the viewfinder a **55 dp letterbox strip
with the zoom dial floating in it**. 500 is 214 plus 280 for a frame that still reads as a
viewfinder rather than a slot, rounded.

`mobile/feature/camera/LensScreen.kt` therefore holds two arrangements, `StackedLens` and
`SideBySideLens`, sharing every component including the frame. The sideways one reaches the
same conclusion the tablet did — picture takes the width, shutter takes the trailing edge —
from a different premise: the tablet moves the shutter there because a tablet is held at both
side edges, the phone because there is no height left. **The phone's control column claims no
width of its own**; it measures to the 78 dp shutter plus a gutter either side, so there is no
landscape twin of `PaneWidth.lensPanel` to keep in step with anything.

**500 and 600 are deliberately not the same number and not wired together.** 600 is what two
panes need; 500 is what one column needs. Coupling them would mean a change to the tablet's
gate silently re-laying-out a phone. `WindowClassTest` walks both boundaries.

---

## 3. One controller, two shells

`navigation/SaolaRoot.kt` remembers **one** `NavHostController` above the fork and hands the
same instance to whichever shell the window selects. Rotate an iPad, unfold a fold, leave
split-screen: the arrangement changes and nothing else — same screen, same back stack.

Two constraints come with that, and both have already cost a device session:

1. **Neither shell may default the `navController` parameter.** A default lets a caller create
   a second controller by omission, and the symptom is the app jumping back to the lens on
   every rotation.
2. **The two graphs must compare structurally equal.** `NavController.setGraph` compares the
   incoming graph with the one it holds and only takes the update-in-place path — swap each
   destination's composable, leave `backQueue` alone — when it judges them equal. One route the
   other shell does not declare, or one `navArgument` default written differently, fails that
   comparison and the controller answers by clearing the back stack. So both graphs list the
   same **eleven** routes — lens, journal, collection, passport, sovereignty, explore,
   settings, licences, discovery, chat, translation — and the same three
   `Routes.TRANSLATION` defaults, and `Routes.PASSPORT` / `Routes.COLLECTION` stay registered
   on the tablet even though they open the journal there.

The rail decides its own visibility through `railDestination()`, **not** `isTopLevel()` — on a
large window the passport and the collection *are* the journal, and judged by `isTopLevel()`
the rail vanished the moment a traveller deep-linked into the passport. `LLM.md` §7 has the
reproduction.

---

## 4. Pane widths — `theme/Dimens.kt` → `PaneWidth`

```kotlin
object PaneWidth { rail 104 · lensPanel 310 · guide 352 · journalList 392 · sheet 440 }
```

Read off the wireframe's 1218 px inner frame: 1218 − 104 = 1114 for content, of which the guide
column is 31.6%. They are **measured positions, not gaps**, which is why they are not on the
`Spacing` scale — see `LLM.md` §13.2.

`TwoPaneScaffold` is the only file that reads the three content widths, and it reads them from
a caller: a screen names the width it wants and hands it over, so changing the proportions is
one file's work rather than five screens'. **Never type a pane width at a call site.**

`sheet` is the width cap for the two `AlertDialog`s, not a pane — a dialog allowed to grow to
1194 dp is a line of text the eye cannot track back from.

| Screen | Fixed pane | Side | Flexible pane |
|---|---|---|---|
| Lens | `lensPanel` 310 | end | viewfinder |
| Discovery | `guide` 352 | end | the story |
| Journal | `journalList` 392 | **start** | passport or collection |
| Explore | — | — | full-bleed map with two floating clusters |
| Settings | — | — | two columns |

The journal is the only screen whose measured pane sits at the start: a list of days is the
index, and the pane beside it is what the index opens. Reversed, the eye would cross the detail
to reach the thing that chooses it.

---

## 5. Approved exceptions

Two, both narrow, both argued in the file that takes them. Add to this list only with the same
kind of argument — a reason that is about *this* screen and would not generalise.

**`tablet/feature/camera/RecentScanList.kt`** — the one place the large window shows the same
data in a different shape. The phone stacks recent captures into a pile because a scrolling
strip would claim a whole row of a viewfinder that has no rows to spare; that argument is about
a phone. The panel here is 310 dp wide and window-tall, mostly empty below the shutter, so the
constraint the pile answers does not exist. Everything drawn is still the phone's —
`RecentCaptureCard` at depth zero — and only the arrangement around it is new. **This is the
line:** re-shaping a list is arrangement, showing a field the phone does not show would not be.

**Three screens are `internal` + `@VisibleForTesting` rather than `private`** —
`TranslationScreen`, `DiscoveryTabletScreen`, `JournalTabletScreen` — because an instrumented
test drives each of them. `androidDeviceTest` compiles inside `:shared`, so `internal` reaches
it and nothing outside the module gains anything. `LLM.md` §5 has the argument for why a probe
would not have done.

---

## 6. Screens with no large-window arrangement

Absence is a decision here, and the checklist in MVI doc §9 asks for it in writing.

| Screen | Why not |
|---|---|
| `TranslationScreen` | It is a photograph with the Vietnamese on it replaced in place. The same picture at any window size, and a pane beside it would have nothing to hold. Revisit in phase 10, when the phone's screens are read in landscape. |
| `ChatScreen` | The guide is not a screen on a large window — it is the column beside the story, so `Routes.CHAT` opens `DiscoveryTabletRoute`. The arrangement exists; it is just not a screen of its own. |
| `PassportScreen`, `CollectionScreen` | Same shape one level down: both are panes of the journal, so they are `XPane.kt` rather than `XTabletScreen.kt`. The file name says which of the two it is. |

---

## 7. How to add a large-window arrangement

1. **Lift first, arrange second.** Every composable both branches will draw moves to
   `feature/<name>/component/`, one per file, `internal`. A `private fun` inside
   `mobile/feature/x/XScreen.kt` is invisible to `tablet/`, so the arrangement will copy it —
   and a copy diverges on the first fix only one side gets. Count the pieces by *screen*, not
   by panel: phase 04's plan listed six and needed fourteen; phase 06's listed eight and needed
   twenty-four.
2. **A shared list is a `LazyListScope` extension, not a composable** — `journalDays(…)`,
   `collectionBoard(…)`. The two branches put the same items into two different scrolling
   containers, and a composable would nest a scroller inside a scroller.
3. **If the Route is more than one call, split it into `XHost.kt` first.** A Route that owns a
   capture coroutine, a permission bridge or a lifecycle observer owns *behaviour*, and `LLM.md`
   §3 forbids a branch from owning behaviour. `LensHost`, `ExploreHost` and `SettingsHost` are
   the three that exist; a Route that is genuinely five lines needs none.
4. **Write the arrangement** as `tablet/feature/<name>/XTabletScreen.kt` — or `XPane.kt` if it
   is a pane of another screen, in which case it takes `state` + `onIntent` and the host Route
   resolves the ViewModel.
5. **Point the route at it** in `tablet/navigation/TabletNavGraph.kt`. Change only that
   `composable` block; leave the route list and every `navArgument` default alone (§3 above).
6. **Update the gates.** Add the screen to `DesignTokenTest.HEADER_OWNERS` unless it draws no
   header, and say why in the list's KDoc if not. The test also prints how many files it scans
   — read the number.

---

## 8. Verifying

- `./gradlew :shared:allTests` — **both** platforms. `:shared:testAndroidHostTest` alone has
  been green while `commonTest` did not compile for Kotlin/Native at all (`LLM.md` §11 row #14).
- `./gradlew :shared:connectedAndroidDeviceTest` on a device at **API ≤ 36** — API 37 breaks
  Espresso before any test body runs (row #18). The two-pane tests live in
  `androidDeviceTest/tablet/` and force a 1280 × 800 dp window so they mean the same thing on
  any AVD.
- By hand, the two that no test covers: **the phone must be unchanged**, and the window must
  survive being resized mid-screen in both directions. The device matrix and results are in
  `plans/260804-1016-large-screen-branch/verification.md`.
- No GPS on the Pixel Tablet AVD. For Explore, force a large window on a phone emulator instead
  — `adb shell wm size 2560x1600 && adb shell wm density 320` — then `adb emu geo fix`.
