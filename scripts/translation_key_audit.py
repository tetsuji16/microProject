#!/usr/bin/env python3
"""Audit the default client bundle without guessing about dynamic keys.

The old unused-key count treated only literal Java calls as references.  This
report also scans XML/configuration resources and marks known runtime-generated
key families as dynamic.  It intentionally reports candidates; deletion must
still be reviewed because external locale bundles and plugins are supported.
"""

from __future__ import annotations

import argparse
import re
from pathlib import Path


DEFAULT_BUNDLE = Path("modules/micrproject_core/src/main/resources/com/microproject/strings/client.properties")
DYNAMIC_PREFIXES = (
    "Category.", "Date.Quarter", "Date.Half", "Field.", "Units.", "tip.",
    "T_", "Text.", "Bar.", "Styles.Bar.",
)


def properties_keys(path: Path) -> list[str]:
    keys: list[str] = []
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key = line.split("=", 1)[0].strip()
        if key:
            keys.append(key)
    return keys


def source_texts(root: Path) -> str:
    chunks: list[str] = []
    for path in root.rglob("*"):
        if not path.is_file() or "src/main" not in path.as_posix():
            continue
        if any(part in {"build", ".gradle", "out", "target"} for part in path.parts):
            continue
        if "/strings/" in path.as_posix():
            continue
        if path.suffix not in {".java", ".xml", ".properties", ".html"}:
            continue
        try:
            chunks.append(path.read_text(encoding="utf-8", errors="ignore"))
        except OSError:
            continue
    return "\n".join(chunks)


def dynamic_key(key: str, source: str) -> bool:
    if any(key.startswith(prefix) for prefix in DYNAMIC_PREFIXES):
        return True
    # Resource bundles are frequently addressed through format strings rather
    # than a literal key; retain keys whose prefix appears in concatenation or
    # a format expression anywhere in production sources.
    prefix = key.rsplit(".", 1)[0] + "." if "." in key else ""
    return bool(prefix and re.search(re.escape(prefix) + r"[\"']?\s*[+}]", source))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    bundle = args.root / DEFAULT_BUNDLE
    source = source_texts(args.root / "modules")
    literal_keys = set(re.findall(r"(?:getString|format)\(\s*[\"']([^\"']+)[\"']", source))
    rows: list[tuple[str, str]] = []
    for key in properties_keys(bundle):
        if dynamic_key(key, source):
            status = "dynamic"
        elif key in literal_keys:
            status = "literal"
        elif key in source:
            status = "resource-or-text"
        else:
            status = "candidate"
        rows.append((status, key))

    counts: dict[str, int] = {}
    for status, _ in rows:
        counts[status] = counts.get(status, 0) + 1
    lines = ["# Translation key audit", "", *(f"{status}: {count}" for status, count in sorted(counts.items())), "", "status\tkey"]
    lines.extend(f"{status}\t{key}" for status, key in rows)
    output = "\n".join(lines) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(output, encoding="utf-8")
    else:
        print(output, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
