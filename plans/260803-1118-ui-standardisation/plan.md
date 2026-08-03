# UI Standardisation Plan — one format for every screen

**Created:** 3 Aug 2026 · **Target:** `:shared` presentation layer
**Produces:** a completed `core/designsystem/` + `LLM.md` §13 + MVI doc §11
**Format note:** one file, matching `plans/260802-2103-mvi-refactor/plan.md`.

---

## Mandate

**Every screen draws its text, its gaps and its corners from the same named set of values,
and every screen's header is one of two components.** Nothing is measured at the call site.

This is the opposite of the MVI refactor, which was explicitly forbidden from changing what
the traveller sees. This plan *does* change it — that was the decision taken at the start
(see below), and it is the only way the lens, the journal and a discovery can stop looking
like three apps.

### The three decisions this plan was written under

| Question | Answer taken |
|---|---|
| How far to standardise | **Properly.** One token set, one header component. Settings, Chat and Journal will visibly change size and spacing. A token-only refactor that preserved every pixel would merely give the existing inconsistencies names. |
| Which screens | **All ten, under two standards.** `PageHeader` for the five document-style screens, `OverlayHeader` for the five that draw over a photo or a camera feed. Spacing, typography and shape tokens are shared by all ten. |
| Drift protection | **Test + docs.** A host test that fails the build on a hardcoded radius or an `.sp` inside `feature/`, plus the rule written into `LLM.md` and the MVI doc. Without a gate this plan is undone in a fortnight. |

---

## Where the codebase stands — measured, not guessed

Every number below is a count over `shared/src/commonMain/kotlin/com/duylt/trave/vietlensai/`.

### 1. Typography — nine of fifteen scales exist

`VietLensTypography` (`theme/Type.kt`) overrides nine scales. Six are left at the Material
default: `displayLarge`, `displayMedium`, `titleSmall`, `bodySmall`, `labelMedium`,
`labelSmall`.

That is not a cosmetic gap. The nine overridden scales all carry
`LineHeightStyle.Trim.None`, and `Type.kt`'s own KDoc says why:

> `LineHeightStyle.Trim.None` keeps Vietnamese stacked diacritics (ề, ộ, ữ) from being
> clipped, which the default trimming does at tight line heights.

The six that were never overridden do not carry it — and they are used **44 times** in
feature code (`bodySmall` 11, `labelMedium` 14, `labelSmall` 13, `titleSmall` 5,
`displayLarge` 1). `Kicker`, which draws every eyebrow label in the app, is built on
`labelSmall`. Completing the scale set is typography standardisation and fixes the clipping
as a side effect.

Also measured:

| Symptom | Count |
|---|---|
| `fontWeight = FontWeight.X` applied at a call site, on top of a theme scale | **44**, across 15 files |
| `typography.X.copy(...)` inventing a scale in place | **5** — incl. `labelLarge.copy(fontSize = 13.sp)` at `LensScreen.kt:986` |
| Page titles, and how many distinct scales they use | 5 titles, **4 scales**: Settings `headlineLarge`, Journal/Collection/Passport `headlineMedium`, Chat `titleLarge`, Explore `titleMedium` |
| `Kicker` — a 16th text style (mono + bold + 1.2 sp tracking) living in `Components.kt`, not in `Type.kt` | 1, plus 2 near-variants re-`.copy()`-ing `labelSmall` (`LensScreen.kt:1446`, `SovereigntyBanner.kt:134`) |

### 2. Spacing — one token, then 156 literals

`theme/Dimens.kt` contains exactly one value: `ScreenGutter = 16.dp`. Its KDoc argues the
case for it well, and then nothing else in the app follows the argument.

| Symptom | Count | Distinct values |
|---|---|---|
| `Spacer(Modifier.height(N.dp))` | 92 | **16** — 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 22, 24, 28 |
| `padding(vertical = N.dp)` | 64 | **12** — 2, 4, 5, 6, 8, 9, 10, 11, 12, 14, 16, 20 |
| `padding(horizontal = N.dp)` | 39 | **11** — incl. `16.dp` written out 11 times, which is `ScreenGutter` re-typed as a literal |
| `ScreenGutter + 4.dp` | 2 (`SettingsScreen.kt:274`, `:739`) | the token being locally corrected — which is the token failing |

The gap between a header and the content under it is decided per screen:

