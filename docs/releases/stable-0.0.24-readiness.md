# microProject 0.0.24 stable release readiness

**Assessment date:** 2026-10-11  
**Candidate source:** `master` at `a7aefbbea033d874926d914e25e34d7362fa32e1`  
**Decision:** candidate work may proceed; this assessment does not authorize calling the current beta a stable release. The stable publication gate must pass on the exact merged candidate commit.

## Current release position

- GitHub lists `v0.0.23.1219` as the latest stable release and `v0.0.24-beta.2` as the latest 0.0.24 prerelease.
- Beta 2 passed its recorded Windows release run: full module tests, GUI smoke, MSI and portable ZIP packaging, packaged file-import checks, EXE launch, and install/package JAR hash comparison. That evidence is for its source commit, `f6f6be2d9ee286638ed19681c620520e73227afa`.
- Beta 3 planning and the local-POD routing slice are now in `master` at `a7aefbbea033d874926d914e25e34d7362fa32e1`. The three latest Build workflow jobs for this commit succeeded. They do not replace the Windows release workflow or its GUI/package gates.
- The beta 2 documentation explicitly leaves the broad GUI audit to the scheduled full suite. The beta 3 plan requires exact-head build, full GUI audit, package inspection, and packaged launch before publication. Those gates have not yet been evidenced for this candidate.

The open architecture epics (#403, #408–#411, #414, #737 and related issues) are ongoing engineering work, not blanket stable-release blockers. A release is blocked by reproducible failures in shipped task-table, Gantt, CCPM, file-open/save, Undo/Redo, persistence, or package behavior, and by any required release gate that has not run on the candidate SHA.

## Stable acceptance gates

1. Merge the guarded stable-release workflow changes and create `release/stable-0.0.24` from that exact merged commit. The branch's embedded base version must match `version` in `build.gradle.kts`.
2. Pass clean build and all module tests, architecture/naming checks, packaged POD/MPP import checks, and the repository maintenance-script tests. Resolve unexplained skips and failures.
3. Pass the full Windows GUI acceptance suite: complete ja/100% functional suite followed by the documented Japanese and English 100/125/150% visual matrix. Confirm each required leg ran and each skip matches an explicit gate waiver.
4. Verify the practical project workflow on the tested build: edit and organize tasks, maintain task-table/Gantt consistency, create and change dependencies, apply and clear CCPM with Undo/Redo, then save and reopen MPO. Verify POD backward reads and supported MPP imports using fixed fixtures.
5. Build the Windows MSI and portable ZIP. Inspect package contents, confirm the packaged UI JAR matches `installDist`, run the packaged application to a visible window, and confirm import checks and SHA-256 checksums.
6. Review release notes and user-facing documentation for the exact version. Only after every gate above passes may the workflow publish the stable GitHub Release and update stable `latest` assets/feed.

## Guarded workflow path

Push `release/stable-0.0.24` only after the workflow change is merged and the source commit has passed its required checks. A stable branch run derives `v0.0.24.<run-number>` and the corresponding four-part package version. It requires the full GUI suite, checks the base-version match, and stops if the stable tag already exists. The release workflow creates the tag and publishes the MSI, portable ZIP, checksums, and release notes only when all jobs pass. Stable workflow dispatch must be run from a matching `release/stable-MAJOR.MINOR.PATCH` branch and must explicitly select `gui_suite=full`.

Do not dispatch the stable channel against `master` or create a stable tag manually to bypass these checks. A failed gate means no publication; fix the failure on a new candidate commit and rerun the complete stable gate.
