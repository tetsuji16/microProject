/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.microproject.pm.resource.ResourcePool;
import com.microproject.undo.DataFactoryUndoController;

class TaskFontPropertiesTest {
	@Test
	void fontPropertiesCopyAndNormalizePersistentValues() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("font", undo), undo);
		NormalTask source = new NormalTask(project);
		NormalTask target = new NormalTask(project);
		source.setFontFamily("  Dialog  ");
		source.setFontSize(99);
		source.setFontColor(Integer.valueOf(0xFF336699));
		source.setFontBold(true);
		source.setFontItalic(true);
		source.setFontStrikethrough(true);
		source.cloneTo(target);

		assertEquals("Dialog", target.getFontFamily());
		assertEquals(72, target.getFontSize());
		assertEquals(Integer.valueOf(0x336699), target.getFontColor());
		assertEquals(true, target.isFontBold());
		assertEquals(true, target.isFontItalic());
		assertEquals(true, target.isFontStrikethrough());

		target.setFontFamily(" ");
		target.setFontColor(null);
		assertNull(target.getFontFamily());
		assertNull(target.getFontColor());
	}
}
