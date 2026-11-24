/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.dialog;

import java.awt.Frame;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.ui.dialog.util.ComponentFactory;
import com.microproject.ui.dialog.util.ExtDateField;
import com.microproject.model.task.Project;
import com.microproject.i18n.Messages;

/** The MSP Project &gt; Status Date editor, including the documented NA value. */
public final class StatusDateDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	private final Project project;
	private ExtDateField dateField;
	private JCheckBox notSet;

	public static StatusDateDialog getInstance(Frame owner, Project project) {
		return new StatusDateDialog(owner, project);
	}

	private StatusDateDialog(Frame owner, Project project) {
		super(owner, Messages.getString("StatusDateDialog.Title"), true);
		this.project = project;
	}

	/** The status-date editor is one bounded form; #724 keeps it at its packed size. */
	@Override
	protected boolean isFixedSizeForm() {
		return true;
	}

	@Override public JComponent createContentPanel() {
		dateField = ComponentFactory.createDateField();
		if (project.isStatusDateSet()) dateField.setValue(new Date(project.getStatusDate()));
		notSet = new JCheckBox(Messages.getString("StatusDateDialog.NotSet"), !project.isStatusDateSet());
		notSet.addActionListener(event -> dateField.setEnabled(!notSet.isSelected()));
		dateField.setEnabled(!notSet.isSelected());

		FormLayout layout = new FormLayout("p, 3dlu, 120dlu:grow", "p, 3dlu, p");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(Messages.getString("StatusDateDialog.Date"), dateField);
		builder.nextLine(2);
		builder.append(notSet);
		return builder.getPanel();
	}

	public boolean isNotSet() {
		return notSet != null && notSet.isSelected();
	}

	public Date getSelectedStatusDate() {
		return dateField == null ? null : dateField.getDateValue();
	}

	@Override public void onOk() {
		if (isNotSet() || getSelectedStatusDate() != null) super.onOk();
	}
}
