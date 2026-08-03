# Verification record — UI standardisation

## Phase 0 baseline (03.08.2026, before any change)

| Measure | Value |
|---|---|
| `:shared:testAndroidHostTest` | green |
| Unstable classes in the Compose report | 20 (the agreed ceiling) |
| Composables total | 192 |
| Restartable but **not** skippable | 0 |

**Screenshots were not captured at baseline.** The plan asked for ten screens, light and
dark, plus a notch capture of the discovery page. Reaching most of those screens needs a
populated journal — a discovery, a day summary, an unlocked province — which this emulator
does not have, and the two that matter most (the discovery page and the notch case) cannot
be reached at all without one. The "after" captures below are therefore a *rendering* check
rather than a diff, and the pixel comparison the plan asked for is still outstanding. It is
the one Definition-of-Done item this work did not close.

## After

| Measure | Value | vs baseline |
|---|---|---|
| `:shared:testAndroidHostTest` | 35 tests, 0 failed | +6 (`DesignTokenTest`) |
| Unstable classes | 20 | unchanged |
| Restartable but not skippable | 0 | unchanged |
| `PageHeader` / `OverlayHeader` / `OverlayIconButton` / `HeadingBlock` | all restartable **and** skippable | new, clean |
| `:app:assembleDebug` | green | |
| `:shared:compileKotlinIosSimulatorArm64` | green | the shared layer reaches iOS |

### The gate, proven both ways

`DesignTokenTest` is green on the migrated tree. Reintroducing two literals into
`CollectionScreen.kt` — one `Spacer(Modifier.height(6.dp))` and one
`RoundedCornerShape(14.dp)` — turned it red on exactly those two rules, naming the file,
the line and the cost. Both were reverted.

### Device check

`screenshots/` holds four captures from a Pixel 7 Pro AVD (API 37):

| File | What it confirms |
|---|---|
| `01-launch.png` | Lens: tool row, viewfinder on `shapes.large`, mode chips at one weight with the gold fill carrying selection, shutter row unmoved |
| `02-journal.png` | Journal's empty state — the header is not drawn on this branch |
| `03-settings.png` | `PageHeader` with its kicker, and the shared `SectionHeader` aligned to the card edge rather than `ScreenGutter + 4` |
| `04-explore.png` | `OverlayHeader` in `Card` style, its two map controls top-aligned beside it, the inset applied once |

The app installs, launches and runs with no fatal.

### `androidDeviceTest` could not run — and it is not this change

All nine instrumented tests fail in `Espresso.onIdle` with
`NoSuchMethodException: android.hardware.input.InputManager.getInstance`. That is a
reflection call Espresso makes into a platform API that modern Android removed; the only
emulator on this machine is API 37.

It is not a regression from this work, and the proof is in the failure list:
`RecompositionTest.theChatStateIsComparedByValue` only compares two `ChatState` instances,
touches no file this plan changed, and fails with the identical stack. Recorded as
`LLM.md` §11 row #18.

**Consequences for this plan's Definition of Done:** `TranslationOverlayGestureTest` could
not be run after the translation migration, and the Lens shutter row was checked by eye in
`01-launch.png` rather than by gesture. Both need an API 34 AVD, or an upgrade of
`androidx.compose.ui:ui-test-junit4` and its transitive Espresso.

---

## Adversarial review — and what it found

Seven independent reviewers over distinct dimensions, each finding put to two skeptics with
different lenses (refute-the-claim, does-it-break-at-runtime). 58 raw findings; the review
ran short — 70 of 123 agents hit a session limit — so this is a floor on what is there, not
a ceiling. Ten defects were confirmed and fixed. **Two of them were real bugs I introduced.**

### The two that mattered

**The chat header stopped painting under the notch.** I had moved `screenInsetsPadding()`
onto the `Surface`'s own modifier. Material3 chains the caller's modifier *ahead of* its
`.background(...)`, so the inset shrank the painted band instead of the content — leaving a
bare cream strip above a tinted header, seamed across the top edge. The comment I wrote
directly above it asserted the opposite of what the code did. Fixed by putting the inset on
`PageHeader` and leaving the `Surface` full-bleed, which is what the original did. Written
up in the MVI doc §11 as a second inset trap.

**The translated overlay could no longer shrink to fit.** Swapping the bare `TextStyle` for
`labelLarge.copy(...)` inherited an absolute `lineHeight = 20.sp`. `TextAutoSize` steps
`fontSize` down but cannot touch `lineHeight`, so a block auto-sized to 8 sp to cover a menu
line kept a 20 sp line box — two lines of 40 sp inside a box measured to the Vietnamese
underneath. Reverted to a bare `TextStyle`; the file is already allowlisted for exactly this
reason.

### The rest

