/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;

import org.junit.jupiter.api.Test;

import com.microproject.model.task.NormalTask;

class TaskFontStyleTest {
	@Test
	void resolvesAllTaskFontPropertiesWithoutChangingFallbackForPlainTasks() {
		Font fallback = new Font("Dialog", Font.PLAIN, 11);
		NormalTask task = new NormalTask();
		assertEquals(fallback, TaskFontStyle.resolveFont(task, fallback));

		task.setFontFamily("Serif");
		task.setFontSize(16);
		task.setFontBold(true);
		task.setFontItalic(true);
		task.setFontStrikethrough(true);
		task.setFontColor(Integer.valueOf(0x123456));

		Font resolved = TaskFontStyle.resolveFont(task, fallback);
		assertEquals("Serif", resolved.getFamily());
		assertEquals(16, resolved.getSize());
		assertEquals(Font.BOLD | Font.ITALIC, resolved.getStyle());
		assertEquals(TextAttribute.STRIKETHROUGH_ON, resolved.getAttributes().get(TextAttribute.STRIKETHROUGH));
		assertEquals(new Color(0x123456), TaskFontStyle.resolveColor(task, Color.BLACK));
	}

	@Test
	void underlineAndBackgroundFillAreResolvedFromTheTaskAndLeaveOtherTasksUntouched() {
		Font fallback = new Font("Dialog", Font.PLAIN, 11);
		NormalTask task = new NormalTask();
		assertEquals(null, TaskFontStyle.resolveBackground(task, null));
		task.setFontUnderline(true);
		task.setFontBackground(Integer.valueOf(0x00FF00));

		assertEquals(java.awt.font.TextAttribute.UNDERLINE_ON,
			TaskFontStyle.resolveFont(task, fallback).getAttributes().get(TextAttribute.UNDERLINE));
		assertEquals(new Color(0x00FF00), TaskFontStyle.resolveBackground(task, null));
		assertEquals(null, TaskFontStyle.resolveBackground(new NormalTask(), null));
	}

	@Test
	void inactiveTasksAreRenderedDimmedAndStruckThroughWithoutChangingSavedFormatting() {
		Font fallback = new Font("Dialog", Font.PLAIN, 11);
		NormalTask task = new NormalTask();
		task.setInactiveTask(true);

		Font inactive = TaskFontStyle.resolveFont(task, fallback);

		assertEquals(TextAttribute.STRIKETHROUGH_ON,
			inactive.getAttributes().get(TextAttribute.STRIKETHROUGH));
		assertNotEquals(Color.BLACK, TaskFontStyle.resolveColor(task, Color.BLACK));
		assertEquals(false, task.isFontStrikethrough(), "rendering must not mutate the user's font formatting");
	}
}
