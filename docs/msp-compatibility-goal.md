# Microsoft Project compatibility goal

## Final objective

microProject's final product objective is complete compatibility with Microsoft
Project (MSP) desktop for the supported planning workflows. MSP behavior is the
reference when it differs from legacy OpenProj/ProjectLibre behavior, a local
convenience, or an earlier reconstruction decision.

## Compatibility scope

- Task, resource, assignment, calendar, dependency, baseline, and scheduling
  semantics.
- Desktop commands, keyboard and mouse interactions, dialogs, views, printing,
  and accessible UI state.
- Project-file behavior: supported MSP interchange formats and microProject's
  native MPOF format must preserve MSP-compatible project data on round trips.
- Multi-project, master-project, shared-resource, reporting, and collaboration
  workflows when those workflows are supported by MSP desktop.

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

## Definition of done for a compatibility issue

- The MSP scenario is reproducible and mapped to code paths.
- Automated regression coverage passes, including save/reload when data is changed.
- GUI behavior is verified with an installed distribution or an equivalent Swing
  interaction test where desktop automation is impractical.
- The issue records the scenario, verification, remaining limits, and whether the
  result is compatible or intentionally divergent.
