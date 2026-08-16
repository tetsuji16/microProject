#!/usr/bin/env python3
"""Normalize license headers in microproject Java sources to the MIT header.

Replaces the legacy CPAL (Common Public Attribution License) banner, and adds
the MIT banner to files that have no header at all. Files already carrying the
MIT header (contains "Copyright (c) 2026 microProject") are left untouched.

Usage:
    python3 scripts/normalize_license_headers.py [--dry-run]
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HEADER_TEMPLATE = os.path.join(ROOT, "scripts", "mit_header.txt")
EXCLUDE_DIRS = {"micrproject_contrib"}  # keep third-party notices intact

def load_template():
    with open(HEADER_TEMPLATE, "r", encoding="utf-8") as f:
        return f.read()

def find_java_files():
    result = []
    modules_dir = os.path.join(ROOT, "modules")
    for module in sorted(os.listdir(modules_dir)):
        if not module.startswith("micrproject_"):
            continue
        if module in EXCLUDE_DIRS:
            continue
        src = os.path.join(modules_dir, module, "src")
        if not os.path.isdir(src):
            continue
        for root, _, files in os.walk(src):
            if "/build/" in root.replace("\\", "/"):
                continue
            for fn in files:
                if fn.endswith(".java"):
                    result.append(os.path.join(root, fn))
    return result

def strip_cpal_block(content):
    """If content starts with a /* ... */ block containing the CPAL marker,
    return (remaining_content_after_block, True). Otherwise (content, False)."""
    stripped = content.lstrip("\ufeff").lstrip()
    if not stripped.startswith("/*"):
        return content, False
    end = stripped.find("*/")
    if end == -1:
        return content, False
    block = stripped[: end + 2]
    if "Common Public Attribution License" in block:
        # keep only what follows the block comment
        rest = stripped[end + 2:]
        return rest, True
    return content, False

def normalize(content, template):
    has_mit = "Copyright (c) 2026 microProject" in content
    if has_mit:
        return content, False  # already normalized
    # Strip a leading CPAL block if present.
    rest, stripped = strip_cpal_block(content)
    new_content = template + rest.lstrip("\n")
    return new_content, True

def main():
    dry = "--dry-run" in sys.argv
    template = load_template()
    files = find_java_files()
    changed = 0
    for path in files:
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
        new_content, did_change = normalize(content, template)
        if did_change:
            changed += 1
            if not dry:
                with open(path, "w", encoding="utf-8", newline="") as f:
                    f.write(new_content)
            print(("WOULD CHANGE" if dry else "CHANGED") + " " + os.path.relpath(path, ROOT))
    print(f"\n{'[dry-run] ' if dry else ''}{changed} file(s) normalized out of {len(files)} scanned.")

if __name__ == "__main__":
    main()
