package com.projectlibre1.pm.graphic.spreadsheet.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

import com.projectlibre1.graphic.configuration.SpreadSheetCategories;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCacheFactory;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetModel;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.undo.DataFactoryUndoController;
import com.projectlibre1.util.DateTime;

class DateEditorBehaviorTest {
	@Test
	void blankTextCancelsWhenInitialValueIsNull() throws Exception {
		SpreadsheetFixture fixture = createFixture();
		final TrackingDateEditor[] editorRef = new TrackingDateEditor[1];

		SwingUtilities.invokeAndWait(() -> {
			int startColumn = fixture.findColumnByFieldId("Field.start");
			TrackingDateEditor editor = new TrackingDateEditor();
			editorRef[0] = editor;
			editor.getTableCellEditorComponent(fixture.sheet, fixture.firstStart, true, 0, startColumn);
			editor.dateField.getTextField().setText("");
			setInitialValue(editor, null);

			assertTrue(editor.stopCellEditing());
		});

		assertTrue(editorRef[0].canceled);
		assertSame(editorRef[0].initialEditorValue, editorRef[0].getCellEditorValue());
	}

	@Test
	void blankTextCommitsClearWhenInitialValueExists() throws Exception {
		SpreadsheetFixture fixture = createFixture();
		final TrackingDateEditor[] editorRef = new TrackingDateEditor[1];

		SwingUtilities.invokeAndWait(() -> {
			int startColumn = fixture.findColumnByFieldId("Field.start");
			TrackingDateEditor editor = new TrackingDateEditor();
			editorRef[0] = editor;
			editor.getTableCellEditorComponent(fixture.sheet, fixture.firstStart, true, 0, startColumn);
			editor.dateField.getTextField().setText("");

			assertTrue(editor.stopCellEditing());
		});

		assertTrue(!editorRef[0].canceled);
		assertNull(editorRef[0].getCellEditorValue());
	}

	@Test
	void invalidDateCancelsAndLeavesValueUnchanged() throws Exception {
		SpreadsheetFixture fixture = createFixture();
		final TrackingDateEditor[] editorRef = new TrackingDateEditor[1];

		SwingUtilities.invokeAndWait(() -> {
			int startColumn = fixture.findColumnByFieldId("Field.start");
			TrackingDateEditor editor = new TrackingDateEditor();
			editorRef[0] = editor;
			editor.getTableCellEditorComponent(fixture.sheet, fixture.firstStart, true, 0, startColumn);
			editor.dateField.getTextField().setText("2/30");

			assertTrue(editor.stopCellEditing());
		});

		assertTrue(editorRef[0].canceled);
		assertEquals(fixture.firstStart, editorRef[0].getCellEditorValue());
	}

	@Test
	void yearlessDateUsesPreviousRowReferenceDate() throws Exception {
		SpreadsheetFixture fixture = createFixture();
		final TrackingDateEditor[] editorRef = new TrackingDateEditor[1];

		SwingUtilities.invokeAndWait(() -> {
			TrackingDateEditor editor = new TrackingDateEditor();
			editorRef[0] = editor;
			JTable table = new JTable(new DefaultTableModel(
				new Object[][] {
					{ fixture.firstStart },
					{ fixture.secondStart },
				},
				new Object[] { "Start" }));
			editor.dateField = new DateEditor.ExtDateField(new SimpleDateFormat("yyyy/MM/dd"));
			editor.dateField.setValue(fixture.secondStart);
			editor.dateField.getTextField().setText("2/2");
			setEditorState(editor, table, 1, 0, new SimpleDateFormat("yyyy/MM/dd"), fixture.secondStart);

			assertTrue(editor.stopCellEditing());
		});

		Date expected = DateTime.calendarInstance(2021, Calendar.FEBRUARY, 2).getTime();
		assertEquals(expected, editorRef[0].getCellEditorValue());
	}

