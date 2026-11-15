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

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import com.microproject.dialog.CalendarViewSettings.TextStyle;

/** Display choices of the Team Planner view that the Team Planner Format tab edits. */
public final class TeamPlannerViewSettings implements ViewSettingsState {
	/** Task categories that carry their own bar style. */
	public enum Category { AUTO, MANUAL, ACTUAL, EXTERNAL, LATE }

	public static final int MIN_TEXT_LINES = 1;
	public static final int MAX_TEXT_LINES = 4;
	public static final int MAX_OUTLINE_LEVEL = 9;

	private int outlineLevel;
	private boolean gridlines = true;
	private int textLines = MIN_TEXT_LINES;
	private boolean expandRows;
	private boolean showUnassigned;
	private boolean showUnscheduled;
	private boolean preventOverallocation;
	private TextStyle textStyle = TextStyle.DEFAULT;
	private final Map<Category, Integer> fills = new EnumMap<>(Category.class);
	private final Map<Category, Integer> borders = new EnumMap<>(Category.class);
	private final Map<Long, Integer> taskFills = new LinkedHashMap<>();
	private final Map<Long, Integer> taskBorders = new LinkedHashMap<>();

	/** Deepest outline level shown; 0 shows every level. */
	public int getOutlineLevel() { return outlineLevel; }
	public void setOutlineLevel(int level) { outlineLevel = Math.max(0, Math.min(MAX_OUTLINE_LEVEL, level)); }
	public boolean isGridlines() { return gridlines; }
	public void setGridlines(boolean value) { gridlines = value; }
	public int getTextLines() { return textLines; }
	public void setTextLines(int value) { textLines = Math.max(MIN_TEXT_LINES, Math.min(MAX_TEXT_LINES, value)); }
	public boolean isExpandRows() { return expandRows; }
	public void setExpandRows(boolean value) { expandRows = value; }
	public boolean isShowUnassigned() { return showUnassigned; }
	public void setShowUnassigned(boolean value) { showUnassigned = value; }
	public boolean isShowUnscheduled() { return showUnscheduled; }
	public void setShowUnscheduled(boolean value) { showUnscheduled = value; }
	public boolean isPreventOverallocation() { return preventOverallocation; }
	public void setPreventOverallocation(boolean value) { preventOverallocation = value; }
	public TextStyle getTextStyle() { return textStyle; }
	public void setTextStyle(TextStyle style) { textStyle = style == null ? TextStyle.DEFAULT : style; }

	/** The category fill as an RGB value, or null while the default applies. */
	public Integer getFill(Category category) { return fills.get(category); }
	public Integer getBorder(Category category) { return borders.get(category); }

	public void setFill(Category category, Integer rgb) { put(fills, category, rgb); }
	public void setBorder(Category category, Integer rgb) { put(borders, category, rgb); }

	/** Returns one category to its default colours. */
	public void resetCategory(Category category) {
		fills.remove(category);
		borders.remove(category);
	}

	/** Colours the selected tasks override their category with. */
	public Integer getTaskFill(Long taskId) { return taskFills.get(taskId); }
	public Integer getTaskBorder(Long taskId) { return taskBorders.get(taskId); }
	public void setTaskFill(Long taskId, Integer rgb) { put(taskFills, taskId, rgb); }
	public void setTaskBorder(Long taskId, Integer rgb) { put(taskBorders, taskId, rgb); }

	private static <K> void put(Map<K, Integer> map, K key, Integer rgb) {
		if (key == null) return;
		if (rgb == null) map.remove(key);
		else map.put(key, rgb);
	}

	@Override
	public String saveSettings() {
		Properties p = new Properties();
		if (outlineLevel != 0) p.setProperty("outlineLevel", Integer.toString(outlineLevel));
		if (!gridlines) p.setProperty("gridlines", "false");
		if (textLines != MIN_TEXT_LINES) p.setProperty("textLines", Integer.toString(textLines));
		if (expandRows) p.setProperty("expandRows", "true");
		if (showUnassigned) p.setProperty("showUnassigned", "true");
		if (showUnscheduled) p.setProperty("showUnscheduled", "true");
		if (preventOverallocation) p.setProperty("preventOverallocation", "true");
		if (!TextStyle.DEFAULT.equals(textStyle)) p.setProperty("text", textStyle.size() + "," + textStyle.bold());
		for (Map.Entry<Category, Integer> e : fills.entrySet()) p.setProperty("fill." + e.getKey().name(), e.getValue().toString());
		for (Map.Entry<Category, Integer> e : borders.entrySet()) p.setProperty("border." + e.getKey().name(), e.getValue().toString());
		for (Map.Entry<Long, Integer> e : taskFills.entrySet()) p.setProperty("taskFill." + e.getKey(), e.getValue().toString());
		for (Map.Entry<Long, Integer> e : taskBorders.entrySet()) p.setProperty("taskBorder." + e.getKey(), e.getValue().toString());
		return Codec.write(p);
	}

	@Override
	public void restoreSettings(String state) {
		Properties p = Codec.read(state);
		setOutlineLevel(Codec.integer(p, "outlineLevel", 0));
		gridlines = Codec.bool(p, "gridlines", true);
		setTextLines(Codec.integer(p, "textLines", MIN_TEXT_LINES));
		expandRows = Codec.bool(p, "expandRows", false);
		showUnassigned = Codec.bool(p, "showUnassigned", false);
		showUnscheduled = Codec.bool(p, "showUnscheduled", false);
		preventOverallocation = Codec.bool(p, "preventOverallocation", false);
		textStyle = TextStyle.DEFAULT;
		String text = p.getProperty("text");
		if (text != null) {
			String[] parts = text.split(",");
			try {
				if (parts.length == 2) textStyle = new TextStyle(Integer.parseInt(parts[0].trim()), Boolean.parseBoolean(parts[1].trim()));
			} catch (NumberFormatException malformed) {
				// keep the default style
			}
		}
		fills.clear();
		borders.clear();
		taskFills.clear();
		taskBorders.clear();
		for (String key : p.stringPropertyNames()) {
			try {
				int rgb = Integer.parseInt(p.getProperty(key).trim());
				if (key.startsWith("fill.")) fills.put(Category.valueOf(key.substring(5)), rgb);
				else if (key.startsWith("border.")) borders.put(Category.valueOf(key.substring(7)), rgb);
				else if (key.startsWith("taskFill.")) taskFills.put(Long.parseLong(key.substring(9)), rgb);
				else if (key.startsWith("taskBorder.")) taskBorders.put(Long.parseLong(key.substring(11)), rgb);
			} catch (IllegalArgumentException malformed) {
				// skip the entry
			}
		}
	}
}
