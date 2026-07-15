package com.projectlibre1.pm.task;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultSubProjTest {
	@Test
	void tracksValidityAndFetchingStateWithoutPretendingAnUnopenedProjectIsOpen() {
		DefaultSubProj subproject = new DefaultSubProj(null, 42L);

		assertTrue(subproject.isValid());
		assertFalse(subproject.isSubprojectOpen());
		subproject.setFetching(true);
		assertFalse(subproject.isValidAndOpen());
	}

	@Test
	void rejectsNegativeSubprojectIds() {
		DefaultSubProj subproject = new DefaultSubProj(null, 1L);

		assertThrows(IllegalArgumentException.class, () -> subproject.setSubprojectUniqueId(-1L));
	}

	@Test
	void subprojectPlaceholderHonorsTheTaskContract() {
		com.projectlibre1.undo.DataFactoryUndoController undoController = new com.projectlibre1.undo.DataFactoryUndoController();
		Project project = Project.createProject(
				com.projectlibre1.pm.resource.ResourcePool.createRourcePool("test", undoController), undoController);
		DefaultSubProj subproject = new DefaultSubProj(project, 42L);

		assertTrue(subproject instanceof NormalTask);
		assertTrue(subproject.isSubproject());
		assertSame(project, subproject.getProject());
	}
}
