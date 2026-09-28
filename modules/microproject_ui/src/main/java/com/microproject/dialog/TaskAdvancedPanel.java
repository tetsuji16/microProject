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

import javax.swing.JComponent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.FieldComponentMap;
import com.microproject.strings.Messages;

/** Owns the Advanced tab's task-specific field layout. */
final class TaskAdvancedPanel {
	private final JComponent component;

	TaskAdvancedPanel(FieldComponentMap fields, JComponent nameField) {
		FormLayout layout = new FormLayout(
				"max(50dlu;pref), 3dlu, max(90dlu;pref), 10dlu, p, 3dlu,max(90dlu;pref),30dlu",
				"max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),max(24dlu;pref),fill:50dlu:grow");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		CellConstraints constraints = new CellConstraints();

		builder.add(nameField, constraints.xyw(builder.getColumn(), builder.getRow(), 8));
		builder.nextLine(2);
		fields.append(builder, "Field.wbs");
		fields.append(builder, "Field.markTaskAsMilestone", 3);
		builder.nextLine(2);
		builder.addSeparator(Messages.getString("TaskInformationDialog.ConstrainTask"));
		// addSeparator advances once; step past the spacer to the content row.
		builder.nextLine();
		fields.append(builder, "Field.constraintType");
		fields.appendSometimesReadOnly(builder, "Field.constraintDate");
		builder.nextLine(2);
		fields.append(builder, "Field.deadline");
		builder.nextLine(4);
		builder.addSeparator("\t");
		builder.nextLine();
		fields.append(builder, "Field.taskType");
		fields.append(builder, "Field.effortDriven", 3);
		builder.nextLine(2);
		fields.append(builder, "Field.taskCalendar");
		fields.append(builder, "Field.ignoreResourceCalendar", 3);
		builder.nextLine(2);
		fields.append(builder, "Field.earnedValueMethod");
		component = builder.getPanel();
	}

	JComponent component() {
		return component;
	}
}
