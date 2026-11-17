# Core scheduler profile on large synthetic projects

Issue #1105 item: measure the core scheduler on a large project and identify bottlenecks.
This document records measurements and hypotheses only; no optimization was implemented.

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

For issue #1178, the candidate harness measures the production bulk paths: task nodes are added in phase-sized
`NodeModel.add(parent, children, NORMAL)` calls and the scheduler is initialized once; sequential links use
`DependencyService.connect`; assignments are added under one `MultipleTransaction`. This differs from the
original per-link/per-assignment baseline below and avoids measuring a synthetic sequence of independent UI
commands as though it were one bulk build. The candidate build uses the simple sequential chain (without the
extra cross links) so the bulk sequential-link workflow is measured directly.

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

The opt-in profile was rerun without JFR with `-Dmicroproject.profile.scales=1000,2000` after batching the
critical-path updates. The profile passed the last-finish movement and undo-restoration checks, and explicit
full recalculation remained 14 ms / 23 ms. Build time is still superlinear, so this evidence does not satisfy
issue #1178's near-linear acceptance criterion.

| Tasks | Create nodes | Add links | Add assignments | Build total | Full recalc | Edit | Undo |
|------:|-------------:|----------:|----------------:|------------:|------------:|-----:|-----:|
| 1,000 | 1,549 | 164 | 149 | 1,863 | 18 | 12 | 10 |
| 2,000 | 8,428 | 617 | 298 | 9,344 | 21 | 21 | 21 |

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
