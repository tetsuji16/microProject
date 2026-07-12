package com.projectlibre1.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;

class DefaultSubprojectHandlerTest {
	@Test
	void storesSubprojectReferencesAndCreatesStatefulPlaceholder() {
		DefaultSubprojectHandler handler = new DefaultSubprojectHandler(null);

		handler.setReferringSubprojectTasks(Collections.emptyList());
		SubProj subproject = handler.createSubProj(42L);

		assertNotNull(subproject);
		assertEquals(42L, subproject.getSubprojectUniqueId());
		assertEquals(0L, handler.getReferringSubprojectTaskDependencyDate());
	}

}
