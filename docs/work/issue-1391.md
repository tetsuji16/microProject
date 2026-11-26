# Issue #1391 — year-included date parsing

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/1391
PR: https://github.com/tetsuji16/microProject/pull/1405
Baseline: `060623240`; clean detached worktree before integration.

## Contract

Replace strict ISO date parsing with `java.time` while preserving the GMT date value and fallback parsing. Yearless parsing and other calendars are out of scope.

| ID | Acceptance | Evidence |
|---|---|---|
| A1 | Same parameterized inputs pass before and after | Focused core test passed against `a1d954d^` and `a1d954d`, using the same corrected Asia/Tokyo fallback expectations at `0955240f7`. |
| A2 | No production `SimpleDateFormat` in parser | Source inspection at `05592e628`. |
| A3 | Existing core and UI parser tests pass | Both focused tests passed locally at `128ecba0b`. |
| A4 | Core and UI module tests | Passed locally at `05592e628` before the historical compatibility fix; rerun required. |
| A5 | Build and GUI acceptance; physical `2026/9/25` entry commits | GUI run 45718486785 at `060623240` passed U26 physical date transaction. Exact integrated candidate CI pending. Historical Gregorian cutover regression passes on both original and corrected code; the initial java.time implementation failed. |

## Next slice

Finish core/UI module tests and exact-candidate CI, review all PR comments and merge only if required checks pass. Record any failure before changing code.
