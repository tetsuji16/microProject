/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.assignment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microproject.pm.task.Project;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class TimeDistributedFieldVisibilityTest {
	@Test
	void hidesWhenThereAreNoApplicableChildren() {
		assertTrue(TimeDistributedFieldVisibility.isBaselineFieldHidden(List.of(), 1, null));
		assertTrue(TimeDistributedFieldVisibility.isBaselineFieldHidden(List.of("unrelated"), 1, null));
	}

	@Test
	void hidesOnlyWhenEveryApplicableChildIsHidden() {
		TimeDistributedFields hidden = fieldVisibility(true);
		TimeDistributedFields visible = fieldVisibility(false);

		assertTrue(TimeDistributedFieldVisibility.isBaselineFieldHidden(List.of(hidden, hidden), 1, null));
		assertFalse(TimeDistributedFieldVisibility.isBaselineFieldHidden(List.of(hidden, visible), 1, null));
		assertFalse(TimeDistributedFieldVisibility.isBaselineFieldHidden(List.of(visible, hidden), 1, null));
	}

	@Test
	void projectHidesBaselineFieldsWhenEveryTaskLacksTheBaseline() {
		DataFactoryUndoController undoController = new DataFactoryUndoController();
		Project project = Project.createProject(
			ResourcePool.createRourcePool("baseline-visibility", undoController), undoController);
		project.createScriptedTask();

		assertTrue(project.fieldHideBaselineCost(0, null));
		assertTrue(project.fieldHideBaselineWork(0, null));
	}

	private static TimeDistributedFields fieldVisibility(boolean hidden) {
		return (TimeDistributedFields) Proxy.newProxyInstance(
			TimeDistributedFields.class.getClassLoader(),
			new Class<?>[] {TimeDistributedFields.class},
			(proxy, method, arguments) -> method.getName().equals("fieldHideBaselineCost") && hidden
		);
	}
}
