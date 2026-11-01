# Issue #1000 — neutral internal identifiers

State: IN PROGRESS (CI and integration pending)
Issue: https://github.com/tetsuji16/microProject/issues/1000
Contract frozen: 2026-11-01, issue body
Baseline HEAD: `1bc21058b`; isolated worktree clean.

## Goal and scope

Neutralize the named internal identifiers, comments, and UI resource keys; add a build check with justified exceptions. Preserve behavior and update all callers. Exclude persistent identifiers, open-project commands, license headers, and display meaning.

## Acceptance

| ID | Required result | State | Evidence |
|---|---|---|---|
| A1 | Remaining in-scope names removed or justified on an allowlist | Local pass | `docs/architecture/legacy-name-allowlist.tsv`; `verifyNamingConventions` and negative fixture |
| A2 | Behavior unchanged and renamed public API callers updated | Local pass | caller search, core/exchange tests, locale value comparison, full build |
| A3 | `gradlew.bat build --console=plain` and `git diff --check` | Local pass | full build on `f95bb2cb1` plus naming-check changes; staged diff check at commit |
| A4 | Exact candidate CI and integration evidence | Open | pending pushed candidate CI |

## Current slice

**Next slice:** push the reviewed candidate and obtain CI on its exact SHA.
**Done when:** required candidate checks pass and the integrated result is available.
**Then:** close issue #1000 with acceptance evidence and select the next GUI-independent issue.

## Evidence

- `gradlew.bat build --console=plain` passed on baseline `f95bb2cb1` plus naming-check changes: 56 tasks, 15 executed, 41 up to date.
- `verifyNamingConventions` passed; adding a new `projectlibreProbe` in an unallowlisted source caused the check to fail with that path and identifier, then the temporary fixture was removed.
- 42 changed localization resource files matched the baseline after reversing only the renamed keys and normalizing line endings; display values were unchanged.
- Earlier focused core/exchange resource-mapping tests, localization tests and `SafeObjectInputTest` passed; the full build included relevant module tests.
- Existing old-format compatibility, runtime migration identifiers (#999), and dormant localization/menu keys (#954/#953) are recorded with exact lines and reasons. License and attribution headers, and open-project commands, remain out of scope.
