/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2012-2019 ProjectLibre, Inc.  (Previous Copyright Holder)
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
package com.microproject.pm.graphic.spreadsheet.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.Duration;
import com.microproject.graphic.configuration.SpreadSheetCategories;
import com.microproject.grouping.core.Node;
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

		var reference = NodeModelCacheFactory.createTaskNodeModelCache(project, project.getTaskModel());
		reference.setTaskCommandGateway(new com.microproject.application.task.TaskCommandGateway(project));
		NodeModelCache cache = NodeModelCacheFactory.getInstance().createFilteredCache(
			reference,
			"summary-sheet-test",
			null);

		Node parentNode = (Node) project.getTaskModel().search(parent);
		Node childNode = (Node) project.getTaskModel().search(child);
		child.setWbsParent(parent);
		parent.setWbsChildrenNodes(List.of(childNode));
		try {
			cache.createHierarchyDependency(
				(com.microproject.pm.graphic.model.cache.GraphicNode) cache.getGraphicNode(parentNode),
				(com.microproject.pm.graphic.model.cache.GraphicNode) cache.getGraphicNode(childNode));
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
			com.microproject.field.Field field = model.getFieldInColumn(column);
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
