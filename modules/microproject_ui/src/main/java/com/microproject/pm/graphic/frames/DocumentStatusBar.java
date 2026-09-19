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
package com.microproject.pm.graphic.frames;

import java.awt.FlowLayout;
import java.util.Calendar;
import java.text.MessageFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.microproject.strings.Messages;
import com.microproject.timescale.TimeScale;

/**
 * Slim status bar at the bottom of a document frame, mirroring the essential
 * MS Project status bar: the current timescale zoom level and the number of
 * selected tasks.
 */
public class DocumentStatusBar extends JPanel {
	private static final long serialVersionUID = 1L;

	private final JLabel zoomLabel = new JLabel();
	private final JLabel selectionLabel = new JLabel();
	private final JLabel modeLabel = new JLabel();

	public DocumentStatusBar() {
		setLayout(new FlowLayout(FlowLayout.LEFT, 16, 2));
		setBorder(new EmptyBorder(1, 6, 1, 6));
		add(zoomLabel);
		add(selectionLabel);
		add(modeLabel);
		setZoom(0, 1);
		setSelectedCount(0);
		setMode("StatusBar.Ready");
	}

	public void setMode(String modeKey) {
		modeLabel.setText(Messages.getString(modeKey));
	}

	/** Shows a short non-modal document warning without interrupting editing. */
	public void setMessage(String message) {
		modeLabel.setText(message == null || message.isBlank() ? Messages.getString("StatusBar.Ready") : message);
	}

	public void setZoom(int scaleIndex, int scaleCount) {
		zoomLabel.setText(formatZoom(scaleIndex, scaleCount));
	}
	public void setZoom(int scaleIndex, int scaleCount, TimeScale scale) {
		zoomLabel.setText(formatZoom(scaleIndex, scaleCount, scale));
	}

	public void setSelectedCount(int count) {
		selectionLabel.setText(formatSelection(count));
	}

	static String formatZoom(int scaleIndex, int scaleCount) {
		int clampedCount = Math.max(1, scaleCount);
		int clampedIndex = Math.min(Math.max(0, scaleIndex), clampedCount - 1);
		return MessageFormat.format(Messages.getString("StatusBar.Zoom"), clampedIndex + 1, clampedCount);
	}
	static String formatZoom(int scaleIndex, int scaleCount, TimeScale scale) {
		if (scale == null) return formatZoom(scaleIndex, scaleCount);
		int clampedCount = Math.max(1, scaleCount);
		int clampedIndex = Math.min(Math.max(0, scaleIndex), clampedCount - 1);
		int position = clampedIndex + 1;
		String label = scaleCount <= 0 ? formatInterval(scale.getCalendarField1(), scale.getNumber1())
				: formatMspScale(position, scale);
		return MessageFormat.format(Messages.getString("StatusBar.ZoomSemantic"),
				label, position, clampedCount);
	}

	/**
	 * Uses the stable MS Project-style names from the original status-bar
	 * proposal. The underlying interval remains the fallback for scales outside
	 * the documented 1..10 range.
	 */
	private static String formatMspScale(int position, TimeScale scale) {
		String key = switch (position) {
			case 1 -> "StatusBar.Scale.Minute";
			case 2 -> "StatusBar.Scale.Hourly";
			case 3 -> "StatusBar.Scale.Daily";
			case 4 -> "StatusBar.Scale.DetailedWeekly";
			case 5 -> "StatusBar.Scale.Weekly";
			case 6 -> "StatusBar.Scale.BiMonthly";
			case 7 -> "StatusBar.Scale.Monthly";
			case 8 -> "StatusBar.Scale.Quarterly";
			case 9 -> "StatusBar.Scale.HalfYearly";
			case 10 -> "StatusBar.Scale.Yearly";
			default -> null;
		};
		return key == null ? formatInterval(scale.getCalendarField1(), scale.getNumber1()) : Messages.getString(key);
	}
	private static String formatInterval(int field, int amount) {
		int n = Math.max(1, amount);
		return switch (field) {
			case Calendar.HOUR_OF_DAY -> interval("StatusBar.Interval.Hour", n);
			case Calendar.DAY_OF_WEEK, Calendar.DAY_OF_MONTH, Calendar.DAY_OF_YEAR ->
					n == 1 ? Messages.getString("StatusBar.Interval.Day") : interval("StatusBar.Interval.Days", n);
			case Calendar.WEEK_OF_YEAR, Calendar.WEEK_OF_MONTH ->
					n == 1 ? Messages.getString("StatusBar.Interval.Week") : interval("StatusBar.Interval.Weeks", n);
			case Calendar.MONTH -> n == 1 ? Messages.getString("StatusBar.Interval.Month")
					: n == 3 ? Messages.getString("StatusBar.Interval.Quarter")
					: n == 6 ? Messages.getString("StatusBar.Interval.HalfYear")
					: interval("StatusBar.Interval.Months", n);
			case Calendar.YEAR -> n == 1 ? Messages.getString("StatusBar.Interval.Year")
					: interval("StatusBar.Interval.Years", n);
			default -> Messages.getString("StatusBar.Interval.Custom");
		};
	}
	private static String interval(String key, int amount) {
		return MessageFormat.format(Messages.getString(key), amount);
	}

	static String formatSelection(int count) {
		return MessageFormat.format(Messages.getString("StatusBar.SelectedTasks"), Math.max(0, count));
	}
}
