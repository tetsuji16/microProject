/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.microproject.preference.GlobalPreferences;
import com.microproject.pm.graphic.gantt.MilestoneShapeChoice;
import com.microproject.ui.shell.AutoSaveControl;
import com.microproject.util.FlatLafSupport;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.FlatLafDialog;
import com.microproject.ui.util.PopupDialogSupport;

/** User-level settings which are independent of a project file. */
public final class PreferencesDialogBox extends FlatLafDialog {
	private static final long serialVersionUID = 1L;
	private boolean applied;

	public static void showDialog(Frame owner, GlobalPreferences preferences) {
		showDialog(owner, preferences, null, null, false);
	}

	/** Opens preferences and optionally exposes the application-wide locale action. */
	public static void showDialog(Frame owner, GlobalPreferences preferences, Runnable localeAction) {
		showDialog(owner, preferences, localeAction, null, false);
	}

	public static void showDialog(Frame owner, GlobalPreferences preferences, Runnable localeAction,
			AutoSaveControl autoSaveControl) {
		showDialog(owner, preferences, localeAction, autoSaveControl, false);
	}

	public static void showDialog(Frame owner, GlobalPreferences preferences, Runnable localeAction,
			AutoSaveControl autoSaveControl, boolean showLocaleTab) {
		PreferencesDialogBox dialog = new PreferencesDialogBox(owner, preferences, autoSaveControl, showLocaleTab);
		dialog.setVisible(true);
		if (dialog.applied && localeAction != null) localeAction.run();
	}

