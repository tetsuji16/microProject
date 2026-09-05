# MSP multiple-project verification matrix

This matrix is the completion-audit record for issue #395. A row is only
marked verified when the named implementation and current test artifact cover
the stated behavior. Proprietary MPP binary-container details are excluded by
the documented MPOF/MSPDI/native-format compatibility boundary.

| Requirement | Authoritative implementation / test evidence | GUI evidence |
| --- | --- | --- |
| Independent documents, switching, close isolation, and Arrange All | `DefaultFrameManager`, `ProjectFileDropTransferHandler`, `GraphicManager.openLocalProject`, `GraphicManager.openLocalProjectsSequentially`, `GraphicManager.activateSubproject`, `MultipleLocalProjectOpenGuiAcceptanceTest`, `MasterSubprojectMpoGuiAcceptanceTest`, `DefaultFrameManagerTest`, `DefaultFrameManagerGuiAcceptanceTest` | `msp-multiple-local-project-open.png`: one File/Open multiple-MPO selection loads two separately addressable document frames in sequence, asserts two visible desktop windows, and captures both in an Arrange All tile. `msp-command-line-multiple-project-open.png` and `msp-file-drop-multiple-project-open.png` exercise the same two-document registration through startup-argument and desktop-file-drop routes. `msp-master-four-window-navigation.png`: a master, two independent MPOs, and a child opened through Open Subproject are all separate navigable document frames and are tiled together; `msp-independent-project-windows-tiled.png`, `msp-window-switch-{horizontal,vertical,cascade,active-project-only}.png` |
| Canonical-path duplicate prevention and safe local-file errors | `GraphicManager.findFrameForProjectFile`, `ProjectFactory`, local-session and chooser regression tests | `canonical-project-window-no-duplicate.png`: Robot opens a `.\\` alias after its canonical path, receives the existing `DocumentFrame`, and asserts that exactly one additional project frame exists. `invalid-standalone-project-open-error.png`: Robot opens a malformed MPO via the ordinary local-file route, sees its path and non-destructive error, and verifies the already open frame remains active and unique. |
| Linked master/subproject projection, read-only rows, refresh, recovery, and child-only save | `DefaultSubprojectHandler`, `DefaultSubProj`, `ProjectFactory`, `JobQueue`, `ProjectMergeService`, `GraphicManager`, `MasterSubprojectMpoGuiAcceptanceTest`, core/exchange round-trip tests | `master-subproject-consolidated-gantt.png`, `master-save-dirty-child.png` (a clean master Save persists an edited linked child while another child is loading; the test reloads the child and proves that a sibling task was not serialized into it), `master-read-only-projection-edit-rejected.png`, `linked-subproject-recovery-popup.png`, `invalid-linked-subproject-recovery-popup.png`, `invalid-linked-subproject-refresh-warning.png`, `access-denied-linked-subproject-refresh-warning.png`, `unsaved-linked-subproject-refresh-cancel.png` |
| Circular subproject insertion is rejected without changing the master | `DefaultSubprojectHandler.wouldCreateCircularReference`, `ProjectFactory.openSubproject` | `circular-subproject-rejection.png` (the complete reference chain is visible) |
| Direct master commands | Project schedule ribbon: Insert/Refresh/Open/Remove; `GraphicManagerLinkRouteTest`, ribbon structure/action tests | Linked-project recovery popup and master Robot cases |
| Cross-project FS/SS/FF/SF, lag/lead, cycle rejection, and unresolved preservation | `DependencyServiceTest`, `TaskInformationDialogDependencyTest`, POD/MSPDI exchange tests | `TaskInformationCrossProjectGuiAcceptanceTest`, `unresolved-cross-project-predecessor.png` |
| Cross-project source identification | Task Information chooser/grid and `GanttRenderer.crossProjectLinkLabel` | Qualified predecessor grid and `master-subproject-cross-project-gantt.png` |
| Shared pools, identity unification, aggregate over-allocation, and pool recovery | `SharedResourcePoolServiceTest`, `TeamPlannerServiceTest` | `team-planner-shared-overload-source-project.png`, `unresolved-shared-resource-pool-assignment-disabled.png` |
| MPOF manifest/checksums, embedded children/pools, relocation, migration, atomic save | `MpoFileImporterTest`, `PodRoundTripTest`, `ProjectMergeServiceLoadResultTest` | `MasterSubprojectMpoGuiAcceptanceTest`; installed application launch |

## Required recurring verification

- After a functional change: focused module test plus the affected Robot case.
- After a UI or persistence change: `:micrproject_ui:guiTest` and the relevant
  core/exchange tests.
- Before a completion claim: `clean build installDist`, inspect test XML counts,
  and launch the freshly regenerated installed layout.

The current artifacts are generated under
`modules/micrproject_ui/build/reports/guiTest-artifacts/`; they are evidence,
not source-controlled deliverables.
