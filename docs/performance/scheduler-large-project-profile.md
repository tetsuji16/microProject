# Core scheduler profile on large synthetic projects

Issue #1105 item: measure the core scheduler on a large project and identify bottlenecks.
The baseline sections record measurements and hypotheses; the later issue #1178 section tracks candidate work
and explicitly separates measured results from unresolved hypotheses.

## How to run

The harness is `modules/microproject_core/src/test/java/com/microproject/pm/scheduling/LargeProjectSchedulingProfile.java`.
It is skipped by the normal `test` task and runs only with `-Dmicroproject.profile=true`
(`microproject.profile*` system properties are forwarded to the test JVM by `modules/microproject_core/build.gradle.kts`).

```
.\gradlew.bat :microproject_core:test --rerun --tests "*LargeProjectSchedulingProfile*" -Dmicroproject.profile=true -i
```

Optional properties: `-Dmicroproject.profile.scales=1000,2000,5000` (task counts; this is the default) and
`-Dmicroproject.profile.phaseSize=100`. Results are printed as `PROFILE ...` lines in the test output (use `-i`).

The 5,000-task scale needs a deeper thread stack than the default (see hypothesis 3): run the test JVM with
`-Xss256m` (for example a temporary `jvmArgs("-Xss256m")` in the core `tasks.test` block). For a JFR recording add
`-XX:StartFlightRecording=filename=sched.jfr,settings=profile` to the same `jvmArgs`, then analyze with
`jfr print --events jdk.ExecutionSample --stack-depth 2000 sched.jfr`.

Project shape: N tasks in N/100 summary phases (50 phases x 100 tasks at 5k), durations 1-5 days, a single
finish-to-start chain through every task, an extra cross link `i-20 -> i` on every 7th task, and one assignment
per task spread over 50 resources. "Edit" is a +3 day change of the finish of task #5 through
`ScheduleService.setInterval` (undo-aware), followed by `undo`. A 300-task warm-up run precedes the measurements.

For issue #1178, the harness supports `-Dmicroproject.profile.mode=bulk|production-batch|sequential`. Bulk and
production-batch modes measure phase-sized `NodeModel.add(parent, children, NORMAL)` calls followed by one
scheduler initialization, `DependencyService.connect` for the sequential links, and grouped
`AssignmentService.newAssignments` by resource. `bulk` adds one explicit `Project.fireMultipleTransaction` around
the whole synthetic build; `production-batch` omits that unverified whole-build wrapper so each production API
controls its own transaction boundary. Sequential mode measures repeated `Project.createLocalTaskNode`,
`DependencyService.newDependency`, and `AssignmentService.newAssignment` calls. All modes use the simple sequential
chain (without the extra cross links) so the link-building paths can be compared directly.

Scope limitation: only `bulk` opens an outer `Project.fireMultipleTransaction` before creating any nodes and
closes it after links and assignments. Production call sites found during issue work use
`MultipleTransaction` at smaller boundaries: hierarchy mutations in `MutableNodeHierarchy`/`DefaultNodeModel`,
event publication in `Project.publishEventBatchNow`, and grouped resource assignment in
`AssignmentDialog` → `AssignmentService.newAssignments`. No ordinary user workflow was found that wraps an
entire project build in one outer transaction. Thus `bulk` timings below describe an unverified orchestration;
`production-batch` measures the APIs at their individual boundaries, and sequential mode is the repeated
single-item comparison. Do not use the `bulk` results to claim production build near-linearity.

## Environment

Windows 11, Intel Core Ultra X7 358H, 63 GB RAM, Temurin OpenJDK 25.0.4.1+1, Gradle 9.9.0, single JVM test worker.
The machine was shared with other builds, so absolute numbers are indicative.

## Results (milliseconds)

| Tasks | Create nodes | Add links | Add assignments | Build total | Full recalc | Edit (propagate) | Undo |
|------:|-------------:|----------:|----------------:|------------:|------------:|-----------------:|-----:|
| 1,000 | 1,365 | 10,148 | 12,138 | 23,653 | 9 | 9 | 8 |
| 2,000 | 4,580 | 62,170 | 54,432 | 121,183 | 20 | 19 | 18 |
| 5,000 | 37,383 | 859,124 | 357,467 | 1,253,975 | 52 | 53 | 45 |

All rows come from one run with JFR enabled (which adds some overhead). Edit and undo propagated to the end of
the chain in every run (last task finish moved, then restored).

### Issue #1178 candidate (2026-11-17)

Opt-in, no JFR, same harness and environment, separate runs for each mode. Both report successful last-task finish
movement after an edit and restoration after undo. The doubling ratios are far above 2x, so neither mode satisfies
issue #1178's near-linear acceptance criterion.

