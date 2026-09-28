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

import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.microproject.pm.task.ScheduleDiagnosticsService;
import com.microproject.pm.task.Task;
import com.microproject.strings.Messages;

/** Read-only diagnostics table for a task's current schedule. */
final class TaskDiagnosticsPanel extends JScrollPane {
	private static final long serialVersionUID = 1L;

	TaskDiagnosticsPanel(Task task) {
		super(createTable(task));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
	}

	private static JTable createTable(Task task) {
		String[] columns = {
			Messages.getString("TaskInformationDialog.Severity"),
			Messages.getString("TaskInformationDialog.Issue"),
			Messages.getString("TaskInformationDialog.Cause"),
			Messages.getString("TaskInformationDialog.Recommendation")
		};
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			private static final long serialVersionUID = 1L;
			@Override public boolean isCellEditable(int row, int column) { return false; }
		};
		for (var issue : new ScheduleDiagnosticsService().diagnose(task)) {
			String key = "diagnostic." + issue.type().name().toLowerCase(Locale.ROOT);
			model.addRow(new Object[] { issue.severity(), UsabilityStrings.text(key + ".summary"),
					UsabilityStrings.text(key + ".cause"), UsabilityStrings.text(key + ".recommendation") });
		}
		JTable table = new JTable(model);
		table.setAutoCreateRowSorter(true);
		table.setRowHeight(Math.max(table.getRowHeight(), 24));
		table.getAccessibleContext().setAccessibleName(Messages.getString("TaskInformationDialog.Diagnostics"));
		return table;
	}
}
