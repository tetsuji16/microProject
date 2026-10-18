# Commons dependency decision record (#857)

## Commons Lang

Production imports of `org.apache.commons.lang` were inventoried across core,
contrib, exchange and UI and migrated to Commons Lang 3. Core APIs used were
`DateUtils`, `StringUtils`, `ObjectUtils` and `ToStringBuilder`; the other
production use of `StringEscapeUtils` was in `LocaleDialog`. The Apache migration
guide says most migration cases need only the package change, but notes removed
APIs and behavior changes. `DateUtils.UTC_TIME_ZONE` was removed in Lang 3, so
its uses now name `TimeZone.getTimeZone("GMT")` explicitly. Lang 2's
constant was initialized from `TimeZone.getTimeZone("GMT")`, so this also
preserves the zone ID used by formatting.

`SelectOption.toConfigurationXML` depended on Lang 2 XML escaping's numeric
escaping of non-ASCII characters. Commons Text does not apply that legacy rule
by default. Its translator is composed from the official XML basic/apostrophe
entity maps plus `NumericEntityEscaper`, and a regression test fixes the output
for XML delimiters, quotes, an accented character and a Euro sign. The Java
unescape used by `LocaleDialog` moved to Commons Text without changing its call.

## Commons Pool

Pool 1 remains a direct runtime dependency. All direct use is in core:

- `ObjectEvent` instances are borrowed by `ObjectEventManager` and
  `AlgorithmFieldUpdater`, reset on activation and returned after notification.
- `CalendarDefinition` borrows an iterator around schedule arithmetic and
  returns it in `finally` blocks.
- `NodeSorter` borrows temporary `ArrayList` instances and clears them before
  return.
- `Factory.GregorianCalendarPool` and `Factory.DatePool` remain declared but no
  in-repository caller was found.

These are mutable, caller-managed reuse lifecycles; removing them without
replacement would change allocation and reset/identity behavior. Commons Pool 2
has a rewritten API requiring `PooledObject` wrappers and lifecycle conversion.
No representative application workload benchmark is available here, so this
change keeps Pool 1 rather than claim a performance win or remove active reuse
without evidence. Revisit Pool 2 when a benchmark can compare the event and
calendar hot paths.

## Commons Digester

All production `org.apache.commons.digester` imports in core and the test import
were migrated to `org.apache.commons.digester3`. The XML rule setup continues to
use the same `Digester` methods (`addObjectCreate`, `addSetProperties`,
`addCallMethod`, and factory rules). `FormFormatTest` exercises parse results
with Digester 3; the full core suite exercises configuration loading and
reflection-backed object creation. The frozen POD and MPO compatibility paths
do not use Digester and were also covered by exchange tests.

The migrated artifact is Apache's `org.apache.commons:commons-digester3:3.2`.
The separate BeanUtils dependency brought by Digester is tracked by #752 and
remains outside this issue's scope.

## References

- [Commons Lang 2 to 3 migration guide](https://commons.apache.org/proper/commons-lang/article3_0.html)
- [Commons Pool overview](https://commons.apache.org/proper/commons-pool/index.html)
- [Commons Digester 3.2 `Digester` API](https://commons.apache.org/proper/commons-digester/commons-digester-3.2/apidocs/org/apache/commons/digester3/Digester.html)
- [Commons Text `StringEscapeUtils` API](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringEscapeUtils.html)
