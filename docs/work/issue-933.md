# Issue #933 — M5.2 Linux/macOS release package evidence

State: Complete; current stable Linux/macOS assets are verified and both launch smoke jobs passed.
Issue: https://github.com/tetsuji16/microProject/issues/933
Parent: https://github.com/tetsuji16/microProject/issues/795
Contract frozen at: 2026-10-25, issue body and current stable release.
Baseline HEAD: `1875236e204f86d887f44eea46809b6f71aa6b0d`.
Baseline working tree: pre-existing untracked attachment and sample sync sidecars; preserve them.

## Goal and scope

Verify the current stable Linux x64 and macOS Apple Silicon packages, including
integrity and a bounded launch on each target OS. No release assets or `latest`
aliases are changed by the verification workflow.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Latest stable tag and exact source SHA for both platform packages | pass | `v0.0.24.1265`, `8404a6c3a15eeda8bc32e06d92cda11cb2fd52af`; API `/releases/latest` |
| A2 | Linux/macOS asset names, SHA-256, size and release URLs | pass | API digests recorded below |
| A3 | Both target OS package jobs passed for the same SHA | pass | Release workflow 39968551059; Ubuntu and macOS jobs succeeded |
| A4 | Downloaded package digest equals release API digest; app launches on OS | pass | Workflow run 40198218372; both target jobs passed |
| A5 | Record platform GUI/accessibility evidence and limits | pass with limits | Release run 39968551059 has Windows full GUI matrix; Unix run 40198218372 proves launch only, not physical interaction/accessibility. Linux log reports AT-SPI bus unavailable in hosted Xvfb. |

## Current release evidence

- Latest stable: `v0.0.24.1265`, published 2026-10-23; target commit is
  `8404a6c3a15eeda8bc32e06d92cda11cb2fd52af`.
- Linux: `microProject-0.0.24.1265-linux-x64.zip`, 132,912,693 bytes,
  SHA-256 `f869d9cf9d570e4dd3a378e676e90c1bf6d284ea9f38d2e326a0d46206b618df`.
- macOS: `microProject-0.0.24.1265-macos-aarch64.zip`, 125,719,191 bytes,
  SHA-256 `366ca73fb13597511d0fbc8e650fa5663b60c3675a8f6b9a2292b646dac7f3f9`.
- The same-release workflow 39968551059 passed both target-OS package jobs and
  the Windows release job. It is not launch evidence for the Unix app images.
- Release API record: https://api.github.com/repos/tetsuji16/microProject/releases/latest
- Release: https://github.com/tetsuji16/microProject/releases/tag/v0.0.24.1265
- Target-OS launch workflow: https://github.com/tetsuji16/microProject/actions/runs/40198218372
- Workflow source at run: `5da89eb6eef51f87ed7525a3ad91b7e24cc0d6a8`.
- Linux artifact `launch-logs-linux-x64` (ID `12599679693`):
  https://github.com/tetsuji16/microProject/actions/runs/40198218372/artifacts/12599679693
- macOS artifact `launch-logs-macos-aarch64` (ID `12600003952`):
  https://github.com/tetsuji16/microProject/actions/runs/40198218372/artifacts/12600003952
- Workflow PRs #935–#939 corrected runner-specific ZIP handling; final merged
  implementation is in the workflow source SHA above.
- Both logs record release API SHA-256 and downloaded SHA-256 equality, and
  byte counts equal the API sizes. Both package launch steps passed.
- Linux stdout/stderr show startup reached `ApplicationStartupFactory` and GUI
  initialization; the desktop accessibility bus is unavailable in hosted
  Xvfb. macOS stdout/stderr show startup reached `ApplicationStartupFactory`.
- Both launch logs report that the external update configuration URL under the
  legacy ProjectLibre GitHub repository has a signature-length error; the
  installed application continues launching without update. This is recorded
  as a release residual, not mistaken for launch success of the updater.

## Current slice

**Completed:** The read-only workflow validated both published asset digests
and launched both packages on their target OS. Logs are retained as run
artifacts linked above.
**Next:** #934 publishes the integrated report. This issue alone does not close
parent #795.

## Remaining blockers

- No remaining acceptance blockers for package currency, integrity, or launch.
- Physical Linux/macOS interaction and accessibility routes remain untested;
  this issue records that limit and does not claim it as verified.

## Latest evidence

- Repository PR checks passed for launch-workflow fixes (#936–#939): Build and
  GUI acceptance. The last target-OS run is 40198218372.
- Release Build/package/GUI: run 39968551059, exact source SHA above.
- Physical Linux/macOS user interaction and accessibility routes are not
  established by package creation or launch smoke; report this distinction.
