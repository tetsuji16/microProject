import json
import struct
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO_ROOT / "scripts"))

import archive_gui_release_evidence as evidence


class ArchiveGuiReleaseEvidenceTest(unittest.TestCase):
	def test_source_log_validation_accepts_the_actual_six_ci_marker_labels(self):
		historical_markers = "\n".join((
			"GUI gate elapsed: functional-full-ja-100 00:09:03 (limit 00:45:00)",
			"GUI gate elapsed: visual-ja-1.25 00:02:44 (limit 00:20:00)",
			"GUI gate elapsed: visual-ja-1.5 00:02:49 (limit 00:20:00)",
			"GUI gate elapsed: visual-en-1.0 00:02:55 (limit 00:20:00)",
			"GUI gate elapsed: visual-en-1.25 00:02:47 (limit 00:20:00)",
			"GUI gate elapsed: visual-en-1.5 00:02:52 (limit 00:20:00)",
		))

		evidence.validate_gui_leg_log(historical_markers)
		with self.assertRaisesRegex(ValueError, "visual-ja-1.25"):
			evidence.validate_gui_leg_log(historical_markers.replace("visual-ja-1.25", "visual-ja-1-25"))

	def _create_source(self, root):
		for label in evidence.GUI_LEGS:
			directory_name = label + "-" + ("a" * 32)
			artifact_dir = (root / "modules" / "microproject_ui" / "build" / "reports"
				/ "guiTest-artifacts" / directory_name)
			artifact_dir.mkdir(parents=True)
			for name in evidence.EVIDENCE_FILES:
				png_header = (b"\x89PNG\r\n\x1a\n" + struct.pack(">I", 13) + b"IHDR"
					+ struct.pack(">II", 320, 240))
				(artifact_dir / name).write_bytes(png_header + f"{label}:{name}".encode("ascii"))

	def test_archive_contains_only_allowlisted_images_and_provenance_per_leg(self):
		with tempfile.TemporaryDirectory() as temporary:
			root = Path(temporary)
			source = root / "source"
			source.mkdir()
			self._create_source(source)
			archive_path = root / "evidence.zip"
			manifest = {"release_tag": "v0.0.24.1244", "source_run_id": "38495421214"}

			result = evidence.build_archive(source, archive_path, manifest)

			with zipfile.ZipFile(archive_path) as archive:
				names = set(archive.namelist())
				self.assertEqual(len(evidence.GUI_LEGS) * len(evidence.EVIDENCE_FILES) + 1, len(names))
				self.assertIn("functional-full-ja-100/ribbon-tabs-narrow-320.png", names)
				self.assertIn("visual-ja-1.25/ribbon-contextual-NetworkFormatRibbonTask.png", names)
				self.assertIn("visual-en-1.5/ribbon-contextual-CalendarFormatRibbonTask.png", names)
				self.assertIn("visual-en-1.5/ribbon-file-backstage-open.png", names)
				self.assertIn("gui-evidence-provenance.json", names)
				self.assertTrue(all(name.endswith(".png") or name == "gui-evidence-provenance.json" for name in names))
				archived_manifest = json.loads(archive.read("gui-evidence-provenance.json"))
				self.assertEqual(48, len(archived_manifest["curated_files"]))
				self.assertEqual(2, archived_manifest["evidence_packet_version"])
				self.assertEqual("microProject-gui-evidence-v2-v0.0.24.1244.zip",
					archived_manifest["evidence_asset_name"])
				self.assertTrue(all(item["width_px"] == 320 and item["height_px"] == 240
					for item in archived_manifest["curated_files"]))
				self.assertIn("numeric bounds are not claimed", archived_manifest["bounds_evidence"])
				self.assertEqual(manifest, {key: archived_manifest[key] for key in manifest})
				first = archive.read("functional-full-ja-100/ribbon-tabs-narrow-320.png")
				self.assertTrue(first.endswith(b"functional-full-ja-100:ribbon-tabs-narrow-320.png"))
			self.assertEqual(result["curated_files"][0]["sha256"], archived_manifest["curated_files"][0]["sha256"])

	def test_archive_fails_closed_when_one_visual_leg_image_is_missing(self):
		with tempfile.TemporaryDirectory() as temporary:
			root = Path(temporary)
			source = root / "source"
			source.mkdir()
			self._create_source(source)
			missing = source / "modules" / "microproject_ui" / "build" / "reports" / "guiTest-artifacts" / (
				"visual-en-1.5-" + "a" * 32) / "ribbon-tabs-overflow-popup-320.png"
			missing.unlink()

			with self.assertRaisesRegex(ValueError, "Expected exactly one ribbon-tabs-overflow-popup-320.png in visual-en-1.5"):
				evidence.build_archive(source, root / "evidence.zip", {})

	def test_archive_fails_closed_when_an_allowlisted_image_is_not_a_png(self):
		with tempfile.TemporaryDirectory() as temporary:
			root = Path(temporary)
			source = root / "source"
			source.mkdir()
			self._create_source(source)
			invalid = source / "modules" / "microproject_ui" / "build" / "reports" / "guiTest-artifacts" / (
				"visual-en-1.5-" + "a" * 32) / "ribbon-contextual-CalendarFormatRibbonTask.png"
			invalid.write_bytes(b"not a png")

			with self.assertRaisesRegex(ValueError, "not a PNG with an IHDR header"):
				evidence.build_archive(source, root / "evidence.zip", {})


if __name__ == "__main__":
	unittest.main()
