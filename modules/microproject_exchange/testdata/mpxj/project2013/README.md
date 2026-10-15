# Project 2013 MPP14 task constraint fixture

This MPP is copied unmodified from the MPXJ pinned test-data corpus under the
upstream LGPL-2.1 license; see [`../LICENSE-LGPL-2.1.txt`](../LICENSE-LGPL-2.1.txt).
The source revision is [`a46383130536687867fc97f37c9d562a4d277445`](https://github.com/joniles/mpxj/commit/a46383130536687867fc97f37c9d562a4d277445),
at `junit/data/mpp14task-from2013.mpp`. MPXJ's
`src.test/net/sf/mpxj/junit/MppTaskTest.java` method
`testMpp14TaskFromProject2013` calls `testBasicTask`, which asserts the source
task's MUST_START_ON constraint and 23-Aug-2006 constraint date. These pinned
upstream expectations are the literal independent oracle; this fixture does
not establish general MPP parity or a measured result against an installed
Project client.

`MpxExportTrackingTest.project2013Mpp14TaskConstraintMatchesUpstreamManifestAndMpoReload`
checks the task's MUST_START_ON type and constraint date after microProject MPP
import and native MPO save/reload against the pinned MPXJ test's literal
expectations.

| File | SHA-256 |
| --- | --- |
| `mpp14task-from2013.mpp` | `276F26A3E8FBFC54FBF8D8DF9CFC5AB6FF2AC3D86B47B37878202C5F82625314` |
