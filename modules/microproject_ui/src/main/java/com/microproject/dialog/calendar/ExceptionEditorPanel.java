/*
 * MIT License
 * Copyright (c) 2026 microProject
 */
package com.microproject.dialog.calendar;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DateFormatSymbols;
import java.time.LocalTime;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

import com.microproject.pm.calendar.CalendarRecurrence;
import com.microproject.pm.calendar.RecurringCalendarException;
import com.microproject.pm.calendar.WorkDay;
import com.microproject.pm.calendar.WorkRangeException;
import com.microproject.pm.calendar.WorkingCalendar;
import com.microproject.pm.calendar.WorkingHours;
import com.microproject.strings.Messages;
import com.microproject.util.DateTime;

/** Scratch-only management of one-off and recurring date exceptions. */
final class ExceptionEditorPanel extends JPanel {
	private final ChangeWorkingTimeDialogBox owner;
	private final DefaultListModel<Entry> model = new DefaultListModel<>();
	private final JList<Entry> entries = new JList<>(model);
	private final JButton add = new JButton(Messages.getString("ChangeWorkingTimeDialogBox.ExceptionAdd"));
	private final JButton details = new JButton(Messages.getString("ChangeWorkingTimeDialogBox.ExceptionDetails"));
	private final JButton remove = new JButton(Messages.getString("ChangeWorkingTimeDialogBox.ExceptionRemove"));
	private boolean editable;