	@Test
	void sameParsedDateCancelsWithoutCommitting() throws Exception {
		final TrackingDateEditor[] editorRef = new TrackingDateEditor[1];

		SwingUtilities.invokeAndWait(() -> {
			Date exactDate = DateTime.calendarInstance(2026, Calendar.JUNE, 6).getTime();
			TrackingDateEditor editor = new TrackingDateEditor();
			editorRef[0] = editor;
			JTable table = new JTable(new DefaultTableModel(new Object[][] { { exactDate } }, new Object[] { "Start" }));
			SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
			editor.dateField = new DateEditor.ExtDateField(format);
			editor.dateField.setValue(exactDate);
			editor.dateField.getTextField().setText(format.format(exactDate));
			setEditorState(editor, table, 0, 0, format, exactDate);

			assertTrue(editor.stopCellEditing());
		});

		assertTrue(editorRef[0].canceled);
		assertEquals(DateTime.calendarInstance(2026, Calendar.JUNE, 6).getTime(), editorRef[0].getCellEditorValue());
	}

	private static void setInitialValue(DateEditor editor, Date value) {
		try {
			Field initialValue = DateEditor.class.getDeclaredField("initialValue");
			initialValue.setAccessible(true);
			initialValue.set(editor, value);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static void setEditorState(DateEditor editor, JTable table, int row, int column, SimpleDateFormat format, Date initialValue) {
		try {
			Field tableField = DateEditor.class.getDeclaredField("table");
			tableField.setAccessible(true);
			tableField.set(editor, table);

			Field editingRow = DateEditor.class.getDeclaredField("editingRow");
			editingRow.setAccessible(true);
			editingRow.setInt(editor, row);

			Field editingColumn = DateEditor.class.getDeclaredField("editingColumn");
			editingColumn.setAccessible(true);
			editingColumn.setInt(editor, column);

			Field editingFormat = DateEditor.class.getDeclaredField("editingFormat");
			editingFormat.setAccessible(true);
			editingFormat.set(editor, format);

			setInitialValue(editor, initialValue);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static SpreadsheetFixture createFixture() throws Exception {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("date-editor-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);

		Calendar startCalendar = DateTime.calendarInstance(2020, Calendar.DECEMBER, 1);
		startCalendar.set(Calendar.HOUR_OF_DAY, 8);
		startCalendar.set(Calendar.MINUTE, 0);
		Date firstDate = startCalendar.getTime();

		Calendar secondCalendar = DateTime.calendarInstance(2020, Calendar.DECEMBER, 3);
		secondCalendar.set(Calendar.HOUR_OF_DAY, 8);
		secondCalendar.set(Calendar.MINUTE, 0);
		Date secondDate = secondCalendar.getTime();

		NormalTask firstTask = createTask(project, "First", firstDate);
		NormalTask secondTask = createTask(project, "Second", secondDate);

		final SpreadsheetFixture[] fixtureRef = new SpreadsheetFixture[1];
		SwingUtilities.invokeAndWait(() -> {
			SpreadSheet sheet = new SpreadSheet();
			sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
			NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
				NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
				"date-editor-test",
				null);
			SpreadSheetUtils.setFieldsAndContext(
				sheet,
				cache,
				SpreadSheetCategories.taskSpreadsheetCategory,
				"Spreadsheet.Task.entry",
				true);
			fixtureRef[0] = new SpreadsheetFixture(sheet, firstTask, secondTask, firstDate, secondDate);
		});
		return fixtureRef[0];
	}

	private static NormalTask createTask(Project project, String name, Date start) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		task.setStart(start.getTime());
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private static final class TrackingDateEditor extends DateEditor {
		private boolean canceled;
		private Object initialEditorValue;

		@Override
		public java.awt.Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected, int row, int col) {
			initialEditorValue = value;
			return super.getTableCellEditorComponent(table, value, isSelected, row, col);
		}

		@Override
		public void cancelCellEditing() {
			canceled = true;
			super.cancelCellEditing();
		}
	}

	private static final class SpreadsheetFixture {
		private final SpreadSheet sheet;
		private final NormalTask firstTask;
		private final NormalTask secondTask;
		private final Date firstStart;
		private final Date secondStart;

		private SpreadsheetFixture(SpreadSheet sheet, NormalTask firstTask, NormalTask secondTask, Date firstStart, Date secondStart) {
			this.sheet = sheet;
			this.firstTask = firstTask;
			this.secondTask = secondTask;
			this.firstStart = firstStart;
			this.secondStart = secondStart;
		}

		private int findColumnByFieldId(String fieldId) {
			SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
			for (int column = 0; column < model.getColumnCount(); column++) {
				com.projectlibre1.field.Field field = model.getFieldInColumn(column);
				if (field != null && fieldId.equals(field.getId())) {
					return column;
				}
			}
			throw new IllegalArgumentException("Missing field: " + fieldId);
		}
	}
}
