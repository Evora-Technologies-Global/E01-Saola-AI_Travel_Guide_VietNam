# Phase 03 — Dead code, and the design system's own convention

**Priority:** medium · **Status:** complete for what was safe · **Depends on:** phase 02

An audit of the whole codebase for dead code and layering violations. Most of it came back
clean, which is worth stating as plainly as the findings: the compiler reports **zero**
"never used" warnings across all four modules, and of 278 strings exactly one is
unreferenced from Kotlin — `app_name`, which `AndroidManifest.xml` uses.

## Context

- [`plan.md`](plan.md) — the pass this came out of
- `LLM.md` §10 — where a new file goes; §12 — what is deliberate and must not be "improved"
- `.claude/rules/development-rules.md` — the 200-line file rule

## What the audit found

| Finding | Verdict |
|---|---|
| Two unreferenced colour constants — `LacquerRedLight`, `JadeLight` | **Removed.** Unused rungs of a documented ramp whose siblings are all in use |
| `core/designsystem/component/Components.kt`, 446 lines, nine unrelated composables | **Split.** See below |
| `SovereigntyViewModel` reads `Res.readBytes` directly | **Recorded, not fixed.** `LLM.md` §11 row #27 |
| A note that fails to save reports nothing | **Recorded, not fixed.** `LLM.md` §11 row #26 |
| Five screen files over 200 lines | **Left alone.** `LLM.md` §11 row #11 already tracks these |
| `BackHandler` deprecated in favour of `NavigationEventHandler`, 7 call sites | **Left alone.** A behavioural migration, not a cleanup |

### The split

`Components.kt` held `LoadingState`, `ErrorState`, `EmptyState`, `AccentChip`, `ShimmerBox`,
`Kicker`, `BackChip`, `FillGauge` and `SectionHeader` in one file — in the one directory
whose stated convention is one composable per file. §5 of `LLM.md` argues that convention for
`feature/*/component/` and five refactors have already applied it there. The design system was
the last place contradicting it, which is the worst place for it: this is where a reader goes
to find out what a shared component looks like.

Ten files now, named after what they draw, plus `SurfaceLuminance.kt` for `isLightSurface` —
which was private to the grab-bag while `AppAsyncImage` needed it too.

**Not one call site changed.** The split is inside a single package, so every existing import
already resolved. That is what made it worth doing now rather than scheduling: the diff is
pure movement, and the test suite and the device suite both confirm it.

## Why the two recorded findings were not fixed here

Both are real and both would widen this change past what it can be verified against.

- **Row #26 (a silent note failure)** needs `DiscoveryEffect.ShowMessage` plus an
  `AppSnackbarHost` in *both* `mobile/feature/discovery/` and `tablet/feature/discovery/`.
  That is UI work in two branches, and the bug it would complete — the composer being
  permanently unusable — is already fixed in phase 02.
- **Row #27 (`Res.readBytes` in a ViewModel)** needs a port in `:domain`, an implementation in
  `:data` and a Koin binding. It is the correct fix and it is a layering change; scheduling it
  honestly beats bolting it onto a cleanup phase.

Deliberately not fixed, deliberately written down. A finding recorded in §11 is one the next
person can pick up; a finding half-fixed is one nobody can see the shape of.

## Todo

- [x] Compiler warning sweep across all four modules
- [x] Unreferenced top-level declaration scan
- [x] Unreferenced string resource scan
- [x] Remove the two dead colours
- [x] Split `Components.kt` one composable per file
- [x] `:shared:allTests` still green on both platforms
- [x] `:shared:connectedAndroidDeviceTest` still 12 / 12
- [x] All four targets re-verified at runtime
- [x] `LLM.md` §3 tree and §11 updated
- [ ] Row #26 — report a failed note save
- [ ] Row #27 — invert the sovereignty asset dependency

## Success criteria

- No behaviour change. **Met** — 385 unit tests and 12 device tests green before and after,
  and the tablet renders identically to the pre-refactor screenshot.
- Nothing deleted that was in use. **Met** — the two colours were verified unreferenced across
  every `.kt`, and the compiler agrees.

## Risk assessment

Low, and the residual risk is worth naming: a scan for "referenced nowhere" is textual, so a
name reached only from XML or from Swift would look dead. Both were checked — that is how
`app_name` was cleared — and neither removed colour appears outside Kotlin.
