# ProjectLibre (microProject) — Issue Triage & Action Plan

Generated: 2026-08-19. Scope: all 38 open issues on `tetsuji16/ProjectLibre`.

## Summary

| Bucket | Issues | Decision |
|---|---|---|
| Bug (must fix) | #227 | **Fixed this session** (regression test + LinkedHashMap ordering) |
| Safe perf/i18n micro-fixes | #228,#229,#230,#231,#232,#233,#234,#235,#236,#237,#239,#240,#250,#222,#251,#252,#253 | **Plan only** — broad mechanical edits, risky under AGENTS.md "no broad refactor" |
| Large refactors / consolidations | #212,#244,#245,#258,#259,#260,#261,#262,#152 | **Plan + design PRs required** — never blind mass-rename (AGENTS.md) |
| Feature requests | #215,#204,#179,#84,#63,#62,#61,#60,#47,#46,#36 | **Plan + scope confirmation** — spec judgment needed per item |

Total open: 38 (1 bug, 26 enhancement, 1 clean-room, 10 untagged feature requests).

---

## What was actually changed this session

### #227 — POD round-trip content drift (BUG, fixed)
**Root cause found by reproduction:** Saving a freshly loaded project twice
(`load -> save -> load -> save`) does **not** grow the file (size is stable:
76995 == 76995 in the repro), so the "namespace migration size increase"
described in the issue is **not currently happening**. What *is* happening is a
1-byte divergence at a fixed offset in the serialized stream, caused by
non-deterministic map ordering inside the serialized `ProjectData`
(`fieldValues` from `FieldValues.getValues`, `extraFields`).

**Changes:**
- `modules/micrproject_core/.../field/FieldValues.java` — `getValues()` now
  returns `LinkedHashMap` (insertion order preserved) instead of `HashMap`.
- `modules/micrproject_core/.../field/HasExtraFieldsImpl.java` — `getExtraFields()`
  lazy init uses `LinkedHashMap`.
- `modules/micrproject_core/.../pm/task/Project.java` — `getExtraFields()` lazy
  init uses `LinkedHashMap`.
- `modules/micrproject_exchange/.../exchange/PodRoundTripTest.java` — added
  `podSaveDoesNotGrowOnRoundTrip` regression test (guards against unbounded
  file growth across round-trips).

**Verification:** `:micrproject_exchange:test` passes (55 tests), including the
new regression test.

**Known remaining gap:** byte-for-byte idempotency is NOT yet guaranteed.
Eliminating the last 1-byte divergence requires stabilizing the *entire*
serialized graph (all collections + serialization handle assignment). That is a
large, risky refactor — tracked below as a dedicated design task, not done here.

> Note: a naïve try-with-resources rewrite of `SerializeUtil.serialize` (ZIP
> path) was attempted and **reverted** — it broke serialization with
> `ZipException: no current ZIP entry`. The ZIP+ObjectOutputStream close-ordering
> is fragile; leave it alone unless doing a careful, tested rewrite.

---

## Bucket 2 — Safe perf/i18n micro-fixes (PLAN ONLY — do not batch)

