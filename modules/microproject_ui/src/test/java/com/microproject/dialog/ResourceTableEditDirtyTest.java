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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;
import javax.swing.undo.AbstractUndoableEdit;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModelDataFactory;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.interval.ValueObjectForIntervalTable;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;

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

	/** A sheet built the way the dialog builds it, with a removable second row; the project starts saved. */
	private static SpreadSheet tableSheet(Fixture fixture, ValueObjectForIntervalTable table, String category,
			String spreadsheetId) throws Exception {
		table.newValueObject(DateTime.calendarInstance(2030, Calendar.JANUARY, 1).getTimeInMillis());
		SpreadSheet sheet = new SpreadSheet();
		sheet.setSpreadSheetCategory(category);
		sheet.setCanSelectFieldArray(false);
		SpreadSheetUtils.createCollectionSpreadSheet(sheet, table.getList(), "View.ResourceInformation.Availability",
			category, spreadsheetId, false, (NodeModelDataFactory) table, 1);
		ResourceInformationDialog.trackTableEdits(sheet, () -> fixture.resource());
		fixture.project().markSaved();
		assertFalse(fixture.project().needsSaving(), "opening the table must not count as an edit");
		return sheet;
	}

	private static List<Node> lastDataRow(SpreadSheet sheet) {
		List<Node> rows = new ArrayList<>();
		sheet.getCache().getModel().getHierarchy().getChildren(null).forEach(o -> {
			if (!((Node) o).isVoid()) rows.add((Node) o);
		});
		assertTrue(rows.size() >= 2, "second row");
		return new ArrayList<>(rows.subList(rows.size() - 1, rows.size()));
	}

	private static void onEdt(Fixture fixture, Consumer<SpreadSheet> step, ValueObjectForIntervalTable table,
			String category, String spreadsheetId) throws Exception {
		AtomicReference<Throwable> failure = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			try {
				step.accept(tableSheet(fixture, table, category, spreadsheetId));
			} catch (Throwable t) {
				failure.set(t);
			}
		});
		if (failure.get() != null) throw new AssertionError(failure.get());
	}

	private static void assertRowRemovalMarksDirty(boolean availability, Consumer<SpreadSheet> route) throws Exception {
		Fixture f = fixture();
		ValueObjectForIntervalTable table = availability ? f.resource().getAvailabilityTable()
			: (ValueObjectForIntervalTable) f.resource().getCostRateTable(0);
		onEdt(f, sheet -> {
			int before = table.getList().size();
			route.accept(sheet);
			assertEquals(before - 1, table.getList().size(), "route must remove the row");
		}, table, availability ? "availabilitySpreadsheet" : "costRates",
			availability ? "Spreadsheet.availability" : "Spreadsheet.costRates");
		assertTrue(f.project().needsSaving(), "row removal must prompt to save");
	}

	@Test
	void deletingAvailabilityRowMarksProjectForSaving() throws Exception {
		assertRowRemovalMarksDirty(true, sheet -> sheet.getCache().deleteNodes(lastDataRow(sheet)));
	}

	@Test
	void cuttingAvailabilityRowMarksProjectForSaving() throws Exception {
		assertRowRemovalMarksDirty(true, sheet -> sheet.getCache().cutNodes(lastDataRow(sheet)));
	}

	@Test
	void deletingCostRateRowMarksProjectForSaving() throws Exception {
		assertRowRemovalMarksDirty(false, sheet -> sheet.getCache().deleteNodes(lastDataRow(sheet)));
	}

	@Test
	void cuttingCostRateRowMarksProjectForSaving() throws Exception {
		assertRowRemovalMarksDirty(false, sheet -> sheet.getCache().cutNodes(lastDataRow(sheet)));
	}

	/** The dialog refills a live sheet whenever another resource is selected; that is navigation, not an edit. */
	@Test
	void refillingSheetForAnotherResourceDoesNotMarkProject() throws Exception {
		Fixture f = fixture();
		ResourceImpl other = f.project().getResourcePool().newResourceInstance();
		other.setName("Other resource");
		var table = f.resource().getAvailabilityTable();
		onEdt(f, sheet -> {
			var otherTable = other.getAvailabilityTable();
			SpreadSheetUtils.updateCollectionSpreadSheet(sheet, otherTable.getList(), otherTable, 1);
			SpreadSheetUtils.updateCollectionSpreadSheet(sheet, table.getList(), table, 1);
		}, table, "availabilitySpreadsheet", "Spreadsheet.availability");
		assertFalse(f.project().needsSaving(), "refilling the sheet must not prompt to save");
	}

	/**
	 * Table rows have no undo stack of their own, so the only Undo/Redo a user can reach is the
	 * project's. It must leave the rows alone and must not hide the unsaved row removal.
	 */
	@Test
	void projectUndoAndRedoKeepRowRemovalUnsaved() throws Exception {
		Fixture f = fixture();
		var table = f.resource().getAvailabilityTable();
		onEdt(f, sheet -> {
			assertNull(sheet.getCache().getModel().getUndoController(), "table rows have no undo stack");
			var history = f.project().getUndoController();
			history.editPosted(new AbstractUndoableEdit());
			sheet.getCache().deleteNodes(lastDataRow(sheet));
			int rows = table.getList().size();

			history.undo();
			assertEquals(rows, table.getList().size(), "project undo must not change table rows");
			assertTrue(f.project().needsSaving(), "project undo must keep the row removal unsaved");

			history.redo();
			assertEquals(rows, table.getList().size(), "project redo must not change table rows");
			assertTrue(f.project().needsSaving(), "project redo must keep the row removal unsaved");
		}, table, "availabilitySpreadsheet", "Spreadsheet.availability");
	}

	/** Before any command has run, a right-click must already find Delete and Cut on both tables. */
	@Test
	void bothTablesOfferRowCommandsFromTheirFirstPopup() throws Exception {
		Fixture f = fixture();
		onEdt(f, sheet -> assertRowCommands(sheet), f.resource().getAvailabilityTable(),
				"availabilitySpreadsheet", "Spreadsheet.availability");
		onEdt(f, sheet -> assertRowCommands(sheet), f.resource().getCostRateTable(0),
				"costRates", "Spreadsheet.costRates");
	}

	private static void assertRowCommands(SpreadSheet sheet) {
		var popup = sheet.getPopup();
		assertNotNull(popup, "the table must have a row popup");
		var items = new ArrayList<String>();
		for (var component : popup.getComponents())
			items.add(component.getName());
		assertTrue(items.contains("popup." + MenuActionConstants.ACTION_DELETE), () -> "Delete missing: " + items);
		assertTrue(items.contains("popup." + MenuActionConstants.ACTION_CUT), () -> "Cut missing: " + items);
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
