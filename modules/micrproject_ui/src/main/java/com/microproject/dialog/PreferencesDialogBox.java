/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.microproject.preference.GlobalPreferences;
import com.microproject.util.FlatUiSupport;
import com.microproject.util.PopupDialogSupport;

/** User-level settings which are independent of a project file. */
public final class PreferencesDialogBox extends JDialog {
	private static final long serialVersionUID = 1L;

	public static void showDialog(Frame owner, GlobalPreferences preferences) {
		PreferencesDialogBox dialog = new PreferencesDialogBox(owner, preferences);
		dialog.setVisible(true);
	}

	private PreferencesDialogBox(Frame owner, GlobalPreferences preferences) {
		super(owner, UsabilityStrings.text("preferences.title"), true);
		FlatUiSupport.styleDialogRoot(getRootPane());
		PopupDialogSupport.bindEscapeToDispose(this);
		JTextField userName = new JTextField(preferences.getUserName(), 24);
		JCheckBox rowLines = new JCheckBox(UsabilityStrings.text("preferences.rowLines"), preferences.isShowRowLines());
		JCheckBox taskRowDragAndDrop = new JCheckBox(UsabilityStrings.text("preferences.taskRowDragAndDrop"), preferences.isTaskRowDragAndDropEnabled());
		String[] fonts = java.awt.GraphicsEnvironment.isHeadless()
			? new String[] { "" }
			: java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		JComboBox<String> font = new JComboBox<>(fonts);
		font.setSelectedItem(preferences.getFontFamily());
		JSpinner size = new JSpinner(new SpinnerNumberModel(preferences.getFontSize(), 0, 32, 1));

		JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
		form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
		form.add(new JLabel(UsabilityStrings.text("preferences.userName"))); form.add(userName);
		form.add(new JLabel(UsabilityStrings.text("preferences.font"))); form.add(font);
		form.add(new JLabel(UsabilityStrings.text("preferences.fontSize"))); form.add(size);
		form.add(new JLabel()); form.add(rowLines);
		form.add(new JLabel()); form.add(taskRowDragAndDrop);

		JButton apply = new JButton(UsabilityStrings.text("preferences.apply"));
		apply.addActionListener(event -> {
			preferences.setUserName(userName.getText());
			preferences.setShowRowLines(rowLines.isSelected());
			preferences.setTaskRowDragAndDropEnabled(taskRowDragAndDrop.isSelected());
			Object selectedFont = font.getSelectedItem();
			preferences.setFontFamily(selectedFont == null ? "" : selectedFont.toString());
			preferences.setFontSize(((Number) size.getValue()).intValue());
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
}
