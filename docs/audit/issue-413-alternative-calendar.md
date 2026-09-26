# Issue #413: alternative calendar display

## Display contract

- The Gregorian `LocalDate` remains the source for month navigation, task
  placement, drag-to-reschedule, scheduling, and persisted values.
- The Calendar View may display a companion month/date when the JVM FORMAT
  locale explicitly selects a Java `Chronology` through its Unicode `ca`
  locale extension. On Windows, if the locale selects ISO, the resolver reads
  the current-user `iCalendarType` value and maps supported Windows IDs to
  equivalent Java chronologies. Without a supported selection, Gregorian
  labels remain unchanged.
- Java 25 `Chronology.ofLocale(Locale)` returns a non-ISO chronology only when
  the calendar system is explicitly selected in the locale. The documented
  supported system chronologies include Japanese, Hijrah, Minguo, and Thai
  Buddhist calendars. See [Java SE 25 Chronology API](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/chrono/Chronology.html#ofLocale(java.util.Locale)).
- Windows documents the active calendar as a locale calendar type and defines
  IDs for Japanese (3), Taiwan (4), Thai (7), and Um Al Qura (23), among other
  calendars. See [Microsoft Calendar Identifiers](https://learn.microsoft.com/en-us/windows/win32/intl/calendar-identifiers).

## Current implementation and evidence

`CalendarViewDialogBox` keeps Gregorian day numbers and adds the selected
chronology's month label and day number as secondary display. The Windows
current-user setting is read once from `HKCU\Control Panel\International`;
IDs are mapped only where Java's built-in chronology represents the same
calendar. Other Windows IDs and unsupported locale extensions fall back to
Gregorian display. Date arithmetic and task model values are not converted.

Verified with:

```text
.\gradlew.bat :microproject_ui:test --tests "com.microproject.util.AlternativeCalendarDisplayTest" :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotCalendarCommandOpensUsableCalendarDialog" --max-workers=1 --console=plain
```

The unit cases cover ISO-only, Japanese, Hijrah, unsupported calendar
extensions, supported Windows ID mappings, and registry-output parsing. The
Robot case opens Calendar View through the physical View ribbon
route, asserts the task card and the Japanese-era month label, and verifies
month navigation.

## Remaining scope

This is partial progress; issue #413 remains open. Windows calendar IDs that do
not have an equivalent built-in Java chronology (including Hebrew and the
distinct CAL_HIJRI calendar) still display Gregorian dates, and the additional
calendar columns/headers requested by the issue have not been implemented.
