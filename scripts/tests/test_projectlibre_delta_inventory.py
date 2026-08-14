import csv
import subprocess
import tempfile
import unittest
from pathlib import Path

import sys


REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "scripts"))

import projectlibre_delta_inventory as inventory
from projectlibre_review_groups import build_groups


class DeltaInventoryTest(unittest.TestCase):
	def test_review_groups_collapse_resource_keys_and_skip_verified(self):
		rows = [
			{"item_id": "A", "kind": "RESOURCE_KEY", "module": "core", "current_path": "src/a.properties", "baseline_path": "", "initial_path": "", "openproj_path": "", "work_status": "NOT_STARTED"},
			{"item_id": "B", "kind": "RESOURCE_KEY", "module": "core", "current_path": "src/a.properties", "baseline_path": "", "initial_path": "", "openproj_path": "", "work_status": "NOT_STARTED"},
			{"item_id": "C", "kind": "RESOURCE_KEY", "module": "core", "current_path": "src/b.properties", "baseline_path": "", "initial_path": "", "openproj_path": "", "work_status": "VERIFIED"},
		]
		groups = build_groups(rows)
		self.assertEqual(len(groups), 1)
		self.assertEqual(groups[0]["item_count"], "2")
		self.assertEqual(groups[0]["priority"], "LOW")
	def test_paths_from_all_snapshots_share_one_identity(self):
		paths = (
			"openproj_core/src/com/projity/example/Thing.java",
			"projectlibre_core/src/com/projectlibre1/example/Thing.java",
			"modules/projectlibre_core/src/main/java/com/projectlibre1/example/Thing.java",
		)
		normalized = [inventory.normalize_path(path) for path in paths]
		self.assertEqual(normalized[0], normalized[1])
		self.assertEqual(normalized[1], normalized[2])

	def test_properties_parser_handles_continuation_and_comments(self):
		lines = inventory.logical_property_lines("# comment\nkey = first\\\n second\n// ignored\nother:value\n")
		entries = [inventory.split_property(line) for line in lines]
		self.assertIn(("key", "firstsecond"), entries)
		self.assertIn(("other", "value"), entries)

	def test_java_extractor_distinguishes_overloads_and_initializers(self):
		with tempfile.TemporaryDirectory() as temporary:
			root = Path(temporary)
			source = root / "com" / "projectlibre1" / "Sample.java"
			source.parent.mkdir(parents=True)
			source.write_text(
				"package com.projectlibre1; class Sample { "
				"static { System.out.println(1); } "
				"Sample() {} void run() {} void run(String value) {} }",
				encoding="utf-8",
			)
			output = subprocess.check_output(
				["java", str(inventory.JAVA_EXTRACTOR), str(root)],
				cwd=REPO_ROOT,
				text=True,
				encoding="utf-8",
			)
			rows = list(csv.DictReader(output.splitlines(), delimiter="\t"))
			identities = {row["canonical_symbol"] for row in rows}
			self.assertIn("com.projity.Sample#<init>()", identities)
			self.assertIn("com.projity.Sample#run()", identities)
			self.assertIn("com.projity.Sample#run(String)", identities)
			self.assertIn("com.projity.Sample#<clinit>[1]", identities)

	def test_xml_is_indexed_by_element_path_without_file_level_template(self):
		with tempfile.TemporaryDirectory() as temporary:
			root = Path(temporary) / "modules" / "projectlibre_reports" / "src" / "main" / "resources"
			path = root / "com" / "projectlibre1" / "reports" / "definition" / "sample.jrxml"
			path.parent.mkdir(parents=True)
			path.write_text(
				"<jasperReport><field name=\"Task\"/><field name=\"Cost\"/></jasperReport>",
				encoding="utf-8",
			)
			items = inventory.parse_xml_nodes(Path(temporary))
			identities = set(items)
			self.assertTrue(any("XML_NODE|src/" in identity for identity in identities))
			self.assertIn(
				"XML_NODE|src/com/projity/reports/definition/sample.jrxml::/jasperReport[1]/field[2]",
				identities,
			)
			self.assertNotIn("TEMPLATE|reports/src/com/projity/reports/definition/sample.jrxml", identities)


if __name__ == "__main__":
	unittest.main()
