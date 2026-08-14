#!/usr/bin/env python3
"""Collapse the fine-grained delta ledger into actionable review groups."""

from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path


GROUP_FIELDS = [
    "group_id", "kind", "module", "representative_path", "item_count",
    "pending_count", "priority", "sample_item_ids", "review_scope",
]


def group_path(row: dict[str, str]) -> str:
    return row.get("current_path") or row.get("baseline_path") or row.get("initial_path") or row.get("openproj_path") or "(unknown)"


def priority(path: str, kind: str) -> str:
    value = path.lower()
    if any(token in value for token in ("brand", "logo", "donate", "paypal", "url", "menu", "release")):
        return "HIGH"
    if kind in {"HUNK", "ASSET", "PACKAGING", "XML_NODE"} or "packaging" in value:
        return "MEDIUM"
    return "LOW"


def scope(kind: str, path: str) -> str:
    if kind == "RESOURCE_KEY":
        return "review resource file as a functional bundle; inspect only ProjectLibre-specific keys"
    if kind == "XML_NODE":
        return "review template/config file as a document; inspect changed nodes in context"
    if kind in {"METHOD", "CONSTRUCTOR", "FIELD", "TYPE"}:
        return "review related Java symbols in the same source file"
    return "review the file or asset and confirm whether the delta is user-visible or distributable"


def build_groups(rows: list[dict[str, str]]) -> list[dict[str, str]]:
    buckets: dict[tuple[str, str, str], list[dict[str, str]]] = defaultdict(list)
    for row in rows:
        if row.get("work_status") == "VERIFIED":
            continue
        kind = row.get("kind", "UNKNOWN")
        path = group_path(row)
        buckets[(kind, row.get("module", ""), path)].append(row)
    result = []
    for index, ((kind, module, path), bucket) in enumerate(sorted(buckets.items()), 1):
        pending = len(bucket)
        ids = ",".join(row["item_id"] for row in bucket[:5])
        result.append({
            "group_id": f"GRP-{index:05d}",
            "kind": kind,
            "module": module,
            "representative_path": path,
            "item_count": str(len(bucket)),
            "pending_count": str(pending),
            "priority": priority(path, kind),
            "sample_item_ids": ids,
            "review_scope": scope(kind, path),
        })
    return result


def write_summary(groups: list[dict[str, str]], path: Path, total: int, verified: int) -> None:
    counts = defaultdict(int)
    priorities = defaultdict(int)
    for group in groups:
        counts[group["kind"]] += int(group["pending_count"])
        priorities[group["priority"]] += 1
    lines = [
        "# ProjectLibre review groups",
        "",
        f"- Fine-grained candidates: **{total:,}**",
        f"- Already VERIFIED: **{verified:,}**",
        f"- Pending candidate items: **{total - verified:,}**",
        f"- Actionable review groups: **{len(groups):,}**",
        "",
        "## Priority groups",
        "",
        "| Priority | Groups |",
        "|---|---:|",
    ]
    for key in ("HIGH", "MEDIUM", "LOW"):
        lines.append(f"| {key} | {priorities[key]:,} |")
    lines += ["", "## Pending items by kind", "", "| Kind | Items |", "|---|---:|"]
    for key in sorted(counts):
        lines.append(f"| {key} | {counts[key]:,} |")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, default=Path("docs/audit/projectlibre-delta-items.csv"))
    parser.add_argument("--output", type=Path, default=Path("docs/audit/projectlibre-review-groups.csv"))
    parser.add_argument("--summary", type=Path, default=Path("docs/audit/projectlibre-review-groups.md"))
    args = parser.parse_args()
    with args.input.open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream))
    groups = build_groups(rows)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=GROUP_FIELDS)
        writer.writeheader()
        writer.writerows(groups)
    write_summary(groups, args.summary, len(rows), sum(row.get("work_status") == "VERIFIED" for row in rows))
    print(f"wrote {len(groups):,} review groups from {len(rows):,} candidates")


if __name__ == "__main__":
    main()
