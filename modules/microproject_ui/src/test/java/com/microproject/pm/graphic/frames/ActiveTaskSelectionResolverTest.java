/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.Node;
import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.pm.task.Task;
import com.microproject.pm.task.NormalTask;
import com.microproject.undo.DataFactoryUndoController;

class ActiveTaskSelectionResolverTest {
	@Test
	void physicalRowSelectionTakesPrecedenceOverStaleFrameSelection() {
		Project project = project();
		Node physicalRow = project.createLocalTaskNode(null);
		Node staleFrameSelection = project.createLocalTaskNode(null);

		assertEquals(List.of(physicalRow), ActiveTaskSelectionResolver.preferPhysicalRowSelection(true,
			List.of(physicalRow), List.of(staleFrameSelection)));
	}

	@Test
	void frameSelectionIsUsedWhenThereIsNoPhysicalRowSelection() {
		Project project = project();
		Node frameSelection = project.createLocalTaskNode(null);

		assertEquals(List.of(frameSelection), ActiveTaskSelectionResolver.preferPhysicalRowSelection(false,
			List.of(), List.of(frameSelection)));
	}

	@Test
	void selectedPhysicalRowSourceRemainsAuthoritativeWhenItsNodesAreTemporarilyEmpty() {
		Project project = project();
		Node staleFrameSelection = project.createLocalTaskNode(null);

		assertEquals(List.of(), ActiveTaskSelectionResolver.preferPhysicalRowSelection(true,
			List.of(), List.of(staleFrameSelection)));
	}

	@Test
	void resolvesTaskRowsAndSharesEligibilityForSingleAndMultipleTaskCommands() {
		Project project = project();
		Node first = project.createLocalTaskNode(null);
		Node second = project.createLocalTaskNode(null);

		var selection = ActiveTaskSelectionResolver.resolve(List.of(first, second), false, false, false);

		assertEquals(List.of(first, second), selection.nodes());
		assertEquals(List.of(((Task) first.getImpl()).getUniqueId(), ((Task) second.getImpl()).getUniqueId()),
			selection.stableTaskIds());
		assertTrue(selection.isEligible(1));
		assertTrue(selection.isEligible(2));
		assertFalse(selection.isEligible(3));
	}

	@Test
	void rejectsHeaderOnlyEmptyAndDisallowedMixedSelectionsWithReasons() {
		Project project = project();
		Node task = project.createLocalTaskNode(null);
		Node nonTask = NodeFactory.getInstance().createNode(project.getResourcePool().createScriptedResource());

		assertEquals("task-row-selection-required",
			ActiveTaskSelectionResolver.resolve(List.of(task), true, false, false).rejectionReason());
		assertEquals("no-selection",
			ActiveTaskSelectionResolver.resolve(List.of(), false, false, false).rejectionReason());
		assertEquals("mixed-selection",
			ActiveTaskSelectionResolver.resolve(List.of(task, nonTask), false, false, false).rejectionReason());
		Node laterTask = project.createLocalTaskNode(null);
		var taskThenResource = ActiveTaskSelectionResolver.resolve(List.of(task, nonTask, laterTask), false, false, false);
		assertEquals("mixed-selection", taskThenResource.rejectionReason());
		assertEquals(List.of(((Task) task.getImpl()).getUniqueId(), ((Task) laterTask.getImpl()).getUniqueId()),
			taskThenResource.stableTaskIds());
		var resourceThenTask = ActiveTaskSelectionResolver.resolve(List.of(nonTask, laterTask), false, false, false);
		assertEquals("mixed-selection", resourceThenTask.rejectionReason());
		assertEquals(List.of(((Task) laterTask.getImpl()).getUniqueId()), resourceThenTask.stableTaskIds());
		assertEquals(List.of(task),
			ActiveTaskSelectionResolver.resolve(List.of(task, nonTask), false, false, true).nodes());
		Node externalTask = project.createLocalTaskNode(null);
		((Task) externalTask.getImpl()).setExternal(true);
		assertEquals("mixed-selection",
			ActiveTaskSelectionResolver.resolve(List.of(externalTask, nonTask), false, true, false).rejectionReason());
	}

	@Test
	void reportsSelectedReadOnlyTaskWithoutTreatingItAsAnEmptySelection() {
		Project project = project();
		Node externalNode = project.createLocalTaskNode(null);
		NormalTask externalTask = (NormalTask) externalNode.getImpl();
		externalTask.setExternal(true);
		assertTrue(externalTask.isReadOnly());

		var selection = ActiveTaskSelectionResolver.resolve(List.of(externalNode), false, true, false);

		assertEquals("task-read-only", selection.rejectionReason());
		assertEquals(List.of(), selection.nodes());
		assertEquals(List.of(externalTask.getUniqueId()), selection.stableTaskIds());
		assertFalse(selection.isEligible(1));
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("active-selection", undo), undo);
		project.initialize(false, false);
		return project;
	}
}
