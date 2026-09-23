# OpenProj Java 25 modernization progress

Status: in progress; this document does not mark any issue phase complete.

Baseline checked: `origin/master` at `59eb4e0dc1157b382d754deb0acc79fd3384ac5b`.
The modernization branch is based on that commit. Progress commits are listed in
Git and summarized in [issue #595](https://github.com/tetsuji16/ProjectLibre/issues/595).

## Inventory caveat

`docs/legal/license-provenance.csv` uses the former `projectlibre_*` module and
`com.projectlibre1` package paths. A read-only path check of its
`projectlibre_core` production-Java rows marked `normalized_openproj_match=true`
found 280 ledger rows and 267 existing files after mapping the old core root
and package path to the current `microproject_core` / `com.microproject` tree.
The remaining 13 path mappings did not resolve with that simple mapping.

These figures are only candidate-discovery results. They do **not** prove that
the rows still match current file contents, that every hunk is OpenProj-origin,
that every file has a production caller, or that each file is safe to modernize.
The CSV's normalized match is not a legal conclusion. Resolve stale paths,
inspect origin at hunk/responsibility level, search callers (including dynamic
configuration and serialization), and classify compatibility-sensitive code
before counting a candidate as eligible.

## Completed, source-confirmed work segments

The following active core classes have relevant code in the OpenProj baseline
under `openproj_core/src/com/projity/`. Only the modified responsibility is
claimed as reviewed; untouched hunks in these classes remain out of scope.

| Area | Classes / responsibility | Outcome |
|---|---|---|
| Calendar intervals | `WorkDay`, `WorkRange`, `WorkingHours`, `WorkWeek`, `WorkingCalendar`, `CalendarService`, `CalendarDefinition`, `Interval`, `CalendarEvent` | Typed collection / clone / comparison modernization; removed the unused calendar-cache list; fixed incorrect working-day intersection, lost overtime state in a range constructor, long-comparison overflow, equality asymmetry, and a strong-reference calendar-cache registry leak. |
| Duration and rates | `Duration`, `DurationFormat`, `Rate`, `RateFormat`, `PercentFormat` | Pattern matching and switch expressions; removed dead duration conversion calculations and redundant string copying; made encoding masks and formatter mode state immutable; fixed parse-position end-of-input exceptions and fractional/large-rate comparison. |
| Distribution identity | `DistributionData` | Pattern matching and hash/equality alignment with the active `DistributionComparator` key, including `projectId`; added focused identity/payload tests. |
| Personal contour | `PersonalContourMaker` | Replaced raw collection types with `List<PersonalContourBucket>` / typed `Collection`; used pattern matching for bucket narrowing. |

Separate work in `com.microproject.core.time` is bridge/fork code, not counted as
an OpenProj-origin modernization result unless hunk provenance is established.

## Verification evidence to date

- Focused regression tests were run for the changed interval, calendar, duration,
  rate, distribution identity, and contour responsibilities. In particular, the
  Rate fractional-ordering, RateFormat end-position, and DistributionData
  project-key tests were observed failing before their fixes.
- `:microproject_core:test` and `:microproject_exchange:test` passed at the
  distribution-identity checkpoint; core tests and exchange compilation passed
  at the contour checkpoint.
- Application, exchange, UI, and reports compilation passed at an earlier
  calendar-clone integration checkpoint. No GUI route, layout, or Swing behavior
  was changed, so Robot/GUI tests were not repeated.
- Compilation/test success is evidence only for those exercised modules; it is
  not evidence that the inventory is exhausted or that every compatibility
  boundary has been audited.

## Remaining work

- Resolve all 13 stale path mappings and validate candidate hashes against the
  current baseline; separate exact-origin hunks from later fork changes.
- Search all 267 provisional active-path candidates for actual callers and
  compatibility boundaries; prioritize candidates by correctness/maintenance
  risk, not by conversion count.
- Audit remaining eligible Java in exchange, UI, reports, and other core
  responsibilities; do not add GUI tests unless a physical GUI contract or
  visual surface is changed.
- Record cleanup/deletion candidates only after checking reflection, resource
  configuration, serialization, ServiceLoader, action IDs, and format readers.
- Update this inventory and issue #595 after each meaningful audited batch; do
  not close the issue while any required phase or unresolved in-scope work
  remains.
