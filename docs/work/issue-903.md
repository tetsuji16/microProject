# Issue #903 — Remove the unused server-local importer route

State: IMPLEMENTED — frozen local verification passed; final CI/closure status in [integration record](https://github.com/tetsuji16/microProject/issues/774#issuecomment-6382996503)
Issue: https://github.com/tetsuji16/microProject/issues/903
Parent: #774, phase 2/8
Contract frozen at: 2026-10-24, delegated slice request
Baseline HEAD: `ffc291d30a4ebe1624337b78d3fd8d5ddb30a815`
Baseline working tree: untracked `.codex-remote-attachments/` only

## Goal and scope

Remove `ServerFileImporter` and `ServerLocalFileImporter` and their typed
registration while preserving ordinary local POD loading, old MPO reads, frozen
POD bytes, and any minimal legacy importer-option aliases needed at the registry
boundary. Do not add connected-service behavior or change serialized model
classes, FQNs, UIDs, or fields.

Out of scope: `com.microproject.server.data` DTOs and the separate
`SessionFactory.call` resource lookup paths in core/UI. No commits created.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Server importer base, subclass, registration, key constant, callers, obsolete tests removed | pass | Diff and `rg` inventory; only compatibility alias strings remain |
| A2 | Keep local POD route and resource/task mapping behavior | pass | exchange suite includes 3 fixed-sample POD round trips and assignment semantics |
| A3 | Retain justified option aliases without exposing server route as a typed format | pass | provider test verifies `server-local` and old class-name alias create `LocalFileImporter`; typed registry contains only `pod`, `mpo`, `mspdi` |
| A4 | Frozen POD descriptor inventory and historical MPO read remain intact | pass | `PodDescriptorInventoryTest`, `PodRoundTripTest`, `MpoHistoricalFixtureTest` in exchange suite |
| A5 | Focused owning module checks pass | partial | exchange 335 tests pass; core 35 tests pass; application 36/37 pass (one failure below) |

## Caller, reflection, and persistence findings

- Production references to the deleted types were only the exchange provider and
  `ServerLocalFileImporter`'s call to `ServerFileImporter.prepareResources`.
  No XML registration or other subclass was found.
- The removed base used reflective `SessionFactory.call` for server resource
  hierarchy/descriptors. Independent core/UI lookups remain because they serve
  separate resource-picker/project workflows.
- Current POD policy selects `LocalSession.LOCAL_PROJECT_IMPORTER`. The old
  `server-local` option and `com.microproject.exchange.ServerLocalFileImporter`
  class-name string now resolve to that same local importer in the legacy session
  registry; neither is exposed through the typed port registry. This preserves a
  boundary bridge without retaining the server resource adapter.
- Fixed POD descriptor inventory records serialized DTO FQNs/UIDs/fields, not
  importer-option values. No fixed-fixture evidence was used to claim that all
  historical user PODs omit those option strings. Existing keys are therefore
  retained as aliases. POD bytes and model descriptors were not edited.

## Verification

- `\.gradlew.bat :microproject_exchange:test :microproject_application:test :microproject_core:test --console=plain --no-daemon --max-workers=1` — exchange passed (335 tests); application failed one assertion in `ProjectLoadWorkflowTest.preparesLoadOptionsForMpoCollaborationFile` concerning `.microproject-sync.json`; core did not run in that combined command.
- `\.gradlew.bat :microproject_core:test --console=plain --no-daemon --max-workers=1` — passed (35 tests).
- `git diff --check` — passed.
- Application failure is outside this slice: concurrent shared-tree edits modify `CollaborationMetadataStore` and its test, and the assertion concerns collaboration sidecar naming, not importer selection. Those files were not changed by this slice. Re-run after the concurrent work is integrated if the parent requires a clean application gate.

## Current slice

**Next:** parent review/integration; re-run application suite once the concurrent
sidecar change is stable. Do not close #903 until the application failure is
resolved or proven unrelated on a stable candidate.

## Final integration gate

The frozen candidate passed architecture/naming gates, maintenance 20/20,
source inventory, clean build/installDist (2,880 tests; no failures/errors/skips),
and GUI source compilation. This supersedes earlier slice-time pending local
checks above. Candidate SHA, Windows Robot outcomes and closure are maintained
in the linked integration record; physical acceptance is required before close.
