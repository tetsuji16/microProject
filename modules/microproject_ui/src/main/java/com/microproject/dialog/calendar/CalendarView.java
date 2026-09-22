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
import java.util.Calendar;

import javax.swing.UIManager;

import com.microproject.contrib.calendar.ContribIntervals;
import com.microproject.contrib.calendar.JXXMonthView;


/**
 *
 */
public class CalendarView extends JXXMonthView {

	/**
	 *
	 */
	public CalendarView() {
		this(System.currentTimeMillis());
	}

	/**
	 * @param initialTime
	 */
	public CalendarView(long initialTime) {
		super(initialTime);
		Font uiFont = UIManager.getFont("Label.font");
		if (uiFont != null) setFont(uiFont);
		// JXXMonthView predates HiDPI Swing and disables text antialiasing by
		// default.  Keep its selection/date model, but opt this user-facing
		// calendar into the platform's high-quality text rasterisation.
		setAntialiased(true);
		setPreferredCols(1);
		setPreferredRows(1);
	}

	static Color weekDayColor(int calendarDayOfWeek) {
		return switch (calendarDayOfWeek) {
			case Calendar.SUNDAY -> Color.RED;
			case Calendar.SATURDAY -> Color.BLUE;
			default -> Color.BLACK;
		};
	}

	@Override
	protected Color getWeekDayForeground(int calendarDayOfWeek) {
		return weekDayColor(calendarDayOfWeek);
	}

	public Intervals getSelectedFixedIntervals(){
		return new Intervals(super.getSelectedIntervals());
	}




}