| Screen | Header top pad | Header bottom pad | Title scale |
|---|---|---|---|
| Journal | 0 | 16 | `headlineMedium` |
| Settings | 0 | 4 | `headlineLarge` |
| Collection | 12 | 4 | `headlineMedium` |
| Passport | 12 | 4 | `headlineMedium` |
| Chat | 10 | 12 | `titleLarge` |

Five screens, five header boxes. And `contentPadding = PaddingValues(bottom = 32.dp)`
appears identically in Journal, Settings and Collection — a token that exists in fact and
has no name.

### 3. Shape — the shape system is dead code

`VietLensShapes` is defined in `Type.kt`, passed to `MaterialTheme` in `Theme.kt`, and
`MaterialTheme.shapes` is referenced **zero times** in the entire codebase.

| Symptom | Count | Distinct values |
|---|---|---|
| `RoundedCornerShape(N.dp)` | 57 | **12** — 3, 4, 6, 10, 12, 14, 16, 18, 20, 24, 28, 32 |
| `RoundedCornerShape(50)` (pill) | 15 | 1 |
| `CircleShape` | across 11 files | 1 |

The theme declares five radii (6/10/16/24/32). Seven of the twelve radii actually drawn are
not among them. The most-used radius in the app is `20.dp`, which the theme does not have.

### 4. Header and top inset — two helpers, four back buttons

`theme/Insets.kt` provides `screenInsetsPadding()` and explains, at length, why
`statusBarsPadding()` is not enough: the app hides the system bars, which takes that inset
to zero, and the notch is still a hole in the glass.

- `screenInsetsPadding()` — used in 9 files.
- Raw `statusBarsPadding()` — used in `DiscoveryScreen.kt` at lines 449, 463, 1493, 1672.
  On a notch phone with the bars hidden, the discovery page's close and delete buttons sit
  under the cutout. This is the exact failure the helper was written to prevent.

Where the inset is applied is also inconsistent: outermost container (Journal, Settings,
Collection, Passport, Lens, Sovereignty, Explore) versus inside the header itself
(`ChatScreen.kt:241`, `TranslationScreen.kt:478`) — and in `ExploreScreen` it is applied
twice inside the same `Box`, once by `MapHeader` and once by the control column.

The back affordance is four different components for one job: `BackChip` (Collection,
Passport, Chat), `GlassButton` (Translation), `OverlayIconButton` (Discovery), `CloseChip`
(Sovereignty).

And the Chat header paints itself outside the colour scheme entirely —
`HeaderBackground`, `InkBrown`, `InkMuted` are private vals in `ChatScreen.kt:93-97`,
derived from the lens palette. The chat header does not respond to light/dark at all.

### 5. Section headings — the shared one is unused

Three implementations of the same idea:

| Implementation | Style | Padding | Call sites |
|---|---|---|---|
| `Components.kt:433 SectionHeader` | `titleMedium`, `onSurfaceVariant` | 20 / 12 | **0** |
| `SettingsScreen.kt:733 SectionLabel` | `Kicker`, `onSurfaceVariant` | start `Gutter+4`, top 24, bottom 10 | 5 |
| `CollectionScreen.kt:250 SectionHeading` | `Kicker` + count row | 20 / 10 | 1 |

The one in the design system is dead; the two private ones drifted apart.

---

## The target

### T1 — Typography: all fifteen scales, plus a role map

`Type.kt` gains the six missing scales, each with `readableLineHeight`, and each with its
weight baked in so no call site has to add one. Then a role map, documented in the file:

| Role | Scale | Used by |
|---|---|---|
| Page title (document screens) | `headlineMedium` | `PageHeader` |
| Page subtitle | `bodyMedium` / `onSurfaceVariant` | `PageHeader` |
| Overlay title (over photo/camera) | `titleLarge` | `OverlayHeader` |
| Eyebrow / kicker / section label | `labelSmall` — mono, bold, 1.2 sp tracking, moved out of `Components.kt` into `Type.kt` | `Kicker`, `SectionHeader` |
| Card / row title | `titleMedium` | list rows, cards |
| Reading body | `bodyLarge` (17 / 27) | discovery narrative, chat bubbles |
| Secondary body | `bodyMedium` (15 / 23) | subtitles, descriptions |
| Metadata / caption | `bodySmall` | timestamps, counts |
| Button | `labelLarge` | all buttons |

