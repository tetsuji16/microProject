/*******************************************************************************
 * MIT License
 *
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
package com.microproject.dialog;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.association.AssociationList;
import com.microproject.help.HelpUtil;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;

/** Owns the Task Information Resources tab and its task-to-resource list. */
final class TaskResourcesPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private final SpreadSheet assignmentSpreadsheet;

	TaskResourcesPanel(Component owner, JComponent header, JButton assignResourceButton, Task task) {
		FormLayout layout = new FormLayout("p:grow,0dlu,right:p", "p,p,p,p,fill:150dlu:grow"); //$NON-NLS-1$ //$NON-NLS-2$
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints constraints = new CellConstraints();
		builder.add(header, constraints.xyw(builder.getColumn(), builder.getRow(), 3));
		builder.nextLine(2);
		builder.append(Messages.format("Format.label", Messages.getString("TaskInformationDialog.Resources")),
				assignResourceButton); //$NON-NLS-1$ //$NON-NLS-2$
		builder.nextLine(2);
		assignmentSpreadsheet = AssignmentSpreadsheetSupport.create(owner,
				AssignmentSpreadsheetSupport.Perspective.TASK_ASSIGNMENTS);
		builder.add(AssignmentSpreadsheetSupport.scrollPane(assignmentSpreadsheet),
				constraints.xyw(builder.getColumn(), builder.getRow(), 3));
		add(builder.getPanel());
		HelpUtil.addDocHelp(this, "Assign_Resources"); //$NON-NLS-1$
		update(task);
	}

	void update(Task task) {
		AssociationList assignments = task == null ? null : ((NormalTask) task).getAssignments();
		AssignmentSpreadsheetSupport.update(assignmentSpreadsheet, assignments,
				AssignmentSpreadsheetSupport.Perspective.TASK_ASSIGNMENTS);
	}

	void selectDocument(DocumentFrame document) {
		AssignmentSpreadsheetSupport.selectDocument(assignmentSpreadsheet, document,
				AssignmentSpreadsheetSupport.Perspective.TASK_ASSIGNMENTS);
	}
}
