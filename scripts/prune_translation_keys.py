#!/usr/bin/env python3
"""Remove explicitly reviewed dead translation keys without changing encoding.

The legacy locale files are a mixture of UTF-8 and Windows code pages.  This
tool therefore edits bytes after identifying each file's encoding and keeps
the original newline convention.  It is dry-run by default; use ``--apply``
only with the reviewed allowlist.
"""

from __future__ import annotations

import argparse
from pathlib import Path


def detect_encoding(data: bytes) -> str:
    try:
        data.decode("utf-8")
        return "utf-8"
    except UnicodeDecodeError:
        return "cp1252"


def load_keys(path: Path) -> set[str]:
    return {
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    }


def prune(path: Path, keys: set[str], apply: bool) -> int:
    raw = path.read_bytes()
    encoding = detect_encoding(raw)
    text = raw.decode(encoding)
    lines = text.splitlines(keepends=True)
    kept: list[str] = []
    removed = 0
    for line in lines:
        candidate = line.lstrip()
        if candidate and not candidate.startswith("#") and "=" in candidate:
            key = candidate.split("=", 1)[0].strip()
            if key in keys:
                removed += 1
                continue
        kept.append(line)
    if apply and removed:
        path.write_bytes("".join(kept).encode(encoding))
    return removed


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("modules/micrproject_core/src/main/resources/com/microproject/strings"))
    parser.add_argument("--allowlist", type=Path, default=Path("scripts/translation_key_prune_allowlist.txt"))
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    keys = load_keys(args.allowlist)
    total = 0
    for path in sorted(args.root.rglob("client*.properties")):
        removed = prune(path, keys, args.apply)
        if removed:
            print(f"{path}: {removed} key(s) {'removed' if args.apply else 'would be removed'}")
            total += removed
    print(f"total: {total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
