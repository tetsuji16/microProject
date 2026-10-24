# Project 2019 MPP14 compatibility fixtures

These eight files are the generated MPP and companion MSPDI XML from MPXJ's
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
checks the MPP fixtures and the assignment-progress literals in both paired
MSPDI import and MPO reload; `MpxExportTrackingTest.project2019TaskBaselineSlotsMatchIndependentManifestAndMpoReload`
checks baseline values from the paired MSPDI fixture, standard MSPDI output, and MPO save/reload:

- `task-links`: 16 named tasks at outline level 1 with eight-hour durations;
  eight fixed predecessor/successor pairs and relation types. Source lags are
  0, 1 day, 2 days, 1 week, 2 weeks, 2 days, 2 days, and 2 days. The project
  declares 480 minutes per workday and 2400 minutes per workweek, so imported
  work lags resolve to 0, 8, 16, 40, 80, 16, 16, and 16 scheduled hours. The
  regression also moves a predecessor, checks the calendar-resolved successor
  date, and verifies the native MPO MSPDI still emits the one-day lag as 480
  minutes (`LinkLag` 4800).
- `assignment-assignments`: three named tasks, each assigned to its matching
  named resource for 80 hours. Task progress is 0%, 25%, and 50%; actual work
  is 0, 20, and 40 hours, with remaining work and assignment progress matching
  the MSPDI companion.
- `calendar-calendars`: the two named resources retain their correspondingly
  named resource calendars. Their source MSPDI has Monday–Friday working
  periods 08:00–12:00 and 13:00–17:00, with Sunday and Saturday marked
  nonworking; the MPP-imported calendar is checked for eight work hours on
  Monday, a nonworking lunch interval, and zero Saturday work after MPO reload.
- `task-baselines`: baseline duration slots 0, 1, and 10 import as 11, 12, and
  21 eight-hour workdays from the paired MPP and the MSPDI `PT88H`, `PT96H`, and
  `PT168H`. MPXJ v11.5.4's upstream `TaskBaselinesTest` expects normalized MPP
  baseline costs 1, 2, and 11 for slots 0, 1, and 10. The companion MSPDI
  encodes those same normalized totals as literal `Baseline/Cost` values 100,
  200, and 1100; MPXJ's MSPDI currency parser divides these values by 100, and
  its currency writer applies the inverse scale. The direct-import test keeps
  these totals through standard MSPDI output and MPO reload despite zero
  baseline work and fixed cost. Explicit zero remains distinct from a missing
  baseline, and total cost stays separate from `Baseline/FixedCost`. Raw MSPDI
  task fixed cost, assignment baseline timephased cost, and resource cost per
  use use the same pinned MPXJ currency conversion. Output preserves fractional
  hundredths; `CurrencyDigits` affects display only and does not cap stored
  precision.

The paired MSPDI omits all `<Baseline>` records for `Baseline Fixed Cost -
Task` and its numbered siblings, while the MPP fixture carries task baseline
fixed costs 11, 12, and 21 at slots 0, 1, and 10. Pinned MPXJ v11.5.4
[`TaskBaselinesTest`](https://github.com/joniles/mpxj/blob/c75014d0a7e310315ce46c426dc3108dc6e73f09/src/test/java/net/sf/mpxj/junit/task/TaskBaselinesTest.java)
expects the MPP values; the MPP import contract checks them again after MPO
reload. The MSPDI absence assertion remains specific to the paired XML import
and is not used as the MPP oracle. This is a fixture-specific representation
difference, not a general MPP/MSPDI equivalence claim.

The calendar rule follows Microsoft's [DayType element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/daytype-element?view=project-client-2016),
[DayWorking element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/dayworking-element-calendar?view=project-client-2016),
and [WorkingTime element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/workingtime-element-calendar?view=project-client-2016).
These calendar expectations are document-derived from the companion MSPDI,
not observed in a Microsoft Project desktop run. The MPP-derived task
baseline duration/cost manifest is checked again after MPO save/reload; this
claim remains limited to slots 0, 1, and 10 in these fixtures.
This calendar fixture has no dated exceptions. An explicit `DayType=WORKING`
on a nonworking parent day with no effective MPXJ intervals is not represented
and remains unverified; the importer does not invent an eight-hour schedule.

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
| `task-baselines-project2019-mpp14.mpp` | `19A4A7A6D3FE510E9F2A71EE827E25D1C9E705C6E24B831F4943FD73FF80C9A7` |
| `task-baselines-project2019-mspdi.xml` | `9814F728E2D88C742CAC5917CD758E1DD094C827D9B2F92D5EADFA4B3C50880C` |

Microsoft format context: the [Project `FileFormatID` documentation](https://learn.microsoft.com/en-us/office/vba/api/project.application.fileformatid)
identifies `MSProject.MPP.14` as the standard Project MPP format. Microsoft's
[desktop-supported format list](https://support.microsoft.com/en-us/project/file-formats-supported-by-project-desktop)
identifies MPP as Project's standard project-file format and lists Project 2019,
2024, and Project Online Desktop as supported desktop products. Those sources
define the relevant file-format family, not a measured read of this particular
file in every listed release; this fixture's generation provenance remains
Project 2019 build `16.0.10730.20102`.

Microsoft defines task `Baseline/Cost` as the projected task cost when the
baseline was saved, and `Baseline/FixedCost` separately as the task's fixed cost
at that time. These tasks have no assignments and zero fixed cost, while their
baseline totals are nonzero, so the fixture directly demonstrates the distinct
historical total field rather than a derived assignment or fixed-cost rollup.
The contract tested here keeps the total scalar authoritative for
whole-task baseline-cost fields; without a timephased cost profile, it does not
invent partial-interval values from that scalar. See the Microsoft [Baseline
element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/baseline-element?view=project-client-2016)
and [Cost element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/cost-element?view=project-client-2016),
accessed 2026-10-15. The pinned MPXJ oracle is [`TaskBaselinesTest.java`
at v11.5.4](https://github.com/joniles/mpxj/blob/v11.5.4/src/test/java/net/sf/mpxj/junit/task/TaskBaselinesTest.java);
the corresponding XML currency scaling is implemented in MPXJ's pinned
[`DatatypeConverter.java`](https://github.com/joniles/mpxj/blob/v11.5.4/src/main/java/net/sf/mpxj/mspdi/DatatypeConverter.java).
Microsoft's [`CurrencyDigits` documentation](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/currencydigits-element?view=project-client-2016)
defines this property as the number of decimal places shown in the UI, not a
storage precision rule. Tests therefore preserve fractional hundredths and
finite negative costs, and reject non-finite baseline totals.
