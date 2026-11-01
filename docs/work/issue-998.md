# Issue #998 — remove tracked artifacts

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/998
Contract frozen at: 2026-11-01; baseline HEAD 6959152048b9be184513f275a948090b103387a6; clean isolated worktree.

## Goal and scope
Audit the listed tracked tools, outputs, logs, notes, backups and SVN metadata; delete unneeded entries, guard against reintroduction. Out of scope: .!NNNN!client properties and history rewriting.

| ID | Required result | State | Evidence |
|---|---|---|---|
| A | Each listed path removed or retained with reason | pass | disposition below |
| B | Prevent recurrence via ignore rules | pass | .gitignore |
| C | build installDist and diff check pass | pass | Windows local build and staged diff check; Build CI 41300696593 |

## Disposition
- Delete all 1,976 .tools files (including jpackage outputs): no tracked build or workflow references; wrapper and supported Gradle build are independent. Existing local ignored copy retained.
- Delete rerr.txt, rout.txt, run_err.txt, run_out.txt: transient logs; no tracked references.
- Delete todo.md, wf_todo.md, .codex-issue-regression-audit.md, ISSUES_TRIAGE_PLAN.md and STALE_FILE_AUDIT_2026-06-02.md: unreferenced scratch/triage notes; issue tracking and docs/work hold current state.
- Delete send_esc.py, launch_app.sh, launch_sample.sh, run_gradlew.sh: unreferenced ad hoc scripts; supported launch/build instructions are in docs/build-and-run.md.
- Delete both timestamped samples/June_1_sample*.bak-20260616-023122: unreferenced backup copies, not canonical fixtures.
- Delete the two org/.svn resource files: source-control metadata, not runtime resources. Existing provenance/audit CSV entries remain historical evidence of the baseline, not active dependencies.
- Search: rg --hidden excluding .git, .tools and docs/work for the exact path names and .tools/.svn; only self references and historical audit/provenance records appeared. Also inspected .github/workflows.

## Current slice
**Next slice:** check CI for the reviewed test-only follow-up. **Done when:** exact-candidate checks pass. **Then:** integrate and close.

## Remaining blockers
- Build and GUI acceptance passed for 8492ce672 (runs 41300696593 and 41300696560). Exact-candidate CI and integration are pending after the test-only correction.
- Local: `./gradlew.bat build installDist --console=plain` passed on Windows JDK 25, 2026-11-01.
