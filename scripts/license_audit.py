#!/usr/bin/env python3
"""Build a provenance ledger for the OpenProj -> ProjectLibre -> fork chain.

This is intentionally a conservative inventory tool.  It does not decide that a
file is a ProjectLibre derivative merely because the current header says so, and
it never edits source or license files.  Files which cannot be proven to be
OpenProj-origin are left as REVIEW for a human hunk-level review.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import re
import subprocess
from collections import Counter, defaultdict
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
OPENPROJ_COMMIT = "d2fa3c20a"
PROJECTLIBRE_BASELINE = "0530be227f4a10c5545cce8d3db20ac5a4d76a66"
CURRENT_REF = "HEAD"
DEFAULT_OPENPROJ_ARCHIVE = "docs/legal/openproj-1.4-src.tar.gz"
DEFAULT_OPENPROJ_SOURCE_URL = "https://sourceforge.net/projects/openproj/files/OpenProj%20Binaries/1.4/openproj-1.4-src.tar.gz/download"
DEFAULT_ARCHIVE_SHA256 = "20071b090d841388860049ce49724e2773b8cec250d76e74264c71adf2a79ac6"

MODULE_MAP = {
    "micrproject_contrib": "openproj_contrib",
    "micrproject_core": "openproj_core",
    "micrproject_exchange": "openproj_exchange",
    "micrproject_reports": "openproj_reports",
    "micrproject_ui": "openproj_ui",
}

DISPOSITIONS = (
    "KEEP_OPENPROJ",
    "KEEP_THIRD_PARTY",
    "KEEP_FORK_ORIGINAL",
    "DELETE_PROJECTLIBRE_DELTA",
    "REIMPLEMENT_PROJECTLIBRE_DELTA",
    "REVIEW",
)


def git(*args: str) -> str:
    return subprocess.check_output(["git", *args], cwd=REPO_ROOT, text=True, encoding="utf-8").replace("\r\n", "\n")


def git_bytes(*args: str) -> bytes:
    return subprocess.check_output(["git", *args], cwd=REPO_ROOT)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def source_kind(path: str) -> str:
    if "/src/main/java/" in path:
        return "java"
    if "/src/main/resources/" in path:
        return "resource"
    if "/src/test/" in path:
        return "test"
    if path.startswith("packaging/"):
        return "packaging"
    return "other"


def claimed_origin(text: str) -> str:
    if re.search(r"Original Code is OpenProj|Original Code is OpenProj and", text, re.I):
        return "OpenProj"
    if re.search(r"Original Code is ProjectLibre|Original Developer.*ProjectLibre", text, re.I):
        return "ProjectLibre"
    if re.search(r"Apache Software Foundation|Apache License", text, re.I):
        return "ThirdParty:Apache"
    if re.search(r"JasperReports|LGPL", text, re.I):
        return "ThirdParty:JasperReports"
    return "Unknown"


def strip_header(text: str) -> str:
    """Remove only standard license prologues for a conservative similarity hint."""
    text = text.replace("\r\n", "\n")
    if text.startswith("#!"):
        text = text.split("\n", 1)[1] if "\n" in text else ""
    if text.startswith("/*"):
        end = text.find("*/")
        if end >= 0:
            text = text[end + 2 :]
    elif text.startswith("#"):
        lines = text.splitlines()
        while lines and (lines[0].lstrip().startswith("#") or not lines[0].strip()):
            lines.pop(0)
        text = "\n".join(lines)
    text = re.sub(r"(?i)com\.projectlibre1", "com.projity", text)
    text = re.sub(r"(?i)org\.projectlibre1", "org.projity", text)
    return re.sub(r"\s+", " ", text).strip()


def old_candidates(current: str) -> list[str]:
    parts = current.split("/", 2)
    if len(parts) != 3 or parts[0] != "modules" or parts[1] not in MODULE_MAP:
        return []
    module, rest = parts[1], parts[2]
    rest = re.sub(r"^src/main/(java|resources)/", "src/", rest)
    rest = re.sub(r"^src/test/(java|resources)/", "test/", rest)
    variants = [rest]
    variants.append(rest.replace("com/projectlibre1", "com/projity"))
    variants.append(rest.replace("org/projectlibre1", "org/projity"))
    variants.append(rest.replace("com/projectlibre1", "com/projity").replace("org/projectlibre1", "org/projity"))
    return [f"{MODULE_MAP[module]}/{item}" for item in dict.fromkeys(variants)]


def tree_hashes(ref: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in git("ls-tree", "-r", ref).splitlines():
        match = re.match(r"\d+ blob ([0-9a-f]+)\t(.+)$", line)
        if match:
            result[match.group(2)] = match.group(1)
    return result


def blobs(hashes: set[str]) -> dict[str, bytes]:
    if not hashes:
        return {}
    payload = ("".join(f"{item}\n" for item in hashes)).encode("ascii")
    process = subprocess.run(["git", "cat-file", "--batch"], cwd=REPO_ROOT, input=payload, stdout=subprocess.PIPE, check=True)
    data = process.stdout
    result: dict[str, bytes] = {}
    offset = 0
    for item in hashes:
        header_end = data.find(b"\n", offset)
        header = data[offset:header_end].decode("ascii")
        _, kind, size_text = header.split(" ")
        offset = header_end + 1
        size = int(size_text)
        value = data[offset : offset + size]
        offset += size + 1
        if kind == "blob":
            result[item] = value
    return result


def build_rows() -> list[dict[str, str]]:
    current_tree = tree_hashes(CURRENT_REF)
    old_tree = tree_hashes(OPENPROJ_COMMIT)
    selected = [path for path in current_tree if path.startswith("modules/") or path.startswith("packaging/")]
    current_blobs = blobs({current_tree[path] for path in selected})
    old_blobs = blobs(set(old_tree.values()))
    rows: list[dict[str, str]] = []
    for path in sorted(selected):
        current = current_blobs[current_tree[path]]
        text = current.decode("utf-8", errors="replace")
        candidates = old_candidates(path)
        old_path = next((candidate for candidate in candidates if candidate in old_tree), "")
        old = b""
        if old_path:
            old = old_blobs[old_tree[old_path]]
        normalized_match = bool(old) and strip_header(text) == strip_header(old.decode("utf-8", errors="replace"))
        origin = claimed_origin(text)
        if normalized_match:
            disposition = "KEEP_OPENPROJ"
            reason = "normalized content matches OpenProj baseline"
        elif origin.startswith("ThirdParty:"):
            disposition = "KEEP_THIRD_PARTY"
            reason = "third-party notice detected; verify upstream license"
        elif not old_path:
            disposition = "REVIEW"
            reason = "no path match in OpenProj baseline; classify origin manually"
        else:
            disposition = "REVIEW"
            reason = "OpenProj path exists but content differs; perform hunk-level comparison"
        rows.append(
            {
                "current_path": path,
                "module": path.split("/", 2)[1] if path.startswith("modules/") else "packaging",
                "kind": source_kind(path),
                "current_ref": CURRENT_REF,
                "current_sha256": sha256(current),
                "openproj_commit": OPENPROJ_COMMIT,
                "openproj_path": old_path,
                "openproj_sha256": sha256(old) if old else "",
                "micrproject_baseline": PROJECTLIBRE_BASELINE,
                # Per-file history is intentionally left for the human review.
                # Running `git log --follow` 1,900 times makes the inventory
                # needlessly slow and can also obscure rename history.
                "first_commit": "REVIEW_REQUIRED",
                "claimed_origin": origin,
                "normalized_openproj_match": str(normalized_match).lower(),
                "introduced_by": "REVIEW_REQUIRED",
                "license": "CPAL-1.0" if "Common Public Attribution License" in text else "REVIEW_REQUIRED",
                "disposition": disposition,
                "evidence": reason,
                "reviewer": "",
            }
        )
    return rows


def write_outputs(rows: list[dict[str, str]], output: Path, summary: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    fields = list(rows[0]) if rows else []
    with output.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)
    by_module: dict[str, Counter[str]] = defaultdict(Counter)
    for row in rows:
        by_module[row["module"]][row["disposition"]] += 1
    lines = [
        "# License provenance audit (Phase 0/1)",
        "",
        f"- Current ref: `{CURRENT_REF}`",
        f"- OpenProj comparison commit: `{OPENPROJ_COMMIT}`",
        f"- ProjectLibre baseline: `{PROJECTLIBRE_BASELINE}`",
        f"- Official OpenProj 1.4 archive: `{DEFAULT_OPENPROJ_ARCHIVE}` (SHA-256: `{DEFAULT_ARCHIVE_SHA256}`)",
        f"- Archive source URL: {DEFAULT_OPENPROJ_SOURCE_URL}",
        "- The CSV is a conservative ledger. `REVIEW` is not a finding that the file is ProjectLibre-derived.",
        "- `KEEP_OPENPROJ` means only that the normalized content matched the repository's OpenProj baseline; it is not a legal conclusion.",
        "",
        "## Disposition counts",
        "",
        "| Module | Total | KEEP_OPENPROJ | KEEP_THIRD_PARTY | KEEP_FORK_ORIGINAL | DELETE_PROJECTLIBRE_DELTA | REIMPLEMENT_PROJECTLIBRE_DELTA | REVIEW |",
        "|---|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for module in sorted(by_module):
        counts = by_module[module]
        lines.append("| " + " | ".join([module, str(sum(counts.values()))] + [str(counts[name]) for name in DISPOSITIONS]) + " |")
    lines += [
        "",
        "## Required human follow-up",
        "",
        "1. Supply and verify the official OpenProj 1.4 archive checksum.",
        "2. Review every `REVIEW` row against OpenProj 1.4, ProjectLibre initial history, the 1.9.8 baseline, and current HEAD.",
        "3. Split mixed files at hunk level before assigning `DELETE_PROJECTLIBRE_DELTA` or `REIMPLEMENT_PROJECTLIBRE_DELTA`.",
        "4. Record reviewer, evidence, and the selected disposition in the CSV; do not remove CPAL notices before this review is complete.",
    ]
    summary.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=REPO_ROOT / "docs" / "legal" / "license-provenance.csv")
    parser.add_argument("--summary", type=Path, default=REPO_ROOT / "docs" / "legal" / "license-provenance-summary.md")
    args = parser.parse_args()
    rows = build_rows()
    write_outputs(rows, args.output, args.summary)
    print(f"wrote {len(rows)} rows to {args.output}")
    print(f"wrote summary to {args.summary}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
