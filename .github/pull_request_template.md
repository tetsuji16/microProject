## Summary

<!-- One pull request addresses one issue. Use "Closes #N", or "Refs #N" when part of the issue remains. -->

- Issue:
- What changed and why:

<!--
Before opening this pull request (draft or not), every box below must be checked.
The only box that may stay open is the issue's "`build` と `GUI acceptance` が通る" line.
Run this locally before `gh pr create`; the "PR checklist" check runs the same script:

    python scripts/check_pr_checklist.py --body-file pr-body.md

Do not check a box for something you did not run yourself. If you cannot do it,
delete the box and explain it under "Risk / remaining work" (and use "Refs #N").
-->

## Verification

<!-- Exact commands you ran and their results, chosen from the issue's 検証 section and TEST_PLAN.md section 1.3. -->

- [ ] `<command>` → `<result>`
- [ ] `git diff --check` is empty.

## Completion criteria

<!-- Copy every line of the issue's 完了条件 verbatim, check it, and add "→ <command and result, or test name>". -->

- [ ] <criterion copied from the issue> → <evidence>
- [ ] PR の `build` と `GUI acceptance` が通る

## Command contract (required for GUI-observable changes)

<!-- Skip this section, "Implementation" and "Evidence" when no user-visible route changes. -->

<!-- Complete this before requesting review. See docs/gui-quality-gate.md. -->

- Command ID and user routes:
- Selection / active-view / lock preconditions:
- Model state before → after:
- Visible state before → after:
- Undo/Redo and save/reload requirement:
- Invalid-state feedback:

## Implementation

- [ ] I searched menu, ribbon, popup, shortcut, `GraphicManager`, `DocumentFrame`, and the underlying service for equivalent routes.
- [ ] Equivalent routes use one canonical selection resolver and command/mutation/Undo path.
- [ ] This patch removes or avoids duplicated responsibility; it is not a one-entry-point workaround.

## Evidence

- [ ] Focused unit/contract test: `<command and result>`
- [ ] Real Robot route test: `<command and result>`
- [ ] Undo/Redo verified, or not applicable with reason:
- [ ] Save/reload verified, or not applicable with reason:
- [ ] Japanese/English and 100/125/150% visual check, or not applicable with reason:
- [ ] `git diff --check` and diff/status review completed.

## Risk / remaining work

<!-- Name skipped verification, a linked issue, and why it is safe to defer. -->
