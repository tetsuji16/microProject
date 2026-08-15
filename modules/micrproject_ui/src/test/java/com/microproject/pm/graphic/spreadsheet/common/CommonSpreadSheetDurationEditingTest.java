package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.options.CalendarOption;
import com.microproject.pm.graphic.model.cache.NodeModelCache;
import com.microproject.pm.graphic.model.cache.NodeModelCacheFactory;
import com.microproject.pm.graphic.spreadsheet.SpreadSheet;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetModel;
import com.microproject.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class CommonSpreadSheetDurationEditingTest {
	@Test
	void validDurationTextCommitsToTask() throws Exception {
		SpreadsheetFixture fixture = createFixture();

		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.changeSelection(0, fixture.durationColumn, false, false);
			assertTrue(fixture.sheet.editCellAt(0, fixture.durationColumn, doubleClick(fixture.sheet)));
			JTextField editor = (JTextField) fixture.sheet.getEditorComponent();
			editor.setText("3d");
			assertTrue(fixture.sheet.getCellEditor().stopCellEditing());
		});

		assertTrue(fixture.sheet.getLastException() == null);
	}

	@Test
	void unchangedDurationTextCancelsCleanly() throws Exception {
		SpreadsheetFixture fixture = createFixture();

		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.changeSelection(0, fixture.durationColumn, false, false);
			assertTrue(fixture.sheet.editCellAt(0, fixture.durationColumn, doubleClick(fixture.sheet)));
			assertTrue(fixture.sheet.getCellEditor().stopCellEditing());
		});

		assertFalse(fixture.sheet.isEditing());
		assertTrue(fixture.task.getDuration() == fixture.originalDuration);
	}

	@Test
	void invalidDurationTextLeavesEditorInErrorState() throws Exception {
		SpreadsheetFixture fixture = createFixture();
		final boolean[] stopped = new boolean[1];

		SwingUtilities.invokeAndWait(() -> {
			fixture.sheet.changeSelection(0, fixture.durationColumn, false, false);
			assertTrue(fixture.sheet.editCellAt(0, fixture.durationColumn, doubleClick(fixture.sheet)));
			JTextField editor = (JTextField) fixture.sheet.getEditorComponent();
			editor.setText("not-a-duration");
			stopped[0] = fixture.sheet.getCellEditor().stopCellEditing();
		});

		assertFalse(stopped[0]);
		assertNotNull(fixture.sheet.getLastException());
		assertTrue(fixture.task.getDuration() == fixture.originalDuration);
	}

	private static SpreadsheetFixture createFixture() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("duration-edit-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);

		NormalTask task = project.createScriptedTask();
		task.setName("Duration edit");
		task.setDuration(CalendarOption.getInstance().getMillisPerDay());
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);

		final SpreadsheetFixture[] fixtureRef = new SpreadsheetFixture[1];
		SwingUtilities.invokeAndWait(() -> {
			TestSpreadSheet sheet = new TestSpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
				"duration-edit-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(
				sheet,
				cache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			fixtureRef[0] = new SpreadsheetFixture(sheet, task);
		});
		return fixtureRef[0];
	}

	private static MouseEvent doubleClick(SpreadSheet sheet) {
		return new MouseEvent(sheet, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 2, false);
	}

	private static final class SpreadsheetFixture {
		private final TestSpreadSheet sheet;
		private final NormalTask task;
		private final int durationColumn;
		private final long originalDuration;

		private SpreadsheetFixture(TestSpreadSheet sheet, NormalTask task) {
			this.sheet = sheet;
			this.task = task;
			this.durationColumn = findColumnByFieldId(sheet, "Field.duration");
			this.originalDuration = task.getDuration();
		}

		private static int findColumnByFieldId(SpreadSheet sheet, String fieldId) {
			SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
			for (int column = 0; column < model.getColumnCount(); column++) {
				com.microproject.field.Field field = model.getFieldInColumn(column);
				if (field != null && fieldId.equals(field.getId())) {
					return column;
				}
			}
			throw new IllegalArgumentException("Missing field: " + fieldId);
		}
	}

	private static final class TestSpreadSheet extends SpreadSheet {
		@Override
		public void setValueAt(Object value, int row, int column) {
			lastException = null;
			try {
				getModel().setValueAt(value, row, column);
			} catch (Exception e) {
				lastException = (e.getCause() instanceof Exception) ? (Exception) e.getCause() : e;
				doPostExceptionTreatment();
			}
		}
	}
}