| Defect | Fix |
|---|---|
| Two discovery cards clipped at the token radius while their hand-drawn dashed borders still traced the old literal — corner arcs sliced off | `Corner` object in the theme: the five radii as numbers, one source with `VietLensShapes` |
| Camera mode chips fell from a 40 dp to a 32 dp target (type scale and padding both dropped in one edit) | vertical padding back to `Spacing.md` |
| `LanguageChip` fell to a 28 dp target | vertical padding up to `Spacing.sm` |
| Chat paid an 8 dp trailing gap on every empty thread | `trailing` is null when there is no clear button |
| Type.kt claimed `extraLarge` was the camera frame; it is `large` | table corrected |
| The weight ladder was stated as "display and headline Bold"; `headlineSmall` is SemiBold | stated accurately in three places |
| `StampType` was described as monospace; `seal` is proportional | KDoc corrected, with the reason |
| "four `statusBarsPadding()` calls" — there were five | corrected in four places |
| "four back/close affordances" — three were deleted | corrected |
| §13.3's absorption table, §3's component list, §11's row order | corrected |

### The gate had a hole, and six live violations were sitting in it

The most valuable finding. The gap rule anchored on the opening paren and scanned with
`[^)\n]*`, which cannot cross a closing paren and never matched `PaddingValues(` at all —
the capital P is not the `padding(` it was looking for. Six spacing literals were in
`feature/` while the gate reported clean, including `PaddingValues(vertical = 14.dp)` and
three `Spacer(Modifier.height(if (…) 16.dp else 14.dp))` whose literals hide behind the
condition's own bracket. Neither 14 nor 18 is on the scale.

**My own Definition-of-Done audit reported zero, because I checked with the same blind
pattern the gate used.** That is the lesson: a gate and its audit must not share a
weakness, and the way to find out is to make something else look.

Replaced with a paren-matching scanner that reads the call's own balanced argument list, so
it sees through a nested condition *and* does not fire on `Modifier.padding(top =
Spacing.xxs).size(14.dp)`, where the literal is a size that merely shares a line with a gap.
`0.dp` is exempt — zero is the absence of a gap, not a choice of one. The shape rule now
also catches named-argument and `CutCornerShape` spellings; the inset rule catches
`windowInsetsPadding(WindowInsets.statusBars)` written longhand; the header rule matches a
*call* rather than the name appearing anywhere, and fails if a name in `HEADER_OWNERS` stops
matching a file.

### Proven red on all six rules, not two

Injected one compiling violation per rule and watched each fail in turn, then reverted:
`fontWeight`, `0.5.sp`, `statusBarsPadding()`, a removed `PageHeader`, a literal radius, a
literal gap. The strengthened gap scanner was re-proven separately against the three forms
that used to defeat it, and reports the right line numbers — the first version of the
scanner did not, because it built its offset table from the unblanked source.

### Final state

`clean` + `--no-build-cache --rerun-tasks`: 158 tasks genuinely recompiled, `BUILD
SUCCESSFUL`, 35/35 host tests, unstable classes 20/20, non-skippable 0. `:app:assembleDebug`
and `:shared:compileKotlinIosSimulatorArm64` both green. App installs, launches and renders
with no fatal.

---

## Second verification pass — the two real bugs, proven on device

The review's two HIGH findings were fixed by reasoning about `Surface` and `TextAutoSize`.
Reasoning is what produced the chat bug in the first place, so this pass sets out to prove
both empirically rather than argue them a second time.

### The chat header — proven by photograph

`screenshots/09-chat-header-notch-fixed.png`. The tinted band runs to the top edge of the
display with no cream strip above it and no seam across it, and the title and back chip
begin below the cutout. That is the whole fix: the `Surface` full-bleed, the inset on
`PageHeader`. The thread is empty, so it also shows `trailing = null` — the title sits hard
against the back chip with no 8 dp gap held open for a button that is not there.

Reaching the screen needed a discovery in the database. There is exactly one
(`Khuê Văn Các`), found by pulling `vietlens.db` **with its `-wal` and `-shm`** — Room runs
in WAL mode, so the main file alone is 4 KB of empty pages and reads as an empty database.

### The translation overlay — proven by diff

Stronger than a screenshot, and cheaper: `git diff` of the `TextStyle` block shows only
added comment lines. `color`, `fontWeight`, `textAlign` and the *absence* of `lineHeight` are
byte-identical to what shipped before this refactor. The style is not a new construction to
be re-tested; it is the original, restored.

Three more screens re-captured on the way: `06-lens-verify.png` (mode chips back at a 40 dp
target), `07-journal-pageheader.png` (`PageHeader` with kicker and trailing count),
`08-discovery-overlayheader.png` (`OverlayHeader` clear of the cutout).

### The gate, re-proven — and one thing it does that the KDoc did not say

A throwaway `GateProbe.kt` under `feature/` carrying the three forms that defeated the first
scanner, plus one form that must stay green. Result: **red on all three** — the literal
behind a nested condition, `PaddingValues(` with its capital P, and `pageSpacing = 12.dp` —
and green on `Box(Modifier.padding(top = Spacing.xxs).size(14.dp))`. Probe deleted.

