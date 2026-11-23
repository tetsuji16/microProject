/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.ui.frame;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;


/** The Timeline and Team Planner Format tabs act on their own views only. */
class TimelineTeamPlannerFormatTest {
	@Test
	void noViewDialogIsActiveOutsideTheirViews() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First", "Second")) {
			HeadlessDocumentFixture.onEdt(() -> {
				assertNull(fx.manager.getActiveTimelineDialog());
				assertNull(fx.manager.getActiveTeamPlannerDialog());
			});
		}
	}

	// The embedded Timeline and Team Planner dialogs need a display, so the command behaviour is
	// exercised by TimelineTeamPlannerRibbonGuiAcceptanceTest (guiTest).
}