Settings' title drops from `headlineLarge` to `headlineMedium`; Chat's rises from
`titleLarge` to the overlay standard; Explore's `titleMedium` rises to `titleLarge`. Those
are the visible changes accepted in decision 1.

**Rule to be enforced:** no `fontWeight`, no `fontSize`, no `.copy()` on a typography scale
inside `feature/`. If a variant is needed, it is a scale and it lives in `Type.kt`.

### T2 — Spacing: a 4 dp scale plus semantic tokens

`Dimens.kt` becomes:

```kotlin
object Spacing {
    val xxs = 2.dp   // between two lines of one block
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp  // == ScreenGutter
    val xl  = 24.dp
    val xxl = 32.dp
}

object PageSpacing {
    val headerTop       = Spacing.md   // 12 — inset edge to the top of the header
    val headerToContent = Spacing.lg   // 16 — header to the first row below it
    val sectionGap      = Spacing.xl   // 24 — between blocks
    val listBottom      = Spacing.xxl  // 32 — the unnamed value in three LazyColumns
}
```

`ScreenGutter` stays as the name it already has, aliased to `Spacing.lg`, so the 60-odd
existing references keep compiling and the KDoc keeps its argument.

Snapping the 16 existing Spacer values onto 7 steps: 5→4, 6→4 or 8, 7→8, 9→8, 10→8 or 12,
11→12, 14→12 or 16, 18→16, 20→16 or 24, 22→24, 28→24 or 32. Each choice is made once, per
call site, during migration — not by a blanket rule.

**`ScreenGutter + 4.dp` is deleted.** Either the gutter is 16 or the component has its own
inner padding; a screen may not adjust the shared gutter.

### T3 — Shape: activate `MaterialTheme.shapes`

The existing five slots stay, and the twelve drawn radii map onto them:

| Slot | Radius | Absorbs | For |
|---|---|---|---|
| `extraSmall` | 6 | 3, 4, 6 | tags, tiny chips |
| `small` | 10 | 10, 12 | inline controls |
| `medium` | 16 | 14, 16, 18 | cards, sheets-in-page |
| `large` | 24 | 20, 24 | bottom sheets, big cards |
| `extraLarge` | 32 | 28, 32 | the camera frame |

Plus two named constants for the shapes that are not radii — `Pill` (`RoundedCornerShape(50)`,
15 uses) and `CircleShape` (unchanged). All 57 literals become
`MaterialTheme.shapes.X`, `Pill` or `CircleShape`.

The most disruptive row is `20 → large (24)`: 17 call sites, the most common radius in the
app, gaining 4 dp. If that reads wrong on device after Phase 3, the alternative is to move
`large` to 20 and push 24 up into `extraLarge` — decided once, on a device, not per screen.

### T4 — `PageHeader` (document screens: Journal, Settings, Collection, Passport, Chat)

```kotlin
@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,          // eyebrow above the title
    subtitle: String? = null,        // one line under it
    onBack: (() -> Unit)? = null,    // renders a BackChip when present
    trailing: (@Composable () -> Unit)? = null,  // the count, the clear button
)
```

One box: `padding(horizontal = ScreenGutter, top = PageSpacing.headerTop,
bottom = PageSpacing.headerToContent)`. Title always `headlineMedium`, subtitle always
`bodyMedium`/`onSurfaceVariant`, kicker always the kicker style. The screen supplies
strings, not styles.

The top inset is **not** applied by `PageHeader` — it stays on the screen's outermost
container, which is where `Insets.kt` says it belongs (landscape moves the cutout to the
side and the whole page has to move with it).

### T5 — `OverlayHeader` (immersive screens: Lens, Discovery, Explore, Translation, Sovereignty)

```kotlin
@Composable
fun OverlayHeader(
    modifier: Modifier = Modifier,
    title: String? = null,           // titleLarge, on the scrim
    subtitle: String? = null,
    leading: OverlayAction? = null,  // back or close
    actions: List<OverlayAction> = emptyList(),
    scrim: Boolean = true,           // the top vertical gradient
)
```

One back/close component replaces the four: `OverlayIconButton`, taking the glass treatment
Translation already uses. `BackChip` stays for `PageHeader` only. `CloseChip` becomes
`OverlayIconButton(icon = Close)`.

`screenInsetsPadding()` is applied by `OverlayHeader` itself, because these headers float
over content rather than sitting in a column above it — and that removes the four raw
`statusBarsPadding()` calls in Discovery, which is where the notch bug is.

---

## Phases

