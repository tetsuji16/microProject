/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.grouping.core.transform.filtering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.microproject.grouping.core.NodeFactory;
import com.microproject.pm.assignment.AssignmentEntry;
import com.microproject.pm.assignment.HasAssignmentsImpl;
import com.microproject.pm.resource.ResourceImpl;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class ResourceInTeamFilterTest {
	@Test
	void evaluatesResourcesAndAssignmentEntriesThroughTheirResource() {
		ResourceImpl resource = ResourcePool.createRourcePool("filter-test", new DataFactoryUndoController())
				.newResourceInstance();
		ResourceInTeamFilter filter = new ResourceInTeamFilter(null);
		AssignmentEntry entry = new AssignmentEntry(resource, new ArrayList<>(), null);
		boolean expected = resource.isInTeam();

		assertEquals(expected, filter.evaluate(NodeFactory.getInstance().createNode(resource)));
		assertEquals(expected, filter.evaluate(NodeFactory.getInstance().createNode(entry)));
		assertFalse(filter.evaluate(NodeFactory.getInstance().createNode(new Object())));
	}

	@Test
	void rejectsAssignmentEntriesWithoutAResourceInsteadOfFailingTheFilter() {
		ResourceInTeamFilter filter = new ResourceInTeamFilter(null);
		AssignmentEntry entry = new AssignmentEntry(new HasAssignmentsImpl(), new ArrayList<>(), null);

		assertFalse(filter.evaluate(NodeFactory.getInstance().createNode(entry)));
	}

	@Test
	void settingFilterModeNotifiesOnlyWhenTheModeChanges() {
		ResourceInTeamFilter filter = new ResourceInTeamFilter(null);
		ResourceImpl resource = createResource();
		AtomicReference<Object> notified = new AtomicReference<>();
		int[] notificationCount = { 0 };
		filter.setRedefinitionCallBack(value -> {
			notified.set(value);
			notificationCount[0]++;
		});

		filter.setFilterTeam(false);
		filter.setFilterTeam(false);

		assertSame(filter, notified.get());
		assertEquals(1, notificationCount[0]);
		assertTrue(filter.evaluate(NodeFactory.getInstance().createNode(resource)));
	}

	private ResourceImpl createResource() {
		return ResourcePool.createRourcePool("filter-test", new DataFactoryUndoController()).newResourceInstance();
	}
}
