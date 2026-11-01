# Issue #1000 — neutral internal identifiers

State: IN PROGRESS (integration pending)
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
| A4 | Exact candidate CI and integration evidence | Open | Windows build CI passed on `64db3d3484a23e79071a1c49cc734af105ce20bf`; integration pending |

## Current slice

**Next slice:** integrate the reviewed candidate and confirm the integrated result.
**Done when:** the integrated result and required checks are available.
**Then:** close issue #1000 with acceptance evidence and select the next GUI-independent issue.

## Evidence

- `gradlew.bat build --console=plain` passed on `f95bb2cb1` plus the initial naming check: 56 tasks, 15 executed, 41 up to date. The subsequent check-only change added the native-code-page file pattern and passed `verifyNamingConventions`; no production or test code changed.
- `verifyNamingConventions` passed; temporary unallowlisted Java and `.properties.1251` probes each caused the check to fail with the precise path and identifier and were removed. Native-code-page resources are included in the guard.
- 42 changed localization resource files matched the baseline after reversing only the renamed keys and normalizing line endings; display values were unchanged.
- Earlier focused core/exchange resource-mapping tests, localization tests and `SafeObjectInputTest` passed; the full build included relevant module tests.
- Existing old-format compatibility, runtime migration identifiers (#999), and dormant localization/menu keys (#954/#953) are recorded with exact lines and reasons. License and attribution headers, and open-project commands, remain out of scope.
- Exact candidate Windows build CI passed on `64db3d3484a23e79071a1c49cc734af105ce20bf`: https://github.com/tetsuji16/microProject/actions/runs/41308178667. The subsequent packet update is documentation only.
