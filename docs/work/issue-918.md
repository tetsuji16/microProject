# Issue #918 — XML/MSPDI semantic contracts

State: IMPLEMENTED; Sol review pending
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
are covered. Sol review remains.

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
- No commit or push.
