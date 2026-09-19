package com.microproject.ui.privacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class PrivacyDisplayModeTest {
	@Test
	void masksAndUnmasksWithoutChangingProjectData() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("private project", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		project.setName("Customer Roadmap");
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		task.setName("Confidential milestone");
		Resource resource = pool.createScriptedResource();
		resource.setName("Alice");

		assertFalse(PrivacyDisplayMode.isMasked(project));
		assertTrue(PrivacyDisplayMode.toggle(project));
		assertTrue(PrivacyDisplayMode.isMasked(project));
		assertEquals("Task 01", PrivacyDisplayMode.taskName(task));
		assertEquals("Resource 01", PrivacyDisplayMode.resourceName(project, resource));
		assertEquals("Project 01", PrivacyDisplayMode.projectName(project));
		assertEquals("Resource 01", PrivacyDisplayMode.resourceNames(project, "Alice"));
		assertEquals("Confidential milestone", task.getName());

		assertFalse(PrivacyDisplayMode.toggle(project));
		assertFalse(PrivacyDisplayMode.isMasked(project));
		assertEquals("Confidential milestone", PrivacyDisplayMode.taskName(task));
		assertEquals("Alice", PrivacyDisplayMode.resourceName(project, resource));
	}
}
