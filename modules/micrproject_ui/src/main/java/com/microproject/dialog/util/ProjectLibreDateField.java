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
import java.awt.Dimension;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.text.DateFormatter;

import com.microproject.options.EditOption;
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
		JSpinner spinner = new JSpinner(new SpinnerDateModel());
		spinner.setPreferredSize(new Dimension(140, spinner.getPreferredSize().height));
		spinner.setValue(coerceDateValue());
		if (dateFormat instanceof SimpleDateFormat sdf) {
			spinner.setEditor(new JSpinner.DateEditor(spinner, sdf.toPattern()));
		} else {
			spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy/MM/dd"));
		}
		JButton accept = new JButton("OK");
		accept.addActionListener(e -> {
			setValue(spinner.getValue());
			popup.setVisible(false);
			SwingUtilities.invokeLater(() -> textField.requestFocusInWindow());
		});
		JPanel panel = new JPanel(new BorderLayout(4, 4));
		panel.add(spinner, BorderLayout.CENTER);
		panel.add(accept, BorderLayout.SOUTH);
		popup.add(panel);
		popup.show(popupButton, 0, popupButton.getHeight());
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
