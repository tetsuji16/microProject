package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.scheduling.ConstraintType;
import com.microproject.undo.DataFactoryUndoController;

class ScheduleDiagnosticsServiceTest {
	@Test
	void reportsManualInactiveAndConstraintCauses() throws Exception {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("diagnostic-test", undo), undo);
		project.initialize(false, false);
		NormalTask task = new NormalTask(project);
		task.setName("Inspect me");
		project.connectTask(task);
		project.getSchedulingAlgorithm().addObject(task);
		task.setManualDates(project.getStart(), project.getStart() + 1L);
		task.setInactiveTask(true);
		task.setConstraintType(ConstraintType.MSO);

		var types = new ScheduleDiagnosticsService().diagnose(task).stream()
			.map(ScheduleDiagnosticsService.Issue::type).toList();

		assertTrue(types.contains(ScheduleDiagnosticsService.Type.MANUAL));
		assertTrue(types.contains(ScheduleDiagnosticsService.Type.INACTIVE));
	}
}
