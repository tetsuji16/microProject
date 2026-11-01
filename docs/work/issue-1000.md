# Issue #1000 — neutral internal identifiers

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1000
Contract frozen: 2026-11-01, issue body
Baseline HEAD: `1bc21058b`; isolated worktree clean.

## Goal and scope

Neutralize the named internal identifiers, comments, and UI resource keys; add a build check with justified exceptions. Preserve behavior and update all callers. Exclude persistent identifiers, open-project commands, license headers, and display meaning.

## Acceptance

| ID | Required result | State | Evidence |
|---|---|---|---|
| A1 | All remaining in-scope names are removed or justified on an allowlist | Open | inventory and build check pending |
| A2 | Behavior unchanged and callers of renamed public APIs updated | Open | partial source search; tests pending |
| A3 | `gradlew.bat build --console=plain` and `git diff --check` pass | Open | pending |

## Current slice

**Next slice:** audit remaining historical resource keys and implement the reasoned legacy-name build allowlist.
**Done when:** each remaining match is either neutralized or explicitly justified and the check rejects new matches.
**Then:** run the full build and review all acceptance criteria.

- Error/warning and legacy file-label resource keys and their callers were renamed with values unchanged across locales. Core `ClientBundleProductNameTest` and UI `DocumentStatusBarTest` passed via `gradlew.bat :microproject_core:test --tests com.microproject.strings.ClientBundleProductNameTest :microproject_ui:test --tests com.microproject.pm.graphic.frames.DocumentStatusBarTest --console=plain`.

- The persisted-data input filter constant was renamed without changing its filter; `gradlew.bat :microproject_core:test --tests com.microproject.util.SafeObjectInputTest --console=plain` passed.
- The named comments and exchange warning were neutralized; third-party modification markers retain their meaning. `gradlew.bat build --console=plain` passed on `b49cc679a` plus this slice (56 tasks).

- `MPXConverter` source-side parameter/local identifiers were renamed without changing types or behavior. `gradlew.bat :microproject_exchange:test --console=plain` passed on the baseline plus these changes.

- `MergeField` renamed its target property and unassigned parameter; repository caller search found no external accessor calls. `gradlew.bat :microproject_core:test :microproject_exchange:test --tests com.microproject.exchange.ResourceMappingFormTest --console=plain` passed on baseline plus this change.

## Remaining blockers

- A1–A3: remaining inventory, build check and full-build evidence are required.
