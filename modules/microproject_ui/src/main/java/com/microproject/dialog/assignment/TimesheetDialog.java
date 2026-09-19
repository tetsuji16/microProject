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
package com.microproject.dialog.assignment;

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
import com.microproject.dialog.AbstractDialog;
import com.microproject.dialog.ButtonPanel;
import com.microproject.pm.graphic.IconManager;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.resource.Resource;
import com.microproject.strings.Messages;
import com.microproject.util.Alert;

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

	public TimesheetEntryPane getSpreadSheetPane() {
		return spreadSheetPane;
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
			resourceNames.setText(Messages.format("Format.labelValue",
					Messages.getString("Text.Resources"), Messages.getString("Text.EntireProject")));
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
		resourceNames.setText(Messages.format("Format.labelValue",
				Messages.getString("Text.Resources"), names));
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
