# Issue #904 — 固定standAlone・appletフラグと到達不能分岐を廃止する

State: IMPLEMENTED — frozen local verification passed; final CI/closure status in [integration record](https://github.com/tetsuji16/microProject/issues/774#issuecomment-6382996503)
Issue: https://github.com/tetsuji16/microProject/issues/904
Contract frozen at: 2026-10-24, issue #904 body
Baseline HEAD: `ffc291d30a4ebe1624337b78d3fd8d5ddb30a815`
Baseline working tree: `.codex-remote-attachments/` untracked; other bounded slices now share this worktree

## Goal and scope

Remove mutable environment modes that startup always sets to standalone/local, dead server-only settings and branches, and dynamic references that fail after removing those modes. Preserve existing local workflows and persisted format boundaries.

Out of scope: Session/DTO deletion and branding/API renames (#905–#907); frozen POD wire/UID/FQN; MPO prior-version reads; local MPP/XML/XLSX, master/linked projects, shared resources, and offline collaboration.
Blockers: final integrated candidate testing belongs to parent; shared Gradle use is currently serialized with #908.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | All Java/resource/dynamic callers of Environment mode flags removed or fixed to local semantics | pass | scoped `rg` has no flag matches; Groovy caller fixed below |
| A2 | Local save/open/import/resource/collaboration semantics retained | pass, integrated tests pending | callers reviewed; module evidence below |
| A3 | Dead server Settings/field/filter registration and fixed shell arguments removed | pass | source review; targeted transform/config tests reported passing below |
| A4 | Frozen POD and MPO backward-read boundaries unchanged | pass by scoped review; integrated tests pending | no POD descriptor/wire edits; Field XML marker only removed |
| A5 | Focused tests and GUI smoke evidence recorded | partial | focused unit tests and GUI test compilation pass; final integrated GUI gate pending |

## Findings and decisions

- Startup/callers used the standalone/local branch. Removed `Environment.standAlone`, applet, and `isProjectLibre` state/accessors and reduced callers to that existing local behavior. Local Open/Save, MPP interoperability, TeamFilter and assignment filter, master/linked projects, shared resources remain.
- Removed unused server `Settings` constants and Field `server` registration guard. `Field.userRole` remains registered as local resource-role metadata (`ResourceImpl`, `SpreadSheetModel`, and `view.xml` use it); removed only obsolete `server="true"`. `Filter.MyTasks` was marked `server="true"`, listed in five view filter arrays, and filtered out by `TransformList.addFactory`; `NormalTask.isAssignedToMe()` exists but this server filter was never locally registered. Removed its definition and five stale view references; the commented `Filter.MyDelegatedTasks` definition was also removed. Removed both filter labels from 42 locale files. No other configuration `server=` attributes or CommonTransform server-property callers were found, so the dead bean fields/getters/setters and both registration guards were removed. The separate `GraphicNode.isServer()` adapter had no caller and is not serialized; it was removed.
- Groovy view configuration had a real dynamic caller missed by Java-only search: `view.xml` evaluated `Environment.standAlone`, causing `MissingPropertyException: No such property: standAlone for class: com.microproject.util.Environment` in `RibbonButtonBehaviorTest`. Replaced it with the local action list (former fixed-true result). Four targeted UI failures disappeared; the focused class passed.
- `Messages.getContextString` previously had real `Open_` overrides, confirmed during #904; #907 has now canonicalized the title/display keys and removed all `Open_` resource entries. The remaining fallback was dead, so it now resolves directly from the canonical bundle while preserving the explicit `microProject` title special cases. `getStringFromBundles` is non-recursive, and its previous missing-key/null behavior is unchanged.
- AssignmentDialog's preference-bound Team/All label and observer only fed a server-only hidden row. Local preference filtering remains through GraphicManager `TeamFilterAction` and assignment transformer closure. Removed only the obsolete GUI test for that hidden label; kept unresolved-pool Robot coverage.
- `GraphModel.isReadOnly()` now returns the fixed local value directly; removed its unused `NodeModel` lookup/import.

## Current slice

**Next slice:** run the focused core transform/configuration and string/UI tests after #905 API edits stabilize; send results to parent.
**Done when:** core/UI focused tests pass; no remaining #904 dynamic callers; packet and scoped diff reviewed.
**Then:** #905 session/reflection cleanup.

## Latest evidence

- `:microproject_core:compileJava :microproject_ui:compileJava :microproject_application:compileJava :microproject_reports:compileJava --console=plain --no-daemon --max-workers=1`: passed on an earlier shared candidate; rerun after concurrent XLSX edits is pending.
- `:microproject_core:test :microproject_ui:test :microproject_application:test :microproject_reports:test`: core passed. UI initially had 4 failures from the `view.xml` Groovy property; after fix `RibbonButtonBehaviorTest` passed (52 tests). Full UI rerun had one unrelated #907 `RibbonCommandCatalogTest` failure (missing use case for `RibbonMicroProjectDocumentation`). Application and reports tests passed separately.
- `:microproject_ui:compileGuiTestJava --console=plain --no-daemon --max-workers=1`: PASS after removing the hidden server-row label test; the concurrent #907 cast issue is also resolved.
- Focused core tests reported passing by #907: `ClientBundleProductNameTest`, `TransformListTest`, `CommonTransformFactoryTest`, and `LegacyJaxbConfigurationTest`; focused UI tests reported passing: `SwingFileChooserProviderTest`, `LicenseDialogResourceTest`, `RibbonStructureTest`, and `RibbonCommandCatalogTest`. `:microproject_ui:compileGuiTestJava` also passed on the shared candidate.
- Scoped production dynamic-reference scan found no `Environment` mode flags/accessors, removed server settings, `Field.server`, server transform bean property/guard, `Filter.MyTasks` / `Filter.MyDelegatedTasks`, `Open_` keys, or retired importer/shell/XLSX/date-field API names. Remaining `GraphicNode.isServer` was separately caller-audited and removed. Independent `CommonDataObject.isLocal` / `DataObject` persistence semantics remain untouched.
- Full `git diff --check` reports only trailing spaces in concurrent #907 Russian native-code-page lines 712–713; `GraphicManager.java` indent warning was fixed. Do not alter #907-owned locale data.
- Gradle reports/artifacts are local. Exact integrated-candidate CI and physical GUI smoke remain parent gate.

## Final integration gate

The frozen candidate passed architecture/naming gates, maintenance 20/20,
source inventory, clean build/installDist (2,880 tests; no failures/errors/skips),
and GUI source compilation. This supersedes earlier slice-time pending local
checks above. Candidate SHA, Windows Robot outcomes and closure are maintained
in the linked integration record; physical acceptance is required before close.
