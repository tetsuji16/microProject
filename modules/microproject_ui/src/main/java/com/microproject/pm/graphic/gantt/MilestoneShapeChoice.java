/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 *******************************************************************************/
package com.microproject.pm.graphic.gantt;

import com.microproject.strings.Messages;

/** Shared choice list for the default and per-task milestone shape controls. */
public enum MilestoneShapeChoice {
	AUTOMATIC(null, "Gantt.FormatBar.automatic"),
	DIAMOND("DIAMOND", "Gantt.FormatBar.shapeDiamond"),
	SQUARE("SQUARE", "Gantt.FormatBar.shapeSquare"),
	TRIANGLE_UP("TRIANGLE_UP", "Gantt.FormatBar.shapeTriangleUp"),
	TRIANGLE_DOWN("TRIANGLE_DOWN", "Gantt.FormatBar.shapeTriangleDown");

	private final String shapeName;
	private final String labelKey;

	MilestoneShapeChoice(String shapeName, String labelKey) {
		this.shapeName = shapeName;
		this.labelKey = labelKey;
	}

	public String getShapeName() { return shapeName; }

	public static MilestoneShapeChoice forName(String shapeName) {
		for (MilestoneShapeChoice choice : values())
			if (java.util.Objects.equals(choice.shapeName, shapeName)) return choice;
		return AUTOMATIC;
	}

	@Override
	public String toString() { return Messages.getString(labelKey); }
}
