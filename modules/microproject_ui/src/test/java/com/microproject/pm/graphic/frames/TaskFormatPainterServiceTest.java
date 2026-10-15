/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

class TaskFormatPainterServiceTest {
	@Test
	void copiesAllTaskTextStylePropertiesAsOneUndoableChangeAndPreservesTaskData() {
		Project project = createProject();
		Task source = (Task)project.createLocalTaskNode(null).getImpl();
		Task target = (Task)project.createLocalTaskNode(null).getImpl();
		source.setFontFamily("Dialog");
		source.setFontSize(14);
		source.setFontColor(0x123456);
		source.setFontBold(true);
		source.setFontItalic(true);
		source.setFontStrikethrough(true);
		target.setName("Target task");
		long originalDuration = target.getDurationMillis();
		project.getUndoController().clear();

		List<Long> changed = TaskFormatPainterService.apply(project, List.of(target),
			TaskFormatPainterService.capture(source), project.getUndoController());

		assertEquals(List.of(target.getUniqueId()), changed);
		assertEquals(TaskFormatPainterService.capture(source), TaskFormatPainterService.capture(target));
		assertEquals("Target task", target.getName());
		assertEquals(originalDuration, target.getDurationMillis());
		project.getUndoController().undo();
		assertEquals(new TaskFormatPainterService.TaskTextFormat(null, 0, null, false, false, false),
			TaskFormatPainterService.capture(target));
		assertEquals("Target task", target.getName());
		project.getUndoController().redo();
		assertEquals(TaskFormatPainterService.capture(source), TaskFormatPainterService.capture(target));
	}

	@Test
	void noOpAndReadOnlyTargetsDoNotCreateAnUndoableChange() {
		Project project = createProject();
		Task task = (Task)project.createLocalTaskNode(null).getImpl();
		TaskFormatPainterService.TaskTextFormat style = TaskFormatPainterService.capture(task);

		assertTrue(TaskFormatPainterService.apply(project, List.of(task), style, project.getUndoController()).isEmpty());
		assertFalse(project.getUndoController().canUndo());
		project.setReadOnly(true);
		assertTrue(TaskFormatPainterService.apply(project, List.of(task),
			new TaskFormatPainterService.TaskTextFormat("Dialog", 13, 0xABCDEF, true, false, false),
			project.getUndoController()).isEmpty());
		assertFalse(project.getUndoController().canUndo());
	}

	@Test
	void mixedEditableAndReadOnlyTargetsRejectWithoutPartiallyChangingStyles() {
		Project project = createProject();
		Task editable = (Task)project.createLocalTaskNode(null).getImpl();
		Task readOnly = (Task)project.createLocalTaskNode(null).getImpl();
		readOnly.setExternal(true);
		TaskFormatPainterService.TaskTextFormat requested =
			new TaskFormatPainterService.TaskTextFormat("Dialog", 13, 0xABCDEF, true, true, false);
		TaskFormatPainterService.TaskTextFormat original = TaskFormatPainterService.capture(editable);

		assertTrue(TaskFormatPainterService.apply(project, List.of(editable, readOnly), requested,
			project.getUndoController()).isEmpty());
		assertEquals(original, TaskFormatPainterService.capture(editable));
		assertFalse(project.getUndoController().canUndo());
	}

	@Test
	void failureOnSecondTargetRollsBackStylesDirtyStateRevisionEventsAndUndo() {
		Project project = createProject();
		Task first = (Task)project.createLocalTaskNode(null).getImpl();
		Task second = (Task)project.createLocalTaskNode(null).getImpl();
		TaskFormatPainterService.TaskTextFormat firstBefore = TaskFormatPainterService.capture(first);
		TaskFormatPainterService.TaskTextFormat secondBefore = TaskFormatPainterService.capture(second);
		TaskFormatPainterService.TaskTextFormat requested =
			new TaskFormatPainterService.TaskTextFormat("Dialog", 16, 0xABCDEF, true, true, true);
		project.setDirty(false);
		first.setDirty(false);
		second.setDirty(false);
		long revision = project.getMutationState().revision();
		int[] events = { 0 };
		project.addObjectListener(event -> events[0]++);
		project.addScheduleListener(event -> events[0]++);

		assertThrows(IllegalStateException.class, () -> TaskFormatPainterService.apply(project,
			List.of(first, second), requested, project.getUndoController(), (task, index) -> {
				if (index == 1) throw new IllegalStateException("injected second-target failure");
			}));

		assertEquals(firstBefore, TaskFormatPainterService.capture(first));
		assertEquals(secondBefore, TaskFormatPainterService.capture(second));
		assertFalse(first.isDirty());
		assertFalse(second.isDirty());
		assertFalse(project.isDirty());
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(0, events[0]);
		assertFalse(project.getUndoController().canUndo());
	}

	@Test
	void missingUndoControllerRejectsBeforeChangingStyle() {
		Project project = createProject();
		Task task = (Task)project.createLocalTaskNode(null).getImpl();
		TaskFormatPainterService.TaskTextFormat before = TaskFormatPainterService.capture(task);

		assertTrue(TaskFormatPainterService.apply(project, List.of(task),
			new TaskFormatPainterService.TaskTextFormat("Dialog", 16, 0xABCDEF, true, false, false), null).isEmpty());
		assertEquals(before, TaskFormatPainterService.capture(task));
	}

	private static Project createProject() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("format-painter", undo), undo);
		project.initialize(false, false);
		return project;
	}
}
