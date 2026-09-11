/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import java.awt.Frame;
import java.util.Date;

import javax.swing.JComponent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.ComponentFactory;
import com.microproject.dialog.util.ExtDateField;
import com.microproject.pm.task.Project;
import com.microproject.strings.Messages;

/** MSP Project &gt; Move Project date entry point. */
public final class MoveProjectDialog extends AbstractDialog {
	private static final long serialVersionUID = 1L;
	private final Project project;
	private ExtDateField startDate;

	public static MoveProjectDialog getInstance(Frame owner, Project project) {
		return new MoveProjectDialog(owner, project);
	}

	private MoveProjectDialog(Frame owner, Project project) {
		super(owner, Messages.getString("MoveProjectDialog.Title"), true);
		this.project = project;
	}

	@Override public JComponent createContentPanel() {
		startDate = ComponentFactory.createDateField();
		startDate.setValue(new Date(project.getStartDate()));
		FormLayout layout = new FormLayout("p, 3dlu, 120dlu:grow", "p");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.append(Messages.getString("MoveProjectDialog.NewStartDate"), startDate);
		return builder.getPanel();
	}

	public Date getSelectedStartDate() {
		return startDate.getDateValue();
	}

	@Override public void onOk() {
		if (getSelectedStartDate() != null) super.onOk();
	}
}
