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

import javax.swing.JScrollPane;

import com.microproject.association.AssociationList;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.frames.DocumentFrame;
import com.microproject.pm.graphic.frames.GraphicManager;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.graphic.views.UsageDetailView;
import com.microproject.strings.Messages;

/** Canonical creation, refresh, and document-cache wiring for assignment lists. */
final class AssignmentSpreadsheetSupport {
	enum Perspective {
		TASK_ASSIGNMENTS(false, "View.TaskInformation.Assignments",
				UsageDetailView.resourceAssignmentSpreadsheetCategory, true, false, false, true),
		RESOURCE_ASSIGNMENTS(true, "View.ResourceInformation.Assignments",
				UsageDetailView.taskAssignmentSpreadsheetCategory, false, true, true, false);

		private final boolean taskRows;
		private final String viewName;
		private final String category;
		private final boolean leftAssociation;
		private final boolean modifyColumns;
		private final boolean documentCacheUsesTaskRows;
		private final boolean fireUpdateAll;

		Perspective(boolean taskRows, String viewName, String category, boolean leftAssociation,
				boolean modifyColumns, boolean documentCacheUsesTaskRows, boolean fireUpdateAll) {
			this.taskRows = taskRows;
			this.viewName = viewName;
			this.category = category;
			this.leftAssociation = leftAssociation;
			this.modifyColumns = modifyColumns;
			this.documentCacheUsesTaskRows = documentCacheUsesTaskRows;
			this.fireUpdateAll = fireUpdateAll;
		}
	}

	private AssignmentSpreadsheetSupport() {
	}

	static SpreadSheet create(Component owner, Perspective perspective) {
		SpreadSheet sheet = SpreadSheetUtils.createFilteredSpreadsheet(
				GraphicManager.getInstance(owner).getCurrentFrame(), perspective.taskRows, perspective.viewName,
				perspective.category, UsageDetailView.getUsageAssignmentSpreadsheetId(perspective.taskRows),
				perspective.leftAssociation, new String[] { MenuActionConstants.ACTION_DELETE });
		if (perspective.modifyColumns) {
			sheet.setCanModifyColumns(true);
			sheet.setCanSelectFieldArray(true);
		}
		return sheet;
	}

	static JScrollPane scrollPane(SpreadSheet sheet) {
		return SpreadSheetUtils.makeSpreadsheetScrollPane(sheet);
	}

	static void update(SpreadSheet sheet, AssociationList assignments, Perspective perspective) {
		SpreadSheetUtils.updateFilteredSpreadsheet(sheet, assignments == null ? new AssociationList() : assignments);
		if (perspective.fireUpdateAll)
			((SpreadSheetModel) sheet.getModel()).fireUpdateAll();
	}

	static void selectDocument(SpreadSheet sheet, DocumentFrame document, Perspective perspective) {
		if (sheet == null || document == null)
			return;
		NodeModelCache cache = document.createCache(perspective.documentCacheUsesTaskRows,
				Messages.getString("View.TaskInformation.Assignments")); //$NON-NLS-1$
		sheet.setCache(cache);
	}
}
