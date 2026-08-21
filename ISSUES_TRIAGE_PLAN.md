# ProjectLibre (microProject) — Issue Triage & Action Plan

Generated: 2026-08-19 (re-audited 2026-08-21). Scope: all 31 currently open issues on
`tetsuji16/ProjectLibre` (the earlier 38-count included issues subsequently
closed or superseded).

## Implementation audit (2026-08-21)

The following non-clean-room items now have code and regression coverage in the
working tree (the GitHub connector does not grant issue-close permission, so the
remote issue state may still be open):

- **CCPM (#310, #319, #320, #327, #328):** deterministic critical-chain
  analysis, resource constraints, immutable buffer measurements, bounded graph
  layout, project/feeding buffer nodes, and a docked network view. Performance
  hot paths use task indexes, cached Gantt state, and an end-time sweep heap for
  resource interval expiry; a 1,000-task shared-resource regression benchmark
  guards the preview path. `DockableProjectToolViewTest` also paints the
  resulting graph with project and feeding buffer nodes, exercising the
  analysis-to-view path headlessly.
- **podx collaboration/spec (#317, #321, #325, #326):** versioned container
  layout, manifest checksum plus document-identity validation before external
  merge, unknown-extension preservation,
  append-only operation logs with idempotent merge/conflict detection, explicit
  `appliedOperationIds` snapshot generations, pending operation caps, atomic
  replacement, and OneDrive/shared-folder round-trip tests.
- **CCPM podx round-trip:** `PodxFileImporterTest` now applies CCPM, saves a
  `.podx`, reloads it, restores settings/baseline, and successfully re-analyzes
  the chain. Legacy `.pod` serialization remains free of transient CCPM state.
- **#267:** summary/grouped task scheduled dates now roll up after child date
  edits; `PodRoundTripTest.groupedTaskFollowsChildDateUpdate` covers the earlier
  finish regression.
- **#60, #63:** task hide/unhide and task font-family/size/color/style controls
  are present in the task model, persisted field set, and Task Information UI;
  the existing field-driven view pipeline applies them without changing `.pod`
  compatibility.
- **#36:** Resource tab is wired to the live resource model/cache, supports
  resource editing and assignment protection checks, and has dedicated
  resource fields; `ResourceView` is part of the document view switcher rather
  than a dead placeholder.
- **#266, #179, #204, #215, #322, #324, #47, #61, #62:** incident dependency unlinking,
  selected-row highlighting, dockable Timeline/Team Planner panels, preference
  persistence, dependency-label layering, clipboard shortcut de-duplication,
  CCPM dialog lifecycle, Task Information labels, and a month-grid date chooser
  are covered by implementation/tests.
- **#46:** README now explains the relationship between the similarly named
  repositories and clarifies that they are separate projects/artifacts.
- **#235, #239:** redundant empty-string allocations were removed and
  configuration initialization now clears its ThreadLocal guard on failure.
  Core/UI regression suites pass after these changes.
- **#252 (focused):** user-visible hexadecimal color formatting and podx
  checksums now use `Locale.ROOT`, preventing Turkish/locale-specific output
  drift. UI and exchange tests pass.
- **#251 (focused):** the Office-style chrome search/help/application/auto-save
  labels now come from the localized usability bundles (`en`/`ja`) instead of
  hardcoded English literals. Export job labels, collaboration conflict
  actions, CCPM graph labels/tooltips/buffer units, and resource-leveling undo
  labels now use localized resources as well.
  Remaining legacy diagnostics and compatibility labels still require a
  caller-by-caller review.
- **#229 (focused):** the XLSX reader now closes file input streams with
  try-with-resources, and the image exporter closes its conditional PDF stream
  deterministically. Other legacy stream sites remain individually audited.
- **#230 (audit):** all remaining production `String.split` call sites found by
  the current source scan already use an explicit `-1` limit; no unbounded
  split remains in `modules/*/src/main`.
- **#233 (audit):** `ResourceLevelingService` already uses reusable
  per-preview scratch buffers with capacity growth and reset semantics; the
  1,000-task leveling/CCPM regression covers this path.
- **#236 (audit):** serializer hierarchy logging is guarded by
  `logger.isLoggable(Level.INFO)` before constructing its `StringBuilder`; no
  unguarded serializer debug-string allocation remains.
- **Import lifecycle logging:** pre-initialization critical-path events are
  now ignored at `FINE` level (the post-import initialization rebuilds the
  path), eliminating misleading warning-level noise without changing event
  behavior.
- **#330:** Task Information routing now treats an empty selection as a
  no-op.  This prevents the Information button/double-click path from
  indexing an empty node list before the dialog is opened.
- **#215 (focused):** the Preferences dialog and `GlobalPreferences` now
  persist user name, row-grid visibility, and spreadsheet font family/size
  through the user preference store and refresh open document views.
- **#245 (incremental):** `ExpenseType`, `Accrual`, and
  `EarnedValueMethodType` now expose validated nested `Kind` enums with
  persisted-code conversion; legacy integer constants remain deprecated for
  POD/MPX compatibility.  Round-trip and invalid-code tests were added.
  The same compatibility bridge now covers `SchedulingType`,
  `RequestDemandType`, `BookingType`, `ProjectType`, and `ProjectStatus`.
- **#257 (incremental):** `TimeTypeBridge` now provides an explicit,
  tested conversion boundary from MPXJ-compatible `core.time` Duration/Rate/
  TimeUnit values to packed domain `datatype` values, including the legacy
  Years/Percent code mismatch.  Existing converter APIs remain unchanged.
- **#237 (audit):** `ClassUtils` already caches resolved static fields and
  methods in concurrent maps; the remaining reflective calls are one-time
  compatibility/plugin dispatch and are not safe to globally cache without
  changing class-loader lifecycle semantics.
- **#239 (audit):** MPX export wraps its `ThreadLocal` ID allocator in a
  `try/finally` and always calls `remove()` on success or failure; no exporter
  lifecycle leak remains.
- **#231/#240 (audit):** Date objects in MPXJ conversion are created at API
  boundaries where MPXJ stores mutable `java.util.Date` values. Reusing one
  instance would alias fields and corrupt exported timestamps; allocation
  removal is therefore not semantics-preserving. No safe cache was introduced.
- **#228 (focused):** CCPM graph/layout maps now size themselves from the
  known task cardinality; generic legacy
  collection sites remain outside this focused change because their cardinality
  is not known at construction.  Critical-chain task/resource maps and active
  assignment sets now also use bounded capacities derived from the project,
  chain, and assignment cardinalities.
- **#234 (focused):** `DocumentFrame` no longer keeps tracking-Gantt columns in
  a process-global static list; the selection is now scoped to each open
  document, preventing one document from changing another document's view.
  `ResourceImpl` read-only user fields are also exposed through a lazy immutable
  set instead of a mutable static cache.
- **#250/#222 (audit):** a complete production scan found no remaining
  user-facing sentence assembled by concatenating translated fragments. The
  remaining `Messages` concatenations are logger text, HTML line-break markup,
  regex construction, or commented legacy code; they must not be converted to
  `MessageFormat` because that would change non-localized syntax.
- **#253 (contained):** `scripts/translation_key_audit.py` separates
  literal Java/XML references, runtime-generated key families,
  resource/text references, and deletion candidates. The current default
  bundle reports zero remaining candidates after excluding dynamic families.
  A reviewed allowlist removed the confirmed-dead keys from all applicable
  locale bundles (including native-code-page fallbacks) while preserving each
  file's original encoding and line endings; the current audit reports zero
  candidates. The review
  procedure is documented in
  [`docs/translation-key-audit.md`](docs/translation-key-audit.md).
- **#325 (hardening):** podx task-identity XML parsing disables DTD, external
  entities, XInclude, and external schema access before reading `project.xml`.
- **#321 (shared-folder hardening):** podx export now serializes the complete
  read/merge/write transaction with a stable `<name>.podx.lock` sidecar using
  an OS file lock, while retaining atomic replacement. This prevents two
  OneDrive-synced desktop writers from losing each other's operation log.
- **#245 (incremental):** `ConstraintType.Kind` now provides a type-safe,
  persisted-code-compatible API with strict `fromCode` validation; legacy int
  constants remain deprecated until each serialized enum can be migrated safely.
- **#245 (incremental):** `AccessControlPolicy.Kind` and
  `TimesheetStatus.Kind` now provide the same validated persisted-code boundary;
  their legacy integer constants remain available only as deprecated aliases.
- **#257 (boundary):** legacy `core.time` Duration/Rate/TimeUnit types are now
  explicitly deprecated; MPXJ converter callers remain isolated until a
  compatibility adapter can be tested against all exchange formats.
- **#244/#262 (incremental):** the unused generic `com.microproject.functor`
  wrappers were removed; assignment-specific functors remain because they
  encode domain behavior and are still referenced by scheduling/contour code.
- **#212 (containment):** caller audit confirmed the JAXB engine is isolated to
  the legacy `core.fields`/`core.nodes` family. Its `Configuration` and
  `Dictionary` are now explicitly deprecated and documented as a compatibility
  boundary; new code is directed to the active Digester engine. Deleting the
  legacy engine remains unsafe until the clean-room namespace migration is
  complete.

The remaining entries below are intentionally **not claimed as complete**:
large namespace/hierarchy/event consolidations and broad mechanical perf/i18n
edits require separate design/caller audits. The caller audit and explicit
non-merge boundaries are recorded in
[`docs/issue-architecture-decisions.md`](docs/issue-architecture-decisions.md);
#152 remains the explicitly excluded clean-room work. This distinction
prevents the issue tracker from being marked green while those
compatibility-sensitive refactors are pending.

## Summary

| Bucket | Issues | Decision |
|---|---|---|
| Bug (must fix) | #227,#330 | **Fixed in working tree** (POD stability + safe Task Information routing) |
| Safe perf/i18n micro-fixes | #228,#229,#231,#232,#233,#234,#236,#237,#240,#250,#222,#251,#252 | **Focused fixes/audits applied:** remaining work is intentionally split by caller/module rather than a blind batch. #253 is contained by the audited allowlist workflow below. |
| Large refactors / consolidations | #212,#244,#245,#257,#258,#259,#260,#261,#262,#152 | **Contained incrementally or by documented compatibility boundaries; #152 remains clean-room-excluded.** |
| Feature requests | #215,#204,#179,#84,#63,#62,#61,#60,#47,#46,#36 | **Implemented in working tree or documented as a compatibility boundary; remaining risk is manual UI parity.** |

Total open at the latest audit: 31 (the remote connector does not permit issue
state mutation from this workspace).

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

The POD serializer now also fixes the timestamp of its compressed `Serialized`
entry, removing ZIP metadata churn from otherwise unchanged saves. The separate
MPXJ XML trailer still carries a generated timestamp, so complete archive-byte
identity is deliberately not asserted.

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
| #232 | Enum.values() called 21× in loops | cache the array | **Investigated 2026-08-19: not a real hotspot.** All `Enum.values()` sites are enhanced-for loops (`for (X x : values())`) or `Map.values()` (collection). Enhanced-for calls `values()` **once** per loop, not per iteration — no per-iteration allocation to cache. A cached `VALUES` field on every enum would be broad + low-value. **Skipped.** |
| #233 | ResourceLevelingService 5 ArrayLists per call | hoist/scratch | profile first |
| #234 | 24 large static collections should be immutable | `List.of`/`Map.of` | init-order pitfalls |
| #235 | ScheduleEvent `new String()` for sentinels | drop redundant ctor | low |
| #236 | Serializer StringBuilder logging allocs | guard/level-check | low |
| #237 | 9 reflection calls in hot paths | cache `Method`/`Field` | low |
| #239 | ThreadLocal without cleanup | `remove()` in finally | session-scoped, verify lifecycle |
| #250,#222 | `Messages.getString()` + string concat breaks word order | use `MessageFormat` | **i18n-correctness win**, but 45 sites; must keep key names |
| #251 | Hardcoded English strings in UI (30+) | move to `Messages` | 30+ sites, UI strings |
| #252 | Locale-sensitive ops without explicit Locale | **Focused fix applied:** stable color/hex/checksum formatting uses `Locale.ROOT`; remaining sites require separate UI review. |
| #253 | 680 unused keys in properties | prune | **Contained 2026-08-21:** conservative audit now reports zero candidates; reviewed allowlist was removed from all applicable locale bundles with encoding/newline preservation. |

### #253 investigation finding (2026-08-19)
Mechanical analysis of `client.properties` (1190 keys) against all `src/main` Java + resource XML/properties:
- **Literal `getString("key")` refs:** 531 unique keys referenced from Java.
- **Resource XML/properties refs:** 382 keys referenced from config files (`view.xml`, `meta.properties`, etc.) — e.g. `Bar.baseline1`–`Bar.baseline10` are referenced by `view.xml`, NOT dead.
- **Dynamic-key prefixes** (`"Category." + x`, `"Date.Quarter" + n`, `"Field." + id`, …): 131 more keys are built at runtime.
- **Remaining high-confidence unused candidates: 183 keys** (no Java literal, no resource ref, no dynamic prefix).

**Conclusion:** the issue's "680 unused" number comes from a Java-only static scan and over-counts — many keys are live via XML config or dynamic construction. Auto-pruning is **unsafe**: a wrong deletion breaks UI/dialogs silently.

**Execution:** the conservative audit/pruner now scans every locale bundle (including `nativeCodePage`), requires an explicit allowlist, preserves each file's original encoding and newline convention, and reports zero remaining candidates. Full `test` passes after the reviewed removals. The allowlist and repeatable audit are committed under `scripts/` and the process is documented in `docs/translation-key-audit.md`.

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
| #212 | Two Configuration/Dictionary engines coexist (`com.microproject.configuration` vs `com.microproject.core.configuration`) | **Contained 2026-08-21:** the JAXB engine remains for the legacy core model, but all active callers now use the explicit `LegacyConfiguration` boundary. Full deletion still requires a format/schema decision. |

### #212 investigation finding (2026-08-19)
- `com.microproject.configuration.Configuration` — dominant (134 refs, 41 imports). Digester-based config-file reader; owns `FieldDictionary`, `TimeScaleManager`, `GraphicConfiguration`, `ScriptConfiguration`. The app's primary configuration engine.
- `com.microproject.core.configuration.Configuration` — only 3 external refs (`core/fields/Field.java`, `core/fields/FieldUtil.java`, `core/nodes/AbstractNode.java` — the last imports but does NOT call it). JAXB-based; owns `com.microproject.core.dictionary.Dictionary` and is reached via `Configuration.getInstance().getDictionary()` from `Field`/`FieldUtil`.
- **Conclusion:** the two classes are not redundant — they back *different* Dictionary types (`com.microproject.configuration.FieldDictionary` vs `com.microproject.core.dictionary.Dictionary`). A blind "delete the unused one" would break `Field`/`FieldUtil` resolution.
- **Real consolidation path (design PR):** decide which `Dictionary` is the canonical source of field metadata, then route `Field`/`FieldUtil` through the surviving engine and delete the other `Configuration` class + its adapters (`core/configuration/adapters/*`). Must verify no serialization/format depends on `core.dictionary.Dictionary`. The current `LegacyConfiguration` facade is an intentionally backward-compatible containment step, not a claim that the engines are fully merged.
| #244 | Replace custom Functors package (10 classes) with `java.util.function` / commons-collections4 | **Contained incrementally:** unused generic wrappers were removed; assignment-specific functors remain because they encode domain behavior and are still referenced. |
| #245 | Replace int-constant enums with Java enums (62+ in `pm/`) | **Contained incrementally:** all domain choice sets now expose persisted-code `Kind` enums with deprecated integer aliases; event/bitmask/index constants remain intentionally integer APIs. |
| #257 | Consolidate `core.time` vs `datatype` time types | **Contained by adapter:** `TimeTypeBridge` isolates MPXJ's legacy time types from the scheduling-domain types; legacy classes are deprecated and round-trip tested. |
| #258 | Consolidate `core.hierarchy` vs `grouping.core.hierarchy` (11 classes) | **Contained by boundary:** caller audit shows legacy document hierarchy and active grouping/view hierarchy are different contracts; no unsafe deletion. |
| #259 | Consolidate Exchange packages (`com.microproject.exchange` vs `com.microproject.core.pm.exchange`, 26 classes) | **Investigated 2026-08-19:** NOT redundant — different layers. `com.microproject.exchange` (12 classes: `LocalFileImporter`, `MicrosoftImporter`, `MpxjApi`, `ProjectLibreXlsxReader/Writer`, …) is the app-side import/export entry point. `com.microproject.core.pm.exchange` (31 classes: `Mpx*Converter`, `MspImporter`, `DateLongConverter`, …) is the MPXJ conversion logic. Do NOT merge; keep the entry-point / converter split. If consolidation is still wanted, only rename for namespace clarity — never collapse the two layers. `SafeObjectInput` remap must follow any rename. |
| #260 | Consolidate `link_routing` packages (5 classes) | **Partially contained:** shared orthogonal geometry now lives in `LinkRouting`; Gantt and Network strategies remain separate because their route contracts differ. |
| #261 | Consolidate event packages (`graph.event` vs `model.event` vs `selection.event`, 8 classes) | **Contained by boundary:** payloads/listener contracts differ; shared `GraphicEvent` is the canonical common base. |
| #262 | Consolidate functor packages (30 classes) | **Contained with #244:** generic wrappers are gone; domain-specific assignment functors remain behind their active callers. |
| #152 | Same-name different-impl classes (`core.time` vs `datatype`, `core.fields` vs `field`, etc.) | Clean-room work; rename PRs already in flight per AGENTS.md. Coordinate, don't duplicate. |

**Process for each:** open a design issue/PR describing the caller search
results + which implementation survives + which is deleted, get sign-off, then
implement with `:micrproject_<module>:test` green after each.

---

## Bucket 4 — Feature requests (IMPLEMENTATION AUDIT)

These are enhancements rather than defects. The entries below record the
implemented behavior and the remaining manual-parity risk instead of treating
the original request as an unbounded refactor.

| # | Title | Suggested approach |
|---|---|---|
| #215 | Preference/Setting option | Add to `Configuration`/`Settings`; needs UI entry point. |
| #204 | Timeline & Team Planner as dockable views (like MS Project) | Refactor floating dialogs → dockable panels in `GraphicManager`. Large UI work. |
| #179 | Highlight complete row of selected task(s) | Spreadsheet renderer change; MS-Project parity. |
| #84 | Code formatting (コードの整形) | **Focused cleanup:** obsolete two-model import comment blocks were removed from `MicrosoftImporter`; remaining comments document compatibility boundaries or active behavior. |
| #63 | Task font properties user-customization | Field + dialog work. |
| #62 | Calendar table for date selection | **Implemented:** locale-aware month grid with navigation and date selection in `ProjectLibreDateField`. |
| #61 | Task Information — UI issues | **Focused fixes applied:** missing General-tab labels restored and date chooser now exposes a calendar table; remaining visual parity is covered by UI regression tests. |
| #60 | Hide/Unhide task | Model + UI + serialization flags. |
| #47 | Copy/Cut-Paste does not work well | **Fixed:** `SpreadSheet` removes the component-level Ctrl+C/X/V bindings while retaining `NodeListTransferHandler.importData`; the root-pane shortcut layer is the sole keyboard registration. `MicrosoftShortcutsRootPaneTest` and `NodeListTransferablePasteFailureTest` pass. |
| #46 | Clarification on multiple "project management" apps | Docs/README clarification, not code. |
| #36 | Resource tab needs fixes | **Implemented:** live resource spreadsheet, editing/deletion protection, assignment checks, and view-switcher integration are covered by the resource view pipeline. |

**Verification rule:** keep the focused regression tests and the manual UI
parity checklist synchronized when these views change; do not reintroduce the
old floating-dialog or component-level shortcut paths.

---

## Recommended immediate follow-ups (this week)
1. Keep the full test suite and the real-sample PODX/CCPM scenario in CI.
2. Keep the architecture decision records updated if a future compatibility
   migration changes a boundary (#212, #257, #258, #259, #261).
3. Keep #253's audit/pruner in regression checks when locale bundles change;
   do not re-enable unreviewed automatic pruning.

## Explicitly NOT done this session (and why)
- PODX operation identity remapping for legacy POD projects: **fixed** with the `changes/task-identities.json` format-level map. Negative/generated POD IDs are translated to the MSPDI UIDs used by `project.xml` before local replay and external shared-folder merge.
- Byte-for-byte POD idempotency (#227 residual): needs full graph stabilization.
- All Bucket-2 micro-fixes: broad mechanical edits forbidden by AGENTS.md without
  per-module PRs + tests.
- All Bucket-3 consolidations: require design sign-off, never blind rename.
- All Bucket-4 features: need spec/acceptance criteria per item.

- Import repair diagnostics: missing assignments remain auto-repaired, but reporting is now one aggregated WARNING per project instead of repeated simulated SEVERE errors; scheduling/data behavior is unchanged.
