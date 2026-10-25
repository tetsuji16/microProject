# Issue #918 — XML/MSPDI semantic contracts

State: CLOSED
Issue: https://github.com/tetsuji16/microProject/issues/918
Contract frozen: 2026-10-25, issue body plus finite intake matrix
Baseline HEAD: `565998a34ad2e04057d35b30b95616e4503b695c`
Baseline tree: preserve unrelated untracked attachment directory and sample sync JSON/locks.

## Goal and scope

Verify bounded MSPDI XML read/write contracts against independent literal
expectations, including MPO persistence. Do not imply general MSP parity.
MPP writing, POD wire changes, dependencies, and new UI routes are excluded.

## Acceptance

| ID | Required result/evidence | State | Evidence |
|---|---|---|---|
| A1 | Microsoft XML primary sources and independent literals define expectations | pass | Duration format table covers all ten working/elapsed codes with fixed ISO clock quantities, 480/2400/20 calendar and coherent dates; baseline/timephased expectations use fixed slot/value/type tables |
| A2 | Read/write and MPO preserve the fixed fields, units, zero/omitted and availability rules | pass | Domain checks cover initial import, MPO reload and XML export/reimport; baseline slots 0–10 and assignment type IDs; availability boundaries/gaps/unbounded ends/scalar fallback |
| A3 | Malformed/missing cases fail clearly without publishing partial success or mutating the source | pass | Malformed baseline duration, missing AvailableUnits, and overlapping periods reject with IllegalArgumentException, unchanged bytes, and no Completed progress |
| A4 | Reuse tests, record tested SHA and exact verification | pass | Core and exchange full suites passed; JUnit XML counts and exact commands below |

## Current slice

**Current slice complete:** F1 duration formats, F2 baseline slots and
future/historical offset isolation, and F3 availability lifecycle/rejections
are covered. Sol reviewed and merged PR #926.

## Implemented evidence

- `MpxResourceConverterTest.literalMspdiAvailabilityPeriodsRoundTripThroughModelMpoAndMspdi`
  uses hand-authored MSPDI Resource AvailabilityPeriod elements and checks
  bounded/unbounded periods at 0%, 25%, 50%, and 150%, a gap, scalar MaxUnits
  coexistence, MPO reload, MSPDI export and import, missing AvailableUnits,
  and overlap rejection.
- Contract is document-derived from Microsoft's [AvailabilityPeriod](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/availabilityperiod-element?view=project-client-2016)
  and [AvailableUnits](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/availableunits-element?view=project-client-2016)
  references. Period rows are authoritative when present; uncovered ranges map
  to zero availability. Missing AvailableUnits is rejected because the schema
  gives no default.
- Core production change: baseline work contours keep the existing shared
  contour pipeline, while its three writes to the current Task (end,
  constraint, cached duration) are skipped for baseline assignments. This
  prevents timephased baseline data from changing current schedule state.
- Root-cause diagnostic for slot 9: zero work contour was followed by
  `importBaselineValues` rebinding to its task snapshot schedule; experimental
  unconditional detached schedule preparation made that zero-work slot's
  schedule duration zero and its otherwise preserved cost contour queried as
  zero. That experiment was reverted. The original work/cost preparation and
  snapshot binding are retained; only Task side-effect guards remain.
