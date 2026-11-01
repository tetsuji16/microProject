# Issue #988 — remove test-only reflection

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/988
Contract frozen at: 2026-11-01; baseline HEAD 6959152048b9be184513f275a948090b103387a6; clean isolated worktree.

## Goal and scope
Remove production reflection to the test-only BootstrapApplet, retain route coverage, and remain consistent with #738. Out of scope: lifecycle redesign.

| ID | Required result | State | Evidence |
|---|---|---|---|
| A | No production reflection to test class | pass | GraphicManager uses FrameHolder only |
| B | Preserve route coverage by another method | pass | GraphicManagerLinkRouteTest (distinct active-manager fallback) and full build |
| C | Consistent with #738 | pass | Existing FrameHolder lifecycle contract, no new responsibility |

## Current slice
**Next slice:** push the reviewed test correction and check its CI. **Done when:** exact-candidate checks pass. **Then:** integrate and close.

## Remaining blockers
- CI for 8492ce672 passed (Build 41300696593; GUI acceptance 41300696560). The follow-up test correction needs exact-candidate CI and integration.
- Local: the strengthened focused test passed after the review correction; `./gradlew.bat :microproject_ui:test --tests com.microproject.pm.graphic.frames.GraphicManagerLinkRouteTest --console=plain` and `./gradlew.bat build installDist --console=plain` passed on Windows JDK 25, 2026-11-01.
