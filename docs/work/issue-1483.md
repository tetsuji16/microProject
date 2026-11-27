# Issue #1483 work packet

- Goal: restore the release-blocking packaged MPP/POD import smoke verification.
- Issue: https://github.com/tetsuji16/microProject/issues/1483
- Baseline: `origin/master` / `9a58bdee786d1802375f38621f0ecef6eeb58a94`; release run `46055954774` failed on Windows with `ClassNotFoundException: com.microproject.integration.PackagedImportSmokeMain`.
- Scope: `build.gradle.kts` `verifyPackagedFileImports` mainClass only; retain dependencies, args, limited modules, and MPP/POD fixtures.
- Out of scope: file formats, persistence, distribution dependencies, disabling package verification.
- Acceptance:
  - [ ] `verifyPackagedFileImports` fails before and succeeds after the change.
  - [ ] Both MPP and POD fixture reads and generated Windows launcher check execute successfully.
  - [ ] `build` succeeds.
  - [ ] `git diff --check` is empty.
  - [ ] PR `build` and `GUI acceptance` pass.
- Next slice: reproduce baseline failure, correct package name, run issue verification commands, then open PR and await required checks.
- Baseline workspace status: clean in dedicated worktree; primary workspace changes left untouched.
