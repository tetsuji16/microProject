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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *******************************************************************************/
package com.microproject.dialog.calendar;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DateFormatSymbols;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.SpinnerDateModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkRangeException;
import com.microproject.pm.calendar.WorkWeek;
import com.microproject.pm.calendar.WorkWeekPeriod;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;

/** Editable MSP-style dated work-week rules on the dialog's private scratch calendar. */
final class WorkWeekEditorPanel extends JPanel {
	private final ChangeWorkingTimeDialogBox owner;
	private final DefaultListModel<WorkWeekPeriod> model = new DefaultListModel<>();
	private final JList<WorkWeekPeriod> periods = new JList<>(model);
	private final JButton add = new JButton(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekAdd"));
	private final JButton edit = new JButton(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekEdit"));
	private final JButton remove = new JButton(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekRemove"));
	private boolean editable;

	WorkWeekEditorPanel(ChangeWorkingTimeDialogBox owner) {
		super(new BorderLayout(6, 6));
		this.owner = owner;
		periods.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setName("workWeekEditorPanel");
		add.setName("workWeekAddButton");
		edit.setName("workWeekDetailsButton");
		remove.setName("workWeekRemoveButton");
		periods.setCellRenderer((list, value, index, selected, focus) -> {
			String start = DateTime.dateFormatInstance("yyyy-MM-dd").format(new Date(value.getStart()));
			String end = DateTime.dateFormatInstance("yyyy-MM-dd").format(new Date(value.getEnd()));
			JLabel label = (JLabel) new DefaultListCellRenderer()
				.getListCellRendererComponent(list, value, index, selected, focus);
			label.setText(value.getName() + "  (" + start + " – " + end + ")");
			return label;
		});
		periods.addListSelectionListener(this::selectionChanged);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING));
		actions.add(add);
		actions.add(edit);
		actions.add(remove);
		add(new JScrollPane(periods), BorderLayout.CENTER);
		add(actions, BorderLayout.SOUTH);
		add.addActionListener(event -> editPeriod(null));
		edit.addActionListener(event -> editPeriod(periods.getSelectedValue()));
		remove.addActionListener(event -> removeSelected());
		refresh();
	}

	void setEditable(boolean editable) {
		this.editable = editable;
		add.setEnabled(editable);
		refreshButtons();
	}

	void refresh() {
		model.clear();
		WorkingCalendar calendar = owner.getScratchCalendar();
		if (calendar == null) return;
		for (WorkWeekPeriod period : calendar.getWorkWeekPeriods()) model.addElement(period);
		refreshButtons();
	}

	private void selectionChanged(ListSelectionEvent event) {
		if (!event.getValueIsAdjusting()) refreshButtons();
	}

	private void refreshButtons() {
		boolean hasSelection = periods.getSelectedValue() != null;
		edit.setEnabled(editable && hasSelection);
		remove.setEnabled(editable && hasSelection);
	}

	private void removeSelected() {
		WorkWeekPeriod selected = periods.getSelectedValue();
		if (selected == null) return;
		owner.getScratchCalendar().removeWorkWeekPeriod(selected);
		owner.markCalendarEdited();
		refresh();
		owner.updateView();
	}

	private void editPeriod(WorkWeekPeriod source) {
		PeriodEditor editor = new PeriodEditor(source);
		while (true) {
			int choice = JOptionPane.showConfirmDialog(this, editor.panel,
				Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekDetails"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (choice != JOptionPane.OK_OPTION) return;
			try {
				WorkWeekPeriod updated = editor.toPeriod();
				WorkingCalendar calendar = owner.getScratchCalendar();
				if (source != null) calendar.removeWorkWeekPeriod(source);
				try {
					calendar.addOrReplaceWorkWeekPeriod(updated);
				} catch (RuntimeException invalid) {
					if (source != null) calendar.addOrReplaceWorkWeekPeriod(source);
					throw invalid;
				}
				owner.markCalendarEdited();
				refresh();
				owner.updateView();
				return;
			} catch (RuntimeException invalid) {
				JOptionPane.showMessageDialog(this, invalid.getMessage(),
					Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekDetails"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private static final class PeriodEditor {
		private final JPanel panel = new JPanel(new GridBagLayout());
		private final JTextField name = new JTextField(22);
		private final JSpinner start;
		private final JSpinner end;
		private final JCheckBox[] working = new JCheckBox[7];
		private final JTextField[][] from = new JTextField[7][WorkingHours.getDefault().getIntervals().size()];
		private final JTextField[][] to = new JTextField[7][WorkingHours.getDefault().getIntervals().size()];

		PeriodEditor(WorkWeekPeriod source) {
			Date first = source == null ? new Date() : new Date(source.getStart());
			Date last = source == null ? first : new Date(source.getEnd());
			start = dateSpinner(first);
			end = dateSpinner(last);
			name.setName("workWeekName");
			if (source != null) name.setText(source.getName());
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(2, 3, 2, 3);
			c.anchor = GridBagConstraints.WEST;
			c.gridx = 0; c.gridy = 0; panel.add(new JLabel(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekName")), c);
			c.gridx = 1; c.gridwidth = 3; panel.add(name, c); c.gridwidth = 1;
			c.gridy++; c.gridx = 0; panel.add(new JLabel(Messages.getString("ChangeWorkingTimeDialogBox.From")), c);
			c.gridx = 1; panel.add(start, c);
			c.gridx = 2; panel.add(new JLabel(Messages.getString("ChangeWorkingTimeDialogBox.To")), c);
			c.gridx = 3; panel.add(end, c);
			String[] weekdays = new DateFormatSymbols().getWeekdays();
			c.gridy++; c.gridx = 0; panel.add(new JLabel(" "), c);
			for (int slot = 0; slot < from[0].length; slot++) {
				c.gridx = 1 + slot * 2; panel.add(new JLabel(Messages.getString("ChangeWorkingTimeDialogBox.From")), c);
				c.gridx = 2 + slot * 2; panel.add(new JLabel(Messages.getString("ChangeWorkingTimeDialogBox.To")), c);
			}
			for (int day = 0; day < 7; day++) {
				WorkDay existing = source == null ? null : source.getWeekDay(day);
				working[day] = new JCheckBox(weekdays[day == 0 ? Calendar.SUNDAY : day + 1],
					existing == null ? day > 0 && day < 6 : existing.isWorking());
				c.gridy++; c.gridx = 0; panel.add(working[day], c);
				WorkingHours hours = existing == null ? (working[day].isSelected() ? WorkDay.getDefaultWorkDay().getWorkingHours() : null)
					: existing.getWorkingHours();
				for (int slot = 0; slot < from[day].length; slot++) {
					from[day][slot] = new JTextField(5);
					to[day][slot] = new JTextField(5);
					var range = hours == null ? null : hours.getInterval(slot);
					if (range != null) {
						from[day][slot].setText(formatTime(range.getStart()));
						to[day][slot].setText(formatTime(range.getEnd()));
					}
					c.gridx = 1 + slot * 2; panel.add(from[day][slot], c);
					c.gridx = 2 + slot * 2; panel.add(to[day][slot], c);
				}
				working[day].addActionListener(event -> refreshEnablement());
			}
			refreshEnablement();
		}

		private void refreshEnablement() {
			for (int day = 0; day < 7; day++)
				for (int slot = 0; slot < from[day].length; slot++) {
					from[day][slot].setEnabled(working[day].isSelected());
					to[day][slot].setEnabled(working[day].isSelected());
				}
		}

		WorkWeekPeriod toPeriod() {
			String label = name.getText().trim();
			if (label.isEmpty()) throw new IllegalArgumentException(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekNameRequired"));
			long first = DateTime.dayFloor(((Date) start.getValue()).getTime());
			long last = DateTime.dayFloor(((Date) end.getValue()).getTime());
			WorkWeek week = new WorkWeek();
			for (int day = 0; day < 7; day++) {
				if (!working[day].isSelected()) {
					week.setWeekDay(day, WorkDay.getNonWorkingDay());
					continue;
				}
				WorkingHours hours = new WorkingHours();
				int count = 0;
				LocalTime previousFinish = null;
				try {
					for (int slot = 0; slot < from[day].length; slot++) {
						String begin = from[day][slot].getText().trim();
						String finish = to[day][slot].getText().trim();
						if (begin.isEmpty() && finish.isEmpty()) continue;
						if (begin.isEmpty() || finish.isEmpty())
							throw new IllegalArgumentException(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekTimePairRequired"));
						LocalTime startTime = LocalTime.parse(begin);
						LocalTime finishTime = LocalTime.parse(finish);
						if (!finishTime.isAfter(startTime) || (previousFinish != null && startTime.isBefore(previousFinish)))
							throw new IllegalArgumentException(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekInvalidTime"));
						hours.setInterval(count++, timeValue(begin), timeValue(finish));
						previousFinish = finishTime;
					}
				} catch (WorkRangeException | java.time.format.DateTimeParseException invalid) {
					throw new IllegalArgumentException(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekInvalidTime"), invalid);
				}
				if (count == 0) throw new IllegalArgumentException(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekTimeRequired"));
				WorkDay workDay = new WorkDay();
				workDay.setWorkingHours(hours);
				week.setWeekDay(day, workDay);
			}
			return new WorkWeekPeriod(label, first, last, week);
		}

		private static JSpinner dateSpinner(Date value) {
			JSpinner spinner = new JSpinner(new SpinnerDateModel(value, null, null, Calendar.DAY_OF_MONTH));
			spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
			return spinner;
		}

		private static String formatTime(long value) {
			GregorianCalendar calendar = DateTime.calendarInstance();
			calendar.setTimeInMillis(value);
			return "%02d:%02d".formatted(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
		}

		private static long timeValue(String value) {
			LocalTime time = LocalTime.parse(value);
			GregorianCalendar calendar = DateTime.calendarInstance();
			calendar.clear();
			calendar.set(1970, Calendar.JANUARY, 1, time.getHour(), time.getMinute());
			return calendar.getTimeInMillis();
		}
	}
}