	ExceptionEditorPanel(ChangeWorkingTimeDialogBox owner) {
		super(new BorderLayout(6, 6));
		this.owner = owner;
		setName("calendarExceptionsPanel");
		entries.setName("calendarExceptionsList");
		entries.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		entries.setCellRenderer((list, value, index, selected, focus) -> {
			JLabel label = (JLabel) new DefaultListCellRenderer()
				.getListCellRendererComponent(list, value, index, selected, focus);
			label.setText(value.toString());
			return label;
		});
		entries.addListSelectionListener(this::selectionChanged);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEADING));
		add.setName("calendarExceptionAddButton");
		details.setName("calendarExceptionDetailsButton");
		remove.setName("calendarExceptionRemoveButton");
		actions.add(add);
		actions.add(details);
		actions.add(remove);
		add(new JScrollPane(entries), BorderLayout.CENTER);
		add(actions, BorderLayout.SOUTH);
		add.addActionListener(event -> addException());
		details.addActionListener(event -> editException(entries.getSelectedValue()));
		remove.addActionListener(event -> removeException());
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
		for (WorkDay day : calendar.getExceptionDays()) model.addElement(new Entry(day, null));
		for (RecurringCalendarException exception : calendar.getRecurringExceptions())
			model.addElement(new Entry(null, exception));
		refreshButtons();
	}

	private void selectionChanged(ListSelectionEvent event) {
		if (!event.getValueIsAdjusting()) refreshButtons();
	}

	private void refreshButtons() {
		boolean selected = entries.getSelectedValue() != null;
		details.setEnabled(editable && selected);
		remove.setEnabled(editable && selected);
	}

	private void addException() {
		long[] selectedRange = owner.getSelectedCalendarDateRange();
		AddEditor editor = new AddEditor(selectedRange[0], selectedRange[1]);
		while (true) {
			int choice = JOptionPane.showConfirmDialog(this, editor.panel,
				Messages.getString("ChangeWorkingTimeDialogBox.ExceptionAddTitle"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (choice != JOptionPane.OK_OPTION) return;
			try {
				WorkDay day = editor.toException();
				WorkingCalendar calendar = owner.getScratchCalendar();
				calendar.addOrReplaceException(day);
				owner.markCalendarEdited();
				owner.updateView();
				return;
			} catch (RuntimeException invalid) {
				JOptionPane.showMessageDialog(this, invalid.getMessage(),
					Messages.getString("ChangeWorkingTimeDialogBox.ExceptionAddTitle"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void editException(Entry source) {
		if (source == null) return;
		DetailsEditor editor = new DetailsEditor(source);
		while (true) {
			int choice = JOptionPane.showConfirmDialog(this, new JScrollPane(editor.panel),
				Messages.getString("ChangeWorkingTimeDialogBox.ExceptionDetailsTitle"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (choice != JOptionPane.OK_OPTION) return;
			try {
				Entry updated = editor.toEntry();
				WorkingCalendar calendar = owner.getScratchCalendar();
				if (source.day != null) calendar.removeException(source.day);
				else calendar.removeRecurringException(source.recurring);
				if (updated.day != null) calendar.addOrReplaceException(updated.day);
				else calendar.addOrReplaceRecurringException(updated.recurring);
				owner.markCalendarEdited();
				owner.updateView();
				return;
			} catch (RuntimeException invalid) {
				JOptionPane.showMessageDialog(this, invalid.getMessage(),
					Messages.getString("ChangeWorkingTimeDialogBox.ExceptionDetailsTitle"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private void removeException() {
		Entry selected = entries.getSelectedValue();
		if (selected == null) return;
		WorkingCalendar calendar = owner.getScratchCalendar();
		if (selected.day != null) calendar.removeException(selected.day);
		else calendar.removeRecurringException(selected.recurring);
		owner.markCalendarEdited();
		owner.updateView();
	}

	private record Entry(WorkDay day, RecurringCalendarException recurring) {
		@Override public String toString() {
			WorkDay base = day != null ? day : recurring.getTemplate();
			String dates = DateTime.dateFormatInstance("yyyy-MM-dd").format(new Date(base.getStart()));
			if (base.getEnd() != base.getStart())
				dates += " – " + DateTime.dateFormatInstance("yyyy-MM-dd").format(new Date(base.getEnd()));
			String suffix = recurring == null ? "" : "  [" + recurring.getRecurrence().getPattern() + "]";
			return (base.getDescription() == null ? "" : base.getDescription()) + "  (" + dates + ")" + suffix;
		}
	}

	private static final class AddEditor {
		private final JPanel panel = new JPanel(new GridBagLayout());
		private final JTextField name = new JTextField(22);
		private final JSpinner start;
		private final JSpinner finish;

		AddEditor(long startDate, long finishDate) {
			start = dateSpinner(new Date(startDate));
			finish = dateSpinner(new Date(finishDate));
			name.setName("calendarExceptionName");
			start.setName("calendarExceptionStart");
			finish.setName("calendarExceptionFinish");
			GridBagConstraints c = constraints();
			row(c, 0, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionName"), name, panel);
			row(c, 1, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionStart"), start, panel);
			row(c, 2, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionFinish"), finish, panel);
		}

		WorkDay toException() {
			String description = name.getText().trim();
			if (description.isEmpty()) throw new IllegalArgumentException(
				Messages.getString("ChangeWorkingTimeDialogBox.ExceptionNameRequired"));
			long first = dayValue(start);
			long last = dayValue(finish);
			if (last < first) throw new IllegalArgumentException(
				Messages.getString("ChangeWorkingTimeDialogBox.ExceptionInvalidRange"));
			WorkDay result = new WorkDay(first, last, description);
			result.setWorkingHours(new WorkingHours());
			return result;
		}
	}

	private static final class DetailsEditor {
		private final JPanel panel = new JPanel(new GridBagLayout());
		private final JTextField name = new JTextField(20);
		private final JSpinner start;
		private final JSpinner finish;
		private final JCheckBox working = new JCheckBox(Messages.getString("ChangeWorkingTimeDialogBox.ExceptionWorking"));
		private final JTextField[] from = new JTextField[5];
		private final JTextField[] to = new JTextField[5];
		private final JCheckBox recurring = new JCheckBox(Messages.getString("ChangeWorkingTimeDialogBox.ExceptionRecurring"));
		private final JComboBox<CalendarRecurrence.Pattern> pattern = new JComboBox<>(CalendarRecurrence.Pattern.values());
		private final JSpinner frequency = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
		private final JCheckBox weekdaysOnly = new JCheckBox(Messages.getString("ChangeWorkingTimeDialogBox.ExceptionWeekdaysOnly"));
		private final JCheckBox[] weekdays = new JCheckBox[7];
		private final JCheckBox relative = new JCheckBox(Messages.getString("ChangeWorkingTimeDialogBox.ExceptionRelative"));
		private final JSpinner dayOfMonth = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
		private final JComboBox<Integer> ordinal = new JComboBox<>(new Integer[] { 1, 2, 3, 4, 5 });
		private final JComboBox<Integer> weekday = new JComboBox<>(new Integer[] { 1, 2, 3, 4, 5, 6, 7 });
		private final JSpinner month = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
		private final JCheckBox endByDate = new JCheckBox(Messages.getString("ChangeWorkingTimeDialogBox.ExceptionEndByDate"));
		private final JSpinner endDate;
		private final JSpinner occurrences = new JSpinner(new SpinnerNumberModel(10, 1, 1000000, 1));

		DetailsEditor(Entry source) {
			WorkDay base = source.day != null ? source.day : source.recurring.getTemplate();
			start = dateSpinner(new Date(base.getStart()));
			finish = dateSpinner(new Date(base.getEnd()));
			endDate = dateSpinner(new Date(base.getEnd()));
			name.setName("calendarExceptionDetailsName");
			start.setName("calendarExceptionDetailsStart");
			finish.setName("calendarExceptionDetailsFinish");
			working.setName("calendarExceptionWorking");
			recurring.setName("calendarExceptionRecurring");
			pattern.setName("calendarExceptionPattern");
			frequency.setName("calendarExceptionFrequency");
			endByDate.setName("calendarExceptionEndByDate");
			endDate.setName("calendarExceptionEndDate");
			occurrences.setName("calendarExceptionOccurrences");
			name.setText(base.getDescription() == null ? "" : base.getDescription());
			working.setSelected(base.isWorking());
			working.addActionListener(event -> updateTimeEnablement());
			WorkingHours hours = base.getWorkingHours();
			for (int slot = 0; slot < from.length; slot++) {
				from[slot] = new JTextField(5);
				to[slot] = new JTextField(5);
				var range = hours == null ? null : hours.getInterval(slot);
				if (range != null) {
					from[slot].setText(formatTime(range.getStart()));
					to[slot].setText(formatTime(range.getEnd()));
				}
			}
			String[] names = new DateFormatSymbols().getWeekdays();
			for (int day = 0; day < weekdays.length; day++)
				weekdays[day] = new JCheckBox(names[day == 6 ? Calendar.SUNDAY : day + 2], false);
			weekday.setRenderer(new DefaultListCellRenderer() {
				@Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean selected, boolean focus) {
					int iso = (Integer) value;
					String label = names[iso == 7 ? Calendar.SUNDAY : iso + 1];
					return super.getListCellRendererComponent(list, label, index, selected, focus);
				}
			});
			pattern.setRenderer(new DefaultListCellRenderer() {
				@Override public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean selected, boolean focus) {
					String key = "ChangeWorkingTimeDialogBox.ExceptionPattern." + value;
					return super.getListCellRendererComponent(list, Messages.getString(key), index, selected, focus);
				}
			});
			recurring.addActionListener(event -> updateRecurrenceEnablement());
			relative.addActionListener(event -> updateRecurrenceEnablement());
			pattern.addActionListener(event -> updateRecurrenceEnablement());
			endByDate.addActionListener(event -> updateRecurrenceEnablement());
			GridBagConstraints c = constraints();
			int row = 0;
			row(c, row++, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionName"), name, panel);
			row(c, row++, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionStart"), start, panel);
			row(c, row++, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionFinish"), finish, panel);
			c.gridy = row++; c.gridx = 0; c.gridwidth = 2; panel.add(working, c); c.gridwidth = 1;
			for (int slot = 0; slot < from.length; slot++) {
				JPanel interval = new JPanel(new FlowLayout(FlowLayout.LEADING, 3, 0));
				interval.add(new JLabel(Messages.getString("ChangeWorkingTimeDialogBox.From")));
				interval.add(from[slot]);
				interval.add(new JLabel(Messages.getString("ChangeWorkingTimeDialogBox.To")));
				interval.add(to[slot]);
				c.gridy = row++; c.gridx = 1; panel.add(interval, c);
			}
			c.gridy = row++; c.gridx = 0; c.gridwidth = 2; panel.add(recurring, c); c.gridwidth = 1;
			row(c, row++, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionPattern"), pattern, panel);
			row(c, row++, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionFrequency"), frequency, panel);
			c.gridy = row++; c.gridx = 0; c.gridwidth = 2; panel.add(weekdaysOnly, c); c.gridwidth = 1;
			JPanel dayPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 0));
			for (JCheckBox day : weekdays) dayPanel.add(day);
			c.gridy = row++; c.gridx = 1; panel.add(dayPanel, c);
			c.gridy = row++; c.gridx = 0; c.gridwidth = 2; panel.add(relative, c); c.gridwidth = 1;
			row(c, row++, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionDayOfMonth"), dayOfMonth, panel);
			row(c, row++, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionOrdinal"), ordinal, panel);
			row(c, row++, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionWeekday"), weekday, panel);
			row(c, row++, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionMonth"), month, panel);
			c.gridy = row++; c.gridx = 0; c.gridwidth = 2; panel.add(endByDate, c); c.gridwidth = 1;
			row(c, row++, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionEndDate"), endDate, panel);
			row(c, row, Messages.getString("ChangeWorkingTimeDialogBox.ExceptionOccurrences"), occurrences, panel);
			if (source.recurring != null) initializeRecurrence(source.recurring.getRecurrence());
			else {
				Calendar calendar = DateTime.calendarInstance();
				calendar.setTimeInMillis(base.getStart());
				weekdays[isoDay(calendar)].setSelected(true);
				dayOfMonth.setValue(calendar.get(Calendar.DAY_OF_MONTH));
				month.setValue(calendar.get(Calendar.MONTH) + 1);
			}
			updateTimeEnablement();
			updateRecurrenceEnablement();
		}

		private void initializeRecurrence(CalendarRecurrence value) {
			recurring.setSelected(true);
			pattern.setSelectedItem(value.getPattern());
			frequency.setValue(value.getFrequency());
			weekdaysOnly.setSelected(value.isWorkingDaysOnly());
			for (int day = 1; day <= 7; day++) weekdays[day - 1].setSelected(value.getWeekdays().get(day));
			relative.setSelected(value.isRelative());
			dayOfMonth.setValue(value.getDayOfMonth());
			ordinal.setSelectedItem(value.getOrdinal() == -1 ? 5 : value.getOrdinal());
			weekday.setSelectedItem(value.getWeekday());
			month.setValue(value.getMonth());
			endByDate.setSelected(value.getEndMode() == CalendarRecurrence.EndMode.BY_DATE);
			endDate.setValue(new Date(value.getFinishDate()));
			occurrences.setValue(Math.max(1, value.getOccurrenceCount()));
		}

		private void updateTimeEnablement() {
			for (int slot = 0; slot < from.length; slot++) {
				from[slot].setEnabled(working.isSelected());
				to[slot].setEnabled(working.isSelected());
			}
		}

		private void updateRecurrenceEnablement() {
			boolean enabled = recurring.isSelected();
			pattern.setEnabled(enabled);
			frequency.setEnabled(enabled);
			weekdaysOnly.setEnabled(enabled && pattern.getSelectedItem() == CalendarRecurrence.Pattern.DAILY);
			for (JCheckBox day : weekdays) day.setEnabled(enabled && pattern.getSelectedItem() == CalendarRecurrence.Pattern.WEEKLY);
			boolean monthlyYearly = pattern.getSelectedItem() == CalendarRecurrence.Pattern.MONTHLY
				|| pattern.getSelectedItem() == CalendarRecurrence.Pattern.YEARLY;
			relative.setEnabled(enabled && monthlyYearly);
			dayOfMonth.setEnabled(enabled && monthlyYearly && !relative.isSelected());
			ordinal.setEnabled(enabled && monthlyYearly && relative.isSelected());
			weekday.setEnabled(enabled && monthlyYearly && relative.isSelected());
			month.setEnabled(enabled && pattern.getSelectedItem() == CalendarRecurrence.Pattern.YEARLY);
			endByDate.setEnabled(enabled);
			endDate.setEnabled(enabled && endByDate.isSelected());
			occurrences.setEnabled(enabled && !endByDate.isSelected());
		}

		Entry toEntry() {
			String label = name.getText().trim();
			if (label.isEmpty()) throw new IllegalArgumentException(
				Messages.getString("ChangeWorkingTimeDialogBox.ExceptionNameRequired"));
			long first = dayValue(start);
			long last = dayValue(finish);
			if (last < first) throw new IllegalArgumentException(
				Messages.getString("ChangeWorkingTimeDialogBox.ExceptionInvalidRange"));
			WorkingHours hours = new WorkingHours();
			if (working.isSelected()) {
				int count = 0;
				java.time.LocalTime previous = null;
				try {
					for (int slot = 0; slot < from.length; slot++) {
						String begin = from[slot].getText().trim();
						String end = to[slot].getText().trim();
						if (begin.isEmpty() && end.isEmpty()) continue;
						if (begin.isEmpty() || end.isEmpty()) throw new IllegalArgumentException(
							Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekTimePairRequired"));
						LocalTime fromTime = LocalTime.parse(begin);
						LocalTime toTime = LocalTime.parse(end);
						if (!toTime.isAfter(fromTime) || (previous != null && fromTime.isBefore(previous)))
							throw new IllegalArgumentException(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekInvalidTime"));
						hours.setInterval(count++, timeValue(begin), timeValue(end));
						previous = toTime;
					}
				} catch (WorkRangeException | java.time.format.DateTimeParseException invalid) {
					throw new IllegalArgumentException(Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekInvalidTime"), invalid);
				}
				if (count == 0) throw new IllegalArgumentException(
					Messages.getString("ChangeWorkingTimeDialogBox.WorkWeekTimeRequired"));
			}
			WorkDay template = new WorkDay(first, last, label);
			template.setWorkingHours(hours);
			if (!recurring.isSelected()) return new Entry(template, null);
			CalendarRecurrence rule = recurrence(first);
			rule.occurrenceDates(); // validate and bound expansion before changing the scratch calendar
			return new Entry(null, new RecurringCalendarException(template, rule));
		}

		private CalendarRecurrence recurrence(long first) {
			CalendarRecurrence.EndMode mode = endByDate.isSelected()
				? CalendarRecurrence.EndMode.BY_DATE : CalendarRecurrence.EndMode.AFTER_OCCURRENCES;
			long finish = dayValue(endDate);
			int count = (Integer) occurrences.getValue();
			int every = (Integer) frequency.getValue();
			CalendarRecurrence.Pattern selected = (CalendarRecurrence.Pattern) pattern.getSelectedItem();
			if (selected == CalendarRecurrence.Pattern.DAILY)
				return CalendarRecurrence.daily(first, every, weekdaysOnly.isSelected(), mode, finish, count);
			if (selected == CalendarRecurrence.Pattern.WEEKLY) {
				BitSet days = new BitSet(8);
				for (int day = 0; day < weekdays.length; day++) if (weekdays[day].isSelected()) days.set(day + 1);
				return CalendarRecurrence.weekly(first, every, days, mode, finish, count);
			}
			int selectedOrdinal = (Integer) ordinal.getSelectedItem();
			if (selectedOrdinal == 5) selectedOrdinal = -1;
			int selectedWeekday = (Integer) weekday.getSelectedItem();
			int selectedDay = (Integer) dayOfMonth.getValue();
			if (selected == CalendarRecurrence.Pattern.MONTHLY)
				return CalendarRecurrence.monthly(first, every, relative.isSelected(), selectedDay,
					selectedOrdinal, selectedWeekday, mode, finish, count);
			return CalendarRecurrence.yearly(first, every, relative.isSelected(), (Integer) month.getValue(),
				selectedDay, selectedOrdinal, selectedWeekday, mode, finish, count);
		}
	}

	private static GridBagConstraints constraints() {
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 3, 2, 3);
		c.anchor = GridBagConstraints.WEST;
		return c;
	}

	private static void row(GridBagConstraints c, int y, String label, java.awt.Component component, JPanel panel) {
		c.gridy = y; c.gridx = 0; panel.add(new JLabel(label), c);
		c.gridx = 1; panel.add(component, c);
	}

	private static JSpinner dateSpinner(Date value) {
		JSpinner spinner = new JSpinner(new SpinnerDateModel(value, null, null, Calendar.DAY_OF_MONTH));
		spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
		return spinner;
	}

	private static long dayValue(JSpinner spinner) {
		return DateTime.dayFloor(((Date) spinner.getValue()).getTime());
	}

	private static int isoDay(Calendar calendar) {
		int day = calendar.get(Calendar.DAY_OF_WEEK);
		return day == Calendar.SUNDAY ? 7 : day - 1;
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
