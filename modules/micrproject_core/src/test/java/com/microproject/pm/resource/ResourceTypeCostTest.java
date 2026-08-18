package com.microproject.pm.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.datatype.TimeUnit;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ResourceTypeCostTest {

	@Test
	void costConstantIsFiveAndUnique() {
		assertEquals(5, ResourceType.COST);
		assertTrue(ResourceType.getMap().containsValue(ResourceType.COST));
	}

	@Test
	void costResourceIsNotLaborAndNotMaterial() {
		EnterpriseResource resource = createResource();
		resource.setResourceType(ResourceType.COST);

		assertTrue(resource.isCost());
		assertFalse(resource.isLabor());
		assertFalse(resource.isWork());
		assertFalse(resource.isMaterial());
	}

	@Test
	void costResourceTypeChangeSetsNonTemporalRates() {
		EnterpriseResource resource = createResource();

		// Default is WORK
		assertTrue(resource.isLabor());
		assertEquals(ResourceType.WORK, resource.getResourceType());

		// Change to COST
		resource.setResourceType(ResourceType.COST);
		assertEquals(ResourceType.COST, resource.getResourceType());
		assertFalse(resource.isLabor());

		// Rates should be non-temporal
		assertEquals(TimeUnit.NON_TEMPORAL, resource.getStandardRate().getTimeUnit());
		assertEquals(TimeUnit.NON_TEMPORAL, resource.getOvertimeRate().getTimeUnit());
	}

	@Test
	void costResourceTypeIsInMap() {
		assertTrue(ResourceType.getMap().containsValue(Integer.valueOf(ResourceType.COST)));
	}

	private EnterpriseResource createResource() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		com.microproject.pm.resource.ResourcePool resourcePool =
			com.microproject.pm.resource.ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return new EnterpriseResource(resourcePool);
	}
}
