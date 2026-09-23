/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.microproject.command.UpdateProjectCommand;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;
import com.microproject.grouping.core.Node;
import com.microproject.pm.dependency.DependencyService;
import com.microproject.pm.dependency.DependencyType;
import javax.swing.undo.UndoableEditSupport;
import javax.swing.undo.UndoManager;

class UpdateProjectRequestTest {
	@Test
	void requestIsImmutableAndRejectsInvalidDate() {
		UpdateProjectRequest request = new UpdateProjectRequest(42L, true, false, true);
		assertEquals(42L, request.statusDate());
		assertThrows(IllegalArgumentException.class,
			() -> new UpdateProjectRequest(0L, false, false, false));
	}

	@Test
	void commandUsesTypedRequestAndReportsOnlyTasksActuallyChanged() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("update", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		UpdateProjectRequest request = new UpdateProjectRequest(task.getEnd() + 86_400_000L,
			true, false, false);
		long originalStatusDate = project.getStatusDate();
		UpdateProjectCommand command = new UpdateProjectCommand(project, request);
		assertSame(project, command.getDocument());
		UndoableEditSupport editSupport = new UndoableEditSupport();
		UndoManager undoManager = new UndoManager();
		editSupport.addUndoableEditListener(undoManager);
		command.execute(java.util.List.of(task), editSupport);
		assertTrue(command.affectedTaskIds().contains(task.getUniqueId()));
		assertEquals(originalStatusDate, project.getStatusDate(), "the command date must not overwrite Status Date");
		assertNotEquals(0D, task.getPercentComplete());
		undoManager.undo();
		assertEquals(0D, task.getPercentComplete(), "one undo restores task progress");
		assertEquals(originalStatusDate, project.getStatusDate());
		undoManager.redo();
		assertNotEquals(0D, task.getPercentComplete(), "one redo reapplies task progress");
		assertEquals(originalStatusDate, project.getStatusDate());
	}

	@Test
	void selectedAndEntireProjectUpdatesRespectLinkedSummaryHierarchyAndUndo() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("update-hierarchy", undo), undo);
		project.initialize(false, false);
		Node summaryNode = project.createLocalTaskNode(null);
		Task summary = (Task) summaryNode.getImpl();
		Task predecessor = (Task) project.createLocalTaskNode(summaryNode).getImpl();
		Task successor = (Task) project.createLocalTaskNode(summaryNode).getImpl();
		DependencyService.getInstance().newDependency(predecessor, successor, DependencyType.FS, 0L, this);
		project.recalculate();
		long originalStatusDate = project.getStatusDate();
		long updateDate = successor.getEnd() + 2L * 86_400_000L;
		UpdateProjectCommand selected = new UpdateProjectCommand(project,
			new UpdateProjectRequest(updateDate, true, false, false));
		UndoableEditSupport edits = new UndoableEditSupport();
		UndoManager manager = new UndoManager();
		edits.addUndoableEditListener(manager);

		selected.execute(java.util.List.of(predecessor), edits);
		assertEquals(java.util.List.of(predecessor.getUniqueId()), selected.affectedTaskIds(),
			"Selected Tasks scope must not update a linked but unselected successor or its summary row");
		assertEquals(1D, predecessor.getPercentComplete(), 0.00001D);
		assertEquals(0D, successor.getPercentComplete(), 0.00001D);
		assertEquals(originalStatusDate, project.getStatusDate(), "through-date is not the project Status Date");
		manager.undo();
		assertEquals(0D, predecessor.getPercentComplete(), 0.00001D);
		assertEquals(0D, successor.getPercentComplete(), 0.00001D);
		manager.redo();
		assertEquals(1D, predecessor.getPercentComplete(), 0.00001D);
		assertEquals(0D, successor.getPercentComplete(), 0.00001D);

		UpdateProjectCommand entire = new UpdateProjectCommand(project,
			new UpdateProjectRequest(updateDate, true, false, false));
		entire.execute(project.getTaskList(), edits);
		assertEquals(java.util.Set.of(predecessor.getUniqueId(), successor.getUniqueId()),
			new java.util.HashSet<>(entire.affectedTaskIds()),
			"Entire Project updates both linked leaf tasks while summary completion remains derived");
		assertEquals(1D, predecessor.getPercentComplete(), 0.00001D);
		assertEquals(1D, successor.getPercentComplete(), 0.00001D);
		assertEquals(1D, summary.getPercentComplete(), 0.00001D);
		assertEquals(originalStatusDate, project.getStatusDate());
		manager.undo();
		assertEquals(0D, successor.getPercentComplete(), 0.00001D,
			"one Undo restores only the second transaction and all dependent task state");
		assertEquals(1D, predecessor.getPercentComplete());
		manager.redo();
		assertEquals(1D, successor.getPercentComplete());
		assertEquals(originalStatusDate, project.getStatusDate());
	}
}
