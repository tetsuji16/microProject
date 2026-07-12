package com.projectlibre1.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.projectlibre1.datatype.Duration;
import com.projectlibre1.graphic.configuration.SpreadSheetCategories;
import com.projectlibre1.grouping.core.Node;
import com.projectlibre1.options.CalendarOption;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCacheFactory;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetModel;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetUtils;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.undo.DataFactoryUndoController;

class CommonSpreadSheetSummaryScheduleEditingTest {
	@Test
	void summaryDurationEditUsesEnvelopeAndLeavesChildrenUntouched() throws Exception {
		Fixture fixture = createFixture();

		SwingUtilities.invokeAndWait(() ->
			fixture.model.setValueAt(new Duration(5L * fixture.day), fixture.parentRow, fixture.durationColumn));

		assertTrue(fixture.parent.hasSummaryEnvelope());
		assertEquals(5L * fixture.day, fixture.parent.getSummaryEnvelope().getManualDuration().longValue());
		assertEquals(fixture.originalChildStart, fixture.child.getStart());
		assertEquals(fixture.originalChildEnd, fixture.child.getEnd());
	}

	@Test
	void summaryStartEditDisplaysEnvelopeValue() throws Exception {
		Fixture fixture = createFixture();
		long manualStart = fixture.child.getEffectiveWorkCalendar().add(fixture.originalChildStart, -fixture.day, false);

		SwingUtilities.invokeAndWait(() ->
			fixture.model.setValueAt(new Date(manualStart), fixture.parentRow, fixture.startColumn));

		assertTrue(fixture.parent.hasSummaryEnvelope());
		assertEquals(manualStart, fixture.parent.getSummaryEnvelope().getManualStart().longValue());
		assertEquals(manualStart, ((Date) fixture.model.getValueAt(fixture.parentRow, fixture.startColumn)).getTime());
		assertEquals(fixture.originalChildStart, fixture.child.getStart());
	}

	private Fixture createFixture() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("summary-sheet-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);

		NormalTask parent = createTask(project, "Parent");
		NormalTask child = createTask(project, "Child");
		child.setDuration(CalendarOption.getInstance().getMillisPerDay());

		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel()),
			"summary-sheet-test",
			null);

		Node parentNode = (Node) project.getTaskModel().search(parent);
		Node childNode = (Node) project.getTaskModel().search(child);
		child.setWbsParent(parent);
		parent.setWbsChildrenNodes(List.of(childNode));
		try {
			cache.createHierarchyDependency(
				(com.projectlibre1.pm.graphic.model.cache.GraphicNode) cache.getGraphicNode(parentNode),
				(com.projectlibre1.pm.graphic.model.cache.GraphicNode) cache.getGraphicNode(childNode));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		cache.update();

		final SpreadSheet[] sheetRef = new SpreadSheet[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				SpreadSheet sheet = new SpreadSheet();
				sheet.setSpreadSheetCategory(SpreadSheetCategories.taskSpreadsheetCategory);
				SpreadSheetUtils.setFieldsAndContext(
					sheet,
					cache,
					SpreadSheetCategories.taskSpreadsheetCategory,
					"Spreadsheet.Task.entry",
					true);
				sheetRef[0] = sheet;
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		SpreadSheet sheet = sheetRef[0];
		SpreadSheetModel model = (SpreadSheetModel) sheet.getModel();
		long day = CalendarOption.getInstance().getMillisPerDay();
		return new Fixture(
			sheet,
			model,
			parent,
			child,
			findRow(model, cache, parent),
			findColumn(model, "Field.start"),
			findColumn(model, "Field.duration"),
			day,
			child.getStart(),
			child.getEnd());
	}

	private NormalTask createTask(Project project, String name) {
		NormalTask task = project.createScriptedTask();
		task.setName(name);
		project.connectTask(task);
		project.getTaskOutlines().addToAll(task, null);
		return task;
	}

	private int findColumn(SpreadSheetModel model, String fieldId) {
		for (int column = 0; column < model.getColumnCount(); column++) {
			com.projectlibre1.field.Field field = model.getFieldInColumn(column);
			if (field != null && fieldId.equals(field.getId()))
				return column;
		}
		throw new IllegalArgumentException("Missing field " + fieldId);
	}

	private int findRow(SpreadSheetModel model, NodeModelCache cache, NormalTask task) {
		Node node = (Node) cache.getModel().search(task);
		return model.findGraphicNodeRow(cache.getGraphicNode(node));
	}

	private record Fixture(
		SpreadSheet sheet,
		SpreadSheetModel model,
		NormalTask parent,
		NormalTask child,
		int parentRow,
		int startColumn,
		int durationColumn,
		long day,
		long originalChildStart,
		long originalChildEnd) {
	}
}
