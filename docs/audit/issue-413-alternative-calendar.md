# Issue #413: alternative calendar display

## Display contract

- The Gregorian `LocalDate` remains the source for month navigation, task
  placement, drag-to-reschedule, scheduling, and persisted values.
- The Calendar View may display a companion month/date when the JVM FORMAT
  locale explicitly selects a Java `Chronology` through its Unicode `ca`
  locale extension. Without a supported selection, the existing Gregorian
  label remains unchanged.
- Java 25 `Chronology.ofLocale(Locale)` returns a non-ISO chronology only when
  the calendar system is explicitly selected in the locale. The documented
  supported system chronologies include Japanese, Hijrah, Minguo, and Thai
  Buddhist calendars. See [Java SE 25 Chronology API](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/chrono/Chronology.html#ofLocale(java.util.Locale)).

## Current implementation and evidence

`CalendarViewDialogBox` keeps Gregorian day numbers and adds the selected
chronology's month label and day number as secondary display. An unsupported
locale extension falls back to Gregorian display. Date arithmetic and task
model values are not converted.

Verified with:

```text
.\gradlew.bat :microproject_ui:test --tests "com.microproject.util.AlternativeCalendarDisplayTest" :microproject_ui:guiTest --tests "com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotCalendarCommandOpensUsableCalendarDialog" --max-workers=1 --console=plain
```

The unit cases cover ISO-only, Japanese, Hijrah, and unsupported calendar
extensions. The Robot case opens Calendar View through the physical View ribbon
route, asserts the task card and the Japanese-era month label, and verifies
month navigation.

## Remaining scope

This is partial progress; issue #413 remains open. Windows' independently
selected alternate calendar is not proven to reach the JVM FORMAT locale, and
the alternative-date display has not been applied to other calendar columns
and headers requested by the issue. Confirm those host integration and surface
requirements before claiming issue completion.
