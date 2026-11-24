/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.dialog;

import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import com.microproject.model.task.Task;

/**
 * Per-view choices of the Calendar Format tab: bar colour per task type, text styles, week height
 * and the layout options, with the pure decisions the calendar canvas takes from them.
 */
public final class CalendarViewSettings implements ViewSettingsState {
	public enum BarType { NORMAL, CRITICAL, MILESTONE }
	public enum TextItem { BAR_TEXT, DAY_NUMBERS }

	/** Font size and weight of one kind of calendar text; a size of 0 keeps the configured one. */
	public record TextStyle(int size, boolean bold) {
		public static final TextStyle DEFAULT = new TextStyle(0, false);
	}

	public static final int DEFAULT_ROW_HEIGHT = 105;
	public static final int MIN_ROW_HEIGHT = 60;
	/** Largest font size the Calendar Format dialog can choose. */
	public static final int MAX_TEXT_SIZE = 36;
	/** Height of one task bar plus the gap below it. */
	public static final int BAR_PITCH = 23;
	private static final int BAR_GAP = 4;
	private static final int TASKS_TOP = 23;
	private static final int TASKS_TOP_WITH_COMPANION = 38;
	private static final int BOTTOM_MARGIN = 10;

	private final Map<BarType, Integer> barColors = new EnumMap<>(BarType.class);
	private final Map<TextItem, TextStyle> textStyles = new EnumMap<>(TextItem.class);
	private int rowHeight = DEFAULT_ROW_HEIGHT;
	private boolean fitAsManyTasksAsPossible;
	private boolean automaticLayout = true;
	private boolean taskListVisible;

	/** Bar colour (RGB) for a task type, or null for the theme colour. */
	public Integer getBarColor(BarType type) { return barColors.get(type); }
	public void setBarColor(BarType type, Integer rgb) {
		if (rgb == null) barColors.remove(type);
		else barColors.put(type, rgb);
	}

	public TextStyle getTextStyle(TextItem item) { return textStyles.getOrDefault(item, TextStyle.DEFAULT); }
	public void setTextStyle(TextItem item, TextStyle style) {
		if (style == null || TextStyle.DEFAULT.equals(style)) textStyles.remove(item);
		else textStyles.put(item, style);
	}

	public int getRowHeight() { return rowHeight; }
	public void setRowHeight(int value) { rowHeight = Math.max(MIN_ROW_HEIGHT, value); }
	public boolean isFitAsManyTasksAsPossible() { return fitAsManyTasksAsPossible; }
	public void setFitAsManyTasksAsPossible(boolean value) { fitAsManyTasksAsPossible = value; }
	public boolean isAutomaticLayout() { return automaticLayout; }
	public void setAutomaticLayout(boolean value) { automaticLayout = value; }
	public boolean isTaskListVisible() { return taskListVisible; }
	public void setTaskListVisible(boolean value) { taskListVisible = value; }

	@Override
	public String saveSettings() {
		Properties p = new Properties();
		if (rowHeight != DEFAULT_ROW_HEIGHT) p.setProperty("rowHeight", Integer.toString(rowHeight));
		if (fitAsManyTasksAsPossible) p.setProperty("fitMany", "true");
		if (!automaticLayout) p.setProperty("automaticLayout", "false");
		if (taskListVisible) p.setProperty("taskListVisible", "true");
		for (Map.Entry<BarType, Integer> entry : barColors.entrySet())
			p.setProperty("bar." + entry.getKey().name(), Integer.toString(entry.getValue()));
		for (Map.Entry<TextItem, TextStyle> entry : textStyles.entrySet())
			p.setProperty("text." + entry.getKey().name(), entry.getValue().size() + "," + entry.getValue().bold());
		return Codec.write(p);
	}

	@Override
	public void restoreSettings(String state) {
		Properties p = Codec.read(state);
		rowHeight = DEFAULT_ROW_HEIGHT;
		fitAsManyTasksAsPossible = false;
		automaticLayout = true;
		taskListVisible = false;
		barColors.clear();
		textStyles.clear();
		setRowHeight(Codec.integer(p, "rowHeight", DEFAULT_ROW_HEIGHT));
		fitAsManyTasksAsPossible = Codec.bool(p, "fitMany", false);
		automaticLayout = Codec.bool(p, "automaticLayout", true);
		taskListVisible = Codec.bool(p, "taskListVisible", false);
		for (BarType type : BarType.values()) {
			String value = p.getProperty("bar." + type.name());
			if (value == null) continue;
			try {
				int rgb = Integer.parseInt(value.trim());
				if (rgb >= 0 && rgb <= 0xFFFFFF) barColors.put(type, rgb);
			} catch (NumberFormatException malformed) {
				// keep the theme colour
			}
		}
		for (TextItem item : TextItem.values()) {
			String value = p.getProperty("text." + item.name());
			if (value == null) continue;
			String[] parts = value.split(",");
			try {
				if (parts.length == 2) {
					int size = Integer.parseInt(parts[0].trim());
					setTextStyle(item, new TextStyle(Math.max(0, Math.min(MAX_TEXT_SIZE, size)),
						Boolean.parseBoolean(parts[1].trim())));
				}
			} catch (NumberFormatException malformed) {
				// keep the default style
			}
		}
	}

	/** The bar type a task is drawn as. */
	public static BarType barType(Task task) {
		if (task.isMilestone()) return BarType.MILESTONE;
		return task.isCritical() ? BarType.CRITICAL : BarType.NORMAL;
	}

	/** Row height that shows the busiest day completely (Week Height Best Fit). */
	public static int bestFitRowHeight(int maxTasksPerDay, boolean companionDay) {
		return bestFitRowHeight(maxTasksPerDay, companionDay, BAR_PITCH);
	}

	/** As {@link #bestFitRowHeight(int, boolean)} for bars of the given pitch (see {@link #barPitch(int)}). */
	public static int bestFitRowHeight(int maxTasksPerDay, boolean companionDay, int pitch) {
		int top = companionDay ? TASKS_TOP_WITH_COMPANION : TASKS_TOP;
		return Math.max(MIN_ROW_HEIGHT, top + Math.max(1, maxTasksPerDay) * pitch + BOTTOM_MARGIN);
	}

	/** Height of a task bar whose text has the given font height; never below the default bar. */
	public static int barHeight(int fontHeight) {
		return Math.max(BAR_PITCH - BAR_GAP, fontHeight + 5);
	}

	/** Height of a bar plus the gap below it. */
	public static int barPitch(int fontHeight) {
		return barHeight(fontHeight) + BAR_GAP;
	}

	/** How many task bars fit wholly in a row of the given height; zero when not even one does. */
	public static int visibleTaskCount(int rowHeight, boolean companionDay) {
		return visibleTaskCount(rowHeight, companionDay, BAR_PITCH);
	}

	/** As {@link #visibleTaskCount(int, boolean)} for bars of the given pitch. */
	public static int visibleTaskCount(int rowHeight, boolean companionDay, int pitch) {
		int top = companionDay ? TASKS_TOP_WITH_COMPANION : TASKS_TOP;
		return Math.max(0, (rowHeight - top - BOTTOM_MARGIN) / pitch);
	}

	/** Sort key of the "fit as many tasks as possible" layout: the tasks with least slack come first. */
	public static long slackSortKey(Task task) {
		return task.getTotalSlack();
	}
}
