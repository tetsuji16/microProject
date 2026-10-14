# MPXJ MPP9 contour fixture

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
