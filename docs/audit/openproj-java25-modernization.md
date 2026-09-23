# OpenProj Java 25 modernization progress

Status: in progress; this document does not mark any issue phase complete.

Baseline checked: `origin/master` at `59eb4e0dc1157b382d754deb0acc79fd3384ac5b`.
The modernization branch is based on that commit. Progress commits are listed in
Git and summarized in [issue #595](https://github.com/tetsuji16/ProjectLibre/issues/595).

## Inventory caveat

`docs/legal/license-provenance.csv` uses the former `projectlibre_*` module and
`com.projectlibre1` package paths. Run
`python scripts/audit/openproj_java25_inventory.py --ref origin/master` to
reconcile its `projectlibre_core` production-Java rows marked
`normalized_openproj_match=true` against the current source tree and OpenProj
baseline. On the pinned base (`origin/master` =
`59eb4e0dc1157b382d754deb0acc79fd3384ac5b`), the script reports 280 ledger
rows: 212 mapped files have normalized content matching the OpenProj source,
55 mapped files differ, and 13 mapped paths are absent. Of those 13, two source
files have same-module relocations (`IntervalConsumer` and
`ScheduleIntervalGenerator`); the other 11 do not resolve by basename in the
current core production Java tree. None of the absent paths are presumed dead.

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
| Collaboration link identity | `LinkData.equals` | Replaced the cast-after-`instanceof` with a pattern binding and expanded the existing equality contract test for null, unrelated values, and a different serialized-data subtype. |
| Filtered graphic-node iteration | `GeneralFilteredIterator` | Typed the iterator input as `Iterator<?>` and replaced the GraphicNode check/cast with a pattern binding. A headless focused test confirms node-based iteration still returns the underlying Node. The Apache Commons `FilterIterator` base remains raw because it is an external non-generic type. |
| Shared node-filter collection | `NodeFilter.filterList`, `filterArray` | Confirmed these methods match the OpenProj baseline at `d2fa3c20`; made `filterList` preserve its input element type with `<T> List<T>` and a typed iterator, and used a diamond-inferred `ArrayList<Object>` for array filtering. Preserved in-place removal, list identity, order, null behavior, and erased signatures. Regression coverage verifies typed assignment, retained order, and in-place mutation; UI caller compilation passed. |

Separate work in `com.microproject.core.time` is bridge/fork code, not counted as
an OpenProj-origin modernization result unless hunk provenance is established.

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

- Review all 55 content-different candidates and the 11 absent class paths;
  determine whether they are relocations, removed sources, or stale ledger rows.
- Search the 212 matching files and the OpenProj-origin hunks within the 55
  differing files for production callers and compatibility boundaries; do not
  treat file-level equality as proof of an active eligible hunk.
- Audit remaining eligible Java in exchange, UI, reports, and other core
  responsibilities; do not add GUI tests unless a physical GUI contract or
  visual surface is changed.
- Record cleanup/deletion candidates only after checking reflection, resource
  configuration, serialization, ServiceLoader, action IDs, and format readers.
- Update this inventory and issue #595 after each meaningful audited batch; do
  not close the issue while any required phase or unresolved in-scope work
  remains.