| Mode | Tasks | Create nodes | Add links | Add assignments | Build total | Full recalc | Edit | Undo |
|------|------:|-------------:|----------:|----------------:|------------:|------------:|-----:|-----:|
| bulk | 1,000 | 2,197 | 213 | 154 | 2,564 | 25 | 24 | 17 |
| bulk | 2,000 | 8,563 | 778 | 427 | 9,770 | 25 | 25 | 23 |
| production-batch | 1,000 | 793 | 59 | 578 | 1,431 | 8 | 9 | 8 |
| production-batch | 2,000 | 8,610 | 772 | 1,494 | 10,877 | 13 | 16 | 16 |
| sequential | 1,000 | 1,446 | 7,556 | 5,870 | 14,873 | 9 | 9 | 7 |
| sequential | 2,000 | 5,016 | 40,960 | 26,641 | 72,619 | 18 | 18 | 17 |

Commands (quote `-D` options in PowerShell so they are passed to Gradle as JVM system properties):

```
.\gradlew.bat "-Dmicroproject.profile=true" "-Dmicroproject.profile.mode=bulk" "-Dmicroproject.profile.scales=1000,2000" :microproject_core:test --rerun --tests "*LargeProjectSchedulingProfile*" -i
.\gradlew.bat "-Dmicroproject.profile=true" "-Dmicroproject.profile.mode=production-batch" "-Dmicroproject.profile.scales=1000,2000" :microproject_core:test --rerun --tests "*LargeProjectSchedulingProfile*" -i
.\gradlew.bat "-Dmicroproject.profile=true" "-Dmicroproject.profile.mode=sequential" "-Dmicroproject.profile.scales=1000,2000" :microproject_core:test --rerun --tests "*LargeProjectSchedulingProfile*" -i
```

A 4k bulk JFR exploratory run (with a temporary, uncommitted recording argument and a sentinel-refresh experiment
that was later removed because it did not improve measured link time) sampled `Task.markDependentTasks` at 19.44%,
`AbstractContour.clone` at 14.16%, hierarchy `visitAll` at 8.16%, and `AssociationList.findAssociation` at 3.52%.
These samples are pointers for further investigation, not proof of the exact nonlinear cause or of a successful
optimization. The latest `production-batch` run took 1.431s / 10.877s for 1k / 2k tasks (7.60x total growth);
its node setup alone was 0.793s / 8.610s (10.86x). Sequential build took 14.873s / 72.619s (4.88x). Both remain
strongly nonlinear; these results do not establish A3.

### Duration setup and snapshot ownership

Every synthetic task is assigned a varying 1–5 day duration after it is connected to the project. Each
`NormalTask.setDuration` call captures a `ProjectTaskSnapshot` over the entire current task list and a calculation
state map before applying the change. In this method, the snapshot is command-local failure compensation: it is
restored if `applyDuration` throws, and is otherwise discarded. It is not the user's undo record. The task-sheet
workflow separately captures the before state and `ScheduleEdit.postProjectSnapshotEdit` creates/owns the after
state for undo/redo. Thus profile node setup includes an O(N) rollback snapshot on each of N duration updates,
which can create O(N²) work independently of the critical-path pass.

An outer scheduling `MultipleTransaction` only suspends critical-path updates; it does not make independent
`setDuration` commands share an atomic snapshot. The existing linked-project refresh path is now connected to
`TaskDurationBatchService`: `ProjectMergeService` owns a `ProjectTaskSnapshot` and calculation-state map for the
entire atomic refresh, the batch service verifies that the project mutation scope and snapshot own each task,
applies durations without per-task rollback snapshots or repeated dependent invalidation, then marks all tasks
dirty and recalculates at the batch boundary. On failure, `ProjectMergeService` restores the snapshot, calculation
states, task metadata, and the pending event batch. This background refresh does not create a user Undo edit, as
confirmed by the integration test. Other duration-edit paths still use their own task-level rollback and any
applicable before/after Undo ownership. Keep the varying-duration profile's direct `setDuration` setup as the
original acceptance baseline; the profile has not been switched to this refresh-specific API.

### Growth order

* Full recalculation, incremental edit and undo are roughly linear (about 10 ms per 1,000 tasks) and are not a bottleneck.
* Building the project is the problem. Fitting 1k to 5k: node creation about n^1.9, link creation about n^2.7
  (2k to 5k: 13.8x time for 2.5x tasks, about n^2.9), assignment creation about n^2.2, total about n^2.4.
  Building 5,000 tasks takes about 21 minutes.

## Hotspots (JFR, 5k run, 26,172 samples on the test thread)

