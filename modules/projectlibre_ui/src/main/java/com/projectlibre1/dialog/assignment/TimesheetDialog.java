/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License
 * Version 1.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of
 * software over a computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be consistent
 * with Exhibit B.
 *******************************************************************************/
package com.projectlibre1.dialog.assignment;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.projectlibre1.dialog.AbstractDialog;
import com.projectlibre1.dialog.ButtonPanel;
import com.projectlibre1.pm.graphic.IconManager;
import com.projectlibre1.pm.graphic.frames.DocumentFrame;
import com.projectlibre1.pm.resource.Resource;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.Alert;

public class TimesheetDialog extends AbstractDialog implements CommonAssignmentDialog {
	private static final long serialVersionUID = 1L;

	private final DocumentFrame documentFrame;
	private final List<Resource> selectedResources;
	private TimesheetEntryPane spreadSheetPane;
	private JLabel resourceNames;
	private JButton applyButton;
	private JButton stopEditorButton;
	private JButton cancelEditorButton;

	public TimesheetDialog(DocumentFrame documentFrame, List<Resource> selectedResources) {
		super(documentFrame.getGraphicManager().getFrame(), Messages.getString("Text.Timesheet"), false);
		this.documentFrame = documentFrame;
		this.selectedResources = selectedResources;
	}

	protected void initControls() {
		spreadSheetPane = new TimesheetEntryPane(this, documentFrame.getProject());
		resourceNames = new JLabel();
		spreadSheetPane.setSelectedResources(selectedResources);
		updateSelectionLabel();

		applyButton = new JButton(new AbstractAction(Messages.getString("ButtonText.Apply")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				applyTimesheet();
			}
		});
		stopEditorButton = new JButton(new AbstractAction(null, IconManager.getIcon("dialog.ok")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				if (spreadSheetPane.getSpreadSheet().getCellEditor() != null) {
					spreadSheetPane.getSpreadSheet().getCellEditor().stopCellEditing();
				}
			}
		});
		cancelEditorButton = new JButton(new AbstractAction(null, IconManager.getIcon("dialog.cancel")) {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				if (spreadSheetPane.getSpreadSheet().getCellEditor() != null) {
					spreadSheetPane.getSpreadSheet().getCellEditor().cancelCellEditing();
				}
				spreadSheetPane.getSpreadSheet().clearSelection();
			}
		});
		setEditorButtonsVisible(false);
	}

	private void updateSelectionLabel() {
		if (selectedResources == null || selectedResources.isEmpty()) {
			resourceNames.setText(Messages.getString("Text.Resources") + ": " + Messages.getString("Text.EntireProject"));
			return;
		}
		StringBuilder names = new StringBuilder();
		int index = 0;
		for (Resource resource : selectedResources) {
			if (index >= 5) {
				break;
			}
			if (index > 0) {
				names.append(", ");
			}
			names.append(resource);
			index++;
		}
		if (selectedResources.size() > 5) {
			names.append("...");
		}
		resourceNames.setText(Messages.getString("Text.Resources") + ": " + names.toString());
	}

	public void setEditorButtonsVisible(boolean visible) {
		stopEditorButton.setEnabled(visible);
		cancelEditorButton.setEnabled(visible);
	}

	public JComponent createContentPanel() {
		initControls();
		FormLayout layout = new FormLayout("fill:320dlu:grow, 3dlu, p", "p, 3dlu, fill:220dlu:grow");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.add(resourceNames, cc.xyw(builder.getColumn(), builder.getRow(), 3));
		builder.nextLine(2);
		builder.append(spreadSheetPane, createEditorsButtons());
		return builder.getPanel();
	}

	private JComponent createEditorsButtons() {
		FormLayout layout = new FormLayout("20px", "20dlu,20px, 3dlu, 20px");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.nextLine(1);
		builder.append(stopEditorButton);
		builder.nextLine(2);
		builder.append(cancelEditorButton);
		return builder.getPanel();
	}

	public ButtonPanel createButtonPanel() {
		createOkCancelButtons();
		ButtonPanel buttonPanel = new ButtonPanel();
		buttonPanel.addButton(applyButton);
		buttonPanel.addButton(ok);
		buttonPanel.addButton(cancel);
		if (hasHelpButton()) {
			buttonPanel.add(getHelpButton());
		}
		return buttonPanel;
	}

	public void onOk() {
		if (applyTimesheet()) {
			setDialogResult(JOptionPane.OK_OPTION);
			setVisible(false);
		}
	}

	private boolean applyTimesheet() {
		if (!spreadSheetPane.hasRows()) {
			Alert.warn(Messages.getString("Message.noTimesheetAssignments"), this);
			return false;
		}
		spreadSheetPane.applyTimesheet();
		return true;
	}
}
