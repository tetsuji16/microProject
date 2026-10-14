# Project 2019 MPP14 compatibility fixtures

These six files are the generated MPP and companion MSPDI XML from MPXJ's
pinned public test-data corpus. They are included under the upstream MPXJ
repository's LGPL-2.1 license; the applicable license text is
[`../LICENSE-LGPL-2.1.txt`](../LICENSE-LGPL-2.1.txt). The fixture files are not
relicensed under this project's MIT license.

Source revision: [`a46383130536687867fc97f37c9d562a4d277445`](https://github.com/joniles/mpxj/commit/a46383130536687867fc97f37c9d562a4d277445), “Project 2019 tests”. The
source files are stored under
[`junit/data/generated`](https://github.com/joniles/mpxj/tree/a46383130536687867fc97f37c9d562a4d277445/junit/data/generated).
MPXJ's `junit/GenerateTestData.bas` at that revision selects Project 2019 when
`Application.Version` is `16.0` and the build is at least `10730`; it saves an
MPP14 project and an MSPDI companion. The companion files report exact build
`16.0.10730.20102`. This identifies the corpus as Project 2019-generated data;
it is not a claim that Project 365 or Project 2024 generated these files.

The companion MSPDI files and upstream MPXJ tests provide an oracle independent
of microProject's MPP reader. `MpxExportTrackingTest.project2019Mpp14FixturesMatchIndependentManifestAndMpoReload`
freezes these contract rows and checks them after MPP import and MPO save/reload:

- `task-links`: 16 named tasks at outline level 1 with eight-hour durations;
  eight fixed predecessor/successor pairs and relation types. Source lags are
  0, 1 day, 2 days, 1 week, 2 weeks, 2 days, 2 days, and 2 days. The project
  declares 480 minutes per workday and 2400 minutes per workweek, so imported
  work lags resolve to 0, 8, 16, 40, 80, 16, 16, and 16 scheduled hours. The
  regression also moves a predecessor, checks the calendar-resolved successor
  date, and verifies the native MPO MSPDI still emits the one-day lag as 480
  minutes (`LinkLag` 4800).
- `assignment-assignments`: three named tasks, each assigned to its matching
  named resource for 80 hours.
- `calendar-calendars`: the two named resources retain their correspondingly
  named resource calendars.

These tests cover only the listed fields and this Project 2019 MPP14 fixture
set. They do not establish general MPP parity or a measured Project 365/2024
build result.

| File | SHA-256 |
| --- | --- |
| `task-links-project2019-mpp14.mpp` | `B2592BE8D205ACAF3D02C240C8A68D33DF00C62BAD27477B6F93A3C950D0DB5A` |
| `task-links-project2019-mspdi.xml` | `0F62AEE93BF52DF56B0726151B7E9CFA973FE9F2FCF77B89F52E6DBE81BC7C2D` |
| `assignment-assignments-project2019-mpp14.mpp` | `D5BBDA5704838AFCC171493425197167671EE57D3BB051CDFDF55613D240C953` |
| `assignment-assignments-project2019-mspdi.xml` | `7DD6168F9207C2FB208F67D558C28E781BCE67C829035F58DC3B189F2B8FFA3F` |
| `calendar-calendars-project2019-mpp14.mpp` | `DBE235069C9FBC91DEB286AD6F5ABA6B2D68120D6B0A2FCF62BE4B588BA227C6` |
| `calendar-calendars-project2019-mspdi.xml` | `EACFA299910F9E7724A375504A0044B2DB67C86D829EFEE018CE9F3F26569B73` |

Microsoft format context: the [Project `FileFormatID` documentation](https://learn.microsoft.com/en-us/office/vba/api/project.application.fileformatid)
identifies `MSProject.MPP.14` as the standard Project MPP format. Microsoft's
[desktop-supported format list](https://support.microsoft.com/en-us/project/file-formats-supported-by-project-desktop)
identifies MPP as Project's standard project-file format and lists Project 2019,
2024, and Project Online Desktop as supported desktop products. Those sources
define the relevant file-format family, not a measured read of this particular
file in every listed release; this fixture's generation provenance remains
Project 2019 build `16.0.10730.20102`.