### Phase 0 — Baseline · ~0.5 day

Nothing changes. Everything after this is compared against it.

1. Install the current build on a device and capture all ten screens, light and dark,
   plus one notch-phone capture of the Discovery page with the system bars hidden (the
   `statusBarsPadding` case).
2. Save to `plans/260803-1118-ui-standardisation/baseline/`.
3. Record the current `ComposeStabilityReportTest` numbers, so a spacing refactor that
   accidentally makes a composable non-skippable is visible.

- [ ] Ten screens captured, light + dark
- [ ] Discovery notch case captured
- [ ] Stability baseline recorded

**Done when:** a reviewer can diff any later screenshot against a saved one.

### Phase 1 — Token layer · ~1 day

Files: `theme/Type.kt`, `theme/Dimens.kt`, `theme/Theme.kt`,
`component/Components.kt` (Kicker moves out).

1. Add the six missing typography scales with `readableLineHeight` and a baked-in weight.
2. Add the kicker style to `Type.kt`; `Kicker` in `Components.kt` becomes a thin wrapper
   that reads it, so the mono/tracking/weight stops being decided in a component file.
3. Write the role map from T1 into `Type.kt`'s KDoc, in the file's existing voice — rule,
   then the cost of breaking it.
4. Replace `Dimens.kt` with `Spacing` + `PageSpacing` from T2, keeping `ScreenGutter`.
5. Add `Pill` next to the shapes; adjust `VietLensShapes` only if T3's `20 → 24` decision
   is reversed after Phase 3.

No feature file is touched in this phase. The app must build and look identical.

- [ ] `Type.kt` covers all 15 scales, all with `Trim.None`
- [ ] Kicker style lives in `Type.kt`
- [ ] `Spacing` / `PageSpacing` exist, `ScreenGutter` still resolves
- [ ] `Pill` exists
- [ ] `./gradlew :shared:assembleDebug` green, screenshots unchanged vs Phase 0

**Risk:** adding a `Trim.None` to `labelSmall` changes the height of every kicker in the
app by 1–2 dp. Expected, and the reason Phase 0 exists.

### Phase 2 — Header components · ~1 day

Files: new `component/PageHeader.kt`, new `component/OverlayHeader.kt`,
`component/Components.kt` (`OverlayIconButton` promoted out of `DiscoveryScreen.kt`).

1. Write `PageHeader` per T4, with the KDoc stating the padding rule and why the top inset
   is not its job.
2. Write `OverlayHeader` per T5, applying `screenInsetsPadding()` internally.
3. Move `OverlayIconButton` from `DiscoveryScreen.kt` into the design system; give it the
   glass treatment from `TranslationScreen`'s `GlassButton` and delete `GlassButton`.
4. Neither component is wired to a screen yet. Both get a preview-friendly signature
   (pure `String` + lambdas, no state class), so Phase 3 and 4 are call-site swaps.

- [ ] `PageHeader` compiles, no ViewModel or state dependency
- [ ] `OverlayHeader` compiles, applies the inset itself
- [ ] `OverlayIconButton` in the design system, `GlassButton` deleted
- [ ] Both composables skippable in the stability report

**Risk:** `OverlayHeader` taking `List<OverlayAction>` makes it non-skippable — `List` is
unstable. Use `ImmutableList` or accept a trailing `@Composable` slot instead; decide when
the stability report is read, not before.

### Phase 3 — Migrate the five document screens · ~1.5 days

Files: `JournalScreen.kt`, `SettingsScreen.kt`, `CollectionScreen.kt`,
`PassportScreen.kt`, `ChatScreen.kt`.

