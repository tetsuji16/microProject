# MSP-compatible multiple-project behavior

This document defines the compatibility target for issue #395. It follows the
Microsoft Project desktop master-project workflow: create or open a master
project, choose **Project > Subproject**, select one or more existing project
files, and display their tasks beneath a subproject summary task in the
master's Gantt view.

## Implemented baseline

- A local master project can insert one or more supported project files from
  the native file picker. Files are linked by default; their canonical paths
  provide stable local identities.
- A linked child is loaded as a subproject, not as a separate top-level
  document frame. Its placeholder is used to reopen the child and reject a
  duplicate insertion.
- Refreshing a linked child reloads clean data. If the linked child has
  unsaved changes, the user must choose **Save**, **Discard**, or **Cancel**:
  Save persists the child before refresh; Discard replaces the complete child
  model and its master projection from disk; Cancel leaves both the child and
  the master projection unchanged. Escape is Cancel.
- The Project schedule ribbon exposes **Refresh Subprojects**, **Open
  Subproject**, and **Remove Subproject** for the selected linked summary row;
  removal detaches only the reference and never deletes the child file. The
  same commands remain available from that row's context menu.
- The master owns the consolidated outline and Gantt display. Saving through
  the existing project factory continues to save the master and dirty children
  as one operation.
- Cross-project FS, SS, FF, and SF dependencies use the existing dependency
  model and are resolved while subprojects are opened.
- Multiple top-level local projects remain available through independent
  desktop windows, the project/window selector, and **Arrange All**. The
  selector marks the active project and shows each project's canonical path,
  master/read-only state, and modified state. This is separate from a master
  display: a master's children are not independent windows until explicitly
  opened.

## Linked mode and shared resources

- **Linked writable / linked read-only:** after selecting child files, the
  Insert Project dialog offers a read-only mode.  The master persists both the
  canonical child-file path and this mode in POD data; on reload, the same child
  is reopened without a document frame and its task row retains the file name.
- **Unlinked copies:** are intentionally not represented as linked
  subprojects.  They are a copy/import operation, so creating one must not
  retain a live file reference.  This remains distinct from inserting a linked
  project and is not silently substituted for it.
- **Shared resource pools:** a project created with an already-open pool saves
  the pool project's canonical file path and its precedence policy. When the
  pool and a sharer are opened in either order, matching persisted resource
  IDs are unified to one resource identity and existing assignments are
  rewired. A name is used only as a legacy recovery key when an old resource
  has no persisted ID; two distinct IDs are never merged merely because their
  display names match. The pool-precedence and sharer-precedence policies
  choose which conflicting resource definition wins.

## Acceptance criteria

1. Insert an unopened local child file from a local master project.
2. The child is represented by one `SubProj` placeholder and does not create a
   second document frame.
3. Re-inserting the same linked file is rejected.
4. Saving the master reaches dirty subprojects through the existing recursive
   save path.
5. Reopening a saved master restores child file paths and read-only status.
6. Opening a saved sharer and its pool in either order restores one shared
   resource pool and therefore one aggregate allocation source.
7. A dirty linked child refresh offers Save, Discard, and Cancel. Discard
   removes local-only child rows/hierarchy edits and recreates an open child
   window against the disk-reloaded child; failures leave the old child view
   and projection intact.

## Compatibility boundary and delivery roadmap

The compatibility target is Microsoft Project desktop behaviour, not the
proprietary MPP container implementation. MPP-specific binary records,
undocumented checksums, and private serialization details are explicitly out
of scope. Supported MSP workflows must still behave the same when the project
is exchanged through MSPDI/XML or stored in MPOF.

The work is delivered in these gates:

1. **Document lifecycle**: canonical project identity, duplicate prevention,
   independent windows, close/focus isolation, and safe missing-file errors.
2. **Master projection**: linked child references, read-only state, refresh,
   cycle rejection, child reopen, summary calculations, and save/reload.
3. **Cross-project scheduling**: project-path resolution, external placeholders,
   FS/SS/FF/SF links, lag/lead, cycle detection, unresolved-link state, and
   recalculation after a linked project changes.
4. **Shared resources**: pool identity matching by resource ID, legacy name
   fallback, precedence policy, assignment rewiring, aggregate usage, and
   over-allocation diagnostics.
