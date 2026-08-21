/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.model.NodeModel;
import com.microproject.options.CalendarOption;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.RecurringTaskSpec;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.util.DateTime;

class TaskVisibilityServiceTest {
	@Test
	void hidesASelectedSummaryAndAllDescendantsAsOneUndoableOperation() {
		Project project = createProject();
		NodeModel model = project.getTaskModel();
		Node summary = new RecurringTaskInsertionService().insertRecurringTasks(
				project, model, null, project.getUndoController().getEditSupport(), new RecurringTaskSpec(
					"Phase", start(2026, Calendar.JANUARY, 5), 0L,
					RecurringTaskSpec.PatternType.DAILY, RecurringTaskSpec.RangeType.END_AFTER_OCCURRENCES,
					0L, 2, null));
		List children = model.getChildren(summary);
		Task parent = (Task) summary.getImpl();
		Task firstChild = (Task) ((Node) children.get(0)).getImpl();
		Task secondChild = (Task) ((Node) children.get(1)).getImpl();
		project.getUndoController().clear();

		TaskVisibilityService.hideSelected(project, List.of(summary), project.getUndoController());

		assertTrue(parent.isHiddenTask());
		assertTrue(firstChild.isHiddenTask());
		assertTrue(secondChild.isHiddenTask());
		project.getUndoController().undo();
		assertFalse(parent.isHiddenTask());
		assertFalse(firstChild.isHiddenTask());
		assertFalse(secondChild.isHiddenTask());
		project.getUndoController().redo();
		assertTrue(parent.isHiddenTask());
		assertTrue(firstChild.isHiddenTask());
		assertTrue(secondChild.isHiddenTask());
	}

	@Test
	void showAllIsUndoable() {
		Project project = createProject();
		Node taskNode = project.createLocalTaskNode(null);
		Task task = (Task) taskNode.getImpl();
		task.setHiddenTask(true);
		project.getUndoController().clear();

		TaskVisibilityService.showAll(project, project.getUndoController());

		assertFalse(task.isHiddenTask());
		project.getUndoController().undo();
		assertTrue(task.isHiddenTask());
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("visibility-service", undoController), undoController);
		project.initialize(false, false);
		return project;
	}

	private long start(int year, int month, int day) {
		return CalendarOption.getInstance().makeValidStart(DateTime.calendarInstance(year, month, day).getTimeInMillis(), true);
	}
}
