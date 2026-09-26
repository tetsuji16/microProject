import unittest

from scripts.license_audit import old_candidates, strip_header
from scripts.audit.openproj_java25_inventory import current_path


class LicenseAuditMappingTest(unittest.TestCase):
	def test_microproject_source_path_maps_to_openproj_package(self):
		candidates = old_candidates(
			"modules/microproject_core/src/main/java/com/microproject/pm/task/Task.java"
		)
		self.assertIn("openproj_core/src/com/projity/pm/task/Task.java", candidates)

	def test_microproject_package_normalization_matches_legacy_package(self):
		self.assertEqual(
			strip_header("package com.microproject.pm; class Task {}"),
			strip_header("package com.projity.pm; class Task {}"),
		)

	def test_inventory_keeps_current_microproject_path(self):
		path = "modules/microproject_core/src/main/java/com/microproject/pm/task/Task.java"
		self.assertEqual(current_path(path), path)


if __name__ == "__main__":
	unittest.main()
