/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;

import org.junit.jupiter.api.Test;

import com.microproject.pm.task.NormalTask;

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
}
