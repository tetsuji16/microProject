/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.dialog.calendar;

import java.awt.Color;
import java.awt.Font;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;

import javax.swing.UIManager;
import javax.accessibility.AccessibleContext;

import com.microproject.contrib.calendar.ContribIntervals;
import com.microproject.contrib.calendar.JXXMonthView;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.AlternativeCalendarDisplay;


/**
 *
 */
public class CalendarView extends JXXMonthView {
	private final Font baseFont;
	private AccessibleContext accessibleContext;
	private static final float MAX_READABLE_SCALE = 2.0f;
	private static final ZoneId CALENDAR_ZONE = ZoneId.of("GMT");

	/**
	 *
	 */
	public CalendarView() {
		this(System.currentTimeMillis());
	}

	@Override
	public AccessibleContext getAccessibleContext() {
		if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {};
		return accessibleContext;
	}

	/**
	 * @param initialTime
	 */
	public CalendarView(long initialTime) {
		super(initialTime);
		Font uiFont = UIManager.getFont("Label.font");
		if (uiFont != null) setFont(uiFont);
		baseFont = getFont();
		// JXXMonthView predates HiDPI Swing and disables text antialiasing by
		// default.  Keep its selection/date model, but opt this user-facing
		// calendar into the platform's high-quality text rasterisation.
		setAntialiased(true);
		setPreferredCols(1);
		setPreferredRows(1);
		setExpandToFitAvailableSpace(false);
		setToolTipText("");
		String legend = AlternativeCalendarDisplay.calendarLegend(Locale.getDefault(Locale.Category.FORMAT));
		getAccessibleContext().setAccessibleName("Working-time calendar");
		getAccessibleContext().setAccessibleDescription(legend);
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				fitReadableMonth();
			}
		});
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		long dateMillis = getDayAt(event.getX(), event.getY());
		// JXXMonthView uses -1 for an empty cell and -100..-106 for weekday headers.
		if (dateMillis == -1 || (dateMillis <= -100 && dateMillis >= -106)) return null;
		LocalDate date = calendarDate(dateMillis);
		return AlternativeCalendarDisplay.dayTooltip(date, Locale.getDefault(Locale.Category.FORMAT));
	}

	private void fitReadableMonth() {
		if (getWidth() <= 0 || getHeight() <= 0 || baseFont == null) return;
		if (!baseFont.equals(getFont())) setFont(baseFont);
		java.awt.Dimension monthSize = getPreferredSize();
		float scale = Math.min(getWidth() / (float) monthSize.width,
				getHeight() / (float) monthSize.height);
		scale = Math.max(1.0f, Math.min(MAX_READABLE_SCALE, scale));
		Font readableFont = baseFont.deriveFont(baseFont.getSize2D() * scale);
		if (!readableFont.equals(getFont())) setFont(readableFont);
	}

	static Color weekDayColor(int calendarDayOfWeek) {
		return switch (calendarDayOfWeek) {
			case Calendar.SUNDAY -> FlatUiSupport.errorForeground();
			case Calendar.SATURDAY -> FlatUiSupport.accentColor();
			default -> FlatUiSupport.labelForeground();
		};
	}

	@Override
	protected Color getWeekDayForeground(int calendarDayOfWeek) {
		return weekDayColor(calendarDayOfWeek);
	}

	@Override
	protected String getCalendarMonthTitle(long firstDayOfMonth, String primaryTitle) {
		LocalDate date = calendarDate(firstDayOfMonth);
		String companion = AlternativeCalendarDisplay.companionMonth(YearMonth.from(date),
			Locale.getDefault(Locale.Category.FORMAT));
		return companion.isEmpty() ? primaryTitle : primaryTitle + " (" + companion + ")";
	}

	@Override
	protected String getSecondaryDayLabel(long dateMillis) {
		LocalDate date = calendarDate(dateMillis);
		String companion = AlternativeCalendarDisplay.companionDay(date,
			Locale.getDefault(Locale.Category.FORMAT));
		return companion.equals(Integer.toString(date.getDayOfMonth())) ? "" : companion;
	}

	public Intervals getSelectedFixedIntervals(){
		return new Intervals(super.getSelectedIntervals());
	}

	private static LocalDate calendarDate(long millis) {
		return LocalDate.ofInstant(Instant.ofEpochMilli(millis), CALENDAR_ZONE);
	}

}
