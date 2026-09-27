# OpenProj Java 25 modernization progress

Status: in progress; this document does not mark any issue phase complete.

Initial modernization baseline checked: `origin/master` at
`59eb4e0dc1157b382d754deb0acc79fd3384ac5b`. Subsequent implementation
checkpoints were each based on the latest `origin/master`; their merge commits
are recorded below. Progress is summarized in
[issue #595](https://github.com/tetsuji16/ProjectLibre/issues/595).

## Integrated checkpoints

- PR [#597](https://github.com/tetsuji16/ProjectLibre/pull/597) merged as
  `5e6f519cad233ff53d5a35363315c192de6784a0` after full CI success. It integrated
  the initial core modernization and audit tranche; issue #595 remains open.
- PR [#598](https://github.com/tetsuji16/ProjectLibre/pull/598) merged as
  `6fd9a4b74cece7bce723beef81cf998c0e751cff` after full CI success. Follow-up
  audit work continues from that latest `origin/master` baseline.
- PR [#599](https://github.com/tetsuji16/ProjectLibre/pull/599) merged as
  `7f6e3fb35c5a925a6ccac838b7ea0f4fc52c9678` after full CI success; removed an
  unused chart popup and fixed chart workspace vertical-scroll restoration.
- PR [#600](https://github.com/tetsuji16/ProjectLibre/pull/600) merged as
  `f4f50c022c4ed075278858c33f28dacaaa67c698` after full CI success; made the
  active not-void filter singleton safely initialized.
- PR [#601](https://github.com/tetsuji16/ProjectLibre/pull/601) merged as
  `c48b9e92224fd29d766bad5e2cde6730bf458643` after full CI success; removed
  the unreferenced numeric maximum visitor.
- PR [#602](https://github.com/tetsuji16/ProjectLibre/pull/602) merged as
  `40dacaa1cb14bcd778faba831f9fbdf8608e9ab8` after full CI success; refreshed
  the audit checkpoint history and inventory counts.
- PR [#603](https://github.com/tetsuji16/ProjectLibre/pull/603) merged as
  `e2df4e960f844df8d2c49ebd34c7c96d5af50323` after a successful CI rerun; used
  modern reflective construction for `NodeFactory` while preserving behavior.
- PR [#604](https://github.com/tetsuji16/ProjectLibre/pull/604) merged as
  `6216e76ed68d97d2850c1bfc6350e145fc8e2bc8` after a successful CI rerun; removed
  the unreferenced serializable `NodeFieldList` class and documented residual
  external serialization risk.
- PR [#606](https://github.com/tetsuji16/ProjectLibre/pull/606) merged as
  `fbd6271dcb5887fa3b0758eb69a25c18e984c307`; made the collaboration lock
  contention test deterministic after its timing race was found in this work.
  The product locking behavior was not changed.
- PR [#607](https://github.com/tetsuji16/ProjectLibre/pull/607) merged as
  `1e28c3df1606aed06a2fffbb4bc32e17ccaa9a2a` after full CI success; removed the
  unreferenced `TestFilter`.
- PR [#608](https://github.com/tetsuji16/ProjectLibre/pull/608) merged as
  `fb7ad67ade2c24cc11f287b7acf25879265bccae` after full CI success; typed
  Digester-populated form configuration collections end-to-end.
- PR [#609](https://github.com/tetsuji16/ProjectLibre/pull/609) merged as
  `98563696ffe8a15705195ff345365f2858073fd3` after full CI success; narrowed an
  association-format collection boundary to a wildcard type.
- PR [#610](https://github.com/tetsuji16/ProjectLibre/pull/610) merged as
  `b5145b32aec4720b3a7ac504e166dad5b821a478` after full CI success; typed the
  shared object-reference collection boundary and its reports consumer.
- PR [#611](https://github.com/tetsuji16/ProjectLibre/pull/611) merged as
  `f887394942562c743ad5893ddd2c4be8ef5ee355` after full CI success; this
  adjacent fork-owned cleanup is excluded from issue #595 progress (see scope
  correction below).
- PR [#612](https://github.com/tetsuji16/ProjectLibre/pull/612) merged as
  `bed064083051d16e3fd07a2586e98b27862cd44e` after full CI success; its
  fork-added undo-position bookkeeping is excluded from issue #595 progress.
- PR [#613](https://github.com/tetsuji16/ProjectLibre/pull/613) merged as
  `63f7e0cd287964b657894429ef6ecbdb6d638d86` after full CI success; typed the
  OpenProj-derived private hierarchy indent traversal.
- PR [#614](https://github.com/tetsuji16/ProjectLibre/pull/614) merged as
  `c437fe9551906c1ef976c222510a5fb0472aa3fe` after full CI success; corrected
  attribution and excluded adjacent fork-origin work from issue progress.
- PR [#615](https://github.com/tetsuji16/ProjectLibre/pull/615) merged as
  `d0d7e3c1be4d243f51a0deb6e98b0af7785a24b8` after full CI success; safely
  initialized the OpenProj-matching general-options singleton.
- PR [#616](https://github.com/tetsuji16/ProjectLibre/pull/616) merged as
  `7860a5aee7cb296bf75596f98860c871ee11d457` after full CI success; made the
  OpenProj-matching CSS hierarchy collection boundary wildcard-typed.
- PR [#617](https://github.com/tetsuji16/ProjectLibre/pull/617) merged as
  `322551e88fd6a5a877aecf3b055c08502d077b43` after full CI success; typed the
  OpenProj-derived node-filter iterator boundary.
- PR [#618](https://github.com/tetsuji16/ProjectLibre/pull/618) merged as
  `2b177c667a48703c668bc9d4a813cf470305268b` after full CI success; typed the
  OpenProj-derived project root-node query.
- PR [#619](https://github.com/tetsuji16/ProjectLibre/pull/619) merged as
  `a629d8d2ea6057b314c2b3d6147bbe2ad09aa508` after full CI success; typed the
  OpenProj-derived WBS child cache and implementation-list conversion.
- PR [#620](https://github.com/tetsuji16/ProjectLibre/pull/620) merged as
  `718c3f6e14df0a5b87c8eb475e32792bcc199774` after full CI success; modernized
  four OpenProj-derived summary-task child traversals.
- PR [#621](https://github.com/tetsuji16/ProjectLibre/pull/621) merged as
  `42cfaeffda1a29d4e006b274fe7f41e5ca8889a0` after full CI success; modernized
  simple assignment iteration and added focused regressions.
- PR [#622](https://github.com/tetsuji16/ProjectLibre/pull/622) merged as
  `a113f4b40a254f2de597179c08f041acb9f3c2dc` after full CI success; modernized
  four more assignment traversals and added an earliest-stop regression.
- PR [#623](https://github.com/tetsuji16/ProjectLibre/pull/623) merged as
  `e0a8272bf6b5e07e09d78203cb5875ba58c5fe88` after full CI success; modernized
  the assignment percent-complete propagation loop.
- PR [#624](https://github.com/tetsuji16/ProjectLibre/pull/624) merged as
  `67ee7014cac5bd0b2c4c9181a847eb9ca378bf69` after full CI success; modernized
  task resume iteration and added a focused setter/query regression.
- PR [#625](https://github.com/tetsuji16/ProjectLibre/pull/625) merged as
  `d140852a35c8bca3d69c86fbf766a65e6581c950` after full CI success; typed the
  interval-value table and verified core, downstream compilation, and POD round-trip.
- PR [#626](https://github.com/tetsuji16/ProjectLibre/pull/626) merged as
  `b825b936edf55f3d8090b72e00121bf11ac6483d` after full CI success; typed
  configured time-scale collections and added toggle/clone regressions.
- PR [#627](https://github.com/tetsuji16/ProjectLibre/pull/627) merged as
  `23d396de3cea75e5b39a9568de2ad8822d70c58f` after full CI success; modernized
  task hierarchy/predecessor traversal and added ordering coverage.
- PR [#628](https://github.com/tetsuji16/ProjectLibre/pull/628) merged as
  `e2db4e5ce15cbc76ccf8788b438e78c71f698157` after full CI success; modernized
  task invalidation and free-slack traversal.
- PR [#629](https://github.com/tetsuji16/ProjectLibre/pull/629) merged as
  `795aa6f6b7e4759f48520b8842a61cda9952ad71` after full CI success; modernized
  composite cache-event diff generation and preserved source event payloads.
- PR [#630](https://github.com/tetsuji16/ProjectLibre/pull/630) merged as
  `dfba55c94a26afd3c89042ebb02c3294c142d790` after a successful CI rerun;
  typed the selected time-spreadsheet field-array event payload.
- PR [#631](https://github.com/tetsuji16/ProjectLibre/pull/631) merged as
  `e6ece1c1e38f20e0d02b5e656580bce9214979d4` after a successful failed-job
  rerun; typed cache-event payloads and interval iterators.
- PR [#632](https://github.com/tetsuji16/ProjectLibre/pull/632) merged as
  `9805ecaf2ab4753bd860172cb2f1742edd890b24` after full CI success; typed the
  graph event node-payload contract.

## Inventory caveat

`docs/legal/license-provenance.csv` now records current `microproject_*` paths.
The ledger generator normalizes the `com.microproject` package when comparing
against the OpenProj tree, and the reconciliation tool accepts both current and
legacy module names. Running `python scripts/audit/openproj_java25_inventory.py --ref HEAD`
reports 168 core production-Java candidates, all with normalized content
matching the OpenProj source. Mapping tests cover the current module and package
paths. This is path/content evidence only; it is not a caller audit, hunk-level
provenance review, or legal conclusion.

The following pinned-checkpoint counts are historical. Re-running the inventory
at the latest integrated checkpoint (`origin/master` =
`7860a5aee7cb296bf75596f98860c871ee11d457`) reports 280 ledger rows: 183
normalized matches, 80 content-different files, and 17 absent mapped paths.
The absent paths include two active relocations, 11 files absent from the
current module graph, and the four intentionally deleted unreferenced classes
`PeakUnitsFunctor`, `NumericMaximum`, `NodeFieldList`, and `TestFilter`. These
are still path/content counts, not proof that every remaining file is active or
eligible.

At the latest integrated checkpoint for the 2026-09-25 follow-up tranche
(`origin/master` = `a33b1d76ebb0a8e28592f3eb3db1787a960b499f`), rerunning the
inventory reports 280 ledger rows: 180 normalized matches, 83 content-different
files, and 17 absent mapped paths. The absent set still includes two active
relocations and four previously verified removals; the remaining paths must
not be inferred dead from path absence alone. These path/content metrics do not
measure completion; the hunk-level and production-caller audit remains open.

At local checkpoint `74e1a7212b9c8fd6ff5da1a9b8bcc563d4279b3a`, running
`python scripts/audit/openproj_java25_inventory.py --ref HEAD --details`
reports 280 ledger candidates: 180 normalized matches, 83 content-different
files, and 17 absent mapped paths. This local checkpoint includes modernization
commits after the latest integrated checkpoint; the counts remain path/content
evidence only.

These are path/content reconciliation results, not an active-caller or hunk
provenance audit. A matching file may contain a narrow fork delta; a differing
file may still contain eligible OpenProj-origin hunks. The results do **not**
prove that each source has a production caller or is safe to modernize. The
ledger's normalized match is not a legal conclusion. Inspect origin at
hunk/responsibility level, search callers (including dynamic configuration and
serialization), and classify compatibility-sensitive code before counting a
candidate as eligible.

## Completed, source-confirmed work segments

The following active core classes have relevant code in the OpenProj baseline
under `openproj_core/src/com/projity/`. Only the modified responsibility is
claimed as reviewed; untouched hunks in these classes remain out of scope.

| Area | Classes / responsibility | Outcome |
|---|---|---|
| WBS child caches | `Task.getWbsChildrenNodes`, `setWbsChildrenNodes`, `getWbsChildrenTasks` | PR #619 typed the node cache as `Collection<Node>`, exposed returned implementations as `List<Object>` (matching `NodeList`), and removed a redundant cast; caller/test setup search found cached entries are node wrappers. These exact raw boundaries occur in the OpenProj-derived source ([source excerpt](https://www.javatips.net/api/ProjectLibre-master/openproj_core/src/com/projity/pm/task/Task.java#L2465-L2486)). |
| Summary-task WBS traversals | `NormalTask.buildReverseQuery`, `updateEstimatedStatus`, `assignActualDatesFromChildren`, `getEarliestStop` | Typed child-node collections and replaced raw iterator/cast loops with enhanced-for and pattern matching, retaining traversal order and the existing NormalTask/Schedule filters. Each exact raw loop is present in the OpenProj baseline `d2fa3c20a`; `:microproject_core:test` passed. |
| Assignment iteration | `NormalTask.isAssignedToMe`, interval `setWork`, `getMostLoadedAssignmentUnits`, `adjustRemainingDuration`, `adjustRemainingUnits`, `adjustRemainingWork`, `moveRemainingToDate`, `getEarliestStop` | Replaced raw `Iterator`/cast loops with enhanced-for traversal over the already typed `AssociationList` iterator, retaining assignment casts, traversal order, and the labor-only check. No scheduling rule or mutation semantics changed. Focused tests create an actual labor assignment and verify the most-loaded-units query, and verify a leaf task's earliest stop against its assignment. |
| HasAssignments iteration | `HasAssignmentsImpl` constructor, `setScheduleForAssignments`, `buildReverseQuery`, `collectIntervals`, `calcDataBetween`, `extractOppositeList`, `hasLaborAssignment`, `invalidateAssignmentCalendars`, `hasActiveAssignment`, `getEarliestAssignmentStart`, shared `findAssignment` scan, and `copyAssignments` | Confirmed the legacy iterator/cast loops in the source-backed methods against `d2fa3c20a:openproj_core/src/com/projity/pm/assignment/HasAssignmentsImpl.java`. Read-only traversals use enhanced-for while preserving cast behavior, encounter order, early returns, mutation/invalidation order, and `Long.MAX_VALUE` for the empty earliest-start query. `updateAssignment` retains `ListIterator` because it replaces an element via `set`. `extractOppositeList` retains the current fork's `Assignment` filter. The resource/task lookup variants share one `AssignmentMatcher` scan. `HasAssignmentsImplTest` and the full `:microproject_core:test` suite passed. |
| Time-distributed value aggregation | `TimeDistributedDataConsolidator` | All aggregation loops correspond to OpenProj source `d2fa3c20a:openproj_core/src/com/projity/pm/assignment/TimeDistributedDataConsolidator.java`. Replaced explicit typed iterators with enhanced-for while preserving each metric call, accumulation order, labor-only filtering, and existing numeric behavior. `TimeDistributedDataConsolidatorTest` covers sum, empty input, and labor filtering; focused and full core tests passed. |
| Assignment removal batches | `AssignmentService.remove(Collection, Collection)` and `remove(Collection, Object, boolean)` | The source hunks match `d2fa3c20a:openproj_core/src/com/projity/pm/assignment/AssignmentService.java`. Enhanced-for retains the raw collection boundary, order, and snapshot/removal behavior. `NormalTask.cleanUp` first copies snapshot assignments into a detached `LinkedList` before batch removal. `AssignmentServiceTest` verifies both that snapshot copies preserve source contents/order and that a detached batch is fully removed from task/resource links. Focused and full core tests passed. |
| Assignment progress propagation | `NormalTask.updateAssignmentPercentComplete` | Replaced the raw iterator and per-element cast-after-next with enhanced-for traversal over the existing typed association iterator. The existing progress synchronization test exercises the task-to-assignment update and remains green. |
| Assignment resume access | `NormalTask.getResume`, `setResume` | Replaced paired raw iterator loops with enhanced-for traversal over the typed association iterator. A focused setter/query test verifies task resume aggregation agrees with its assignment. |
| Interval-value table traversal | `ValueObjectForIntervalTable` | Typed the internal `ArrayList<ValueObjectForInterval>` and its list view, replaced raw iterator/casts with enhanced-for and typed indexed access, and modernized clone ownership traversal. Kept raw `getValueObjects`/serialization constructor descriptors and serialized ArrayList contents as compatibility adapters. Added bounds/clone-rebinding regression coverage; core tests, application/exchange/UI/reports compilation, and `PodRoundTripTest` (including the resource-calendar round-trip) passed. The transformed traversal and clone hunks correspond to baseline `d2fa3c20a`; unrelated post-fork `findActive` code is not claimed as OpenProj work. |
| Time-scale collections | `TimeScaleManager` | Typed the configured scale list, removed casts/raw iterator loops, and used enhanced-for for width toggling and defensive instance cloning. Config-Digester entry points and the collection implementation/order are unchanged. Added tests for all-scale width toggling and independent cloned scales. The exact raw loops occur in baseline `d2fa3c20a`; downstream UI compilation checks consumers. |
| Task hierarchy/predecessor traversal | `Task.isWbsParent`, `arrangeTask`, `arrangeChildren` | Typed WBS child iteration as `Node`, traversed predecessor associations with enhanced-for, and retained the explicit `Dependency` cast and disabled-dependency behavior. Added predecessor-before-task ordering coverage alongside the existing summary-marker ordering test. These exact loops are present in baseline `d2fa3c20a`. |
| Task invalidation/slack traversal | `Task.markDependentTasks`, `getFreeSlack` | Typed successor association traversal and WBS child iteration, using pattern matching for task implementations. Reused `TaskDependencyInvalidationTest`'s affected-closure assertions; no scheduling invalidation behavior changed. Focused invalidation/dependency tests, full core suite, and application/exchange/UI/reports compilation passed. These traversals match the OpenProj baseline `d2fa3c20a`. |
| Project root-node query | `Project.getRootNodes` | PR #618 typed `List<Task>` input and `List<Node>` output and used enhanced-for. The implementation corresponds to the OpenProj-derived source excerpt ([source excerpt](https://www.javatips.net/api/ProjectLibre-master/openproj_core/src/com/projity/pm/task/Project.java#L3420-L3427)); no production callers were found, so a focused contract test was added. |
| Filter iterator API | `NodeFilter.filteredListIterator` / `filteredIterator` | PR #617 typed the input and output iterator references as wildcards; Apache Commons raw API remains at the adapter edge. Corresponding raw methods are present in the OpenProj-derived source ([source excerpt](https://www.javatips.net/api/ProjectLibre-master/openproj_core/src/com/projity/grouping/core/transform/filtering/NodeFilter.java#L2088-L2095)). |
| CSS style hierarchy contract | `HasCssStyle.getHierarchy` | PR #616 changed the raw collection return to `Collection<?>`, preserving erasure and leaving the heterogeneous element contract unspecified rather than guessing a concrete type. The interface is a normalized-content match to OpenProj; caller search found the `TimesheetAssignment` implementation and no active consumer of this method. |
| General options singleton | `GeneralOption.getInstance` | PR #615 replaced racy lazy initialization with class-initialized `static final`; public construction and option defaults remain unchanged. The ledger marks the file's normalized contents as matching the OpenProj baseline. |
| Hierarchy indent traversal | `MutableNodeHierarchy.internalIndent` | PR #613 typed its selected-node and temporary void-node lists and iterators without changing traversal order. The corresponding raw traversal appears in the ProjectLibre mirror's OpenProj-derived source ([source excerpt](https://www.javatips.net/api/ProjectLibre-master/openproj_core/src/com/projity/grouping/core/hierarchy/MutableNodeHierarchy.java#L2546-L2635)); this comparison confirms code correspondence, not a legal conclusion. |
| Hierarchy descendant event traversal | `MutableNodeHierarchy.addDescendants(Node, List<Node>)` | Changed the raw preorder `Enumeration` to `Enumeration<?>`, retaining the same NodeBridge preorder traversal, cast behavior, and append order. The method matches `d2fa3c20a:openproj_core/src/com/projity/grouping/core/hierarchy/MutableNodeHierarchy.java`; its list overload is used by active hierarchy insert/remove event paths. |
| Subproject-level propagation traversal | `MutableNodeHierarchy.setSubprojectLevel` | Changed its child enumeration from raw `Enumeration` to `Enumeration<?>`, preserving recursive order and the existing Node cast. The method is source-identical to `d2fa3c20a:openproj_core/src/com/projity/grouping/core/hierarchy/MutableNodeHierarchy.java` and is called by active hierarchy add/paste paths. |
| Hierarchy removal and move event traversals | `MutableNodeHierarchy.extractSameProjectBranch`, `removeSubTree`, `move` | Typed the raw child, postorder, and preorder enumerations as `Enumeration<?>`; retained recursion/order, cast behavior, and event payload construction. The modified traversals match baseline `d2fa3c20a`; active callers include node removal, move, and descendant event collection. |
| Subproject placeholder traversal | `MutableNodeHierarchy.checkSubprojectEndVoidNodes` | Changed the child enumeration to `Enumeration<?>`, preserving recursive traversal and placeholder counting order. The loop matches `d2fa3c20a`; `DefaultNodeModel` calls `checkEndVoidNodes` after active hierarchy mutations and during outline initialization. |
| Hierarchy node-list contract | `NodeHierarchy.add`, `paste`, `remove`, `indent`; `MutableNodeHierarchy` implementations | Replaced raw `List` boundaries with `List<Node>` across the OpenProj-derived hierarchy contract and routed implementations. Add, paste, and remove now use enhanced-for loops without per-node casts; runtime erasure and traversal/mutation order are unchanged. The fork-added `relocate` method remains outside this modernization. Full core tests and downstream module compilation passed. |
| Project open/close identifier sets | `ProjectFactory.loadingProjects`, `closingProjects`, `getOpenOrLoadingProjects`, `addClosingProjects` | Typed both ID sets and the returned ID set as `Set<Long>`, and narrowed the batch-close input to `Collection<Long>`. Values are project IDs inserted as `Long`; exact declarations and APIs correspond to OpenProj baseline `d2fa3c20a`. Added coverage that loading IDs appear until closing and that close completion/removal clears the state. |
| Project close collections | `ProjectFactory.getCloseProjectsOnServerJob`, `getRemoveProjectJob` | Typed the close-project input as `Collection<Project>`, its local/server partitions as diamond-inferred lists, the removal snapshot as `List<Node>`, and replaced its non-mutating raw iterator with enhanced-for. Verified the method and traversal against `d2fa3c20a`; the project branch close path is the active caller. Ordering, read-only filtering, session selection, and per-node cleanup are unchanged. |
| Dependency-cycle traversal visited set | `Task.dependsOn` | Typed the recursion guard as `Set<Task>` and its `HashSet` construction with diamond inference. The declaration and recursion match OpenProj baseline `d2fa3c20a`; production dependency validation and predecessor-tree callers exercise the public method. Cycle detection order and identity/equality behavior are unchanged. |
| Single-node hierarchy insertion | `DefaultNodeModel.add(Node, Node, int, int)` | Typed its one-element staging list as `ArrayList<Node>` with diamond inference, then routes through the shared `add(Node, List<Node>, ...)` contract. The helper matches baseline `d2fa3c20a`; capacity, insertion order, event and undo handling are unchanged. Full core tests passed. |
| Resource-pool collection locals | `ResourcePool.userResources`, `setLocalParent(Node, Node)` | Typed the pre-sized user-resource result list as `ArrayList<Resource>` and the single-node hierarchy-move list as `ArrayList<Node>`. Both loops and destinations match baseline `d2fa3c20a`; resource filtering and local-parent move behavior are unchanged. |
| Single-node hierarchy removal | `DefaultNodeModel.remove(Node, int, boolean, boolean)` | Typed its one-element staging list as `ArrayList<Node>` with diamond inference; the existing overload still dispatches to the shared `List<Node>` removal path. The helper matches baseline `d2fa3c20a`; removal filtering, dependency cleanup, Undo, and event behavior are unchanged. |
| Nested transform factory staging | `CommonTransformFactory.setProperties`, `addFactory` | Typed the transform staging list as `ArrayList<Object>` using its existing `List<Object>` contract, iterated the typed inherited list with enhanced-for while retaining the factory cast, and added diamond inference when initializing the inherited list. The methods match baseline `d2fa3c20a`; child transform order and failure behavior remain unchanged. |
| Personal work-contour staging lists | `PersonalContour` bucket split, insertion, adjustment, and range operations | Typed the private bucket lists as `ArrayList<PersonalContourBucket>` (or `ArrayList<AbstractContourBucket>` where the public insertion accepts that base type), and narrowed `getInstance` to `Collection<? extends AbstractContourBucket>`. These operations match baseline `d2fa3c20a`; added focused tests that assert exact bucket durations, units, and order after interval replacement and bucket insertion. |
| Calendar intervals | `WorkDay`, `WorkRange`, `WorkingHours`, `WorkWeek`, `WorkingCalendar`, `CalendarService`, `CalendarDefinition`, `Interval`, `CalendarEvent` | Typed collection / clone / comparison modernization; removed the unused calendar-cache list; fixed incorrect working-day intersection, lost overtime state in a range constructor, long-comparison overflow, equality asymmetry, and a strong-reference calendar-cache registry leak. |
| Duration and rates | `Duration`, `DurationFormat`, `Rate`, `RateFormat`, `PercentFormat` | Pattern matching and switch expressions; removed dead duration conversion calculations and redundant string copying; made encoding masks and formatter mode state immutable; fixed parse-position end-of-input exceptions and fractional/large-rate comparison. |
| Distribution identity | `DistributionData` | Pattern matching and hash/equality alignment with the active `DistributionComparator` key, including `projectId`; added focused identity/payload tests. |
| Personal contour | `PersonalContourMaker` | Replaced raw collection types with `List<PersonalContourBucket>` / typed `Collection`; used pattern matching for bucket narrowing. |
| Scripting configuration | `ScriptConfiguration` | Replaced raw `Set` with `Set<String>` and diamond construction; added configured/unlisted class-name behavior coverage. |
| Project extra fields | `HasExtraFields`, `HasExtraFieldsImpl`, `Project` | Typed the extensible custom-field map as `Map<String, Object>` across the shared API and Project owner. Preserved heterogeneous values, LinkedHashMap insertion order, the erased Map method descriptors, and existing serializer copy semantics. |
| Dynamic field lookup | `Finder`, `Field`, `DynamicSelect`, `DependencyFormat` | Replaced raw `Collection` parameters and locals with `Collection<?>` across the reflection-backed finder contract. Generic erasure and configured reflection signature are unchanged; Finder regression test and all direct downstream module compilations passed. |
| Grouping placeholder cleanup | `VoidNodeImpl`, `GroupNodeImpl` | Typed the existing `LinkedList` field/API as `LinkedList<Object>` without changing its erased return descriptor, and removed only fully commented-out group-field/summary code. Full core tests and application, exchange, UI, and reports compilation passed. The void/group implementations remain active compatibility nodes and were not removed. |
| Grouping XML configuration | `NodeGrouper` | Typed the XML-populated group list and `addGroup`/getter API as `NodeGroup`; verified insertion order and transform relationship, then compiled the UI consumer. |
| Assignment composition filtering | `AssignmentCompositionFilter` | Applied pattern matching to the source-exact OpenProj Assignment branch; added delegation tests for both Assignment-to-Resource composition and unchanged non-Assignment nodes. The `Filter.WhoDoesWhatReport` XML configuration remains unchanged. |
| Assignment exclusion filtering | `NotAssignmentFilter` | Replaced racy mutable lazy singleton fields with immutable `static final` instances and made the mode flag final; tests verify stable, distinct standard/writable instances and task acceptance. |
| Resource team filtering | `ResourceInTeamFilter` | Applied pattern matching to the two OpenProj-origin type checks; preserved the later `Consumer` callback fork delta. A newly reproduced ClassCastException for an `AssignmentEntry` wrapping non-`ResourceImpl` `HasAssignments` is now a clean filter rejection; existing resource paths and change-only notification behavior remain covered. |
| Timesheet aggregation | `TimesheetHelper` | Replaced raw iterators with enhanced-for loops and wildcard collection parameters; retained per-element casts required by `AssociationList`'s `Association` declaration and preserved processing/early-return behavior. Expanded existing timesheet aggregation tests. |
| Object event delivery and pooling | `ObjectEvent`, `ObjectEventManager` | Typed assignment iteration; fixed a pooled-event stale-state bug by resetting `field`/`info`, and now recycles in `finally` when a listener throws. Regression test asserts exception propagation, object reuse, and cleared state. |
| Script field arrays | `FieldArrayUtil` | Typed the iterator over `SpreadSheetFieldArray`'s `Field` elements; tests verify excluded IDs by category and that filtering mutates only the clone, not the configured source. |
| Selected time-spreadsheet fields event | `FieldArrayEvent` | Confirmed the payload is an `ArrayList<Field>` at its active producer (`TimeSpreadSheetModel.getSelectedFieldArray`) and consumer (`UsageDetailView`). Typed the stored value, constructor, getter, and setter without changing the erased `ArrayList` descriptor or the existing shared-reference behavior. A focused event test verifies constructor/setter reference identity. |
| Cache event payload and interval traversal | `CacheEvent` | Typed node/interval list boundaries as `List<?>` and removal/insertion `ListIterator` loops without changing list descriptors, shared-reference semantics, callback contract, or traversal order. Focused tests assert removals visit intervals in reverse while insertions visit forward, and node-list replacement retains the supplied reference. |
| Graph update event payload | `GraphEvent` | Typed the node payload as `List<?>` through its stored field, constructor, getter, and setter. GraphModel and Graph consumers use the erased List contract; focused test preserves the existing shared-reference behavior. |
| Spreadsheet transfer action values | `NodeListTransfertAction.map` | Confirmed the OpenProj-matching action wrapper is created only by `NodeListTransferHandler` for cut/copy/paste. Typed its local action-value override map as `Map<String, Object>` with diamond inference, preserving local-key precedence (including stored null), delegate fallback, enabled-state delegation, and spreadsheet event-source remapping. Focused tests cover these adapter contracts; the physical routes and underlying TransferHandler are unchanged. |
| Session save delegation | `AbstractSession` | Replaced the raw diamond for its `List<Project>` delegation with `new ArrayList<>()`; a recording session test verifies one-item and options-preserving delegation. |
| Typed node iteration | `TypedNodeIterator` | Added `Iterator<Object>` / `Class<?>` generics and fixed lookahead removal deleting the wrong selected node, null-implementation dereference, and exhausted `next()` contract. Removal rewinds list-backed selections by source index; non-list removal explicitly throws `UnsupportedOperationException` rather than risking a wrong deletion. |
| Scheduling field notifications | `AlgorithmFieldUpdater`, `CriticalPathFields` | Typed input/output sets and iteration as `Field`, including the subclass's shared cached sets; core tests and all direct downstream module compilations passed. |
| External task resolution | `ExternalTaskManager` | Typed the manager-owned external-task list and iterator; retained `AssociationList`'s actual `Association` iterator contract and the existing cast semantics for dependencies. |
| Resource pool registry | `ResourcePoolFactory` | Typed the existing `ArrayList` without changing its erased public return descriptor; removed unreferenced private `removePool` and unused `name` state after repository-wide caller search. |
| Schedule undo snapshots | `ScheduleBackupEdit` | Typed the backup map, replaced raw iterator traversal with enhanced-for/`Map.forEach`, used `instanceof` pattern matching for collection input, and made captured source immutable; regression tests cover singleton and collection snapshots plus undo/redo behavior. |
| Hierarchy paste undo | `NodePasteEdit` | Typed the captured children as `List<?>` and the extracted removal roots as `List<Node>` while preserving erased API descriptors; an integration regression verifies undo/redo restores node identity and insertion order. |
| Snapshot clear undo | `ClearSnapshotEdit` | Typed selection and backup detail containers as wildcard lists/collections; extended the project snapshot test to verify clear → undo restore → redo clear through the real undo controller. |
| Selection membership filter | `BelongsToCollectionFilter` | Replaced raw membership collection declarations and setter parameters with `Collection<?>`; caller erasure is unchanged, and tests cover selected/nonselected implementations plus callback suppression/notification. |
| Range query predicate | `SelectFrom.whereInRange` | Confirmed this method hunk is unchanged from the OpenProj source despite the file-level fork delta; replaced its cast-after-`instanceof` with pattern matching and added tests for repeated range intersection and invalid reverse ranges. |
| Printer media-size lookup | `ExtendedPageFormat.getDefaultMediaSizeName` | Confirmed the branch is unchanged from OpenProj; used a Java pattern binding and added characterization for `MediaSizeName` acceptance and non-size `Media` rejection. The existing printer-boundary regression remains in the same focused test class. |
| Resource parent identity | `ResourceImpl.getParentId` | Confirmed the parent `HasKey` type-check hunk is unchanged from OpenProj; applied pattern matching and added a resource-outline regression for the no-key parent fallback (`0`). |
| Reverse cost query aggregation | `AssignmentFieldClosureCollection.getFixedValue` | Confirmed this aggregation hunk matches OpenProj; replaced cast-after-`instanceof` with pattern matching and tested that only `CostFunctor` fixed values contribute. Fork-specific `Consumer` chain behavior was left untouched. |
| WBS task ordering | `Task.arrangeChildren` | Confirmed the child traversal hunk matches OpenProj; changed raw iterator/cast control flow to enhanced-for plus pattern matching, with an ordering regression for parent-begin / child / parent-end task references. |
| Summary dependency cycle detection | `Task.dependsOn` child-predecessor traversal | Confirmed the child-task type narrowing matches OpenProj; replaced the iterator/cast branch with enhanced-for and pattern matching. A focused dependency test verifies an indirect parent/child cycle is rejected without adding a partial edge. |
| Weighted completion calculation | `ScheduleUtil.percentCompleteClosureInstance` | Confirmed both schedule dispatch hunks match OpenProj; replaced cast-after-`instanceof` with pattern bindings and tested weighted percent calculation plus ignored non-schedule input. |
| Common-key equality | `HasCommonKeyImpl.equals` | Confirmed the equality hunk matches OpenProj; replaced the type check/cast with a pattern binding and expanded existing unique-ID equality/hash tests with null and unrelated-object inputs. Existing fork hashCode fix remains intact. |
| Schedule bar interval boundary | `BarClosure.accept` | Confirmed the file is normalized-identical to OpenProj; replaced the `ScheduleWindow` check/cast with pattern binding and tested both the resume/unsplit stop adjustment and unchanged non-window intervals. |
| Earned-value schedule offsets | `EarnedValueCalculator.getStartOffset` / `getFinishOffset` | Replaced repeated interface checks and casts with pattern bindings while preserving the zero result when required schedule fields are absent; added a focused regression for that fallback. |
| Printer printable-area bounds | `ExtendedPageFormat.adaptMediaPrintableArea` | Fixed an OpenProj-origin bug where all four upper bounds were read from the requested area instead of the printer-supported maximum. A focused test failed before the fix and verifies all four bounds after clamping. |
| Group sorter resolution | `NodeGroup.getSorter` | Replaced the remaining cast-after-`instanceof` branch with a Java 25 pattern binding; retained the existing null fallback and verified the grouping core test plus the UI consumer compilation. |
| Assignment update notification | `ObjectEventManager.fireUpdateEvent` | Replaced the raw iterator and cast-after-`instanceof` with a typed enhanced-for loop and pattern binding. A focused regression verifies both the task event and propagation to its assignment; the original OpenProj bug-258 behavior is retained. |
| Date cell rendering | `DateRenderer.getTableCellRendererComponent` | Replaced the redundant null-plus-`instanceof` check and cast with a pattern binding; retained the same date-formatting and superclass-rendering path. UI compilation passed; no Robot scenario was added for this behavior-preserving local refactor. |
| Split-view component narrowing | `MainView` split synchronization methods | Replaced the `SplittedView` check/cast pairs with pattern bindings in parent assignment, divider synchronization, and synchronization setup/removal; simplified the synchronizability predicate because `instanceof` already rejects null. Also replaced the stale captured-component null branch with a check of the current split components. `SplittedViewLifecycleTest` and UI compilation passed; no command or layout contract changed. |
| Deep child traversal | `DeepChildWalker.accept` | Replaced the raw collection plus iterator utility call with `List<?>` and enhanced-for recursion; preserved parent-first and child-list order. A proxy-backed focused test verifies the recursive visitation order. |
| Deep hierarchy search | `DeepChildSearcher.accept` | Replaced the raw child collection and iterator utility with `List<?>` and enhanced-for recursion, stopping sibling iteration once the first result is found. A proxy-backed test verifies the first depth-first match. |
| Leaf and shallow summary traversal | `LeafWalker.accept`, `ShallowChildWalker.accept` | Replaced raw child collections and iterator-helper calls with `List<?>` and enhanced-for traversal while preserving leaf-only and immediate-child semantics. The leaf contract test also exposed and fixed a defect: `LeafWalker` accepted any `Consumer<Object>` but only dispatched to `SummaryVisitor`; it now invokes the configured closure for leaf nodes. |
| Non-summary descendant traversal | `CountNonsummariesWalker.accept` | Typed the local model-child collection as `List<?>` without widening the legacy `NodeModel` API. A focused regression verifies depth-first order, summary-node exclusion while still traversing summary children, and void-node exclusion; the focused test, full core suite, and application/exchange/UI/reports compilation passed. |
| Selected hierarchy roots | `HierarchyUtils.extractParents` | Typed the selection and output collection variance while preserving the erased `Collection` signature; replaced raw iterator/casts with enhanced-for traversal and preserved selection order. A focused regression verifies selected descendants collapse under the highest selected ancestor while independent roots remain. The focused test, full core suite, and downstream compilation passed. |
| Hierarchy preorder indexing, traversal, and navigation | `AbstractMutableNodeHierarchy.getIndexOfNode`, `visitAll`, `visitAllLevelOrder`, `visitLeaves`, `getNext`, `getPrevious` | Replaced raw child collections/iterators/enumeration with `List<?>`, `Enumeration<?>`, and enhanced-for traversal; removed redundant iterator reinitialization without changing visitation phases. Focused real-hierarchy tests exposed/fixed swapped recursive arguments that made descendant indexes return `-1`, and root-sentinel leakage at the previous-node boundary. Tests verify root/parent/child/sibling indexes, preorder and level-order visitation, next/previous relationships, and first/last boundaries. Focused and full core tests plus downstream compilation passed. |
| Shallow hierarchy iterator | `AbstractMutableNodeHierarchy.ShallowPreorderIterator` | Replaced uninitialized raw `Stack` state with initialized typed `Deque`/`ArrayDeque` state, typed enumeration frames, and corrected the private iterator's misspelled name. Fixed exhausted `next()` to follow the `Iterator` contract with `NoSuchElementException`. Regression tests cover root inclusion, depth caps, root exclusion, traversal order, and exhaustion. Focused and full core tests plus downstream compilation passed. |
| Hierarchy diagnostic dump | `AbstractMutableNodeHierarchy.dump` | Typed the child list as `List<?>`, replaced iterator/cast traversal with enhanced-for, and narrowed the private callback to `Consumer<String>` while converting anonymous consumers to lambdas. A focused test verifies depth-first content, indentation, and newline output. Full core tests and downstream compilation passed. |
| MSPDI export association traversal | `MSPDISerializer` snapshot-assignment and predecessor loops | Confirmed both raw `AssociationList`/`Iterator` loops match the OpenProj baseline at `d2fa3c20` and replaced them with enhanced-for loops over `Association` while preserving the existing casts/processing order. The subproject-outline iterator in the same method is a fork-specific path and is not counted as source-proven OpenProj work. `MpxProjectConverterTest`, full exchange tests, and application/UI/reports compilation passed. |
| Shared exchange linker state | `linker.Linker` transformation map, result collection, iterator, and outline void-node set | Compared declarations against OpenProj baseline `d2fa3c20`; typed internal containers with diamond operators and `Set<Node>`, and exposed heterogeneous serializer results as `Map<?, ?>`/`Collection<?>` without changing erased method descriptors. Corrected `hasNext()` to express the null-safe iterator contract. Added a focused test verifying initialization clears prior mappings/results and reconstructs typed results. Full exchange tests and application/UI/reports compilation passed. Serializer-specific unchecked casts remain only at the boundary where each concrete format knows its result type. |
| MSPDI calendar-option restoration | `MSPDISerializer.saveTasks` dependency serialization scope | Modernization inspection found global `CalendarOption` was restored only on normal return, so a failure during task/dependency output leaked the temporary MPXJ defaults to later operations. Wrapped the affected scope in a `Callable`-based `try/finally` helper and added a regression that throws from the operation and asserts the exact caller option instance is restored. Focused MSPDI atomic-save/converter tests and full `:microproject_exchange:test` passed. This fork-specific region is tracked as a bug fix, not claimed as an OpenProj syntax modernization. |
| Collaboration link identity | `LinkData.equals` | Replaced the cast-after-`instanceof` with a pattern binding and expanded the existing equality contract test for null, unrelated values, and a different serialized-data subtype. |
| Filtered graphic-node iteration | `GeneralFilteredIterator` | Typed the iterator input as `Iterator<?>` and replaced the GraphicNode check/cast with a pattern binding. A headless focused test confirms node-based iteration still returns the underlying Node. The Apache Commons `FilterIterator` base remains raw because it is an external non-generic type. |
| Shared node-filter collection | `NodeFilter.filterList`, `filterArray` | Confirmed these methods match the OpenProj baseline at `d2fa3c20`; made `filterList` preserve its input element type with `<T> List<T>` and a typed iterator, and used a diamond-inferred `ArrayList<Object>` for array filtering. Preserved in-place removal, list identity, order, null behavior, and erased signatures. Regression coverage verifies typed assignment, retained order, and in-place mutation; UI caller compilation passed. |
| Filtered hierarchy facade cleanup | `FilteredNodeHierarchy` | Confirmed the removed blocks are commented-out OpenProj delegation methods, not declarations or active compatibility surface. Removed obsolete stubs for unsupported/retired overloads; left live hierarchy delegation and behavior untouched. No runtime API or format boundary changed. |
| Node-list visitor and implementation conversion | `NodeList.getType`, `accept`, `nodeListToImplList` | Confirmed the raw signatures/casts match OpenProj baseline `d2fa3c20`; typed `getType` as `Class<? extends Node>`, the visitor iterator as `Iterator<? extends Node>`, and collection inputs as `Collection<? extends Node>`, removing redundant casts while preserving erased signatures, traversal order, filter behavior, and null-to-empty conversion. Typed the UI selection event producer/consumer as Node lists to carry the invariant through the actual call chain and removed its now-obsolete unchecked-warning suppression. Added regression coverage for visitor order, implementation identity, runtime type, and null fallback. Focused test, full core suite, and UI compilation passed. |
| Association-list text contract | `AssociationListFormat.parseObject`, `format` | Confirmed the parser/formatter loops match OpenProj baseline `d2fa3c20`; typed formatting iteration, made the wrapped formatter immutable/private, and removed a stale comment incorrectly describing the returned type. Corrected two reproduced Format-contract defects: parsing now honors the incoming `ParsePosition`, advances it on success and sets an error index on failure; skipped default associations no longer cause leading/trailing separators. Added focused tests for partial-input parsing, failure positions, and mixed/default formatting. Focused `AssociationListFormatTest`, full `:microproject_core:test`, and `:microproject_ui:compileJava` passed. The visible text correction was asserted at the formatter boundary; no route, layout, or model persistence changed, so GUI/Robot runs were not repeated. |
| Earliest-ending interval evaluation | `IntervalGeneratorSet.earliestEndingGenerator`, `evaluate` | Confirmed the state flag and branch match OpenProj baseline `d2fa3c20`; `sameEarliestEnding` was initialized `true` and never set `false`, making its alternate branch unreachable. Removed the dead flag and unified evaluation over all generators at the minimum end, preserving evaluation of every tied generator and no evaluation of later-ending generators. A new `Long.MAX_VALUE` endpoint regression failed before the fix because the valid generator was mistaken for an empty set; selecting the first candidate when no result exists fixed it. Added unique-end and tied-end regressions, including no short-circuit when one tied evaluation is false. Focused `IntervalGeneratorContractTest`, full core suite, and application/exchange/UI/reports compilation passed after the fix. |
| Time iterator generator state | `TimeIteratorGenerator` | Compared against OpenProj baseline `d2fa3c20`; removed unused `currentEnd` and `visitor` fields and the source-identical no-op `compareTo(Object)` (the class does not implement `Comparable`). Repository search found no callers or dynamic registrations for the removed members; the core module has no `api` dependencies or separately published API contract. Made the owned iterator final and mutable implementation state private. Added a deterministic hourly-interval regression for current interval, index, bounds, advancement, and exhaustion. Focused test, full `:microproject_core:test`, and application/exchange/UI/reports `compileJava` all passed. No GUI command, state presentation, or layout changed, so no GUI/Robot run was warranted. |
| Interval-generator comparison stubs | `CollectionIntervalGenerator`, `RangeIntervalGenerator` | Confirmed both `compareTo(Object)` stubs match OpenProj baseline `d2fa3c20`; each always returns zero, neither class implements `Comparable`, and repository-wide searches found no direct or reflective use. Removed the dead methods. Added a focused collection-generator traversal regression covering active interval, gap, next interval, and exhaustion; existing range-generator contract/query tests cover its active behavior. Focused tests, full `:microproject_core:test`, and application/exchange/UI/reports `compileJava` all passed. |
| Calculated-value point ordering and series | `algorithm.buffer.Point`, `GroupedCalculatedValues` | Confirmed the OpenProj-origin ordering implementation was used by `GroupedCalculatedValues` but narrowed raw `Comparable` / `Comparator` values with casts and compared `long` dates by subtraction. The new extreme-date regression failed before the fix (`Long.MIN_VALUE` incorrectly sorted after `Long.MAX_VALUE`). Switched to `Comparable<Point>` / `Comparator<Point>` and `Long.compare`, preserving erased method descriptors while making the full signed-long ordering correct. In `GroupedCalculatedValues`, removed unused boxed-array allocations and obsolete commented series code, applied diamond inference, removed a redundant element cast, and used enhanced-for for point copying. Added callback-order/value coverage. The focused boundary, grouped-values tests, full `:microproject_core:test`, and application/exchange/UI/reports `compileJava` all passed. |
| Configurable field ordering | `Field.compareTo` | Confirmed the method hunk matches OpenProj baseline `d2fa3c20` and is used by production UI/configuration sorting. The `index - other.index` ordering overflowed for opposite integer extremes; a focused test reproduced reversal before the fix. Changed the raw `Comparable` to `Comparable<Field>` and replaced subtraction with `Integer.compare`. Focused field tests, full `:microproject_core:test`, and application/exchange/UI/reports `compileJava` passed; `javap` confirms the erased `compareTo(Object)` bridge remains alongside the type-safe method. |
| Interval-table ordering | `ValueObjectForInterval.compare`, `compareTo` | Confirmed the comparator hunk matches OpenProj baseline `d2fa3c20`; production interval-table insertion and search use it with `Collections.binarySearch`. The start-date subtraction overflowed for opposite long extremes, reproduced by a focused pre-fix regression. Replaced raw `Comparable`/`Comparator` with typed contracts and `Long.compare`. Focused comparator/table/equality tests, full `:microproject_core:test`, and application/exchange/UI/reports `compileJava` passed; `javap` confirms the erased `compareTo(Object)` and `compare(Object,Object)` bridges remain. |
| Interval value equality | `ValueObjectForInterval.equals` | Bound the already-checked interval value and removed the redundant cast. OpenProj's baseline implementation directly cast and compared `start`; this fork's null/foreign-type guard is retained. Production use is interval-table value lookup. Existing `EqualsHashCodeContractTest` verifies equality/hash consistency and false for null/foreign types. |
| Timesheet natural-order stub | `TimesheetAssignment.compareTo` | The OpenProj-origin subtraction comparator had no caller: current `TimesheetEntryPane` sorts with its own explicit resource/task/start comparator. Repository search found no other natural-order, reflection, or configuration use; the class is not Serializable. Removed the unused `Comparable` implementation and method without changing the timesheet list's actual sort order. Full `:microproject_core:test`, application/exchange/UI/reports `compileJava`, and `verifyArchitectureBoundaries` passed. |
| Duration natural ordering | `Duration.compareTo`, `ClassUtils` duration/Work comparators | Compared against OpenProj baseline `d2fa3c20`; made the raw `Comparable` contract `Comparable<Duration>` and typed comparator operands at the integration boundary, including `Work`'s inherited Duration comparison. Kept existing comparison arithmetic and null/type failure behavior unchanged. Added regression coverage for direct order and both registered comparators. Focused Duration comparison/encoding and Rate comparison tests, full `:microproject_core:test`, application/exchange/UI/reports `compileJava`, and `javap` confirmation of the erased bridge passed. |
| Job queue cancellation dispatch | `JobQueue.cancel` | Confirmed the thread type-check/cast hunk matches OpenProj; used a Java pattern binding and added a bounded latch-based regression proving a running Job in the queue's thread group is cancelled and then terminates. The focused `JobQueueCriticalSectionTest`, full `:microproject_core:test`, and application/exchange/UI/reports `compileJava` passed. |
| Working-interval merge comparator | `Merge`, `Assignment.forEachWorkingInterval` | Confirmed the raw comparator declaration and factory parameter match OpenProj. Typed the comparator as `Comparator<Object>` through the single production call path; the existing assignment working-interval integration test exercises the `WorkComparator` merge path. `HasAssignmentsImplTest`, full `:microproject_core:test`, application/exchange/UI/reports `compileJava`, and `javap -s` confirmation that the factory descriptor still erases to `(Consumer, Comparator)` passed. |
| Command base-state immutability | `Command.text`, `Command.document` | Confirmed both fields are constructor-initialized and never reassigned; repository search found only `UpdateProjectCommand` as a subclass. Made them `final` and asserted the project document identity through the existing update-command regression. `UpdateProjectRequestTest`, full `:microproject_core:test`, and application/exchange/UI/reports `compileJava` passed. |
| Node state delegation | `NodeBridge.isDirty`, `setDirty`, `isValidLazyParent` | Confirmed all type-check/cast hunks match OpenProj. Used pattern bindings; added a headless regression for delegated DataObject state, unchanged false/no-op behavior for non-DataObject implementations, and valid/invalid/non-LazyParent dispatch. Focused `NodeBridgeDirtyStateTest`, full `:microproject_core:test`, and application/exchange/UI/reports `compileJava` passed after both changes. |
| Project task iterator | `Project.TaskIterator` | Replaced its raw iterator and unchecked casts with a wildcard iterator and pattern bindings. The iterator previously returned `null` from `next()` after exhaustion, violating `Iterator`'s contract; corrected it to throw `NoSuchElementException` and added a focused task-order/exhaustion regression in `ProjectHierarchyQueriesTest`. |
| Text document length filter | `FixedSizeFilter.maxSize` | Confirmed the class and its insertion/replacement logic match OpenProj; production callers are `SimpleEditor` and `ComponentFactory`. Marked the constructor-set limit final and added a headless `PlainDocument` test for insertion truncation and replacement capacity. No visible GUI contract changed; the focused `:microproject_ui:test --tests com.microproject.dialog.util.FixedSizeFilterTest` passed. |
| Resource export traversal | `ResourceLinker.executeNext` | Confirmed the type-check/cast is source-identical to OpenProj. Replaced the duplicate `ResourceImpl` check/cast with a Java pattern binding while preserving null-skip behavior for non-resource outline nodes. `MpxExportTrackingTest` and `MpxProjectConverterTest` passed, exercising active MSPDI export paths and assigned-resource output. |
| Chart popup dead-code removal and workspace restore | `TimeChartPopupMenu`, `TimeChartPanel.verticalScrollingItem`, `ChartInfo.restoreWorkspace` | The popup constructor documents replacement by JFreeChart; the popup has no production caller and its workspace menu item was never initialized after its addition was commented out. Removed the unused popup class/references and dead item field/call. A focused workspace regression failed before the fix with `NullPointerException`, then verifies the persisted vertical-scroll value restores through the live chart-panel state. The right-click route still obtains the current `TimeChartPanel` popup. The UI module is an application, not a documented plugin artifact; no in-repo reflection, configuration, serialized reference, or public extension contract was found. Unknown external binaries remain an explicit compatibility risk; obsolete locale resource keys are retained. |
| Node void-filter singleton | `NotVoidFilter.getInstance` | Confirmed normalized source against the OpenProj-derived candidate and found active callers in `NodeList` plus UI `ViewNodeModelCache`. Replaced the mutable lazy singleton with eager `static final` initialization. Concurrent first access could previously publish multiple filter instances and data-race on the shared reference; callers now always receive the safely published singleton. The focused test verifies stable identity and regular-node acceptance. |
| Unused numeric maximum visitor | `NumericMaximum` | Removed after repository-wide search found no production/test caller, class-name string, reflective/configuration registration, or serialization route. Unlike `Maximum`, this class is not registered in `SummaryVisitorFactory` or configuration XML. It does not implement `Serializable`, and the active application core has no documented external plugin/API contract. Its historical provenance row remains in the ledger; deletion is not a licensing conclusion. |
| Node factory reflection | `NodeFactory.createNode(Class)` | Replaced zero-length reflective argument arrays with `Class.getConstructor().newInstance()` and parameterized the `Class` as `Class<?>` without changing the erased descriptor. Focused tests verify virtual-node construction and preserve the existing null-on-unsupported-class behavior. |
| Unreferenced node-field list | `NodeFieldList` | Removed after repository-wide symbol search found only its definition (plus the historical provenance ledger row); no configuration/reflection registration or project persistence route references it. It is an application-internal class, not a separately published core API. It inherits `Serializable` from `LinkedList`, so unknown external serialized consumers remain a compatibility risk; no in-repository serialized use exists. The historical provenance row remains unchanged. |
| Unreferenced test filter | `TestFilter` | Removed after full source/configuration search found no caller, configuration registration, or reflective class-name reference beyond its own declaration. The class always accepted every value and had no active summary-filter use. Historical provenance remains recorded; this deletion does not change the configured summary or filtering behavior. |
| Form configuration collections | `FormFormat.boxes`, `layouts` | Confirmed normalized OpenProj provenance, the Apache Digester `addBox`/`addLayout` rules, and the active UI `FormComponent` consumer. Replaced raw collections and casts with `List<FormBox>`/`List<FormBoxLayout>` while preserving `List` erasure and XML element names. A Digester regression verifies order, default zoom selection, and typed UI configuration access. |
| Association container boundary | `AssociationFormat.getContainer` | Confirmed normalized OpenProj provenance and both active overrides (`Collection<Task>` and `Collection<Resource>`); narrowed the abstract return to `Collection<?>`, removing a raw parent contract without changing its erased `Collection` descriptor or either subclass's behavior. Existing association-format tests and the full core suite cover the formatter boundary. |
| Shared object-reference collection contract | `ObjectRef.getCollection`, `Field` collection consumers | Confirmed normalized OpenProj provenance and the active implementations in UI `FieldComponentMap` and reports `DataSource`; typed the interface return as `Collection<?>`, the reports override, and `Field` iterators without changing erasure or heterogeneous item semantics. |
| Snapshot undo collection boundaries | `Project.saveCurrentToSnapshot`, `restoreSnapshot`, `clearSnapshot`, `collectSnapshotDetails`, `getSnapshotIterator`, `SaveSnapshotEdit` | Confirmed these raw traversal/helper hunks against `d2fa3c20a:openproj_core/src/com/projity/pm/task/Project.java`. Typed public generic parameters as `List<?>`/`Collection<?>` (retaining erased descriptors), plus private iterators and the snapshot-detail collection. Kept cast points, task/detail pairing, encounter order, and empty/null behavior. Repository-wide caller search found only core undo and UI document-frame routes; all accept the wildcard contract. Existing `ProjectScheduleBehaviorTest` covers selected-task restoration plus clear/undo/redo snapshot state; focused and full core suites passed. |
| Project collection API boundaries | `Project.findTaskById`, `dump`, `applyTimesheet`, `UpdatesFromTimesheet` implementations | Matched the public raw collection methods to the OpenProj baseline. Narrowed their Java source contracts to `Collection<?>` without changing method erasures or consumer casts. The timesheet operation shares one wildcard contract across `UpdatesFromTimesheet`, `Project`, `NormalTask`, `Assignment`, `TimesheetAssignment`, `EnterpriseResource`, and `ResourceImpl`; all route their fields to wildcard-aware `TimesheetHelper` or `Field.copyData`. Caller search found task-ID lookup in core collaboration/tests and timesheet application in core/UI. `ProjectScheduleBehaviorTest` retains lookup coverage; full core tests and UI compilation passed. |
| Workspace field aliases | `Project.Workspace.fieldAliasMap` | The saved `HashMap` is produced by `FieldDictionary.getAliasMap()` and consumed by `setAliasMap()`, both of which define `HashMap<String,String>`. Added matching generic arguments and diamond inference. Serialized field type remains `HashMap`; key/value representation is unchanged. Core and downstream UI compilation passed. |
| Referring subproject collection contract | `SubprojectHandler`, `Project` facade, `DefaultSubprojectHandler` | Typed the shared getter as `Collection<Object>` and setter input as `Collection<?>`, preserving the intentionally heterogeneous backing list: task-date queries and Serializer still ignore non-Task values, while insertion retains existing duplicate checks and mutation behavior. Collection erasures are unchanged (`javap -s`). Added regression coverage for preserving a non-Task entry and retaining the zero dependency-date result. Full `:microproject_core:test` and `:microproject_exchange:test` passed, including POD round trips. |

## Correctness defects found during the audit

- `DictionaryCategory.equals` comes from later ProjectLibre code, not the
  OpenProj baseline, and is not counted as a modernization candidate. While
  scanning remaining core type checks, a regression test demonstrated that
  categories with the same class but different category names compared equal,
  despite `hashCode` including the category. Corrected equality to compare both
  fields using Java pattern matching. The new focused test failed before the
  fix and passed afterward; the full core suite and application/exchange/UI/
  reports `compileJava` also passed.

Separate work in `com.microproject.core.time` is bridge/fork code, not counted as
an OpenProj-origin modernization result unless hunk provenance is established.

- `CompositeCacheEvent.generateDiffLists` is a normalized-content match to the
  OpenProj baseline. Its insert/remove reconciliation mutated the original
  inserted `CacheEvent` node list while generating the derived diff, despite
  that payload remaining exposed through `getNodeEvents()`. This made diff
  generation observably destructive to its input; a focused regression failed
  before the fix because the source insertion list lost the reinserted node.
  A null inserted-node payload after a prior removal also reached an
  intersection call and could throw. The implementation now uses typed
  event/diff lists, enhanced-for, and a local copy for reconciliation,
  preserving source payloads and tolerating null node data. Focused tests verify
  generated inserted/removed/updated sets, source-list immutability, and null
  payload behavior. Caller review confirmed the existing spreadsheet model
  emits its row notifications from event intervals, so no claim is made that
  its row notifications were lost. No physical route, selection contract, or
  layout changed; no Robot rerun is needed.

- `PrintDocumentFactory.getInstance` is an OpenProj-derived unsynchronized
  lazy singleton. Concurrent print/export callers could each observe a null
  field and publish separate instances. The getter is now synchronized while
  retaining the protected field and public construction surface. A focused
  identity test and the full UI module test suite passed.

## Fork-only code explicitly excluded from this issue

- `Project.isBaselineFieldHidden` and `EnterpriseResource.isBaselineFieldHidden`
  are duplicate current helpers, but neither helper exists in the OpenProj
  baseline; the corresponding baseline `fieldHideBaselineCost` methods are
  fixed `false` stubs. Consolidating these methods here would change
  fork-specific visibility behavior, so they are not included in this
  OpenProj-only refactor. Any correctness change to their shared policy should
  be reviewed as a separate fork-behavior task with its own regression contract.
- `Field.toTaskSheetScheduleValue` was introduced in the ProjectLibre fork
  (tracked to commit `54e5480390`), and `Field.getGroupDuration` was introduced
  in commit `82de370c3d`; neither responsibility exists in the OpenProj source.
  Commits `b2c7ae394` and `4849e197e` modernize those fork additions and are
  excluded from #595 progress.
- In commit `3c96088e2`, the `Field.fireEvent` guard is OpenProj-derived, but the
  adjacent task-sheet `DataObject` dirty-state branch was added by the fork.
  Only the `fireEvent` modernization counts toward #595.

## Exchange dead-code candidate retained for API compatibility

- `com.microproject.exchange.Context` is a provenance-ledger candidate and
  currently has no production references to its accessors or fields; the only
  in-repository code reference is initialization of
  `MicrosoftImporter.context`. However, `Context` is public and that importer
  field is `protected`, so external importer subclasses can compile against and
  use this extension surface. No consumer search inside this repository can
  establish that removing the type/field is binary compatible for downstream
  integrations. Keep both until a deliberate compatibility-breaking API
  deprecation/removal decision is made; do not count this as safely deletable
  dead code in issue #595.

## Contrib SPI and absent-source review

- The OpenProj-matching contrib ledger candidates contain one active contract:
  `ClassResolverFilter` is implemented by `ScriptConfiguration` and passed to
  Groovy's `ResolveVisitor.setClassResolverFilter` through reflection in
  `Init`. Its public `canBeResoved` spelling is therefore retained as an SPI
  method name; renaming it would break the reflective/third-party contract.
  `ScriptConfigurationTest` already verifies allowed and unlisted class names.
- The other two contrib candidates (`Log` and `LogFactory`) are absent from the
  active `microproject_contrib` source tree. Repository-wide Java search found
  only commented-out former logging references, so they are not active code or
  new deletion work for this issue.

## Deliberately retained compatibility-sensitive comparison

- `Rate` remains on raw `Comparable`: its public `compareTo(Object)` explicitly
  throws `IllegalArgumentException` for a non-Rate operand. Converting it to
  `Comparable<Rate>` would generate a bridge that throws `ClassCastException`
  before entering the method, changing an observable contract. A type-safe
  migration needs a separately designed compatibility adapter; this syntax
  cleanup does not justify that behavior change.

## Deleted code, caller and compatibility evidence

- Removed `PeakUnitsFunctor`: normalized source matched the OpenProj baseline,
  but a repository-wide symbol search found no production/test caller, class-name
  string, reflection/configuration registration, ServiceLoader entry, or
  serialization reference. The core module is not published as a separately
  versioned library and has no `module-info.java`/declared exported API; the
  class was not part of documented extension configuration. Core tests and all
  direct application/exchange/UI/reports compilations passed after removal.
  The provenance CSV row is retained as historical origin evidence, not as a
  statement that the deleted class remains in the current runtime.
- Removed `ValueDivision`: a repository-wide search found no production/test
  caller, FQCN string, reflection/configuration registration, ServiceLoader
  entry, or serialized form. It does not implement `Serializable`, the core
  module has no declared public API dependency/export or separate publication,
  and its public factory is unused. Removed the stale algorithm/query backlog
  entry as well. Full `:microproject_core:test`, application/exchange/UI/reports
  `compileJava`, and `verifyArchitectureBoundaries` passed after removal.
- Removed `DistributionHolder`: a repository-wide source/configuration search
  found no production/test caller or ScriptRunner exposure; its only former
  configuration reference was an already-commented XML fragment. The class
  does not implement `Serializable`. Removed the dead class and configuration
  fragment, and cleared the corresponding TODO. The full core test suite,
  application/exchange/UI/reports compilations, and
  `verifyArchitectureBoundaries` passed after removal.

## Verification evidence to date

- Focused regression tests were run for the changed interval, calendar, duration,
  rate, distribution identity, and contour responsibilities. In particular, the
  Rate fractional-ordering, RateFormat end-position, and DistributionData
  project-key tests were observed failing before their fixes.
- `:microproject_core:test` and `:microproject_exchange:test` passed at the
  distribution-identity checkpoint; core tests and exchange compilation passed
  at the contour checkpoint; the core full suite passed again after the
  `ScriptConfiguration` type-safety change. The focused `NodeGrouperTest`, full
  core suite, and UI compilation passed after typing the XML grouping contract.
  The focused `AssignmentCompositionFilterTest` and the full core suite passed
  after modernizing Assignment composition dispatch.
  The focused `NotAssignmentFilterTest`, full core suite, and UI compilation
  passed after making the filter instances immutable and initialization-safe.
  The focused `ResourceInTeamFilterTest`, full core suite, and UI compilation
  passed after pattern-matching the resource/team filter branches. An added
  non-resource `AssignmentEntry` regression failed before the defensive type
  narrowing and passed after it; the full core suite plus application,
  exchange, UI, and reports compilation also passed.
  The focused `TimesheetInfrastructureTest`, full core suite, and exchange /
  application compilations passed after typing timesheet aggregation iteration.
  The focused `ObjectEventManagerTest`, full core suite, and exchange /
  application compilations passed after the event pooling fix.
  The focused `FieldArrayUtilTest`, full core suite, and application /
  exchange compilations passed after typing field-array filtering.
  The focused `AbstractSessionTest`, full core suite, and application
  compilation passed after typing the session save delegation.
  The focused `TypedNodeIteratorTest`, full core suite, and UI compilation
  passed after fixing selection iteration/removal semantics.
  The core full test suite and application, exchange, and UI compilations passed
  after typing scheduling field notification state. Core tests and exchange /
  application compilations passed after typing external task manager storage.
  Core tests and UI compilation passed after the resource-pool registry typing
  and dead-state cleanup.
  `ScheduleBackupEditTest` and the full core suite passed after typing schedule
  snapshot capture/restoration; application, exchange, UI, and reports all
  compiled against the unchanged constructor descriptor.
  The `NodePasteEdit` insertion-position undo/redo test and the
  `ClearSnapshotEdit` clear/restore/clear regression passed, followed by the
  full core suite and application, exchange, UI, and reports compilation.
  `SelectionFilterTest` (including collection membership and callback behavior),
  the full core suite, and UI compilation passed after typing the active
  `BelongsToCollectionFilter` collection contract.
  `QueryTest` passed after modernizing the source-exact `whereInRange` type
  branch; the full core suite and application, exchange, UI, and reports
  compilation passed afterward.
  `ResourcePoolIdentityTest` passed for the `getParentId` no-key fallback,
  followed by the full core suite and application, exchange, UI, and reports
  compilation.
  `AssignmentFieldClosureCollectionTest` passed for cost-only fixed-value
  aggregation; full core tests and application, exchange, UI, and reports
  compilation passed afterward.
  `NormalTaskDurationTest.arrangeTaskKeepsSummaryChildrenBetweenParentMarkers`
  passed after modernizing WBS child traversal; the full core suite and
  application, exchange, UI, and reports compilation also passed.
  The focused `DependencyServiceTest.summaryTaskPredecessorSearchRejectsChildDependencyCycles`
  and full core suite passed after modernizing the summary child-predecessor
  scan; application, exchange, UI, and reports compilation passed as well.
  `ScheduleUtilTest` and the full core suite passed after modernizing the
  weighted-completion schedule branches; application, exchange, UI, and reports
  compilation passed.
  The focused `HasCommonKeyImpl` equality/hash contract test and full core
  suite passed after pattern-binding the OpenProj equality hunk; application,
  exchange, UI, and reports compilation passed.
  `BarClosureTest` and the full core suite passed for the schedule-window
  boundary modernization; application, exchange, UI, and reports compilation
  passed.
- Application, exchange, UI, and reports compilation passed at an earlier
  calendar-clone integration checkpoint. No GUI route, layout, or Swing behavior
  was changed, so Robot/GUI tests were not repeated.
- Compilation/test success is evidence only for those exercised modules; it is
  not evidence that the inventory is exhausted or that every compatibility
  boundary has been audited.
- `EarnedValueCalculatorTest` and the full core suite passed after the offset
  dispatch modernization; application, exchange, UI, and reports `compileJava`
  passed. No GUI route, layout, or Swing behavior changed, so GUI/Robot tests
  were not repeated.
- `ExtendedPageFormatTest` reproduced the printer-boundary defect before the
  fix and passed afterward. The full core suite plus application, exchange, UI,
  and reports `compileJava` passed. The change is in print-area calculation;
  no GUI interaction or layout was changed, so no GUI/Robot run was repeated.
- `ObjectEventManagerTest` passed for task-to-assignment update propagation;
  the full core suite plus application, exchange, UI, and reports `compileJava`
  also passed. No GUI route or layout changed.
- `DateRenderer` behavior-preserving pattern-binding cleanup passed
  `:microproject_ui:compileJava`; no interaction, visual contract, or layout
  changed, so GUI/Robot tests were intentionally not repeated.
- `SplittedViewLifecycleTest` passed alongside UI compilation for the
  `MainView` pattern-binding cleanup. Existing split lifecycle checks remain
  green; no physical interaction/layout contract changed and no new Robot
  scenario was warranted.
- `DeepChildWalkerTest` and the full core suite passed after typing the local
  child list and replacing the generic iterator helper; application, exchange,
  UI, and reports `compileJava` passed.
- `DeepChildSearcherTest` passed for the first depth-first match; the full core
  suite and application, exchange, UI, and reports `compileJava` passed.
- `ChildWalkerTest` first reproduced the leaf-consumer dispatch defect, then
  passed for terminal-leaf order and shallow immediate-child order after the
  fix. The full core suite and application, exchange, UI, and reports
  `compileJava` passed.
- `DataObjectEqualsHashCodeTest` passed after the `LinkData` pattern-binding
  cleanup. `GeneralFilteredIteratorTest` passed after explicitly installing
  its documented null-as-accept-all predicate; the initial run exposed a test
  fixture omission (missing predicate), not a product regression.
- `HasExtraFieldsImplTest` passed for insertion order and heterogeneous values;
  full core tests, `PodRoundTripTest`, and application/exchange/UI/reports
  compilation passed after typing the extra-field map.
- At the integration checkpoint after the recorded core batches,
  `.\gradlew.bat clean build installDist verifyArchitectureBoundaries
  --console=plain` completed successfully (all module tests included, 3m52s).
  `git status` remained clean after the build; no generated output or sample
  rewrites were staged or committed.

## Remaining work

### Scope correction

The issue's target excludes fork-origin code unless its specific responsibility
is independently confirmed as OpenProj-derived. PR #611's
`CollaborationMetadataStore` has no corresponding OpenProj source path in the
provenance ledger, and PR #612's `createPositionMap` / `createPositions`
undo-position helpers do not occur in the compared OpenProj implementation.
Both changes are already integrated, but are adjacent cleanups and are **not**
counted as issue #595 modernization progress. PR #613's separate
`internalIndent` collection typing is retained as in-scope because that exact
raw traversal is present in the checked source excerpt linked above. No
OpenProj coverage or completion percentage is inferred from the adjacent PRs.

- Hunk-provenance and active-caller review remains for the 83 content-different
  candidates; the 17 absent paths are classified above and do not all represent
  active source requiring modernization.
- Search the 180 matching files and OpenProj-origin hunks within the 83
  differing files for production callers and compatibility boundaries; do not
  treat file-level equality as proof of an active eligible hunk.
- Audit remaining eligible Java in exchange, UI, reports, and other core
  responsibilities; do not add GUI tests unless a physical GUI contract or
  visual surface is changed.
- Exchange traversal follow-up: the remaining active explicit iterators in
  `Serializer.saveTasks` remove unchanged `TaskData` entries while iterating;
  distribution serialization removes unchanged `DistributionData`; and
  assignment deserialization removes malformed entries from its working list.
  These require `Iterator.remove()` to preserve the in-place filtering contract.
  `MSPDISerializer` walks `project.getTaskOutline().iterator(exportRoot)`, a
  hierarchy iterator with a caller-selected root and no `Iterable` traversal
  contract. Keep these iterators; do not convert them to enhanced-for. DOM
  `NodeList` loops in `MpoFileImporter` are indexed API traversals, not Java
  collection iterators.
- Record cleanup/deletion candidates only after checking reflection, resource
  configuration, serialization, ServiceLoader, action IDs, and format readers.
- Update this inventory and issue #595 after each meaningful audited batch; do
  not close the issue while any required phase or unresolved in-scope work
  remains.

Latest follow-up #627 is based directly on `origin/master` at
`b825b936edf55f3d8090b72e00121bf11ac6483d` (verified by merge-base). The
focused task traversal test passed. Core tests and downstream application,
exchange, UI, and reports compilation passed. The first combined core-suite run
had one timing-sensitive failure in
`CriticalChainServiceTest.previewScalesToLargeSharedResourceWithoutQuadraticExpiryScan()`;
that test passed when run alone and the full core suite passed on one retry.
No GUI route or visual surface changed, so no Robot rerun was warranted.

PRs #627 and #628 passed CI and were squash-merged as recorded above. Follow-up
#629 starts directly from latest `origin/master` at
`e2db4e5ce15cbc76ccf8788b438e78c71f698157` (verified by merge-base). The
`CompositeCacheEvent` regression was first run against the old implementation
and failed, then passed after the fix. No GUI route or visual surface changed.

PR #629 passed CI and was merged as
`795aa6f6b7e4759f48520b8842a61cda9952ad71`. Follow-up #630 starts directly
from that latest `origin/master` (verified by merge-base); the focused
`FieldArrayEventTest` passed after typing its `ArrayList<Field>` payload.

PR #630 was merged as recorded above after a failed first CI run and a
successful failed-job rerun; the lone first-run failure was an unrelated
`ScaledScrollPaneTest` assertion that passed on the origin/master baseline and
on a focused retry of the PR branch. Follow-up #631 starts from latest
`origin/master` at `dfba55c94a26afd3c89042ebb02c3294c142d790` (verified by
merge-base); focused `CacheEventTest` passed.

PR #631 passed its failed-job rerun and merged as recorded above. Follow-up
#632 starts from latest `origin/master` at
`e6ece1c1e38f20e0d02b5e656580bce9214979d4` (verified by merge-base); focused
`GraphEventTest` passed.

PR #632 passed CI and merged as recorded above. Follow-up #633 starts from
latest `origin/master` at `9805ecaf2ab4753bd860172cb2f1742edd890b24` (verified
by merge-base); focused `NodeListTransfertActionTest` passed after running the
Swing component setup/assertions on the EDT.

PR #633 passed CI and merged as
`2e5d2817e43f1642b42741f20b9ac7b22052c49c`. Follow-up #634 starts directly
from this latest `origin/master` (verified by merge-base). The provenance
ledger identifies `NetworkUI` as normalized content matching OpenProj; its
`getNodeAt` loop now declares the iterator as `ListIterator<?>`, preserving
the existing traversal order, cast behavior, hit test, and return value. UI
`compileJava` passed. This is behavior-preserving Swing source modernization;
no GUI route or visual contract changed, so no Robot run was needed.

PR #634 passed CI and merged as
`e13973ca0602cdb591e382dd8af857cebaf19473`. Follow-up #635 starts directly
from that latest `origin/master` (verified by merge-base). The OpenProj-matched
`DependencyCacheTransformer` had one production construction site, no external
or reflective references, no state use, and an empty `transfrom` body. Replaced
it with an explicit no-op lambda in `VisibleDependencies`, preserving the
identity behavior for dependency elements, and deleted the unused class. Added
a focused regression assertion for element/order preservation; the focused UI
test passed. No GUI interaction or visual contract changed, so no Robot run was
needed.

PR #635 passed CI and merged as
`04823213aace6bd5ec7a6d6d079ba759df5e6dea`. Follow-up #636 starts directly
from that latest `origin/master` (verified by merge-base). The OpenProj-matched
`NodeHierarchy`/`NodeModel` traversal API and corresponding implementation
methods exposed raw iterators even though both full and shallow traversals
yield `Node` exclusively. The directly compared OpenProj methods have the same
preorder and adapter behavior. Their signatures now expose `Iterator<Node>`;
the enumeration adapter preserves traversal order and unsupported removal,
and shallow traversal keeps its existing depth/root semantics. The existing
focused hierarchy test now statically checks the generic type and asserts
preorder plus removal behavior. Full core tests and downstream
application/exchange/UI/reports compilation passed; the focused hierarchy test
also passed after its additional assertion. No GUI route or visual contract
changed, so no Robot run was needed.

PR #636 passed CI and merged as
`80ee53cc72dbcf1399c0c5efc9d00a438662c5cb`. Follow-up #637 starts directly
from that latest `origin/master` (verified by merge-base). A direct comparison
against OpenProj's `Portfolio.forProjects` confirmed its raw node iterator and
Project type filter are inherited behavior. The method now uses
`Iterator<Node>` and pattern matching, and its callback contract is
`Consumer<? super Project>` because it only emits Projects. All production
callers in core/UI were migrated to typed consumers; a focused test verifies
that the callback receives the project from the portfolio. The first test
compile exposed the former `Consumer<Object>` mismatch, which was corrected at
the API contract rather than weakening the assertion. `DefaultSubprojectHandlerTest`
and application/exchange/UI/reports compilation passed. No GUI route or visual
contract changed, so Robot was not repeated.

PR #637 CI's initial full build encountered the known intermittent
`ScaledScrollPaneTest.originChangeKeepsTheVisibleLeftDateAnchored` failure
(886 UI tests: 1 failed, 7 skipped). The failed-job rerun passed; no production
changes were made in response to the unrelated flaky assertion. PR #637 was
merged as `df94fff3de7a85375b7af3f0273c5925325aa519`.

Follow-up #638 starts directly from that latest `origin/master` (verified by
merge-base). The node/dependency cache event queue producers create only
`CacheEvent`, and its consumers pass that same event list into
`CompositeCacheEvent`. The OpenProj-derived event pipeline now uses
`List<CacheEvent>` at storage, enqueue, diff-generation, and listener-dispatch
boundaries. A focused test asserts event source, type, payload, and removal-then-
insertion order. UI focused test passed; no user route or visual contract
changed, so no Robot run was needed.

PR #638 passed CI and merged as
`97bb4815a3ee437974646f743647e1f53fdfd3ee`. Follow-up #639 starts directly
from that latest `origin/master` (verified by merge-base). In the directly
OpenProj-matched `NetworkRenderer.paint`, the dependency and graphic-node
traversal locals are now `Iterator<?>`/`ListIterator<?>`; casts, iteration
order, hit testing, and painting are unchanged. UI `compileJava` and diff check
passed. No GUI route or visual contract changed, so no Robot run was needed.

Follow-up #640 starts directly from that latest `origin/master` (verified by
merge-base). `Node.childrenIterator` was OpenProj-derived and exposed raw
`ListIterator` values even though its backing children are tree nodes. The API
and every production caller now use `ListIterator<TreeNode>`/`Iterator<TreeNode>`.
During the migration, a focused test reproduced a defect in `NodeBridge`'s
custom empty iterator: `next()` and `previous()` returned `null` instead of
throwing `NoSuchElementException`, and its mutation methods did not follow the
standard iterator contract. Replaced that bespoke implementation with the
standard empty-list iterator and added coverage for traversal, mutation, and
index bounds. Full core tests and downstream UI compilation passed. No GUI
route or visual contract changed, so no Robot run was needed.

Follow-up #641 starts directly from the latest `origin/master` (verified by
merge-base). The OpenProj-derived `Node.getType()` contract returned a runtime
class but exposed raw `Class`; it now returns `Class<?>`. `NodeBridge` and the
test implementation were migrated without changing runtime behavior. Full
core tests and `git diff --check` passed. No GUI route or visual contract
changed, so no Robot run was needed.

Follow-up #642 starts directly from the latest `origin/master` (verified by
merge-base). The raw `NodeHierarchy.getChildren(Node)` declaration matches the
OpenProj baseline `d2fa3c20a`; its live mutable and filtered implementations
both return `Node` elements, and hierarchy/model callers use those elements as
nodes (including WBS-cache rebuilds). Narrowed the API and filtered facade to
`List<Node>` without changing the erased `List` descriptor or list contents.
Updated hierarchy integration assertions to use the declared element type.
Full core tests and application/exchange/UI/reports compilation passed. No GUI
route or visual contract changed, so no Robot run was needed.

Follow-up #643 starts directly from the latest `origin/master` (verified by
merge-base). `SummaryVisitorFactory.getInstance` is active through
`Field.getSummaryVisitor`; the factory source matches the OpenProj baseline for
its raw `Class` parameters. Both class parameters are now `Class<?>`, and the
factory's summary-name bidirectional maps use `BidiMap<String, Integer>` with
typed entry construction. The public `getMap` method currently has no in-repo
caller, so it was retained rather than treated as dead code. A focused test
checks the Boolean summary reverse lookup and immutability. The focused
`SummaryVisitorFactoryTest` and application/UI/reports compilation passed. No
GUI route or visual contract changed, so no Robot run was needed.

Review note: a proposed generic `PredicatedNodeFilterIterator<T>` migration was
rejected after compilation showed that the active `GeneralFilteredIterator`
inherits Apache Commons Collections' raw `FilterIterator` and cannot also
implement `Iterator<Object>` without resolving the superclass boundary. The
experiment was reverted; a future change would need to replace or wrap that
adapter and preserve its filtering/removal semantics, rather than merely
parameterize the marker interface.

Follow-up #644 starts directly from the latest `origin/master` (verified by
merge-base). `AssociationFormatParameters` is a normalized OpenProj match and
its constructor guarantees a `HasDependencies` association endpoint, while
`DependencyFormat` repeated casts the legacy `Object getThisObject()` result
back to that type. Added `getAssociationObject()` as a typed accessor and
migrated those two dependency-construction casts; retained the old Object
getter and its descriptor for compatibility. A focused test asserts both
accessors return the same endpoint. Association-format tests and downstream
exchange/UI compilation passed. No GUI or persisted format contract changed.

Follow-up #645 starts directly from the latest `origin/master` (verified by
merge-base). `CalculationPreference` is a normalized OpenProj match. The two
calculation policy flags were assigned after construction only in the static
initializer and have no production setters; moved them into a private
parameterized constructor and made them final. The public no-arg constructor
and its false/false defaults remain intact. The active reference has no
reassignment path and is now final. A focused test verifies defaults, the
MS_PROJECT true/true preset, and active-instance identity. Full core tests and
application/exchange/UI compilation passed. No GUI or persisted format
contract changed.

Follow-up #646 starts directly from the latest `origin/master` (verified by
merge-base). `Field`'s raw `Class` fields, metadata accessors, applicability
checks, and reflection parameter arrays correspond to the OpenProj source; the
active consumers include field conversion, applicability, and spreadsheet
rendering. Replaced those raw types with `Class<?>` / `Class<?>[]` and made the
private fixed reflection signature arrays final. Updated the reports
`DecoratedField` adapter and added a focused applicability/type regression.
Erased descriptors remain unchanged. Full core tests and application,
exchange, reports, and UI compilation passed. No GUI route or visual contract
changed.

Follow-up #647 starts directly from the latest `origin/master` (verified by
merge-base). `Select` and its active `StaticSelect`/`DynamicSelect` subclasses
exposed raw list/map types although the options are represented as Objects and
the configuration XML helper specifically consumes string keys and values.
Added those generic boundaries without changing erased method descriptors.
Reviewing the null-allowed option-list path exposed a functional defect: it
created a list with a leading null but omitted every real option. The method
now copies the original options after null, with a focused order regression.
Full core tests and application/exchange/reports/UI compilation passed. No GUI
route or persisted format contract changed.

Follow-up #648 starts directly from the latest `origin/master` (verified by
merge-base). `StaticSelect` stores string option keys and heterogeneous object
values in its bidirectional maps and ordered option list; added those concrete
generic types and replaced its raw iterator with enhanced for. The targeted
cache review also showed `put` did not invalidate `keyArray`, so adding options
after the first key-array read silently returned stale choices. Invalidate
that derived cache on every put and cover the sequence in `SelectTest`. Focused
`SelectTest`, application/exchange/reports/UI compilation, and diff check passed.

Follow-up #649 starts directly from the latest `origin/master` (verified by
merge-base). The active `DynamicSelect` reflection lookup and `ClassUtils`
resolver passed runtime parameter classes through raw `Class[]` signatures.
Narrowed the signature and reflective parameter array to `Class<?>[]` while
preserving the erased `Class[]` descriptor, with focused method-resolution
coverage. The focused test, application/exchange/reports/UI compilation, and
diff check passed.

Follow-up #650 starts directly from the latest `origin/master` (verified by
merge-base). `FieldDictionary` resolves configured Java classes and passes
them to `Field.isApplicable`; all its production callers were checked. Typed
the stored class, public overloads, helper arrays, and internal class lists as
`Class<?>` / `Class<?>[]` without changing runtime resolution or erased
descriptors. Full core tests, downstream module compilation, and diff check
passed.

Follow-up #651 starts directly from the latest `origin/master` (verified by
merge-base). `OptionsFilter` is instantiated and wired by the Digester from
configuration XML; its reflective callback contract remains `List,List` at
runtime. Typed its internally copied mutable keys and read-only option values,
then added a focused reflective-callback regression proving key filtering
still works without mutating the supplied values. Full core tests,
application/exchange/reports/UI compilation, and diff check passed.

Follow-up #652 starts directly from the latest `origin/master` (verified by
merge-base). `FieldValues.getValues` is active in POD serialization and
deliberately constructs a `LinkedHashMap` to preserve byte-stable field order.
Typed its field collection and string-key/object-value map boundaries, replaced
raw iterators with enhanced for, and used pattern matching for Serializable
values without changing insertion order or the HashMap erased return type.
Full core tests, the `PodRoundTripTest` format-specific byte-stability/save-
reload suite, downstream application/reports/UI compilation, and diff check
passed.

Follow-up #653 starts directly from the latest `origin/master` (verified by
merge-base). `FieldConverter` is active throughout field display, parsing, and
assignment callers. Typed its conversion-target and BeanUtils converter class
parameters and its per-context converter map, preserving the erased runtime
contracts. Added a focused parse/convert regression.

Compatibility exception: the bundled BeanUtils `Converter` SPI declares a
generic method `<T> T convert(Class<T>, Object)`. Its converter implementations
currently produce several target-specific types (and null) through that
boundary; changing them to wildcard parameters does not override the SPI, and
forcing generic return casts would add unchecked runtime assumptions. Keep
those implementation overrides raw until the conversion SPI/adapters can be
redesigned and characterized separately.
Core tests, application/exchange/reports/UI compilation, and diff check passed.

Follow-up #654 started directly from the latest `origin/master` (verified by
merge-base). The active Digester configuration readers in `Configuration` and
`FieldDictionary` pass explicit reflective parameter signatures; changed those
`Class[]` arrays to `Class<?>[]` without altering XML paths, method names,
parameter order, or runtime descriptors. Full core tests,
application/exchange/reports/UI compilation, and diff check passed.

Follow-up #655 starts directly from the latest `origin/master` (verified by
merge-base). `CustomFieldsMapper` reflects over MPXJ `TaskField` and
`ResourceField` constants to populate import/export mapping tables. Narrowed
those reflection inputs to `Class<?>` and added focused checks for representative
task and resource text-field mappings; field names, array ordering, and format
mapping behavior are unchanged. Full exchange tests, downstream
application/reports/UI compilation, and diff check passed.

Follow-up #656 corrected a false failure in the existing
`ScaledScrollPaneTest.originChangeKeepsTheVisibleLeftDateAnchored` regression.
The test had compared against the requested initial viewport x-coordinate
without reading back Swing's actual position after layout/scroll-range
adjustment. It now measures the visible left date from the observed pre-change
viewport position. The focused test and full CI passed; no GUI product behavior
changed, so no Robot rerun was warranted. This test-only fix was merged in
PR #657, after which PR #655's full CI rerun also passed.

Follow-up #658 starts from the latest integrated `origin/master` (verified by
merge-base). `Project.getType()` still exposed raw `Class` even though the
canonical `Node.getType()` contract is `Class<?>`. Narrowed the return
signature while preserving the erased `Class` descriptor and added a focused
runtime-type identity assertion. `ProjectScheduleBehaviorTest`, downstream
application/exchange/reports/UI compilation, and diff check passed.

Follow-up #659 starts from the latest integrated `origin/master` (verified by
merge-base). Modernized `CommonTransformFactory`'s reflective constructor call
to use the `Class<?>` parameter overload and direct varargs argument. This is
the shared factory behind XML-configured filters, sorters, groupers, and
transformers; constructor selection, configured string (including null), and
exception wrapping remain unchanged. Added focused checks for both configured
and null arguments. Core focused test and diff check passed.

Follow-up #660 starts from the newly updated `origin/master` after #659
(merge-base verified). `ProjectFactory` passes reflection signatures into the
existing `SessionFactory` adapter for optional resource loading and project
data retrieval. Narrowed both signature arrays to `Class<?>[]`; method names,
parameter order, argument values, and session compatibility fallbacks are
unchanged. Focused `ProjectFactoryClosingTest`, downstream compilation, and
diff check passed.

Follow-up #661 starts from the latest integrated `origin/master` after #660
(merge-base verified). `SimpleEditor` and `TimeSimpleEditor` store a runtime
conversion target and pass it to `FieldConverter`, whose API already accepts
`Class<?>`. Typed both editor fields and constructor parameters while retaining
the same constructor erasures and conversion behavior. Focused editor tests,
downstream compilation, and diff check passed.

Follow-up #662 starts from the latest integrated `origin/master` after #661
(merge-base verified). `DistributionConverter` loads an optional delegate class
by configured name and uses it only to resolve extension methods. Typed the
protected delegate-class metadata as `Class<?>`; reflective extension lookup,
configuration, and erased field descriptor remain unchanged. Full core tests,
downstream compilation, and diff check passed.

Follow-up #663 starts from the latest integrated `origin/master` after #662
(merge-base verified). Modernized the legacy subproject migration fallback in
`Serializer` by passing its two constructor parameter classes directly rather
than allocating a raw reflection signature array. The configured class name,
constructor order/types, and serialized format are unchanged. POD subproject
round-trip tests, exchange tests, downstream compilation, and diff check passed.

Follow-up #664 starts from the latest integrated `origin/master` after #663
(merge-base verified). Modernized `Project`'s configured subproject-handler
constructor lookup and project-role reset lookup to pass parameter classes and
invocation arguments directly through reflection's varargs APIs. Configuration
keys, parameter order/types, and error handling are unchanged. Core tests,
downstream compilation, and diff check passed.

Follow-up #665 starts from the latest integrated `origin/master` after #664
(merge-base verified). Modernized the remaining active core reflection
signature/argument wrappers in `Init` and `Alert` to use class/argument
varargs, while retaining string-based optional Groovy and GraphicManager
lookups (and therefore the module boundary). Typed the remaining
`UniqueIdPool` signature array passed through the fixed `SessionFactory` API.
Core tests, downstream compilation, and diff check passed.

Follow-up #666 starts from the latest integrated `origin/master` after #665
(merge-base verified). Modernized active UI-module reflection calls across
printing, optional JNLP persistence, reports, LAF, startup, and project
descriptor adapters. String/classloader integration boundaries remain intact;
SessionFactory signature arrays are now `Class<?>[]`. Removed the adjacent
unused `GraphicManager` local in the login callback after confirming it had no
reads. UI module tests/compilation, downstream compilation, and diff check
passed. No GUI contract or physical route changed, so no Robot rerun was
needed.

Bug found while reviewing #666: `GraphicManager.getInstance(Component)` used
`Class.forName("com.microproject.bootstrap.BootstrapApplet.class")` for its
optional component-wrapper lookup, so that branch could never resolve the
class name and silently returned null. Corrected the class name and retained
the string-based optional integration boundary. Added one headless route test
whose test-only wrapper verifies the owning manager identity. This internal
lookup changes no physical command route or visible UI contract, so no Robot
run was warranted. The regression failed with the original class name and
passed after the correction. The new test fixtures carry the required MIT
header; this also corrects the missing header on the prior `CommonTransformFactoryTest`.

Follow-up #668 starts from the latest integrated `origin/master` after #667
(merge-base verified). `BrowserControl` still carried the fully commented-out
OpenProj 1.4 OS-specific browser launcher alongside its active `Desktop.browse`
implementation. Confirmed the commented block against the archived OpenProj
source and searched all 15 production callers; removed only the dead block and
its now-unused `Method`/`JOptionPane` imports. The active public method and all
callers remain unchanged. Core tests, downstream compilation, and diff check
passed; no GUI route behavior changed.

Follow-up #669 starts from the latest integrated `origin/master` after #668
(merge-base verified). Removed redundant empty signature/argument arrays from
zero-argument reflective calls in core `Alert`, `JobQueue`, and `DynamicSelect`,
using reflection varargs directly. Class-name boundaries, target names,
exception handling, and return values are unchanged. Core tests, downstream
compilation, and diff check passed.

Follow-up modernization: `CollectionIntervalGenerator.makeIterator()` now uses
the collection's `iterator()` directly. The active method only calls
`hasNext()`/`next()`; no caller or subclass relies on `ListIterator` methods.
This preserves List encounter order and uses the existing generic-collection
path for non-List collections. The choice matches the OpenProj source's List
traversal, while retaining the fork's working generic-collection behavior.
`CollectionIntervalGeneratorTest` covers both List and insertion-ordered
non-List collections; the focused core test and `git diff --check` passed.

Follow-up #670 starts from the latest integrated `origin/master` after #669
(merge-base verified). Completed the matching optional BootstrapApplet
`GraphicManager(Container)` binding path by replacing its empty signature and
argument arrays with direct zero-argument reflection varargs. Added a focused
headless assertion that container construction stores the exact manager on its
FrameHolder; existing missing-wrapper fallback remains unchanged. Core/UI
focused tests, downstream compilation, and diff check passed; no Robot route
was added for this non-command legacy embedding adapter.

Follow-up #671 starts from the latest integrated `origin/master` after #670
(merge-base verified). Typed the serialized project field-value map and the
resource/task ID maps built during exchange structure reconstruction, using the
existing `FieldValues` and `createIdMap` generic contracts. Keys, values,
insertion/order semantics, serialized map implementation, and POD format are
unchanged. POD round-trip and exchange tests, downstream compilation, and diff
check passed.

Follow-up #678 starts from the latest integrated `origin/master` after #677
(HEAD and merge-base verified). Modernized OpenProj-origin `WorkingCalendar.dump`
to build diagnostic text with `StringBuilder` and iterate its typed
`TreeSet<WorkDay>` directly. Added a focused assertion for header, all weekday
entries, and exception entries. Existing output format and exception ordering
are preserved; `WorkingCalendarTest` and `CalendarDefinitionTest` passed, as
did diff check. Full CI passed before merge.

Follow-up #679 starts from the latest integrated `origin/master` after #678
(rebased and merge-base verified). Removed the fully commented-out OpenProj-
origin `PredecessorTaskTree.cleanTree` implementation and its sole commented
call. No runtime or public API behavior changed, and the PERT tree class itself
remains because internal non-use alone does not justify deleting a public type.
`microproject_core:compileJava` and diff check passed.

Follow-up #680 starts from the latest integrated `origin/master` after #679
(HEAD and merge-base verified). Typed the closure execution chain as
`List<Consumer<Object>>`, removed raw iterators from initialization and value
calculations, and kept the externally supplied `Collection<?>` reference live
because `ReverseQuery` passes a `List<Object>`. Preserved consumer-only entries
for `accept()` with a localized unchecked cast; compile exposed why narrowing
the contract to `AssignmentFieldFunctor` would break both source compatibility
and existing consumer behavior. Focused tests pin live-view aggregation,
last-nonzero caching, and consumer-only invocation. Core focused tests,
ReverseQuery caller compilation, and diff check passed.

Follow-up #672 starts from the latest integrated `origin/master` after #671
(HEAD and merge-base verified). Typed `Job`'s OpenProj-origin runnable queue
and its iterator as `InternalRunnable`, removing casts from the queue traversal
and using enhanced-for in `addJob`. Repository-wide production search found no
external access to this protected queue; list ordering, iterator transitions,
and runnable execution order are unchanged. Focused job critical-section and
exception-handler tests plus application/exchange/reports/UI downstream
compilation passed. This is source-only core work, so no GUI/Robot repetition
was needed.

Follow-up #673 starts from the latest integrated `origin/master` after #672
(HEAD and merge-base verified). Typed the OpenProj-origin iterator over
`PredecessorTaskList.TaskReference` in critical-path sentinel initialization;
the iterator comes from an already-typed `ListIterator` API. Removed only its
redundant cast; order and sentinel dependency decisions are unchanged. Focused
critical-path scheduling compatibility tests and diff check passed.

Follow-up #674 starts from the latest integrated `origin/master` after #673
(HEAD and merge-base verified). Typed the forward/reverse `TaskReference`
iterator in critical-path scheduling and converted the two sentinel-boundary
association traversals to enhanced-for over the existing `Association` list
contract. Kept the `Dependency`/`Task` conversions because `Dependency.getTask`
returns the broader `HasDependencies` type; removing that cast was rejected
after the compiler exposed the mismatch. Focused critical-path scheduling
compatibility tests and diff check passed.

Follow-up #675 starts from the latest integrated `origin/master` after #674
(HEAD and merge-base verified). In `Task.dependsOn`, converted the two
OpenProj-origin raw predecessor iterators to enhanced-for over the existing
typed `AssociationList`. Kept the `Association` to `Dependency` cast because
the list contract intentionally exposes the broader association type and the
prior code had the same runtime check behavior. The full core test suite and
diff check passed; no user-visible UI route changed.

Follow-up #676 starts from the latest integrated `origin/master` after #675
(HEAD and merge-base verified). Typed `TaskSchedule`'s two OpenProj-origin
dependency collections as the actual `AssociationList` return type and replaced
their raw iterator loops with enhanced-for over `Association`. Retained the
cast to `Dependency` because it is a domain conversion from the collection's
declared `Association` element type. Critical-path, project-schedule, and
forward/reverse compatibility tests passed; diff check passed.

Follow-up #677 starts from the latest integrated `origin/master` after #676
(HEAD and merge-base verified). Converted the two OpenProj-origin assignment
loops in `TaskSnapshotBackup` to enhanced-for over the existing
`AssociationList`, retaining the assignment domain cast and preserving list
order. In restore, the saved detail iterator remains paired one-for-one with
that ordered assignment traversal. Schedule backup/undo focused tests and diff
check passed.

Follow-up #678 starts from the latest integrated `origin/master` after #677
(HEAD and merge-base verified). Modernized OpenProj-origin `WorkingCalendar.dump`
to build diagnostic text with `StringBuilder` and iterate its typed
`TreeSet<WorkDay>` directly. Added a focused assertion for header, all weekday
entries, and exception entries. Existing concatenation format and exception
ordering are preserved; `WorkingCalendarTest` and `CalendarDefinitionTest`
passed, as did diff check.

Follow-up #681 starts from the latest integrated `origin/master` after #680
(will rebase and verify merge-base). Replaced raw iterators and casts in
`Project.buildReverseQuery` and `Project.forEachWorkingInterval` with enhanced-
for over the existing `LinkedList<Task>` field. Traversal order and per-task
dispatch remain unchanged. Full core tests, application compilation, and diff
check passed.

Follow-up #682 starts from the latest integrated `origin/master` after #681
(HEAD and merge-base verified). Replaced raw task iterators in OpenProj-origin
`Project.setForward` and `repairTasks` with enhanced-for over the existing
`LinkedList<Task>`, retaining the `NormalTask` cast and repair behavior.
Forward/reverse schedule compatibility is covered by the full core suite;
application compilation and diff check passed.

Follow-up #683 starts from the latest integrated `origin/master` after #682
(HEAD and merge-base verified). Parameterized the existing task-outline
iterators in OpenProj-origin `Project.renumber`, `forTasks`, and `getRowHeight`
as `Iterator<Task>`, removing only a redundant `Task` cast. Retained the
`NormalTask` casts, traversal order, and callback contract. Full core tests,
application compilation, and diff check passed.

Follow-up #684 starts from the latest integrated `origin/master` after #683
(HEAD and merge-base verified). Parameterized the two OpenProj-origin child
collection iterators in `TaskSchedule.flagChildren` and
`assignDatesFromChildren` as `Iterator<?>`. Kept runtime node/task filtering,
casts, order, and scheduling calculations unchanged because the child
collection API remains raw. Full core tests, application compilation, and diff
check passed.

Follow-up #685 starts from the latest integrated `origin/master` after #684
(HEAD and merge-base verified). Modernized the same OpenProj-origin child
traversals in `TaskSchedule` to enhanced-for over the actual
`Collection<Node>` API and Java pattern matching for task implementations.
Added a non-task child node to the existing aggregation regression scenario to
verify it remains ignored. Focused and full core tests, application compilation,
and diff check passed; no scheduling bug was found in this scope.

Follow-up #686 starts from the latest integrated `origin/master` after #685
(HEAD and merge-base verified). Modernized `Task.cleanUp`'s dependency snapshot
loops with `LinkedList<Dependency>` and enhanced-for, and fixed a reproduced
bug: `cleanDependencies=false` was ignored, so undoing pasted tasks could delete
their dependency links despite `NodePasteEdit` explicitly requesting that they
be retained. Added tests for both preserve and remove paths; the preserve test
failed before the fix and both pass after it. Full core tests, application
compilation, and diff check passed.

Follow-up #687 starts from the latest integrated `origin/master` after #686
(HEAD and merge-base verified). Typed the local task/dependency staging lists
and removed casts/iterator boilerplate in OpenProj-origin
`DependencyService.connect`, retaining the public raw-List signature and its
read-only filtering, circularity checks, pair-validation order, and sequential
link selection. Existing focused `DependencyServiceTest`, full core tests,
application compilation, and diff check passed; no behavior defect was found.

Follow-up #688 starts from the latest integrated `origin/master` after #687
(HEAD and merge-base verified). Typed `UniqueIdPool.serverIntervals` as
`List<MutableInterval>` and its removal-aware iterators accordingly, preserving
reservation order and exhausted-interval removal. Added a focused test for ID
consumption and diagnostic dump order across two intervals. Review also found
that async reservation admission synchronized on each anonymous `Thread`, not
on the shared pool, so concurrent callers were not mutually excluded. Routed
admission and release through a shared pool monitor and added a concurrent
admission test. Focused/full core tests, application compilation, and diff
check passed.

Follow-up #689 starts from the latest integrated `origin/master` after #688
(HEAD and merge-base verified). Modernized OpenProj-origin
`Task.forSnapshotsAssignments` to iterate its `AssociationList` as
`Association` values, removing an unnecessary size pre-check while retaining
the assignment cast and `Consumer<Object>` callback contract. Full core tests,
application compilation, and diff check passed.

Follow-up #690 starts from the latest integrated `origin/master` after #689
(HEAD and merge-base verified). Modernized the OpenProj-origin assignment and
dependency dirty-state loops in `Project.setAllTasksAsUnchangedFromPersisted`
to enhanced-for over the existing `AssociationList` API, retaining the domain
casts and all dirty-state updates. Full core tests, application compilation,
and diff check passed.

Follow-up #691 starts from the latest integrated `origin/master` after #690
(HEAD and merge-base verified). Modernized the OpenProj-origin assignment
traversals in `NormalTask.getPercentComplete`, `getDuration`, and `hasDuration`
to enhanced-for over `AssociationList`, retaining assignment casts, counting,
max-end selection, and early return semantics. Scheduling-focused and full
core tests, application compilation, and diff check passed.

Follow-up #692 starts from the latest integrated `origin/master` after #691
(HEAD and merge-base verified). Replaced the OpenProj-origin iterator/cast in
`AssignmentEntry.setAssignmentsFromTaskList` with enhanced-for and an
`instanceof Task` pattern variable, preserving the raw public `List` API and
filtering behavior. Added focused coverage that ignores non-Task entries and
tasks not assigned to the resource. Focused/full core tests, application
compilation, and diff check passed.

Follow-up #693 starts from the latest integrated `origin/master` after #692
(HEAD and merge-base verified). Modernized the OpenProj-origin assignment
traversal in `NormalTask.adjustActualStartFromAssignments` to enhanced-for over
the typed `AssociationList`, preserving the existing percentage predicate and
early-exit behavior. Added a focused test for 0% and started-assignment actual
start behavior. Focused/full core tests, application compilation, and diff
check passed.

Follow-up #694 starts from the latest integrated `origin/master` after #693
(HEAD and merge-base verified). Modernized the three OpenProj-origin assignment
traversals in `NormalTask.moveInterval` and `NormalTask.split` to enhanced-for
over the existing typed `AssociationList`, preserving mutation order and
arguments. Relevant scheduling/assignment tests and application compilation
passed. The first full core run had one failure in the unrelated
`CriticalChainServiceTest.previewScalesToLargeSharedResourceWithoutQuadraticExpiryScan`;
its isolated rerun passed. Diff check passed. This transient full-suite failure
is disclosed for review and CI remains the merge gate.

Follow-up #695 starts from the latest integrated `origin/master` after #694
(HEAD and merge-base verified). Modernized the OpenProj-origin assignment loop
in `NormalTask.setStop` to enhanced-for over the typed `AssociationList`,
preserving the stop update and earliest-nonzero-actual-start reduction. Added a
focused regression test using a multi-day assignment and a mid-task stop.
Focused `NormalTaskPercentCompleteTest` and application compilation passed.
The full core suite currently fails the unrelated
`CriticalChainServiceTest.previewScalesToLargeSharedResourceWithoutQuadraticExpiryScan`
runtime assertion (16.4 s vs. its 10 s limit in an isolated local run); PR CI
must pass before merge. No GUI behavior is changed.

Follow-up #696 starts from the latest integrated `origin/master` after #695
(HEAD and merge-base verified). Modernized the OpenProj-origin assignment
reduction in `NormalTask.calcOffsetFrom` to enhanced-for over the existing
typed `AssociationList`, retaining the task-duration fallback for unassigned
tasks and max/min selection. Added coverage comparing task and real-assignment
offsets for 0%/25% progress and ahead/behind calculations. The focused test
class and application compilation passed. The full core suite has the same
unrelated CriticalChain runtime-threshold failure noted above; PR CI is the
merge gate.

Follow-up #697 starts from the latest integrated `origin/master` after #696
(HEAD and merge-base verified). Modernized the OpenProj-origin assignment
traversal in `NormalTask.setCompletedThrough` to enhanced-for over the typed
`AssociationList`, preserving earliest actual-start reduction and schedule
notifications. Added focused coverage for propagating the completed-through
date and actual start from a real resource assignment. Focused
`NormalTaskPercentCompleteTest`, application compilation, and diff check passed.

Follow-up #698 starts from the latest integrated `origin/master` after #697
(HEAD and merge-base verified). Modernized the OpenProj-origin assignment loop
in `NormalTask.setEnd` to enhanced-for over the typed `AssociationList`,
preserving the assignment-end update and later duration handling. Added a
focused regression test asserting that extending a task finish extends its
assigned resource's finish. `NormalTaskDurationTest`, application compilation,
and diff check passed. The unrelated full-core CriticalChain timing instability
remains disclosed in #695/#696 and is guarded by required PR CI.

Follow-up #699 starts from the latest integrated `origin/master` after #698
(HEAD and merge-base verified). Modernized the interval-specific OpenProj-
origin `NormalTask.setActualWork` assignment traversal to enhanced-for over the
typed `AssociationList`, preserving per-assignment interval updates. Added a
focused interval-work regression with an explicitly scheduled task and
assignment. Focused `NormalTaskPercentCompleteTest`, application compilation,
and diff check passed.

Follow-up #700 starts from the latest integrated `origin/master` after #699
(HEAD and merge-base verified). Modernized the OpenProj-origin predicate loop
in `NormalTask.isInvalidIntersectionCalendar` to enhanced-for over the typed
`AssociationList`, preserving short-circuit behavior. Strengthened the existing
headless invalid-calendar regression to assert the task indicator exposes the
invalid intersection cached on its assignment. `InvalidIntersectionFreezeTest`,
application compilation, and diff check passed.

Follow-up #701 starts from the latest integrated `origin/master` after #700
(HEAD and merge-base verified). Modernized the OpenProj-origin repair-state
loops in `NormalTask.setTaskAssignementAndPredsDirty` to enhanced-for over the
typed assignment/dependency association lists, preserving the distinct dirty
flag values (assignments true, predecessor dependencies false). Added focused
coverage for both flags. The focused regression, full
`NormalTaskPercentCompleteTest`, application compilation, and diff check passed.

Follow-up #702 starts from the latest integrated `origin/master` after #701
(HEAD and merge-base verified). Confirmed `AssignmentEntry` matches the
OpenProj baseline and remains active through resource-team filtering and
assignment-entry UI models. Modernized `setRequestDemandType` iteration over
its legacy raw `ArrayList` using enhanced-for while preserving the raw public
constructor/API and cast behavior. Added focused coverage that the request type
reaches the assignment. `AssignmentEntryTest`, application compilation, and
diff check passed.

Follow-up #703 starts from the latest integrated `origin/master` after #702
(HEAD and merge-base verified). Modernized the OpenProj-origin `AssignmentEntry.setRate`
loop over its legacy raw `ArrayList` to enhanced-for, preserving per-assignment
labor/non-labor branches, units handling, and rate event dispatch. Added a test
that applies one rate through an entry containing two assignments and verifies
both update. Removed the now-unused `Iterator` import after both active loops
were modernized. `AssignmentEntryTest`, application compilation, and diff check
passed; the public raw collection boundary remains unchanged.

Follow-up #704 starts from the latest integrated `origin/master` after #703
(HEAD and merge-base verified). Modernized the active OpenProj-origin
`DependencyService.remove(Collection, Collection)` snapshot loop to enhanced-for,
preserving the raw public Collection signatures and the copy-before-remove
semantics used by `Task.cleanUp`. Existing `TaskCleanupTest` and
`DependencyServiceTest`, application compilation, and diff check passed.

Worktree follow-up after #704 modernizes the OpenProj-origin
`DependencyService.remove(Collection, Object)` traversal to enhanced-for,
preserving the raw collection API, encounter order, and per-entry dependency
cast. The loop matches `d2fa3c20a:openproj_core/src/com/projity/pm/dependency/DependencyService.java`.
Added a focused regression that removes two incident links from an input
snapshot; `DependencyServiceTest` and full `:microproject_core:test` passed.

Additional worktree follow-up modernizes OpenProj-origin traversals in
`Serializer.saveTasks` from `origin/master`'s equivalent method: task outline,
predecessor-link, and successor-link traversals now use enhanced-for over the
already materialized task list and typed `AssociationList`. The subsequent
distribution cleanup traversal over `distMap.values()` also uses enhanced-for;
the iterator that removes unchanged values from `dist` remains because it uses
`Iterator.remove()`. Encounter order, project/external filters, dirty-link
handling, predecessor accumulation, distribution statuses, and external-link
serialization are preserved. The source paths were compared with
`d2fa3c20a:openproj_exchange/src/com/projity/server/data/Serializer.java`.
Full `:microproject_exchange:test` passed, including native POD and exchange
round-trip coverage. In the resource/task deserialization path, the second
assignment loop also now uses enhanced-for; the preceding filter loop retains
its iterator because it removes entries. This loop matches the same OpenProj
baseline, and the full exchange suite passed after the change.

Core follow-up modernizes the OpenProj-origin `Project` baseline and task
aggregation scans over the existing `LinkedList<Task>` to enhanced-for. The
baseline paths retain the `NormalTask` cast; aggregation paths retain the
zero handling, extrema, short-circuiting, and duration accumulation behavior.
The exact methods were checked against
`d2fa3c20a:openproj_core/src/com/projity/pm/task/Project.java`.
`ProjectScheduleBehaviorTest`, `NormalTaskPercentCompleteTest`, and the full
`:microproject_core:test` suite passed.

Further Project follow-up converts the read-only `dump(Collection, String)` and
`updateDistributionMap()` traversals to enhanced-for while keeping the raw
collection boundary, cast points, encounter order, recursive output, and
project-ID filter unchanged. Both methods match the OpenProj baseline
`d2fa3c20a:openproj_core/src/com/projity/pm/task/Project.java`.
The full `:microproject_core:test` suite passed.

Resource follow-up modernizes the source-backed resource name/ID lookup scans in
`ResourcePool` and the baseline-visibility scan in `EnterpriseResource` to
enhanced-for. `ResourceImpl` assignment predicates also use enhanced-for over
the existing raw `AssociationList`, retaining assignment casts and short-circuit
behavior. `ResourcePoolIdentityTest` covers first-match and missing-value
lookups; focused and full `:microproject_core:test` runs passed.

`SharedResourcePoolService.rewireAssignments` now uses enhanced-for over the
same detached `ArrayList` snapshot while it removes and reconnects each live
assignment. This preserves the protection against mutating the source
association list during traversal and its encounter order. The focused
`SharedResourcePoolServiceTest` and full `:microproject_core:test` suite passed.

Dependency snapshot collection in `DependencyService.getIncidentDependencies`
and project baseline visibility traversal now use enhanced-for over their
existing collections. The snapshot still lists predecessor links before
successor links, including existing duplicate semantics; visibility retains
the `foundChild` rule and early return. `DependencyServiceTest`,
`ProjectScheduleBehaviorTest`, and the full `:microproject_core:test` suite
passed.

`Project` now traverses the repaired-task list with enhanced-for, preserving
the `NormalTask` cast, encounter order, dirty-state update, and the subsequent
list reset. This loop matches `d2fa3c20a:openproj_core/src/com/projity/pm/task/Project.java`.
The full `:microproject_core:test` suite passed.

`DependencyService.fireTaskPredecessors` now traverses the input task collection
and each predecessor association list with enhanced-for, retaining the existing
casts and task/link encounter order. The method matches the OpenProj baseline
`d2fa3c20a:openproj_core/src/com/projity/pm/dependency/DependencyService.java`.
`DependencyServiceTest.fireTaskPredecessorsPreservesTaskAndLinkEncounterOrder`
and the focused `:microproject_core:test --tests
"com.microproject.pm.dependency.DependencyServiceTest"` passed.

`AssignmentService.remove(Collection, Object)` now uses enhanced-for over its
read-only input snapshot, retaining the per-item `Assignment` cast and removal
order. This loop matches the OpenProj baseline
`d2fa3c20a:openproj_core/src/com/projity/pm/assignment/AssignmentService.java`.
`AssignmentServiceTest.singleEventSourceRemovalPreservesInputSnapshotAndRemovesAssignments`
and the focused AssignmentService test class passed.

`ReportAdapter` now uses typed enhanced-for traversal for active report field
generation, aggregate-variable creation, headers, detail rows, and footers.
Private raw `ArrayList` parameters now accept `SpreadSheetFieldArray`, the type
used by every caller. Field order and generated Jasper field names are covered
by `ReportAdapterTest.designFieldTraversalPreservesConfiguredFieldOrder`; the
full `:microproject_reports:test` suite passed. All converted loops match the
OpenProj source at `d2fa3c20a:openproj_reports/src/com/projity/reports/adapter/ReportAdapter.java`.

`MenuManager` action-state propagation now traverses registered toolbar buttons
with enhanced-for in the enabled, visible, selected, and tooltip paths. Existing
`AbstractButton`/`JCommandToggleButton` handling and menu-item updates are
unchanged. The methods match the OpenProj baseline
`d2fa3c20a:openproj_ui/src/com/projity/menu/MenuManager.java`; the focused
`:microproject_ui:test --tests
"com.microproject.menu.RibbonAndToolbarButtonTest"` passed.

`ApplicationStartupFactory.dumpOpts` now traverses the parsed option keys with
enhanced-for, preserving map encounter order and each option's indexed argument
logging. The method matches `d2fa3c20a:openproj_ui/src/com/projity/pm/graphic/frames/ApplicationStartupFactory.java`.
`ApplicationStartupFactoryProjectIdTest.startupOptionLoggingPreservesMapAndArgumentOrder`
and the focused startup-factory test class passed.

`ChangeWorkingTimeDialogBox` now filters the extracted project calendars with
enhanced-for, retaining the order and exclusion of base calendars. The loop
matches `d2fa3c20a:openproj_ui/src/com/projity/dialog/calendar/ChangeWorkingTimeDialogBox.java`.
The existing focused `ChangeWorkingTimeDialogBoxSaveTest` passed; its
non-headless dialog case constructs this calendar list.

`PredecessorTaskList` read traversals in `dump`, `rearrangeAll`, reverse-
scheduled-count recalculation, and `findTaskPosition` now use enhanced-for over
the existing linked list. Encounter order, position counting, early termination,
and the detached `oldList` traversal during rebuild are unchanged. Mutation
loops using `Iterator.remove()` or `ListIterator.add()` remain explicit.
`PredecessorTaskListTraversalTest.taskPositionsMatchListEncounterOrder` and the
focused critical-path tests passed. The changed traversal structure corresponds
to `d2fa3c20a:openproj_core/src/com/projity/pm/criticalpath/PredecessorTaskList.java`.

`ChartLegend` now uses enhanced-for for Project expansion and resource filtering
in selection handling. Project traversal and filtered resource encounter order
are unchanged. Both loops match
`d2fa3c20a:openproj_ui/src/com/projity/pm/graphic/chart/ChartLegend.java`; the
full `:microproject_ui:test --console=plain --max-workers=1` suite passed.

Adjacent cleanup, excluded from issue #595: `ToolBarFactory.initJComponent`
traverses configured toolbar items with enhanced-for, preserving item and
separator order. Its matching source is under
`d2fa3c20a:openproj_ui/src/org/apache/batik/util/gui/resource/ToolBarFactory.java`,
which is bundled Apache Batik code rather than OpenProj-derived code. The
focused `RibbonAndToolbarButtonTest` passed; this change is not counted toward
the #595 modernization scope.

`ResourceManager.getStringList` still exposes raw `List` values, and its menu
and ribbon callers contain unchecked `List<String>` casts. Although the method
returns only `StringTokenizer` tokens and appears to be a clear typing candidate,
the implementation is bundled Apache Batik code at
`d2fa3c20a:openproj_ui/src/org/apache/batik/util/gui/resource/ResourceManager.java`.
It is classified as third-party and excluded from #595; do not count or expand
this modernization into the bundled dependency. The in-scope callers should be
revisited only if their own provenance is independently established.

`TimeSpreadSheetColumnsPopupMenu` now traverses configured field definitions
with enhanced-for, preserving menu item order and selected-state lookup. The
source matches `d2fa3c20a:openproj_ui/src/com/projity/pm/graphic/spreadsheet/selection/TimeSpreadSheetColumnsPopupMenu.java`;
the full `:microproject_ui:test --console=plain --max-workers=1` suite passed.

`AssignmentEntryPane` now traverses the selected assignment entries with
enhanced-for, preserving selection order and the `assignedOnly` filter. The
loop matches `d2fa3c20a:openproj_ui/src/com/projity/dialog/assignment/AssignmentEntryPane.java`;
the full `:microproject_ui:test --console=plain --max-workers=1` suite passed.

`ChartModel.computeOtherProjects` now traverses the chosen task/resource list
with enhanced-for, preserving the existing resource selection and aggregation
order. The time iterator remains explicit because it is a dedicated API, not a
Java collection. This matches
`d2fa3c20a:openproj_ui/src/com/projity/pm/graphic/chart/ChartModel.java`; the
full `:microproject_ui:test --console=plain --max-workers=1` suite passed.

`GraphicNode` cached-schedule interval consumption and containment now use
enhanced-for over the existing interval list, preserving encounter order and
early return behavior. The source matches
`d2fa3c20a:openproj_ui/src/com/projity/pm/graphic/model/cache/GraphicNode.java`.
`GraphicNodeIntervalTraversalTest` verifies both consumers.

`NodeCache.fireEvents` now traverses registered visible-node views with
enhanced-for, preserving registration order and per-view event clearing.
The loop matches
`d2fa3c20a:openproj_ui/src/com/projity/pm/graphic/model/cache/NodeCache.java`.
`NodeCacheEventTraversalTest` verifies delivery order and cleanup.
The same cache path exposed an existing null-handling defect: its empty-set
capacity branch accepted a null update set, but `addAll` then dereferenced it.
`updateVisibleElements` now treats null as an empty incremental change set, and
the test verifies this call completes with an empty visible cache.

`CellCache.clear` now traverses registered visible elements with enhanced-for,
still clearing each view after the backing cache and before the base index.
The loop matches `d2fa3c20a:openproj_ui/src/com/projity/pm/graphic/model/cache/CellCache.java`.
`NodeCacheEventTraversalTest` verifies that every registered view is cleared.

`DependencyCache` read-only registered-view and backing-cache traversals now use
enhanced-for; the loop that removes dependencies from a visible list retains
`Iterator.remove()`. Its null change-set path now matches NodeCache's empty-set
contract instead of dereferencing null. The source matches
`d2fa3c20a:openproj_ui/src/com/projity/pm/graphic/model/cache/DependencyCache.java`;
`DependencyCacheTraversalTest` passed.

`VisibleElements.getRow` now traverses elements with enhanced-for and an explicit
position counter, preserving the first equal element's row and the `-1` miss
result. It matches
`d2fa3c20a:openproj_ui/src/com/projity/pm/graphic/model/cache/VisibleElements.java`;
`VisibleElementsTraversalTest` passed.

`NodeCache.createUpdateDiff` now traverses rows with enhanced-for and advances
an explicit row index, preserving adjacent-row coalescing and emitted cache
intervals. It matches the OpenProj NodeCache source; the existing
`NodeCacheEventTest.changedRowsRetainEncounterPositionsAndContiguousRanges`
asserts changed-node order and exact row ranges.

`NodeCache.updateVisibleElements(VisibleNodes, Set)` now scans the cached rows
with enhanced-for, preserving collapsed-summary pruning and subsequent sibling
visibility. `NodeCacheHierarchyTraversalTest` verifies that a collapsed parent
hides its descendants without hiding the next sibling; the loop matches the
OpenProj NodeCache implementation.

The shared-event overload `NodeCache.fireEvents(source, nodeEvents, edgeEvents)`
also uses enhanced-for across registered views. Event delivery remains in view
registration order; `NodeCacheEventTraversalTest.firesSharedEventsInRegistrationOrder`
passed against the matching OpenProj implementation.

`Intervals` now uses enhanced-for for source interval copying, bulk addition, and
date containment lookup. The range-merging and weekday-pruning loops retain
explicit iterators because they call `Iterator.remove()`. The conversion matches
`d2fa3c20a:openproj_ui/src/com/projity/dialog/calendar/Intervals.java`;
`IntervalsTraversalTest` passed.

`DefaultFrameManager` now traverses its frame list during cleanup and persisted
workspace entries during restoration with enhanced-for, preserving their list
order. Both loops match
`d2fa3c20a:openproj_ui/src/com/projity/pm/graphic/frames/workspace/DefaultFrameManager.java`.
The focused `DefaultFrameManagerTest` passed.

`ResourceView` now shares the child-policy traversal used by delete and cut
eligibility. This exposed and fixes a pre-existing copy/paste defect: recursive
cut eligibility previously called the delete policy for descendants, suppressing
the cut-specific warning and move hint. The focused `ResourceViewChildPolicyTest`
passed. The surrounding implementation matches the OpenProj `ResourceView`
source; no physical route or selection behavior changed.

`GraphModel.searchJustModifiedNodes` now uses `instanceof` pattern matching for
task implementations, preserving the modified-task filter and encounter order.
Its implementation matches
`d2fa3c20a:openproj_ui/src/com/projity/pm/graphic/graph/GraphModel.java`.

Adjacent cleanup, excluded from issue #595: `MenuFactory` menu-bar,
popup-menu, and menu-item list traversal uses enhanced-for, preserving configured
item order and visibility filtering. Its matching source is under
`d2fa3c20a:openproj_ui/src/org/apache/batik/util/gui/resource/MenuFactory.java`,
which is bundled Apache Batik code rather than OpenProj-derived code. The
focused `RibbonAndToolbarButtonTest` passed; this change is not counted toward
the #595 modernization scope.

`DataSource.setNodeBased` now uses an `instanceof` pattern binding to forward
the mode to predicated iterators without a redundant cast. This conditional
matches `d2fa3c20a:openproj_reports/src/com/projity/reports/adapter/DataSource.java`.
`DataSourceTest` verifies the mode reaches the iterator.

`FontUtil` now types its Swing defaults enumerations, uses pattern matching when
returning a `FontUIResource`, and traverses available-font arrays with
enhanced-for. These implementations match the OpenProj `FontUtil` source; font
selection priority and enumeration order remain unchanged.

`NodeWalker` now binds its `SummaryVisitor` closure with `instanceof` pattern
matching, keeping the existing visitor selection contract. The constructor
matches the OpenProj `NodeWalker` source; existing child-walker tests exercise
the visitor path.

`TexturedShape.applyPaint` now binds a `PredefinedPaint` with pattern matching
and removes its immediate cast. The type-check/cast branch matches the OpenProj
source and continues to call the same paint implementation with the same
graphics context and texture flag.

`AssociationList.find` and `findAssociation` now use enhanced-for over their
backing linked lists. They retain identity comparisons, exclusion behavior,
first-match selection, and encounter order. Both loops match the OpenProj
`AssociationList` source; the focused association lookup regression passed.

`TaskFormula.evaluate` and `AssignmentFormula.evaluate` now bind their expected
domain types with `instanceof` patterns while retaining `Boolean.FALSE` for
other and null inputs. The methods correspond to the OpenProj formula sources;
`TaskFormulaTest` covers task dispatch and both rejection cases.

`UniqueIdPool.dump` now traverses reserved intervals with enhanced-for and an
explicit separator flag, preserving interval order and the exact dump format.
The loop matches OpenProj `UniqueIdPool`; existing `UniqueIdPoolTest` assertions
cover empty, single, and multiple intervals. The mutating `getId` loop retains
its iterator because it removes exhausted intervals in place.

`Range.validate` now binds a numeric input with pattern matching and removes
redundant casts while preserving its non-number skip and lower/upper bound
errors. The conditional matches OpenProj `Range`; focused `RangeTest` covers
non-numeric, in-range, and both out-of-range cases.

`TransformList.getFactories` now uses enhanced-for over its typed factory
list, preserving authorization filtering and factory order. The traversal
matches the OpenProj `TransformList`; `TransformListTest` verifies that only
the authorized no-filter factory is returned.

`NodeSorter.sortList` now uses enhanced-for over the supplied list while
retaining its element cast, recursive child sorting, and comparator order. The
loop matches OpenProj `NodeSorter`; `NodeSorterTraversalTest` checks root and
nested sort order.

`Field` now uses pattern bindings for its OpenProj-derived `DelegatesFields`
checks in applicability, value retrieval, and read-only evaluation. The three
branches keep their separate contracts. `FieldComparisonTest` verifies that a
delegated object can declare a field applicable.

`Field.isZero` now binds Number and String values with pattern matching and
preserves its numeric-zero and empty-string checks. The method matches OpenProj
`Field`; `FieldComparisonTest` covers zero, non-zero, empty, non-empty, and null
inputs.

`DataUtils.extractObjectOfClass` now binds assignment inputs once and uses the
binding to resolve the requested task or resource. The branch matches OpenProj
`DataUtils`; `DataUtilsTest` covers both projections and rejection of an
unrelated object.

`DataUtils.extractObjectsOfClassFromNodeList` now uses enhanced-for while
retaining node casts, result order, and duplicate suppression. The loop matches
OpenProj `DataUtils`; `DataUtilsTest` verifies ordered extraction and duplicate
handling.

`SplitEdit` and `ScheduleEdit` now share `UndoEditPresentationName` for their
`DataObject` type check and displayed class/name/ID, replacing the duplicated
null/type checks and casts with an `instanceof` pattern. Both action prefixes
and the fallback label are covered by `SplitEditTest`. The `SplitEdit` display
path matches OpenProj; fork-specific `source` versus `this` arguments in
undo/redo were intentionally left untouched.

`SelectionFilter.evaluate` now binds its OpenProj-derived `Assignment` branch
with `instanceof` pattern matching, retaining task-or-resource membership
semantics. The type-check/cast branch matches the OpenProj implementation;
`SelectionFilterTest` now covers selected-task, selected-resource, and unrelated
task outcomes.

`FieldConverter._convert` now binds its OpenProj-derived String input once and
passes that binding to both configured and fallback converters, removing
redundant casts without changing converter selection. The branch matches the
OpenProj implementation; `FieldConverterTest` covers conversion through the
public API. The calendar converter's String branch also binds the input once;
`FieldConverterTest` verifies parsing through the registered date and calendar,
work, and money converters. The duration converter path remains covered by
`DurationEncodingTest`.

`TimeDistributedHelper` now binds `Field`, `Number`, and `String` values in its
existing type checks instead of repeating casts. The corresponding OpenProj
methods have the same branches and expressions in the
[OpenProj preservation source](https://github.com/OldRepoPreservation/projectlibre/blob/master/openproj_core/src/com/projity/pm/assignment/TimeDistributedHelper.java).
Active callers include assignment time-phased data and spreadsheet field
configuration. `TimeDistributedHelperTest` covers field classification,
numeric boundaries, null handling, and string/field ID resolution; the change
does not alter those contracts.

`NumericMinimum.addToSummary` now binds the accepted `Number` and reuses it for
conversion. The OpenProj source has the same `Number` type check and numeric
conversion, but its non-number path differs from this fork: microProject's
existing early return is retained so non-numeric values remain ignored. The
[OpenProj source](https://github.com/OldRepoPreservation/projectlibre/blob/master/openproj_core/src/com/projity/grouping/core/summaries/NumericMinimum.java)
provides provenance for the typed branch; `SummaryVisitorFactoryTest` verifies
the fork's ignore-and-minimize behavior.

`IntervalGeneratorSet.earliestEndingGenerator` now uses enhanced-for over its
typed generator list. The traversal is read-only; the current earliest-end
selection and encounter-order tie behavior are unchanged. The active loop
corresponds to the OpenProj traversal, while its fork-specific selection logic
remains as implemented. `IntervalGeneratorContractTest` covers unique and tied
earliest endpoints.

`SnapshottableImpl`'s save/clear consumer closures now bind accepted
`Snapshottable` values and call through the bindings. The same guarded calls
appear in the [OpenProj source](https://github.com/OldRepoPreservation/projectlibre/blob/master/openproj_core/src/com/projity/pm/snapshot/SnapshottableImpl.java).
`Project.saveCurrentToSnapshot` and `Project.clearSnapshot` use these closures
to traverse selected objects; `SnapshottableImplTest` verifies snapshot save,
clear, and ignoring unrelated objects.

`HasUniqueIdImpl.equals` now binds a matching identifier implementation and
uses it to compare IDs without a cast. The
[OpenProj implementation](https://github.com/OldRepoPreservation/projectlibre/blob/master/openproj_core/src/com/projity/pm/key/HasUniqueIdImpl.java)
has the same guard and comparison. This equality is inherited by key wrappers such as
`HasKeyImpl`; `EqualsHashCodeContractTest` verifies equal IDs, matching hash
codes, and rejection of null/foreign types.

`SpreadSheetFieldArray.equals` now binds its matching type with Java pattern
matching and compares the name through that binding. The corresponding OpenProj
branch uses the same type guard and name lookup; the existing fork value
comparison is preserved. `SpreadSheetFieldArrayTest` covers equal names,
matching hash codes, and rejection of null/foreign types.

`NodeModelUtil.nodeIsSubproject` now uses the `SubProj` contract as its single
classification rule. `Task.isSubproject()` already derives its value from that
same contract, so the previous second branch duplicated the predicate and
allowed an invalid `Task` override to classify a non-`SubProj` object as a
subproject. `TaskSchedule.invalidate` assumes a subproject task implements
`SubProj`; violating that invariant throws `ClassCastException`. A non-Task
object can implement `SubProj`, so the interface check still covers that valid
case. `NodeModelUtilTest` covers an interface-only reference, the built-in
Task plus SubProj placeholder, and an ordinary task. Active callers include
`MutableNodeHierarchy` and `NodeBridge`. `NodeModelUtil.canBeChildOf` now binds
both task implementations
before comparing their owning projects; the original same-project restriction
and non-task behavior are unchanged. The expanded test covers same-project,
cross-project, and subproject-parent cases through the `NodeBridge` caller.
`NodeModelUtil.cacheWbs` now binds parent and child tasks during cache setup;
the projected-child test verifies the cached WBS parent through the active
`DefaultSubprojectHandler` path.

`FieldEdit` now binds `ObjectRef` once in its undo and redo dispatch. The
branches match OpenProj; `FieldEditTest` verifies both reference and plain
object targets preserve the target, source, and old/new values on both routes.
Active callers include `FieldVerifier` for spreadsheet/dialog edits and
`ScheduleService` for scheduling field undo.

`Field.setValue` now pattern-binds String input before option-field text
preprocessing. The OpenProj branch is source-identical; `FieldConverterTest`
verifies the original string reaches preprocessing and its converted value is
stored for the same target.

`Field.setInternalValue` now binds an unwrapped `FieldParseException` from a
reflective setter and rethrows that same instance. The OpenProj type-check/cast
branch is preserved; `FieldSetValueTest` verifies exception identity across the
reflection boundary.

`Field.isReadOnly` now resolves a `BelongsToHierarchy` object once and shares
that binding between task-sheet schedule editing and summary-field checks. The
OpenProj summary-parent behavior is preserved, and `FieldComparisonTest`
verifies that only hierarchy parents are read-only for summary fields.

`FieldDictionary.setAliasMap` now uses enhanced-for over the alias map's key
set. Alias lookup, ignoring unknown field IDs, and field mutation remain the
same. The loop matches the OpenProj implementation; `FieldDictionaryTest`
verifies a known alias is applied and an unknown field is ignored.

`MutableNodeHierarchy` paste handling now binds parent tasks, descendant tasks,
and descendant resources directly in their OpenProj-derived `instanceof`
branches, removing three immediate casts without changing validation, project
assignment, resource initialization, or insertion order. The full core tests
were run for this hierarchy modernization batch.

`AssignmentNodeModel.paste` now binds leaf assignments and parent tasks in its
OpenProj-derived matching branches, removing the adjacent casts while
preserving the free-assignment check and reconnection path. The original
`instanceof` branches match OpenProj; core tests validate the owning model and
assignment services after this behavior-preserving syntax change.

`ResourcePool.initializeOutlines` now binds `AssignmentNodeModel` in its
OpenProj-derived setup branch. The active path is `ResourcePool.createRourcePool`
through outline initialization; it must set the pool as the outline document
before resource assignment events are processed. `ResourcePoolIdentityTest`
asserts that binding, and the focused resource-pool test class passed.

`Project.initializeOutlines` and `postDeserialization` repeated the same
assignment-outline binding and undo-controller loop. Consolidated that shared
responsibility in `bindAssignmentOutlines`, preserving both lifecycle call
orders and the following default-outline initialization. `disconnectOutlines`
also binds the assignment model before detaching its document listener. The
type-check/cast branches correspond to OpenProj `Project` lifecycle code;
`ProjectScheduleBehaviorTest` verifies attach/detach/re-attach, and
`PodRoundTripTest` verifies the deserialization route. Both focused suites
passed.

`Serializer`'s outline writer now binds `Task` in its OpenProj-source-matching
`instanceof` branch instead of immediately casting `outlineChild`. The active
caller is project serialization during POD save; `PodRoundTripTest` verifies
that the persisted task outline still reloads correctly.

The same POD save path now sizes each per-task assignment output list from the
current assignment count. Older baseline snapshots can contribute additional
entries, so that count is an estimate rather than a cap; `PodRoundTripTest`
passed after the allocation change.

`DefaultNodeModel.addAssignments` now binds the OpenProj-derived
`HasAssignments` check and uses a typed reverse `ListIterator<Association>`.
Its active caller is the task-copy branch, which reconstructs assignment rows
in original order. `DefaultNodeModelTest.copyRebuildsAssignmentRowsForCopiedTask`
checks that the copy has a distinct assignment connected to the same resource;
the focused test passed.


`DefaultNodeModel.cloneNode` now binds the OpenProj-derived task checks for
dependency collection and parent WBS-cache rebuilding. Its active task-copy
caller is covered by `DefaultNodeModelTest`, including a nested summary-task
copy that verifies the copied parent cache points at the copied child node.
The focused test class passed with these copy-path assertions.

`DefaultNodeModel.cloneNodeImpl` and `cleanNodeImpl` now bind OpenProj-derived
`NormalTask` and `ResourceImpl` branches instead of recasting their inputs. The
active task-copy path invokes both methods through `cloneNode` and `cleanBranch`;
`DefaultNodeModelTest` verifies copied assignment reconstruction and nested WBS
cache rebuilding. The focused test class passed.

`DefaultNodeModel.removeApartFromHierarchy` now binds the OpenProj-derived
`Assignment` case used while hierarchy deletion walks a task's assignment rows.
The traversal still calls `AssignmentService.remove` with the original cleanup,
undo, and dependency flags. `DefaultNodeModelTest.removingAssignedTaskCleansResourceAssignmentAndUndoState`
verifies task deletion removes its resource assignment and Undo/Redo restores and
removes both sides. The focused test passed.

`Field.setValue` now binds `DataObject` in its OpenProj-derived reflective-setter
path before marking the object dirty. The separate task-sheet scheduling path is
fork-owned and remains untouched. `FieldSetValueTest.marksDataObjectDirtyAfterReflectiveFieldSet`
verifies a successful reflected write still marks the target dirty; the focused
test passed.

`Field.invokeAction` now binds the OpenProj-derived `Hyperlink` result from the
configured field before invoking it. Its active UI caller is
`GraphicManager`'s field-action route. `FieldSetValueTest.invokesHyperlinkReturnedByConfiguredField`
verifies dispatch without opening a browser; the focused test passed.

`FieldConverter.StringConverter` now binds OpenProj-derived `Work`, `Duration`,
`Money`, and `Date` values before formatting. `LongConverter` binds `Date` and
`GregorianCalendar`; its duration branch binds `Duration`, which also covers
`Work` because `Work extends Duration`. Existing public conversion and field
rendering callers are unchanged. `FieldConverterTest.formatsAndConvertsOpenProjTimeValuesWithoutChangingTheirValues`
verifies Work, Money, and Date formatting plus epoch-millisecond values for
Date, Calendar, and Work; the focused test passed.

The same OpenProj-derived converter family now uses pattern bindings in
`DateConverter`, `CalendarConverter`, `DurationConverter`, `WorkConverter`,
`DoubleConverter`, and `MoneyConverter`. Conversion ordering and results are
unchanged, including zero-millisecond dates mapping to null and Work retaining
its millisecond value when converted to Duration. These converters remain behind
the existing `FieldConverter.convert` entry point used by field parsing and
display. The expanded `FieldConverterTest` also checks converter identity and
zero-date behavior; focused and core-module tests passed.

`Project.getFactoryToUseForChildOfParent` now binds its OpenProj-derived `Task`
argument and uses the bound task to resolve the enclosing project. The public
factory method and its `DefaultNodeModel.createChildNode` caller are unchanged.
`ProjectScheduleBehaviorTest.childFactoryUsesParentTaskEnclosingProject`
verifies null and non-task parents still select the current factory, while a
task belonging to another project selects that enclosing project.

`Project.equals` now binds the OpenProj-derived `DataObject` before comparing
names. `ProjectScheduleBehaviorTest.projectEqualityUsesNameOnlyForDataObjects`
verifies equal names remain equal across Project instances and non-DataObject
values remain unequal.

`Project.setAllTasksAsUnchangedFromPersisted` now binds the OpenProj-derived
`NormalTask` node implementation before clearing its dirty state and recording
the persisted schedule boundaries. The task-outline traversal and repaired-task
path are unchanged. `ProjectScheduleBehaviorTest.markingProjectTasksUnchangedClearsNormalTaskDirtyState`
verifies dirty-state clearing and saved start/finish values.

`Project.nodesChanged` now binds `Task` values both for the changed node and
for descendants visited while removing cross-parent dependencies. The
hierarchy event flow and dependency cleanup are unchanged.
`DefaultNodeModelTest.indentAndOutdentKeepTaskIdsUniqueAndInDepthFirstOrder`
exercises actual model indent/outdent transitions through this listener.

`CriticalPath.objectChanged` now binds OpenProj-derived Task, Dependency,
Assignment, and BelongsToDocument event payloads. Event ordering, early returns,
recalculation, and cross-document filtering are preserved. Existing
`DependencyServiceTest` and core scheduling tests exercise dependency and task
event paths; the core module suite verifies the full dispatch family.

`NormalTask.cloneTo` now binds a NormalTask destination before copying the
OpenProj-derived scheduling fields. Targets of other Task subtypes still skip
that subtype-specific copy. `TaskFontPropertiesTest` and
`TaskVisibilityStateTest` exercise the active NormalTask clone path.

`DependencyService.removeAnyDependencies` now routes its OpenProj pairwise
selection path and the later #266 incident-link path through one
`writableDependencies` resolver. It preserves the interface check and existing
read-only guard while removing three copies of that eligibility decision.
`DependencyServiceTest` covers read-only selections, non-`HasDependencies`
values, editable pairs, and a single selected incident endpoint.

`NormalTask.getEarliestStop` now binds child Schedule values before comparing
their stop dates. It continues to ignore non-Schedule node implementations and
uses assignment dates for leaf tasks. `NormalTaskPercentCompleteTest` verifies
both leaf assignment and summary child results.

`HasAssignmentsImpl.updateAssignment` now binds the matching `Assignment` in
its existing `instanceof` guard. The `ListIterator` remains necessary because
the method replaces the matching list entry; task/resource identity matching,
first-match behavior, and replacement order are unchanged. The full core test
suite passed.

The descendant traversal in `NormalTask.setStop` now binds `NormalTask` values
in the existing guard. Assignment nodes continue to be skipped, and the stop
date remains clamped to each descendant's end. `NormalTaskPercentCompleteTest`
passed. The core module suite passed immediately before this syntax-only
change.

`ProjectFactory.getSaveProjectJob` now binds the OpenProj-derived `Project`
value in its existing type guard before applying the branch-save eligibility
check. Project traversal order, date refresh, and the fork's dirty-child save
rules are unchanged. The full core test suite passed.

The OpenProj-derived `ProjectFactory.getCloseProjectsOnServerJob(Project)` and
`getRemoveProjectJob` traversal callbacks now bind `Project` implementations
directly in their existing type checks. The close and removal workflows retain
their separate post-processing and order; the null-node guard added by the fork
is unchanged. The full core test suite passed.

In OpenProj-derived `Serializer` deserialization, resource/task renumbering
visitors and the subproject reference map now bind `HasId` / `SubProj` values
in their existing guards. Renumbering order, nonpositive ID handling,
subproject keys, and outline insertion order are unchanged. The full
`:microproject_exchange:test` suite passed, including POD round-trip cases.

`PrintDocument` now binds `GanttSettings`, `ScaleToSettings`, and
`FitToSettings` in the existing settings scan. The serialized settings classes,
list order, selected scaling index, and visibility values are unchanged. The
full `:microproject_ui:test` suite passed; no physical GUI route or visual
surface changed.

`PrintDocumentFactory.createDocument` now binds the OpenProj-derived `TreeView`
inside its existing view dispatch, removing the follow-up cast. WBS/RBS cache
selection and renderer configuration are unchanged. The full
`:microproject_ui:test` suite passed; no physical GUI route or visual surface
changed.

`PrintDocumentFactory.getInstance` now synchronizes lazy initialization to
prevent concurrent callers from publishing separate factories. Its protected
field and public constructor remain unchanged. `PrintDocumentFactoryTest`
verifies shared instance identity; the full `:microproject_ui:test` suite passed.

`PrintPreviewFrame` now binds `GraphPageable` directly in its existing type
guards for left/right preview toggles and button-state updates. `git blame`
traces all three guards to the OpenProj source, and the branch bodies and
rendering calls are unchanged. No dedicated preview-frame test exists; the
full UI module suite is the proportionate verification for this syntax-only
change. No physical GUI route or visual surface changed.

`AssignmentData.equals` now binds its OpenProj-derived `AssignmentData`
operand in the existing type guard. The `super.equals` check, equality fields,
and false result for null or unrelated types are unchanged. Extended
`DataObjectEqualsHashCodeTest` to retain explicit null/foreign-type coverage;
the focused exchange test passed.

`PageSizes` now resolves `Format` through one private pattern-binding helper,
used by dimension, custom-size, and single-page queries. `git blame` traces
the original guards to OpenProj. Null and foreign-object fallbacks and all
three query results remain covered by `PageSizesTest`; the focused UI test
passed. This consolidation removes three copies of the same type-resolution
rule.

`MenuManager` now binds `AbstractButton` and `JCommandToggleButton` values in
the existing enabled, visible, selected, and tooltip updates. `git blame`
confirms these guarded paths derive from OpenProj; the menu/ribbon behavior
and command routes are unchanged. Existing `RibbonAndToolbarButtonTest` covers
the active enabled/selected paths, and the full UI test suite is used for this
shared menu manager update.

`FieldDialog.setObject` now pattern-binds its previous and incoming
`BelongsToDocument` objects and their `Project` listeners. Listener detach,
assignment order, and listener attach remain unchanged; null still skips the
incoming-document branch through the pattern match. These guards trace to
OpenProj. No headless `FieldDialog` test fixture exists, so verification uses
the full UI suite; no visible dialog route or layout changed.

`ProjectInformationDialog.objectChanged` now binds task and resource event
payloads before checking project/resource-pool identity. Event filtering and
the conditional refresh are unchanged; `git blame` traces both branches to
OpenProj. No dedicated headless test exists for this dialog callback, so the
full UI suite is used. The user-visible dialog route and layout are unchanged.

`AbstractDialog.containedInDialog` now binds the initial `Component` and
ancestor `JDialog` values directly. Non-component input still returns null,
and parent traversal still returns the nearest containing dialog. `git blame`
traces both guards to OpenProj. One full UI test run had a failure in the
unrelated `ScaledScrollPaneTest.originChangeKeepsTheVisibleLeftDateAnchored`;
the focused test and the next full `:microproject_ui:test` run passed.

`FieldVerifier.valueHoldingComponent` now binds the LookupField, parent
Component, and resolved spinner/date value component. The parent traversal and
null fallback are unchanged; `git blame` traces this helper to OpenProj. Added
`FieldVerifierTest` for spinner-editor resolution and a parentless component.
The fork-added date parsing branch in `verify` remains untouched.

`AssignmentEntryPane` now binds `AssignmentDialog`, `ReplaceAssignmentDialog`,
and `HasAssignments` values in its existing double-click dispatch and node
transformer. The two dialog actions, visibility change, and assignment-entry
construction remain unchanged; `git blame` traces these hunks to OpenProj.
The full UI suite verifies the module; no command route or visible layout was
changed by this source-only modernization.

`ChartModel.computeTrace` now binds `HasAssignments` and `Assignment` values;
`computeOtherProjects` binds its assignment/resource inputs. The historical
`ResourceImpl` cast on `Assignment.getResource()` remains because that method
returns the `Resource` interface and the chart path requires the concrete
global-resource calculation API. `git blame` traces the changed guards to
OpenProj. The full UI suite verifies the chart module; aggregation formulas
and filtering remain unchanged.

## Local follow-up after the documented #632 checkpoint

`Field.getSummarizedValueForField` now caches its OpenProj-derived
`GroupNodeImpl` check and binds the `Double` result before display conversion.
The summary selection and option conversion still use the same values and
order. `git blame` traces these original guards to `openproj_core`; its active
OpenProj caller is the task/node summary read path. A separate
fork-specific group-duration caller is excluded above. The full
`:microproject_core:test --console=plain` suite passed. Commit: `47eb17560`.

`Field.fireEvent` now binds the OpenProj-derived `BelongsToDocument` guard,
while keeping the source-null and no-update checks intact. Its caller remains
`Field.setInternalValueAndUpdate`; the full core test suite passed. The
task-sheet-only `DataObject` dirty branch in the same file is fork-origin and
excluded above. Commit: `3c96088e2`.

`MutableNodeHierarchy.paste` now reads its `DataFactory` once and pattern-binds
the OpenProj-derived `Project` and `ResourcePool` branches. Paste routing and
the null fallback are unchanged; `DefaultNodeModel` remains the active caller.
The focused `DefaultNodeModelTest` passed. Commit: `8f8d87b40`.

`FieldComponentMap.setDataFactoryFromObject` now pattern-binds its
OpenProj-derived `BelongsToDocument` check. The cast of `getDocument()` remains
because the public return type is broader than `NodeModelDataFactory`; assignment
semantics are unchanged. Dialog callers were verified, focused dialog tests
passed, and the subsequent full UI test suite passed. Commit: `4bf559268`.

`TimeChartPanel` now pattern-binds the OpenProj-derived `Field` trace and
`NumberFormat` checks. The fork-added nonempty-trace guard, resource-graph
guard, money formatting, and fallback format remain unchanged. `ChartView` is
the active production caller; the full UI test suite passed. Commit:
`3c937b907`.

`Field.setText(ObjectRef, ...)`, `setValue(ObjectRef, ...)`, and
`isReadOnly(ObjectRef, ...)` now use enhanced-for traversal for their
OpenProj-derived `Collection<?>` branches. `git blame` traces the replaced
iterator loops to `openproj_core` (`d2fa3c20a`); UI callers include
`FieldVerifier`, `FieldChangeListener`, and `ComponentFactory`, while undo
replay calls `setValue` through `FieldEdit`. Collection encounter order,
element delegation, and the read-only early return are unchanged. Added
`FieldSetValueTest` coverage for collection-wide value updates and read-only
aggregation, including the empty collection case. Full
`:microproject_core:test --console=plain` passed. Commit: `f2dd7f178`.

`NodeModel.addImplCollection` is now typed as `Collection<?>` across the interface,
implementation, and `NodeModelFactory` callers; `DefaultNodeModel` traverses it
with enhanced-for while constructing and adding each implementation in encounter
order. `git blame` traces the implementation to OpenProj (`d2fa3c20a`), and
repository-wide caller search found only the two active factory paths. The input
collection is read-only in this method. Added a `DefaultNodeModelTest` asserting
both inserted implementation objects and their order. Verification passed:
focused `DefaultNodeModelTest`, full `:microproject_core:test`, and compilation of
application, exchange, UI, and reports callers. Commit: recorded in this changeset.

`DefaultNodeModel.testAncestorOrDescendant` and the `NodeModel.move` contract now
use `List<Node>` instead of raw lists and cast-after-iterator traversal. Project
reparenting and both XBS move callers now construct typed node lists. `git blame`
traces the ancestor check and move implementation to OpenProj (`d2fa3c20a`); the
caller search covered the interface, Project, and XBS interaction routes. Added
a regression for rejecting a parent/child pair while accepting siblings and an
empty selection. Verification passed: full `:microproject_core:test`,
`:microproject_ui:compileJava`, focused `DefaultNodeModelTest`, and
`git diff --check`. Commit: recorded in this changeset.

Typed the remaining `NodeModel` node-list contract across add/paste/remove/copy,
move/relocate, children, and implementation-replacement history. The
`NodeImplChangeAndValueSetEdit` snapshot lists and `DefaultNodeModel` traversal
now use `Node` and its `Position` type directly. `WalkersNodeModel.getChildren`
uses `List<?>` because its active `NodeCacheTransformer` implementation returns
virtual `GraphicNode` values for grouped views and real `Node` children for the
base view; the wildcard keeps that union explicit without unchecked client
casts. `git blame` identifies the core node-model methods as OpenProj-derived,
and all repository callers compiled. Full core and UI suites passed, as did
application/exchange/reports compilation. The generic signature changes retain
JVM erasure and do not change runtime behavior. Commit: recorded in this
changeset.

`AssociationList.testValid` now uses enhanced-for over its typed association list.
`git blame` confirms the traversal is OpenProj-derived (`d2fa3c20a`), and its
active caller is `AssociationList.setAssociations`. Encounter order, the
`allowDuplicate` value, and propagation of the first `InvalidAssociationException`
are unchanged. Added a regression that confirms ordered validation and that
later entries are not visited after a failure. Focused `AssociationListFormatTest`
and full `:microproject_core:test --console=plain` passed. Commit: recorded in
this changeset.

`NodeModelFactory.replicate` now traverses the `List<Node>` returned by
`NodeHierarchy.getChildren` directly, replacing its raw collection and iterator
casts with enhanced-for. `git blame` and the OpenProj source at `d2fa3c20a`
confirm the original traversal. Its active caller is `AssignmentEntryPane`'s
resource-outline replication path. Child order, recursive structure, and the
existing behavior of skipping a node and its descendants when the transformer
returns null are unchanged. Added `NodeModelFactoryTest` for order and skipped
transform results. Focused and full core tests passed. Commit: recorded in this
changeset.

`NodeModelFactory.createTaskModel` and `createResourceModel` now bind the
`AssignmentNodeModel` pattern and invoke `addAssignments` without a follow-up
cast. Both checks match OpenProj at `d2fa3c20a`; `Project.initializeOutlines`
is the active caller for both paths. Assignment population and the returned
model identity are unchanged. The existing `ProjectScheduleBehaviorTest` covers
outline initialization; focused and full core tests passed. Commit: recorded in
this changeset.

`DefaultNodeModel.copy` now types its copied parent list, assigned-node set,
implementation map, and list iterators. The related clone/dependency-rebuild
helpers accept `Map<Object, Object>`; `git blame` confirms the raw collection
responsibility is OpenProj-derived (`d2fa3c20a`). The active UI copy routes use
`NodeModel.copy`, and the existing `DefaultNodeModelTest` covers copied task
dependencies, assignment rows, and summary WBS children. Encounter order,
iterator-based replacement, and dependency-map lookups are unchanged. Focused
and full core tests passed. Commit: recorded in this changeset.

`AssignmentNodeModel.addAssignments` now uses a typed node iterator, a
`Map<Node, Node>` for pending assignment rows, typed map entries, and enhanced-
for traversal of each owner's `AssociationList`. The implementation matches
OpenProj at `d2fa3c20a`; active calls include `NodeModelFactory` outline setup
and `ProjectFactory` subproject loading. HashMap traversal, assignment
filtering, and the existing child-identity check are unchanged. Added
`ProjectScheduleBehaviorTest.assignmentOutlinePopulationIsIdempotent` to verify
the assignment row appears once after repeated population. Focused and full
`:microproject_core:test --console=plain` passed. Commit: recorded in this
changeset.

`AssignmentNodeModel.paste` now types the extracted roots and detached
assignment-node list and traverses both with enhanced-for; the leaf visitor is
a lambda. The underlying free-assignment scan matches OpenProj at `d2fa3c20a`.
The active caller is `ViewNodeModelCache.paste`, which dispatches to the
assignment-aware model. The filter, detach-before-connect order, and undo flag
are unchanged. Full core tests and the UI `NodeListTransferablePasteFailureTest`
passed, as did `git diff --check`. Commit: recorded in this changeset.

`DefaultNodeModel.cloneBranch` and `cleanBranch` now traverse the typed
`List<Node>` returned by the active hierarchy instead of iterating
`TreeNode` values and casting each child. Both traversals match OpenProj at
`d2fa3c20a`; active callers are `DefaultNodeModel.copy` and clone cleanup.
Child order and recursive behavior are unchanged, and null child lists retain
the former empty-iterator behavior. `DefaultNodeModelTest` and full
`:microproject_core:test --console=plain` passed, as did `git diff --check`.
Commit: recorded in this changeset.

`AssignmentNodeModel.objectChanged` now uses pattern matching for the assignment
event object and wildcard-typed `Enumeration` variables for child traversals.
`git blame` traces these event and child-scan paths to OpenProj (`d2fa3c20a`);
active construction is through `NodeModelFactory`. Child encounter order,
identity matching, insertion position, and event/undo behavior are unchanged.
Full `:microproject_core:test --console=plain` passed. Commit: recorded in this
changeset.

`DefaultNodeModel.collectRemovalRoots` now uses `List<Node>` and removes
assignment roots with `removeIf`, preserving the remaining roots' encounter
order. Its assignment-filtering behavior matches OpenProj at `d2fa3c20a`; the
active path is `DefaultNodeModel.remove(List<Node>, ...)`. Added a regression
that tries to remove an assignment outline node through the default filtering
route and confirms it remains under its task. Focused `DefaultNodeModelTest`
and `git diff --check` passed. Commit: recorded in this changeset.

`DataObjectImpl.equals` now binds its OpenProj-derived `instanceof` check with
pattern matching, removing the immediate cast. The enclosing equality logic
contains a later fork change for issue #177, which remains untouched. Equality
still compares `id` and `uniqueId`; `DataObjectEqualsHashCodeTest` passed after
the hunk-only change.

`FieldChangeListener.stateChanged` now binds the `LookupField` pattern and
uses that binding for the existing `getValue` call. The check/cast hunk matches
OpenProj at `d2fa3c20a`; `ComponentFactory` is the active producer. The later
fork logging behavior in the exception path is unchanged. Full
`:microproject_ui:test --console=plain` passed.

`FieldVerifier.verify` now binds the OpenProj-derived date-string pattern and
passes the binding directly to `DateFormat.parseObject`, removing the redundant
cast. `git blame` traces the check/cast block to OpenProj (`d2fa3c20a`);
`ComponentFactory` installs the verifier for date fields. Date parse fallback,
null-date handling, and subsequent edit/undo behavior are unchanged. Full
`:microproject_ui:test --console=plain` passed.

`HelpUtil` now binds the OpenProj-derived `JTableHeader` type check and uses the
binding for table-header coordinate lookup, removing the immediate cast.
`git blame` traces this branch to OpenProj (`d2fa3c20a`); `HelpUtil` is called
from active dialog, report, print-preview, and toolbar help routes. The help
target resolution is unchanged. Full `:microproject_ui:test --console=plain`
passed.

`FieldDialog.createFieldsPanel` now uses the collection's `isEmpty()` contract
instead of comparing `size()` to zero. `git blame` traces this guard to OpenProj
(`d2fa3c20a`); active subclasses/callers include project,
project-information, update-task, and delegate-task dialogs. Null handling and
whether the shared field panel is omitted are unchanged. Full
`:microproject_ui:test --console=plain` passed.

`Serializer.forAssignments` now iterates snapshot assignments directly instead
of guarding the loop with `size() > 0`; the empty collection naturally produces
no callbacks. `git blame` traces this method to OpenProj (`19d0b4e9e1`); the
active caller serializes baseline assignments into POD/XLSX document data. Null
behavior, callback order, and casts are unchanged. Full
`:microproject_exchange:test --console=plain` passed.

The POD/XLSX assignment serialization callback now uses a lambda for the
single-method `AssignmentClosure` instead of an anonymous class. `git blame`
traces the interface and callback scaffolding to OpenProj (`d2fa3c20a` /
`19d0b4e9e1`); `Serializer.taskLinker` is the active caller. The callback body,
including the fork's orphan-resource handling, was preserved. Full
`:microproject_exchange:test --console=plain` passed.

`AbstractDialog` now registers OK, Cancel, and Close actions with lambdas rather
than anonymous `ActionListener` classes. `git blame` traces the button callback
scaffolding to OpenProj (`d2fa3c20a`); all application dialogs inherit this
button pipeline. The same virtual `onOk`/`onCancel` methods are called by the
same buttons. Full `:microproject_ui:test --console=plain` passed.

`ErrorLogger.log(Exception)` and `ErrorLogger.log(String)` now pass their
asynchronous session writes to `Thread(Runnable)` instead of anonymous
`Thread` subclasses. `git blame` traces both methods to OpenProj
(`9b5c2f988e`); callers include project/task recovery and job error handling.
Each call still creates and starts one unnamed thread, checks the session for
null, and invokes the same logger method. Full
`:microproject_core:test --console=plain` passed.

ResourceView now passes its once-only warning action to SwingUtilities.invokeLater as a lambda. The queued Runnable body is OpenProj-derived (d2fa3c20a); DocumentFrame creates ResourceView in the active resource-view route. The later fork-specific condition, warning text, preference key, and EDT scheduling remain unchanged. Full :microproject_ui:test --console=plain passed.

ClassUtils now registers its built-in comparable types through one null-safe natural-order comparator instead of thirteen duplicated typed comparator bodies. `git blame` traces the registration table to OpenProj (`d2fa3c20a`); active consumers include `Field.getComparator()` and field filtering/sorting. The null-first order is unchanged, and Work still compares against Duration through its natural `compareTo` contract. Added assertions for that cross-type comparison and null ordering. Full `:microproject_core:test --console=plain` passed.

`Project.initialize` now supplies its earliest-start recalculation callback to `SwingUtilities.invokeLater` as a lambda. `git blame` traces the callback to OpenProj (`d2fa3c20a`); `Serializer` invokes the active initialization path. The zero-start condition, EDT scheduling, recalculation, and subsequent start assignment are unchanged. Full `:microproject_core:test --console=plain` passed.

The packaged `com.microproject.main.Main` entry point now schedules its desktop startup through a lambda. `git blame` traces the `invokeLater` callback to OpenProj (`9b5c2f988e`); the bootstrap launcher names this entry point and the callback dispatches to the existing Gantt application main. Argument parsing and EDT dispatch are unchanged. Full `:microproject_ui:test --console=plain` passed.

`Job.setProgress`, `Job.cancel`, and the Swing execution dispatcher now use lambdas for `ProgressMonitor` updates and `InternalRunnable` dispatch. `runThread` also passes its execution callback to `Thread(Runnable)` while preserving daemon configuration and start order. `git blame` traces these paths to OpenProj (`d2fa3c20a`); current import/export, save/load, print, and project lifecycle flows construct `Job` instances. Progress calculation, monitor guards, cancellation checks, exception handling, mutex release, and completion behavior are unchanged. The custom cancellation-monitor `Thread` subclass remains because its `isInterrupted()` check belongs to that monitor thread. Full `:microproject_core:test --console=plain` passed.

`Task.recalculateLater` now passes its queued task-recalculation callback to `SwingUtilities.invokeLater` as a lambda. `git blame` traces the callback to OpenProj (`d2fa3c20a`); active `Assignment` progress paths call it. The recalculation-needed marker is still set before the same EDT scheduling and recalculation call. Full `:microproject_core:test --console=plain` passed.

`LocalFileImporter` now uses lambdas for the XML-recovery open callback and import-error alerts. `git blame` traces the original EDT callbacks to OpenProj (`2ae61558c1` / `fea5de3f19`); the active importer registry routes project imports through this implementation. The two failure branches now select the existing message key and share one alert callback; error text, exception rethrow behavior, and EDT dispatch remain unchanged. Full `:microproject_core:test --console=plain` passed.

`Job`'s `warm`, `error`, `confirm`, `okCancel`, and `renameProject` routes now share one typed EDT dispatch/wait/result helper instead of five copies of the same mutex and scheduling protocol. `git blame` traces these routes to OpenProj (`d2fa3c20a`); project/import/export error handling uses them. The `wait=false` default return values and asynchronous dialog scheduling are preserved. Added tests for EDT execution, synchronous result delivery, and asynchronous default results. Full `:microproject_core:test --console=plain` passed.

`DependencyService.warnCircularCrossProjectLinkMessage` now passes the same deferred warning action to `SwingUtilities.invokeLater` as a lambda. `git blame` traces it to OpenProj (`d2fa3c20a`); `ExternalTaskManager` and POD serializer import paths are active callers. The popup guard, message formatting, and deferred EDT display are unchanged. Full core and exchange tests passed.

`FieldDialog` now queues its initial `updateAll` refresh through a method reference. `git blame` traces the callback to OpenProj (`d2fa3c20a`); active subclasses include project, update-task, and delegate-task dialogs. The callback remains queued with the same virtual dispatch after constructor initialization. Full `:microproject_ui:test --console=plain` passed.

`DocumentFrame` now schedules its initial view activation through a lambda. `git blame` traces the callback and activation body to OpenProj (`d2fa3c20a`); `GraphicManager` constructs `DocumentFrame` for opened projects. The activation condition, resource-vs-Gantt selection, disabled-button state, and deferred EDT timing are unchanged. Full `:microproject_ui:test --console=plain` passed.

`StartupFactory` now uses lambdas for existing-session view initialization and new-session startup/welcome dispatch. `git blame` traces both callback boundaries to OpenProj (`d2fa3c20a`); application startup invokes these session routes. The existing-session EDT timing is unchanged, and the latter callback retains its fork-added recovery check and ProjectLibre-specific condition verbatim. UI compilation and focused `StartupFactoryCommandStateTest` / `ApplicationStartupFactoryProjectIdTest` passed. Two full `:microproject_ui:test --console=plain` runs each reported one failure in the unrelated `ScaledScrollPaneTest.originChangeKeepsTheVisibleLeftDateAnchored()`; that test class passes alone.

`GraphicManager.addProjectFrame` now passes its post-registration filter reset to `SwingUtilities.invokeLater` as a lambda. `git blame` traces the callback to OpenProj (`fa200d2c9a`); `addProjectFrame` is the active document creation path. The plugin guard and deferred filter clear remain unchanged. `GraphicManagerLinkRouteTest`, `DefaultFrameManagerTest`, and `RibbonButtonBehaviorTest` passed.

`StartupFactory.instanceFromNewSession` now supplies the config-loading action to the named `Thread` constructor as a lambda. `git blame` traces the thread body to OpenProj (`d2fa3c20a`); `ApplicationStartupFactory` routes new desktop sessions through this method. The `loadConfig` thread name, non-daemon default, `start()` / `join()` ordering, and interrupted-join handling are unchanged. UI compilation and focused `StartupFactoryCommandStateTest` / `ApplicationStartupFactoryProjectIdTest` passed. The elapsed-time local only feeds a commented-out diagnostic and is retained with a descriptive name.

`GraphicManager.closeApplication` now supplies its asynchronous close-project scheduling action to `Thread` as a lambda. `git blame` traces the callback boundary to OpenProj (`fa200d2c9a`); both `MainFrame` and `MainRibbonFrame` invoke this active shutdown route. The default thread naming/daemon behavior, project removal job construction, and scheduling remain unchanged; the nested `JobRunnable` stays an anonymous implementation because it implements the domain callback contract. UI compilation and `GraphicManagerQuitLifecycleTest` passed.

`VersionUtils` now passes the legacy restart warning directly to `SwingUtilities.invokeLater` as a lambda. `git blame` traces this EDT action to OpenProj (`9b5c2f988e`); startup calls `VersionUtils.checkForUpdate` through the active initialization path. The applet/JRE condition, restart flag, localized message, and EDT dispatch are unchanged. Full `:microproject_core:test --console=plain` passed.

`UniqueIdPool.makeServerReservationAsync` now passes its ID-reservation work to `Thread` as a lambda. `git blame` traces the worker body to OpenProj (`9b5c2f988e`); `getId` invokes this path when the server interval falls below its minimum, while `UniqueIdPoolTest` verifies the admission guard's single-reservation contract. Exception logging, reservation-finalization behavior, thread defaults, and asynchronous start remain unchanged. Full `:microproject_core:test --console=plain` passed.

`LafManagerImpl` now keeps its dialog-button traversal and sort collection as `List<Component>`, uses enhanced-for traversal, and supplies a typed sort lambda. `git blame` traces the focus-axis and ordering logic to OpenProj (`d2fa3c20a`); installed arrow-key traversal uses this active path. Eligibility filtering, axis choice, tie ordering, boundary behavior, and focus request are unchanged. Full `:microproject_ui:test --console=plain` passed.

`UILister.main` now types Look-and-Feel defaults as `Set<Map.Entry<Object, Object>>`, uses a typed `TreeSet` comparator and enhanced-for traversal, and registers its Close action with a lambda. `git blame` traces the diagnostic entry point and listing logic to OpenProj (`9b5c2f988e`); it remains manually launchable as a UI-defaults inspection tool, with no product startup caller found. Key sorting, displayed values, and close behavior are unchanged. Full `:microproject_ui:test --console=plain` passed.

`Intervals` now specializes its `TreeSet` and comparator on `HasStartAndEnd`, types its `addAll` input, and removes iterator/result casts from interval traversal. `git blame` traces the merge and ordering behavior to OpenProj (`d2fa3c20a`); `CalendarView` and `ChangeWorkingTimeDialogBox` are active callers, while `CalendarService` consumes the values as `Set<?>`. Overlap merging, disjoint interval order, weekday removal, and existing erased `TreeSet`/collection method descriptors are preserved. `IntervalsTraversalTest` and full `:microproject_ui:test --console=plain` passed.
