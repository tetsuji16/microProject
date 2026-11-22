# Issue #1297 — [MSP] Details の復元先を上部ビューの種別に合わせる

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1297
Contract frozen at: 2026-11-22, issue revision as fetched
Baseline HEAD: `11a3034e9`
Baseline working tree: clean

## Goal and scope

Fix the Details (View > Split View > Details) command to restore the bottom view matching the active top view's type (task vs resource). Currently it always defaults to Task Usage.

Out of scope: Task Form / Resource Form creation, bottom pane layout changes, #1166 ribbon placement, #1259 docking, Task Inspector changes.

Blockers: none

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| AC1 | リソース系の上部ビューで Details を押すと Resource Usage が下部に出る。上部ビュー・下部ビュー・Details の選択状態が一致する。 | pass | Unit test `detailsWithResourceTopViewRestoresResourceUsage` passes |
| AC2 | Task Usage を開いたままリソース系の上部ビューへ移り、Details で閉じて再度開くと Resource Usage になる。 | pass | Unit test `detailsRestoresBottomViewMatchingCurrentTopViewType` passes |
| AC3 | タスク系の上部ビューでは従来どおり（直前の下部ビュー、無ければ Task Usage）。 | pass | Existing test `bottomViewTransitionsKeepComponentAndRibbonStateInSync` passes |
| AC4 | Details の開閉でモデル・dirty・Undo が変わらない（既存テストの性質を維持）。 | pass | All existing RibbonButtonBehaviorTest tests pass (62 tests) |
| AC5 | 上記 2 つのテストが通り、`git diff --check` が空。 | pass | `git diff --check` passes, all tests pass |
| AC6 | GUI acceptance test for Resource Sheet Details route | pending | Requires desktop session; to be verified in CI |

## Current slice

**Next slice:** Add GUI acceptance test to `TaskInformationRibbonGuiAcceptanceTest` for Resource Sheet Details route.

**Done when:** GUI test added and passes in CI.

**Then:** Create PR and verify CI passes.

## Remaining blockers

- GUI acceptance test requires desktop session (CI will run it)

## Latest evidence

- Tests/build: `./gradlew.bat test` - BUILD SUCCESSFUL (all modules)
- Unit tests: `RibbonButtonBehaviorTest` - 62 tests pass including 2 new tests
- `git diff --check`: passes
- Physical GUI/manual: not run (no desktop session in cron)
- CI: required checks `build` and `GUI acceptance` pending