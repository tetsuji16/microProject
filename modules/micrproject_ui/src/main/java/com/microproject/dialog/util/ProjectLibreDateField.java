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
package com.microproject.dialog.util;

import java.awt.BorderLayout;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.DateFormatter;

import com.microproject.options.EditOption;
import com.microproject.dialog.UsabilityStrings;
import com.microproject.util.DateFieldSupport;

/**
 * Lightweight date input component that keeps the old ProjectLibre API
 * surface without depending on NachoCalendar.
 */
public class ProjectLibreDateField extends JPanel {
	private final DateFormat dateFormat;
	private final JFormattedTextField textField;
	private final JButton popupButton;

	public ProjectLibreDateField() {
		this(EditOption.getInstance().getDateFormat());
	}

	public ProjectLibreDateField(boolean showWeekNumbers) {
		this();
	}

	public ProjectLibreDateField(DateFormatter formatter) {
		this(formatter != null && formatter.getFormat() instanceof DateFormat
			? (DateFormat) formatter.getFormat()
			: EditOption.getInstance().getDateFormat());
		if (formatter != null) {
			textField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(formatter));
		}
	}

	public ProjectLibreDateField(DateFormat dateFormat) {
		super(new BorderLayout(4, 0));
		this.dateFormat = dateFormat;
		this.textField = new JFormattedTextField(new DateFormatter(dateFormat));
		this.popupButton = new JButton("...");
		styleCalendarButton(popupButton);
		initialize();
	}

	public ProjectLibreDateField(Locale locale) {
		this(DateFormat.getDateInstance(DateFormat.SHORT, locale));
	}

	private void initialize() {
		textField.setColumns(10);
		textField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		textField.setOpaque(true);
		popupButton.setFocusable(false);
		popupButton.setMargin(new java.awt.Insets(0, 6, 0, 6));
		popupButton.addActionListener(e -> showPopup());
		add(textField, BorderLayout.CENTER);
		add(popupButton, BorderLayout.EAST);
		// The field must paint a solid background. When transparent the date digits
		// show whatever is behind the (modal) dialog -- e.g. the spreadsheet/Gantt --
		// which makes them look "hidden behind other characters". See issue reported
		// for the Task Information dialog date section.
		setOpaque(true);
	}

	private void showPopup() {
		JPopupMenu popup = new JPopupMenu();
		JPanel panel = createCalendarPanel(popup);
		popup.add(panel);
		popup.show(popupButton, 0, popupButton.getHeight());
	}

	/** Builds the month table used by the date chooser (issue #62). */
	JPanel createCalendarPanel(JPopupMenu popup) {
		Date initial = coerceDateValue();
		LocalDate selected = initial.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		JPanel panel = new JPanel(new BorderLayout(4, 4));
		JPanel header = new JPanel(new BorderLayout(2, 2));
		JButton previous = new JButton("‹");
		JButton next = new JButton("›");
		styleCalendarButton(previous);
		styleCalendarButton(next);
		String previousText = UsabilityStrings.text("common.previous");
		String nextText = UsabilityStrings.text("common.next");
		previous.setToolTipText(previousText);
		previous.getAccessibleContext().setAccessibleName(previousText);
		next.setToolTipText(nextText);
		next.getAccessibleContext().setAccessibleName(nextText);
		JLabel month = new JLabel("", JLabel.CENTER);
		header.add(previous, BorderLayout.WEST);
		header.add(month, BorderLayout.CENTER);
		header.add(next, BorderLayout.EAST);
		panel.add(header, BorderLayout.NORTH);
		JPanel days = new JPanel(new java.awt.GridLayout(7, 7, 1, 1));
		panel.add(days, BorderLayout.CENTER);
		YearMonth[] visible = { YearMonth.from(selected) };
		Runnable redraw = () -> {
			month.setText(visible[0].getYear() + "-" + String.format(Locale.ROOT, "%02d", visible[0].getMonthValue()));
			days.removeAll();
			String[] weekdays = new DateFormatSymbols(Locale.getDefault()).getShortWeekdays();
			DayOfWeek firstDay = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
			for (int column = 0; column < 7; column++) {
				DayOfWeek day = firstDay.plus(column);
				JLabel label = new JLabel(weekdays[day.getValue() == 7 ? 1 : day.getValue() + 1], JLabel.CENTER);
				label.setEnabled(false);
				days.add(label);
			}
			LocalDate first = visible[0].atDay(1);
			int offset = (first.getDayOfWeek().getValue() - firstDay.getValue() + 7) % 7;
			for (int i = 0; i < offset; i++) days.add(new JLabel());
			for (int day = 1; day <= visible[0].lengthOfMonth(); day++) {
				LocalDate date = visible[0].atDay(day);
				JButton button = new JButton(Integer.toString(day));
				styleCalendarButton(button);
				button.setMargin(new java.awt.Insets(1, 2, 1, 2));
				if (date.equals(selected)) button.putClientProperty("dateField.selected", Boolean.TRUE);
				button.addActionListener(event -> {
					setValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
					popup.setVisible(false);
					SwingUtilities.invokeLater(() -> textField.requestFocusInWindow());
				});
				days.add(button);
			}
			while (days.getComponentCount() < 49) days.add(new JLabel());
			days.revalidate();
			days.repaint();
		};
		previous.addActionListener(event -> { visible[0] = visible[0].minusMonths(1); redraw.run(); });
		next.addActionListener(event -> { visible[0] = visible[0].plusMonths(1); redraw.run(); });
		redraw.run();
		return panel;
	}

	private static void styleCalendarButton(JButton button) {
		button.putClientProperty("JButton.buttonType", "toolBarButton");
		button.setFocusable(false);
	}

	private Date coerceDateValue() {
		Object value = getValue();
		if (value instanceof Date date) {
			return date;
		}
		if (value instanceof String text) {
			try {
				return DateFieldSupport.parseYearless(text, dateFormat, null);
			} catch (ParseException ignored) {
				// Fall through to "today".
			}
		}
		return new Date();
	}

	public JFormattedTextField getFormattedTextField() {
		return textField;
	}

	public JTextField getTextField() {
		return textField;
	}

	public DateFormat getDateFormat() {
		return dateFormat;
	}

	public Object getValue() {
		Object value = textField.getValue();
		if (value == null) {
			String text = textField.getText();
			return text == null || text.isBlank() ? null : text;
		}
		return value;
	}

	public void setValue(Object value) {
		if (value instanceof String text) {
			textField.setText(text);
			textField.setValue(text);
		} else {
			textField.setValue(value);
		}
	}

	public Date getDateValue() {
		Object value = getValue();
		if (value instanceof Date date) {
			return date;
		}
		if (value instanceof String text && !text.isBlank()) {
			try {
				return DateFieldSupport.parseYearless(text, dateFormat, null);
			} catch (ParseException ignored) {
				return null;
			}
		}
		return null;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		textField.setEnabled(enabled);
		popupButton.setEnabled(enabled);
	}
}
