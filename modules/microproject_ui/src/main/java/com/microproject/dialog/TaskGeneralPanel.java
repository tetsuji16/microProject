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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.microproject.dialog.util.ComponentFactory;
import com.microproject.dialog.util.FieldComponentMap;
import com.microproject.graphic.configuration.GanttBarFormatOverrides.BarFormat;
import com.microproject.pm.graphic.gantt.BarColorEditorPanel;
import com.microproject.pm.graphic.gantt.Gantt;
import com.microproject.pm.graphic.gantt.GanttRenderer;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;

/** Owns the General tab layout and the editable Gantt bar-color controls. */
final class TaskGeneralPanel {
	private final JPanel component;
	private final BarColorEditorPanel barColorEditor;

	TaskGeneralPanel(Component owner, FieldComponentMap fields, Task task, BarFormat barFormat,
			GanttRenderer.DisplayedBarColors displayedColors) {
		component = new JPanel(new GridBagLayout());
		component.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 12, 10, 12));
		int row = 0;
		addField(fields, "Field.name", 0, row++, 0, 4);
		addField(fields, "Field.duration", ComponentFactory.SOMETIMES_READ_ONLY, row, 0, 2);
		addField(fields, "Field.estimated", 0, row++, 2, 2);
		addField(fields, "Field.percentComplete", ComponentFactory.SOMETIMES_READ_ONLY, row, 0, 2);
		addField(fields, "Field.priority", 0, row++, 2, 2);
		addField(fields, "Field.manuallyScheduled", 0, row, 0, 2);
		addField(fields, "Field.inactiveTask", 0, row++, 2, 2);
		addField(fields, "Field.hiddenTask", 0, row++, 0, 2);
		addField(fields, "Field.cost", 0, row, 0, 2);
		addField(fields, "Field.work", 0, row++, 2, 2);
		addSection(Messages.getString("TaskInformationDialog.Dates"), row++);
		addField(fields, "Field.start", 0, row, 0, 2);
		addField(fields, "Field.finish", 0, row++, 2, 2);
		addField(fields, "Field.baselineStart", 0, row, 0, 2);
		addField(fields, "Field.baselineFinish", 0, row++, 2, 2);
		addSection(Messages.getString("TaskInformationDialog.BarColor"), row++);
		barColorEditor = new BarColorEditorPanel(owner, barFormat, displayedColors,
				task.isMilestone(), task.isSummary(), null);
		GridBagConstraints constraints = constraints(0, row);
		constraints.gridwidth = 4;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		component.add(barColorEditor, constraints);
		barColorEditor.setEnabled(!task.isReadOnly());
	}

	JComponent component() {
		return component;
	}

	void refresh(BarFormat format, boolean readOnly) {
		refreshBarColorFields(barColorEditor, format, readOnly);
	}

	void apply(Task task, Gantt gantt) {
		if (task == null || task.isReadOnly() || gantt == null)
			return;
		gantt.applyBarFormat(task, new BarFormat(barColorEditor.getStart().getRgb(),
				barColorEditor.getMiddle().getRgb(), barColorEditor.getEnd().getRgb()));
	}

	static void refreshBarColorFields(BarColorEditorPanel editor, BarFormat format, boolean readOnly) {
		if (editor == null)
			return;
		BarFormat resolved = format == null ? BarFormat.automatic() : format;
		editor.setEnabled(!readOnly);
		editor.getStart().setRgb(resolved.getStartRgb());
		editor.getMiddle().setRgb(resolved.getMiddleRgb());
		editor.getEnd().setRgb(resolved.getEndRgb());
	}

	private void addField(FieldComponentMap fields, String fieldId, int flag, int row, int column, int width) {
		JComponent field = fields.getComponent(fieldId, flag);
		if (field instanceof JCheckBox) {
			GridBagConstraints constraints = constraints(column, row);
			constraints.gridwidth = width;
			constraints.anchor = GridBagConstraints.WEST;
			component.add(field, constraints);
			return;
		}
		GridBagConstraints label = constraints(column, row);
		label.anchor = GridBagConstraints.EAST;
		component.add(new JLabel(fields.getLabel(fieldId) + ":"), label);
		GridBagConstraints value = constraints(column + 1, row);
		value.gridwidth = Math.max(1, width - 1);
		value.weightx = 1.0;
		value.fill = GridBagConstraints.HORIZONTAL;
		component.add(field, value);
	}

	private void addSection(String text, int row) {
		GridBagConstraints label = constraints(0, row);
		label.gridwidth = 1;
		label.anchor = GridBagConstraints.WEST;
		component.add(new JLabel(text), label);
		GridBagConstraints line = constraints(1, row);
		line.gridwidth = 3;
		line.weightx = 1.0;
		line.fill = GridBagConstraints.HORIZONTAL;
		component.add(new JSeparator(), line);
	}

	private static GridBagConstraints constraints(int x, int y) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		// Preserve vertical breathing room so Japanese glyphs do not touch adjacent rows.
		constraints.insets = new Insets(6, 4, 6, 8);
		constraints.ipady = 2;
		return constraints;
	}
}
