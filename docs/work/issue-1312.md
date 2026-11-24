# Issue #1312 — Core package relocation

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1312
Contract frozen at: 2026-11-24, issue body.
Baseline HEAD: `a9fe36227`
Baseline working tree: clean isolated worktree.

## Goal and scope

Relocate the specified core packages and resources, preserving earlier MPO/POD reads and fixing reflection, generated formulas, resource paths and architectural maps. Exclude unrelated package moves and class redesign.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | old core package directories absent | pass | tracked sources moved; check final tree |
| A2 | old name grep empty | open | conflicts with required persisted-class aliases; issue comment 7125044644 |
| A3 | historical MPO and POD fixtures read | pass | focused inventory, historical MPO and POD descriptor tests |
| A4 | required build and GUI acceptance pass | open | pending |
| A5 | prescribed compilation, architecture, tests and clean build pass | open | pending |

## Current slice

**Next slice:** complete clean build, PR checks and resolve acceptance mismatch.
**Done when:** required checks pass and the A2 contract is clarified.
**Then:** prepare final PR and integrate.

## Remaining blockers

- A2: exact grep includes required legacy alias strings; issue comment records the contradiction.
- A1 and A3–A5 pending.

- Compile, architecture boundary fixtures, Japanese bundle, formula configuration, relocation, prior MPO and POD descriptor tests passed. Source inventory and maintenance-script tests passed. Clean build pending.
