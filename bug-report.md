# Bug Report — uncollected MVI effect channels

**Module:** `:shared` (`/Users/macmini0021/Downloads/shared`)
**Reported:** 2 Aug 2026
**Found by:** architecture audit of the presentation layer (84 Kotlin files)
**Related:** `LLM.md` §11 rows #1, #2, #10 · `docs/android-mvi-best-practices.md` §4

---

## Summary

Two screens declare a `UiEffect` type, their ViewModels emit into it, and **no composable
ever collects it**. Both share one root cause: there is no shared effect-collection helper,
so each of the five screens that do collect effects hand-rolls the same block — and two
screens simply never got one.

| ID | Screen | User-visible? | Class | Severity |
|---|---|---|---|---|
| [BUG-1](#bug-1) | Chat | No | Suspended-coroutine leak + unreachable feature code | **Medium** |
| [BUG-2](#bug-2) | Journal | **Yes** | Silent failure — no feedback when AI summary fails | **High** |
| [BUG-3](#bug-3) | — | No | Systemic: nothing prevents a third occurrence | **Medium** |

### Correction to the earlier verbal report

Two claims made before the code was read line-by-line were wrong, and the fix differs
because of it:

- *"The mic button in Chat cannot work."* — There is **no mic button**. `ChatComposer` is a
  text field and a send button. The voice feature was built on the ViewModel side and the UI
  side was never written. This is unreachable code, not a broken control.
- *"ChatScreen forgot to collect `ScrollToBottom`."* — It did not forget. `ChatRoute:113`
  carries a comment explaining that scrolling is deliberately driven by message count
  instead, so the row is laid out before the scroll runs. The **emissions** are the orphans,
  not the collector.

Net effect: BUG-1 causes no loss of function, only a leak and dead code. BUG-2 is the one
a user actually feels.

---

## Shared mechanism

`MviViewModel` (`core/mvi/MviViewModel.kt:47`):

```kotlin
private val _effects = Channel<E>(Channel.BUFFERED)
val effects = _effects.receiveAsFlow()

protected fun sendEffect(effect: E) {
    viewModelScope.launch { _effects.send(effect) }
}
```

`Channel.BUFFERED` resolves to `CHANNEL_DEFAULT_CAPACITY` — **64** unless the
`kotlinx.coroutines.channels.defaultBuffer` system property overrides it. `receiveAsFlow()`
is cold: with no collector nothing ever drains the buffer.

So with no collector:

1. Sends 1–64 complete immediately into the buffer.
2. Send 65 onward **suspends** inside a `viewModelScope.launch` that will never resume.
3. Each suspended send holds one coroutine and one effect object alive.

**Bound on the damage:** these coroutines are cancelled when `viewModelScope` is cancelled
in `onCleared()`, so the leak dies with the screen's back-stack entry rather than with the
process. It is wasted allocation per screen visit, not an unbounded process-lifetime leak.
That is why BUG-1 is Medium and not Critical.

---

<a id="bug-1"></a>
## BUG-1 — `ChatScreen` never collects `ChatEffect`

**Severity:** Medium · **User-visible:** No · **Status:** Open

### Location

| Role | File | Line |
|---|---|---|
| Effects declared | `feature/chat/ChatContract.kt` | 38–42 |
| Effects emitted | `feature/chat/ChatViewModel.kt` | 42, 74, 110, 123 |
| Collector | `feature/chat/ChatScreen.kt` | **absent** |
| Unreachable method | `feature/chat/ChatViewModel.kt` | 99 |

### Evidence

`ChatRoute` (`ChatScreen.kt:100–129`) collects `state` and nothing else — there is no
`viewModel.effects` reference anywhere in the file:

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
val listState = rememberLazyListState()
PinSystemBarIcons(darkIcons = true)

LaunchedEffect(state.messages.size, state.isSending) { … }   // state-driven scroll

ChatScreen(state = state, listState = listState, onIntent = viewModel::onIntent, …)
```

### Per-effect analysis

| Effect | Emitted at | Reachable? | Consequence |
|---|---|---|---|
| `ScrollToBottom` | `ChatViewModel:42` (every `observeChat` emission), `:123` (every successful send) | Yes, frequently | **The leak driver.** Superseded by the state-driven scroll at `ChatScreen:116`, which works correctly. |
| `ShowMessage(error)` | `ChatViewModel:110` (send failure) | Yes, on failure | Redundant — `ChatScreen:215` already renders `state.error` inline with a tap-to-dismiss. |
| `RequestMicPermission` | `ChatViewModel:74` | **No** | Requires `ChatIntent.StartListening`, which no composable dispatches. |

### Leak rate

`observeChat(discoveryId)` emits on subscribe and on every write to the thread. One
question-and-answer inserts the user message and the assistant message, and the success path
sends a third `ScrollToBottom` — roughly **3 effects per exchange**.

> 64 ÷ 3 ≈ **~21 exchanges in a single chat session** before every subsequent `sendEffect`
> leaves a permanently suspended coroutine behind.

A long conversation about one discovery reaches this. It is not a theoretical bound.

### Secondary finding — the voice feature is half-built

Reachable only through `ChatIntent.StartListening`, which nothing dispatches:

- `ChatViewModel:52` — `speechRecognizer.state.onEach(::handleVoiceState).launchIn(...)`
- `ChatViewModel:69–80` — `StartListening` / `StopListening` branches
- `ChatViewModel:99` — `fun onMicPermissionGranted()`, public, zero call sites
- `ChatViewModel:129–149` — `handleVoiceState(...)`
- `ChatState.isListening` — written, never read by any composable
- `ChatEffect.RequestMicPermission`
- `SpeechRecognizerManager` injected into `ChatViewModel` and effectively unused

`onMicPermissionGranted()` is also an MVI violation in its own right — a public method that
bypasses `onIntent` (`LLM.md` §11 row #3).

### Fix

Pick one of two directions, and do the same thing to all seven items above.

**Option A — finish the feature.** Add the mic button to `ChatComposer`, wire the permission
flow, collect the effects. Convert `onMicPermissionGranted()` to
`ChatIntent.MicPermissionGranted`.

**Option B — remove it.** Delete the voice wiring from `ChatViewModel` and the mic entries
from `ChatContract`, and drop `SpeechRecognizerManager` from the constructor and from the
Koin binding in `SharedModules.kt:151`.

Independently of A/B, and required either way:

1. Delete the two `sendEffect(ChatEffect.ScrollToBottom)` calls at `ChatViewModel:42` and
   `:123`. The state-driven scroll at `ChatScreen:116` is the correct mechanism and is
   already documented as the deliberate choice — the effect is a duplicate.
2. Either delete `ChatEffect.ShowMessage` (the inline banner already covers it) or collect
   it. Do not leave it emitted-and-uncollected.
3. Add the `CollectEffects` call to `ChatRoute` for whatever effects survive.

---

<a id="bug-2"></a>
## BUG-2 — a failed day summary tells the user nothing

**Severity:** High · **User-visible:** **Yes** · **Status:** Open

### Location

| Role | File | Line |
|---|---|---|
| Effect declared | `feature/journal/JournalViewModel.kt` | 68–70 |
| Effect emitted | `feature/journal/JournalViewModel.kt` | 146 |
| `error` written to state | `feature/journal/JournalViewModel.kt` | 130, 141, 144 |
| Collector | `feature/journal/JournalScreen.kt` | **absent** |
| `state.error` rendered | `feature/journal/JournalScreen.kt` | **absent** |

### Evidence

Both channels for reporting the failure are dead. A case-insensitive search for `error`
across all 806 lines of `JournalScreen.kt` returns **nothing**, and the file contains no
reference to `viewModel.effects`. The four intents it dispatches are `SetFavoritesOnly`,
`GenerateSummary` ×2 and `ToggleFavorite` — `JournalIntent.DismissError` is never sent,
because there is no error UI to dismiss.

### Reproduction

1. Open Journal with a day that has captures but no written narrative.
2. Put the device in airplane mode, or use an invalid Gemini key, or exhaust the quota.
3. Tap the generate-summary control on that day.

**Expected:** an explanation — offline, invalid key, rate limited — and a way to retry.

**Actual:** the spinner runs, `generatingDate` returns to `null`, the row settles back to
exactly how it looked before, and nothing is said. The relevant code
(`JournalViewModel:143–151`):

```kotlin
when (val result = generateDaySummary(date)) {
    is AppResult.Failure -> {
        setState { copy(generatingDate = null, error = result.error) }   // never rendered
        sendEffect(JournalEffect.ShowMessage(result.error))              // never collected
    }
    is AppResult.Success -> setState { copy(generatingDate = null) }
}
```

`JournalIntent.ToggleFavorite` has the same problem — its `launchSafely(onError = { setState
{ copy(error = it) } })` at `:130` writes to the same unread field.

### Why this is High and BUG-1 is Medium

The user asked for something, waited, and was told nothing. The failure modes here are the
ordinary ones for this product — `AppError.NoConnection`, `RateLimited`, `AllModelsBusy`,
`InvalidApiKey` are all first-class cases in the domain layer — so this is the common path
for a traveller with a weak signal, not an edge case. A silent failure reads as a broken
button, and the natural response is to tap it again, which spends another model call.

The leak side of BUG-2 is negligible by comparison: filling a 64-slot buffer needs 64 failed
generations in one screen lifetime.

### Fix

1. Render `state.error` in `JournalScreen` — either a dismissible banner above the day list
   (matching `ChatScreen:215`) or an inline message on the day row that failed, which is
   better here because the failure belongs to one day.
2. Wire `JournalIntent.DismissError` to that UI.
3. Add `CollectEffects` to `JournalRoute` for `JournalEffect.ShowMessage`, **or** delete the
   effect if the inline error is judged sufficient. Prefer inline-only: the error is
   attached to a specific row, and a snackbar would leave the failed row looking untouched.
4. Consider carrying the failed date in the error state so the message can sit on the right
   row rather than at the top of the screen.

---

<a id="bug-3"></a>
## BUG-3 — nothing stops this happening again

**Severity:** Medium · **User-visible:** No · **Status:** Open

### The actual root cause

Five screens collect effects. Each hand-rolls the same block, and no two are identical:

| Screen | Operator | Wrapper |
|---|---|---|
| `LensScreen:248` | `collectLatest` | `LaunchedEffect(viewModel)` |
| `SettingsScreen:145` | `collectLatest` | `LaunchedEffect(viewModel)` |
| `DiscoveryScreen:204` | `collectLatest` | `LaunchedEffect(viewModel)` |
| `ExploreScreen:119` | `collect` | `LaunchedEffect(viewModel)` |
| `TranslationScreen:129` | `collect` | `LaunchedEffect(viewModel)` |
| `ChatScreen` | — | **missing** |
| `JournalScreen` | — | **missing** |

Two further defects are visible in that table:

- **`collectLatest` is wrong for an effect channel.** It cancels the handling of the
  previous effect when the next arrives — a navigation half-performed. `LensScreen:257`
  already works around this by re-launching the capture in a separate
  `rememberCoroutineScope()`, with a comment explaining why. That workaround is evidence the
  operator is wrong, not that the problem is solved.
- **None is lifecycle-aware.** `LaunchedEffect(viewModel)` keeps collecting while the
  destination is stopped but still composed. `repeatOnLifecycle(STARTED)` is correct, and
  costs nothing because the channel buffers meanwhile.

### Fix — one helper, then convert all seven screens

`core/mvi/CollectEffects.kt`:

```kotlin
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

Then:

1. Convert the five existing collectors, dropping `collectLatest`.
2. Add it to `ChatRoute` and `JournalRoute` for whichever effects survive BUG-1 and BUG-2.
3. Re-check `LensScreen:257` — with `collect` instead of `collectLatest`, the separate
   capture scope may still be wanted (the capture should outlive the collector), but the
   stated reason for it no longer applies and the comment must be corrected either way.

### Prevention

A one-line rule in review is not enough — this failed twice already. Add a real gate:

- **A test per feature** asserting every declared effect reaches a handler. The sealed
  hierarchy makes this enumerable, and Turbine is already a test dependency.
- **A checklist item** in `docs/android-mvi-best-practices.md` §9 — *"Every declared Effect
  has a branch"* — which is present; the gap is that nothing enforces it.
- Consider making the ViewModel log at warn level when a `send` suspends, so an
  uncollected channel is noisy in a debug build rather than silent.

---

## Suggested order

| Step | Work | Why first |
|---|---|---|
| 1 | BUG-3 — add `CollectEffects` | Both fixes below need it, and it prevents a third occurrence. |
| 2 | BUG-2 — render `state.error` in Journal | The only user-visible defect of the three. |
| 3 | BUG-1 — decide A or B on the voice feature, then delete the orphaned `ScrollToBottom` emissions | Blocked on a product decision, not on code. |
| 4 | Convert the five `collectLatest` collectors | Latent, no known symptom yet, but the same class of defect. |

Update `LLM.md` §11 as each lands: move rows #1, #2, #8, #9, #10 from **Open** to **Fixed**
with the commit reference.

---

## Verification

Add to `commonTest`, one per fix:

```kotlin
@Test
fun `a failed day summary surfaces an error the screen can render`() = runTest {
    generateDaySummary.result = AppResult.Failure(AppError.NoConnection)
    val vm = viewModel()

    vm.onIntent(JournalIntent.GenerateSummary(someDate))
    settle()

    assertNull(vm.state.value.generatingDate, "the spinner must stop")
    assertEquals(AppError.NoConnection, vm.state.value.error, "and say why")
}
```

For the leak, assert the contract rather than the coroutine count — emit past the buffer
capacity with no collector and confirm the ViewModel still responds to intents:

```kotlin
@Test
fun `the screen stays responsive past the effect buffer capacity`() = runTest {
    val vm = viewModel()
    repeat(100) { /* drive whatever emits the effect */ }
    settle()

    vm.onIntent(SomeIntent.Trivial)
    assertEquals(expected, vm.state.value.field, "state updates must not be blocked")
}
```

Manual check for BUG-2: airplane mode, tap generate, confirm a message appears and a retry
is offered.
