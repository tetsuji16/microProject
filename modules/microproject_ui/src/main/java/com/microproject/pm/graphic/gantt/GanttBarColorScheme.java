/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/
package com.microproject.pm.graphic.gantt;

import java.awt.Color;

/**
 * Preset Gantt Chart Style colour schemes of the Gantt Chart Format tab. A scheme recolours the
 * regular and critical task bars of one Gantt view; {@link #DEFAULT} keeps the palette colours.
 * The id is what the view persists with its workspace.
 */
public enum GanttBarColorScheme {
	DEFAULT("default", null, null),
	BLUE("blue", new Color(0x2F6FBF), new Color(0xD9372B)),
	GREEN("green", new Color(0x3C9A4F), new Color(0xC2410C)),
	ORANGE("orange", new Color(0xE08A1E), new Color(0x9B1C1C)),
	PURPLE("purple", new Color(0x7A4FB5), new Color(0xD9372B)),
	GRAY("gray", new Color(0x6B7280), new Color(0x111827));

	private final String id;
	private final Color taskColor;
	private final Color criticalColor;

	GanttBarColorScheme(String id, Color taskColor, Color criticalColor) {
		this.id = id;
		this.taskColor = taskColor;
		this.criticalColor = criticalColor;
	}

	public String getId() {
		return id;
	}

	/** Colour of regular task bars, or null when the palette decides. */
	public Color getTaskColor() {
		return taskColor;
	}

	/** Colour of critical task bars, or null when the palette decides. */
	public Color getCriticalColor() {
		return criticalColor;
	}

	/** Resolves a persisted id; null and unknown ids (older or newer files) fall back to {@link #DEFAULT}. */
	public static GanttBarColorScheme fromId(String id) {
		for (GanttBarColorScheme scheme : values()) {
			if (scheme.id.equals(id))
				return scheme;
		}
		return DEFAULT;
	}
}
