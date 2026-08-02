# Base Project — AI Configuration

Reusable `.claude/` setup for **mobile app** projects: skills, agents, and rules only — no application code lives in this directory.

Derived from ClaudeKit and trimmed from 73 skills down to **28**, keeping what serves mobile app work plus what the bundled agents actually call. Vendor-neutral: no dependency on any organization's private template or SDK.

To start a real project: copy `.claude/` over, then replace this file with that project's description.

> **Current state of this working copy:** the repo root also contains `docs/SRS.md` and `prototype/index.html` for a product called **OneRoll** (a disposable film camera app). That content is unrelated to the reusable config below — either move it to its own project or rewrite this file to describe OneRoll.

## Skills

Many skills carry a `ck:` prefix in their frontmatter `name` (`ck:plan`, `ck:cook`, `ck:docs`, …) so they don't collide with Claude Code's built-in skills and commands. Invoke them with the prefix.

### Mobile app pipeline

| Skill | Use when |
|---|---|
| `project-kickoff` | Step 1 — Kickoff XLSX from an idea/use case: user segments, competitor analysis, scope. |
| `product-requirements` | Step 2 — generate the PRD. |
| `qa-uat` | Step 3 — generate QA test cases and UAT scenarios from the PRD. |
| `ui-design-pencil` | Step 4 — wireframes (`.pen`) from the PRD via Pencil MCP: screen layout, navigation flow, component specs. |
| `ui-ux-pro-max` | Design intelligence: 84 styles, 192 palettes, 74 font pairings, 98 UX guidelines, 25 chart types, 22 stacks. Searchable local CSV database. |
| `mobile-development` | Build the app: React Native, Flutter, Swift/SwiftUI, Kotlin/Jetpack Compose. |
| `ai-multimodal` | Gemini image generation and vision — mockups, describing images/video/documents. Requires `GEMINI_API_KEY`. |
| `media-processing` | FFmpeg (video/audio), ImageMagick (images), RMBG (background removal) — app icons, screenshots, assets. |
| `document-skills` | Four sub-skills: `ck:xlsx`, `ck:docx`, `ck:pptx`, `ck:pdf`. `project-kickoff` depends on `ck:xlsx` to emit the Kickoff spreadsheet. |

Kickoff → PRD → QA-UAT are exactly the three skills the `ba-writer` agent declares it needs.

### Dev workflow

Kept because the 16 agents in `agents/` call them directly — remove one and the corresponding agent runs degraded.

| Skill | Use when | Agent |
|---|---|---|
| `plan` | Implementation plans, architecture, phased roadmaps | `planner` |
| `cook` | Implement a feature (after `plan`, or standalone) | — |
| `code-review` | Review code | `code-reviewer` |
| `scout` | Surface edge cases the diff doesn't reveal | `code-reviewer` |
| `test` | Run and analyze tests | `tester` |
| `debug` / `fix` | Investigate and fix defects | `debugger` |
| `docs` | Analyze the codebase, manage documentation | `docs-manager` |
| `docs-seeker` | Fetch current docs for packages/plugins | `debugger`, `researcher`, `brainstormer` |
| `research` | Technical research | `researcher` |
| `brainstorm` | Ideation with trade-off analysis | `brainstormer` |
| `sequential-thinking` | Break complex problems into ordered steps | `tester`, `brainstormer` |
| `project-management` | Track progress, update plan status, report | `project-manager` |
| `team` | Team mode — coordinate multiple agents | several agents |
| `git` | Stage, commit, push using conventional commits | `git-manager` |
| `journal` | Record hard bugs and repeated test failures | `journal-writer` |
| `worktree` | Git worktrees for parallel work. Requires a git repo. | — |
| `ck-help` | List installed skills. Scans `.claude/skills/` live, so it always matches reality. | — |
| `common` | Shared library (`api_key_helper.py`, `api_key_rotator.py`) imported by `ai-multimodal`. Not invoked directly. | — |

## Agents (16)