- Future and historical baseline matrices assert current task/assignment
  start/finish, current assignment work, and current task constraint across
  importer stages. This is document-derived from Microsoft's [Baseline
  element](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/baseline-element?view=project-client-2016)
  and [baseline guidance](https://support.microsoft.com/en-us/project/create-or-update-a-baseline-or-an-interim-plan-in-project-desktop),
  not an empirical MSP run.
- No physical GUI/MSP run; none is needed for these file contracts.
- `MspdiDurationFormatContractTest` covers task and baseline codes 3/5/7/9/11
  and 4/6/8/10/12 using independent ISO-clock quantities and the explicit
  08:00–12:00/13:00–17:00 480/2400/20 calendar. Domain task and baseline
  quantities are checked after initial import, MPO reload, and XML reimport;
  elapsed identity is asserted. Writer presentation normalization is allowed.
  Microsoft reference: [DurationFormat](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/durationformat-element?view=project-client-2016).
  For code 12 the supplied PT1440H clock quantity is preserved with elapsed
  identity; 30-day elapsed-month internal conversion is the pinned MPXJ basis,
  not a universal Microsoft month definition.
- `MspdiBaselineMatrixContractTest` checks task baseline start/finish/duration/
  cost and assignment baseline start/finish/work/cost for slots 0–10 with unique
  fixed values, explicit zero slot 9, existing missing-baseline controls, and
  exported TimephasedData type IDs from an independent hardcoded table.
  Microsoft references: [Baseline](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/baseline-element?view=project-client-2016),
  [Type](https://learn.microsoft.com/en-us/office-project/xml-data-interchange/type-element-multiple-parents?view=project-client-2016).

## Latest verification

- Tested source base SHA: `565998a34ad2e04057d35b30b95616e4503b695c` plus scoped changes.
- PASS: `.\gradlew.bat :microproject_core:test --console=plain` (`BUILD SUCCESSFUL`; JUnit XML: 1,182 tests, 0 failures, 0 errors, 0 skipped).
- PASS: `.\gradlew.bat :microproject_exchange:test --console=plain` (`BUILD SUCCESSFUL`; JUnit XML: 344 tests, 0 failures, 0 errors, 0 skipped).
- PASS: `.\gradlew.bat :microproject_exchange:test --tests "com.microproject.core.pm.exchange.converters.mpx.MspdiBaselineMatrixContractTest" --console=plain` (JUnit XML: 2 tests, 0 failures/errors/skips; includes no-baseline control and future/historical invariants).
- One exchange attempt was accidentally run concurrently and failed with a missing Gradle in-progress result file. It is not counted as a test result; the subsequent serial run above passed.
- Initial PR head: `4ec224b334bff7cd98f7db45fae7ace10899f547`; test-only timezone follow-up awaits required CI.
- PR 926 CI run [40123626606](https://github.com/tetsuji16/microProject/actions/runs/40123626606), job `120960288902`, failed on Linux/JDK 25 in `MpxResourceConverterTest.literalMspdiAvailabilityPeriodsRoundTripThroughModelMpoAndMspdi`, initial literal import at line 65: 2030-06-03 09:00Z expected 0.25, actual 1.0. The test changed the JVM default timezone to GMT+09 after the test worker could have cached an MPXJ `DateHelper` calendar under UTC. Pinned MPXJ 11.5.4 `DateHelper.popCalendar()` reuses `Calendar` instances from its per-thread deque (`pushCalendar()` returns them); changing the default timezone cannot update an already cached calendar. The test now leaves process timezone unchanged so MPXJ parsing and `DateUTCConverter.toModelTime` use one consistent zone for floating MSPDI walltimes. No production change was made for this CI failure.
- PASS: `.\gradlew.bat "-Duser.timezone=UTC" :microproject_exchange:test --tests "com.microproject.core.pm.exchange.converters.mpx.MpxResourceConverterTest.literalMspdiAvailabilityPeriodsRoundTripThroughModelMpoAndMspdi" --no-daemon --console=plain` (1 test, 0 failures/errors/skips).
- PASS: `.\gradlew.bat "-Duser.timezone=Asia/Tokyo" :microproject_exchange:test --tests "com.microproject.core.pm.exchange.converters.mpx.MpxResourceConverterTest.literalMspdiAvailabilityPeriodsRoundTripThroughModelMpoAndMspdi" --rerun-tasks --no-daemon --console=plain` (1 test, 0 failures/errors/skips).
- PASS: `.\gradlew.bat "-Duser.timezone=UTC" :microproject_exchange:test --no-daemon --console=plain` (344 tests, 0 failures/errors/skips). Core production source did not change; the earlier core 1,182-test result remains applicable.

- Integrated via PR #926 / b0bf097825c4f59c101805fcb2b99991ca30eccd, tree identical to tested dd49d8b52a0c07ce4c7668087e98e88ba15c156b. Required Build 40124226992 and Windows GUI 40124227051 passed; GUI smoke reported 53 tests, 0 failures/errors, 1 known HiDPI-only skip (52 passes). Initial failed Build remains recorded above.
