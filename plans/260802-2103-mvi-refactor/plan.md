# Refactor Plan — MVI structural compliance for `:shared`

**Created:** 2 Aug 2026 · **Target:** `/shared`
**Source:** `LLM.md` §11 · **Standard:** `docs/android-mvi-best-practices.md`
**Format note:** kept as one file at the user's request, rather than the `plan.md` +
`phase-XX.md` split in `.claude/rules/documentation-management.md`.

---

## Mandate

**Make every feature package conform to the same MVI shape. Nothing else.**

This plan does not fix bugs, does not change what the user sees, and does not add or remove
features. Every step is either moving code, renaming a call path, or replacing a hand-rolled
pattern with the shared one. If a step changes what appears on screen, it is out of scope and
belongs in `docs/bug-report-effect-collection.md`.

### The target shape

Ten feature packages, all identical:

```
feature/<name>/
├── XContract.kt     State + Intent + Effect. Nothing else.
├── XViewModel.kt    extends MviViewModel<XState, XIntent, XEffect>.
│                    onIntent() is the only public method.
└── XScreen.kt       XRoute (stateful) collects state + effects via CollectEffects.
                     XScreen (private, stateless) takes state + onIntent.
```

### Where the codebase stands against it

| Conformance rule | Conforming | Deviating |
|---|---|---|
| `XContract.kt` exists | 5 | 4 inline, 1 absent |
| Extends `MviViewModel` | 9 | 1 plain `ViewModel` |
| `onIntent` is the only public method | 7 | 3 with extra public methods |
| Effects collected via a shared helper | 0 | 5 hand-rolled, 2 missing |
| Effect declarations match collectors | 8 | 2 declare effects nobody handles |

---

## Scope

| In | `LLM.md` §11 rows | Why it is structural |
|---|---|---|
| Phase 1 | #8, #9, #10 | Five screens hand-roll effect collection, no two identical. Replace with one helper. |
| Phase 2 | #7, #12 | Four features declare the contract inline. Pure file movement. |
| Phase 3 | #1, #2 *(declaration half only)* | Every declared Effect must have exactly one collector, or not be declared. |
| Phase 4 | #3, #4, #5, #6 | Public methods bypassing `onIntent`, and one screen not on `MviViewModel`. |