These are mechanically valid but touch 100+ sites each. Per AGENTS.md
("avoid broad refactors unless they remove duplicated responsibility directly
involved in the bug", "smallest change that fixes the root cause"), they are
**not** done in this session. Each should be its own PR after the rename
stabilizes, with module-level test runs.

| # | Title | Mechanical fix | Risk if batched blindly |
|---|---|---|---|
| #228 | 110 ArrayList + 43 HashMap without initial capacity | add capacity args | huge diff, easy to mis-estimate size → churn |
| #229 | 13 FileInputStream + 11 FileOutputStream w/o try-with-resources | convert to try-with-resources | **some are fragile** (SerializeUtil ZIP path proved this) — per-site care |
| #230 | 25 String.split() without limit | add `-1` limit | low, but 25 sites |
| #231,#240 | Serializer creates Date objects per assignment/timestamp | reuse/cache | hot path, measure first |
| #232 | Enum.values() called 21× in loops | cache the array | low |
| #233 | ResourceLevelingService 5 ArrayLists per call | hoist/scratch | profile first |
| #234 | 24 large static collections should be immutable | `List.of`/`Map.of` | init-order pitfalls |
| #235 | ScheduleEvent `new String()` for sentinels | drop redundant ctor | low |
| #236 | Serializer StringBuilder logging allocs | guard/level-check | low |
| #237 | 9 reflection calls in hot paths | cache `Method`/`Field` | low |
| #239 | ThreadLocal without cleanup | `remove()` in finally | session-scoped, verify lifecycle |
| #250,#222 | `Messages.getString()` + string concat breaks word order | use `MessageFormat` | **i18n-correctness win**, but 45 sites; must keep key names |
| #251 | Hardcoded English strings in UI (30+) | move to `Messages` | 30+ sites, UI strings |
| #252 | Locale-sensitive ops without explicit Locale | pass `Locale` | correctness; affects formatting tests |
| #253 | 680 unused keys in properties | prune | **verify no code references them first** (dynamic key build) |

**Recommended execution order:** #253 (prune dead keys) → #252 (Locale) →
#250/#222 (MessageFormat) → #251 (hardcoded strings) → perf items #228–#240 in
small per-module PRs with `:module:test` after each.

---

## Bucket 3 — Large refactors / consolidations (DESIGN PRs REQUIRED)

AGENTS.md hard rule: **never launch an opportunistic broad rename**. Every
consolidation below must (1) search every caller across `modules/*/src`, (2)
keep the API the rest of the code uses, (3) delete only the dead/duplicated
implementation, (4) run the callers' module tests.

| # | Title | Design note |
|---|---|---|
| #212 | Two Configuration/Dictionary engines coexist (`com.microproject.configuration` vs `com.microproject.core.configuration`) | Find which is live; the other is dead legacy. Merge config XML paths (`meta.properties`/`DictionaryFiles`) carefully — see pod-loading skill. |
| #244 | Replace custom Functors package (10 classes) with `java.util.function` / commons-collections4 | Confirm no serialization depends on the custom functor classes; prefer `java.util.function`. |
| #245 | Replace int-constant enums with Java enums (62+ in `pm/`) | Highest-risk: enums are serialized and referenced by ordinal in many formats. Requires a compatibility shim for old `.pod` reads. Do per-enum with tests. |
| #258 | Consolidate `core.hierarchy` vs `grouping.core.hierarchy` (11 classes) | Trace which hierarchy abstraction the model actually uses; delete the other. |
| #259 | Consolidate Exchange packages (`com.microproject.exchange` vs `com.microproject.core.pm.exchange`, 26 classes) | Biggest package move; `SafeObjectInput` remap must follow renamed classes. |
| #260 | Consolidate `link_routing` packages (5 classes) | Small; safe after caller search. |
| #261 | Consolidate event packages (`graph.event` vs `model.event` vs `selection.event`, 8 classes) | Confirm no duplicate listener contracts; merge to one. |
| #262 | Consolidate functor packages (30 classes) | Overlaps #244; do together. |
| #152 | Same-name different-impl classes (`core.time` vs `datatype`, `core.fields` vs `field`, etc.) | Clean-room work; rename PRs already in flight per AGENTS.md. Coordinate, don't duplicate. |

**Process for each:** open a design issue/PR describing the caller search
results + which implementation survives + which is deleted, get sign-off, then
implement with `:micrproject_<module>:test` green after each.

---

## Bucket 4 — Feature requests (SCOPE CONFIRMATION NEEDED)

These are enhancements, not bugs. Each needs a spec decision (often
"match Microsoft Project behavior" per user directive) before coding.

| # | Title | Suggested approach |
|---|---|---|
| #215 | Preference/Setting option | Add to `Configuration`/`Settings`; needs UI entry point. |
| #204 | Timeline & Team Planner as dockable views (like MS Project) | Refactor floating dialogs → dockable panels in `GraphicManager`. Large UI work. |
| #179 | Highlight complete row of selected task(s) | Spreadsheet renderer change; MS-Project parity. |
| #84 | Code formatting (コードの整形) | EditorConfig exists; run formatter in a dedicated cleanup PR (not mixed with fixes). |
| #63 | Task font properties user-customization | Field + dialog work. |
| #62 | Calendar table for date selection | Date picker component. |
| #61 | Task Information — UI issues | Needs specific repro; open sub-issues. |
| #60 | Hide/Unhide task | Model + UI + serialization flags. |
| #47 | Copy/Cut-Paste does not work well | Already partially addressed by shortcut-wiring rule (AGENTS.md). Re-test against current code. |
| #46 | Clarification on multiple "project management" apps | Docs/README clarification, not code. |
| #36 | Resource tab needs fixes | Needs specific repro list; open sub-issues. |

**Recommended next step:** pick the highest-value item(s) (e.g. #179 row
highlight, #47 paste) and open a focused issue with acceptance criteria, rather
than batching.

---

## Recommended immediate follow-ups (this week)
1. **Merge #227 fix** (done, needs PR + push).
2. Open design PRs for #212 and #259 (the two biggest consolidation risks) —
   caller-search first, no code yet.
3. Start #253 (prune 680 unused keys) as a standalone, test-gated PR.

## Explicitly NOT done this session (and why)
- Byte-for-byte POD idempotency (#227 residual): needs full graph stabilization.
- All Bucket-2 micro-fixes: broad mechanical edits forbidden by AGENTS.md without
  per-module PRs + tests.
- All Bucket-3 consolidations: require design sign-off, never blind rename.
- All Bucket-4 features: need spec/acceptance criteria per item.
