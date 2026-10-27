# Issue #944 — Merge Preferences and Locale Settings

State: Implementation slice complete; verification pending.
Issue: https://github.com/tetsuji16/microProject/issues/944
Contract frozen at: 2026-10-26, issue body and comment.
Baseline HEAD: `ec0df6e9e6a4173cbf08301655b902f7c75c9efb`.
Baseline working tree: pre-existing edits in docs/MSP_STANDARD_COMMAND_INVENTORY.md and three UI test/source paths; untracked .codex-remote-attachments/, docs/book/, docs/work/issue-941.md, and sample sync sidecars. Preserve all.

## Goal and scope

Bring locale and language configuration into the Preferences experience so users can find overlapping settings in one coherent location, while preserving every existing preference and locale behavior.

Out of scope: changing locale semantics, removing a setting, or modifying project data.
The user explicitly directed work on issues #941, #944, and #945, overriding the earlier sequencing hold for this slice. Sol selected a two-tab Preferences dialog: General and Language and locale.

## Acceptance

| ID | Required result/evidence | State | Evidence (SHA + link) |
|---|---|---|---|
| A1 | Locale/language options are discoverable within Preferences without duplicate or conflicting controls | pending | Implementation added Language and locale tab; UI verification pending |
| A2 | Existing locale selection and preference values remain functional and persist as before | pending | Existing LocaleDialog bind and catalog functions reused; persistence verification pending |
| A3 | Existing entry point remains coherent or redirects to the consolidated location | pending | RibbonLocale routes to Language and locale tab; route verification pending |
| A4 | Layout is usable in supported locales and display scales | pending | Physical visual acceptance pending |

## Current slice

**Implemented slice:** Preferences now has General and Language and locale tabs. LocaleDialog supplies the existing locale controls as embedded content; Preferences Apply commits locale and general settings, and the established locale-change restart path runs only after Apply. RibbonLocale opens the same Preferences dialog on Language and locale; Options opens General.
**Verification:** On candidate `ee61b2ca0a3cd6cb40baa6543dc1bf795516ac1e`, `:microproject_ui:test --console=plain` and `:microproject_ui:compileGuiTestJava --console=plain` passed. The focused Robot test on the identical scoped code/test tree reached locale selector interaction, then the desktop guard stopped before physical input because MSPaint (PID 25288) overlapped the Preferences window. The environment artifact is `modules/microproject_ui/build/reports/guiTest-artifacts/gui-test-contended-1793002342258-44696.txt`. Exact-candidate GitHub CI remains pending.

## Remaining blockers

- Physical route, layout/scale, persistence, and exact-candidate CI evidence.

## Latest evidence

- Reporter notes overlap and the existing Preferences link to Language and locale; no additional requested setting was listed.
- Tests/build: candidate `ee61b2ca0a3cd6cb40baa6543dc1bf795516ac1e` — `:microproject_ui:test --console=plain` passed; `:microproject_ui:compileGuiTestJava --console=plain` passed.
- Physical GUI: `:microproject_ui:guiTest --tests "com.microproject.dialog.PreferencesDialogGuiAcceptanceTest.languageLocaleTabHelpApplyAndCancelUseThePreferencesDialog" --console=plain` stopped before dispatch due to the documented foreign foreground window; locale Apply/Cancel and restart Robot assertions remain unverified.
- CI: none.
