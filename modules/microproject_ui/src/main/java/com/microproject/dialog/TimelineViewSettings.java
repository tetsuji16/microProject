/*******************************************************************************
 * MIT License
 *
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
package com.microproject.dialog;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.microproject.dialog.CalendarViewSettings.TextStyle;

/** Display choices of the Timeline view that the Timeline Format tab edits. */
public final class TimelineViewSettings implements ViewSettingsState {
	public enum TextItem { LABEL, DATES }

	public static final int MIN_TEXT_LINES = 1;
	public static final int MAX_TEXT_LINES = 4;
	/** The most timeline bars a view can hold; creation and restore use the same limit. */
	public static final int MAX_BARS = 20;

	private long rangeStart;
	private long rangeEnd;
	private boolean showDates = true;
	private boolean showTimescale = true;
	private boolean showToday = true;
	private boolean showTaskDates;
	private boolean showProgress;
	private boolean showPanZoom;
	private boolean showOverlapped = true;
	private boolean expanded;
	private boolean barLabels = true;
	private int textLines = MIN_TEXT_LINES;
	private int barCount = 1;
	private final Map<TextItem, TextStyle> textStyles = new EnumMap<>(TextItem.class);
	private final Set<Long> callouts = new LinkedHashSet<>();
	private final Map<Long, Integer> barOfTask = new LinkedHashMap<>();

	/** Start of the explicit date range, or 0 while the range follows the displayed tasks. */
	public long getRangeStart() { return rangeStart; }
	public long getRangeEnd() { return rangeEnd; }
	public boolean hasDateRange() { return rangeStart > 0L && rangeEnd > rangeStart; }

	/** Sets an explicit range; an empty or reversed range returns the view to automatic. */
	public void setDateRange(long start, long end) {
		if (start > 0L && end > start) {
			rangeStart = start;
			rangeEnd = end;
		} else {
			rangeStart = 0L;
			rangeEnd = 0L;
		}
	}

	public boolean isShowDates() { return showDates; }
	public void setShowDates(boolean value) { showDates = value; }
	public boolean isShowTimescale() { return showTimescale; }
	public void setShowTimescale(boolean value) { showTimescale = value; }
	public boolean isShowToday() { return showToday; }
	public void setShowToday(boolean value) { showToday = value; }
	public boolean isShowTaskDates() { return showTaskDates; }
	public void setShowTaskDates(boolean value) { showTaskDates = value; }
	public boolean isShowProgress() { return showProgress; }
	public void setShowProgress(boolean value) { showProgress = value; }
	public boolean isShowPanZoom() { return showPanZoom; }
	public void setShowPanZoom(boolean value) { showPanZoom = value; }
	public boolean isShowOverlapped() { return showOverlapped; }
	public void setShowOverlapped(boolean value) { showOverlapped = value; }
	public boolean isExpanded() { return expanded; }
	public void setExpanded(boolean value) { expanded = value; }
	public boolean isBarLabels() { return barLabels; }
	public void setBarLabels(boolean value) { barLabels = value; }
	public int getTextLines() { return textLines; }
	public void setTextLines(int value) { textLines = Math.max(MIN_TEXT_LINES, Math.min(MAX_TEXT_LINES, value)); }

	public TextStyle getTextStyle(TextItem item) { return textStyles.getOrDefault(item, TextStyle.DEFAULT); }
	public void setTextStyle(TextItem item, TextStyle style) {
		if (style == null || TextStyle.DEFAULT.equals(style)) textStyles.remove(item);
		else textStyles.put(item, style);
	}

	public boolean isCallout(Long taskId) { return taskId != null && callouts.contains(taskId); }
	public void setCallout(Long taskId, boolean callout) {
		if (taskId == null) return;
		if (callout) callouts.add(taskId);
		else callouts.remove(taskId);
	}

	/** How many timeline bars the view shows, at least one. */
	public int getBarCount() { return barCount; }

	/** Appends an empty timeline bar and returns its index. */
	public int addBar() {
		if (barCount < MAX_BARS) barCount++;
		return barCount - 1;
	}

	/** The timeline bar a task is on; bars that no longer exist fall back to the first. */
	public int barOf(Long taskId) {
		Integer bar = taskId == null ? null : barOfTask.get(taskId);
		return bar == null || bar < 0 || bar >= barCount ? 0 : bar;
	}

