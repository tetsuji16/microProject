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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

/**
 * The Resource Information dialog edits AvailabilityTable and CostRateTable rows in its own
 * spreadsheets.  Those rows are not DataObjects and the tables have no owner link, so the edit
 * must reach the project through the table's group-dirty flag (issue #984 item g).
 */
class ResourceTableEditDirtyTest {
	private record Fixture(Project project, ResourceImpl resource) { }

	private static Fixture fixture() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("table-dirty", undo), undo);
		project.initialize(false, false);
		project.getResourcePool().addProject(project); // as Portfolio does for a loaded project
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		resource.setName("Table dirty resource");
		project.markSaved();
		assertFalse(project.needsSaving());
		return new Fixture(project, resource);
	}

	/** Builds the sheet exactly as ResourceInformationDialog does and edits the first data row. */
	private static void edit(Fixture fixture, Object table, java.util.Collection<?> rows, String category,
			String spreadsheetId, String fieldId, Object value) throws Exception {
		AtomicReference<Throwable> failure = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				SpreadSheet sheet = new SpreadSheet();
				sheet.setSpreadSheetCategory(category);
				sheet.setCanSelectFieldArray(false);
				SpreadSheetUtils.createCollectionSpreadSheet(sheet, rows, "View.ResourceInformation.Availability",
					category, spreadsheetId, false, (com.microproject.grouping.core.model.NodeModelDataFactory) table, 1);
				ResourceInformationDialog.trackTableEdits(sheet, () -> fixture.resource());
				SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
				int column = -1;
				for (int c = 1; c < model.getColumnCount(); c++)
					if (fieldId.equals(model.getFieldInColumn(c).getId())) column = c;
				assertTrue(column > 0, fieldId + " column");
				model.setValueAt(value, 0, column);
			} catch (Throwable t) {
				failure.set(t);
			}
		});
		if (failure.get() != null) throw new AssertionError(failure.get());
	}

	@Test
	void availabilityRowEditMarksProjectForSaving() throws Exception {
		Fixture f = fixture();
		var table = f.resource().getAvailabilityTable();
		edit(f, table, table.getList(), "availabilitySpreadsheet", "Spreadsheet.availability",
			"Field.maximumUnits", Double.valueOf(0.5d));
		assertEquals(0.5d, ((com.microproject.pm.availability.Availability) table.getList().get(0)).getMaximumUnits());
		assertTrue(f.project().needsSaving(), "availability edit must prompt to save");
	}

	@Test
	void costRateRowEditMarksProjectForSaving() throws Exception {
		Fixture f = fixture();
		var table = f.resource().getCostRateTable(0);
		edit(f, table, table.getList(), "costRates", "Spreadsheet.costRates", "Field.costPerUse",
			Double.valueOf(12.0d));
		assertEquals(12.0d, ((com.microproject.pm.costing.CostRate) table.getList().get(0)).getCostPerUse());
		assertTrue(f.project().needsSaving(), "cost rate edit must prompt to save");
	}
}
