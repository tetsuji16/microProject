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
import com.microproject.undo.DataFactoryUndoController;

class ActiveTaskSelectionResolverTest {
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
		assertEquals(List.of(task),
			ActiveTaskSelectionResolver.resolve(List.of(task, nonTask), false, false, true).nodes());
	}

	private static Project project() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("active-selection", undo), undo);
		project.initialize(false, false);
		return project;
	}
}