- **Plan & research** — `planner`, `researcher`, `brainstormer`
- **Implement** — `fullstack-developer`, `code-simplifier`
- **Review & test** — `code-reviewer`, `tester`, `debugger`
- **Design** — `ui-ux-designer`, `ui-designer`
- **Docs & BA** — `docs-manager`, `ba-writer`, `journal-writer`
- **Ops** — `project-manager`, `git-manager`, `mcp-manager`

## Rules

Read these when the task touches them:

- `development-rules.md` — coding standards; referenced by `code-reviewer` and `fullstack-developer`
- `primary-workflow.md` — the main working loop
- `orchestration-protocol.md` — multi-agent orchestration
- `team-coordination-rules.md` — coordination between agents
- `documentation-management.md` — documentation conventions

## Config

| File | Role |
|---|---|
| `.ck.json` | Plan naming format (`{date}-{issue}-{slug}`), `paths.docs` / `paths.plans`, Gemini model. Read by `research`, `scout`, `ai-multimodal`, `ck-help`. |
| `schemas/ck-config.schema.json` | JSON Schema for `.ck.json`, referenced via `$schema`. Not used at runtime — it exists so editors give autocomplete and validation when `.ck.json` is edited by hand. |
| `.env.example` | Environment variable template. Copy to `.env` and fill in — **never commit `.env`**. |
| `.mcp.json.example` | MCP server template. `cp .claude/.mcp.json.example .mcp.json` to enable project-scoped servers. |

### MCP servers

The template lists only the servers this skill set actually uses:

| Server | Needed by |
|---|---|
| `pencil` | `ui-design-pencil` — 71 references, the hard requirement of this set |
| `sequential-thinking` | the `sequential-thinking` skill |
| `context7` | docs lookup for `docs-seeker`; paste `CONTEXT7_API_KEY` into its `--api-key` argument |

Two servers from the upstream template were dropped: `chrome-devtools` (its skill was removed) and `human-mcp` (not a real dependency — it appears once in `plan/references/research-phase.md` as an example URL for `repomix`).

The `pencil` entry points at a local macOS arm64 binary under `/Applications/Pen.app/`. Verified present on this machine; on a different machine or architecture, read the correct path from that install first. Servers may also be registered globally in `~/.claude.json` instead of per project.

`.ck.json` has been stripped of keys that pointed at machinery not carried over (`statusline`, `privacyBlock`, `codingLevel`, `trust`, `hooks`, `assertions`). The schema declares no `required` fields and allows additional properties, so the trimmed file still validates, and any key you add back will still autocomplete.

### Statusline

`settings.json` wires exactly two things — a statusline and the one hook that feeds it. The other nine hooks from upstream are still deliberately absent.

```
🤖 Opus 5  ▰▰▰▰▰▰▰▰▱▱▱▱ 65%  ⌛ 1h 37m left (15% used)  📁 ~/project  📝 +42 -7
           context window          usage limit
```

- **`65%`** — how full the context window is, computed by `statusline.cjs` from what Claude Code pipes in. Equivalent to `/context`.
- **`1h 37m left (15% used)`** — remaining 5-hour quota. Equivalent to `/usage`, but always visible instead of on demand.

Two moving parts, both required:

| File | Role |
|---|---|
| `hooks/usage-context-awareness.cjs` | Fetches `https://api.anthropic.com/api/oauth/usage`, writes `$TMPDIR/ck-usage-limits-cache.json`. Cached 60s, and declared `async` so it never blocks a turn. Wired to `UserPromptSubmit` + `PostToolUse`. |
| `statusline.cjs` | Reads that cache and renders. Requires `hooks/lib/`: `colors`, `transcript-parser`, `config-counter`, `git-info-cache`, `ck-config-utils`. |

Without the hook the usage segment shows `N/A`; without the statusline nothing reads the cache.

To turn the fetching off, set `hooks.usage-context-awareness` to `false` in `.ck.json` (`isHookEnabled()` treats a missing key as enabled). To drop the statusline, remove the `statusLine` block from `settings.json`.

Note that `hooks/lib/ck-config-utils.cjs` is the single copy of that library — `scripts/set-active-plan.cjs` reaches it via `../hooks/lib/`.

## Scripts

