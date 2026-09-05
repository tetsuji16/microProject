## Command contract (required for GUI-observable changes)

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
