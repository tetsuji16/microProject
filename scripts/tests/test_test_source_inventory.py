import tempfile
import unittest
from pathlib import Path

import sys

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "scripts"))

import test_source_inventory as inventory


class TestSourceInventoryTest(unittest.TestCase):
	def test_classifies_test_and_support_files_by_source_set(self):
		with tempfile.TemporaryDirectory() as temporary:
			root = Path(temporary)
			unit = root / "modules" / "core" / "src" / "test" / "java" / "p" / "DomainTest.java"
			gui = root / "modules" / "ui" / "src" / "guiTest" / "java" / "p" / "Journey.java"
			unit.parent.mkdir(parents=True)
			gui.parent.mkdir(parents=True)
			unit.write_text("package p; class DomainTest { @Test void invariant() {} }", encoding="utf-8")
			gui.write_text("package p; class Journey { void helper() {} }", encoding="utf-8")

			rows = inventory.test_sources(root)

			self.assertEqual(
				[(row["source_set"], row["classification"]) for row in rows],
				[("test", "test"), ("guiTest", "support-or-manual")],
			)

	def test_finds_missing_paths_and_duplicate_numbered_dynamic_names(self):
		with tempfile.TemporaryDirectory() as temporary:
			root = Path(temporary)
			for name in ("FirstTest", "SecondTest"):
				path = root / "modules" / "core" / "src" / "test" / "java" / "p" / f"{name}.java"
				path.parent.mkdir(parents=True, exist_ok=True)
				path.write_text(
					'package p; class ' + name + ' { '
					'@TestFactory Stream<DynamicTest> cases() { '
					'DynamicTest.dynamicTest(id("CASE", 2), () -> {}); } '
					'private static String id(String group, int number) { return "SUITE-" + group; } }',
					encoding="utf-8",
				)
			commented = root / "modules" / "core" / "src" / "test" / "java" / "p" / "Commented.java"
			commented.write_text(
				'package p; class Commented { '
				'// @Test DynamicTest.dynamicTest("duplicate-id", () -> {});\n'
				'String sample = "DynamicTest.dynamicTest(\\\"duplicate-id\\\", () -> {})"; '
				+ 'String documentation = """\n'
				+ '@TestFactory class Fake { DynamicTest.dynamicTest("fake-textblock", () -> {});\n'
				+ '\\""" escaped delimiter remains inside this text block\n'
				+ 'class AlsoFake {}\n"""; '
				+ '@TestFactory Stream<DynamicTest> actual() { '
				+ 'return Stream.of(DynamicTest.dynamicTest("real-id", () -> {})); } }',
				encoding="utf-8",
			)
			(root / "TEST_PLAN.md").write_text("Fixture `fixtures/missing.mpp`", encoding="utf-8")

			self.assertEqual(inventory.missing_documented_paths(root), ["fixtures/missing.mpp"])
			self.assertEqual(
				inventory.dynamic_id_audit(root)[0],
				{"SUITE-CASE-02": [
					"modules/core/src/test/java/p/FirstTest.java",
					"modules/core/src/test/java/p/SecondTest.java",
				]},
			)
			self.assertEqual(
				inventory.dynamic_id_audit(root)[1],
				[],
			)
			commented_rows = [
				row for row in inventory.test_sources(root)
				if row["path"].endswith("Commented.java")
			]
			self.assertEqual(commented_rows[0]["class_names"], ["Commented"])
			self.assertEqual(commented_rows[0]["junit_annotations"], 1)


if __name__ == "__main__":
	unittest.main()
