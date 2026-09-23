#!/usr/bin/env python3
"""Reconcile the legacy OpenProj provenance ledger with the current core tree.

This reports path/content evidence only. It does not determine active callers,
hunk-level origin, legal disposition, or whether a source file is a migration
candidate.
"""

from __future__ import annotations

import argparse
import csv
import re
import subprocess
import sys
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from scripts.license_audit import strip_header  # noqa: E402


MODULES = {
    "projectlibre_core": "microproject_core",
    "projectlibre_exchange": "microproject_exchange",
    "projectlibre_ui": "microproject_ui",
    "projectlibre_reports": "microproject_reports",
    "projectlibre_contrib": "microproject_contrib",
}


def git(*args: str) -> bytes:
    return subprocess.check_output(["git", *args], cwd=ROOT)


def tree(ref: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in git("ls-tree", "-r", ref).decode("utf-8").splitlines():
        match = re.match(r"\d+ blob ([0-9a-f]+)\t(.+)$", line)
        if match:
            result[match.group(2)] = match.group(1)
    return result


def read_blobs(object_ids: set[str]) -> dict[str, bytes]:
    if not object_ids:
        return {}
    request = "".join(f"{object_id}\n" for object_id in sorted(object_ids)).encode("ascii")
    response = subprocess.check_output(["git", "cat-file", "--batch"], cwd=ROOT, input=request)
    blobs: dict[str, bytes] = {}
    offset = 0
    for object_id in sorted(object_ids):
        header_end = response.index(b"\n", offset)
        _, kind, size_text = response[offset:header_end].decode("ascii").split(" ")
        offset = header_end + 1
        size = int(size_text)
        content = response[offset : offset + size]
        offset += size + 1
        if kind == "blob":
            blobs[object_id] = content
    return blobs


def current_path(legacy_path: str) -> str:
    parts = legacy_path.split("/", 2)
    if len(parts) != 3 or parts[0] != "modules" or parts[1] not in MODULES:
        return ""
    path = f"modules/{MODULES[parts[1]]}/{parts[2]}"
    return path.replace("/com/projectlibre1/", "/com/microproject/").replace(
        "/org/projectlibre1/", "/org/microproject/"
    )


def normalized(text: bytes) -> str:
    source = strip_header(text.decode("utf-8", errors="replace"))
    return source.replace("com.microproject", "com.projity").replace("org.microproject", "org.projity")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ref", default="origin/master", help="current source ref to compare (default: origin/master)")
    parser.add_argument("--ledger", type=Path, default=ROOT / "docs/legal/license-provenance.csv")
    parser.add_argument("--details", action="store_true", help="list each candidate's comparison status and paths")
    args = parser.parse_args()

    with args.ledger.open(encoding="utf-8", newline="") as handle:
        candidates = [
            row
            for row in csv.DictReader(handle)
            if row["module"] == "projectlibre_core"
            and row["kind"] == "java"
            and row["normalized_openproj_match"] == "true"
            and "/src/main/java/" in row["current_path"]
        ]

    current_tree = tree(args.ref)
    upstream_tree = tree("d2fa3c20a")
    mapped_paths = {row["current_path"]: current_path(row["current_path"]) for row in candidates}
    object_ids = {
        current_tree[path]
        for path in mapped_paths.values()
        if path in current_tree
    } | {
        upstream_tree[row["openproj_path"]]
        for row in candidates
        if row["openproj_path"] in upstream_tree
    }
    contents = read_blobs(object_ids)

    counts: Counter[str] = Counter()
    missing: list[tuple[str, str]] = []
    details: list[tuple[str, str, str]] = []
    for row in candidates:
        old_path = row["current_path"]
        mapped = mapped_paths[old_path]
        current_oid = current_tree.get(mapped)
        upstream_oid = upstream_tree.get(row["openproj_path"])
        if current_oid is None:
            counts["missing_current_path"] += 1
            missing.append((mapped, row["openproj_path"]))
            details.append(("missing_current_path", old_path, mapped))
        elif upstream_oid is None:
            counts["missing_openproj_path"] += 1
            details.append(("missing_openproj_path", old_path, mapped))
        elif normalized(contents[current_oid]) == strip_header(
            contents[upstream_oid].decode("utf-8", errors="replace")
        ):
            counts["normalized_content_match"] += 1
            details.append(("normalized_content_match", old_path, mapped))
        else:
            counts["content_differs"] += 1
            details.append(("content_differs", old_path, mapped))

    print(f"ref: {args.ref}")
    print(f"ledger candidates: {len(candidates)}")
    for status, count in sorted(counts.items()):
        print(f"{status}: {count}")
    if missing:
        print("\nMissing mapped paths (not evidence of dead code):")
        for mapped, upstream in missing:
            print(f"{mapped} | {upstream}")
    if args.details:
        print("\nCandidate details (status | ledger path | mapped current path):")
        for status, ledger_path, mapped in sorted(details):
            print(f"{status} | {ledger_path} | {mapped}")
    print("\nPath/content comparison is not a caller audit, hunk-level provenance review, or legal conclusion.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
