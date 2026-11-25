#!/usr/bin/env python3
# Copyright (c) 2026 microProject
# SPDX-License-Identifier: MIT
"""Check that a pull request body proves every completion criterion of its issue.

A pull request may leave a checkbox open only for the required CI checks
(`build` and `GUI acceptance`), which branch protection already enforces.
Every other checkbox must be checked, and every completion criterion of the
linked issue must be copied into the body as a checked line followed by
`→ <evidence>`.

Usage:
	python scripts/check_pr_checklist.py --body-file pr.md            # issue fetched with gh
	python scripts/check_pr_checklist.py --body-file pr.md --issue-body-file issue.md
	python scripts/check_pr_checklist.py --pr 1395                    # check an open PR
	python scripts/check_pr_checklist.py --event "$GITHUB_EVENT_PATH" # GitHub Actions
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

DEFAULT_REPO = "tetsuji16/microProject"
REQUIRED_HEADINGS = ("Summary", "Verification", "Completion criteria", "Risk / remaining work")
CRITERIA_HEADINGS = ("完了条件", "完了基準・提出証跡", "完了基準", "Completion criteria", "Acceptance criteria")
CHECKBOX = re.compile(r"^(\s*)[-*]\s+\[( |x|X)\]\s*(.*)$")
ISSUE_LINK = re.compile(r"\b(Closes|Refs)\s+#(\d+)\b", re.IGNORECASE)
PLACEHOLDER = re.compile(r"<(command|result|command and result|evidence|criterion copied from the issue)>", re.IGNORECASE)
# "→ CI", "→ not run: Windows required", "→ 未実行（理由）" and the like are not evidence.
NOT_DONE_EVIDENCE = re.compile(r"→\s*`?(CI|not run|未実行|TODO|TBD|pending)(?![A-Za-z0-9])", re.IGNORECASE)
# The one criterion that only CI can satisfy, after normalize(): "PR の `build` と `GUI acceptance` が通る".
REQUIRED_CI_LABEL = re.compile(
	r"^(PRの)?(buildとGUIacceptance|必須チェック(buildとGUIacceptance)?)(の両方)?が(通る|成功する)[。.]?$")
# A trailing note that only tells the implementer what to write in the PR may be left out.
PR_WRITING_NOTE = re.compile(r"\s*(\([^()]*PR[^()]*\)|（[^（）]*PR[^（）]*）)\s*[。.]?\s*$")


@dataclass
class Item:
	checked: bool
	text: str  # first line and indented continuation lines, joined with spaces
	line_no: int


def normalize(text: str) -> str:
	text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
	return re.sub(r"[\s`*_]", "", text)


def strip_comments(markdown: str) -> str:
	return re.sub(r"<!--.*?-->", "", markdown, flags=re.S)


def sections(markdown: str) -> dict[str, str]:
	"""Map each `## heading` to its text (up to the next heading of level 1 or 2)."""
	result: dict[str, str] = {}
	current = None
	buffer: list[str] = []
	for line in strip_comments(markdown).splitlines():
		match = re.match(r"^#{1,2}\s+(.+?)\s*$", line)
		if match:
			if current is not None:
				result.setdefault(current, "\n".join(buffer))
			current = re.sub(r"\s*\(.*\)$|\s*（.*）$", "", match.group(1)).strip()
			buffer = []
		elif current is not None:
			buffer.append(line)
	if current is not None:
		result.setdefault(current, "\n".join(buffer))
	return result


def checkbox_items(markdown: str) -> list[Item]:
	items: list[Item] = []
	lines = strip_comments(markdown).splitlines()
	index = 0
	while index < len(lines):
		match = CHECKBOX.match(lines[index])
		if not match:
			index += 1
			continue
		indent = len(match.group(1))
		parts = [match.group(3).strip()]
		start = index
		index += 1
		while index < len(lines):
			line = lines[index]
			if not line.strip() or CHECKBOX.match(line):
				break
			if len(line) - len(line.lstrip()) <= indent and re.match(r"^\s*([-*]|\d+\.)\s", line):
				break
			if re.match(r"^#{1,6}\s", line):
				break
			parts.append(line.strip())
			index += 1
		items.append(Item(match.group(2) != " ", " ".join(parts), start + 1))
	return items


def is_required_ci_item(text: str) -> bool:
	return bool(REQUIRED_CI_LABEL.match(normalize(text)))


def issue_criteria(issue_body: str, comments: list[str] = ()) -> list[str]:
	"""Return the completion criteria, preferring the newest comment that restates them."""
	for source in [*reversed(list(comments)), issue_body]:
		found = sections(source or "")
		for heading in CRITERIA_HEADINGS:
			if heading in found:
				criteria = [item.text for item in checkbox_items(found[heading]) if item.text]
				if criteria:
					return criteria
	return []


def check(body: str, issue_number: int | None, criteria: list[str]) -> list[str]:
	problems: list[str] = []
	body = body or ""
	links = [(kind.capitalize(), int(number)) for kind, number in ISSUE_LINK.findall(body)]
	if len(links) != 1:
		problems.append("本文に `Closes #N` か `Refs #N` を 1 つだけ書く（今: %d 個）。" % len(links))
	kind = links[0][0] if len(links) == 1 else "Closes"

	found = sections(body)
	for heading in REQUIRED_HEADINGS:
		if heading not in found:
			problems.append("見出し `## %s` がない（.github/pull_request_template.md に沿う）。" % heading)

	items = checkbox_items(body)
	for item in items:
		label = "%d 行目「%s」" % (item.line_no, item.text[:60])
		if not item.checked:
			if not is_required_ci_item(item.text):
				problems.append(label + ": チェックされていない。確認して [x] にするか、行を消して「Risk / remaining work」に理由を書く。")
			continue
		if PLACEHOLDER.search(item.text):
			problems.append(label + ": テンプレートの `<command>` などが残っている。実際のコマンドと結果を書く。")
		if NOT_DONE_EVIDENCE.search(item.text):
			problems.append(label + ": 実行していない確認をチェックしている。自分で実行した結果を書く。")
		if re.search(r"(:|：|→)\s*$", item.text):
			problems.append(label + ": 根拠か「not applicable」の理由が空。")

	if issue_number is None:
		return problems
	if not criteria:
		problems.append("issue #%d に `## 完了条件` のチェックボックスがない。実装する前に issue に完了条件を足す。" % issue_number)
		return problems

	risk = normalize(found.get("Risk / remaining work", ""))
	normalized_items = [(item, normalize(item.text)) for item in items]
	for criterion in criteria:
		if is_required_ci_item(criterion):
			continue
		labels = {normalize(criterion), normalize(PR_WRITING_NOTE.sub("", criterion))} - {""}
		if not labels:
			continue
		# The checked line must start with the whole criterion and continue only with "→ <evidence>".
		matches = [(item, text, label) for item, text in normalized_items for label in labels
			if text == label or text.startswith(label + "→")]
		if not matches:
			if kind == "Refs" and any(label in risk for label in labels):
				continue
			problems.append("完了条件「%s」が PR にない。そのまま写して `- [x] <完了条件> → <根拠>` と書く。" % criterion[:80])
			continue
		item, text, label = matches[0]
		if not item.checked:
			continue  # already reported above
		if not re.search(r"→.{2,}", text[len(label):]):
			problems.append("完了条件「%s」の後に `→ <コマンドと結果、またはテスト名>` がない。" % criterion[:80])
	return problems


def gh_json(path: str) -> object:
	output = subprocess.run(["gh", "api", path], check=True, capture_output=True, text=True, encoding="utf-8").stdout
	return json.loads(output)


def fetch_issue(repo: str, number: int) -> tuple[str, list[str]]:
	issue = gh_json("repos/%s/issues/%d" % (repo, number))
	comments: list = []
	page = 1
	while True:
		batch = gh_json("repos/%s/issues/%d/comments?per_page=100&page=%d" % (repo, number, page))
		comments.extend(batch)
		if len(batch) < 100:
			break
		page += 1
	return issue.get("body") or "", [comment.get("body") or "" for comment in comments]


def main(argv: list[str] | None = None) -> int:
	parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
	source = parser.add_mutually_exclusive_group(required=True)
	source.add_argument("--body-file", type=Path, help="PR body to check")
	source.add_argument("--pr", type=int, help="number of an existing PR to check")
	source.add_argument("--event", type=Path, help="GitHub Actions event payload")
	parser.add_argument("--issue-body-file", type=Path, help="issue body (skips fetching the issue)")
	parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY", DEFAULT_REPO))
	args = parser.parse_args(argv)

	if args.event:
		pull = json.loads(args.event.read_text(encoding="utf-8"))["pull_request"]
		body = pull.get("body") or ""
	elif args.pr:
		body = gh_json("repos/%s/pulls/%d" % (args.repo, args.pr)).get("body") or ""
	else:
		body = args.body_file.read_text(encoding="utf-8")

	links = ISSUE_LINK.findall(body)
	issue_number = int(links[0][1]) if len(links) == 1 else None
	criteria: list[str] = []
	if args.issue_body_file:
		criteria = issue_criteria(args.issue_body_file.read_text(encoding="utf-8"))
	elif issue_number is not None:
		criteria = issue_criteria(*fetch_issue(args.repo, issue_number))

	problems = check(body, issue_number, criteria)
	if problems:
		print("PR のチェックリストが未完了です（%d 件）:" % len(problems))
		for problem in problems:
			print("- " + problem)
		return 1
	print("OK: すべてのチェックボックスと issue #%s の完了条件 %d 件に根拠がある。" % (issue_number, len(criteria)))
	return 0


if __name__ == "__main__":
	sys.exit(main())