Samples are dominated by the build phase. Inclusive shares count a sample once per method.

Top self (leaf) frames:

| Share | Frame |
|------:|-------|
| 9.8% | `sun.util.calendar.BaseCalendar.getCalendarDateFromFixedDate` (via `GregorianCalendar`; about 15.6% with `computeFields`/`computeTime`) |
| 8.5% | `java.util.LinkedList.node`, reached from `association/AssociationList.java:248` (`list.iterator()`) |
| 4.7% | `document/ObjectEvent.java:145` `ObjectEventFactory.activateObject` |
| 4.5% | `pm/task/Task.java:669` `Task.dependsOn` |
| 4.3% | `ConcurrentHashMap.transfer`, from `pm/calendar/CalendarDefinition.java:333` (`AddCacheScope.add`, cache `put`) |

Top inclusive frames in `com.microproject.*`:

| Share | Method |
|------:|--------|
| 92.2% | `pm/criticalpath/CriticalPath.objectChanged` (line 395: `calculate(true, null)`) |
| 84.3% | `pm/criticalpath/TaskSchedule.calcDates` (called from `CriticalPath.doPass`, line 262) |
| 79.9% | `pm/assignment/AssignmentService.newAssignment` (via `DefaultNodeModel.add` / `Project.nodesInserted`) |
| 52.4% | `pm/calendar/CalendarDefinition.add` (date arithmetic) |
| 13.4% | `grouping/core/summaries/DeepChildWalker` (11.7% `DivisionSummaryVisitor.accept`) |
| 13.2% | link creation (`DependencyService.newDependency`) |
| 6.7% | `pm/task/Task.dependsOn` (circular check) |

## Bottleneck hypotheses

1. Every object event triggers a scheduling pass. `CriticalPath.objectChanged` (`CriticalPath.java:395`) calls
   `calculate(true, null)` for each link or assignment change, and `doPass` (`CriticalPath.java:262`) then recalculates
   every task whose calculation state is dirty. After a link, `markAllDependentTasksAsNeedingRecalculation` dirties the
   downstream chain, so adding n chained links costs O(n^2) task calculations. This is consistent with 92%
   inclusive time in `objectChanged` and the n^2.7 link growth. Bulk operations (import, paste, build) would
   benefit from batching events and running one pass at the end.
2. Calendar arithmetic is the inner-loop cost of each task calculation: 52% inclusive in `CalendarDefinition.add`,
   with `CalendarIterator.nextDay` (`CalendarDefinition.java:781`), `dayOf` (656) and `initialize` (672) building
   `GregorianCalendar` fields per call, and `AddCacheScope.add` (`CalendarDefinition.java:330-334`) growing a
   `ConcurrentHashMap` per scope (4.3% in `transfer`). The iterator pool (`CalendarIteratorFactory.getInstance`/`recycle`,
   lines 595/604; about 7% together via commons-pool allocate/addObjectToPool) is a further avoidable cost. Caching per-day
   working-time lookups and avoiding `Calendar.setTimeInMillis` per day would reduce this.
3. The circular-link check is recursive and builds strings. `Task.dependsOn(HasDependencies)` (`Task.java:742-749`)
   walks the whole predecessor chain with a fresh `HashSet` for each new link, and passes `""` as `taskNames`
   (non-null), so every recursion level concatenates `taskNames + ...` (`Task.java:684, 697, 706`), costing O(depth^2)
   characters per check. The recursion depth equals the chain length: with the default stack the 5k run failed with
   `StackOverflowError` in `Task.dependsOn` (`Task.java:669`/`706`/`697`). An iterative search with no name building
   (names only on the warning path) would remove the overflow and much of this cost.
4. Summary and hierarchy updates run on each assignment. `AssignmentNodeModel.objectChanged` to `DefaultNodeModel.add`
   to `Project.nodesInserted` / `updateScheduling` is 78% inclusive, with `DeepChildWalker` and `DivisionSummaryVisitor.accept`
   (`DivisionSummaryVisitor.java:39`) re-walking subtrees (13%). `AssociationList.iterator` (`AssociationList.java:248`) over
   a `LinkedList` accounts for 8.5% of leaf samples; JFR attributes that to `LinkedList.node`, so list traversal in
   predecessor/successor lists is worth checking (may include inlining skew). `ObjectEventFactory.activateObject`
   (4.7%) shows event churn from resetting a pooled `ObjectEvent` per publish.

## Caveats

* JFR samples are safepoint-biased for inlined code; use line numbers as pointers, not exact costs.
* Single run per scale, on a shared machine.
* The harness measures a build-then-edit workflow; interactive edit and undo latency (about 50 ms at 5k) looks acceptable.
