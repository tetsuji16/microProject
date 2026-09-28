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

import javax.swing.JComponent;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.microproject.dialog.util.FieldComponentMap;
import com.microproject.pm.graphic.gantt.BarColorField;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;

/** Owns task text-style controls and their task-specific font-color state. */
final class TaskTextStylePanel {
	private final JComponent component;
	private final BarColorField fontColorField;

	TaskTextStylePanel(Component owner, FieldComponentMap fields, Task task) {
		FormLayout layout = new FormLayout("p,3dlu,130dlu,12dlu,p,3dlu,80dlu",
				"max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref),max(30dlu;pref)");
		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.addSeparator(Messages.getString("TaskInformationDialog.TextStyle"));
		// addSeparator advances over its separator row; skip the following spacer.
		builder.nextLine();
		fields.append(builder, "Field.fontFamily");
		fields.append(builder, "Field.fontSize");
		builder.nextLine(2);
		fields.append(builder, "Field.fontBold");
		fields.append(builder, "Field.fontItalic");
		builder.nextLine(2);
		fields.append(builder, "Field.fontStrikethrough");
		builder.nextLine(2);
		fontColorField = new BarColorField(owner, task == null ? null : task.getFontColor(), 0x000000,
				"TaskInformationDialog.FontColor", null);
		builder.append(Messages.getString("TaskInformationDialog.FontColor"), fontColorField);
		component = builder.getPanel();
		refresh(task);
	}

	JComponent component() {
		return component;
	}

	void refresh(Task task) {
		fontColorField.setEnabled(task != null && !task.isReadOnly());
		fontColorField.setRgb(task == null ? null : task.getFontColor());
	}

	void applyFontColor(Task task, Object eventSource) {
		if (task == null || task.isReadOnly())
			return;
		Integer color = fontColorField.getRgb();
		if (java.util.Objects.equals(task.getFontColor(), color))
			return;
		task.setFontColor(color);
		task.getProject().fireUpdateEvent(eventSource, task);
	}
}
