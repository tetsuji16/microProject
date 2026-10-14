# Microsoft Project compatibility goal

## Final objective

microProject's product objective is complete compatibility with the standalone
Microsoft Project (MSP) desktop application. This is a goal, not an achieved
capability or a claim limited to whichever workflows happen to be implemented
today. MSP behavior is
the reference when it differs from legacy OpenProj/ProjectLibre behavior, a
local convenience, or an earlier reconstruction decision. The staged scope,
coverage blockers and issue review are tracked in
[docs/msp-compatibility-roadmap.md](msp-compatibility-roadmap.md).

## Compatibility scope

- Task, resource, assignment, calendar, dependency, baseline, and scheduling
  semantics.
- Desktop commands, keyboard and mouse interactions, dialogs, views, printing,
  and accessible UI state.
- Project-file behavior: microProject's native `.mpo` (MPO) is the authoritative
  project format and default save format; it must preserve project data and read
  files from earlier supported MPO versions. MPP (`.mpp`) is a separate external
  Microsoft Project import format; reading remains in scope, while MPP writing
  is explicitly excluded by the user's 2026-10-12 product decision and MPO is
  the native save substitute. Project XML is another
  external interchange format and does not substitute for MPP coverage. POD is
  a frozen legacy import format: preserve its wire contract and fixed-fixture
  reads, and add no new POD write behavior.
- Multi-project, master-project, shared-resource, reporting, and collaboration
  workflows supported by MSP desktop, including local linked projects and
  offline collaboration. On 2026-10-12 the user explicitly excluded Microsoft
  Project VBA/macros, the macro-facing Project object model, COM automation,
  COM/VSTO add-ins, and local Office add-ins from scope. This exclusion does not
  remove microProject-native local automation/extensions or local/offline
  collaboration. Unsupported or unassessed in-scope local desktop behavior
  blocks a complete claim.
- Project Server, Project Web App (PWA), Project Online, Microsoft Planner cloud
  authentication/synchronization/publishing, and other connected-service APIs
  are outside this standalone product goal. Do not add connected-service
  imports, clients, or dependencies. This exclusion does not remove local file
  interoperability, local master/linked projects, shared resources, offline
  collaboration, or microProject-native local automation/extensions from scope.

## Engineering rules

1. Every compatibility-affecting change identifies the corresponding MSP behavior
   and records any intentional deviation in the issue or change documentation.
2. A change is not complete merely because it compiles: add focused regression
   coverage for model/persistence behavior and GUI or GUI-simulated coverage for
   user-visible operations.
3. Preserve existing project data. Reads must fail clearly for unsupported input;
   writes must be deterministic and round-trip tested.
4. When MSP behavior is uncertain, keep the issue open and verify against current
   Microsoft documentation or a controlled desktop evaluation before choosing a
   behavior.
5. Compatibility work takes priority over product-specific convenience behavior.
   A deliberate deviation requires an explicit user decision and a documented
   migration/compatibility boundary.
6. The compatibility inventory includes every in-scope command, workflow,
   file format, and local integration/extension. Missing, partial, unsupported,
   and unassessed
   rows remain visible; “complete” is not claimed while any such row remains.
7. Microsoft-published specifications and support documentation are normative
   for behavior and issue closure. Mark inference as a document-derived
   decision. Installed Project desktop captures supplement behavior evidence
   and are required when claiming current visual appearance; record the exact
   product build and capture environment.

## Definition of done for a compatibility issue

- The MSP scenario is reproducible and mapped to code paths.
- Automated regression coverage passes, including save/reload when data is changed.
- GUI behavior is verified with an installed distribution or an equivalent Swing
  interaction test where desktop automation is impractical.
- The issue records the scenario, verification, remaining limits, and whether the
  result is compatible or intentionally divergent.
