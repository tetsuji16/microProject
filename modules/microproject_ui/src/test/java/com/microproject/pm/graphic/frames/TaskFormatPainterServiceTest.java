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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.undo.DataFactoryUndoController;

class TaskFormatPainterServiceTest {
	@Test
	void appliesToMultipleTargetsAsOneUndoableChangeAndRejectsEmptySelection() {
		Project project = createProject();
		Task first = (Task)project.createLocalTaskNode(null).getImpl();
		Task second = (Task)project.createLocalTaskNode(null).getImpl();
		TaskFormatPainterService.TaskTextFormat requested =
			new TaskFormatPainterService.TaskTextFormat("Dialog", 14, 0x123456, true, true, true);
		project.getUndoController().clear();

		assertTrue(TaskFormatPainterService.apply(project, List.of(), requested,
			project.getUndoController()).isEmpty(), "an empty selection must be rejected");
		assertFalse(project.getUndoController().canUndo());
		assertEquals(List.of(first.getUniqueId(), second.getUniqueId()), TaskFormatPainterService.apply(project,
			List.of(first, second), requested, project.getUndoController()));
		assertEquals(requested, TaskFormatPainterService.capture(first));
		assertEquals(requested, TaskFormatPainterService.capture(second));
		assertTrue(project.getUndoController().canUndo(), "the multi-target apply must have one undo step");
		project.getUndoController().undo();
		assertTrue(isDefaultFormat(first));
		assertTrue(isDefaultFormat(second));
		project.getUndoController().redo();
		assertEquals(requested, TaskFormatPainterService.capture(first));
		assertEquals(requested, TaskFormatPainterService.capture(second));
	}

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
		project.markSaved();
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
		assertFalse(project.isGroupDirty(), "rollback must also restore the group-dirty flag");
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(0, events[0]);
		assertFalse(project.getUndoController().canUndo());
	}

	@Test
	void undoAndRedoFailuresRollbackAtomicallyAndKeepEditHistoryUsable() {
		Project project = createProject();
		Task first = (Task)project.createLocalTaskNode(null).getImpl();
		Task second = (Task)project.createLocalTaskNode(null).getImpl();
		TaskFormatPainterService.TaskTextFormat before = TaskFormatPainterService.capture(first);
		TaskFormatPainterService.TaskTextFormat formatted =
			new TaskFormatPainterService.TaskTextFormat("Dialog", 16, 0xABCDEF, true, true, true);
		Map<Task, TaskFormatPainterService.TaskTextFormat> beforeStates = new LinkedHashMap<>();
		Map<Task, TaskFormatPainterService.TaskTextFormat> afterStates = new LinkedHashMap<>();
		beforeStates.put(first, before);
		beforeStates.put(second, before);
		afterStates.put(first, formatted);
		afterStates.put(second, formatted);
		setFormat(first, formatted);
		setFormat(second, formatted);
		first.setDirty(false);
		second.setDirty(false);
		project.setDirty(false);
		long revision = project.getMutationState().revision();
		int[] events = { 0 };
		project.addObjectListener(event -> events[0]++);
		project.addScheduleListener(event -> events[0]++);
		AtomicInteger operation = new AtomicInteger();
		TaskFormatPainterService.ApplyHook hook = (task, index) -> {
			if (index == 0) operation.incrementAndGet();
			if (index == 1 && (operation.get() == 1 || operation.get() == 3))
				throw new IllegalStateException("injected replay failure");
		};
		TaskFormatPainterService.TaskTextFormatEdit edit = new TaskFormatPainterService.TaskTextFormatEdit(
			project, beforeStates, afterStates, project.getUndoController(), hook);
		project.getUndoController().clear();

		assertThrows(RuntimeException.class, edit::undo);
		assertEquals(formatted, TaskFormatPainterService.capture(first));
		assertEquals(formatted, TaskFormatPainterService.capture(second));
		assertEquals(revision, project.getMutationState().revision());
		assertEquals(0, events[0]);
		assertFalse(project.getUndoController().canUndo());
		assertFalse(project.getUndoController().canRedo());
		assertFalse(first.isDirty());
		assertFalse(second.isDirty());
		assertFalse(project.isDirty());
		assertTrue(edit.canUndo(), "failed undo must preserve the edit's undo state");

		edit.undo();
		assertEquals(before, TaskFormatPainterService.capture(first));
		assertEquals(before, TaskFormatPainterService.capture(second));
		long undoneRevision = project.getMutationState().revision();
		int undoneEvents = events[0];
		assertThrows(RuntimeException.class, edit::redo);
		assertEquals(before, TaskFormatPainterService.capture(first));
		assertEquals(before, TaskFormatPainterService.capture(second));
		assertEquals(undoneRevision, project.getMutationState().revision());
		assertEquals(undoneEvents, events[0]);
		assertTrue(edit.canRedo(), "failed redo must preserve the edit's redo state");

		edit.redo();
		assertEquals(formatted, TaskFormatPainterService.capture(first));
		assertEquals(formatted, TaskFormatPainterService.capture(second));
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

	private static void setFormat(Task task, TaskFormatPainterService.TaskTextFormat format) {
		task.setFontFamily(format.family());
		task.setFontSize(format.size());
		task.setFontColor(format.color());
		task.setFontBold(format.bold());
		task.setFontItalic(format.italic());
		task.setFontStrikethrough(format.strikethrough());
	}

	private static boolean isDefaultFormat(Task task) {
		return TaskFormatPainterService.capture(task).equals(
			new TaskFormatPainterService.TaskTextFormat(null, 0, null, false, false, false));
	}
}