5. **MPOF persistence**: deterministic manifest, checksums, embedded child and
   pool payloads, migration, atomic save, and round-trip tests independent of
   the original filesystem location.
6. **Compatibility verification**: every gate gets model/persistence tests,
   GUI-simulated acceptance tests, and a real installed-application scenario.

An item is not considered MSP-compatible until its normal path, missing-file
path, read-only path, unsaved-change path, and save/reload path are tested.
Any intentional deviation must be recorded next to the affected acceptance
criterion with a reason and migration rule.

## Current verification record

- Core scheduling and multi-project model tests pass, including shared-pool
  over-allocation caused by assignments in another project.
- Exchange tests pass for MPOF journal preservation without snapshot replay,
  manifest checksum validation, POD persistence (including stable document UUID
  migration), MSPDI/XML dependency types, external
  project-file path round trips, and malformed embedded-child recovery
  (125 tests, 0 failures).
- GUI acceptance covers task-information dependency controls, missing linked
  project recovery actions, resource leveling, Team Planner shared-pool
  overload rendering, and workspace navigation. The navigation evidence covers
  tiled, horizontal, vertical, cascaded, and maximized active-project-only
  layouts; returning from an arrangement now explicitly hides every inactive
  project frame. It also performs an F2 edit attempt on a read-only projected
  child row (which must not open an editor or alter the source task), and opens
  the dirty-child refresh prompt to confirm Save/Discard/Cancel visibility and
  Escape-as-Cancel retention of the in-memory child.
- The Team Planner shared-pool case also uses Robot hover over an aggregated
  overloaded assignment and verifies the visible tooltip identifies the task's
  source project, dates, units, and over-allocation state.
- For a project whose persisted resource-pool reference is unavailable, the
  assignment view visibly disables Assign, Remove, and Replace and supplies a
  recovery explanation; the client project remains open without mutating its
  existing assignments.
- The installed application was launched with two MPO files at the same time;
  the master projection and the independent project Gantt were both visible.
- The regenerated `installDist` application was launched again after the
  unresolved-pool UI correction; the native `microProject` window and its two
  installed-layout Java processes were confirmed from the launcher logs.

An end-to-end Robot workflow selects a task from a second open project in the
Task Information dialog, chooses FS/SS/FF/SF and a one-day lag through the
visible controls, and creates the link. The final task-information grid shows
the qualified external endpoint, dependency type, and lag. The underlying
candidate enumeration and all four dependency types are also covered by
model/UI tests. Candidate labels use the owning source project rather than the
containing master projection, keeping project-grouped choices unambiguous for
projected tasks and for identical task names in different files.

The Gantt renderer uses the same `Source project: predecessor task` label on a
cross-project connector, in addition to the external-link color, so the visual
link is not distinguishable by color alone.

A separate Robot acceptance case marks a cross-project predecessor unavailable,
recalculates the successor project, and opens the predecessor grid. The visible
row continues to show the qualified `Offline project: Unavailable predecessor`
endpoint, FS type, and zero lag; this verifies that unresolved external links
remain inspectable rather than being discarded during recalculation.

The MPOF implementation deliberately persists linked child and resource-pool
references and their metadata; it does not embed proprietary MPP binary
records. Portable exchange is provided through MPOF plus MSPDI/XML, which is
the supported compatibility boundary for this fork. Within MPOF, the manifest
`documentId` is the stable UUID project identity; the legacy numeric
`projectUniqueId` remains only as a backward-compatible task/model lookup key.
The UUID is regression-tested across save, reload, and save again. Each
master-local linked-subproject reference also has a persisted UUID, separate
from the child project identity and its legacy numeric lookup id. POD reference
records also retain a portable stored path, the last successfully observed
canonical path, child project UUID, and modification time; this lets recovery
present both the location originally stored by the master and the last resolved
local identity without conflating either with the reference UUID.

Native POD files persist the same project `documentId`. Opening a legacy POD
that predates this field generates an identity in memory; its first subsequent
save is an intentional one-time migration, after which no-op save/reload/save
cycles remain byte-size stable. This preserves the stable identity required for
cross-project references without claiming compatibility with undocumented MPP
binary records.

Microsoft reference: [Link projects to create a master project in Project
desktop](https://support.microsoft.com/en-us/project/link-projects-to-create-a-master-project-in-project-desktop).
