/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.command.UpdateProjectCommand;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

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
		project.setStatusDate(request.statusDate());
		long normalizedStatusDate = project.getStatusDate();
		project.clearStatusDate();
		UpdateProjectCommand command = new UpdateProjectCommand(project, request);
		command.accept(task);
		assertTrue(command.affectedTaskIds().contains(task.getUniqueId()));
		assertEquals(normalizedStatusDate, project.getStatusDate(),
			"Update Project must use the project's calendar-normalized status date");
	}
}