```bash
# Search the design database
python3 ".claude/skills/ui-ux-pro-max/scripts/search.py" "<query>" --domain <style|color|typography|ux|chart|product>
python3 ".claude/skills/ui-ux-pro-max/scripts/search.py" "<query>" --design-system -p "Project Name"

# Set the active plan (used by the `plan` skill and `planner` agent)
# NOTE: currently a no-op — see "Scripts that need hooks" below
node .claude/scripts/set-active-plan.cjs plans/<YYMMDD-HHMM-feature-name>

# Validate docs (used by the `docs` skill and `docs-manager` agent)
node .claude/scripts/validate-docs.cjs

# List installed skills
python3 .claude/skills/ck-help/scripts/ck-help.py

# Git worktrees (create | remove | info | list)
node .claude/skills/worktree/scripts/worktree.cjs <command>

# Debug env var resolution order
python3 .claude/scripts/resolve_env.py --show-hierarchy
```

`scripts/lib/ck-config-utils.cjs` is a shared library (30 exports: `.ck.json` loading, plan-path resolution, naming patterns, git helpers, session state). Here only `set-active-plan.cjs` imports it.

### Scripts that need hooks

`set-active-plan.cjs` writes the active plan into a session temp file at `/tmp/ck-session-{id}.json`, keyed by the `CK_SESSION_ID` environment variable. That variable is set by the upstream `session-init.cjs` / `subagent-init.cjs` hooks, which this project deliberately does not carry. With no hook, the script prints `Warning: CK_SESSION_ID not set`, reports what it *would* do, and exits 0 without writing anything.

So the `plan` skill and the `planner` agent will still tell you to run it, and it will still appear to succeed — but it stores nothing. Either accept that plan context is passed explicitly in prompts instead, or restore the upstream `hooks/` directory plus `settings.json` to make it work.

Working standalone with no hooks: `validate-docs.cjs`, `resolve_env.py`, `ck-help.py`, `search.py`, `worktree.cjs`.

## External dependencies

Present: `node` v24.18.1, `python3` 3.9.6, `git` 2.50.1, and the Python packages `Pillow`, `pdf2image`, `six`.

Still missing — the dependent skills will fail until these are installed:

```bash
# document-skills (xlsx / docx / pptx / pdf).
# project-kickoff cannot emit its Kickoff spreadsheet without this.
pip3 install defusedxml openpyxl python-pptx pypdf lxml

# media-processing
brew install ffmpeg imagemagick
```

## Local modifications to upstream skills

Intentional — don't revert them:

| Skill | Change | Why |
|---|---|---|
| `ui-ux-pro-max` | `${CLAUDE_PLUGIN_ROOT}/...` → project-relative paths | That variable is only set for plugin installs; as a project skill it resolves empty and the path breaks. |
| `ui-ux-pro-max`, `ck-help` | `python` → `python3` | No bare `python` on this machine. |
| `ui-design-pencil` | `.claude/skills/.venv/bin/python3` → `python3` | That shared venv does not exist here; the target script needs no third-party packages. |
| `qa-uat` | Added YAML frontmatter (`name`, `description`) | Upstream shipped without it, so Claude Code could not discover the skill at all. |
| `project-kickoff` | Default feature renamed to `App Open Ads` | Removes a vendor-specific SDK name; story points unchanged, so the 13 SP system total still holds. |

## Known dangling references

A few agents and rules still mention skills that were deliberately dropped. They lose that capability but do not crash.

| Reference | Where | Substitute |
|---|---|---|
| `mcp-management` | `agents/mcp-manager.md` | none — that agent runs with reduced capability |
| `chrome-devtools` | `agents/debugger.md` | none — browser automation, not applicable to mobile |
| `imagemagick` | `rules/development-rules.md` | use `media-processing` |
| `problem-solving` | `rules/development-rules.md` | use `sequential-thinking` |
| `context-engineering` | rules | none |

## Notes when copying to a new project

- Script paths inside `SKILL.md` files are **relative to the project root** — run them from the root, not from inside the skill directory.
- `ui-design-pencil` needs the Pencil MCP server configured for the session.
- `ai-multimodal` and `research` (Gemini mode) need `GEMINI_API_KEY`; `docs-seeker` optionally uses `CONTEXT7_API_KEY`.