The first probe *also* fired on `Spacer(Modifier.padding(top = Spacing.xxs).size(14.dp))`,
which I had written as the must-not-fire case. The gate was right and the probe was wrong: a
`Spacer` has no content, so whatever sizes it **is** the gap. The KDoc gave that example
without saying what it was attached to, which would read as a false positive to the next
person and get the rule suppressed. Corrected in place.

### An independent sweep, deliberately not the gate's method

The lesson from the first pass was that a gate and its audit must not share a blind spot, so
this one is a Python paren-walker over every `.dp` literal in `commonMain` — 213 of them,
comments stripped — classified by the enclosing call rather than by the gate's own patterns.

Sixteen sit off the 4 dp scale, and all sixteen are **sizes**: button heights (56, 48, 34),
an image and a box (180, 110), a map marker (240), waveform bars (28, 3), a progress bar and
a gauge (6, 10), and five 1 dp rules. Not one is inside a `Spacer`, a `padding`, a
`PaddingValues` or a `spacedBy`. The gap discipline is genuinely clean, confirmed by a
method that shares nothing with the rule it is checking.

The same sweep found the design-system layer holds no corner literal, no call-site
`fontWeight` and no `.sp` outside `Type.kt` — which matters, because four of the six rules
only scan `feature/`. Clean today, but a hole; recorded as `LLM.md` §11 row 19.

### Found while verifying, and NOT from this refactor

Five strings reach the screen with their own escape characters: `100%% MATCH` on the
discovery badge, `N%% collected` / `N%% explored`, and `Turn today\'s one find into a story`
on the journal card. `strings.xml` is written in `aapt` conventions — `\'` unescaped at build
time, `%%` collapsed by `String.format` — and Compose Multiplatform's resource reader does
neither, though it does substitute `%1$d`. `strings.xml` is not among the 57 files this work
touched. Recorded as `LLM.md` §11 row 20, unfixed: it is a resource bug, not a UI-standard
one, and worth its own change.

### Result

35/35 host tests, `:app:assembleDebug` and `:shared:compileKotlinIosSimulatorArm64` green,
test XML confirmed regenerated rather than replayed from cache. Both HIGH bugs now have
evidence behind them instead of an argument.

---

## Third pass — the dark-mode sweep, and the five strings

### Dark mode, nine screens

`screenshots/dark-01…09`. `cmd uimode night yes`; the app's `ThemePreference` defaults to
SYSTEM, so the emulator's setting is enough. Every screen renders: `PageHeader`'s title,
kicker and back chip hold contrast on the dark scheme, `OverlayHeader` reads as a dark card
over the dark map tiles, and no text drops below legibility anywhere.

Three screens had never been rendered at all before this pass — **collection, passport and
sovereignty**. All three are correct. Sovereignty keeps its fixed red palette in both themes
by design (§12), and its close affordance sits clear of the cutout.

**Chat is pixel-identical in dark and light**, which is `LLM.md` §11 row 16 working exactly
as written: the screen is cream in both themes on purpose. The notch fix holds in dark too —
the tinted band still runs to the top edge. Worth saying plainly, though: crossing from a
dark discovery page into a cream chat is a jolt, and that is the cost row 16 is describing.
It stays a colour-system decision, out of this plan's scope.

One observation, not a defect and not from this work: the dark scheme turns `primary` into a
pale pink, so "Allow camera" and the read-aloud toggle read pink rather than the brand's
vermilion. That is standard Material 3 dark behaviour. `Color.kt` and `Theme.kt` are not
among the files this refactor touched, so nothing here changed it.

The translation overlay is still the one screen never rendered — it needs a camera frame
with text on it.

### The five strings

Fixed, and each one re-checked on the device that showed it wrong: `100% MATCH`,
`1% COLLECTED`, `0% EXPLORED`, `Turn today's one find into a story`. The fifth is the plural
"other" branch of the same journal string — the identical edit, but it needs a second
discovery to display.

What settled the diagnosis was the **Vietnamese file**: it already wrote `Khớp %1$d%` with a
single percent. So the renderer was not at fault and the English was simply written in
`aapt` conventions that Compose Multiplatform does not honour.

**`\n` was deliberately left alone.** Two more escapes live in that file, and the assumption
that they leak the same way is wrong: the sovereignty seal renders "CHỦ / QUYỀN / VN" on
three lines, so Compose Multiplatform *does* unescape `\n` while ignoring `\'`. Checking
before editing was the difference between a fix and a regression.

### `§11` had two rows numbered 15

Found while filing the above. The Open table and the Fixed table each held a different
deviation under `15`, and **seven places cite `§11 row #15`** — including live source
comments in `JournalContract.kt`, `JournalViewModelTest.kt` and `SettingsViewModelTest.kt`.
Every citation means the Fixed row. The Open row was renumbered to 21 and both tables sorted;
a number cited from source code is not free to reuse.

### Result

`--rerun-tasks` across all three: `BUILD SUCCESSFUL`, 35/35 host tests written fresh,
`:app:assembleDebug` and `:shared:compileKotlinIosSimulatorArm64` green.
