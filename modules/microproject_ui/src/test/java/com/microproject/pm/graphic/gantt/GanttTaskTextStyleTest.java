/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.gantt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;

import org.junit.jupiter.api.Test;

import com.microproject.pm.graphic.TaskFontStyle;
import com.microproject.pm.resource.ResourcePool;
import com.microproject.pm.task.NormalTask;
import com.microproject.pm.task.Project;
import com.microproject.undo.DataFactoryUndoController;

class GanttTaskTextStyleTest {
	@Test
	void taskAnnotationUsesCopiedFamilySizeWeightSlantStrikeAndColor() {
		DataFactoryUndoController undo = new DataFactoryUndoController();
		Project project = Project.createProject(ResourcePool.createRourcePool("gantt-task-text-style", undo), undo);
		project.initialize(false, false);
		NormalTask task = project.createScriptedTask();
		task.setFontFamily("Dialog");
		task.setFontSize(15);
		task.setFontColor(0x2457A6);
		task.setFontBold(true);
		task.setFontItalic(true);
		task.setFontStrikethrough(true);

		Font annotation = GanttRenderer.resolveTaskAnnotationFont(task, new Font("SansSerif", Font.PLAIN, 12));
		assertEquals("Dialog", annotation.getFamily());
		assertEquals(15, annotation.getSize());
		assertEquals(Font.BOLD | Font.ITALIC, annotation.getStyle());
		assertEquals(TextAttribute.STRIKETHROUGH_ON, annotation.getAttributes().get(TextAttribute.STRIKETHROUGH));
		assertEquals(new Color(0x2457A6), TaskFontStyle.resolveColor(task, Color.BLACK));
	}
}
