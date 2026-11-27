## Summary

- Issue: #1435
- What changed and why: Count selected Task nodes only so blank rows do not inflate the status bar count.

Closes #1435

## Verification

- [ ] `.\gradlew.bat :microproject_ui:test --tests "com.microproject.ui.frame.DocumentStatusBarTest" --console=plain` → pending
- [ ] `.\gradlew.bat :microproject_ui:test --console=plain` → pending
- [ ] `python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json` → pending
- [ ] `git diff --check` is empty. → pending

## Completion criteria

- [ ] 手順 3 のテストが修正前に失敗し、修正後に通る → pending
- [ ] タスク 1 件 + 空白行 50 件のリストで selectedTaskCount が 1 → pending
- [ ] 空白行だけのリストで selectedTaskCount が 0 → pending
- [ ] タスク 0 件のプロジェクトで全選択しても、ステータスバーが「選択中のタスク数: 0」（GUI で確認） → pending
- [ ] サンプル（11 タスク）で全選択すると、ステータスバーが 11 件 → pending
- [ ] python scripts/test_source_inventory.py --check --output build/audit/test-source-inventory.json が通る → pending
- [ ] git diff --check が空 → pending
- [ ] PR の `build` と `GUI acceptance` が通る

## Evidence

- [ ] Focused unit/contract test: pending
- [ ] Real Robot route test: pending
- [ ] Undo/Redo verified, or not applicable with reason: selection-count display only
- [ ] Save/reload verified, or not applicable with reason: selection-count display only
- [ ] Japanese/English and 100/125/150% visual check, or not applicable with reason: status bar count behavior is locale-independent but GUI acceptance is still pending
- [ ] `git diff --check` and diff/status review completed.

## Risk / remaining work

- GUI acceptance and required PR checks must pass before merge.
