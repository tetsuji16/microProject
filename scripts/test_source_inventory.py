#!/usr/bin/env python3
"""Inventory Java test sources and catch stale file paths in TEST_PLAN.md.

This is a conservative audit aid: it classifies source files by visible JUnit
annotations and validates repository-relative file paths documented inline.
It does not infer that an assertion is meaningful or that a GUI route passed.
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter, defaultdict
from pathlib import Path


TEST_ANNOTATION = re.compile(
    r"@(?:Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate)\b"
)
CLASS_DECLARATION = re.compile(r"\b(?:class|record|enum)\s+(\w+)")
JUNIT_DYNAMIC_CALL = re.compile(r"DynamicTest\.dynamicTest\s*\(")
LITERAL_ID = re.compile(r'^\s*"([^\"]+)"\s*$')
NUMBERED_ID = re.compile(r'^\s*id\(\s*"([^\"]+)"\s*,\s*(\d+)\s*\)\s*$')
ID_PREFIX = re.compile(r'return\s+"([^\"]+)"\s*\+\s*group\b')
INLINE_CODE = re.compile(r"`([^`]+)`")
FILE_SUFFIX = re.compile(r"\.[A-Za-z0-9]{1,8}$")


def test_sources(repo: Path) -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    modules = repo / "modules"
    for source_set in ("test", "guiTest"):
        for path in sorted(modules.glob(f"*/src/{source_set}/java/**/*.java")):
            text = path.read_text(encoding="utf-8")
            searchable, comment_free = java_views(text)
            annotations = TEST_ANNOTATION.findall(searchable)
            classes = CLASS_DECLARATION.findall(searchable)
            rows.append(
                {
                    "path": path.relative_to(repo).as_posix(),
                    "module": path.relative_to(modules).parts[0],
                    "source_set": source_set,
                    "classification": "test" if annotations else "support-or-manual",
                    "class_names": classes,
                    "junit_annotations": len(annotations),
                }
            )
    return rows


def documented_file_paths(markdown: str) -> list[str]:
    paths: set[str] = set()
    for token in INLINE_CODE.findall(markdown):
        candidate = token.strip().strip('"')
        if candidate.startswith(".") or candidate.startswith(("python ", "java ")):
            continue
        if "/" not in candidate or not FILE_SUFFIX.search(candidate):
            continue
        if any(marker in candidate for marker in ("<", ">", "$", "*")):
            continue
        paths.add(candidate)
    return sorted(paths)


def first_call_argument(text: str, opening_paren: int) -> str | None:
    depth = 1
    quoted = False
    escaped = False
    argument_start = opening_paren + 1
    for index in range(argument_start, len(text)):
        char = text[index]
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
            continue
        if char == '"':
            quoted = True
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[argument_start:index].strip()
        elif char == "," and depth == 1:
            return text[argument_start:index].strip()
    return None


def java_views(text: str) -> tuple[str, str]:
    """Return (code-only, code-with-strings) views with comments blanked out."""
    searchable = list(text)
    comment_free = list(text)
    state = "code"
    index = 0
    while index < len(text):
        char = text[index]
        next_char = text[index + 1] if index + 1 < len(text) else ""
        if state == "code":
            if char == "/" and next_char == "/":
                state = "line-comment"
                searchable[index:index + 2] = [" ", " "]
                comment_free[index:index + 2] = [" ", " "]
                index += 2
                continue
            if char == "/" and next_char == "*":
                state = "block-comment"
                searchable[index:index + 2] = [" ", " "]
                comment_free[index:index + 2] = [" ", " "]
                index += 2
                continue
            if text.startswith('"""', index):
                state = "textblock"
                searchable[index:index + 3] = [" ", " ", " "]
                index += 3
                continue
            if char == '"':
                state = "string"
                searchable[index] = " "
                index += 1
                continue
            if char == "'":
                state = "char"
                searchable[index] = " "
                index += 1
                continue
        elif state == "textblock":
            searchable[index] = " "
            if text.startswith('"""', index):
                backslashes = 0
                cursor = index - 1
                while cursor >= 0 and text[cursor] == "\\":
                    backslashes += 1
                    cursor -= 1
                if backslashes % 2 == 0:
                    searchable[index:index + 3] = [" ", " ", " "]
                    state = "code"
                    index += 3
                    continue
            index += 1
            continue
        elif state in ("string", "char"):
            searchable[index] = " "
            if char == "\\":
                if index + 1 < len(text):
                    searchable[index + 1] = " "
                    index += 2
                    continue
            elif (state == "string" and char == '"') or (state == "char" and char == "'"):
                state = "code"
            index += 1
            continue
        elif state == "line-comment":
            searchable[index] = " "
            comment_free[index] = " "
            if char == "\n":
                state = "code"
            index += 1
            continue
        elif state == "block-comment":
            searchable[index] = " "
            comment_free[index] = " "
            if char == "*" and next_char == "/":
                searchable[index + 1] = " "
                comment_free[index + 1] = " "
                state = "code"
                index += 2
                continue
            index += 1
            continue
        index += 1
    return "".join(searchable), "".join(comment_free)


