import sys
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "scripts"))

import check_pr_checklist as checklist

ISSUE = """## 概要

例。

## 完了条件

- [ ] `rg -n "SimpleDateFormat" TimeUtil.java` が 0 件
- [ ] 手順 1 のテストが前と後の両方で通る（PR に両方の結果を書く）
- [ ] PR の `build` と `GUI acceptance` が通る

## 範囲外

- なし
"""

GOOD = """## Summary

- Issue: Closes #10
- What changed and why: 例。

## Verification

- [x] `gradlew test` → BUILD SUCCESSFUL, 5 tests, 0 failures
- [x] `git diff --check` is empty.

## Completion criteria

- [x] `rg -n "SimpleDateFormat" TimeUtil.java` が 0 件 → `rg` の出力なし（exit 1）
- [x] 手順 1 のテストが前と後の両方で通る → 前: 5 tests 0 failures、後: 5 tests 0 failures
- [ ] PR の `build` と `GUI acceptance` が通る

## Risk / remaining work

- なし
"""


def problems(body, issue=ISSUE):
	return checklist.check(body, 10, checklist.issue_criteria(issue))


class CheckPrChecklistTest(unittest.TestCase):
	def test_complete_body_passes_with_only_the_required_ci_box_open(self):
		self.assertEqual([], problems(GOOD))

	def test_unchecked_box_other_than_required_ci_fails(self):
		body = GOOD.replace("- [x] `git diff --check`", "- [ ] `git diff --check`")
		self.assertTrue(any("チェックされていない" in p for p in problems(body)))

	def test_missing_criterion_fails(self):
		body = GOOD.replace("- [x] `rg -n \"SimpleDateFormat\" TimeUtil.java` が 0 件 → `rg` の出力なし（exit 1）\n", "")
		self.assertTrue(any("SimpleDateFormat" in p and "PR にない" in p for p in problems(body)))

	def test_criterion_without_evidence_fails(self):
		body = GOOD.replace(" → `rg` の出力なし（exit 1）", "")
		self.assertTrue(any("→ <コマンドと結果" in p for p in problems(body)))

	def test_checked_line_that_defers_to_ci_or_keeps_a_placeholder_fails(self):
		deferred = GOOD.replace("→ BUILD SUCCESSFUL, 5 tests, 0 failures", "→ CI")
		self.assertTrue(any("実行していない" in p for p in problems(deferred)))
		placeholder = GOOD.replace("`gradlew test` → BUILD SUCCESSFUL, 5 tests, 0 failures", "`<command>` → `<result>`")
		self.assertTrue(any("<command>" in p for p in problems(placeholder)))

	def test_issue_link_is_required_once(self):
		self.assertTrue(any("Closes #N" in p for p in problems(GOOD.replace("Closes #10", "#10"))))
		self.assertTrue(any("Closes #N" in p for p in problems(GOOD + "\nRefs #11\n")))

	def test_refs_allows_an_unmet_criterion_listed_as_remaining_work(self):
		body = GOOD.replace("Closes #10", "Refs #10").replace(
			"- [x] 手順 1 のテストが前と後の両方で通る → 前: 5 tests 0 failures、後: 5 tests 0 failures\n", "").replace(
			"## Risk / remaining work\n\n- なし", "## Risk / remaining work\n\n- 手順 1 のテストが前と後の両方で通る: 次の PR で行う。")
		self.assertEqual([], problems(body))

	def test_issue_without_criteria_fails(self):
		self.assertTrue(any("完了条件" in p for p in problems(GOOD, issue="## 概要\n\n例。\n")))

	def test_criteria_restated_in_a_comment_win_over_the_body(self):
		comment = "## 完了条件\n\n- [ ] コメントで足した条件\n"
		self.assertEqual(["コメントで足した条件"], checklist.issue_criteria(ISSUE, [comment]))

	def test_html_comments_in_the_template_are_ignored(self):
		body = GOOD.replace("## Verification\n", "## Verification\n\n<!-- - [ ] `<command>` -->\n")
		self.assertEqual([], problems(body))

	def test_unrun_evidence_with_an_explanation_fails(self):
		for marker in ("not run: Windows required", "未実行（環境がない）", "CI で確認する"):
			body = GOOD.replace("BUILD SUCCESSFUL, 5 tests, 0 failures", marker)
			self.assertTrue(any("実行していない" in p for p in problems(body)), marker)

	def test_only_the_dedicated_ci_status_criterion_may_stay_open(self):
		issue = ISSUE.replace("- [ ] PR の `build`", "- [ ] GUI acceptance の失敗時に診断ログを保存する\n- [ ] PR の `build`")
		body = GOOD.replace("- [ ] PR の `build`", "- [ ] GUI acceptance の失敗時に診断ログを保存する\n- [ ] PR の `build`")
		found = problems(body, issue)
		self.assertTrue(any("診断ログ" in p and "チェックされていない" in p for p in found))
		self.assertTrue(checklist.is_required_ci_item("上記の必須チェックが成功する") is False)
		self.assertTrue(checklist.is_required_ci_item("PR の必須チェック `build` と `GUI acceptance` が通る"))

	def test_a_repeated_issue_link_fails(self):
		self.assertTrue(any("Closes #N" in p for p in problems(GOOD + "\nCloses #10\n")))

	def test_a_substantive_trailing_parenthesis_is_kept(self):
		issue = ISSUE.replace("- [ ] PR の `build`", "- [ ] 出力形式を維持する（旧版ファイルを読み込める）\n- [ ] PR の `build`")
		body = GOOD.replace("- [ ] PR の `build`", "- [x] 出力形式を維持する → FormatTest: PASS\n- [ ] PR の `build`")
		self.assertTrue(any("出力形式" in p and "PR にない" in p for p in problems(body, issue)))

	def test_a_different_label_containing_the_criterion_does_not_match(self):
		issue = ISSUE.replace("- [ ] PR の `build`", "- [ ] 保存テストが通る\n- [ ] PR の `build`")
		body = GOOD.replace("- [ ] PR の `build`", "- [x] 自動保存テストが通る → AutoSaveTest: PASS\n- [ ] PR の `build`")
		self.assertTrue(any("保存テストが通る" in p and "PR にない" in p for p in problems(body, issue)))


if __name__ == "__main__":
	unittest.main()
