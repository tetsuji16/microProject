import os
import re
import shutil
import subprocess
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]


class ReleaseWorkflowPolicyTest(unittest.TestCase):
	@classmethod
	def setUpClass(cls):
		cls.gui = (REPO_ROOT / ".github" / "workflows" / "gui-acceptance.yml").read_text(encoding="utf-8")
		cls.build = (REPO_ROOT / ".github" / "workflows" / "build.yml").read_text(encoding="utf-8")
		cls.release = (REPO_ROOT / ".github" / "workflows" / "release-and-pages.yml").read_text(encoding="utf-8")

	def run_hosted_policy_probe(self, body):
		powershell = shutil.which("pwsh") or shutil.which("powershell")
		if powershell is None or os.name != "nt":
			self.skipTest("Windows PowerShell is required for registry state probes")
		runner = (REPO_ROOT / ".github" / "scripts" / "run-gui-acceptance.ps1").read_text(encoding="utf-8")
		start = re.search(r"(?ms)^function Start-HostedNotificationCenterPolicy\b.*?^\}", runner).group(0)
		stop = re.search(r"(?ms)^function Stop-HostedNotificationCenterPolicy \{.*?^\}", runner).group(0)
		probe = r"""
$ErrorActionPreference = 'Stop'
$script:isGitHubActions = $true
$script:hostedNotificationPolicySnapshot = $null
$testRoot = 'Software\GuiAcceptancePolicyTest_' + [Guid]::NewGuid().ToString('N')
""" + start + "\n" + stop + r"""
try {
""" + body + r"""
} finally {
  $script:hostedNotificationPolicySnapshot = $null
  [Microsoft.Win32.Registry]::CurrentUser.DeleteSubKeyTree($testRoot, $false)
}
"""
		result = subprocess.run(
			[powershell, "-NoProfile", "-NonInteractive", "-Command", probe],
			capture_output=True,
			text=True,
			check=False,
		)
		self.assertEqual(0, result.returncode, result.stdout + result.stderr)

	def test_build_tests_every_release_os_and_keeps_test_reports(self):
		self.assertRegex(self.build, r"(?ms)^  workflow_dispatch:\n    inputs:\n      runner_os:.*?default: ubuntu-latest\n        type: choice\n        options:\n          - ubuntu-latest\n          - windows-latest\n          - macos-latest")
		self.assertIn("""fromJSON('["ubuntu-latest","windows-latest","macos-latest"]')""", self.build)
		self.assertIn("fail-fast: false", self.build)
		self.assertIn("needs: verify", self.build)
		self.assertIn('test "${{ needs.verify.result }}" = success', self.build)
		self.assertRegex(self.build, r"(?ms)- name: Install fonts used by report rendering tests\n        if: runner.os == 'Linux'\n")
		self.assertIn("if: always()", self.build)
		self.assertIn("modules/**/build/reports/tests/test", self.build)
		self.assertIn("modules/**/build/test-results/test", self.build)
		self.assertIn("if: failure()", self.release)
		self.assertIn("failed-gradle-test-reports-${{ matrix.platform }}-${{ github.run_id }}", self.release)
		self.assertIn("modules/**/build/test-results/test/*.xml", self.release)
		self.assertIn("modules/**/build/reports/tests/test/**", self.release)

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

	def test_release_assets_follow_unix_job_result_not_dispatch_event(self):
		download = re.search(
			r"(?ms)^      - name: Download Linux and macOS packages\r?\n(.*?)(?=^      - name: )",
			self.release,
		).group(1)
		stage = re.search(
			r"(?ms)^      - name: Stage release assets\r?\n(.*?)(?=^      - name: )",
			self.release,
		).group(1)
		self.assertIn("if: ${{ needs.unix-packages.result == 'success' }}", download)
		self.assertIn("if ('${{ needs.unix-packages.result }}' -eq 'success')", stage)
		self.assertNotIn("inputs.build_unix_packages", download + stage)
		release_job = re.search(r"(?ms)^  release:\r?\n(.*?)(?=^  [a-z][a-z-]*:\r?$)", self.release).group(1)
		self.assertIn("needs.unix-packages.result == 'success' || needs.unix-packages.result == 'skipped'", release_job)

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

	def test_policy_restore_failure_still_stops_the_hosted_warning_watcher(self):
		runner = (REPO_ROOT / ".github" / "scripts" / "run-gui-acceptance.ps1").read_text(encoding="utf-8")
		cleanup_match = re.search(r"(?ms)^function Complete-GuiAcceptanceCleanup \{.*?^\}", runner)
		cleanup = cleanup_match.group(0)
		self.assertRegex(cleanup, r"(?s)try\s*\{\s*Stop-HostedNotificationCenterPolicy\s*\}\s*finally\s*\{\s*Stop-HostedWarningWatcher\s*\}")
		self.assertIn("Complete-GuiAcceptanceCleanup", runner[runner.rfind("} finally {"):])
		restore = re.search(r"(?ms)^function Stop-HostedNotificationCenterPolicy \{(.*?)^\}", runner).group(1)
		self.assertIn("throw 'Original hosted GUI notification-center policy was not restored exactly'", restore)
		self.assertIn("throw 'Temporary hosted GUI notification-center policy remains after restoration'", restore)
		powershell = shutil.which("pwsh") or shutil.which("powershell")
		if powershell is None or os.name != "nt":
			self.skipTest("Windows PowerShell is required for the cleanup failure-path probe")
		probe = r"""
$ErrorActionPreference = 'Stop'
$script:watcherStopped = $false
function Stop-HostedNotificationCenterPolicy { throw 'RESTORE_FAILURE_SENTINEL' }
function Stop-HostedWarningWatcher { $script:watcherStopped = $true }
""" + cleanup + r"""
$restoreFailurePropagated = $false
try { Complete-GuiAcceptanceCleanup } catch {
  if ($_.Exception.Message -ne 'RESTORE_FAILURE_SENTINEL') { throw }
  $restoreFailurePropagated = $true
}
if (-not $script:watcherStopped) { throw 'Watcher cleanup was skipped after policy restoration failed' }
if (-not $restoreFailurePropagated) { throw 'Policy restoration failure was hidden' }
"""
		result = subprocess.run(
			[powershell, "-NoProfile", "-NonInteractive", "-Command", probe],
			capture_output=True,
			text=True,
			check=False,
		)
		self.assertEqual(0, result.returncode, result.stdout + result.stderr)

	def test_hosted_notification_center_policy_is_temporary_and_exactly_restored(self):
		runner = (REPO_ROOT / ".github" / "scripts" / "run-gui-acceptance.ps1").read_text(encoding="utf-8")
		self.assertIn("function Start-HostedNotificationCenterPolicy", runner)
		self.assertIn("if (-not $script:isGitHubActions) { return }", runner)
		self.assertIn("function Stop-HostedNotificationCenterPolicy", runner)
		self.assertIn("Start-HostedNotificationCenterPolicy", runner)
		self.assertIn("Stop-HostedNotificationCenterPolicy", runner)
		self.assertIn("CreateSubKey($path)", runner)
		self.assertIn("$snapshot.CreatedKeyPaths", runner)
		self.assertIn("Refusing to remove temporary policy key containing unrelated data", runner)
		self.assertIn("$key.SetValue($name, 1, [Microsoft.Win32.RegistryValueKind]::DWord)", runner)
		self.assertIn("Original hosted GUI notification-center policy was not restored exactly", runner)
		self.assertIn("Temporary hosted GUI notification-center policy remains after restoration", runner)
		self.assertIn("finally {\n  Complete-GuiAcceptanceCleanup", runner)
		self.assertNotIn("ToastEnabled", runner)

	def test_notification_policy_key_creation_restores_existing_and_missing_paths(self):
		self.run_hosted_policy_probe(r"""
$existingPath = "$testRoot\Existing"
$existing = [Microsoft.Win32.Registry]::CurrentUser.CreateSubKey($existingPath)
$existing.SetValue('DisableNotificationCenter', 0, [Microsoft.Win32.RegistryValueKind]::DWord)
$existing.SetValue('KeepMe', 'preserve', [Microsoft.Win32.RegistryValueKind]::String)
$child = $existing.CreateSubKey('UnrelatedChild')
$child.SetValue('KeepMe', 29, [Microsoft.Win32.RegistryValueKind]::DWord)
$child.Dispose()
$existing.Dispose()
Start-HostedNotificationCenterPolicy $existingPath
$active = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($existingPath, $false)
if ([int]$active.GetValue('DisableNotificationCenter') -ne 1 -or $active.GetValue('KeepMe') -ne 'preserve' -or $active.GetSubKeyNames() -notcontains 'UnrelatedChild') { throw 'Existing key setup changed unrelated data' }
$active.Dispose()
Stop-HostedNotificationCenterPolicy
$restored = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($existingPath, $false)
if ([int]$restored.GetValue('DisableNotificationCenter') -ne 0 -or $restored.GetValueKind('DisableNotificationCenter') -ne [Microsoft.Win32.RegistryValueKind]::DWord -or $restored.GetValue('KeepMe') -ne 'preserve' -or $restored.GetSubKeyNames() -notcontains 'UnrelatedChild') { throw 'Existing key was not restored exactly' }
$restored.Dispose()

$missingPath = "$testRoot\Missing\Explorer"
if ($null -ne [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($missingPath, $false)) { throw 'Missing-path fixture unexpectedly exists' }
Start-HostedNotificationCenterPolicy $missingPath
$created = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($missingPath, $false)
if ($null -eq $created -or [int]$created.GetValue('DisableNotificationCenter') -ne 1) { throw 'Missing policy path was not created and set' }
$created.Dispose()
Stop-HostedNotificationCenterPolicy
$missingAfterRestore = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($missingPath, $false)
$missingParentAfterRestore = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey("$testRoot\Missing", $false)
if ($null -ne $missingAfterRestore -or $null -ne $missingParentAfterRestore) { throw 'New policy path was not removed after restoration' }

$failurePath = "$testRoot\Failure"
$failureKey = [Microsoft.Win32.Registry]::CurrentUser.CreateSubKey($failurePath)
$failureKey.SetValue('DisableNotificationCenter', 'keep-original', [Microsoft.Win32.RegistryValueKind]::String)
$failureKey.SetValue('KeepMe', 'preserve', [Microsoft.Win32.RegistryValueKind]::String)
$failureKey.Dispose()
$setupFailed = $false
try { Start-HostedNotificationCenterPolicy $failurePath } catch { if ($_.Exception.Message -notmatch 'unsupported existing value type') { throw }; $setupFailed = $true }
if (-not $setupFailed) { throw 'Unsupported existing policy type did not fail setup' }
$failureRestored = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($failurePath, $false)
if ($failureRestored.GetValue('DisableNotificationCenter') -ne 'keep-original' -or $failureRestored.GetValueKind('DisableNotificationCenter') -ne [Microsoft.Win32.RegistryValueKind]::String -or $failureRestored.GetValue('KeepMe') -ne 'preserve') { throw 'Failed setup did not preserve original values' }
$failureRestored.Dispose()
""")

	def test_missing_policy_path_cleanup_refuses_to_delete_unrelated_data(self):
		self.run_hosted_policy_probe(r"""
$missingPath = "$testRoot\Conflict\Explorer"
Start-HostedNotificationCenterPolicy $missingPath
$created = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($missingPath, $true)
$created.SetValue('ArrivedLater', 73, [Microsoft.Win32.RegistryValueKind]::DWord)
$created.Dispose()
$restoreFailed = $false
try { Stop-HostedNotificationCenterPolicy } catch { if ($_.Exception.Message -notmatch 'unrelated data') { throw }; $restoreFailed = $true }
if (-not $restoreFailed) { throw 'Cleanup removed a created key containing unrelated data' }
$remaining = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey($missingPath, $false)
if ($null -eq $remaining -or $remaining.GetValue('ArrivedLater') -ne 73 -or $remaining.GetValueNames() -contains 'DisableNotificationCenter') { throw 'Cleanup failure removed unrelated data or kept the temporary policy value' }
$remaining.Dispose()
""")

	def test_gui_runner_diagnostics_are_isolated_and_restore_registry_values(self):
		diagnostics = (REPO_ROOT / ".github" / "workflows" / "gui-runner-desktop-diagnostics.yml").read_text(encoding="utf-8")
		self.assertIn("workflow_dispatch:", diagnostics)
		self.assertIn("pull_request:", diagnostics)
		self.assertIn("runs-on: windows-latest", diagnostics)
		self.assertIn("Test-RegistryDwordWriteAndRestore", diagnostics)
		self.assertIn("finally {", diagnostics)
		self.assertIn("Registry probe restoration failed", diagnostics)
		self.assertIn("Press-WinN", diagnostics)
		self.assertIn("IsNotificationCenterWindow(GetForegroundWindow())", diagnostics)
		self.assertIn("Save-DesktopEvidence 'win-n-with-policy-before-reboot'", diagnostics)
		self.assertIn("Save-DesktopEvidence 'win-n-after-policy-restore'", diagnostics)
		self.assertIn("Ensure-PanelClosed 'baseline'", diagnostics)
		self.assertIn("Ensure-PanelClosed 'policy-active'", diagnostics)
		self.assertIn("Ensure-PanelClosed 'policy-restored'", diagnostics)
		self.assertIn("Policy cleanup: $policyRestoreState", diagnostics)
		self.assertNotIn("run-gui-acceptance.ps1", diagnostics)
		self.assertNotIn("gradlew", diagnostics)

if __name__ == "__main__":
	unittest.main()
