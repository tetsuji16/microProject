/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class GanttAnnotationVisibilityTest {
	@Test
	void hiddenAnnotationStateSuppressesOnlyTheSelectedLabelMode() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		ResourcePool resourcePool = ResourcePool.createRourcePool("annotation-visibility-test", undoController);
		Project project = Project.createProject(resourcePool, undoController);
		Gantt gantt = new Gantt(project, "Gantt");
		try {
			gantt.setAnnotationFieldId(Gantt.ANNOTATION_FIELD_HIDDEN);
			assertTrue(gantt.isAnnotationHidden());

			gantt.setAnnotationFieldId("Field.name");
			assertFalse(gantt.isAnnotationHidden());
		} finally {
			gantt.cleanUp();
		}
	}
}