	private PreferencesDialogBox(Frame owner, GlobalPreferences preferences, AutoSaveControl autoSaveControl,
			boolean showLocaleTab) {
		super(owner, UsabilityStrings.text("preferences.title"), true);
		FlatUiSupport.styleDialogRoot(getRootPane());
		PopupDialogSupport.bindEscapeToDispose(this);
		JTextField userName = new JTextField(preferences.getUserName(), 24);
		JCheckBox rowLines = new JCheckBox(UsabilityStrings.text("preferences.rowLines"), preferences.isShowRowLines());
		JCheckBox checkUpdates = new JCheckBox(UsabilityStrings.text("preferences.checkUpdates"), preferences.isCheckForUpdates());
		String lightTheme = UsabilityStrings.text("preferences.themeLight");
		String darkTheme = UsabilityStrings.text("preferences.themeDark");
		JComboBox<String> theme = new JComboBox<>(new String[] { lightTheme, darkTheme });
		theme.setName("preferencesTheme");
		theme.setSelectedItem(preferences.isDarkTheme() ? darkTheme : lightTheme);
		JTextField datePattern = new JTextField(preferences.getDatePattern(), 16);
		datePattern.setName("preferencesDatePattern");
		datePattern.setToolTipText(UsabilityStrings.text("preferences.datePatternHelp"));
		JTextField dateTimePattern = new JTextField(preferences.getDateTimePattern(), 16);
		dateTimePattern.setName("preferencesDateTimePattern");
		dateTimePattern.setToolTipText(UsabilityStrings.text("preferences.datePatternHelp"));
		JTextField currencyCode = new JTextField(preferences.getCurrencyCode(), 8);
		currencyCode.setName("preferencesCurrencyCode");
		currencyCode.setToolTipText(UsabilityStrings.text("preferences.currencyCodeHelp"));
		JLabel formatRestartNotice = new JLabel(UsabilityStrings.text("preferences.dateFormatRestartRequired"));
		formatRestartNotice.setName("preferencesDateFormatRestartRequired");
		Runnable updateFormatRestartNotice = () -> formatRestartNotice.setVisible(
				!preferences.getDatePattern().equals(datePattern.getText().trim())
					|| !preferences.getDateTimePattern().equals(dateTimePattern.getText().trim())
					|| !preferences.getCurrencyCode().equals(currencyCode.getText().trim().toUpperCase(java.util.Locale.ROOT)));
		watchFormatPatternChanges(updateFormatRestartNotice, datePattern, dateTimePattern, currencyCode);
		updateFormatRestartNotice.run();
		JSpinner recoveryInterval = autoSaveControl == null ? null
				: new JSpinner(new SpinnerNumberModel(autoSaveControl.getIntervalMinutes(), 1, 1440, 1));
		if (recoveryInterval != null) recoveryInterval.setName("preferencesAutoSaveInterval");
		String[] fonts = java.awt.GraphicsEnvironment.isHeadless()
			? new String[] { "" }
			: java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		String systemFont = UsabilityStrings.text("preferences.fontAutomatic");
		JComboBox<String> font = new JComboBox<>(fontChoices(fonts, preferences.getFontFamily(), systemFont));
		font.setName("preferencesFontFamily");
		font.setSelectedItem(fontChoiceOf(preferences.getFontFamily(), systemFont));
		JSpinner size = new JSpinner(new FontSizeModel(preferences.getFontSize()));
		JSpinner.NumberEditor sizeEditor = new JSpinner.NumberEditor(size);
		sizeEditor.getTextField().setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(
			new FontSizeFormatter(UsabilityStrings.text("preferences.fontSizeAutomatic"))));
		size.setEditor(sizeEditor);
		String resourceNames = UsabilityStrings.text("preferences.ganttBarTextResourceNames");
		String taskName = UsabilityStrings.text("preferences.ganttBarTextTaskName");
		JComboBox<String> ganttBarText = new JComboBox<>(new String[] { resourceNames, taskName });
		ganttBarText.setSelectedItem(GlobalPreferences.GANTT_BAR_TEXT_TASK_NAME.equals(preferences.getDefaultGanttBarText())
				? taskName : resourceNames);
		String automaticPosition = UsabilityStrings.text("preferences.ganttBarTextPositionAutomatic");
		String rightPosition = UsabilityStrings.text("preferences.ganttBarTextPositionRight");
		String leftPosition = UsabilityStrings.text("preferences.ganttBarTextPositionLeft");
		JComboBox<String> ganttBarTextPosition = new JComboBox<>(new String[] { automaticPosition, rightPosition, leftPosition });
		String savedPosition = preferences.getDefaultGanttBarTextPosition();
		ganttBarTextPosition.setSelectedItem(GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT.equals(savedPosition) ? leftPosition
				: GlobalPreferences.GANTT_BAR_TEXT_POSITION_RIGHT.equals(savedPosition) ? rightPosition : automaticPosition);
		JComboBox<MilestoneShapeChoice> milestoneShape = new JComboBox<>(MilestoneShapeChoice.values());
		milestoneShape.setName("preferencesMilestoneShape");
		milestoneShape.setSelectedItem(MilestoneShapeChoice.forName(preferences.getDefaultMilestoneShape()));
		ColorChoice gridColor = new ColorChoice(this, UsabilityStrings.text("preferences.gridColor"),
				UsabilityStrings.text("preferences.gridColorAutomatic"), preferences.getGridLineColor());
		ColorChoice barColor = new ColorChoice(this, UsabilityStrings.text("preferences.ganttBarColor"),
				UsabilityStrings.text("preferences.ganttBarColorAutomatic"), preferences.getDefaultGanttBarColor());
		JPanel form = new JPanel(new GridBagLayout());
		form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
		addRow(form, UsabilityStrings.text("preferences.userName"), userName);
		addRow(form, UsabilityStrings.text("preferences.font"), font);
		addRow(form, UsabilityStrings.text("preferences.fontSize"), size);
		addRow(form, UsabilityStrings.text("preferences.ganttBarText"), ganttBarText);
		addRow(form, UsabilityStrings.text("preferences.ganttBarTextPosition"), ganttBarTextPosition);
		addRow(form, UsabilityStrings.text("preferences.milestoneShape"), milestoneShape);
		addRow(form, UsabilityStrings.text("preferences.ganttBarColor"), barColor.controls());
		addRow(form, UsabilityStrings.text("preferences.gridColor"), gridColor.controls());
		addRow(form, null, rowLines);
		addRow(form, null, checkUpdates);
		addRow(form, UsabilityStrings.text("preferences.theme"), theme);
		addRow(form, UsabilityStrings.text("preferences.datePattern"), datePattern);
		addRow(form, UsabilityStrings.text("preferences.dateTimePattern"), dateTimePattern);
		addRow(form, UsabilityStrings.text("preferences.currencyCode"), currencyCode);
		addRow(form, null, formatRestartNotice);
		if (recoveryInterval != null)
			addRow(form, UsabilityStrings.text("preferences.autoSaveInterval"), recoveryInterval);
		// Absorb spare height so rows keep their natural size inside the scroll pane.
		GridBagConstraints filler = new GridBagConstraints();
		filler.gridx = 0; filler.gridy = GridBagConstraints.RELATIVE; filler.gridwidth = 2; filler.weighty = 1;
		form.add(new JPanel(), filler);
		LocaleDialog localeSettings = LocaleDialog.createPreferencesSection(owner);
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab(UsabilityStrings.text("preferences.general"), new JScrollPane(form));
		tabs.addTab(UsabilityStrings.text("preferences.languageLocale"),
			new JScrollPane(localeSettings.createPreferencesContent()));
		tabs.setSelectedIndex(showLocaleTab ? 1 : 0);

