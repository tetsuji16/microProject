/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;

import com.microproject.dialog.CalendarViewSettings.TextStyle;
import com.microproject.dialog.TimelineViewSettings;
import com.microproject.pm.graphic.frames.NetworkCalendarFormatDialogs.OptionPanel;
import com.microproject.pm.task.Task;

/**
 * Option panels of the Timeline and Team Planner Format tabs (text styles, date range, text lines,
 * colours, the existing-task picker). Each panel reads the current choices on creation and writes
 * them back through {@code apply()} when the user confirms.
 */
final class TimelineTeamPlannerFormatDialogs {
	private static final long DAY = 24L * 60L * 60L * 1000L;

	private TimelineTeamPlannerFormatDialogs() {
	}

	private static JSpinner sizeSpinner(TextStyle style, String name) {
		JSpinner size = new JSpinner(new SpinnerNumberModel(style.size(), 0, 36, 1));
		size.getAccessibleContext().setAccessibleName(name);
		return size;
	}

	/** Timeline Text Styles: size and weight of the task labels and of the dates. */
	static OptionPanel timelineTextStyles(TimelineViewSettings settings, Function<String, String> text) {
		OptionPanel panel = new OptionPanel();
		Map<TimelineViewSettings.TextItem, JSpinner> sizes = new EnumMap<>(TimelineViewSettings.TextItem.class);
		Map<TimelineViewSettings.TextItem, JCheckBox> bolds = new EnumMap<>(TimelineViewSettings.TextItem.class);
		int rowIndex = 0;
		for (TimelineViewSettings.TextItem item : TimelineViewSettings.TextItem.values()) {
			TextStyle style = settings.getTextStyle(item);
			String itemText = text.apply("FormatDialog.timeline." + item.name());
			JSpinner size = sizeSpinner(style, itemText + " " + text.apply("FormatDialog.fontSize"));
			JCheckBox bold = new JCheckBox(text.apply("FormatDialog.bold"), style.bold());
			bold.getAccessibleContext().setAccessibleName(itemText + " " + text.apply("FormatDialog.bold"));
			sizes.put(item, size);
			bolds.put(item, bold);
			NetworkCalendarFormatDialogs.row(panel, rowIndex++, new JLabel(itemText), size, bold);
		}
		panel.applier = () -> {
			for (TimelineViewSettings.TextItem item : TimelineViewSettings.TextItem.values())
				settings.setTextStyle(item, new TextStyle((Integer) sizes.get(item).getValue(), bolds.get(item).isSelected()));
		};
		return panel;
	}

	/** Team Planner Text Styles: size and weight of the task text. */
	static OptionPanel textStyle(TextStyle current, Consumer<TextStyle> onApply, Function<String, String> text) {
		OptionPanel panel = new OptionPanel();
		String label = text.apply("FormatDialog.text.BAR_TEXT");
		JSpinner size = sizeSpinner(current, label + " " + text.apply("FormatDialog.fontSize"));
		JCheckBox bold = new JCheckBox(text.apply("FormatDialog.bold"), current.bold());
		bold.getAccessibleContext().setAccessibleName(label + " " + text.apply("FormatDialog.bold"));
		NetworkCalendarFormatDialogs.row(panel, 0, new JLabel(label), size, bold);
		panel.applier = () -> onApply.accept(new TextStyle((Integer) size.getValue(), bold.isSelected()));
		return panel;
	}

