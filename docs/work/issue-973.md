# Issue #973 — Show shortcut keys where the user meets the command

State: IN PROGRESS (implementation in master; Japanese screen-tip evidence in PR #1271)
Issue: https://github.com/tetsuji16/microProject/issues/973
Contract frozen: issue body + acceptance comment of 2026-11-10
Baseline HEAD: `fb4bffbc9` (origin/master)
Baseline working tree: `samples/CCPM path comparison 日本語.mpo` modified by a desktop run (not committed)

## Goal and scope

A user who does not already know a shortcut should be able to discover it: every
ribbon command with a key advertises that key in its screen tip, the advertised
key is the one the document root pane dispatches, and the tip keeps the same
shape in English and Japanese.

Out of scope: user-defined shortcut assignment (#770), the key-hint (Alt) layer
of #1151, message-key or translation review (#954).
Blockers: none.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Every ribbon command with a shortcut shows the key in its screen tip; a test walks the built ribbon and detects a missing or stale hint | Pass | `MicrosoftShortcutsRootPaneTest.ribbonScreenTipsShowTheRegisteredShortcutOfEveryCommandThatHasOne` (PR #1096, master `4b876a66e`) |
| A2 | The advertised key is derived from the root-pane binding, so changing the binding changes the tip | Pass | Same test resolves each hint through `MenuRibbonCommandSource.getShortcut` / `ShortcutHints.primary` against the InputMap `GraphicManager.applyMicrosoftShortcuts` writes; `ShortcutHintsTest` pins the formatting |
| A3 | English and Japanese tips keep the one-line `name (key)` shape | Pass | `MicrosoftShortcutsRootPaneTest.japaneseRibbonScreenTipsKeepTheShortcutAfterTheTranslatedName` (PR #1271): Japanese bundle names plus the same keys, no unregistered key advertised |
| A4 | `GUI acceptance` passes | Pending | CI on PR #1271 |

## Current slice

**Next slice:** run the required checks on PR #1271 and, once `GUI acceptance` and
`build` pass on the merged candidate, close #973.
**Done when:** both required checks are green on the PR head and the issue states
the closing evidence.
**Then:** #973 closes; no follow-up slice is needed for this issue.

## Remaining blockers

- A4 needs CI on the PR #1271 head.

## Latest evidence

- Tests/build: `.\gradlew.bat :microproject_ui:test --tests "com.microproject.pm.graphic.frames.MicrosoftShortcutsRootPaneTest" --console=plain` → BUILD SUCCESSFUL (14 tests, 0 failures); `.\gradlew.bat :microproject_ui:test --console=plain` → see PR.
- Physical GUI/manual: not run locally; the changed file is a headless test, and the tooltip route itself is unchanged. `GUI acceptance` covers the rendered ribbon.
- CI: pending on the PR #1271 head (`4fc6ebdc8`).

## Notes

The Japanese fixture builds the standard ribbon from the Japanese menu bundles
without a document action map, so the clipboard group is absent from its surface;
A1's root-locale test covers those commands with the real action map. The key text
itself is locale-independent (`ShortcutHints.keyName`), so only the name and the
description differ between locales.
