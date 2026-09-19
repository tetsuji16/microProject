/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import com.microproject.pm.task.Task;

/** Resolves optional task text formatting without changing the application defaults. */
public final class TaskFontStyle {
	private TaskFontStyle() {
	}

	public static Font resolveFont(Object object, Font fallback) {
		if (!(object instanceof Task task) || fallback == null)
			return fallback;
		String family = task.getFontFamily();
		int size = task.getFontSize();
		int style = (task.isFontBold() ? Font.BOLD : Font.PLAIN) | (task.isFontItalic() ? Font.ITALIC : Font.PLAIN);
		if ((family == null || family.isBlank()) && size == 0 && style == Font.PLAIN && !task.isFontStrikethrough())
			return fallback;
		Font resolved = fallback.deriveFont(style, (float)(size == 0 ? fallback.getSize() : size));
		if (family != null && !family.isBlank())
			resolved = new Font(family, style, size == 0 ? fallback.getSize() : size);
		if (!task.isFontStrikethrough())
			return resolved;
		Map<TextAttribute, Object> attributes = new HashMap<>(resolved.getAttributes());
		attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		return resolved.deriveFont(attributes);
	}

	public static Color resolveColor(Object object, Color fallback) {
		if (!(object instanceof Task task) || task.getFontColor() == null)
			return fallback;
		return new Color(task.getFontColor().intValue());
	}

	public static void apply(Component component, Object object, boolean selected) {
		if (component == null || !(object instanceof Task))
			return;
		component.setFont(resolveFont(object, component.getFont()));
		if (!selected)
			component.setForeground(resolveColor(object, component.getForeground()));
	}
}
