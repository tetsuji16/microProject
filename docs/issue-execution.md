# Issue execution

This document defines the bounded workflow for implementing and closing an
issue. The issue packet at `docs/work/issue-<number>.md` is the current
execution state; use [`templates/issue-work-packet.md`](templates/issue-work-packet.md).

## Freeze the issue contract at intake

Record the issue's goal, explicit acceptance IDs and evidence for each, scope,
out-of-scope items, blockers, and the current commit. Preserve all original
required criteria. A discovered cause that prevents an existing criterion
belongs in the current slice. An independent discovery becomes a linked
existing or new follow-up; it does not silently expand scope. A new human
requirement revises the contract explicitly. Never defer or scope out a failed
original criterion just to close the issue.

Parent/epic closure requires evidence for every original required acceptance
criterion and resolution of required children. A closed child is not proof that
parent acceptance passed. If all criteria, evidence, and required children are
complete within the approved scope, close the issue and report it in that same
work; do not start a broader audit or cosmetic work. If closure is not
possible, report only the unmet criterion IDs and their required evidence.
Closure does not require unrelated cosmetic perfection across the product.

## Execute one finite slice at a time

Use this sequence: **scope → one active slice → focused evidence → integration
→ close or precise blocker/next slice**. Each step must remove one named
remaining criterion. Keep exactly one next slice in the packet, with a concrete
definition of done and the next unresolved acceptance afterward. Do not mark a
hypothesis as a confirmed cause.

Before edits, record baseline working-tree status and HEAD. For a failure,
capture the failing case/class, environment, head, and complete command/result
lifecycle. Inspect logs and screenshots plus model state before changing code,
especially after GUI failure. After two failed fix attempts against the same
failure, review root-cause evidence and establish a reproducer or diagnostic
before another fix attempt; do not continue guessed patches.

For repeated cases on one code path, define the matrix as one finite slice and
resolve expected calendar/date outcomes before expanding cases. Run focused
checks while iterating, then run the broad suite once after the matrix or
integration is complete. Serialize writers that share files.

Before enabling successful publication from a low-level setter, inventory its
callers, including initialization and detached/internal objects, and gate
project revisions on canonical project ownership where appropriate.

## Evidence and integration

### Classify GUI evidence before diagnosis

- Name the evidence layer explicitly: direct setter/model probe, headless
  verifier, physical Robot route, MPO save/reload, and CI on the exact candidate
  SHA prove different things. Do not substitute one layer for another or
  describe an earlier SHA as exact-candidate CI.
- Before interpreting an error screen, decide whether it is an expected
  application dialog under test or a test failure. Capture the screen/log and
  relevant model state first; for expected dialogs verify message, dismissal,
  and state outcome. Keep diagnostic artifacts for actual failures.
- Record detected foreground overlap or other pre-dispatch desktop contention
  separately from product assertions, with its artifacts and command result.
  Retry only after the desktop is clear; preserve display resolution and do
  not close other apps to force a GUI run.
- Persistence assertions use canonical persisted identity keys (for example,
  task UID plus resource UID for assignments); never assume in-memory object
  identity or generated IDs survive save/reload.
- Run screenshot/DPI matrices when visual constraints change and at the final
  release gate. Continue physical tests for changed GUI routes regardless of
  whether layout changed. Preserve resolution and visual assertions.

- Choose the narrowest existing test/fixture that covers the criterion. Reuse
  it; do not multiply mirrored cases. Compilation is not physical GUI proof.
- Record the tested SHA, scope, and evidence link for every acceptance row.
  Evidence from an earlier SHA may be reused when a scoped diff confirms the
  relevant production code, fixture, and dependencies are unchanged. A
  documentation-only commit alone does not require rerunning tests. Follow any
  separate gate that requires CI on the exact integrated candidate SHA.
- Batch independent changes and run the needed CI gate once. Do not use full CI
  as a debugging loop or repeat a passed broad run without relevant changes or
  a failure.
- For GUI changes, run the focused physical case first when the desktop/CI path
  supports it; otherwise mark it pending and use the GUI acceptance workflow.
  Preserve current screen bounds and resolution; never change resolution to
  satisfy a test. Keep visual assertions intact when they fail.
- Run broad GUI matrices at the existing release/scheduled quality gate, not
  after every small change. Keep the gate requirements in
  [`gui-quality-gate.md`](gui-quality-gate.md) and recovery order in
  [`gui-recovery-sequence.md`](gui-recovery-sequence.md).
- Integrate against the exact candidate SHA required by the gate. Commit the
  reviewed slice and check CI for the pushed head before claiming completion.
  Report unrelated baseline failures honestly.
- Stage only intended files before the final whitespace check so new files are
  included. Run `git diff --cached --check` separately and stop the commit if it
  fails; a later command must not mask its exit status.

## Keep work and context small

The issue packet is the sole source for current state and next action. Keep it
to about 100 lines per issue: acceptance table with status/evidence, one next
slice, remaining blockers, and latest test/CI links. Long historical logs and
screenshots remain supporting history; do not append each step to multiple
large status documents. Resume by reading the packet, then only its relevant
linked sources. At intake, fetch issue body/status and failing-job summaries
once, save facts in the packet, and use scoped `rg` searches. Do not paste huge
logs or status documents into agent context.

Use one Luna investigator or implementer for a bounded task in the normal case.
Use an explorer only for a specific unresolved question, without repeating
completed investigation. Parallelize only independent ownership. Workers get a
compact self-contained packet; use a full-history fork only when necessary,
otherwise fork without history. A worker report is at most about 12 lines:
conclusion, files, commands/outcomes, and unknowns. The primary agent reviews
the diff and evidence instead of rereading the entire investigation. Use local
LLM tools for concrete mechanical work where available. Wait with 30–60 second
backoff; report only new findings and decisions.

Honor fixed review counts already in the issue contract. Reuse completed
evidence; do not add review rounds or repeat acceptance checks without a
changed relevant diff or a finding. Re-review only changed code or a finding's
scope. Do not add monitoring, automation, validators, dependencies, dashboards,
or another status layer to this workflow.