	/** Set Date Range: the start and finish the timeline spans, or fit to the displayed tasks. */
	static OptionPanel dateRange(TimelineViewSettings settings, long[] shownRange, Function<String, String> text) {
		OptionPanel panel = new OptionPanel();
		boolean fixed = settings.hasDateRange();
		long start = fixed ? settings.getRangeStart() : shownRange[0];
		long finish = fixed ? settings.getRangeEnd() : shownRange[1];
		JCheckBox auto = new JCheckBox(text.apply("FormatDialog.dateRangeAuto"), !fixed);
		JSpinner startSpinner = new JSpinner(new SpinnerDateModel(new Date(start), null, null, java.util.Calendar.DAY_OF_MONTH));
		JSpinner finishSpinner = new JSpinner(new SpinnerDateModel(new Date(finish), null, null, java.util.Calendar.DAY_OF_MONTH));
		startSpinner.setEditor(new JSpinner.DateEditor(startSpinner, "yyyy-MM-dd"));
		finishSpinner.setEditor(new JSpinner.DateEditor(finishSpinner, "yyyy-MM-dd"));
		startSpinner.getAccessibleContext().setAccessibleName(text.apply("FormatDialog.dateRangeStart"));
		finishSpinner.getAccessibleContext().setAccessibleName(text.apply("FormatDialog.dateRangeFinish"));
		startSpinner.setEnabled(fixed);
		finishSpinner.setEnabled(fixed);
		auto.addActionListener(event -> {
			startSpinner.setEnabled(!auto.isSelected());
			finishSpinner.setEnabled(!auto.isSelected());
		});
		NetworkCalendarFormatDialogs.row(panel, 0, auto);
		NetworkCalendarFormatDialogs.row(panel, 1, new JLabel(text.apply("FormatDialog.dateRangeStart")), startSpinner);
		NetworkCalendarFormatDialogs.row(panel, 2, new JLabel(text.apply("FormatDialog.dateRangeFinish")), finishSpinner);
		panel.applier = () -> {
			if (auto.isSelected()) {
				settings.setDateRange(0L, 0L);
				return;
			}
			long from = ((Date) startSpinner.getValue()).getTime();
			long to = ((Date) finishSpinner.getValue()).getTime();
			settings.setDateRange(from, to > from ? to : from + DAY);
		};
		return panel;
	}

	/** Text Lines: how many lines of text a bar shows. */
	static OptionPanel textLines(int current, int min, int max, Consumer<Integer> onApply, Function<String, String> text) {
		OptionPanel panel = new OptionPanel();
		JSpinner lines = new JSpinner(new SpinnerNumberModel(Math.max(min, Math.min(max, current)), min, max, 1));
		lines.getAccessibleContext().setAccessibleName(text.apply("FormatDialog.textLines"));
		NetworkCalendarFormatDialogs.row(panel, 0, new JLabel(text.apply("FormatDialog.textLines")), lines);
		panel.applier = () -> onApply.accept((Integer) lines.getValue());
		return panel;
	}

	/** One colour choice (fill or border); the first entry keeps the theme colour. */
	static OptionPanel color(String title, Integer current, Consumer<Integer> onApply, Function<String, String> text) {
		OptionPanel panel = new OptionPanel();
		JComboBox<Integer> combo = NetworkCalendarFormatDialogs.colorCombo(current, title, text.apply("FormatDialog.default"));
		NetworkCalendarFormatDialogs.row(panel, 0, new JLabel(title), combo);
		panel.applier = () -> onApply.accept(NetworkCalendarFormatDialogs.colorOf(combo));
		return panel;
	}

	/** Existing Tasks: the project tasks not yet on the timeline, to show there. */
	static OptionPanel existingTasks(List<Task> candidates, Consumer<List<Task>> onApply, Function<String, String> text) {
		OptionPanel panel = new OptionPanel();
		DefaultListModel<Task> model = new DefaultListModel<>();
		candidates.forEach(model::addElement);
		JList<Task> list = new JList<>(model);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setVisibleRowCount(10);
		list.getAccessibleContext().setAccessibleName(text.apply("FormatDialog.chooseTasks"));
		list.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;
			@Override public java.awt.Component getListCellRendererComponent(JList<?> source, Object value, int index, boolean selected, boolean focus) {
				return super.getListCellRendererComponent(source, value instanceof Task task ? task.getName() : value, index, selected, focus);
			}
		});
		NetworkCalendarFormatDialogs.row(panel, 0, new JLabel(text.apply("FormatDialog.chooseTasks")));
		NetworkCalendarFormatDialogs.row(panel, 1, new JScrollPane(list));
		panel.applier = () -> onApply.accept(list.getSelectedValuesList());
		return panel;
	}
}
