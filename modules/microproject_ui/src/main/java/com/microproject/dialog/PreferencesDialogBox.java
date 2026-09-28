/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.microproject.preference.GlobalPreferences;
import com.microproject.pm.graphic.gantt.MilestoneShapeChoice;
import com.microproject.ui.shell.AutoSaveControl;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.FlatLafDialog;
import com.microproject.ui.util.PopupDialogSupport;

/** User-level settings which are independent of a project file. */
public final class PreferencesDialogBox extends FlatLafDialog {
	private static final long serialVersionUID = 1L;

	public static void showDialog(Frame owner, GlobalPreferences preferences) {
		showDialog(owner, preferences, null);
	}

	/** Opens preferences and optionally exposes the application-wide locale action. */
	public static void showDialog(Frame owner, GlobalPreferences preferences, Runnable localeAction) {
		PreferencesDialogBox dialog = new PreferencesDialogBox(owner, preferences, localeAction, null);
		dialog.setVisible(true);
	}

	public static void showDialog(Frame owner, GlobalPreferences preferences, Runnable localeAction,
			AutoSaveControl autoSaveControl) {
		PreferencesDialogBox dialog = new PreferencesDialogBox(owner, preferences, localeAction, autoSaveControl);
		dialog.setVisible(true);
	}

	private PreferencesDialogBox(Frame owner, GlobalPreferences preferences, Runnable localeAction,
			AutoSaveControl autoSaveControl) {
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
		JLabel themeRestartNotice = new JLabel(UsabilityStrings.text("preferences.themeRestartRequired"));
		themeRestartNotice.setName("preferencesThemeRestartNotice");
		Runnable updateThemeRestartNotice = () -> themeRestartNotice.setVisible(
				preferences.isDarkTheme() != darkTheme.equals(theme.getSelectedItem()));
		theme.addActionListener(event -> updateThemeRestartNotice.run());
		updateThemeRestartNotice.run();
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
		JComboBox<String> font = new JComboBox<>(fonts);
		font.setSelectedItem(preferences.getFontFamily());
		JSpinner size = new JSpinner(new SpinnerNumberModel(preferences.getFontSize(), 0, 32, 1));
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
		JButton gridColor = new JButton(UsabilityStrings.text("preferences.gridColorAutomatic"));
		Integer savedGridColor = preferences.getGridLineColor();
		final Color[] selectedGridColor = { savedGridColor == null ? null : new Color(savedGridColor.intValue()) };
		updateGridColorButton(gridColor, selectedGridColor[0]);
		gridColor.addActionListener(event -> {
			Color selected = JColorChooser.showDialog(this, UsabilityStrings.text("preferences.gridColor"), selectedGridColor[0]);
			if (selected != null) { selectedGridColor[0] = selected; updateGridColorButton(gridColor, selected); }
		});
		JButton resetGridColor = new JButton(UsabilityStrings.text("preferences.reset"));
		resetGridColor.addActionListener(event -> { selectedGridColor[0] = null; updateGridColorButton(gridColor, null); });
		JButton defaultBarColor = new JButton(UsabilityStrings.text("preferences.ganttBarColorAutomatic"));
		Integer savedBarColor = preferences.getDefaultGanttBarColor();
		final Color[] selectedBarColor = { savedBarColor == null ? null : new Color(savedBarColor.intValue()) };
		updateDefaultBarColorButton(defaultBarColor, selectedBarColor[0]);
		defaultBarColor.addActionListener(event -> {
			Color selected = JColorChooser.showDialog(this, UsabilityStrings.text("preferences.ganttBarColor"), selectedBarColor[0]);
			if (selected != null) { selectedBarColor[0] = selected; updateDefaultBarColorButton(defaultBarColor, selected); }
		});
		JButton resetBarColor = new JButton(UsabilityStrings.text("preferences.reset"));
		resetBarColor.addActionListener(event -> { selectedBarColor[0] = null; updateDefaultBarColorButton(defaultBarColor, null); });

		JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
		form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
		form.add(new JLabel(UsabilityStrings.text("preferences.userName"))); form.add(userName);
		form.add(new JLabel(UsabilityStrings.text("preferences.font"))); form.add(font);
		form.add(new JLabel(UsabilityStrings.text("preferences.fontSize"))); form.add(size);
		form.add(new JLabel(UsabilityStrings.text("preferences.ganttBarText"))); form.add(ganttBarText);
		form.add(new JLabel(UsabilityStrings.text("preferences.ganttBarTextPosition"))); form.add(ganttBarTextPosition);
		form.add(new JLabel(UsabilityStrings.text("preferences.milestoneShape"))); form.add(milestoneShape);
		form.add(new JLabel(UsabilityStrings.text("preferences.ganttBarColor")));
		JPanel barColorControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); barColorControls.add(defaultBarColor); barColorControls.add(resetBarColor); form.add(barColorControls);
		form.add(new JLabel(UsabilityStrings.text("preferences.gridColor")));
		JPanel gridColorControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); gridColorControls.add(gridColor); gridColorControls.add(resetGridColor); form.add(gridColorControls);
		form.add(new JLabel()); form.add(rowLines);
		form.add(new JLabel()); form.add(checkUpdates);
		form.add(new JLabel(UsabilityStrings.text("preferences.theme"))); form.add(theme);
		form.add(new JLabel()); form.add(themeRestartNotice);
		form.add(new JLabel(UsabilityStrings.text("preferences.datePattern"))); form.add(datePattern);
		form.add(new JLabel(UsabilityStrings.text("preferences.dateTimePattern"))); form.add(dateTimePattern);
		form.add(new JLabel(UsabilityStrings.text("preferences.currencyCode"))); form.add(currencyCode);
		form.add(new JLabel()); form.add(formatRestartNotice);
		if (recoveryInterval != null) {
			form.add(new JLabel(UsabilityStrings.text("preferences.autoSaveInterval"))); form.add(recoveryInterval);
		}
		if (localeAction != null) {
			JButton locale = new JButton(UsabilityStrings.text("preferences.locale"));
			locale.addActionListener(event -> { dispose(); localeAction.run(); });
			form.add(new JLabel()); form.add(locale);
		}

		JButton apply = new JButton(UsabilityStrings.text("preferences.apply"));
		apply.addActionListener(event -> {
			if (!GlobalPreferences.isValidDatePattern(datePattern.getText())
					|| !GlobalPreferences.isValidDatePattern(dateTimePattern.getText())
					|| !GlobalPreferences.isValidCurrencyCode(currencyCode.getText())) {
				JOptionPane.showMessageDialog(this, UsabilityStrings.text("preferences.invalidFormat"),
					UsabilityStrings.text("preferences.title"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			preferences.setUserName(userName.getText());
			preferences.setShowRowLines(rowLines.isSelected());
			Object selectedFont = font.getSelectedItem();
			preferences.setFontFamily(selectedFont == null ? "" : selectedFont.toString());
			preferences.setFontSize(((Number) size.getValue()).intValue());
			preferences.setDefaultGanttBarText(taskName.equals(ganttBarText.getSelectedItem())
					? GlobalPreferences.GANTT_BAR_TEXT_TASK_NAME : GlobalPreferences.GANTT_BAR_TEXT_RESOURCE_NAMES);
			preferences.setDefaultGanttBarTextPosition(leftPosition.equals(ganttBarTextPosition.getSelectedItem())
					? GlobalPreferences.GANTT_BAR_TEXT_POSITION_LEFT
					: rightPosition.equals(ganttBarTextPosition.getSelectedItem())
						? GlobalPreferences.GANTT_BAR_TEXT_POSITION_RIGHT : GlobalPreferences.GANTT_BAR_TEXT_POSITION_AUTO);
			preferences.setDefaultMilestoneShape(((MilestoneShapeChoice)milestoneShape.getSelectedItem()).getShapeName());
			preferences.setGridLineColor(selectedGridColor[0] == null ? null : Integer.valueOf(selectedGridColor[0].getRGB()));
			preferences.setDefaultGanttBarColor(selectedBarColor[0] == null ? null : Integer.valueOf(selectedBarColor[0].getRGB()));
			preferences.setCheckForUpdates(checkUpdates.isSelected());
			preferences.setDarkTheme(darkTheme.equals(theme.getSelectedItem()));
			preferences.setDatePattern(datePattern.getText());
			preferences.setDateTimePattern(dateTimePattern.getText());
			preferences.setCurrencyCode(currencyCode.getText());
			if (recoveryInterval != null)
				autoSaveControl.setIntervalMinutes(((Number) recoveryInterval.getValue()).intValue());
			dispose();
		});
		JButton cancel = new JButton(UsabilityStrings.text("preferences.cancel"));
		cancel.addActionListener(event -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(cancel); buttons.add(apply);
		setLayout(new BorderLayout());
		add(form, BorderLayout.CENTER); add(buttons, BorderLayout.SOUTH);
		pack(); setLocationRelativeTo(owner);
	}

	private static void updateGridColorButton(JButton button, Color color) {
		button.setBackground(color);
		button.setText(color == null ? UsabilityStrings.text("preferences.gridColorAutomatic") : String.format("#%06X", color.getRGB() & 0x00ffffff));
	}

	private static void updateDefaultBarColorButton(JButton button, Color color) {
		button.setBackground(color);
		button.setText(color == null ? UsabilityStrings.text("preferences.ganttBarColorAutomatic") : String.format("#%06X", color.getRGB() & 0x00ffffff));
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
