#!/usr/bin/env python3
"""Enumerate ProjectLibre changes relative to the OpenProj 1.4 source.

The output is a conservative work ledger, not an automatic rewrite list.  Java
symbols are parsed with the JDK compiler API so overloaded methods and nested
types receive stable identities.  Resource bundles are compared by property key.
Every changed item starts as REVIEW unless a completed deletion is explicitly
recorded below with verification evidence.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import re
import shutil
import subprocess
import tarfile
import tempfile
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
OPENPROJ_ARCHIVE = REPO_ROOT / "docs" / "legal" / "openproj-1.4-src.tar.gz"
OPENPROJ_ARCHIVE_SHA256 = "20071b090d841388860049ce49724e2773b8cec250d76e74264c71adf2a79ac6"
INITIAL_COMMIT = "d2fa3c20a"
PROJECTLIBRE_BASELINE = "0530be227f4a10c5545cce8d3db20ac5a4d76a66"
JAVA_EXTRACTOR = REPO_ROOT / "scripts" / "audit" / "JavaSymbolExtractor.java"

MODULE_NAMES = {
    "openproj_build": "packaging",
    "openproj_contrib": "projectlibre_contrib",
    "openproj_core": "projectlibre_core",
    "openproj_exchange": "projectlibre_exchange",
    "openproj_reports": "projectlibre_reports",
    "openproj_ui": "projectlibre_ui",
    "projectlibre_build": "packaging",
    "projectlibre_contrib": "projectlibre_contrib",
    "projectlibre_core": "projectlibre_core",
    "projectlibre_exchange": "projectlibre_exchange",
    "projectlibre_reports": "projectlibre_reports",
    "projectlibre_ui": "projectlibre_ui",
    "projectlibre_application": "projectlibre_application",
    "packaging": "packaging",
}

KNOWN_VERIFIED_DELETIONS = (
    "org.projity.util.UpdateChecker",
    "org.projity.util.UpdateCheckerFormula",
    "com.projity.dialog.DonateDialog",
    "com.projity.dialog.UserInfoDialog",
    "com.projity.main.EclipseMain",
    "com.projity.pm.graphic.frames.TestFrame",
)

KNOWN_VERIFIED_SYMBOLS = {
    "com.projity.main.Main#getProjectLibreRunNumber()",
    "com.projity.main.Main#getProjectLibreFirstRun()",
    "com.projity.main.Main#getRunSinceMessage()",
}

KNOWN_VERIFIED_RESOURCE_KEYS = {
	"DonateDialog.show",
	"DonateDialog.showEvery",
	"HelpDialog.GoToCloudTrial",
	"HelpDialog.RegisterToOnlineHelp",
	"HelpDialog.RegisterToOnlineHelpMessage",
	"Text.newVersion",
	"Text.donateMessage",
	"Text.donateTitle",
	"Text.runsSinceMessage",
	"paypal.donate",
}

KNOWN_VERIFIED_XML_PREFIXES = (
    "resources/projectlibre.xml::",
)

KNOWN_VERIFIED_ASSET_PATHS = {
    "src/com/projity/pm/graphic/images/projectlibre-application.png",
    "src/com/projity/pm/graphic/images/projectlibre-logo-whitebg.png",
    "src/com/projity/pm/graphic/images/projectlibre-logo.png",
    "src/com/projity/pm/graphic/images/projityFormat.gif",
    "src/com/projity/pm/graphic/images/ribbon/projectlibre-wordmark.svg",
    "windows/icons/projectlibre.ico",
    # OpenProj assets removed from the current distribution; they are not
    # ProjectLibre additions and require no replacement.
    "src/com/projity/pm/graphic/images/openproj_logo.png",
    "src/com/projity/pm/graphic/images/serena-logo.gif",
    "src/com/projity/pm/graphic/images/OpenProj_big.jpg",
    "src/com/projity/pm/graphic/images/openproj-new.png",
    "src/com/projity/pm/graphic/images/openproj.gif",
    "license/index_html_0.gif",
    "resources/fx/package/windows/ProjectLibre-setup-icon.bmp",
    "resources/mac/jpackage/background.png",
    "resources/openproj.png",
    "resources/projectlibre.png",
    "resources/wix/msi_images/projectlibre.ico",
    "resources/wix/msi_images/projectlibre_msi_banner.bmp",
    "resources/wix/msi_images/projectlibre_msi_splash.bmp",
    "src/com/projity/pm/graphic/images/projity.png",
}

THIRD_PARTY_SYMBOL_PREFIXES = (
    "net.sf.mpxj.",
    "org.apache.batik.",
    "org.pushingpixels.flamingo.",
)

FIELDS = (
    "item_id",
    "kind",
    "module",
    "symbol_or_key",
    "canonical_identity",
    "delta_kind",
    "change_stage",
    "openproj_path",
    "openproj_lines",
    "initial_path",
    "baseline_path",
    "baseline_lines",
    "current_path",
    "current_lines",
    "current_references",
    "expected_behavior",
    "disposition",
    "work_status",
    "introduced_or_changed_by",
    "evidence",
    "verification",
    "reviewer",
)


@dataclass(frozen=True)
class Item:
    module: str
    kind: str
    symbol: str
    canonical: str
    path: str
    start_line: int
    end_line: int
    content_sha256: str

    @property
    def identity(self) -> str:
        # Fully qualified Java symbols and normalized resource paths remain
        # unique when code moves between modules.  Keeping the module out of
        # the identity prevents a move from becoming a false add/delete pair.
        return f"{self.kind}|{self.canonical}"


def run(*args: str, cwd: Path = REPO_ROOT) -> str:
    return subprocess.check_output(args, cwd=cwd, text=True, encoding="utf-8", errors="replace").replace("\r\n", "\n")


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def safe_target(root: Path, member_name: str) -> Path:
    target = (root / member_name).resolve()
    if root.resolve() not in target.parents and target != root.resolve():
        raise ValueError(f"archive member escapes extraction root: {member_name}")
    return target


def extract_archive(archive: Path, destination: Path) -> Path:
    with tarfile.open(archive, "r:gz") as source:
        for member in source:
            if not member.isfile():
                continue
            target = safe_target(destination, member.name)
            target.parent.mkdir(parents=True, exist_ok=True)
            extracted = source.extractfile(member)
            if extracted is None:
                continue
            with target.open("wb") as output:
                output.write(extracted.read())
    return destination / "openproj-1.4-src"


def git_top_level(ref: str) -> list[str]:
    lines = run("git", "ls-tree", "-d", "--name-only", ref).splitlines()
    return [line for line in lines if line.startswith(("openproj_", "projectlibre_")) or line == "packaging"]


def extract_git_tree(ref: str, destination: Path) -> Path:
    prefixes = git_top_level(ref)
    command = ["git", "archive", "--format=tar", ref, *prefixes]
    archive = subprocess.check_output(command, cwd=REPO_ROOT)
    with tarfile.open(fileobj=io.BytesIO(archive), mode="r:") as source:
        for member in source:
            if not member.isfile():
                continue
            target = safe_target(destination, member.name)
            target.parent.mkdir(parents=True, exist_ok=True)
            extracted = source.extractfile(member)
            if extracted is None:
                continue
            with target.open("wb") as output:
                output.write(extracted.read())
    return destination


def normalize_java_sources(root: Path) -> None:
    """Make legacy-encoded parser inputs readable without touching repository files."""
    for path in root.rglob("*.java"):
        data = path.read_bytes()
        try:
            data.decode("utf-8")
        except UnicodeDecodeError:
            path.write_text(data.decode("utf-8", errors="replace"), encoding="utf-8")


def copy_current_sources(destination: Path) -> Path:
    tracked = run("git", "ls-files", "modules", "packaging").splitlines()
    for relative_text in tracked:
        source = REPO_ROOT / relative_text
        if not source.is_file():
            continue
        relative = Path(relative_text)
        target = destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
    normalize_java_sources(destination)
    return destination


def asset_kind(path: Path) -> str:
    suffix = path.suffix.lower()
    if suffix in {".xml", ".jrxml"}:
        return "XML_NODE"
    if suffix in {".png", ".gif", ".jpg", ".jpeg", ".svg", ".ico", ".bmp"}:
        return "ASSET"
    if suffix in {".html", ".htm", ".json", ".yaml", ".yml", ".conf", ".properties"}:
        return "PACKAGING" if "packaging" in path.parts or path.name.endswith(".conf") else "HUNK"
    return "HUNK"


def parse_assets(root: Path) -> dict[str, Item]:
    result: dict[str, Item] = {}
    for path in sorted(root.rglob("*")):
        if not path.is_file() or path.suffix.lower() in {".java", ".properties", ".xml", ".jrxml"}:
            continue
        normalized = normalize_path(path.relative_to(root).as_posix())
        if normalized is None:
            continue
        module, logical_path = normalized
        logical_path = logical_path.removeprefix(f"{module}/")
        kind = asset_kind(path)
        data = path.read_bytes()
        canonical = logical_path
        item = Item(
            module=module,
            kind=kind,
            symbol=path.relative_to(root).as_posix(),
            canonical=canonical,
            path=path.relative_to(root).as_posix(),
            start_line=1,
            end_line=max(1, data.count(b"\n") + 1),
            content_sha256=hashlib.sha256(data).hexdigest(),
        )
        result[item.identity] = item
    return result


def local_tag(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def canonical_xml_element(element: ET.Element) -> str:
    attrs = " ".join(f"{local_tag(key)}={value!r}" for key, value in sorted(element.attrib.items()))
    text = " ".join((element.text or "").split())
    return f"<{local_tag(element.tag)} {attrs}>{text}</{local_tag(element.tag)}>"


def parse_xml_nodes(root: Path) -> dict[str, Item]:
    result: dict[str, Item] = {}
    for path in sorted(root.rglob("*")):
        if not path.is_file() or path.suffix.lower() not in {".xml", ".jrxml"}:
            continue
        relative = path.relative_to(root).as_posix()
        normalized = normalize_path(relative)
        if normalized is None:
            continue
        module, logical_path = normalized
        logical_path = logical_path.removeprefix(f"{module}/")
        try:
            tree = ET.parse(path)
        except (ET.ParseError, UnicodeDecodeError, OSError):
            data = path.read_bytes()
            item = Item(
                module=module,
                kind="XML_NODE",
                symbol=relative + "::<parse-error>",
                canonical=f"{logical_path}|<parse-error>",
                path=relative,
                start_line=1,
                end_line=max(1, data.count(b"\n") + 1),
                content_sha256=hashlib.sha256(data).hexdigest(),
            )
            result[item.identity] = item
            continue
        lines = path.read_text(encoding="utf-8", errors="replace").splitlines()

        def visit(element: ET.Element, xpath: str) -> None:
            canonical = f"{logical_path}::{xpath}"
            serialized = canonical_xml_element(element)
            item = Item(
                module=module,
                kind="XML_NODE",
                symbol=f"{relative}::{xpath}",
                canonical=canonical,
                path=relative,
                start_line=1,
                end_line=max(1, len(lines)),
                content_sha256=hashlib.sha256(serialized.encode("utf-8")).hexdigest(),
            )
            result[item.identity] = item
            sibling_ordinals: dict[str, int] = {}
            for child in list(element):
                name = local_tag(child.tag)
                sibling_ordinals[name] = sibling_ordinals.get(name, 0) + 1
                visit(child, f"{xpath}/{name}[{sibling_ordinals[name]}]")

        visit(tree.getroot(), f"/{local_tag(tree.getroot().tag)}[1]")
    return result


def normalize_path(path: str) -> tuple[str, str] | None:
    value = path.replace("\\", "/").lstrip("./")
    if value.startswith("modules/"):
        value = value[len("modules/") :]
    first, separator, rest = value.partition("/")
    if not separator or first not in MODULE_NAMES:
        return None
    module = MODULE_NAMES[first]
    rest = re.sub(r"^(src/main/(java|resources)|src/test/(java|resources)|src|test)/", "src/", rest)
    rest = rest.replace("com/projectlibre1", "com/projity").replace("org/projectlibre1", "org/projity")
    return module, f"{module}/{rest}"


def parse_java_symbols(root: Path) -> dict[str, Item]:
    output = run("java", str(JAVA_EXTRACTOR), str(root))
    rows = csv.DictReader(output.splitlines(), delimiter="\t")
    result: dict[str, Item] = {}
    duplicates: list[str] = []
    for row in rows:
        normalized = normalize_path(row["path"])
        if normalized is None:
            continue
        module, _ = normalized
        item = Item(
            module=module,
            kind=row["kind"],
            symbol=row["symbol"],
            canonical=row["canonical_symbol"],
            path=row["path"],
            start_line=int(row["start_line"]),
            end_line=int(row["end_line"]),
            content_sha256=row["content_sha256"],
        )
        if item.identity in result:
            duplicates.append(item.identity)
        result[item.identity] = item
    if duplicates:
        sample = ", ".join(sorted(set(duplicates))[:10])
        raise ValueError(f"duplicate Java symbol identities ({len(duplicates)}): {sample}")
    return result


def logical_property_lines(text: str) -> list[str]:
    result: list[str] = []
    pending = ""
    for physical in text.replace("\r\n", "\n").replace("\r", "\n").split("\n"):
        line = pending + physical.lstrip() if pending else physical
        slash_count = len(line) - len(line.rstrip("\\"))
        if slash_count % 2:
            pending = line[:-1]
            continue
        result.append(line)
        pending = ""
    if pending:
        result.append(pending)
    return result


def split_property(line: str) -> tuple[str, str] | None:
    stripped = line.lstrip()
    if not stripped or stripped.startswith(("#", "!", "//")):
        return None
    escaped = False
    split_at = len(stripped)
    for index, character in enumerate(stripped):
        if escaped:
            escaped = False
            continue
        if character == "\\":
            escaped = True
            continue
        if character in "=:" or character.isspace():
            split_at = index
            break
    key = stripped[:split_at]
    remainder = stripped[split_at:].lstrip()
    if remainder.startswith(("=", ":")):
        remainder = remainder[1:].lstrip()
    return key, remainder


def canonical_value(value: str) -> str:
    return re.sub(r"\s+", " ", value.replace("com.projectlibre1", "com.projity").replace("org.projectlibre1", "org.projity")).strip()


def parse_properties(root: Path) -> dict[str, Item]:
    result: dict[str, Item] = {}
    for path in sorted(root.rglob("*.properties")):
        relative = path.relative_to(root).as_posix()
        normalized = normalize_path(relative)
        if normalized is None:
            continue
        module, logical_path = normalized
        text = path.read_text(encoding="utf-8", errors="replace")
        for line_number, line in enumerate(logical_property_lines(text), 1):
            entry = split_property(line)
            if entry is None:
                continue
            key, value = entry
            path_without_module = logical_path.removeprefix(f"{module}/")
            canonical = f"{path_without_module}::{key}"
            item = Item(
                module=module,
                kind="RESOURCE_KEY",
                symbol=f"{relative}::{key}",
                canonical=canonical,
                path=relative,
                start_line=line_number,
                end_line=line_number,
                content_sha256=hashlib.sha256(canonical_value(value).encode("utf-8")).hexdigest(),
            )
            result[item.identity] = item
    return result


def snapshot(root: Path) -> dict[str, Item]:
    result = parse_java_symbols(root)
    for identity, item in parse_properties(root).items():
        if identity in result:
            raise ValueError(f"duplicate cross-kind identity: {identity}")
        result[identity] = item
    for identity, item in parse_assets(root).items():
        if identity in result:
            raise ValueError(f"duplicate asset identity: {identity}")
        result[identity] = item
    for identity, item in parse_xml_nodes(root).items():
        if identity in result:
            raise ValueError(f"duplicate XML node identity: {identity}")
        result[identity] = item
    return result


def named_snapshot(name: str, root: Path) -> tuple[str, dict[str, Item]]:
    print(f"parsing {name}: {root}", flush=True)
    result = snapshot(root)
    print(f"parsed {name}: {len(result):,} symbols/keys", flush=True)
    return name, result


def change_stage(openproj: Item | None, initial: Item | None, baseline: Item | None) -> str:
    def same(left: Item | None, right: Item | None) -> bool:
        return left is not None and right is not None and left.content_sha256 == right.content_sha256

    if same(openproj, initial) and not same(initial, baseline):
        return "INITIAL_TO_1_9_8"
    if not same(openproj, initial) and same(initial, baseline):
        return "OPENPROJ_TO_INITIAL"
    if not same(openproj, initial) and not same(initial, baseline):
        return "MULTI_STAGE"
    return "REVIEW_REQUIRED"


def line_range(item: Item | None) -> str:
    if item is None:
        return ""
    return str(item.start_line) if item.start_line == item.end_line else f"{item.start_line}-{item.end_line}"


def stable_id(identity: str) -> str:
    return "PLD-" + hashlib.sha256(identity.encode("utf-8")).hexdigest()[:16].upper()


def is_known_verified(item: Item, current: Item | None) -> bool:
    if current is not None:
        return False
    if item.canonical in KNOWN_VERIFIED_SYMBOLS:
        return True
    if item.kind == "RESOURCE_KEY":
        key = item.canonical.rsplit("::", 1)[-1]
        if key in KNOWN_VERIFIED_RESOURCE_KEYS:
            return True
    if item.kind == "XML_NODE" and any(item.canonical.startswith(prefix) for prefix in KNOWN_VERIFIED_XML_PREFIXES):
        return True
    if item.kind == "ASSET" and item.canonical in KNOWN_VERIFIED_ASSET_PATHS:
        return True
    return any(item.canonical == prefix or item.canonical.startswith(prefix + "#") for prefix in KNOWN_VERIFIED_DELETIONS)


def lexical_reference_counts(current_root: Path) -> Counter[str]:
    counts: Counter[str] = Counter()
    for path in current_root.rglob("*.java"):
        text = path.read_text(encoding="utf-8", errors="replace")
        counts.update(re.findall(r"[A-Za-z_$][A-Za-z0-9_$]*", text))
    return counts


def simple_name(item: Item) -> str:
    if item.kind == "RESOURCE_KEY":
        return item.canonical.rsplit("::", 1)[-1]
    tail = item.canonical.rsplit("#", 1)[-1]
    if tail.startswith("<init>"):
        return item.canonical.split("#", 1)[0].rsplit(".", 1)[-1].split("$")[-1]
    return re.split(r"[@(]", tail, 1)[0]


def build_rows(
    openproj: dict[str, Item],
    initial: dict[str, Item],
    baseline: dict[str, Item],
    current: dict[str, Item],
    reference_counts: Counter[str],
) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for identity in sorted(set(openproj) | set(baseline)):
        original = openproj.get(identity)
        projectlibre = baseline.get(identity)
        if original is not None and projectlibre is not None and original.content_sha256 == projectlibre.content_sha256:
            continue
        source = projectlibre or original
        assert source is not None
        if source.kind != "RESOURCE_KEY" and any(source.canonical.startswith(prefix) for prefix in THIRD_PARTY_SYMBOL_PREFIXES):
            continue
        current_item = current.get(identity)
        initial_item = initial.get(identity)
        if original is None:
            delta_kind = "ADDED"
        elif projectlibre is None:
            delta_kind = "REMOVED"
        else:
            delta_kind = "MODIFIED"
        verified = is_known_verified(source, current_item)
        disposition = "DELETE_PROJECTLIBRE_DELTA" if verified else "REVIEW"
        work_status = "VERIFIED" if verified else "NOT_STARTED"
        expected = "unused; deletion verified" if verified else ("absent from current; verify deletion or replacement" if current_item is None else "REVIEW_REQUIRED")
        current_references = "0" if current_item is None else str(max(0, reference_counts[simple_name(current_item)] - 1))
        evidence_parts = [f"OpenProj 1.4 vs ProjectLibre 1.9.8: {delta_kind.lower()}"]
        if current_item is None:
            evidence_parts.append("symbol/key absent from working tree")
        verification = ""
        if verified:
            verification = "build installDist; focused UI audit; independent-boundary check; git diff --check"
        rows.append(
            {
                "item_id": stable_id(identity),
                "kind": source.kind,
                "module": source.module,
                "symbol_or_key": (current_item or projectlibre or original).symbol,
                "canonical_identity": identity,
                "delta_kind": delta_kind,
                "change_stage": change_stage(original, initial_item, projectlibre),
                "openproj_path": original.path if original else "",
                "openproj_lines": line_range(original),
                "initial_path": initial_item.path if initial_item else "",
                "baseline_path": projectlibre.path if projectlibre else "",
                "baseline_lines": line_range(projectlibre),
                "current_path": current_item.path if current_item else "",
                "current_lines": line_range(current_item),
                "current_references": current_references,
                "expected_behavior": expected,
                "disposition": disposition,
                "work_status": work_status,
                "introduced_or_changed_by": change_stage(original, initial_item, projectlibre),
                "evidence": "; ".join(evidence_parts),
                "verification": verification,
                "reviewer": "",
            }
        )
    return rows


def write_outputs(rows: list[dict[str, str]], output: Path, summary: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)

    by_kind = Counter(row["kind"] for row in rows)
    by_module = Counter(row["module"] for row in rows)
    by_status = Counter(row["work_status"] for row in rows)
    by_delta = Counter(row["delta_kind"] for row in rows)
    lines = [
        "# ProjectLibre delta inventory",
        "",
        f"- OpenProj archive SHA-256: `{OPENPROJ_ARCHIVE_SHA256}`",
        f"- ProjectLibre initial commit: `{INITIAL_COMMIT}`",
        f"- ProjectLibre comparison baseline: `{PROJECTLIBRE_BASELINE}`",
        f"- Total candidate items: **{len(rows):,}**",
        f"- Verified items: **{by_status['VERIFIED']:,}**",
        f"- Remaining items: **{len(rows) - by_status['VERIFIED']:,}**",
        "- A candidate is a syntactic difference, not an automatic rewrite decision.",
        "",
        "## Work status",
        "",
        "| Status | Count |",
        "|---|---:|",
    ]
    lines.extend(f"| {name} | {count:,} |" for name, count in sorted(by_status.items()))
    lines += ["", "## Delta kind", "", "| Delta | Count |", "|---|---:|"]
    lines.extend(f"| {name} | {count:,} |" for name, count in sorted(by_delta.items()))
    lines += ["", "## Item kind", "", "| Kind | Count |", "|---|---:|"]
    lines.extend(f"| {name} | {count:,} |" for name, count in sorted(by_kind.items()))
    lines += ["", "## Module", "", "| Module | Count |", "|---|---:|"]
    lines.extend(f"| {name} | {count:,} |" for name, count in sorted(by_module.items()))
    summary.write_text("\n".join(lines) + "\n", encoding="utf-8")


def validate(rows: list[dict[str, str]]) -> None:
    ids = [row["item_id"] for row in rows]
    if len(ids) != len(set(ids)):
        raise ValueError("duplicate item_id values")
    required = ("item_id", "kind", "module", "symbol_or_key", "canonical_identity", "delta_kind", "disposition", "work_status", "evidence")
    for index, row in enumerate(rows, 2):
        missing = [field for field in required if not row[field]]
        if missing:
            raise ValueError(f"row {index} is missing required fields: {', '.join(missing)}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=REPO_ROOT / "docs" / "audit" / "projectlibre-delta-items.csv")
    parser.add_argument("--summary", type=Path, default=REPO_ROOT / "docs" / "audit" / "projectlibre-delta-summary.md")
    args = parser.parse_args()

    actual_hash = file_sha256(OPENPROJ_ARCHIVE)
    if actual_hash != OPENPROJ_ARCHIVE_SHA256:
        raise SystemExit(f"OpenProj archive SHA-256 mismatch: expected {OPENPROJ_ARCHIVE_SHA256}, got {actual_hash}")

    with tempfile.TemporaryDirectory(prefix="microproject-delta-") as temporary:
        root = Path(temporary)
        openproj_root = extract_archive(OPENPROJ_ARCHIVE, root / "openproj")
        initial_root = extract_git_tree(INITIAL_COMMIT, root / "initial")
        baseline_root = extract_git_tree(PROJECTLIBRE_BASELINE, root / "baseline")
        current_root = copy_current_sources(root / "current")
        normalize_java_sources(openproj_root)
        normalize_java_sources(initial_root)
        normalize_java_sources(baseline_root)
        roots = {
            "openproj": openproj_root,
            "initial": initial_root,
            "baseline": baseline_root,
            "current": current_root,
        }
        with ThreadPoolExecutor(max_workers=len(roots)) as executor:
            snapshots = dict(executor.map(lambda entry: named_snapshot(*entry), roots.items()))
        openproj = snapshots["openproj"]
        initial = snapshots["initial"]
        baseline = snapshots["baseline"]
        current = snapshots["current"]
        references = lexical_reference_counts(REPO_ROOT / "modules")
        rows = build_rows(openproj, initial, baseline, current, references)
        validate(rows)
        write_outputs(rows, args.output, args.summary)

    print(f"wrote {len(rows)} candidate items to {args.output}")
    print(f"wrote summary to {args.summary}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
