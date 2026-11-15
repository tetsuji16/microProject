/*******************************************************************************
 * MIT License
 *
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.microproject.dialog.TeamPlannerViewSettings.Category;

/** The Team Planner Format settings and their workspace round trip. */
class TeamPlannerViewSettingsTest {
	@Test
	void outlineLevelAndTextLinesAreClamped() {
		TeamPlannerViewSettings settings = new TeamPlannerViewSettings();
		settings.setOutlineLevel(50);
		assertEquals(TeamPlannerViewSettings.MAX_OUTLINE_LEVEL, settings.getOutlineLevel());
		settings.setOutlineLevel(-1);
		assertEquals(0, settings.getOutlineLevel());
		settings.setTextLines(0);
		assertEquals(TeamPlannerViewSettings.MIN_TEXT_LINES, settings.getTextLines());
		settings.setTextLines(40);
		assertEquals(TeamPlannerViewSettings.MAX_TEXT_LINES, settings.getTextLines());
	}

	@Test
	void resettingACategoryClearsOnlyThatCategory() {
		TeamPlannerViewSettings settings = new TeamPlannerViewSettings();
		settings.setFill(Category.AUTO, 0x112233);
		settings.setBorder(Category.AUTO, 0x445566);
		settings.setFill(Category.LATE, 0x778899);
		settings.resetCategory(Category.AUTO);
		assertNull(settings.getFill(Category.AUTO));
		assertNull(settings.getBorder(Category.AUTO));
		assertEquals(0x778899, settings.getFill(Category.LATE));
	}

	@Test
	void everyOptionSurvivesASaveAndRestore() {
		TeamPlannerViewSettings settings = new TeamPlannerViewSettings();
		settings.setOutlineLevel(3);
		settings.setGridlines(!settings.isGridlines());
		settings.setTextLines(2);
		settings.setExpandRows(!settings.isExpandRows());
		settings.setShowUnassigned(!settings.isShowUnassigned());
		settings.setShowUnscheduled(!settings.isShowUnscheduled());
		settings.setPreventOverallocation(!settings.isPreventOverallocation());
		settings.setFill(Category.MANUAL, 0xAA0011);
		settings.setBorder(Category.EXTERNAL, 0x00BB22);
		settings.setTaskFill(5L, 0x123456);
		settings.setTaskBorder(6L, 0x654321);

		TeamPlannerViewSettings restored = new TeamPlannerViewSettings();
		restored.restoreSettings(settings.saveSettings());

		assertEquals(3, restored.getOutlineLevel());
		assertEquals(settings.isGridlines(), restored.isGridlines());
		assertEquals(2, restored.getTextLines());
		assertEquals(settings.isExpandRows(), restored.isExpandRows());
		assertEquals(settings.isShowUnassigned(), restored.isShowUnassigned());
		assertEquals(settings.isShowUnscheduled(), restored.isShowUnscheduled());
		assertEquals(settings.isPreventOverallocation(), restored.isPreventOverallocation());
		assertEquals(0xAA0011, restored.getFill(Category.MANUAL));
		assertEquals(0x00BB22, restored.getBorder(Category.EXTERNAL));
		assertEquals(0x123456, restored.getTaskFill(5L));
		assertEquals(0x654321, restored.getTaskBorder(6L));
	}
}
