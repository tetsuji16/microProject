# ADR-0003: Date and time APIs

- Status: Accepted
- Date: 2026-11-25
- Decision owner: Project maintainers
- Related work: issue #1389 (this decision), #1390, #1391; parent #1368

## Context

Date and time code mixes the legacy `java.util` APIs with `java.time`. At
master `80ce3279c`, `modules/*/src/main` has about 92 files that use
`java.util.Date`, 41 that use `java.util.Calendar` or `GregorianCalendar`,
17 that use `SimpleDateFormat`, and 44 that already use `java.time`
(counting commands are under "Inventory"). Without a rule, each change decides
again whether a call site may move to `java.time`, and some changes would
silently alter persisted values.

Three constraints make a wholesale rewrite unsafe:

- Schedule values are `long` epoch milliseconds whose calendar fields are read
  in GMT. `DateTime.calendarInstance()` returns a GMT `GregorianCalendar`, and
  `DateTime` helpers such as `dayFloor` and `midnightNextDay` work in that
  zone. Saved projects carry these values; POD serializes the `long` fields
  directly.
- The user's date format preference (`GlobalPreferences`) is a
  `SimpleDateFormat` pattern string, so its syntax is part of saved settings.
- Classes that extend `java.text.Format` (`SpecialDateFormat`,
  `DurationFormat` and others) plug into Swing formatted
  fields and must keep the `format(Object, StringBuffer, FieldPosition)` and
  `parseObject` contracts.

## Decision

1. **Stored and computed values.** Keep `long` milliseconds with the GMT
   calendar convention for schedule values. Do not change the meaning of any
   value read from or written to POD or MPO.
2. **New code.** At boundaries (parsing user input, producing display or log
   strings, reading or writing external formats) use `java.time`
   (`Instant`, `LocalDate`, `LocalDateTime`, `Duration`, `DateTimeFormatter`).
   Do not add new uses of `SimpleDateFormat`, `Calendar` or
   `GregorianCalendar`.
3. **Left as they are.** The user's format pattern strings, the
   `java.text.Format` subclasses, and the scheduling arithmetic
   (`CalendarDefinition`, `WorkingCalendar`, `WorkWeek`, `WorkRange` and their
   iterators). Moving the scheduling arithmetic to `java.time` needs a
   performance measurement and a separate ADR.
4. **How to convert an existing call site.** First add a characterization test
   that records the current inputs and outputs, then replace the API, then show
   that the same test passes unchanged. The table must include boundary values:
   the first and last day of a year, a leap day, a non-existent date, one-digit
   month and day input, and leading or trailing extra characters. If any result
   differs, stop and record the difference in the issue rather than changing
   the expected value.
5. **Conversions.** Convert between milliseconds and `java.time` with
   `Instant.ofEpochMilli(millis)` / `Instant.toEpochMilli()` and
   `ZoneOffset.UTC`. Do not use `ZoneId.systemDefault()` for schedule values.
   A schedule date `d` is `Date.from(d.atStartOfDay(ZoneOffset.UTC).toInstant())`.
6. **Known difference.** `SimpleDateFormat` switches to the Julian calendar
   before 15 October 1582, while `java.time` uses the proleptic Gregorian
   calendar. Results differ only for such dates. The schedule range is far
   inside the Gregorian era (`DateTime` caps dates at 2050), so this difference
   is accepted for schedule values.

## Inventory

The counts above come from these commands, run at the repository root:

```text
rg -l "java\.util\.Date\b|\bnew Date\(" modules --type java -g "**/src/main/**"
rg -l "java\.util\.(Gregorian)?Calendar\b" modules --type java -g "**/src/main/**"
rg -l "SimpleDateFormat" modules --type java -g "**/src/main/**"
rg -l "java\.time\." modules --type java -g "**/src/main/**"
```

## First conversions

- [#1390](https://github.com/tetsuji16/microProject/issues/1390):
  `TimeUtil.toUTCString` uses a shared `DateTimeFormatter`, and an unused
  shared `SimpleDateFormat` in `CalendarDefinition` is removed.
- [#1391](https://github.com/tetsuji16/microProject/issues/1391):
  `YearlessDateInputParser` parses explicit year-month-day input with a
  strict `DateTimeFormatter`.

Further conversions are filed as separate issues under #1368 and follow
section 4.

## Consequences

- Reviews can reject a new `SimpleDateFormat` or `Calendar` in new code by
  pointing to this ADR.
- Conversions stay small and test-first; persisted values do not change.
- The scheduling engine keeps its current date arithmetic until a measured
  proposal replaces it.
