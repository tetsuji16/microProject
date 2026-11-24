/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.UIManager;

import com.microproject.model.task.Task;

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
		boolean strikethrough = task.isFontStrikethrough() || task.isInactiveTask();
		boolean underline = task.isFontUnderline();
		if ((family == null || family.isBlank()) && size == 0 && style == Font.PLAIN && !strikethrough && !underline)
			return fallback;
		FontKey key = new FontKey(fallback, family == null || family.isBlank() ? null : family, style, size, strikethrough, underline);
		synchronized (FONT_CACHE) {
			Font cached = FONT_CACHE.get(key);
			if (cached != null)
				return cached;
		}
		Font resolved = fallback.deriveFont(style, (float)(size == 0 ? fallback.getSize() : size));
		if (family != null && !family.isBlank())
			resolved = new Font(family, style, size == 0 ? fallback.getSize() : size);
		if (strikethrough || underline) {
			Map<TextAttribute, Object> attributes = new HashMap<>(resolved.getAttributes());
			if (strikethrough)
				attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
			if (underline)
				attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
			resolved = resolved.deriveFont(attributes);
		}
		synchronized (FONT_CACHE) {
			FONT_CACHE.put(key, resolved);
		}
		return resolved;
	}

	private record FontKey(Font fallback, String family, int style, int size, boolean strikethrough, boolean underline) {
	}

	/** Small LRU of derived fonts so painting does not allocate a Font per cell/row. */
	private static final Map<FontKey, Font> FONT_CACHE = new java.util.LinkedHashMap<>(16, 0.75f, true) {
		private static final long serialVersionUID = 1L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<FontKey, Font> eldest) {
			return size() > 32;
		}
	};

	public static Color resolveColor(Object object, Color fallback) {
		if (!(object instanceof Task task))
			return fallback;
		if (task.isInactiveTask()) {
			Color disabled = UIManager.getColor("Label.disabledForeground");
			return disabled == null ? fallback.darker() : disabled;
		}
		if (task.getFontColor() == null)
			return fallback;
		return new Color(task.getFontColor().intValue());
	}

	/** The task's cell fill color, or {@code fallback} when it has none. */
	public static Color resolveBackground(Object object, Color fallback) {
		if (!(object instanceof Task task) || task.getFontBackground() == null)
			return fallback;
		return new Color(task.getFontBackground().intValue());
	}

	public static void apply(Component component, Object object, boolean selected) {
		if (component == null || !(object instanceof Task))
			return;
		component.setFont(resolveFont(object, component.getFont()));
		if (!selected) {
			component.setForeground(resolveColor(object, component.getForeground()));
			Color fill = resolveBackground(object, null);
			if (fill != null)
				component.setBackground(fill);
		}
	}
}
