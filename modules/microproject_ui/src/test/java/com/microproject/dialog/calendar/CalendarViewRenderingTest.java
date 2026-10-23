/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.SwingUtilities;

import com.microproject.util.AlternativeCalendarDisplay;

import org.junit.jupiter.api.Test;

class CalendarViewRenderingTest {
	@Test
	void calendarUsesHiDpiTextRendering() {
		CalendarView calendar = new CalendarView(0L);

		assertTrue(calendar.getAntialiased(),
				"the legacy month view must opt into antialiased text for HiDPI displays");
	}

	@Test
	void calendarKeepsOneReadableMonthWhenItsContainerIsLarge() {
		GregorianCalendar expectedMonth = new GregorianCalendar(2026, Calendar.SEPTEMBER, 1);
		CalendarView calendar = new CalendarView(expectedMonth.getTimeInMillis());
		calendar.setSize(665, 420);

		GregorianCalendar lastVisible = new GregorianCalendar();
		lastVisible.setTimeInMillis(calendar.getLastDisplayedDate());
		assertTrue(lastVisible.get(Calendar.YEAR) == 2026
				&& lastVisible.get(Calendar.MONTH) == Calendar.SEPTEMBER,
			"a large dialog must not shrink calendar text by packing extra months");
	}

	@Test
	void calendarUsesOfficeStyleWeekdayColors() {
		assertTrue(com.microproject.util.FlatUiSupport.errorForeground().equals(CalendarView.weekDayColor(Calendar.SUNDAY)));
		assertTrue(com.microproject.util.FlatUiSupport.accentColor().equals(CalendarView.weekDayColor(Calendar.SATURDAY)));
		assertTrue(com.microproject.util.FlatUiSupport.labelForeground().equals(CalendarView.weekDayColor(Calendar.MONDAY)));
	}

	@Test
	void calendarAddsAlternativeCalendarLabelsWithoutChangingPrimaryDates() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			Locale previousFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
			TimeZone previousTimeZone = TimeZone.getDefault();
			try {
				Locale.setDefault(Locale.Category.FORMAT,
					Locale.forLanguageTag("en-US-u-ca-islamic-umalqura"));
				class InspectableCalendarView extends CalendarView {
					String monthTitle(long millis, String primary) {
						return getCalendarMonthTitle(millis, primary);
					}
					String dayLabel(long millis) {
						return getSecondaryDayLabel(millis);
					}
				}
				GregorianCalendar gregorianDate = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
				gregorianDate.clear();
				gregorianDate.set(2024, Calendar.MARCH, 11);
				long dateMillis = gregorianDate.getTimeInMillis();
				GregorianCalendar marchStart = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
				marchStart.clear();
				marchStart.set(2024, Calendar.MARCH, 1);
				long marchStartMillis = marchStart.getTimeInMillis();
				LocalDate targetDate = LocalDate.of(2024, 3, 11);
				String expectedMonth = "March 2024 ("
					+ AlternativeCalendarDisplay.companionMonth(YearMonth.of(2024, 3),
						Locale.getDefault(Locale.Category.FORMAT)) + ")";
				String expectedDay = AlternativeCalendarDisplay.companionDay(targetDate,
					Locale.getDefault(Locale.Category.FORMAT));
				for (String zoneId : new String[] { "UTC", "America/Los_Angeles", "Asia/Tokyo" }) {
					TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
					InspectableCalendarView calendar = new InspectableCalendarView();
					calendar.setFirstDisplayedDate(dateMillis);
					calendar.setSize(665, 420);
					calendar.doLayout();
					renderOffscreen(calendar);

					String actualMonth = calendar.monthTitle(marchStartMillis, "March 2024");
					String actualDay = calendar.dayLabel(dateMillis);
					assertEquals(expectedMonth, actualMonth,
						"the Gregorian March title must use the same March dates in " + zoneId);
					assertEquals(expectedDay, actualDay,
						"the secondary day must use the same Gregorian date in " + zoneId);

					String tooltip = null;
					Long tooltipCellMillis = null;
					Point tooltipPoint = null;
					for (int y = 0; y < calendar.getHeight() && tooltip == null; y++) {
						for (int x = 0; x < calendar.getWidth(); x++) {
							long cellDate = calendar.getDayAt(x, y);
							if (cellDate > 0 && LocalDate.ofInstant(Instant.ofEpochMilli(cellDate), ZoneId.of("GMT"))
								.equals(targetDate)) {
								tooltipCellMillis = cellDate;
								tooltipPoint = new Point(x, y);
								tooltip = calendar.getToolTipText(new MouseEvent(calendar, MouseEvent.MOUSE_MOVED,
									0L, 0, x, y, 0, false));
								break;
							}
						}
					}
					String tooltipDiagnostics = "tooltip=" + tooltip
						+ ", FORMAT=" + Locale.getDefault(Locale.Category.FORMAT)
						+ ", DISPLAY=" + Locale.getDefault(Locale.Category.DISPLAY)
						+ ", systemZone=" + ZoneId.systemDefault()
						+ ", targetDate=" + targetDate + ", cellMillis=" + tooltipCellMillis
						+ ", cellDateGMT=" + (tooltipCellMillis == null ? null
							: LocalDate.ofInstant(Instant.ofEpochMilli(tooltipCellMillis), ZoneId.of("GMT")))
						+ ", point=" + tooltipPoint;
					assertTrue(tooltip != null && tooltip.contains("Gregorian date:")
						&& tooltip.contains("March 11, 2024") && tooltip.contains("Selected calendar:"),
						"a per-day tooltip must show both full dates while preserving Gregorian as primary; "
							+ tooltipDiagnostics);
					gregorianDate.setTimeInMillis(dateMillis);
					assertTrue(gregorianDate.get(Calendar.YEAR) == 2024
							&& gregorianDate.get(Calendar.MONTH) == Calendar.MARCH
							&& gregorianDate.get(Calendar.DAY_OF_MONTH) == 11,
						"the primary Gregorian date remains unchanged in " + zoneId);
				}
			} finally {
				Locale.setDefault(Locale.Category.FORMAT, previousFormatLocale);
				TimeZone.setDefault(previousTimeZone);
			}
		});
	}

	@Test
	void calendarTooltipSupportsDatesBeforeUnixEpoch() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			GregorianCalendar date = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			date.clear();
			date.set(1969, Calendar.DECEMBER, 31);
			CalendarView calendar = new CalendarView(date.getTimeInMillis());
			calendar.setFirstDisplayedDate(date.getTimeInMillis());
			calendar.setSize(665, 420);
			calendar.doLayout();
			renderOffscreen(calendar);
			String tooltip = null;
			for (int y = 0; y < calendar.getHeight() && tooltip == null; y++) {
				for (int x = 0; x < calendar.getWidth(); x++) {
					long cellDate = calendar.getDayAt(x, y);
					if (cellDate < -106) {
						tooltip = calendar.getToolTipText(new MouseEvent(calendar, MouseEvent.MOUSE_MOVED,
							0L, 0, x, y, 0, false));
						break;
					}
				}
			}
			assertTrue(tooltip != null && tooltip.contains("1969"),
				"a date before the Unix epoch must still produce a Gregorian date tooltip");
		});
	}

	private static void renderOffscreen(CalendarView calendar) {
		BufferedImage image = new BufferedImage(calendar.getWidth(), calendar.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			calendar.printAll(graphics);
		} finally {
			graphics.dispose();
		}
	}
}
