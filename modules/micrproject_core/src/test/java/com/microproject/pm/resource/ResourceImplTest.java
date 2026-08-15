package com.microproject.pm.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.company.ApplicationUser;
import com.microproject.field.FieldContext;
import com.microproject.pm.assignment.Assignment;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class ResourceImplTest {
	@Test
	void fixedCostAndOffsetsDelegateToGlobalResource() {
		DelegatingEnterpriseResource global = new DelegatingEnterpriseResource();
		ResourceImpl resource = new ResourceImpl(global);

		assertEquals(123.0D, resource.fixedCost(10L, 20L), 0.00001D);
		assertEquals(77.0D, resource.getActualFixedCost(null), 0.00001D);
		assertTrue(resource.fieldHideActualFixedCost(FieldContext.DEFAULT_CONTEXT));
		assertEquals(45L, resource.getStartOffset());
		assertEquals(67L, resource.getFinishOffset());
	}

	@Test
	void addAssignmentPromotesInactiveResourceIntoTeam() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		resource.setRole(ApplicationUser.INACTIVE);

		Assignment assignment = Assignment.getInstance(task, resource, 1.0D, 0);
		resource.addAssignment(assignment);

		assertEquals(ApplicationUser.TEAM_RESOURCE, resource.getRole());
		assertTrue(resource.isInTeam());
	}

	@Test
	void assignmentStatusMethodsReflectUnderlyingAssignments() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		Assignment assignment = Assignment.getInstance(task, resource, 1.0D, 0);
		resource.addAssignment(assignment);

		assertTrue(resource.isUnstarted());
		assertFalse(resource.inProgress());

		assignment.setPercentComplete(0.5D);

		assertTrue(resource.inProgress());
		assertFalse(resource.isComplete());

		assignment.setPercentComplete(1.0D);

		assertTrue(resource.isComplete());
	}

	private Project createProject() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		project.initialize(false, false);
		return project;
	}

	private NormalTask createTask(Project project) {
		NormalTask task = new NormalTask(project);
		project.connectTask(task);
		return task;
	}

	private static final class DelegatingEnterpriseResource extends EnterpriseResource {
		private DelegatingEnterpriseResource() {
			super((ResourcePool) null);
		}

		public double fixedCost(long start, long end) {
			return 123.0D;
		}

		public double getActualFixedCost(FieldContext fieldContext) {
			return 77.0D;
		}

		public boolean fieldHideActualFixedCost(FieldContext fieldContext) {
			return true;
		}

		public long getStartOffset() {
			return 45L;
		}

		public long getFinishOffset() {
			return 67L;
		}
	}
}