		JButton apply = new JButton(UsabilityStrings.text("preferences.apply"));
		apply.addActionListener(event -> {
			if (!GlobalPreferences.isValidDatePattern(datePattern.getText())
					|| !GlobalPreferences.isValidDatePattern(dateTimePattern.getText())
					|| !GlobalPreferences.isValidCurrencyCode(currencyCode.getText())) {
				JOptionPane.showMessageDialog(this, UsabilityStrings.text("preferences.invalidFormat"),
					UsabilityStrings.text("preferences.title"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (!localeSettings.savePreferencesContent()) return;
			applied = true;
			preferences.setUserName(userName.getText());
			preferences.setShowRowLines(rowLines.isSelected());
			String previousFontFamily = preferences.getFontFamily();
			int previousFontSize = preferences.getFontSize();
			preferences.setFontFamily(fontFamilyOf(font.getSelectedItem(), systemFont));
			preferences.setFontSize(((Number) size.getValue()).intValue());
			boolean fontChanged = !preferences.getFontFamily().equals(previousFontFamily)
					|| preferences.getFontSize() != previousFontSize;
			preferences.setDefaultGanttBarText(taskName.equals(ganttBarText.getSelectedItem())
					? GlobalPreferences.GANTT_BAR_TEXT_TASK_NAME : GlobalPreferences.GANTT_BAR_TEXT_RESOURCE_NAMES);
			preferences.setDefaultGanttBarTextPosition(leftPosition.equals(ganttBarTextPosition.getSelectedItem())
					? GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT
					: rightPosition.equals(ganttBarTextPosition.getSelectedItem())
						? GlobalPreferences.GANTT_BAR_TEXT_POSITION_RIGHT : GlobalPreferences.GANTT_BAR_TEXT_POSITION_AUTO);
			preferences.setDefaultMilestoneShape(((MilestoneShapeChoice)milestoneShape.getSelectedItem()).getShapeName());
			preferences.setGridLineColor(gridColor.rgb());
			preferences.setDefaultGanttBarColor(barColor.rgb());
			preferences.setCheckForUpdates(checkUpdates.isSelected());
			boolean themeChanged = preferences.isDarkTheme() != darkTheme.equals(theme.getSelectedItem());
			preferences.setDarkTheme(darkTheme.equals(theme.getSelectedItem()));
			preferences.setDatePattern(datePattern.getText());
			preferences.setDateTimePattern(dateTimePattern.getText());
			preferences.setCurrencyCode(currencyCode.getText());
			if (recoveryInterval != null)
				autoSaveControl.setIntervalMinutes(((Number) recoveryInterval.getValue()).intValue());
			dispose();
			// Typography is applied once, after the modal dialog is gone, and only
			// when it changed. A theme switch below refreshes every window itself.
			if (fontChanged) {
				FlatLafSupport.applyUserFontPreference(preferences.getFontFamily(), preferences.getFontSize());
				if (!themeChanged)
					FlatUiSupport.updateAllWindowTrees();
			}
			// The theme applies live to every open window; no restart is needed.
			if (themeChanged)
				com.microproject.ui.theme.ThemeManager.applyTheme(preferences.isDarkTheme());
		});
		JButton cancel = new JButton(UsabilityStrings.text("preferences.cancel"));
		getRootPane().setDefaultButton(apply); // Enter presses the primary command (Windows/Office dialog rule)
		cancel.addActionListener(event -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(cancel); buttons.add(apply);
		setLayout(new BorderLayout());
		add(tabs, BorderLayout.CENTER); add(buttons, BorderLayout.SOUTH);
		pack();
		Rectangle usableBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		Dimension packedSize = getSize();
		setSize(Math.min(packedSize.width, usableBounds.width), Math.min(packedSize.height, usableBounds.height));
		setLocationRelativeTo(owner);
	}

	private static void addRow(JPanel form, String label, java.awt.Component field) {
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0; labelConstraints.gridy = GridBagConstraints.RELATIVE;
		labelConstraints.anchor = GridBagConstraints.WEST;
		labelConstraints.insets = new Insets(4, 0, 4, 8);
		form.add(label == null ? new JLabel() : new JLabel(label), labelConstraints);
		GridBagConstraints fieldConstraints = new GridBagConstraints();
		fieldConstraints.gridx = 1; fieldConstraints.gridy = GridBagConstraints.RELATIVE;
		fieldConstraints.weightx = 1; fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		fieldConstraints.anchor = GridBagConstraints.WEST;
		fieldConstraints.insets = new Insets(4, 0, 4, 0);
		form.add(field, fieldConstraints);
	}

	/** A button showing the chosen colour (or an automatic label) together with a reset button. */
	private static final class ColorChoice {
		private final JButton choose;
		private final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		private final String automaticText;
		private Color selected;

		ColorChoice(java.awt.Component parent, String chooserTitle, String automaticText, Integer saved) {
			this.automaticText = automaticText;
			selected = saved == null ? null : new Color(saved.intValue());
			choose = new JButton(automaticText);
			JButton reset = new JButton(UsabilityStrings.text("preferences.reset"));
			refresh();
			choose.addActionListener(event -> {
				Color chosen = JColorChooser.showDialog(parent, chooserTitle, selected);
				if (chosen != null) { selected = chosen; refresh(); }
			});
			reset.addActionListener(event -> { selected = null; refresh(); });
			controls.add(choose);
			controls.add(reset);
		}

		JPanel controls() { return controls; }

		/** Returns the selected RGB value, or null when the automatic colour is kept. */
		Integer rgb() { return selected == null ? null : Integer.valueOf(selected.getRGB()); }

		private void refresh() {
			choose.setBackground(selected);
			choose.setText(selected == null ? automaticText : String.format("#%06X", selected.getRGB() & 0x00ffffff));
		}
	}

	/**
	 * Font list shown to the user: the system-default entry first, then the
	 * installed families, then a stored family that is no longer installed so
	 * that opening and applying the dialog never replaces it silently.
	 */
	static String[] fontChoices(String[] families, String storedFamily, String systemDefaultLabel) {
		java.util.LinkedHashSet<String> choices = new java.util.LinkedHashSet<>();
		choices.add(systemDefaultLabel);
		if (families != null) {
			for (String family : families) {
				if (family != null && !family.isBlank())
					choices.add(family);
			}
		}
		if (storedFamily != null && !storedFamily.isBlank())
			choices.add(storedFamily.trim());
		return choices.toArray(new String[0]);
	}

	/** Combo entry that represents a stored family; empty means system default. */
	static String fontChoiceOf(String storedFamily, String systemDefaultLabel) {
		return storedFamily == null || storedFamily.isBlank() ? systemDefaultLabel : storedFamily.trim();
	}

	/** Stored family for a combo entry; the system-default entry is stored as empty. */
	static String fontFamilyOf(Object selected, String systemDefaultLabel) {
		if (selected == null || systemDefaultLabel.equals(selected.toString()))
			return "";
		return selected.toString().trim();
	}

	/** Font size 0 means automatic; sizes 1-7 are not usable and snap to {@link #MIN_FONT_SIZE}. */
	static final class FontSizeModel extends SpinnerNumberModel {
		private static final long serialVersionUID = 1L;
		static final int MIN_FONT_SIZE = 8;

		FontSizeModel(int value) {
			super(snap(value), 0, 32, 1);
		}

		static int snap(int value) {
			return value <= 0 ? 0 : Math.max(MIN_FONT_SIZE, Math.min(32, value));
		}

		@Override
		public void setValue(Object value) {
			super.setValue(value instanceof Number number ? Integer.valueOf(snap(number.intValue())) : value);
		}

		@Override
		public Object getNextValue() {
			int current = ((Number) getValue()).intValue();
			return current >= 32 ? null : Integer.valueOf(current == 0 ? MIN_FONT_SIZE : current + 1);
		}

		@Override
		public Object getPreviousValue() {
			int current = ((Number) getValue()).intValue();
			return current <= 0 ? null : Integer.valueOf(current <= MIN_FONT_SIZE ? 0 : current - 1);
		}
	}

	/** Shows font size 0 as the localized "Auto" label. */
	private static final class FontSizeFormatter extends javax.swing.JFormattedTextField.AbstractFormatter {
		private static final long serialVersionUID = 1L;
		private final String automaticText;

		FontSizeFormatter(String automaticText) {
			this.automaticText = automaticText;
		}

		@Override
		public Object stringToValue(String text) throws java.text.ParseException {
			String trimmed = text == null ? "" : text.trim();
			if (trimmed.isEmpty() || trimmed.equalsIgnoreCase(automaticText))
				return Integer.valueOf(0);
			try {
				return Integer.valueOf(FontSizeModel.snap(Integer.parseInt(trimmed)));
			} catch (NumberFormatException ex) {
				throw new java.text.ParseException(trimmed, 0);
			}
		}

		@Override
		public String valueToString(Object value) {
			int size = value instanceof Number number ? number.intValue() : 0;
			return size <= 0 ? automaticText : Integer.toString(size);
		}
	}

	private static void watchFormatPatternChanges(Runnable refresh, JTextField... fields) {
		javax.swing.event.DocumentListener listener = new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent event) { refresh.run(); }
			public void removeUpdate(javax.swing.event.DocumentEvent event) { refresh.run(); }
			public void changedUpdate(javax.swing.event.DocumentEvent event) { refresh.run(); }
		};
		for (JTextField field : fields)
			field.getDocument().addDocumentListener(listener);
	}
}
