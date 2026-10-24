# Issue #865 — Welcome dialog still uses ProjectLibre branding

State: IN PROGRESS
Issue: https://github.com/tetsuji16/ProjectLibre/issues/865
Contract frozen at: 2026-10-24, issue body as filed 2026-10-19
Baseline HEAD: `8c5aa6596`
Baseline working tree: clean apart from untracked `.codex-remote-attachments/`

## Goal and scope

The startup welcome dialog names the current `microProject` product in every
shipped locale, consistently with the main window title.

Root cause: `WelcomeDialog` takes its title from `Text.welcomeToPod`, whose
translated values embed the upstream brand in every `client*.properties`
bundle (Urdu translates the brand itself as `لبرے منصوبے`). The main window is
correct only because `Messages.getContextString` pins `Text.ApplicationTitle`.
The application message-box titles (`Title.ProjectLibreError` /
`Title.ProjectLibreWarning`, shown by `SwingAlertPresenter`) use the same
`getContextString` title path and had drifted (Japanese Error fixed, Warning not),
so they are corrected in the same slice.

Out of scope: other stale brand text (tips, Help, Java-version and legacy-import
messages, license text); the legacy `nativeCodePage/` source copies, which are
not loaded at runtime; internal key names, which are identifiers, not UI text.
Blockers: none.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Welcome title uses `microProject` in every shipped locale bundle | pass | `ClientBundleProductNameTest` (fails on baseline, passes after fix) |
| A2 | Japanese visible welcome dialog title is `microProject へようこそ` | pass | `WelcomeDialogGuiAcceptanceTest`, `-PguiTestLocale=ja` |
| A3 | Focused localization/UI regression check added | pass | A1 test + A2 assertion |
| A4 | Related user-visible title strings audited | pass | Error/Warning titles fixed; remainder listed as out of scope |

## Current slice

**Next slice:** integrate the PR and close the issue.
**Done when:** PR CI passes on the candidate SHA.
**Then:** close #865.

## Remaining blockers

- none

## Latest evidence

- Tests/build: `:microproject_core:test --tests "com.microproject.strings.*"` pass;
  `:microproject_core:test :microproject_ui:test` pass (core 1165, UI 1269, 0 failures).
- Physical GUI/manual: `WelcomeDialogGuiAcceptanceTest` passed under `ja`, UI
  scale 1, display resolution unchanged. The `en` run was rejected before
  dispatch as `GUI_ENVIRONMENT_CONTENDED` (a foreign foreground window overlapped
  the test window); it is not a product failure and remains for CI.
- CI: pending on the PR head.
