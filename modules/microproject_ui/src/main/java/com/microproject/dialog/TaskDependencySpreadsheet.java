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

import javax.swing.JLabel;
import javax.swing.table.TableCellRenderer;

import com.microproject.association.AssociationList;
import com.microproject.configuration.Configuration;
import com.microproject.field.Field;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.dependency.Dependency;
import com.microproject.pm.dependency.DependencyNodeModelDataFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.task.Task;

/** Shared dependency table behavior for the predecessor and successor views. */
final class TaskDependencySpreadsheet extends SpreadSheet {
	enum Direction {
		PREDECESSORS(true, "Field.predecessorName", "View.TaskInformation.Predecessors",
				"Spreadsheet.Dependency.predecessors"),
		SUCCESSORS(false, "Field.successorName", "View.TaskInformation.Successors",
				"Spreadsheet.Dependency.successors");

		private final boolean predecessors;
		private final String fieldId;
		private final String viewId;
		private final String fieldsId;

		Direction(boolean predecessors, String fieldId, String viewId, String fieldsId) {
			this.predecessors = predecessors;
			this.fieldId = fieldId;
			this.viewId = viewId;
			this.fieldsId = fieldsId;
		}
	}

	private final TaskInformationDialog dialog;
	private final Direction direction;
	private final Field clickField;

	private TaskDependencySpreadsheet(TaskInformationDialog dialog, Direction direction) {
		this.dialog = dialog;
		this.direction = direction;
		clickField = Configuration.getFieldFromId(direction.fieldId);
	}

	static TaskDependencySpreadsheet create(TaskInformationDialog dialog, Direction direction, Task task) {
		TaskDependencySpreadsheet sheet = new TaskDependencySpreadsheet(dialog, direction);
		sheet.setSpreadSheetCategory(TaskInformationDialog.DEPENDENCY_SPREADSHEET);
		sheet.setCanModifyColumns(false);
		sheet.setCanSelectFieldArray(false);
		sheet.setActions(new String[] { MenuActionConstants.ACTION_DELETE });
		SpreadSheetUtils.createCollectionSpreadSheet(sheet, assignments(task, direction),
				 direction.viewId, TaskInformationDialog.DEPENDENCY_SPREADSHEET, direction.fieldsId,
				 direction.predecessors, new DependencyNodeModelDataFactory(), 0);
		return sheet;
	}

	static void update(TaskDependencySpreadsheet sheet, Task task, Direction direction) {
		if (sheet != null)
			SpreadSheetUtils.updateCollectionSpreadSheet(sheet, assignments(task, direction),
					new DependencyNodeModelDataFactory(), 0);
	}

	private static AssociationList assignments(Task task, Direction direction) {
		if (task == null)
			return new AssociationList();
		return direction.predecessors ? task.getPredecessorList() : task.getSuccessorList();
	}

	@Override
	public void doDoubleClick(int row, int col) {
	}

	@Override
	public void doClick(int row, int col) {
		Object rowObject = getCurrentRowImpl();
		if (rowObject == null || ((SpreadSheetModel) getModel()).getFieldInColumn(col + 1) != clickField)
			return;
		Task endpoint = endpoint(rowObject);
		if (endpoint instanceof com.microproject.pm.task.NormalTask normalTask) {
			dialog.setObject(normalTask);
			dialog.updateAll();
			normalTask.getDocument().getObjectSelectionEventManager().fire(this, normalTask);
		}
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component component = super.prepareRenderer(renderer, row, column);
		Field field = ((SpreadSheetModel) getModel()).getFieldInColumn(column + 1);
		if (field == clickField) {
			JLabel label = (JLabel) component;
			label.setText("<html><a href=\"\">"
					+ TaskDependencyChoices.dependencyDisplayName((Task) dialog.getObject(), endpoint(
							((SpreadSheetModel) getModel()).getObjectInRow(row))) + "</a></html>");
		}
		return component;
	}

	private Task endpoint(Object rowObject) {
		if (!(rowObject instanceof Dependency dependency))
			return null;
		return (Task) (direction.predecessors ? dependency.getLeft() : dependency.getRight());
	}
}
