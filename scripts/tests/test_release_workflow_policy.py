import re
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]


class ReleaseWorkflowPolicyTest(unittest.TestCase):
	@classmethod
	def setUpClass(cls):
		cls.gui = (REPO_ROOT / ".github" / "workflows" / "gui-acceptance.yml").read_text(encoding="utf-8")
		cls.release = (REPO_ROOT / ".github" / "workflows" / "release-and-pages.yml").read_text(encoding="utf-8")

	def test_pull_request_gui_gate_is_named_and_defaults_to_smoke(self):
		self.assertRegex(self.gui, r"(?m)^name: GUI acceptance$")
		self.assertRegex(self.gui, r"(?ms)^  gui-acceptance:\n    name: GUI acceptance\b")
		self.assertRegex(self.gui, r"(?ms)gui_suite:\n.*?default: smoke\n.*?options:\n          - smoke\n          - visual\n          - full")
		self.assertIn("if ($env:GITHUB_EVENT_NAME -eq 'schedule') { $requestedSuite = 'full' }", self.gui)
		self.assertIn("else { $requestedSuite = 'smoke' }", self.gui)
		self.assertNotIn("codex/stable-", self.gui)

	def test_stable_release_has_one_full_gate_and_no_stable_tag_trigger(self):
		self.assertRegex(self.release, r"(?ms)^  push:\n.*?^    tags: \['v\*-beta\.\*'\]")
		self.assertNotRegex(self.release, r"(?m)^    tags: \['v\*'\]")
		self.assertIn("if ($env:GITHUB_EVENT_NAME -eq 'workflow_dispatch' -and '${{ inputs.release_channel }}' -eq 'stable') { $isStableCandidate = $true }", self.release)
		self.assertIn("if ($env:GITHUB_REF_TYPE -eq 'branch' -and $env:GITHUB_REF_NAME -like 'release/stable-*') { $isStableCandidate = $true }", self.release)
		self.assertRegex(self.release, re.escape("if ($isStableCandidate -and $requestedSuite -ne 'full') { throw 'Stable releases require gui_suite=full.' }"))

	def test_unix_packages_run_on_manual_flag_or_stable_branch_push(self):
		condition = re.search(r"(?m)^  unix-packages:\r?\n    if: \$\{\{(.*)\}\}\s*$", self.release).group(1).strip()
		self.assertEqual(
			"(github.event_name == 'workflow_dispatch' && inputs.build_unix_packages) || "
			"(github.event_name == 'push' && github.ref_type == 'branch' && startsWith(github.ref_name, 'release/stable-'))",
			condition,
		)
		self.assertIn("github.event_name == 'workflow_dispatch' && inputs.build_unix_packages", condition)
		self.assertIn("github.ref_type == 'branch' && startsWith(github.ref_name, 'release/stable-')", condition)
		self.assertNotIn("release/beta-", condition)
		self.assertNotIn("github.ref_type == 'tag'", condition)

	def test_visual_gate_keeps_all_classes_and_only_the_reviewed_task_information_methods(self):
		runner = (REPO_ROOT / ".github" / "scripts" / "run-gui-acceptance.ps1").read_text(encoding="utf-8")
		class_block = re.search(r"\$visualClasses = @\((.*?)\n\)", runner, re.S).group(1)
		classes = re.findall(r"'([^']+GuiAcceptanceTest)'", class_block)
		task_info = "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest"
		self.assertEqual(11, len(classes))
		self.assertNotIn(task_info, classes)
		self.assertIn("foreach ($testClass in $visualTests)", runner)
		self.assertIn('$visualTests = @($visualClasses + @($taskInformationVisualMethods | ForEach-Object { "$taskInformationTestClass.$_" }))', runner)
		gui_build = (REPO_ROOT / "modules" / "microproject_ui" / "build.gradle.kts").read_text(encoding="utf-8")
		self.assertIn('providers.gradleProperty("guiTestSuite").orElse("full")', gui_build)
		self.assertIn('} else require(guiTestSuite == "full")', gui_build)
		self.assertNotIn("excludeTags", gui_build)

		method_block = re.search(r"\$taskInformationVisualMethods = @\((.*?)\n\)", runner, re.S).group(1)
		methods = re.findall(r"'([^']+)'", method_block)
		self.assertEqual(21, len(methods))
		self.assertEqual(len(methods), len(set(methods)))
		for method in (
			"physicalReportPrintPreviewShowsEveryPageThroughTheSharedGraphPageableRoute",
			"robotOpensIssue590DialogsWithoutClippedText",
			"physicalSelectedTasksZoomFitsBothSeparatedGanttTasksWithoutChangingModelOrUndo",
			"physicalTaskInspectorFollowsSelectionAndNavigatesOnlyVisiblePredecessors",
			"secondaryDocumentWindowUsesTheSameRibbonShell",
		):
			self.assertIn(method, methods)
		source = (REPO_ROOT / "modules" / "microproject_ui" / "src" / "guiTest" / "java" /
			"com" / "microproject" / "pm" / "graphic" / "frames" / "TaskInformationRibbonGuiAcceptanceTest.java").read_text(encoding="utf-8")
		for method in methods:
			self.assertRegex(source, rf"(?m)^\s*@Test\s*(?:\r?\n\s*)+void\s+{re.escape(method)}\s*\(")
		self.assertIn("Visual GUI test method must map to exactly one @Test source method", runner)


if __name__ == "__main__":
	unittest.main()