| Out | Why |
|---|---|
| Rendering `JournalState.error` on screen | User-facing behaviour change → `docs/bug-report-effect-collection.md` step 2.1. |
| Building a mic button in Chat | Feature work. Phase 4 makes the *call path* conform; whether voice ships is a separate decision. |
| Screen file sizes (#11) | Line count is a project rule, not an MVI rule. See the appendix. |
| Anything in `:domain` or `:data` | This plan touches the presentation layer only. |

**Estimate: 4–5 working days**, one developer already familiar with the codebase.

---

## Ground rules

1. **One phase per branch**, conventional commits, all `refactor:`.
2. **A diff that changes rendered output is wrong.** Back it out and file it in `LLM.md` §11
   instead.
3. **Compile after every step.** The Compose stability report is written on every build and
   `ComposeStabilityReportTest` gates it.
4. **Run `LensViewModelCrashTest` after every phase.** It is the only fuzz coverage in the repo.
5. **Update `LLM.md` §11 as each row lands** — Open → Fixed, with the commit hash.

---

# Phase 1 — One effect collector instead of five

**Rows:** #8, #9, #10 · **Risk:** Low · **Estimate:** 1 day · **Blocks:** Phase 3

Five screens each wrote their own effect-collection block. Three use `collectLatest`, which
cancels the handling of the previous effect when the next arrives — wrong for a channel of
instructions. None is lifecycle-aware. Two screens have no collector at all (Phase 3).

| Screen | Line | Operator | Wrapper |
|---|---|---|---|
| `LensScreen.kt` | 248 | `collectLatest` | `LaunchedEffect(viewModel)` |
| `SettingsScreen.kt` | 145 | `collectLatest` | `LaunchedEffect(viewModel)` |
| `DiscoveryScreen.kt` | 204 | `collectLatest` | `LaunchedEffect(viewModel)` |
| `ExploreScreen.kt` | 119 | `collect` | `LaunchedEffect(viewModel)` |
| `TranslationScreen.kt` | 129 | `collect` | `LaunchedEffect(viewModel)` |

### Step 1.1 — Create the helper

**Create** `core/mvi/CollectEffects.kt`:

```kotlin
package com.duylt.trave.vietlensai.core.mvi

/**
 * Collects one-shot effects for a screen, once, while it is at least STARTED.
 *
 * `collect` rather than `collectLatest`: an effect is an instruction that has to be carried
 * out, and cancelling the handling of one the moment the next arrives leaves it half done.
 *
 * `repeatOnLifecycle` rather than a bare `LaunchedEffect`: a destination that is stopped but
 * still composed must not act on effects. Nothing is lost — [MviViewModel] buffers them in a
 * Channel and delivers them when the screen returns.
 */
@Composable
fun <E : UiEffect> CollectEffects(effects: Flow<E>, onEffect: suspend (E) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val handler by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effects.collect { handler(it) }
        }
    }
}
```

**Verify:** module compiles.

### Step 1.2 — Convert `ExploreScreen` first

`feature/explore/ExploreScreen.kt:118–119`. Already `collect`, so it is the smallest possible
change — use it to prove the helper before touching anything harder.

Replace the `LaunchedEffect(viewModel) { viewModel.effects.collect { … } }` wrapper with
`CollectEffects(viewModel.effects) { effect -> … }`. **Leave the `when` body byte-identical.**

**Verify:** compiles; `git diff` shows only the wrapper lines changed.

### Step 1.3 — Convert `TranslationScreen`

`feature/translate/TranslationScreen.kt:129`. Also already `collect`. Same mechanical change.

### Step 1.4 — Convert `SettingsScreen`

`feature/settings/SettingsScreen.kt:145`. First `collectLatest` → `collect`. Two effects,
`ApiKeySaved` and `HistoryCleared`.

### Step 1.5 — Convert `DiscoveryScreen`

`feature/discovery/DiscoveryScreen.kt:204`. Same.

### Step 1.6 — Convert `LensScreen` — the one that needs care

`feature/camera/LensScreen.kt:247–282`. A workaround was built on top of `collectLatest`, so
this is the only step where a comment must change along with the code.

`LensEffect.TakePhoto` is handled by re-launching into a separate `rememberCoroutineScope()`,
with a comment at `:258` saying `collectLatest` would otherwise cancel a capture in flight.

**Keep the `scope.launch`. The code stays; the reason changes:**

- **Old reason:** `collectLatest` cancels the capture when the next effect arrives.
- **New reason:** the collector is now lifecycle-scoped and is cancelled at `ON_STOP`. A
  capture already in flight must finish and reach its `finally`, which is what raises
  `PhotoCaptured` / `CaptureFailed` / `CaptureAborted` and releases the shutter.
  `rememberCoroutineScope()` lives until the composable leaves composition — the correct
  lifetime.

Rewrite the comment to say that. Do **not** touch the `isActive` check at `:274`; it refers to
the launched capture coroutine and its meaning is unchanged.

**Verify:** `LensViewModelCrashTest` green. Manually: 3-second self-timer, background the app
mid-countdown → no photo, no error banner on return; then background *during* a capture →
`isCapturing` is false on return.

### Step 1.7 — Sweep

```bash
grep -rn "effects.collect" shared/src/commonMain     # 1 hit, inside CollectEffects
grep -rn "collectLatest"   shared/src/commonMain     # 0 hits on an effects flow
```

### Step 1.8 — Document

- `LLM.md` §3 — add `core/mvi/CollectEffects.kt` to the tree.
- `LLM.md` §11 — rows #8, #9, #10 → Fixed.
- `docs/android-mvi-best-practices.md` §4 — confirm the documented snippet matches what was
  written.

### Done when

- [ ] `CollectEffects.kt` is the only place `effects.collect` appears
- [ ] Zero `collectLatest` on an effects flow
- [ ] Five screens converted, each diff limited to the wrapper
- [ ] `LensViewModelCrashTest` + `ComposeStabilityReportTest` green

---

# Phase 2 — Contract extraction

**Rows:** #7, #12 · **Risk:** None · **Estimate:** half a day · **Depends on:** nothing

Five features keep State/Intent/Effect in `XContract.kt`; four declare them inline at the top
of the ViewModel. Same architecture, two conventions — and the inline ones bury the state class
where nobody reads it, which is exactly the failure `LLM.md` exists to prevent.

**Pure code movement. If a diff shows anything but moved lines and imports, back it out.**

| Feature | Move from | Lines | Create |
|---|---|---|---|
| Collection | `CollectionViewModel.kt` | 19–46 | `CollectionContract.kt` |
| Journal | `JournalViewModel.kt` | 25–70 | `JournalContract.kt` |
| Settings | `SettingsViewModel.kt` | 24–47 | `SettingsContract.kt` |
| Translate | `TranslationViewModel.kt` | 25–65 | `TranslationContract.kt` |

### Steps

1. **One feature per commit.**
2. Move `data class XState`, `sealed interface XIntent`, `sealed interface XEffect`, and any
   contract-only helper type (the pattern `PassportContract.kt` sets with `CoverPhoto`).
3. **Keep every KDoc comment.** Several document non-obvious decisions that belong with the
   field, not with the ViewModel — `LensState.hasAskedLocation`'s inverted default,
   `CollectionState.selectedItemId` holding an id rather than an entry, the
   `TranslationState` note about reading arguments into the initial state.
4. Fix imports. Same package, so usually only the ViewModel file changes.
5. While the file is open, delete the unused `import kotlinx.coroutines.launch` — present in
   the Journal, Settings, Chat, Explore and Translation ViewModels (row #12).

**Verify:** compiles; `git diff --stat` is near-symmetric per commit.

### Done when

- [ ] Nine features have an `XContract.kt` *(Sovereignty is Phase 4)*
- [ ] `grep -rn "^data class .*State" shared/src/commonMain/**/*ViewModel.kt` → 0 hits
- [ ] Five unused imports gone
- [ ] `LLM.md` §11 rows #7, #12 → Fixed

---

# Phase 3 — Effect declarations must match reality

**Rows:** #1, #2 *(structural half)* · **Risk:** Low · **Estimate:** half a day
**Depends on:** Phase 1

The conformance rule: **every declared Effect has exactly one collector, or it is not
declared.** Two screens break it — `ChatScreen` and `JournalScreen` collect nothing while their
ViewModels emit.

This phase reconciles declaration with reality. It does **not** add UI.

> **Explicitly out of scope:** rendering `JournalState.error` on the failed day row. That is a
> user-visible change and belongs in `docs/bug-report-effect-collection.md` step 2.1. This
> phase leaves `JournalState.error` exactly as it is — written and unread.

### Step 3.1 — Chat: delete the effects that were superseded

| Effect | Emitted at | Why it goes |
|---|---|---|
| `ScrollToBottom` | `ChatViewModel.kt:42`, `:123` | `ChatScreen.kt:113–120` deliberately scrolls from message count instead, with a comment explaining that the row must be laid out first. The effect is a duplicate of a mechanism that already works. |
| `ShowMessage` | `ChatViewModel.kt:110` | `ChatScreen.kt:215` already renders `state.error` with tap-to-dismiss. Same news twice. |

1. Delete both `sendEffect(ChatEffect.ScrollToBottom)` calls and the case in `ChatContract.kt`.
2. Delete `sendEffect(ChatEffect.ShowMessage(...))` at `:110`. **Keep the
   `setState { copy(error = ...) }` beside it** — that is what the screen renders.
3. Leave `ChatScreen.kt:116` untouched, comment included.

**Verify:** no rendered change. Scrolling still follows new messages, the error banner still
appears. Both were already driven by state.

### Step 3.2 — Chat: `RequestMicPermission`

`ChatEffect.RequestMicPermission` is emitted at `ChatViewModel.kt:74`, reachable only through
`ChatIntent.StartListening`, which no composable dispatches.

**Keep the declaration.** It is structurally valid — an effect with a reachable emit site — and
whether Chat gets a mic button is a product decision this plan does not make. Add
`CollectEffects` to `ChatRoute` with a single branch:

```kotlin
CollectEffects(viewModel.effects) { effect ->
    when (effect) {
        // No UI dispatches StartListening yet, so this cannot currently fire. The branch
        // exists so the contract is complete and adding the mic button is a one-line change.
        ChatEffect.RequestMicPermission -> requestMicPermission()
    }
}
```

If wiring the actual permission helper pulls in more than the `ExploreScreen.kt:114` pattern,
leave a `TODO` with the product decision named — do not build a permission flow inside a
structural refactor.

### Step 3.3 — Journal: retire `ShowMessage`

`JournalEffect.ShowMessage` is emitted at `JournalViewModel.kt:146` and collected nowhere.

Delete the effect case and the `sendEffect` call. `JournalEffect` then has no cases — keep it
declared and empty, following the precedent already set in `CollectionViewModel.kt:46`:

```kotlin
/**
 * Nothing to emit.
 *
 * Failures are reported through [JournalState.error]. Declared rather than removed because
 * [MviViewModel] is typed on an effect, and a sealed interface with no cases states exactly
 * what is true.
 */
sealed interface JournalEffect : UiEffect
```

`JournalRoute` needs no collector, the same as `CollectionRoute` and `PassportRoute`.

> **Follow-up, not this plan:** `JournalState.error` is now the only failure path and the
> screen still does not render it. That is the open bug — leave the field, leave the writes,
> and let the bug report close it.

### Step 3.4 — Assert the rule

For every feature, exactly one of:

- effect set is non-empty **and** `XRoute` has a `CollectEffects` with an exhaustive `when`, or
- effect set is empty **and** `XRoute` has no collector.

| Feature | Effects | Collector |
|---|---|---|
| Lens, Discovery, Explore, Settings, Translation | non-empty | required |
| Chat | `RequestMicPermission` only | required (step 3.2) |
| Journal, Collection, Passport | empty | none |
| Sovereignty | empty | none *(Phase 4)* |

### Done when

- [ ] Every non-empty effect set has exactly one collector
- [ ] Every empty effect set is declared with a comment saying why
- [ ] No `sendEffect` call has no possible collector
- [ ] No rendered output changed
- [ ] `LLM.md` §11 rows #1, #2 → Fixed, each annotated *"structural half; UI in bug report"*

---

# Phase 4 — Close the escape hatches

**Rows:** #3, #4, #5, #6 · **Risk:** Medium · **Estimate:** 2–3 days
**Depends on:** Phase 2 (Sovereignty gets a Contract here)

`onIntent` must be the only public method on a screen ViewModel. Three break that, and one
screen is not on `MviViewModel` at all.

### Step 4.1 — `ChatViewModel.onMicPermissionGranted()` → an intent

`ChatViewModel.kt:99`, public, zero call sites.

```kotlin
// ChatContract.kt
sealed interface ChatIntent : UiIntent {
    …
    /** The microphone permission came back granted. */
    data object MicPermissionGranted : ChatIntent
}
```

Move the body into an `onIntent` branch and delete the method. It remains unreachable until a
mic button exists — that is fine and is not this plan's problem. What matters is that the
*call path* is now the standard one.

**Verify:** compiles; `ChatViewModel` has no public method but `onIntent`.

### Step 4.2 — `newCapturePath()` → carried in the effect

`LensViewModel.kt:74` and `DiscoveryViewModel.kt:153` expose a public method the composable
calls (`LensScreen.kt:264`; threaded five levels deep in `DiscoveryScreen.kt` at
`:217 → :236 → :311 → :1634 → :1707`).

**Fix: the ViewModel already decides *when* the shutter fires — let it decide *where the file
goes* at the same moment.**

```kotlin
// LensContract.kt — before
data object TakePhoto : LensEffect
// after
data class TakePhoto(val outputPath: String) : LensEffect
```

`LensViewModel.onShutterPressed()` sends `TakePhoto` from two sites — `:141` (immediate) and
`:151` (after the countdown). Both become
`sendEffect(LensEffect.TakePhoto(captureStore.newCapturePath()))`. Delete the public method;
`LensScreen.kt:264` becomes `controller.capture(effect.outputPath)`.

Discovery's note-photo capture takes the same shape and loses the five-level lambda thread:

```
composable → DiscoveryIntent.NotePhotoRequested
ViewModel  → DiscoveryEffect.TakeNotePhoto(outputPath)
composable → captures, then DiscoveryIntent.NotePhotoCaptured(path)   ← already exists
```

**Verify:** `LensViewModelCrashTest` green — note that `TakePhoto` becoming a `data class`
changes the assertions at `LensViewModelCrashTest.kt:317, 341, 380`, which currently
`expectNoEvents()` or type-check the object. Update those assertions; do not weaken them.
Manually: capture with and without the self-timer, capture into a note, confirm files land in
the same directory and the orphan sweep finds nothing on next launch.

### Step 4.3 — `SovereigntyViewModel` → `MviViewModel`

`feature/sovereignty/SovereigntyViewModel.kt` is a plain `ViewModel` exposing
`StateFlow<RegionMap?>`.

**Honest note:** this screen is read-only, so `SovereigntyIntent` and `SovereigntyEffect` will
both be empty and the effect channel will never be used. The value is not the channel — it is
that ten feature packages present ten identical shapes with no exception to reason about. That
is the promise `LLM.md` makes to whoever reads the codebase next, human or model.
`CollectionEffect` already sets the precedent.

1. Create `SovereigntyContract.kt`:
   ```kotlin
   data class SovereigntyState(
       val isLoading: Boolean = true,
       /** Null when the asset would not parse; the page draws its prose regardless. */
       val map: RegionMap? = null,
   ) : UiState

   /** Read-only screen — the traveller has nothing to send. */
   sealed interface SovereigntyIntent : UiIntent

   /** Nothing to emit. */
   sealed interface SovereigntyEffect : UiEffect
   ```
2. Extend `MviViewModel<SovereigntyState, SovereigntyIntent, SovereigntyEffect>`.
3. Replace the hand-rolled `try/catch` in `load()` with `launchSafely`. **Preserve the existing
   behaviour exactly**: a parse failure logs and leaves `map = null` so the prose still renders.
4. `override fun onIntent(intent: SovereigntyIntent) = Unit`, with a comment saying the screen
   is read-only.
5. Update `SovereigntyRoute` (`SovereigntyScreen.kt:79`) from `map` to `state.map`.

**Verify:** the map renders. Temporarily corrupt `composeResources/files/sovereignty_map.json`
and confirm the page still shows its prose instead of crashing — the behaviour the original
`try/catch` was protecting.

### Step 4.4 — `MainViewModel`: exempt it, in writing

`MainViewModel` is a plain `ViewModel` with two StateFlows. **Leave it as it is**, and write
the rule down so the next reader does not file it as an oversight:

> `MviViewModel` is for **screens** — anything with a route and a back-stack entry.
> `MainViewModel` is the **window host**: no route, owns theme and the splash gate for the
> whole window, read by both the Android Activity and `MainViewController` on iOS. An intent
> channel with no sender and an effect channel with no collector would be ceremony.

Add it to `LLM.md` §3 beside `MainViewModel.kt`, and to
`docs/android-mvi-best-practices.md` §3 as a stated boundary of the rule.

### Done when

- [ ] `grep -rn "^    fun " shared/src/commonMain/**/[A-Z]*ViewModel.kt` returns only `onIntent`
- [ ] All ten features have an `XContract.kt`
- [ ] All ten screen ViewModels extend `MviViewModel`
- [ ] The `MainViewModel` exemption is written into both docs
- [ ] `LLM.md` §11 rows #3, #4, #5, #6 → Fixed

---

## Final conformance check

Run against `shared/src/commonMain`. All four must hold:

```bash
# 1. Every feature has a contract file
ls feature/*/ | grep -c "Contract.kt"                    # 10

# 2. No contract type declared inside a ViewModel
grep -rn "^data class .*State\|^sealed interface .*Intent" **/*ViewModel.kt   # 0

# 3. onIntent is the only public method
grep -rn "^    fun " **/[A-Z]*ViewModel.kt               # only onIntent

# 4. One effect collector, shared
grep -rn "effects.collect" .                             # 1, inside CollectEffects
```

Plus: `LensViewModelCrashTest` and `ComposeStabilityReportTest` green, and `LLM.md` §11 Open
contains only row #11.

---

## Tracking

| Phase | Rows | Est. | Depends on | Status |
|---|---|---|---|---|
| 1 — One effect collector | #8 #9 #10 | 1 d | — | ☐ Not started |
| 2 — Contract extraction | #7 #12 | 0.5 d | — | ☐ Not started |
| 3 — Declarations match collectors | #1 #2 | 0.5 d | Phase 1 | ☐ Not started |
| 4 — Close escape hatches | #3 #4 #5 #6 | 2–3 d | Phase 2 | ☐ Not started |

Phases 1 and 2 are independent and can run in either order or in parallel.

---

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Step 1.6 breaks the self-timer or leaves the shutter stuck | Medium | The riskiest step in the plan. Two manual checks plus the fuzz test. |
| Step 4.2's `TakePhoto` signature change breaks existing tests | **Certain** | Three assertions in `LensViewModelCrashTest` reference it. Update them, do not weaken them. |
| Contract extraction silently drops a KDoc | Medium | Review `git diff --stat` for symmetry; several comments document non-obvious defaults. |
| A "structural" change alters rendered output | Low | Ground rule 2. Phase 3 is where this is most likely — Chat's deletions are only safe because state already drives both behaviours. |
| Phase 4 lands after feature work starts on Lens or Discovery | Medium | `TakePhoto` gains a parameter. Do Phase 4 before any feature touches the capture path. |

**Security:** nothing here touches the API key path. One thing to preserve in Phase 2:
`SettingsViewModel` clears `apiKeyDraft` after a save so the field never keeps a secret on
screen.

---

## Appendix — what this plan deliberately leaves alone

**Screen file sizes (`LLM.md` §11 row #11).** Six files exceed the 200-line project rule, up to
`LensScreen.kt` at 2,170. Line count is not an MVI rule, so it is out of this plan's mandate —
and it is not safe mechanical work: extracting a composable changes its recomposition scope,
and an unstable parameter makes the child non-skippable, a performance regression no test here
catches.

The standing rule instead: **any file you open for feature work, you split before you add to
it.** Split by cohesion, never at an arbitrary line, and confirm in `build/compose-reports`
that the extracted composable is `skippable` before committing.

**The two open defects.** Phase 3 makes Chat's and Journal's effect *declarations* honest. It
does not make Journal tell the user when a day summary fails — `JournalState.error` is still
written and never rendered. That work is scoped in
`docs/bug-report-effect-collection.md` and is deliberately not merged into this plan.
