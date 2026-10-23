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


if __name__ == "__main__":
	unittest.main()
