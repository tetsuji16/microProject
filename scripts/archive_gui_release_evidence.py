#!/usr/bin/env python3
"""Build a deterministic, narrowly curated GUI evidence archive."""

from __future__ import annotations

import hashlib
import json
import re
import zipfile
from pathlib import Path


EVIDENCE_FILES = (
    "ribbon-tab-1.png",  # Task tab: representative main ribbon state.
    "ribbon-tab-5.png",  # View tab: a second main ribbon state.
    "ribbon-file-backstage-open.png",
    "ribbon-tabs-narrow-320.png",
    "ribbon-tabs-overflow-focused-320.png",
    "ribbon-tabs-overflow-popup-320.png",
)
GUI_LEGS = (
    "functional-full-ja-100",
    "visual-ja-1.25",
    "visual-ja-1.5",
    "visual-en-1.0",
    "visual-en-1.25",
    "visual-en-1.5",
)
LEG_DIRECTORY = re.compile(
    r"^(?P<label>functional-full-ja-100|visual-(?:ja|en)-(?:1\.25|1\.5|1\.0))-(?P<invocation>[0-9a-f]{32})$"
)


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _find_leg(root: Path, label: str) -> Path:
    candidates = []
    for child in root.rglob("*"):
        match = LEG_DIRECTORY.fullmatch(child.name)
        normalized = match.group("label") if match else None
        if child.is_dir() and child.parent.name == "guiTest-artifacts" and normalized == label:
            candidates.append(child)
    if len(candidates) != 1:
        raise ValueError(f"Expected exactly one artifact directory for {label}; found {len(candidates)}")
    return candidates[0]


def build_archive(source_root: Path, archive_path: Path, manifest: dict) -> dict:
    """Package only source-listed images, preserving all six GUI matrix-leg paths."""
    source_root = source_root.resolve(strict=True)
    archive_path.parent.mkdir(parents=True, exist_ok=True)
    staged: list[tuple[str, Path]] = []
    image_records = []

    for label in GUI_LEGS:
        leg = _find_leg(source_root, label)
        for name in EVIDENCE_FILES:
            candidates = [path for path in leg.rglob(name) if path.is_file()]
            if len(candidates) != 1:
                raise ValueError(f"Expected exactly one {name} in {label}; found {len(candidates)}")
            source = candidates[0].resolve(strict=True)
            if not source.is_relative_to(source_root):
                raise ValueError(f"Evidence path escapes extracted artifact root: {source}")
            relative = f"{label}/{name}"
            staged.append((relative, source))
            image_records.append({
                "archive_path": relative,
                "sha256": _sha256(source),
                "bytes": source.stat().st_size,
                "source_artifact_path": source.relative_to(source_root).as_posix(),
            })

    output_manifest = dict(manifest)
    output_manifest["curated_files"] = image_records
    output_manifest["bounds_evidence"] = (
        "The source GUI test asserts ribbon/tab bounds, but the historical run artifact does not serialize numeric bounds. "
        "This archive contains the screenshots only; numeric bounds are not claimed."
    )
    manifest_bytes = (json.dumps(output_manifest, indent=2, sort_keys=True) + "\n").encode("utf-8")

    with zipfile.ZipFile(archive_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, source in staged:
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, source.read_bytes())
        info = zipfile.ZipInfo("gui-evidence-provenance.json", date_time=(1980, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o100644 << 16
        archive.writestr(info, manifest_bytes)
    return output_manifest


def main() -> None:
    import argparse

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True, help="Extracted Actions artifact directory")
    parser.add_argument("--manifest", type=Path, required=True, help="JSON file with verified run/release provenance")
    parser.add_argument("--output", type=Path, required=True, help="Destination ZIP archive")
    args = parser.parse_args()
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    build_archive(args.source_root, args.output, manifest)


if __name__ == "__main__":
    main()