def missing_documented_paths(repo: Path) -> list[str]:
    plan = repo / "TEST_PLAN.md"
    if not plan.exists():
        return ["TEST_PLAN.md"]
    return [
        value
        for value in documented_file_paths(plan.read_text(encoding="utf-8"))
        if not (repo / value).exists()
    ]


def dynamic_id_audit(repo: Path) -> tuple[dict[str, list[str]], list[str]]:
    locations: dict[str, list[str]] = defaultdict(list)
    unresolved: list[str] = []
    for row in test_sources(repo):
        path = repo / str(row["path"])
        text = path.read_text(encoding="utf-8")
        searchable, comment_free = java_views(text)
        helper = ID_PREFIX.search(comment_free)
        prefix = helper.group(1) if helper else ""
        for match in JUNIT_DYNAMIC_CALL.finditer(searchable):
            expression = first_call_argument(comment_free, match.end() - 1)
            if expression is None:
                unresolved.append(f"{row['path']}: <unparsed call>")
                continue
            literal = LITERAL_ID.fullmatch(expression)
            numbered = NUMBERED_ID.fullmatch(expression)
            if literal:
                name = literal.group(1)
            elif numbered and prefix:
                name = f"{prefix}{numbered.group(1)}-{int(numbered.group(2)):02d}"
            else:
                unresolved.append(f"{row['path']}: {expression}")
                continue
            locations[name].append(str(row["path"]))
    duplicates = {name: paths for name, paths in sorted(locations.items()) if len(paths) > 1}
    return duplicates, unresolved


def build_inventory(repo: Path) -> dict[str, object]:
    rows = test_sources(repo)
    counts = Counter((str(row["source_set"]), str(row["classification"])) for row in rows)
    duplicate_ids, unresolved_ids = dynamic_id_audit(repo)
    return {
        "source_counts": {
            f"{source_set}:{classification}": count
            for (source_set, classification), count in sorted(counts.items())
        },
        "sources": rows,
        "missing_documented_test_plan_paths": missing_documented_paths(repo),
        "duplicate_static_dynamic_test_names": duplicate_ids,
        "unresolved_dynamic_test_name_expressions": unresolved_ids,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--output", type=Path, help="Write JSON to this path instead of stdout")
    parser.add_argument("--check", action="store_true", help="Fail on missing documented paths or duplicate literal IDs")
    args = parser.parse_args()
    inventory = build_inventory(args.repo.resolve())
    rendered = json.dumps(inventory, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")
    if args.check and (
        inventory["missing_documented_test_plan_paths"]
        or inventory["duplicate_static_dynamic_test_names"]
    ):
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
