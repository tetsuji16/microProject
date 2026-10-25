# Project 2007 MPP12 task-link fixture

The MPP file and its companion MSPDI XML are copied from MPXJ v11.5.4 commit
[`c75014d0a7e310315ce46c426dc3108dc6e73f09`](https://github.com/joniles/mpxj/commit/c75014d0a7e310315ce46c426dc3108dc6e73f09),
under `junit/data/generated/task-links`. The MPP family token is MPP12. The
upstream [`TaskLinksTest`](https://github.com/joniles/mpxj/blob/c75014d0a7e310315ce46c426dc3108dc6e73f09/src/test/java/net/sf/mpxj/junit/task/TaskLinksTest.java)
fixes eight source relationships: UID pairs (1,2), (3,4), (5,6), (7,8),
(9,10), (11,12), (13,14), and (15,16), with FS/FS/FS/FS/FS/SF/SS/FF types
and lags 0 days, 1 day, 2 days, 1 week, 2 weeks, 2 days, 2 days, and 2 days.
The companion declares 480 minutes/day and 2400 minutes/week; the contract
test records converted scheduled lags as 0, 8, 16, 40, 80, 16, 16, and 16
hours. This source-derived interpretation is not a physical MSP measurement.

The fixture is test-only and distributed under the upstream MPXJ LGPL-2.1
license in [`../LICENSE-LGPL-2.1.txt`](../LICENSE-LGPL-2.1.txt).

| File | SHA-256 |
| --- | --- |
| `task-links-project2007-mpp12.mpp` | `D3DA69BE55A329A19AF2440DEE8FD99DAA025150D6E8E97751703654891ED181` |
| `task-links-project2007-mspdi.xml` | `D377A7783AA59882BDA649163E9DD196A0BAD1E88A5507C7DBA4ED19BDA74158` |
