package com.projectlibre1.dialog.util;

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

import com.projectlibre1.options.EditOption;
import com.projectlibre1.util.DateFieldSupport;

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
		popupButton.setFocusable(false);
		popupButton.setMargin(new java.awt.Insets(0, 6, 0, 6));
		popupButton.addActionListener(e -> showPopup());
		add(textField, BorderLayout.CENTER);
		add(popupButton, BorderLayout.EAST);
		setOpaque(false);
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
