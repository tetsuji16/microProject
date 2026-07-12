package com.projectlibre1.pm.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.projectlibre1.pm.assignment.timesheet.TimesheetStatus;
import com.projectlibre1.pm.resource.ResourceImpl;
import com.projectlibre1.pm.resource.ResourcePool;
import com.projectlibre1.pm.task.NormalTask;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.undo.DataFactoryUndoController;

class AssignmentTimesheetBehaviorTest {
	@Test
	void applyTimesheetMarksTheAssignmentAndSourceIntegrated() {
		Assignment assignment = createAssignment();
		assignment.setTimesheetAssignment(true);
		assignment.setTimesheetStatus(TimesheetStatus.VALIDATED);

		long updateTime = 1_717_504_800_000L;
		boolean updated = assignment.applyTimesheet(Collections.emptyList(), updateTime);

		assertTrue(updated);
		assertEquals(TimesheetStatus.INTEGRATED, assignment.getTimesheetStatus());
		assertEquals(updateTime, assignment.getLastTimesheetUpdate());
	}

	@Test
	void timesheetStatusPredicatesPreserveTheExistingContract() {
		Assignment assignment = createAssignment();
		assignment.setTimesheetAssignment(true);
		assignment.setTimesheetStatus(TimesheetStatus.ENTERED);

		assertFalse(assignment.isTimesheetEntered());
		assertTrue(assignment.isTimesheetEditable());
		assertTrue(assignment.isTimesheetValidated());
		assertTrue(assignment.isTimesheetRejected());
	}

	private Assignment createAssignment() {
		Project project = createProject();
		NormalTask task = createTask(project);
		ResourceImpl resource = project.getResourcePool().newResourceInstance();
		return Assignment.getInstance(task, resource, 1.0D, 0);
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
}
