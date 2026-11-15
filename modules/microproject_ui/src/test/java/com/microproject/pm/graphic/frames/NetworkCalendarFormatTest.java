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

	@Test
	void networkDisplayOptionsReachTheDiagramAndSurviveAWorkspaceRoundTrip() throws Exception {
		try (HeadlessDocumentFixture fx = new HeadlessDocumentFixture("First", "Second")) {
			HeadlessDocumentFixture.onEdt(() -> assertTrue(fx.frame.activateView(MenuActionConstants.ACTION_NETWORK)));
			HeadlessDocumentFixture.onEdt(() -> {
				PertView view = fx.manager.getActiveNetworkView();
				assertFalse(view.getDisplayOptions().isLinkLabels());
				assertFalse(view.getLinkRouting() instanceof com.microproject.pm.graphic.link_routing.StraightNetworkLinkRouting);
				view.setLinkLabels(true);
				view.setProgressMarks(true);
				view.setStraightLinks(true);
				view.setCollapsedBoxes(true);
				view.setBoxSpacing(30);
				view.setBoxStyle(com.microproject.pm.graphic.network.NetworkDisplayOptions.BoxType.CRITICAL,
					new com.microproject.pm.graphic.network.NetworkDisplayOptions.BoxStyle(0xC0392B, com.microproject.pm.graphic.network.NetworkDisplayOptions.BoxShape.ROUNDED));
				assertTrue(view.getLinkRouting() instanceof com.microproject.pm.graphic.link_routing.StraightNetworkLinkRouting);
				assertTrue(view.getDisplayOptions().isCollapsed());
				com.microproject.workspace.WorkspaceSetting workspace = view.createWorkspace(0);
				view.setLinkLabels(false);
				view.setProgressMarks(false);
				view.setStraightLinks(false);
				view.setCollapsedBoxes(false);
				view.setBoxSpacing(0);
				view.setBoxStyle(com.microproject.pm.graphic.network.NetworkDisplayOptions.BoxType.CRITICAL, null);
				assertFalse(view.getLinkRouting() instanceof com.microproject.pm.graphic.link_routing.StraightNetworkLinkRouting);
				view.restoreWorkspace(workspace, 0);
				assertTrue(view.getDisplayOptions().isLinkLabels());
				assertTrue(view.getDisplayOptions().isProgressMarks());
				assertTrue(view.getDisplayOptions().isCollapsed());
				assertEquals(30, view.getDisplayOptions().getBoxSpacing());
				assertTrue(view.getLinkRouting() instanceof com.microproject.pm.graphic.link_routing.StraightNetworkLinkRouting);
				assertEquals(0xC0392B, view.getDisplayOptions().getStyle(com.microproject.pm.graphic.network.NetworkDisplayOptions.BoxType.CRITICAL).rgb());
			});
		}
	}
}
