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
	void calendarViewIsAnExclusiveTopViewLikeGantt() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First")) {
			// The real calendar panel needs a window owner, which a headless run cannot create.
			HeadlessDocumentFixture.onEdt(() -> fx.frame.calendarView = new com.microproject.pm.graphic.views.DockableProjectToolView(
				fx.project, MenuActionConstants.ACTION_CALENDAR_VIEW, new javax.swing.JPanel()));
			assertEquals(true, pressed(fx, MenuActionConstants.ACTION_GANTT));
			assertEquals(false, pressed(fx, MenuActionConstants.ACTION_CALENDAR_VIEW));

			HeadlessDocumentFixture.onEdt(() -> fx.frame.activateView(MenuActionConstants.ACTION_CALENDAR_VIEW));
			assertEquals(true, pressed(fx, MenuActionConstants.ACTION_CALENDAR_VIEW));
			assertEquals(false, pressed(fx, MenuActionConstants.ACTION_GANTT));
			assertEquals(MenuActionConstants.ACTION_CALENDAR_VIEW, fx.frame.getTopViewId());

			// Selecting the view that is already shown keeps it shown.
			HeadlessDocumentFixture.onEdt(() -> fx.frame.activateView(MenuActionConstants.ACTION_CALENDAR_VIEW));
			assertEquals(true, pressed(fx, MenuActionConstants.ACTION_CALENDAR_VIEW));

			HeadlessDocumentFixture.onEdt(() -> fx.frame.activateView(MenuActionConstants.ACTION_GANTT));
			assertEquals(false, pressed(fx, MenuActionConstants.ACTION_CALENDAR_VIEW));
			assertEquals(true, pressed(fx, MenuActionConstants.ACTION_GANTT));
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

	@Test
	void privacyMaskButtonFollowsTheDocumentsMaskState() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First")) {
			String id = MenuActionConstants.ACTION_TOGGLE_PRIVACY_MASK;
			assertEquals(false, pressed(fx, id));
			// The mask state belongs to the document; the button only mirrors it.
			assertEquals(true, com.microproject.ui.privacy.PrivacyDisplayMode.toggle(fx.project));
			assertEquals(false, pressed(fx, id));
			HeadlessDocumentFixture.onEdt(fx.frame::syncPrivacyMaskToggle);
			assertEquals(true, pressed(fx, id));

			// Re-activating the document re-reads its state instead of keeping a stale press.
			com.microproject.ui.privacy.PrivacyDisplayMode.toggle(fx.project);
			HeadlessDocumentFixture.onEdt(() -> fx.frame.refreshViewButtons(true));
			assertEquals(false, pressed(fx, id));
		}
	}
}
