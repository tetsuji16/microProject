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
| Grouping XML configuration | `NodeGrouper` | Typed the XML-populated group list and `addGroup`/getter API as `NodeGroup`; verified insertion order and transform relationship, then compiled the UI consumer. |
| Assignment composition filtering | `AssignmentCompositionFilter` | Applied pattern matching to the source-exact OpenProj Assignment branch; added delegation tests for both Assignment-to-Resource composition and unchanged non-Assignment nodes. The `Filter.WhoDoesWhatReport` XML configuration remains unchanged. |
| Assignment exclusion filtering | `NotAssignmentFilter` | Replaced racy mutable lazy singleton fields with immutable `static final` instances and made the mode flag final; tests verify stable, distinct standard/writable instances and task acceptance. |
| Resource team filtering | `ResourceInTeamFilter` | Applied pattern matching only to the two OpenProj-origin type checks; preserved the later `Consumer` callback fork delta and tested both resource paths plus change-only notification behavior. |
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
  passed after pattern-matching the resource/team filter branches.
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
- Application, exchange, UI, and reports compilation passed at an earlier
  calendar-clone integration checkpoint. No GUI route, layout, or Swing behavior
  was changed, so Robot/GUI tests were not repeated.
- Compilation/test success is evidence only for those exercised modules; it is
  not evidence that the inventory is exhausted or that every compatibility
  boundary has been audited.
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
