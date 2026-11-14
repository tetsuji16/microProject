/*******************************************************************************
 * MIT License
 * Copyright (c) 2026 microProject
 ******************************************************************************/
package com.microproject.pm.graphic.frames;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.Action;

import org.junit.jupiter.api.Test;

import com.microproject.menu.MenuActionConstants;

/** The Timeline and Details check boxes follow the visibility of their panes. */
class ViewPaneToggleSyncTest {
	private static boolean pressed(HeadlessDocumentFixture fx, String actionId) {
		Action action = fx.frame.getMenuManager().getActionFromId(actionId);
		return Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
	}

	@Test
	void timelineCheckBoxShowsAndHidesTheTimelinePane() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First")) {
			// The real timeline panel needs a window owner, which a headless run cannot create.
			HeadlessDocumentFixture.onEdt(() -> fx.frame.timelineView = new com.microproject.pm.graphic.views.DockableProjectToolView(fx.project,
				MenuActionConstants.ACTION_TIMELINE, new javax.swing.JPanel()));
			assertEquals(false, pressed(fx, MenuActionConstants.ACTION_TIMELINE));
			HeadlessDocumentFixture.onEdt(fx.frame::toggleTimelineView);
			assertEquals(true, pressed(fx, MenuActionConstants.ACTION_TIMELINE));
			assertEquals(MenuActionConstants.ACTION_TIMELINE, fx.frame.getTopViewId());
			assertEquals(false, pressed(fx, MenuActionConstants.ACTION_GANTT));

			HeadlessDocumentFixture.onEdt(fx.frame::toggleTimelineView);
			assertEquals(false, pressed(fx, MenuActionConstants.ACTION_TIMELINE));
			assertEquals(true, pressed(fx, MenuActionConstants.ACTION_GANTT));
			assertEquals(MenuActionConstants.ACTION_GANTT, fx.frame.getTopViewId());
		}
	}

	@Test
	void detailsCheckBoxFollowsTheBottomPane() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First")) {
			assertEquals(false, pressed(fx, MenuActionConstants.ACTION_DETAILS));
			HeadlessDocumentFixture.onEdt(() -> fx.frame.activateView(MenuActionConstants.ACTION_DETAILS));
			assertEquals(true, pressed(fx, MenuActionConstants.ACTION_DETAILS));
			HeadlessDocumentFixture.onEdt(() -> fx.frame.activateView(MenuActionConstants.ACTION_DETAILS));
			assertEquals(false, pressed(fx, MenuActionConstants.ACTION_DETAILS));
		}
	}
}
