/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.spreadsheet.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.microproject.field.Field;
import com.microproject.pm.resource.Resource;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.ui.privacy.PrivacyDisplayMode;
import com.microproject.undo.DataFactoryUndoController;

class SpreadSheetCellRendererAdapterPrivacyTest {
	@Test
	void masksResourceAssignmentsForScreenAndPrintValuePaths() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		ResourcePool pool = ResourcePool.createRourcePool("project", undo);
		Project project = Project.createProject(pool, undo);
		project.initialize(false, false);
		NormalTask task = (NormalTask) project.createLocalTaskNode(null).getImpl();
		Resource resource = pool.createScriptedResource();
		resource.setName("Confidential Resource");
		Field resourceNames = new Field();
		resourceNames.setId("Field.resourceNames");

		assertEquals("Confidential Resource[25%]", SpreadSheetCellRendererAdapter.maskResourceNames(
				"Confidential Resource[25%]", task, resourceNames));
		PrivacyDisplayMode.toggle(project);
		assertEquals("Resource 01[25%]", SpreadSheetCellRendererAdapter.maskResourceNames(
				"Confidential Resource[25%]", task, resourceNames));
		assertEquals("Confidential Resource[25%]", SpreadSheetCellRendererAdapter.maskResourceNames(
				"Confidential Resource[25%]", task, new Field()));
	}
}
