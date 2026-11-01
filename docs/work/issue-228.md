# Issue #228 — bounded collection capacities

State: IN PROGRESS
Issue: https://github.com/tetsuji16/microProject/issues/228
Baseline HEAD: `5f442e260`; clean worktree.

## Acceptance

Review the current active-source inventory; change only cases with a reliable bound, document every remaining site and measurement evidence, and run owning module tests. Do not use conversion counts as the completion metric.

## Current slice

**Next slice:** complete the remaining active-source inventory, documenting each bound or reason to leave the site unchanged.
**Done when:** every remaining candidate has a disposition and the relevant module checks and measurements are recorded.
**Then:** integrate and close only after all acceptance conditions are met.

## Evidence

- The export validates each input row, creates exactly one output row, and only updates the destination after the full loop succeeds. The existing empty and invalid paths remain unchanged.
- `MpxResourceConverterTest` and `.\gradlew.bat :microproject_exchange:test --console=plain` passed.
- Synthetic allocation probe, Temurin 25.0.4.1, default JVM, `ThreadMXBean.getThreadAllocatedBytes`, 5 warmups and 20 measurements of 100,000 lists per size, object references reused: unsized versus exact capacity averaged 80 vs 48 bytes/list (1 row), 80 vs 80 (10), 160 vs 88 (11), and 1400 vs 440 (100). This isolates ArrayList allocation and is not an end-to-end throughput claim.
- The full inventory review remains incomplete; the issue remains open.
