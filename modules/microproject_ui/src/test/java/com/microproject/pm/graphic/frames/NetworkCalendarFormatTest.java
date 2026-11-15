/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.microproject.dialog.CalendarViewDialogBox;
import com.microproject.menu.MenuActionConstants;
import com.microproject.pm.graphic.views.PertView;

/** The Network Diagram and Calendar Format tabs act on their own views and not on the Gantt. */
class NetworkCalendarFormatTest {
	@Test
	void networkFormatCommandsActOnTheNetworkDiagramOnly() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First", "Second")) {
			HeadlessDocumentFixture.onEdt(() -> assertNull(fx.manager.getActiveNetworkView()));
			HeadlessDocumentFixture.onEdt(() -> assertTrue(fx.frame.activateView(MenuActionConstants.ACTION_NETWORK)));
			HeadlessDocumentFixture.onEdt(() -> {
				PertView view = fx.manager.getActiveNetworkView();
				assertNotNull(view);
				assertTrue(view.supportsSummaryTasksToggle());
				assertTrue(view.isSummaryTasksVisible());
				view.setSummaryTasksVisible(false);
				assertFalse(view.isSummaryTasksVisible());
				view.setSummaryTasksVisible(true);
				assertTrue(view.isSummaryTasksVisible());
				view.layoutNow();
			});
		}
	}
}
