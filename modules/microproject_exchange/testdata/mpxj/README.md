# MPXJ MPP test fixtures

## MPP9 contour fixture

`mpp9timephasedsegments.mpp` is copied from MPXJ commit
[`9b86a4670ae324e67572acec399729ebe1ef3191`](https://github.com/joniles/mpxj/tree/9b86a4670ae324e67572acec399729ebe1ef3191),
file [`junit/data/mpp9timephasedsegments.mpp`](https://github.com/joniles/mpxj/blob/9b86a4670ae324e67572acec399729ebe1ef3191/junit/data/mpp9timephasedsegments.mpp).
MPXJ labels this fixture MPP9 and tests it in
[`TimephasedWorkSegmentTest.testMpp9`](https://github.com/joniles/mpxj/blob/9b86a4670ae324e67572acec399729ebe1ef3191/src/test/java/org/mpxj/junit/TimephasedWorkSegmentTest.java).
The fixture is test data only; its MPP9 format does not establish coverage for
Project 365 or Project 2024.

The independent expected values for Task Sixteen are recorded in that upstream
test: its assignment is hand edited and has 1, 2, 3, 4, 5, 6, 7, 8, 9, and 10
hours on ten successive workdays, totalling 55 hours. The upstream test source
and a read of the fixed MPP identify the assignment as `CONTOURED`.

SHA-256: `B7064B105EA7ECEA1D72A0FE51766EE3A34A663952F9EE105C2A83EF7CA471E5`.

The MPXJ repository publishes this material under LGPL-2.1; the applicable
license text is included as [`LICENSE-LGPL-2.1.txt`](LICENSE-LGPL-2.1.txt).

## Project 2003 MPP8 and Project 2007 MPP12 task-link fixtures

The fixed MPP/MSPDI pairs in [`project2003`](project2003/README.md) and
[`project2007`](project2007/README.md) are from MPXJ tag `v11.5.4`, resolved
to commit [`c75014d0a7e310315ce46c426dc3108dc6e73f09`](https://github.com/joniles/mpxj/commit/c75014d0a7e310315ce46c426dc3108dc6e73f09),
under `junit/data/generated/task-links`. MPXJ's pinned
[`TaskLinksTest`](https://github.com/joniles/mpxj/blob/c75014d0a7e310315ce46c426dc3108dc6e73f09/src/test/java/net/sf/mpxj/junit/task/TaskLinksTest.java)
defines the eight UID pairs, names, relation types, and source lag amounts;
each checked-in MSPDI companion independently records the source workday and
workweek units. The contract test compares those literal expectations after
MPP import and MPO reload. These generated Project 2003/2007 fixtures were
obtained from MPXJ's published corpus; no macro or Project automation was run
to create them here.

Microsoft's [DayType element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/daytype-element?view=project-client-2016),
[DayWorking element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/dayworking-element-calendar?view=project-client-2016),
and [WorkingTime element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/workingtime-element-calendar?view=project-client-2016)
specify the weekday, working-day flag, and time intervals used by the calendar
fixture assertions. These are document-derived format interpretations, not
measurements from a Microsoft Project desktop run.