Per screen, in this order (Collection first — it is the smallest and validates the
component before Journal's 841 lines):

1. Delete the private `XHeader` composable; call `PageHeader`.
2. Replace every `Spacer(Modifier.height(N.dp))` and `padding(vertical = N.dp)` with a
   `Spacing.*` token, snapping per T2.
3. Replace every `RoundedCornerShape(N.dp)` with `MaterialTheme.shapes.*` or `Pill`.
4. Delete every call-site `fontWeight` and `.copy()` on a typography scale.
5. Replace the three `PaddingValues(bottom = 32.dp)` with `PageSpacing.listBottom`.
6. Chat only: delete `HeaderBackground` / `InkMuted`, take the colours from the scheme.
   This is the screen that changes most, and it is the one currently ignoring dark mode.

- [ ] Collection migrated, screenshot diffed
- [ ] Passport migrated, screenshot diffed
- [ ] Journal migrated, screenshot diffed
- [ ] Settings migrated, screenshot diffed
- [ ] Chat migrated, dark mode verified on device
- [ ] Zero `.dp` literals left in a `Spacer` or a `padding` in these five files
- [ ] `./gradlew :shared:allTests` no worse than the Phase 0 baseline (see Out of scope re: #14)

**Risk:** Settings' title dropping a scale, and the 20→24 radius shift, land here. This is
the checkpoint for the T3 fallback: if 24 reads too soft on the settings cards, change
`VietLensShapes.large` to 20 in `Type.kt` — one line, no call site touched. That is the
payoff for doing the token layer first.

### Phase 4 — Migrate the five immersive screens · ~1.5 days

Files: `DiscoveryScreen.kt`, `TranslationScreen.kt`, `ExploreScreen.kt`,
`SovereigntyScreen.kt`, `LensScreen.kt` (+ `PlaceDetailSheet.kt`, `SovereigntyMap.kt`).

1. **Discovery first** — swap the four `statusBarsPadding()` for `OverlayHeader`. Verify on
   the notch capture from Phase 0. This is the one place where the standardisation removes
   a real defect rather than an inconsistency.
2. Translation — `TranslationTopBar` becomes `OverlayHeader` with three actions; the
   press-and-hold peek behaviour stays exactly as it is (it is feature logic, not chrome).
3. Explore — `MapHeader` becomes `OverlayHeader`; the duplicated `screenInsetsPadding()` in
   the control column goes, since the header now owns it. Title rises to `titleLarge`.
4. Sovereignty — `CloseChip` becomes `OverlayIconButton`; the fixed `PaperCream`/`Vermilion`
   palette **stays** (it is the screen's identity, documented in `Color.kt`), only the
   spacing and radii are tokenised.
5. Lens — tokens only. **No header component.** The camera tool row is not a header, and
   the fixed lens palette stays for the reason `Color.kt` gives.
6. `LensScreen.kt:986` — `labelLarge.copy(fontSize = 13.sp)` resolves to `labelMedium` or a
   named scale; it does not survive as a `.copy()`.

- [ ] Discovery migrated, notch case verified against Phase 0 capture
- [ ] Translation migrated
- [ ] Explore migrated, single inset application
- [ ] Sovereignty migrated, palette unchanged
- [ ] Lens tokenised, palette unchanged
- [ ] Zero `statusBarsPadding()` in `feature/`
- [ ] Zero `.sp` in `feature/` except the two documented map-label constants

**Risk:** the Lens screen is 2173 lines and drives a camera. Tokenise it last, in one pass.
A spacing change to the shutter row is a change to a hit target, and nothing covers it —
`androidDeviceTest` holds exactly two files, `TranslationOverlayGestureTest` and
`RecompositionTest`, so the automated safety net here is Translation's, not Lens's. Run
`TranslationOverlayGestureTest` after step 2 and check the Lens shutter by hand on device.

### Phase 5 — Section headings and dead components · ~0.5 day

Files: `component/Components.kt`, `SettingsScreen.kt`, `CollectionScreen.kt`.

1. Rewrite `SectionHeader` in `Components.kt` to the kicker style with
   `PageSpacing.sectionGap` above and `Spacing.sm` below, plus an optional trailing slot for
   Collection's count.
2. Delete `SettingsScreen.SectionLabel` and `CollectionScreen.SectionHeading`; call the
   shared one.
3. Sweep for anything else in `Components.kt` with zero call sites.

- [ ] One `SectionHeader`, six call sites
- [ ] Both private variants deleted
- [ ] No zero-call-site public composable left in `Components.kt`

### Phase 6 — Gates and documentation · ~1 day

Files: new `androidHostTest/.../designsystem/DesignTokenTest.kt`, `LLM.md`,
`docs/android-mvi-best-practices.md`.

1. Write `DesignTokenTest` in the voice and shape of `ComposeStabilityReportTest`: it reads
   the `commonMain` sources and fails on
   - `RoundedCornerShape(<number>.dp)` anywhere under `feature/`
   - `.sp` under `feature/`, against a named allowlist (`VietnamMapCanvas`, `SovereigntyMap`)
   - `fontWeight =` under `feature/`
   - `statusBarsPadding()` anywhere in `commonMain`
   - a `Spacer(Modifier.height(N.dp))` whose N is not a `Spacing` step

   Each rule's failure message states the rule *and the cost*, the way the stability test's
   messages do — a gate whose message is "violation at line 12" gets suppressed.
2. `LLM.md`: new **§13 — The UI standard**, holding the T1–T5 tables. Add a row to §10
   ("A page header" → `core/designsystem/component/`). Close §11 row #11's spirit if the
   line counts moved.
3. `docs/android-mvi-best-practices.md`: new **§11 — Screen chrome**, stating that
   `XScreen` renders a `PageHeader` or an `OverlayHeader` and never a hand-rolled one, and
   adding two lines to the §9 pre-PR checklist.
4. `LLM.md` §11: add an Open row for anything found and deliberately not fixed.

- [ ] `DesignTokenTest` green on the migrated tree, and demonstrably red when a literal is
      reintroduced (verify by reverting one line, running, reverting back)
- [ ] `LLM.md` §13 written, §10 row added
- [ ] MVI doc §11 written, §9 checklist extended
- [ ] Any deliberate exception recorded in `LLM.md` §11

---

## Out of scope

| Out | Why |
|---|---|
| Splitting the six over-length screen files (`LLM.md` §11 row #11) | That is the standing refactor backlog and a different change. This plan will *reduce* those files by deleting header code, but it does not restructure them. |
| The colour system | `Color.kt` is coherent and argued. The only colour work here is Chat's private palette, which is a header bug, not a palette decision. |
| The fixed lens/sovereignty palettes | Documented in `Color.kt` as deliberate. `LLM.md` §12 applies. |
| Motion | `Motion.kt` is already one set of numbers for the whole app — it is the one token file that got this right. |
| `commonTest` failing to compile for Kotlin/Native (`LLM.md` §11 row #14) | Pre-existing, unrelated. It means Phase 3/4 verification runs the Android host suite only; note that when reporting. |
| Any change in `:domain` or `:data` | Presentation layer only. |
| iOS-specific visual review | The presentation layer is shared, so the changes reach iOS automatically — but this plan does not budget for an iOS device pass. Flag it as a follow-up. |

---

## Risks

| Risk | Mitigation |
|---|---|
| "Standardised" turns out to mean "worse" on a real device | Phase 0 baseline, and a device check after Phase 3 before Phase 4 starts. The token layer means a reversal is one line in `Type.kt`, not 57 call sites. |
| A spacing change breaks a hit target on the camera screen | Lens is migrated last, and `androidDeviceTest` gesture tests run after it. Device tests need a clear foreground — force-stop the app first. |
| The gate test becomes noise and gets `@Ignore`d | Failure messages state the cost, not just the location. Allowlist the two legitimate `.sp` map-label cases up front rather than after the first false positive. |
| Header components become non-skippable and cost recomposition | `ComposeStabilityReportTest` already guards this; check it at the end of Phase 2, not at the end of the plan. |
| Scope creep into the 200-line refactor | Explicitly out of scope above. Deleting a header is in; restructuring a screen is not. |

**Security:** none. No file in this plan touches storage, network, permissions or keys.

---

## Definition of done

1. Ten screens, two header components, zero hand-rolled headers.
2. Zero `RoundedCornerShape(N.dp)`, zero `.sp`, zero `fontWeight =` under `feature/`,
   except the allowlisted map labels.
3. Every `Spacer` and `padding` value in `feature/` is a `Spacing` or `PageSpacing` token.
4. `MaterialTheme.shapes` referenced; `VietLensShapes` no longer dead code.
5. All fifteen typography scales defined with `Trim.None`.
6. `statusBarsPadding()` gone; `screenInsetsPadding()` applied exactly once per screen.
7. `DesignTokenTest` green, and proven to fail when a literal returns.
8. `LLM.md` §13 and MVI doc §11 written; §10 and the §9 checklist updated.
9. `./gradlew :shared:assembleDebug` green; Android host tests no worse than baseline;
   `TranslationOverlayGestureTest` and `RecompositionTest` green on device after Phase 4;
   the Lens shutter row checked by hand, since nothing covers it.
10. Screenshot diff reviewed against Phase 0 for all ten screens, light and dark.

**Estimate: 6–7 working days**, one developer already familiar with the codebase.
Phases 1 and 2 are prerequisites for everything; 3 and 4 are independent of each other and
could run in parallel across two developers with clean file ownership.