	public void setBarOf(Long taskId, int bar) {
		if (taskId == null) return;
		if (bar <= 0 || bar >= barCount) barOfTask.remove(taskId);
		else barOfTask.put(taskId, bar);
	}

	/** The text-line count of the view as a lane height, so that Expand and Text Lines scale the bars. */
	public int laneHeight() {
		return (expanded ? 26 : 16) + (textLines - 1) * 14;
	}

	@Override
	public String saveSettings() {
		Properties p = new Properties();
		if (hasDateRange()) {
			p.setProperty("rangeStart", Long.toString(rangeStart));
			p.setProperty("rangeEnd", Long.toString(rangeEnd));
		}
		if (!showDates) p.setProperty("showDates", "false");
		if (!showTimescale) p.setProperty("showTimescale", "false");
		if (!showToday) p.setProperty("showToday", "false");
		if (showTaskDates) p.setProperty("showTaskDates", "true");
		if (showProgress) p.setProperty("showProgress", "true");
		if (showPanZoom) p.setProperty("showPanZoom", "true");
		if (!showOverlapped) p.setProperty("showOverlapped", "false");
		if (expanded) p.setProperty("expanded", "true");
		if (!barLabels) p.setProperty("barLabels", "false");
		if (textLines != MIN_TEXT_LINES) p.setProperty("textLines", Integer.toString(textLines));
		if (barCount > 1) p.setProperty("barCount", Integer.toString(barCount));
		for (Map.Entry<TextItem, TextStyle> entry : textStyles.entrySet())
			p.setProperty("text." + entry.getKey().name(), entry.getValue().size() + "," + entry.getValue().bold());
		if (!callouts.isEmpty()) p.setProperty("callouts", join(callouts));
		if (!barOfTask.isEmpty()) {
			List<String> parts = new ArrayList<>();
			for (Map.Entry<Long, Integer> entry : barOfTask.entrySet())
				parts.add(entry.getKey() + ":" + entry.getValue());
			p.setProperty("bars", String.join(",", parts));
		}
		return Codec.write(p);
	}

	@Override
	public void restoreSettings(String state) {
		Properties p = Codec.read(state);
		setDateRange(Codec.number(p, "rangeStart", 0L), Codec.number(p, "rangeEnd", 0L));
		showDates = Codec.bool(p, "showDates", true);
		showTimescale = Codec.bool(p, "showTimescale", true);
		showToday = Codec.bool(p, "showToday", true);
		showTaskDates = Codec.bool(p, "showTaskDates", false);
		showProgress = Codec.bool(p, "showProgress", false);
		showPanZoom = Codec.bool(p, "showPanZoom", false);
		showOverlapped = Codec.bool(p, "showOverlapped", true);
		expanded = Codec.bool(p, "expanded", false);
		barLabels = Codec.bool(p, "barLabels", true);
		setTextLines(Codec.integer(p, "textLines", MIN_TEXT_LINES));
		barCount = Math.max(1, Math.min(MAX_BARS, Codec.integer(p, "barCount", 1)));
		textStyles.clear();
		for (TextItem item : TextItem.values()) {
			String value = p.getProperty("text." + item.name());
			if (value == null) continue;
			String[] parts = value.split(",");
			try {
				if (parts.length == 2)
					setTextStyle(item, new TextStyle(Integer.parseInt(parts[0].trim()), Boolean.parseBoolean(parts[1].trim())));
			} catch (NumberFormatException malformed) {
				// keep the default style
			}
		}
		callouts.clear();
		for (String id : p.getProperty("callouts", "").split(",")) {
			try {
				if (!id.isBlank()) callouts.add(Long.parseLong(id.trim()));
			} catch (NumberFormatException malformed) {
				// skip the entry
			}
		}
		barOfTask.clear();
		for (String entry : p.getProperty("bars", "").split(",")) {
			String[] parts = entry.split(":");
			try {
				if (parts.length == 2) setBarOf(Long.parseLong(parts[0].trim()), Integer.parseInt(parts[1].trim()));
			} catch (NumberFormatException malformed) {
				// skip the entry
			}
		}
	}

	private static String join(Set<Long> ids) {
		List<String> parts = new ArrayList<>();
		for (Long id : ids) parts.add(Long.toString(id));
		return String.join(",", parts);
	}
}
